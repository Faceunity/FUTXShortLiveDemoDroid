package com.tencent.qcloud.xiaoshipin.videoeditor.bgm.utils;

/**
 * Created by hanszhli on 2017/7/7.
 */

public class TCBGMInfo {
    public String name;
    public String url;

    public String localPath;
    public int status = STATE_UNDOWNLOAD;
    public int progress;

    public static final int STATE_UNDOWNLOAD = 1;
    public static final int STATE_DOWNLOADING = 2;
    public static final int STATE_DOWNLOADED = 3;
    public static final int STATE_USED = 4;
}
