package co.mobiwise.library;

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

    void startNotification(String radioName, String trackInformation, Bitmap bitmapIcon);

    void updateNotificationMetadata(String radioName, String trackInformation, Bitmap bitmapIcon);
}
