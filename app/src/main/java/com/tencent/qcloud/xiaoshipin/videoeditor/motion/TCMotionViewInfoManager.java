package com.tencent.qcloud.xiaoshipin.videoeditor.motion;

import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.videotimeline.ColorfulProgress;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vinsonswang on 2018/1/30.
 */

public class TCMotionViewInfoManager {
    private static TCMotionViewInfoManager instance;
    private List<ColorfulProgress.MarkInfo> mMarkInfoList;
    public static TCMotionViewInfoManager getInstance(){
        if(instance == null){
            synchronized (TCMotionViewInfoManager.class){
                if(instance == null){
                    instance = new TCMotionViewInfoManager();
                }
            }
        }
        return instance;
    }

    private TCMotionViewInfoManager(){
        mMarkInfoList = new ArrayList<>();
    }

    public void setMarkInfoList(List<ColorfulProgress.MarkInfo> markInfoList){
        this.mMarkInfoList = markInfoList;
    }

    public List<ColorfulProgress.MarkInfo> getMarkInfoList(){
        return mMarkInfoList;
    }

    public void clearMarkInfoList(){
        this.mMarkInfoList.clear();
    }

}
