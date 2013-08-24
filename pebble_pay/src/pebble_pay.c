#include "pebble_os.h"
#include "pebble_app.h"
#include "pebble_fonts.h"


#define MY_UUID { 0xF5, 0x98, 0x21, 0x23, 0xB1, 0x03, 0x43, 0x1B, 0xA3, 0x30, 0xB6, 0xF0, 0x35, 0xC7, 0xDC, 0xAF }
PBL_APP_INFO(MY_UUID,
             "Pebble Pay", "Steven Karis",
             1, 0, /* App version */
             DEFAULT_MENU_ICON,
             APP_INFO_STANDARD_APP);

enum {
  PAY_KEY_RECIEVE=0x00,
  PAY_KEY_RECIEVE_LAST=0x01,
  PAY_KEY_FETCH=0x02,
};

static Window s_window;
static BitmapLayer s_bitmap_layer;
static GBitmap bitmap;
static uint8_t qr_code_data[16384];
static AppContextRef s_app_ctx;
static bool callbacks_registered;
int packets;
static AppMessageCallbacksNode app_callbacks;
TextLayer errorTextLayer;
char* msg;
GBitmap qr_code;

// Using this is a workaround as there's currently no other way to
// allocate storage for a bitmap--but the extra automatically created
// layer is useful for getting dimension information anyway.
enum {
  CMD_KEY = 0x0, // TUPLE_INTEGER
};

static void displayErrorMessage(char *errorMessage)
{
    text_layer_set_text(&errorTextLayer, errorMessage);
    layer_set_hidden(&errorTextLayer.layer, false);

    vibes_double_pulse();
}

static char *appMessageResultToString(AppMessageResult reason)
{
    switch (reason)
    {
        case APP_MSG_BUFFER_OVERFLOW:
            return "buffer overflow";
        default:
            return "unknown error";
    }
}

static void pebble_pay_init(void) {
      DictionaryIterator *iter;

      if (app_message_out_get(&iter) != APP_MSG_OK) {
          return;
      }
      if (dict_write_uint8(iter, PAY_KEY_FETCH, 0) != DICT_OK) {
          return;
      }
      app_message_out_send();
      app_message_out_release();
}

static void display_no_qr() {
    resource_load(resource_get_handle(RESOURCE_ID_QR_CODE_MISSING), qr_code_data, 512);
    layer_mark_dirty((Layer*)&s_bitmap_layer);
}

static void in_received_handler(DictionaryIterator *iter, void *context) {
    Tuple* tuple = dict_find(iter, PAY_KEY_RECIEVE);
    if(tuple) {
            size_t offset = tuple->value->data[0];
            memcpy(qr_code_data + packets, tuple->value->data, tuple->length);
            layer_mark_dirty(&s_bitmap_layer.layer);
            bitmap_layer_set_bitmap(&s_bitmap_layer, &bitmap);
            packets = packets + 64;
            pebble_pay_init();
    } else {
        Tuple* tuple = dict_find(iter, PAY_KEY_RECIEVE_LAST);
        if(tuple) {
                size_t offset = tuple->value->data[0];
                memcpy(qr_code_data + packets, tuple->value->data, tuple->length);
                layer_mark_dirty(&s_bitmap_layer.layer);
                bitmap_layer_set_bitmap(&s_bitmap_layer, &bitmap);
        }
    }
}

static void in_dropped_handler(void *context, AppMessageResult reason) {
    displayErrorMessage(appMessageResultToString(reason));
    app_log(APP_LOG_LEVEL_DEBUG, "here", 76, msg);
}

static void window_load(Window* window) {
    //TODO add bitmap rendering here
    //  menu_layer_init(&s_menu_layer, window->layer.bounds);

    bitmap = (GBitmap) {
        .addr = qr_code_data,
        .bounds = GRect(5, 5, 128, 128),
        .info_flags = 1,
        .row_size_bytes = 16,
    };
    packets = 0;
    //memset(album_art_data, 0, 512);
    vibes_long_pulse();
    bitmap_layer_init(&s_bitmap_layer, GRect(8, 10, 128, 128));
    bitmap_layer_set_bitmap(&s_bitmap_layer, &bitmap);
    layer_add_child(window_get_root_layer(window), &s_bitmap_layer.layer);
}

static void app_send_failed(DictionaryIterator* failed, AppMessageResult reason, void* context) {
  // TODO: error handling
}

void handle_init(AppContextRef ctx) {

  pebble_pay_init();

  app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);

  window_init(&s_window, "Draw bitmap");
  window_set_window_handlers(&s_window, (WindowHandlers){
        .load = window_load
    });
  window_stack_push(&s_window, true /* Animated */);
}


void pbl_main(void *params) {
  PebbleAppHandlers handlers = {
    .init_handler = &handle_init,
    .messaging_info = {
      .buffer_sizes = {
        .inbound = 128,
        .outbound = 124,
      },
      .default_callbacks.callbacks = {
        .in_received = in_received_handler,
        .in_dropped = in_dropped_handler,
      },
    },
  };
  app_event_loop(params, &handlers);
}
