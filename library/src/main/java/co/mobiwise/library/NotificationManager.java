package co.mobiwise.library;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

public class NotificationManager extends BroadcastReceiver {

  private static final int NOTIFICATION_ID = 222;
  private static final int REQUEST_CODE = 100;

  public static final String ACTION_CANCEL = "co.mobiwise.library.cancel";
  public static final String ACTION_PLAY_PAUSE = "co.mobiwise.library.play_pause";
  public static final String ACTION_PREV = "co.mobiwise.library.prev";
  public static final String ACTION_NEXT = "co.mobiwise.library.next";

  private final RadioPlayerService mService;

  private final android.app.NotificationManager mNotificationManager;

  private PendingIntent mCancelIntent;
  private PendingIntent mPlayIntent;
  private PendingIntent mPreviousIntent;
  private PendingIntent mNextIntent;

  private RemoteViews mNotificationTemplate;

  private RemoteViews mExpandedView;

  private boolean mStarted = false;
  private Notification mNotification;

  public NotificationManager(RadioPlayerService service) {
    mService = service;

    mNotificationManager = (android.app.NotificationManager) mService
        .getSystemService(Context.NOTIFICATION_SERVICE);
  }

  public void startNotification(String radioName, String trackInformation, Bitmap bitmapIcon) {
    if (!mStarted) {
      mNotification = createNotification(radioName, trackInformation, bitmapIcon);
      if (mNotification != null) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_PREV);
        filter.addAction(ACTION_CANCEL);
        mService.registerReceiver(this, filter);

        mService.startForeground(NOTIFICATION_ID, mNotification);
        mStarted = true;
      }
    }
  }

  /**
   * Removes the notification and stops tracking service
   */
  public void stopNotification() {
    if (mStarted) {
      mStarted = false;
      try {
        mNotificationManager.cancel(NOTIFICATION_ID);
        mService.unregisterReceiver(this);
      } catch (IllegalArgumentException ex) {
        // ignore if the receiver is not registered.
      }
      mService.stopForeground(true);
      mService.stop();
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    final String action = intent.getAction();
    switch (action) {
      case ACTION_CANCEL:
        stopNotification();
        break;
      case ACTION_PLAY_PAUSE:
        boolean isPlaying = mService.isPlaying();
        if (isPlaying) {
          mService.stop();
        } else {
          mService.playFromNotification();
        }
        setPlayPauseActionView(mService.isPlaying());
        break;
      case ACTION_NEXT:
        Log.v("TAG", "ACTION_NEXT");
        break;
      case ACTION_PREV:
        Log.v("TAG", "ACTION_PREV");
        break;
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private Notification createNotification(String radioName, String
      trackInformation, Bitmap bitmapIcon) {
    // Default Notification layout
    mNotificationTemplate = new RemoteViews(mService.getPackageName(),
        R.layout.notification);

    Notification.Builder notificationBuilder = new Notification.Builder(mService);

    mNotificationTemplate.setTextViewText(R.id.notification_line_one,
        radioName);
    mNotificationTemplate.setTextViewText(R.id.notification_line_two,
        trackInformation);
    mNotificationTemplate.setImageViewBitmap(R.id.notification_image, bitmapIcon);

    // Notification Builder
    Notification notification =
        notificationBuilder.
            setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentIntent(getPendingIntent())
            .setPriority(Notification.PRIORITY_DEFAULT).setContent(mNotificationTemplate)
            .build();

    setPlaybackActions();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      // Expanded Notification style
      mExpandedView = new RemoteViews(mService.getPackageName(),
          R.layout.notification_expanded);
      notification.bigContentView = mExpandedView;
      mExpandedView.setTextViewText(R.id.notification_line_one,
          radioName);
      mExpandedView.setTextViewText(R.id.notification_line_two,
          trackInformation);
      mExpandedView.setImageViewBitmap(R.id.notification_image,
          bitmapIcon);
      setExpandedPlaybackActions();
    }
    return notification;
  }


  private void setExpandedPlaybackActions() {
    String pkg = mService.getPackageName();

    mCancelIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
        new Intent(ACTION_CANCEL).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    mExpandedView.setOnClickPendingIntent(R.id
        .notification_collapse, mCancelIntent);

    mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
        new Intent(ACTION_PLAY_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play,
        mPlayIntent);

    mPreviousIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
        new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_previous,
        mPreviousIntent);

    mNextIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
        new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_next,
        mNextIntent);

    // Cancel all notifications to handle the case where the Service was killed and
    // restarted by the system.
    mNotificationManager.cancelAll();
  }

  private void setPlaybackActions() {
    String pkg = mService.getPackageName();

    mCancelIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
        new Intent(ACTION_CANCEL).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    mNotificationTemplate.setOnClickPendingIntent(R.id.notification_collapse, mCancelIntent);

    mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
        new Intent(ACTION_PLAY_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, mPlayIntent);

    // Cancel all notifications to handle the case where the Service was killed and
    // restarted by the system.
    mNotificationManager.cancelAll();
  }

  private PendingIntent getPendingIntent() {
    return PendingIntent.getActivity(mService, 0, new Intent(mService.getPackageName())
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
  }

  /**
   * Changes the playback controls in and out of a paused state
   */
  public void setPlayPauseActionView(final boolean isPlaying) {
    if (mNotificationTemplate == null || mNotificationManager == null) {
      return;
    }
    mNotificationTemplate.setImageViewResource(R.id.notification_play,
        isPlaying ? R.drawable.btn_playback_play : R.drawable.btn_playback_pause);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      mExpandedView.setImageViewResource(R.id.notification_expanded_play,
          isPlaying ? R.drawable.btn_playback_play : R.drawable.btn_playback_pause);
    }
    mNotificationManager.notify(NOTIFICATION_ID, mNotification);
  }

  public void updateNotificationMediaData(String radioName, String
      trackInformation, Bitmap bitmapIcon) {
    if (mNotificationTemplate == null || mNotificationManager == null) {
      return;
    }
    mNotificationTemplate.setTextViewText(R.id.notification_line_one,
        radioName);
    mNotificationTemplate.setTextViewText(R.id.notification_line_two,
        trackInformation);
    mNotificationTemplate.setImageViewBitmap(R.id.notification_image, bitmapIcon);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      mExpandedView.setTextViewText(R.id.notification_line_one,
          radioName);
      mExpandedView.setTextViewText(R.id.notification_line_two,
          trackInformation);
      mExpandedView.setImageViewBitmap(R.id.notification_image,
          bitmapIcon);
    }
    mNotificationManager.notify(NOTIFICATION_ID, mNotification);
  }

}