package com.ljb.makedictdb;

public class LrcSentence {
    public long start;
    public long stop;
    public String content;

    @Override
    public String toString() {
        return String.format("[%d,%d]%s", start, stop, content);
    }
}

/* Location:           /home/ljb/Downloads/com.changba_192657.apk_FILES/classes_dex2jar.jar
 * Qualified Name:     com.changba.playrecord.view.LrcWord
 * JD-Core Version:    0.6.2
 */