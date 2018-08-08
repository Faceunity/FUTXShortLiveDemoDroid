package com.tencent.qcloud.xiaoshipin.videoeditor.filter;

/**
 * Created by vinsonswang on 2018/1/30.
 */

public class TCStaticFilterViewInfoManager {
    private static TCStaticFilterViewInfoManager instance;
    private int mCurrentPosition = 0;

    public static TCStaticFilterViewInfoManager getInstance(){
        if(instance == null){
            synchronized (TCStaticFilterViewInfoManager.class){
                if(instance == null){
                    instance = new TCStaticFilterViewInfoManager();
                }
            }
        }
        return instance;
    }

    public void setCurrentPosition(int position){
        this.mCurrentPosition = position;
    }

    public int getCurrentPosition(){
        return mCurrentPosition;
    }

    public void clearCurrentPosition(){
        this.mCurrentPosition = 0;
    }

}
