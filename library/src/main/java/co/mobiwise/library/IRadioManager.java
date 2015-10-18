package co.mobiwise.library;

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

    void updateNotification(String textHeader, String textSub, int artImage);

    void enableNotification(boolean isEnabled);

}
