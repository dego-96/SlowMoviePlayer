package jp.mydns.dego.slowmovieplayer;

public interface OnVideoStatusChangeListener {

    void onProgressChanged(int progress);

    void onDurationChanged(int duration);

    void onPlayToEnd();
}
