package co.mobiwise.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import co.mobiwise.library.RadioListener;
import co.mobiwise.library.RadioManager;


public class MainActivity extends Activity implements RadioListener {

  /**
   * Example radio stream URL
   */
  private final String RADIO_URL = "http://radyoland.radyotvonline.com/radyoland/akustikland/icecast.audio";

  /**
   * Radio Manager initialization
   */
  RadioManager mRadioManager = RadioManager.with(this);

  Button mButtonControl;
  TextView mTextViewControl;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    /**
     * Register this class to manager. @onRadioStarted, @onRadioStopped and @onMetaDataReceived
     * Listeners will be notified.
     */
    mRadioManager.registerListener(this);

    /**
     * initialize layout widgets to play, pause radio.
     */
    initializeUI();


  }

  public void initializeUI() {
    mButtonControl = (Button) findViewById(R.id.buttonControl);
    mTextViewControl = (TextView) findViewById(R.id.textviewControl);

    mButtonControl.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (!mRadioManager.isPlaying()) {
          mRadioManager.startRadio(RADIO_URL);
          // mRadioManager.startNotification();
        } else {
          mRadioManager.stopRadio();
        }
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    /**
     * Remember to connect manager to start service.
     */
    mRadioManager.connect();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    /**
     * Remember to disconnect from manager.
     */
    mRadioManager.disconnect();
  }

  @Override
  public void onRadioConnected() {
    // Called when the service is connected, allowing, for example, for starting the stream as soon as possible
    // mRadioManager.startRadio(RADIO_URL);
  }

  @Override
  public void onRadioStarted() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        //TODO Do UI works here.
        mTextViewControl.setText("RADIO STATE : PLAYING...");
      }
    });
  }

  @Override
  public void onRadioStopped() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        //TODO Do UI works here
        mTextViewControl.setText("RADIO STATE : STOPPED.");
      }
    });
  }

  @Override
  public void onMetaDataReceived(String s, String s1) {
    //TODO Check metadata values. Singer name, song name or whatever you have.
    Log.v("TAG", "s :" + s);
    Log.v("TAG", "s1 :" + s1);
  }
}
