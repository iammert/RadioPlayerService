package co.mobiwise.library.radio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mertsimsek on 03/07/15.
 */
public class RadioManager implements IRadioManager {

  /**
   * Logging enable/disable
   */
  private static boolean isLogging = false;

  /**
   * Singleton
   */
  private static RadioManager instance = null;

  /**
   * RadioPlayerService
   */
  private static RadioPlayerService mService;

  /**
   * Context
   */
  private Context mContext;

  /**
   * Listeners
   */
  private List<RadioListener> mRadioListenerQueue;

  /**
   * Service connected/Disconnected lock
   */
  private boolean isServiceConnected;

  /**
   * notification enabled/disabled control
   */
  private boolean isEnabled = true;
  /**
   * Connection
   */
  private ServiceConnection mServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName arg0, IBinder binder) {

      log("Service Connected.");

      mService = ((RadioPlayerService.LocalBinder) binder).getService();
      mService.setLogging(isLogging);
      isServiceConnected = true;
      mService.enableNotification(isEnabled);

      if (!mRadioListenerQueue.isEmpty()) {
        for (RadioListener mRadioListener : mRadioListenerQueue) {
          registerListener(mRadioListener);
          mRadioListener.onRadioConnected();
        }
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
    }
  };

  /**
   * Private constructor because of Singleton pattern
   *
   * @param mContext
   */
  private RadioManager(Context mContext) {
    this.mContext = mContext;
    mRadioListenerQueue = new ArrayList<>();
    isServiceConnected = false;
  }

  /**
   * Singleton
   *
   * @param mContext
   * @return
   */
  public static RadioManager with(Context mContext) {
    if (instance == null)
      instance = new RadioManager(mContext);
    return instance;
  }

  /**
   * get current service instance
   *
   * @return RadioPlayerService
   */
  public static RadioPlayerService getService() {
    return mService;
  }
  
  /**
   * Check if we are currectnly connected/bind to a service
   */
  public boolean isConnected(){
    return isServiceConnected;
  }
  
  /**
   * Resets the instance to 0 and requires re-initialization using with()
   */
  public static void flush(){
    instance = null;
  }

  /**
   * Start Radio Streaming
   *
   * @param streamURL
   */
  @Override
  public void startRadio(String streamURL) {
    mService.play(streamURL);
  }

  /**
   * Stop Radio Streaming
   */
  @Override
  public void stopRadio() {
    mService.stop();
  }

  /**
   * Check if radio is playing
   *
   * @return
   */
  @Override
  public boolean isPlaying() {
    log("IsPlaying : " + mService.isPlaying());
    return mService.isPlaying();
  }

  /**
   * Register listener to listen radio service actions
   *
   * @param mRadioListener
   */
  @Override
  public void registerListener(RadioListener mRadioListener) {
    if (isServiceConnected)
      mService.registerListener(mRadioListener);
    else
      mRadioListenerQueue.add(mRadioListener);
  }

  /**
   * Unregister listeners
   *
   * @param mRadioListener
   */
  @Override
  public void unregisterListener(RadioListener mRadioListener) {
    log("Register unregistered.");
    mService.unregisterListener(mRadioListener);
  }

  /**
   * Set/Unset Logging
   *
   * @param logging
   */
  @Override
  public void setLogging(boolean logging) {
    isLogging = logging;
  }

  /**
   * Connect radio player service
   */
  @Override
  public void connect() {
    log("Requested to connect service.");
    Intent intent = new Intent(mContext, RadioPlayerService.class);
    mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
  }

  /**
   * Disconnect radio player service
   */
  @Override
  public void disconnect() {
    log("Service Disconnected.");
    mContext.unbindService(mServiceConnection);
  }

  /**
   * Update notification data
   *
   * @param singerName
   * @param songName
   * @param smallArt
   * @param bigArt
   */
  @Override
  public void updateNotification(String singerName, String songName, int smallArt, int bigArt) {
    if (mService != null && isEnabled)
      mService.updateNotification(singerName, songName, smallArt, bigArt);
  }

  /**
   * Update notification data
   *
   * @param singerName
   * @param songName
   * @param smallArt
   * @param bigArt
   */
  @Override
  public void updateNotification(String singerName, String songName, int smallArt, Bitmap bigArt) {
    if (mService != null && isEnabled)
      mService.updateNotification(singerName, songName, smallArt, bigArt);
  }

  @Override
  public void enableNotification(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  /**
   * Logger
   *
   * @param log
   */
  private void log(String log) {
    if (isLogging)
      Log.v("RadioManager", "RadioManagerLog : " + log);
  }

}
