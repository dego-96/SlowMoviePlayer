package jp.mydns.dego.slowmovieplayer;

class DecodeEvent {
    private boolean mIsInput;
    private int mBufferId;

    /**
     * DecodeEvent
     *
     * @param aIsInput  is input event
     * @param aBufferId buffer id
     */
    DecodeEvent(boolean aIsInput, int aBufferId) {
        mIsInput = aIsInput;
        mBufferId = aBufferId;
    }

    /**
     * isInput
     *
     * @return true: input event / false: output event
     */
    boolean isInput() {
        return mIsInput;
    }

    /**
     * getBufferId
     *
     * @return buffer id
     */
    int getBufferId() {
        return mBufferId;
    }
}
