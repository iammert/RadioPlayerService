package co.mobiwise.library.media;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import co.mobiwise.library.R;

/**
 * Created by mertsimsek on 04/11/15.
 */
public class MediaPlayerService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnCompletionListener{


    /**
     * Notification ID
     */
    private static final int NOTIFICATION_ID = 001;

    /**
     * PLAY/PAUSE intent and OPENPLAYER intent strings
     */
    public static final String NOTIFICATION_INTENT_PLAY_PAUSE = "co.mobiwise.library.notification.media.INTENT_PLAYPAUSE";

    public static final String NOTIFICATION_INTENT_CANCEL = "co.mobiwise.library.notification.media.INTENT_CANCEL";

    public static final String NOTIFICATION_INTENT_OPEN_PLAYER = "co.mobiwise.library.notification.media.INTENT_OPENPLAYER";

    /**
     * Notification current values
     */
    private String singerName = "";
    private String songName = "";
    private int smallImage = R.drawable.default_art;
    private Bitmap artImage;

    /**
     * Player State Enum
     */
    public enum State {
        IDLE,
        PLAYING,
        STOPPED,
        PAUSED
    }

    /**
     * Media listeners
     */
    List<MediaListener> mediaListenerList;

    /**
     * MediaPlayer State
     */
    private State mState = State.IDLE;

    /**
     * Music stream URL;
     */
    private String mStreamURL = "";

    /**
     * Notification manager
     */
    private NotificationManager mNotificationManager;

    /**
     * Stop action. If another radioplayer will start.It needs
     * to send broadcast to stop this service.
     */
    public static final String ACTION_RADIOPLAYER_STOP = "co.mobiwise.library.ACTION_STOP_RADIOPLAYER";

    /**
     * Media Player
     */
    private MediaPlayer mediaPlayer;

    /**
     * Binder
     */
    public final IBinder mLocalBinder = new LocalBinder();

