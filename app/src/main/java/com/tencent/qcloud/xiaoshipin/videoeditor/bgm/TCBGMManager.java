package com.tencent.qcloud.xiaoshipin.videoeditor.bgm;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.qcloud.xiaoshipin.TCApplication;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.common.utils.TCHttpEngine;
import com.tencent.qcloud.xiaoshipin.videoeditor.bgm.utils.TCBGMDownloadProgress;
import com.tencent.qcloud.xiaoshipin.videoeditor.bgm.utils.TCBGMInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by vinsonswang on 2017/12/8.
 */

public class TCBGMManager {
    private static final String TAG = "TCBgmManager";
    private boolean isLoading;
    private SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(TCApplication.getApplication());
    private LoadBgmListener mLoadBgmListener;

    private static class TCBgmMgrHolder {
        private static TCBGMManager instance = new TCBGMManager();
    }

    public static TCBGMManager getInstance() {
        return TCBgmMgrHolder.instance;
    }

    public void loadBgmList(){
        if(isLoading){
            TXCLog.e(TAG, "loadBgmList, is loading");
            return;
        }
        isLoading = true;
        TCHttpEngine.getInstance().get(TCConstants.SVR_BGM_GET_URL, new TCHttpEngine.Listener() {
            @Override
            public void onResponse(int retCode, String retMsg, JSONObject retData) {
                TXCLog.i(TAG, "retData = " + retData);
                try {
                    JSONObject bgmObject = retData.getJSONObject("bgm");
                    if(bgmObject == null && mLoadBgmListener != null){
                        mLoadBgmListener.onBgmList(null);
                        return;
                    }
                    JSONArray list = bgmObject.getJSONArray("list");
                    Type listType = new TypeToken<ArrayList<TCBGMInfo>>(){}.getType();
                    ArrayList<TCBGMInfo> bgmInfoList = new Gson().fromJson(list.toString(), listType);

                    getLocalPath(bgmInfoList);
                    if(mLoadBgmListener != null){
                        mLoadBgmListener.onBgmList(bgmInfoList);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    isLoading = false;
                }
            }
        });
    }

    /**
     * 根据bgmList，获取本地已保存过的路径
     * @param bgmInfoList
     */
    private void getLocalPath(ArrayList<TCBGMInfo> bgmInfoList){
        if(bgmInfoList == null || bgmInfoList.size() == 0){
            return;
        }
        for(TCBGMInfo tcbgmInfo : bgmInfoList){
            tcbgmInfo.localPath = mPrefs.getString(tcbgmInfo.name, "");
        }
    }

    public void downloadBgmInfo(final String bgmName, final int position, String url){
        TCBGMDownloadProgress tcbgmDownloadProgress = new TCBGMDownloadProgress(bgmName, position, url);
        tcbgmDownloadProgress.start(new TCBGMDownloadProgress.Downloadlistener() {
            @Override
            public void onDownloadFail(String errorMsg) {
                if(mLoadBgmListener != null){
                    mLoadBgmListener.onDownloadFail(errorMsg);
                }
            }

            @Override
            public void onDownloadProgress(int progress) {
                TXCLog.i(TAG, "downloadBgmInfo, progress = " + progress);
                if(mLoadBgmListener != null){
                    mLoadBgmListener.onDownloadProgress(progress);
                }
            }

            @Override
            public void onDownloadSuccess(String filePath) {
                TXCLog.i(TAG, "onDownloadSuccess, filePath = " + filePath);
                if(mLoadBgmListener != null){
                    mLoadBgmListener.onBgmDownloadSuccess(position, filePath);
                }
                // 本地保存，防止重复下载
                mPrefs.edit().putString(bgmName, filePath).apply();
            }
        });

    }

    public void setOnLoadBgmListener(LoadBgmListener loadBgmListener){
        mLoadBgmListener = loadBgmListener;
    }

    public interface LoadBgmListener {
        /**
         * @param tcBgmInfoList BGM列表数据
         */
        void onBgmList(final ArrayList<TCBGMInfo> tcBgmInfoList);

        void onBgmDownloadSuccess(int position, String filePath);

        void onDownloadFail(String errorMsg);

        void onDownloadProgress(int progress);
    }
}
