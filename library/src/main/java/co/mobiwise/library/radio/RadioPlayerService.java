package co.mobiwise.library.radio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataDecoder;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;

import co.mobiwise.library.R;


/**
 * Created by mertsimsek on 01/07/15.
 */
public class RadioPlayerService extends Service implements ExoPlayer.EventListener {

    /**
     * Notification action intent strings
     */
    public static final String NOTIFICATION_INTENT_PLAY_PAUSE = "co.mobiwise.library.notification.radio.INTENT_PLAYPAUSE";

    public static final String NOTIFICATION_INTENT_CANCEL = "co.mobiwise.library.notification.radio.INTENT_CANCEL";

    public static final String NOTIFICATION_INTENT_OPEN_PLAYER = "co.mobiwise.library.notification.radio.INTENT_OPENPLAYER";

    /**
     * Notification current values
     */
    private String singerName = "";
    private String songName = "";
    private int smallImage = R.drawable.default_art;
    private Bitmap artImage;

    /**
     * Notification ID
     */
    private static final int NOTIFICATION_ID = 001;

    /**
     * Logging control variable
     */
    private static boolean isLogging = false;

    /**
     * Stream url suffix
     */
    private final String SUFFIX_PLS = ".pls";
    private final String SUFFIX_RAM = ".ram";
    private final String SUFFIX_WAX = ".wax";

    /**
     * State enum for Radio Player state (IDLE, PLAYING, STOPPED, INTERRUPTED)
     */
    public enum State {
        IDLE,
        PLAYING,
        STOPPED,
    }

    List<RadioListener> mListenerList;

    /**
     * Radio State
     */
    private State mRadioState;

    /**
     * Current radio URL
     */
    private String mRadioUrl;

    /**
     * Stop action. If another mediaplayer will start.It needs
     * to send broadcast to stop this service.
     */
    public static final String ACTION_MEDIAPLAYER_STOP = "co.mobiwise.library.ACTION_STOP_MEDIAPLAYER";


    /**
     * Exo player properties
     */
    private SimpleExoPlayer radioPlayer;
    private Handler mainHandler;
    private BandwidthMeter bandwidthMeter;
    private TrackSelection.Factory defaultTrackSelectionFactory;
    private TrackSelector trackSelector;
    private LoadControl loadControl;
    private DataSource.Factory dataSourceFactory;

    /**
     * Will be controlled on incoming calls and stop and start player.
     */
    private TelephonyManager mTelephonyManager;

    /**
     * If closed from notification, it will be checked
     * on Stop method and notification will not be created
     */
    private boolean isClosedFromNotification = false;

    /**
     * Incoming calls interrupt radio if it is playing.
     * Check if this is true or not after hang up;
     */
    private boolean isInterrupted;

    /**
     * Notification manager
     */
    private NotificationManager mNotificationManager;

    /**
     * Binder
     */
    public final IBinder mLocalBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    /**
     * Binder
     */
    public class LocalBinder extends Binder {
        public RadioPlayerService getService() {
            return RadioPlayerService.this;
        }
    }

    /**
     * Service called
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();

        /**
         * If cancel clicked on notification, then set state to
         * IDLE, stop player and cancel notification
         */
        if (action.equals(NOTIFICATION_INTENT_CANCEL)) {
            if (isPlaying()) {
                isClosedFromNotification = true;
                stop();
            }
            if (mNotificationManager != null)
                mNotificationManager.cancel(NOTIFICATION_ID);
        }
        /**
         * If play/pause action clicked on notification,
         * Check player state and stop/play streaming.
         */
        else if (action.equals(NOTIFICATION_INTENT_PLAY_PAUSE)) {
            if (isPlaying())
                stop();
            else if (mRadioUrl != null)
                play(mRadioUrl);

        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mListenerList = new ArrayList<>();

        mRadioState = State.IDLE;
        isInterrupted = false;
        getPlayer();

        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (mTelephonyManager != null)
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * Play url if different from previous streaming url.
     *
     * @param mRadioUrl
     */
    public void play(String mRadioUrl) {
        MediaSource mediaSource = new HlsMediaSource(Uri.parse(mRadioUrl), dataSourceFactory, mainHandler, null);
        radioPlayer.setPlayWhenReady(false);
        radioPlayer.prepare(mediaSource);

        sendBroadcast(new Intent(ACTION_MEDIAPLAYER_STOP));

        notifyRadioLoading();

        if (checkSuffix(mRadioUrl))
            decodeStremLink(mRadioUrl);
        else {
            this.mRadioUrl = mRadioUrl;
            radioPlayer.setPlayWhenReady(true);
        }
    }

    public void stop() {
        radioPlayer.setPlayWhenReady(false);
        mRadioState = State.STOPPED;

        /**
         * If player stopped from notification then dont
         * call buildNotification().
         */
        if (!isClosedFromNotification)
            buildNotification();
        else
            isClosedFromNotification = false;

        notifyRadioStopped();
    }

    public boolean isPlaying() {
        if (State.PLAYING == mRadioState)
            return true;
        return false;
    }

    public void resume() {
        if (mRadioUrl != null)
            play(mRadioUrl);
    }

    public void stopFromNotification() {
        isClosedFromNotification = true;
        if (mNotificationManager != null) mNotificationManager.cancelAll();
        stop();
    }

    public void registerListener(RadioListener mListener) {
        mListenerList.add(mListener);
    }

    public void unregisterListener(RadioListener mListener) {
        mListenerList.remove(mListener);
    }

    private void notifyRadioStarted() {
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onRadioStarted();
        }
    }

