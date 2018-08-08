package com.tencent.qcloud.xiaoshipin.videojoiner;

import com.tencent.qcloud.xiaoshipin.videochoose.TCVideoFileInfo;

/**
 * Created by liyuejiao on 2018/1/11.
 */

public class ItemView {
    public interface OnDeleteListener {
        void onDelete(int position);
    }

    public interface OnAddListener {
        void onAdd(TCVideoFileInfo fileInfo);
    }
}
