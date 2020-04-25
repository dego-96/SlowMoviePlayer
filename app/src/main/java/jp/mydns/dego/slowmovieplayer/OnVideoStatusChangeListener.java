package jp.mydns.dego.slowmovieplayer;

public interface OnVideoStatusChangeListener {

    void onPlayerStatusChanged(VideoPlayer player);

    void onProgressChanged(int progress);

    void onDurationChanged(int duration);

    void onPlayToEnd();
}