    private void notifyRadioStopped() {
        for (RadioListener mRadioListener : mListenerList)
            mRadioListener.onRadioStopped();
    }

    private void notifyMetaDataChanged(String s, String s2) {
        for (RadioListener mRadioListener : mListenerList)
            mRadioListener.onMetaDataReceived(s, s2);
    }

    private void notifyRadioLoading() {
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onRadioLoading();
        }
    }

    private void notifyErrorOccured() {
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onError();
        }
    }

    /**
     * Return AAC player. If it is not initialized, creates and returns.
     *
     * @return MultiPlayer
     */
    private SimpleExoPlayer getPlayer() {

        if (radioPlayer == null) {
            mainHandler = new Handler();
            bandwidthMeter = new DefaultBandwidthMeter();
            defaultTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
            trackSelector = new DefaultTrackSelector(defaultTrackSelectionFactory);
            loadControl = new DefaultLoadControl();
            radioPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
            dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "mediaPlayerSample"));
            radioPlayer.addListener(this);
        }

        return radioPlayer;
    }

    PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {

                /**
                 * Stop radio and set interrupted if it is playing on incoming call.
                 */
                if (isPlaying()) {
                    isInterrupted = true;
                    stop();
                }

            } else if (state == TelephonyManager.CALL_STATE_IDLE) {

                /**
                 * Keep playing if it is interrupted.
                 */
                if (isInterrupted)
                    play(mRadioUrl);

            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {

                /**
                 * Stop radio and set interrupted if it is playing on outgoing call.
                 */
                if (isPlaying()) {
                    isInterrupted = true;
                    stop();
                }

            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    /**
     * Check supported suffix
     *
     * @param streamUrl
     * @return
     */
    public boolean checkSuffix(String streamUrl) {
        if (streamUrl.contains(SUFFIX_PLS) ||
                streamUrl.contains(SUFFIX_RAM) ||
                streamUrl.contains(SUFFIX_WAX))
            return true;
        else
            return false;
    }

    /**
     * Enable/Disable log
     *
     * @param logging
     */
    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    /**
     * Logger
     *
     * @param log
     */
    private void log(String log) {
        if (isLogging)
            Log.v("RadioManager", "RadioPlayerService : " + log);
    }

    /**
     * If stream link is a file, then we
     * call stream decoder to get HTTP stream link
     * from that file.
     *
     * @param streamLink
     */
    private void decodeStremLink(String streamLink) {
        new StreamLinkDecoder(streamLink) {
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                play(s);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Build notification
     */
    private void buildNotification() {

        /**
         * Intents
         */
        Intent intentPlayPause = new Intent(NOTIFICATION_INTENT_PLAY_PAUSE);
        Intent intentOpenPlayer = new Intent(NOTIFICATION_INTENT_OPEN_PLAYER);
        Intent intentCancel = new Intent(NOTIFICATION_INTENT_CANCEL);

        /**
         * Pending intents
         */
        PendingIntent playPausePending = PendingIntent.getBroadcast(this, 23, intentPlayPause, 0);
        PendingIntent openPending = PendingIntent.getBroadcast(this, 31, intentOpenPlayer, 0);
        PendingIntent cancelPending = PendingIntent.getBroadcast(this, 12, intentCancel, 0);

        /**
         * Remote view for normal view
         */

        RemoteViews mNotificationTemplate = new RemoteViews(this.getPackageName(), R.layout.notification);
        Notification.Builder notificationBuilder = new Notification.Builder(this);

        /**
         * set small notification texts and image
         */
        if (artImage == null)
            artImage = BitmapFactory.decodeResource(getResources(), R.drawable.default_art);

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
                .build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        /**
         * Expanded notification
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            RemoteViews mExpandedView = new RemoteViews(this.getPackageName(), R.layout.notification_expanded);

            mExpandedView.setTextViewText(R.id.notification_line_one, singerName);
            mExpandedView.setTextViewText(R.id.notification_line_two, songName);
            mExpandedView.setImageViewResource(R.id.notification_expanded_play, isPlaying() ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
            mExpandedView.setImageViewBitmap(R.id.notification_image, artImage);

            mExpandedView.setOnClickPendingIntent(R.id.notification_collapse, cancelPending);
            mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play, playPausePending);

            notification.bigContentView = mExpandedView;
        }

        if (mNotificationManager != null)
            mNotificationManager.notify(NOTIFICATION_ID, notification);

    }

    public void updateNotification(String singerName, String songName, int smallImage, int artImage) {
        this.singerName = singerName;
        this.songName = songName;
        this.smallImage = smallImage;
        this.artImage = BitmapFactory.decodeResource(getResources(), artImage);
        buildNotification();
    }


    public void updateNotification(String singerName, String songName, int smallImage, Bitmap artImage) {
        this.singerName = singerName;
        this.songName = songName;
        this.smallImage = smallImage;
        this.artImage = artImage;
        buildNotification();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        for (int i = 0; i < trackGroups.length; i++) {
            TrackGroup trackGroup = trackGroups.get(i);
            for (int j = 0; j < trackGroup.length; j++) {
                Metadata trackMetadata = trackGroup.getFormat(j).metadata;
                if (trackMetadata != null) {
                }
            }
        }

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        if(playbackState == ExoPlayer.STATE_READY){

            mRadioState = State.PLAYING;
            buildNotification();
            notifyRadioStarted();

            if (isInterrupted)
                isInterrupted = false;
        }

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.v("TEST", "ERRORRR : " + error.getMessage());
    }

    @Override
    public void onPositionDiscontinuity() {

    }


}
