package com.tencent.qcloud.xiaoshipin.mainui.list;

import com.tencent.qcloud.xiaoshipin.login.TCUserMgr;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class TCVideoListMgr {
    private static final String TAG = TCVideoListMgr.class.getSimpleName();
    private static final int PAGESIZE = 200;
    public static final int SUCCESS_CODE = 200;
    private boolean mIsFetching;

    private ArrayList<TCVideoInfo> mUGCInfoList = new ArrayList<>();
    private int mIndex = 1;

    private TCVideoListMgr() {
        mIsFetching = false;
    }

    private static class TCVideoListMgrHolder {
        private static TCVideoListMgr instance = new TCVideoListMgr();
    }

    public static TCVideoListMgr getInstance() {
        return TCVideoListMgrHolder.instance;
    }

    /**
     * 视频列表获取结果回调
     */
    public interface Listener {
        /**
         * @param retCode 获取结果，0表示成功
         * @param result  列表数据
         * @param refresh 是否需要刷新界面，首页需要刷新
         */
        public void onVideoList(int retCode, final ArrayList<TCVideoInfo> result, int index, int total, boolean refresh);
    }

    public void fetchUGCList(final Listener listener) {
        fetchVideoList("get_ugc_list", listener);
    }

    private void fetchVideoList(String cmd, final Listener listener) {
        try {
            String str = String.valueOf(mIndex);
            JSONObject body = new JSONObject().put("index", str).put("count", PAGESIZE);
            TCUserMgr.getInstance().request("/" + cmd, body, new TCUserMgr.HttpCallback(cmd, new TCUserMgr.Callback() {
                @Override
                public void onSuccess(JSONObject data) {
                    ArrayList<TCVideoInfo> videoList = new ArrayList();
                    if (data != null) {
                        JSONArray list = data.optJSONArray("list");
                        if (list != null) {
                            for (int i = 0; i < list.length(); i++) {
                                JSONObject obj = list.optJSONObject(i);
                                if (obj != null) {
                                    TCVideoInfo video = new TCVideoInfo(obj);
                                    videoList.add(video);
                                }
                            }
                        }
                    }
                    int total = data.optInt("total");
                    if (listener != null) {
                        listener.onVideoList(0, videoList, mIndex, total, true);
                    }
                    if (videoList.size() >= 0 && total > PAGESIZE) { //下次拉下一页视频
                        mIndex++;
                    }
                }

                @Override
                public void onFailure(int code, final String msg) {
                    if (listener != null) {
                        listener.onVideoList(code, null, 0, 0, false);
                    }
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