    /**
     * Binder
     */
    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    /**
     * OnBind
     *
     * @param intent
     * @return mLocalBinder
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    /**
     * Service start call
     *
     * @param intent
     * @param flags
     * @param startId
     * @return Flag
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();

        if(action.equals(NOTIFICATION_INTENT_CANCEL)){
            if(isPlaying())
                stop();
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
        else if(action.equals(NOTIFICATION_INTENT_PLAY_PAUSE)){
            if(isPlaying())
                pause();
            else
                play(mStreamURL);
        }


        return START_NOT_STICKY;
    }

    /**
     * OnCreate
     */
    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mediaListenerList = new ArrayList<>();
        initializeMediaPlayer();
    }

    /**
     * Creates media player if it is null
     *
     * @return media player
     */
    private MediaPlayer initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
            mediaPlayer.setOnCompletionListener(this);
        }

        return mediaPlayer;
    }

    /**
     * Play music streaming
     *
     * @param mStreamURL
     */
    public void play(String mStreamURL) {

        sendBroadcast(new Intent(ACTION_RADIOPLAYER_STOP));

        notifyPlayerLoading();

        if (mState == State.PAUSED && this.mStreamURL.equals(mStreamURL)) {
            mediaPlayer.start();
            mState = State.PLAYING;
            notifyPlayerStarted(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition());
        } else {
            try {
                resetMediaPlayer();
                this.mStreamURL = mStreamURL;
                mediaPlayer.setDataSource(mStreamURL);
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void resume(){
        if(mStreamURL != null)
            play(mStreamURL);
    }

    public void stopFromNotification(){
        if(mNotificationManager != null) mNotificationManager.cancelAll();
        stop();
    }

    /**
     * Stop music streaming
     */
    public void stop() {
        mediaPlayer.stop();
        mState = State.STOPPED;
        notifyPlayerStopped();
    }

    /**
     * Pause music streaming
     */
    public void pause() {
        mediaPlayer.pause();
        mState = State.PAUSED;
        buildNotification();
        notifyPlayerStopped();
    }

    /**
     * Seek to specific position
     *
     * @param duration
     */
    public void seekTo(int duration) {
        notifyPlayerLoading();
        mediaPlayer.seekTo(duration * 1000);
    }

    /**
     * Reset mediaplayer
     */
    private void resetMediaPlayer() {
        mediaPlayer.reset();
        mState = State.IDLE;
    }

    /**
     * Called when mediaplayer prepared to play
     * @param mp
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        mState = State.PLAYING;
        mp.start();
        notifyPlayerStarted(mp.getDuration(), mp.getCurrentPosition());
    }

    /**
     * Seek completed
     * @param mp
     */
    @Override
    public void onSeekComplete(MediaPlayer mp) {
        mp.start();
        notifyPlayerStarted(mp.getDuration(), mp.getCurrentPosition());
    }

    /**
     * End of file
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        mState = State.STOPPED;
        mp.stop();
        mp.reset();
    }

    public boolean isPlaying() {
        return mState == State.PLAYING;
    }

    /**
     * REGISTERING AND NOTIFYING LISTENERS
     */
    public void registerMediaListener(MediaListener mediaListener) {
        mediaListenerList.add(mediaListener);
    }

    public void unregisterMediaListener(MediaListener mediaListener) {
        if (mediaListenerList.contains(mediaListener))
            mediaListenerList.remove(mediaListener);
    }

    private void notifyPlayerStarted(int totalDuration, int currentDuration) {
        for (MediaListener mediaListener : mediaListenerList)
            mediaListener.onMediaStarted(totalDuration, currentDuration);
        buildNotification();
    }

    private void notifyPlayerStopped() {
        for (MediaListener mediaListener : mediaListenerList)
            mediaListener.onMediaStopped();
    }

    private void notifyPlayerLoading() {
        for (MediaListener mediaListener : mediaListenerList)
            mediaListener.onMediaLoading();
    }

    /**
     * Build notification
     */
    private void buildNotification(){

        /**
         * Intents
         */
        Intent intentPlayPause = new Intent(NOTIFICATION_INTENT_PLAY_PAUSE);
        Intent intentOpenPlayer = new Intent(NOTIFICATION_INTENT_OPEN_PLAYER);
        Intent intentCancel = new Intent(NOTIFICATION_INTENT_CANCEL);

        /**
         * Pending intents
         */
        PendingIntent playPausePending = PendingIntent.getBroadcast(this,100,intentPlayPause,0);
        PendingIntent openPending = PendingIntent.getBroadcast(this,101,intentOpenPlayer,0);
        PendingIntent cancelPending = PendingIntent.getBroadcast(this,102,intentCancel,0);

        /**
         * Remote view for normal view
         */

        RemoteViews mNotificationTemplate = new RemoteViews(this.getPackageName(), R.layout.notification);
        Notification.Builder notificationBuilder = new Notification.Builder(this);

        /**
         * set small notification texts and image
         */
        if(artImage == null)
            artImage = BitmapFactory.decodeResource(getResources(),R.drawable.default_art);

        mNotificationTemplate.setTextViewText(R.id.notification_line_one, singerName);
        mNotificationTemplate.setTextViewText(R.id.notification_line_two, songName);
        mNotificationTemplate.setImageViewResource(R.id.notification_play, isPlaying() ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
        mNotificationTemplate.setImageViewBitmap(R.id.notification_image, artImage);

        /**
         * OnClickPending intent for collapsed notification
         */
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_collapse, cancelPending);
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, playPausePending);


        /**
         * Create notification instance
         */
        Notification notification = notificationBuilder
                .setSmallIcon(smallImage)
                .setContentIntent(openPending)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContent(mNotificationTemplate)
                .setUsesChronometer(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .build();
        notification.contentView = mNotificationTemplate;
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        notification.icon = R.drawable.default_art;
        startForeground(NOTIFICATION_ID, notification);

        /**
         * Expanded notification
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            RemoteViews mExpandedView = new RemoteViews(this.getPackageName(),R.layout.notification_expanded);

            mExpandedView.setTextViewText(R.id.notification_line_one, singerName);
            mExpandedView.setTextViewText(R.id.notification_line_two, songName);
            mExpandedView.setImageViewResource(R.id.notification_expanded_play, isPlaying() ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
            mExpandedView.setImageViewBitmap(R.id.notification_image, artImage);

            mExpandedView.setOnClickPendingIntent(R.id.notification_collapse, cancelPending);
            mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play, playPausePending);

            notification.bigContentView = mExpandedView;
        }

        if(mNotificationManager != null)
            mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void updateNotification(String singerName, String songName, int smallImage, int artImage){
        this.singerName = singerName;
        this.songName = songName;
        this.smallImage = smallImage;
        this.artImage = BitmapFactory.decodeResource(getResources(),artImage);
        buildNotification();
    }

    public void updateNotification(String singerName, String songName, int smallImage, Bitmap artImage){
        this.singerName = singerName;
        this.songName = songName;
        this.smallImage = smallImage;
        this.artImage = artImage;
        buildNotification();
    }
}
