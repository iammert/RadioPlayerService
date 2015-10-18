package co.mobiwise.library;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;


import com.spoledge.aacdecoder.IcyURLStreamHandler;
import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by mertsimsek on 01/07/15.
 */
public class RadioPlayerService extends Service implements PlayerCallback {

    private static boolean isLogging = false;

    /**
     * Radio buffer and decode capacity(DEFAULT VALUES)
     */
    private final int AUDIO_BUFFER_CAPACITY_MS = 800;
    private final int AUDIO_DECODE_CAPACITY_MS = 400;

    /**
     * Listeners will be notified depends on these codes.
     */
    private final int NOTIFY_RADIO_STARTED = 0;
    private final int NOTIFY_RADIO_STOPPED = 1;
    private final int NOTIFY_METADATA_CHANGED = 2;

    /**
     * State enum for Radio Player state (IDLE, PLAYING, STOPPED, INTERRUPTED)
     */
    public enum State {
        IDLE,
        PLAYING,
        STOPPED,
    };

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
     * AAC Radio Player
     */
    private MultiPlayer mRadioPlayer;

    /**
     * Will be controlled on incoming calls and stop and start player.
     */
    private TelephonyManager mTelephonyManager;

    /**
     * While current radio playing, if you give another play command with different
     * source, you need to stop it first. This value is responsible for control
     * after radio stopped.
     */
    private boolean isSwitching;

    /**
     * Incoming calls interrupt radio if it is playing.
     * Check if this is true or not after hang up;
     */
    private boolean isInterrupted;

    /**
     * If play method is called repeatedly, AAC Decoder will be failed.
     * play and stop methods will be turned mLock = true when they called,
     * @onRadioStarted and @onRadioStopped methods will be release lock.
     */
    private boolean mLock;

    /**
     * Notification will be shown if this
     * value set true;
     */
    private boolean isNotificationEnabled = true;

    /**
     * Binder
     */
    public final IBinder mLocalBinder = new LocalBinder();

    /**
     * NotificationManager instance
     */
    private NotificationManager mNotificationManager;

    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    public class LocalBinder extends Binder {
        public RadioPlayerService getService(){
            return RadioPlayerService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mListenerList = new ArrayList<>();

        mRadioState = State.IDLE;
        isSwitching = false;
        isInterrupted = false;
        mLock = false;
        getPlayer();

        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mTelephonyManager != null) {
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        mNotificationManager = new NotificationManager(this);
    }

    /**
     * Play url if different from previous streaming url.
     * @param mRadioUrl
     */
    public void play(String mRadioUrl){

        this.mRadioUrl = mRadioUrl;
        isSwitching = false;

        if(isPlaying()){
            log("Switching Radio");
            isSwitching = true;
            stop();
        }
        else if(!mLock){
            log("Play requested.");
            mLock = true;
            getPlayer().playAsync(mRadioUrl);
        }

    }

    public void stop(){
        if(!mLock){
            log("Stop requested.");
            mLock = true;
            getPlayer().stop();
        }
    }

    @Override
    public void playerStarted() {
        mRadioState = State.PLAYING;
        mLock = false;
        notifyRadioStarted();

        log("Player started. State : " + mRadioState);

        if(isInterrupted)
            isInterrupted = false;

        if(isNotificationEnabled)
            updateNotificationMetadata("Playing","", R.drawable.default_art);
    }

    public boolean isPlaying(){
        if(State.PLAYING == mRadioState)
            return true;
        return false;
    }

    @Override
    public void playerPCMFeedBuffer(boolean b, int i, int i1) {
        //Empty
    }

    @Override
    public void playerStopped(int i) {
        mRadioState = State.STOPPED;
        mLock = false;
        notifyRadioStopped();
        log("Player started. State : " + mRadioState);

        if(isSwitching)
            play(mRadioUrl);


    }

    @Override
    public void playerException(Throwable throwable) {
        log("ERROR OCCURED.");
        //Empty
    }

    @Override
    public void playerMetadata(String s, String s2) {
        notifyMetaDataChanged(s, s2);
    }

    @Override
    public void playerAudioTrackCreated(AudioTrack audioTrack) {
        //Empty
    }

    public void registerListener(RadioListener mListener) {
        mListenerList.add(mListener);
    }

    public void unregisterListener(RadioListener mListener){
        mListenerList.remove(mListener);
    }

    private void notifyRadioStarted(){
        for (RadioListener mRadioListener : mListenerList){
            mRadioListener.onRadioStarted();
        }

    }

    private void notifyRadioStopped(){
        for (RadioListener mRadioListener : mListenerList)
            mRadioListener.onRadioStopped();
    }

    private void notifyMetaDataChanged(String s, String s2){

        for (RadioListener mRadioListener : mListenerList)
            mRadioListener.onMetaDataReceived(s, s2);
    }

    /**
     * Return AAC player. If it is not initialized, creates and returns.
     * @return MultiPlayer
     */
    private MultiPlayer getPlayer() {
        try {

            java.net.URL.setURLStreamHandlerFactory( new java.net.URLStreamHandlerFactory(){

                public java.net.URLStreamHandler createURLStreamHandler( String protocol ) {
                    Log.d("LOG", "Asking for stream handler for protocol: '" + protocol + "'");
                    if ("icy".equals( protocol )) return new com.spoledge.aacdecoder.IcyURLStreamHandler();
                    return null;
                }
            });
        }
        catch (Throwable t) {
            Log.w( "LOG", "Cannot set the ICY URLStreamHandler - maybe already set ? - " + t );
        }

        if (mRadioPlayer == null) {
            mRadioPlayer = new MultiPlayer(this, AUDIO_BUFFER_CAPACITY_MS, AUDIO_DECODE_CAPACITY_MS);
            mRadioPlayer.setResponseCodeCheckEnabled(false);
        }
        return mRadioPlayer;
    }

    PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {

                /**
                 * Stop radio and set interrupted if it is playing on incoming call.
                 */
                if(isPlaying()){
                    isInterrupted = true;
                    stop();
                }

            } else if(state == TelephonyManager.CALL_STATE_IDLE) {

                /**
                 * Keep playing if it is interrupted.
                 */
                if(isInterrupted)
                    play(mRadioUrl);

            } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {

                /**
                 * Stop radio and set interrupted if it is playing on outgoing call.
                 */
                if(isPlaying()){
                    isInterrupted = true;
                    stop();
                }

            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    public void setLogging(boolean logging){
        isLogging = logging;
    }

    private void log(String log){
        if(isLogging)
            Log.v("RadioManager","RadioPlayerService : " + log);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNotificationManager.stopNotification();
    }

    /**
     * NOTIFICATION
     */

    /**
     * method that builds notification and shows track information also.
     */
    public void buildNotification(String radioName, String trackInformation, int artImage) {
        mNotificationManager.startNotification(radioName, trackInformation, artImage);
    }

    /**
     * method that controls music service from notification.
     */
    public void playFromNotification() {
        play(mRadioUrl);
    }

    /**
     * method that update notification and shows track information also.
     */
    public void updateNotificationMetadata(String radioName, String trackInformation, int artImage) {

        if(!mNotificationManager.isStarted())
            buildNotification(radioName, trackInformation, artImage);
        else
            mNotificationManager.updateNotificationMediaData(radioName, trackInformation, artImage);
    }

    /**
     * stop notification
     */
    public void stopNotification() {
        mNotificationManager.stopNotification();
    }

    public void enableNotification(boolean isEnabled){
        isNotificationEnabled = isEnabled;
    }
}
