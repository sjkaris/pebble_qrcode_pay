package com.clover.pebblepay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.clover.pebblepay.connection.HTTPPostTask;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import org.json.JSONObject;


public class payActivity extends Activity
{

  private final Random rand = new Random();
  private PebbleKit.PebbleDataReceiver pebblePayReceiver = null;
  private PebbleKit.PebbleNackReceiver pebbleNackReceiver = null;
  private LinkedList<byte[]> chunks;
  private String access_token;
  private TextView txt;
  private EditText edt_txt;
  private Button button;
  private String qr_code;
  private static boolean isRunning = false;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    if(access_token == null) {
      txt = (TextView) findViewById(R.id.textview);
      edt_txt = (EditText) findViewById(R.id.edittext);
      button = (Button) findViewById(R.id.button);
    } else {
      access_token = savedInstanceState.getString("token");
      txt.setVisibility(View.INVISIBLE);
      edt_txt.setVisibility(View.INVISIBLE);
      button.setVisibility(View.INVISIBLE);
    }

    final Handler handler = new Handler();

    // To receive data back from the sports watch-app, Android
    // applications must register a "DataReceiver" to operate on the
    // dictionaries received from the watch.
    //
    // In this example, we're registering a receiver to listen for
    // changes in the activity state sent from the watch, allowing
    // us the pause/resume the activity when the user presses a
    // button in the watch-app.
    pebblePayReceiver = new PebbleKit.PebbleDataReceiver( UUID.fromString("f5982123-b103-431b-a330-b6f035c7dcaf")) {
      @Override
      public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {

        PebbleKit.sendAckToPebble(context, transactionId);
        if(isRunning){
          return;
        }
        if(chunks == null) {
          try {
            isRunning = true;
            JSONObject obj = new JSONObject();
            obj.put("token", access_token);
            HTTPPostTask post = new HTTPPostTask(obj, "http://clover-example-app.com/get_qr_from_token");
            post.execute();
            JSONObject resp = post.get();
            qr_code = resp.getString("qr_code");
            access_token = resp.getString("token");
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        handler.post(new Runnable() {
          @Override
          public void run() {
            sendChunks();
          }
        });
      }
    };
    pebbleNackReceiver = new PebbleKit.PebbleNackReceiver(UUID.fromString("f5982123-b103-431b-a330-b6f035c7dcaf")) {
      @Override
      public void receiveNack(Context context, int transactionId) {
        chunks = null;
      }
    };
    PebbleKit.registerReceivedDataHandler(this, pebblePayReceiver);
    PebbleKit.registerReceivedNackHandler(this, pebbleNackReceiver);
  }

  public void setToken(View view) {
    access_token = edt_txt.getText().toString();
    Intent homeIntent= new Intent(Intent.ACTION_MAIN);
    homeIntent.addCategory(Intent.CATEGORY_HOME);
    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(homeIntent);
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);
    savedInstanceState.putString("token", access_token);

  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  public void sendChunks() {
    PebbleDictionary data = new PebbleDictionary();
    if(chunks == null || chunks.size() == 0) {
      QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qr_code,
              null,
              Contents.Type.TEXT,
              BarcodeFormat.QR_CODE.toString(),
              64);
      byte[] byteArray = null;
      try {
        Bitmap bmp = qrCodeEncoder.encodeAsBitmap();
        ImageView image = (ImageView) findViewById(R.id.image);
        image.setImageBitmap(bmp);
        String bits = qrCodeEncoder.encodeForPebble();

        chunks = new LinkedList<byte[]>();
        byteArray = BitZip.zip(bits);

        int c = byteArray.length;
        for (int i = 0; i < byteArray.length; i += 64) {
          chunks.add(Arrays.copyOfRange(byteArray, i, i+64));
        }
      } catch (WriterException e) {
        e.printStackTrace();
      }
      data.addBytes(0, chunks.pop());
    } else if (chunks.size() != 1) {
      data.addBytes(0, chunks.pop());
    } else {
      data.addBytes(1, chunks.pop());
      chunks = null;
    }
    isRunning = false;
    PebbleKit.sendDataToPebble(getApplicationContext(), UUID.fromString("f5982123-b103-431b-a330-b6f035c7dcaf"), data);
  }

}
