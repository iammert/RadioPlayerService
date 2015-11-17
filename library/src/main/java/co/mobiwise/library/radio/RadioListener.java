package co.mobiwise.library.radio;

/**
 * Created by mertsimsek on 01/07/15.
 */
public interface RadioListener {

  void onRadioLoading();

  void onRadioConnected();

  void onRadioStarted();

  void onRadioStopped();

  void onMetaDataReceived(String s, String s2);

  void onError();
}
