package co.mobiwise.library.media;

/**
 * Created by mertsimsek on 04/11/15.
 */
public interface MediaListener {

    void onMediaLoading();

    void onMediaStarted(int totalDuration, int currentDuration);

    void onMediaStopped();

}
