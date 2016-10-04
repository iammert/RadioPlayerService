package co.mobiwise.library.media;

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
 * Created by mertsimsek on 04/11/15.
 */
public class MediaManager {

  /**
   * Context
   */
  private Context mContext;

  /**
   * MediaPlayerService
   */
  private static MediaPlayerService mService;

  /**
   * Boolean check for service connection
   */
  private boolean isServiceConnected = false;

  /**
   * Listeners wil lbe added here if
   * service is not connected yet.
   * After service connected, we will empty this queue
   * by adding them to service listeners list.
   */
  private List<MediaListener> mMediaListenerQueue;

  /**
   * MediaManager
   */
  private static MediaManager instance = null;

  /**
   * if play requested before service connection, we set this value as true.
   */
  private boolean isPlayRequested = false;

  /**
   * current stream URL
   */
  private String mStreamURL;

  /**
   * private construtor
   */
  private MediaManager(Context mContext) {
    this.mContext = mContext;
    mMediaListenerQueue = new ArrayList<>();
  }

  /**
   * Singleton instance
   *
   * @return
   */
  public static MediaManager with(Context context) {
    if (instance == null)
      instance = new MediaManager(context);
    return instance;
  }

  public static MediaPlayerService getService() {
    return mService;
  }

  public void play(String mStreamURL) {
    this.mStreamURL = mStreamURL;
    if (isServiceConnected) {
      mService.play(mStreamURL);
    } else {
      isPlayRequested = true;
    }
  }

  public void pause() {
    if (isServiceConnected)
      mService.pause();
  }

  public void resume(){
    if(isServiceConnected)
      mService.resume();
  }

  public void seekTo(int duration) {
    if (isServiceConnected)
      mService.seekTo(duration);
  }

  public boolean isPlaying() {
    if (isServiceConnected)
      return mService.isPlaying();
    return false;
  }

  public void updateNotification(String singerName, String songName, int smallArt, int bigArt) {
    if (mService != null)
      mService.updateNotification(singerName, songName, smallArt, bigArt);
  }

  public void updateNotification(String singerName, String songName, int smallArt, Bitmap bigArt) {
    if (mService != null)
      mService.updateNotification(singerName, songName, smallArt, bigArt);
  }

  public void registerListener(MediaListener mediaListener) {
    if (isServiceConnected)
      mService.registerMediaListener(mediaListener);
    else
      mMediaListenerQueue.add(mediaListener);
  }

  /**
   * Connect service
   */
  public void connect() {
    Intent intent = new Intent(mContext, MediaPlayerService.class);
    mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
  }

  /**
   * Disconnect service
   */
  public void disconnect() {
    mContext.unbindService(mServiceConnection);
  }

  /**
   * Connection
   */
  private ServiceConnection mServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName arg0, IBinder binder) {

      Log.v("TEST", "SERVICE CONNECTED.");
      mService = ((MediaPlayerService.LocalBinder) binder).getService();
      isServiceConnected = true;
      if (isPlayRequested) {
        play(mStreamURL);
        isPlayRequested = false;
      }

      if (!mMediaListenerQueue.isEmpty()) {
        for (MediaListener mediaListener : mMediaListenerQueue)
          registerListener(mediaListener);
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
    }
  };


}
