package jp.mydns.dego.slowmovieplayer;

public interface OnVideoStatusChangeListener {

    void onPlayerStatusChanged(VideoPlayer.PLAYER_STATUS status);

    void onProgressChanged(int progress);

    void onDurationChanged(int duration);

    void onPlayToEnd();
}
