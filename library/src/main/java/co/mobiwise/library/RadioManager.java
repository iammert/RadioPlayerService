package co.mobiwise.library;

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
public class RadioManager implements IRadioManager{

    private static boolean isLogging = false;

    private static RadioManager instance = null;

    private RadioPlayerService mService;

    private Context mContext;

    private List<RadioListener> mRadioListenerQueue;

    private boolean isServiceConnected;

    private RadioManager(Context mContext) {
        this.mContext = mContext;
        mRadioListenerQueue = new ArrayList<>();
        isServiceConnected = false;
    }

    public static RadioManager with(Context mContext){
        if(instance == null)
            instance = new RadioManager(mContext);
        return instance;
    }

    @Override
    public void startRadio(String streamURL) {
        mService.play(streamURL);
    }

    @Override
    public void stopRadio() {
        mService.stop();
    }

    @Override
    public boolean isPlaying() {
        log("IsPlaying : " + mService.isPlaying());
        return mService.isPlaying();
    }

    @Override
    public void registerListener(RadioListener mRadioListener) {
        if(isServiceConnected)
            mService.registerListener(mRadioListener);
        else
            mRadioListenerQueue.add(mRadioListener);
    }

    @Override
    public void unregisterListener(RadioListener mRadioListener) {
        log("Register unregistered.");
        mService.unregisterListener(mRadioListener);
    }

    @Override
    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    @Override
    public void connect() {
        log("Requested to connect service.");
        Intent intent = new Intent(mContext, RadioPlayerService.class);
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void disconnect() {
        log("Service Disconnected.");
        mContext.unbindService(mServiceConnection);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {

            log("Service Connected.");

            mService = ((RadioPlayerService.LocalBinder) binder).getService();
            mService.setLogging(isLogging);
            isServiceConnected = true;

            if(!mRadioListenerQueue.isEmpty()){
                for (RadioListener mRadioListener : mRadioListenerQueue){
                    registerListener(mRadioListener);

                    mRadioListener.onRadioConnected();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }

    };

    private void log(String log){
        if(isLogging)
            Log.v("RadioManager","RadioManagerLog : " + log);
    }

    @Override
    public void startNotification(String radioName, String trackInformation, Bitmap bitmapIcon) {
        mService.buildNotification(radioName, trackInformation, bitmapIcon);
    }

    @Override
    public void updateNotificationMetadata(String radioName, String trackInformation, Bitmap bitmapIcon) {
        mService.updateNotificationMetadata(radioName, trackInformation, bitmapIcon);
    }
}
