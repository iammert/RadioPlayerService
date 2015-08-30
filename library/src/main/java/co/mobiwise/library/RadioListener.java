package co.mobiwise.library;

import java.util.ArrayList;

/**
 * Created by mertsimsek on 01/07/15.
 */
public interface RadioListener {

    void onRadioConnected();

    void onRadioStarted();

    void onRadioStopped();

    void onMetaDataReceived(String s, String s2);
}
