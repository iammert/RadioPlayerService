package co.mobiwise.library.radio;


import android.graphics.Bitmap;

/**
 * Created by mertsimsek on 03/07/15.
 */
public interface IRadioManager {

    void startRadio(String streamURL);

    void stopRadio();

    boolean isPlaying();

    void registerListener(RadioListener mRadioListener);

    void unregisterListener(RadioListener mRadioListener);

    void setLogging(boolean logging);

    void connect();

    void disconnect();

    void updateNotification(String singerName, String songName);

    void updateNotification(String singerName, String songName, int smallArt);

    void updateNotification(String singerName, String songName, Bitmap bigArt);

    void updateNotification(String singerName, String songName, int smallArt, int bigArt);

    void updateNotification(String singerName, String songName, int smallArt, Bitmap bigArt);

    void enableNotification(boolean isEnabled);

    void cancelNotification();
}
