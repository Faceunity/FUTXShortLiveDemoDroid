package com.tencent.qcloud.xiaoshipin.videoeditor.utils;

/**
 * Created by liyuejiao on 2018/5/29.
 * 草稿箱保存【点击返回】
 */

public class DraftEditer {
    private static DraftEditer sInstance;

    public static DraftEditer getInstance() {
        if (sInstance == null) {
            synchronized (DraftEditer.class) {
                if (sInstance == null) {
                    sInstance = new DraftEditer();
                }
            }
        }
        return sInstance;
    }

    private DraftEditer() {
        bgmVolume = 0.5f;
        videoVolume = 0.5f;
    }

    //背景音乐相关
    private String bgmPath;
    private int bgmPos;
    private float bgmVolume;
    private float videoVolume;
    private long bgmStartTime;
    private long bgmEndTime;
    private long bgmDuration;

    public String getBgmPath() {
        return bgmPath;
    }

    public void setBgmPath(String bgmPath) {
        this.bgmPath = bgmPath;
    }

    public int getBgmPos() {
        return bgmPos;
    }

    public void setBgmPos(int bgmPos) {
        this.bgmPos = bgmPos;
    }

    public float getBgmVolume() {
        return bgmVolume;
    }

    public void setBgmVolume(float bgmVolume) {
        this.bgmVolume = bgmVolume;
    }

    public float getVideoVolume() {
        return videoVolume;
    }

    public void setVideoVolume(float videoVolume) {
        this.videoVolume = videoVolume;
    }

    public long getBgmStartTime() {
        return bgmStartTime;
    }

    public void setBgmStartTime(long bgmStartTime) {
        this.bgmStartTime = bgmStartTime;
    }

    public long getBgmEndTime() {
        return bgmEndTime;
    }

    public void setBgmEndTime(long bgmEndTime) {
        this.bgmEndTime = bgmEndTime;
    }

    public long getBgmDuration() {
        return bgmDuration;
    }

    public void setBgmDuration(long bgmDuration) {
        this.bgmDuration = bgmDuration;
    }

    public void clear() {
        this.bgmPath = null;
        this.bgmPos = -1;
        this.bgmVolume = 0.5f;
        this.videoVolume = 0.5f;
        this.bgmStartTime = -1;
        this.bgmEndTime = -1;
    }
}
