package co.mobiwise.library;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mertsimsek on 03/07/15.
 */
public class RadioManager implements IRadioManager{

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
        mService.unregisterListener(mRadioListener);
    }

    @Override
    public void connect() {
        Intent intent = new Intent(mContext, RadioPlayerService.class);
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void disconnect() {
        mContext.unbindService(mServiceConnection);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            mService = ((RadioPlayerService.LocalBinder) binder).getService();
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
}
