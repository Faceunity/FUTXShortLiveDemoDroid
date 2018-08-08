package com.tencent.qcloud.xiaoshipin.videoeditor.bgm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoEditerActivity;
import com.tencent.qcloud.xiaoshipin.videoeditor.bgm.utils.TCBGMInfo;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.BaseRecyclerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vinsonswang on 2017/12/8.
 */

public class BGMSelectActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {
    private final String TAG = "BGMSelectActivity";
    private LinearLayout mBackLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mBGMRecyclerView;
    private View mEmptyView;
    private TCMusicAdapter mTCMusicAdapter;
    private TCBGMManager.LoadBgmListener mLoadBgmListener;
    private List<TCBGMInfo> mTCBgmInfoList;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bgm_select);

        initData();

        initView();

        initListener();

        prepareToRefresh();
    }

    private void prepareToRefresh() {
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
        this.onRefresh();
    }

    private void initData() {
        mTCBgmInfoList = new ArrayList<>();
    }

    private void initListener() {
        mLoadBgmListener = new TCBGMManager.LoadBgmListener() {
            @Override
            public void onBgmList(ArrayList<TCBGMInfo> tcBgmInfoList) {
                mTCBgmInfoList.clear();
                if (tcBgmInfoList != null) {
                    mTCBgmInfoList.addAll(tcBgmInfoList);
                }
                mTCMusicAdapter.notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);

                mEmptyView.setVisibility(mTCMusicAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onBgmDownloadSuccess(int position, String filePath) {
                TCBGMInfo tcbgmInfo = mTCBgmInfoList.get(position);
                tcbgmInfo.localPath = filePath;
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
                mTCMusicAdapter.changeUseSelection(position);
                backToEditActivity(position, filePath);
            }

            @Override
            public void onDownloadFail(String errorMsg) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            }

            @Override
            public void onDownloadProgress(int progress) {
//                Log.d("lyj", "onDownloadProgress progress:" + progress);
                mTCMusicAdapter.updateProgress(progress);
                if (mProgressDialog == null) {
                    mProgressDialog = new ProgressDialog(BGMSelectActivity.this);
                    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mProgressDialog.setMax(100);
                    mProgressDialog.setIndeterminate(true);
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.show();
                }
                mProgressDialog.setProgress(progress);
            }
        };
        TCBGMManager.getInstance().setOnLoadBgmListener(mLoadBgmListener);
    }

    private void initView() {
        mBackLayout = (LinearLayout) findViewById(R.id.back_ll);
        mBackLayout.setOnClickListener(this);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.bgm_swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mBGMRecyclerView = (RecyclerView) findViewById(R.id.bgm_recycler_view);
        mEmptyView = findViewById(R.id.tv_bgm_empty);

        mTCMusicAdapter = new TCMusicAdapter(this, mTCBgmInfoList);
        mTCMusicAdapter.setOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                TCBGMInfo tcBgmInfo = mTCBgmInfoList.get(position);
                mTCMusicAdapter.changeUseSelection(position);
                TXCLog.i(TAG, "tcBgmInfo name = " + tcBgmInfo.name + ", url = " + tcBgmInfo.url);
                if (TextUtils.isEmpty(tcBgmInfo.localPath)) {
                    downloadBgmInfo(position, tcBgmInfo);
                    return;
                }
                backToEditActivity(position, tcBgmInfo.localPath);
            }
        });
        mBGMRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mBGMRecyclerView.setAdapter(mTCMusicAdapter);
    }

    private void backToEditActivity(int position, String path) {
        Intent intent = new Intent();
        intent.putExtra(TCConstants.BGM_POSITION, position);
        intent.putExtra(TCConstants.BGM_PATH, path);
        setResult(TCConstants.ACTIVITY_BGM_REQUEST_CODE, intent);
        finish();
    }

    @Override
    public void onRefresh() {
        TXCLog.i(TAG, "onRefresh");
        reloadBGMList();
    }

    private void downloadBgmInfo(int position, TCBGMInfo tcbgmInfo) {
        TCBGMManager.getInstance().downloadBgmInfo(tcbgmInfo.name, position, tcbgmInfo.url);
    }

    private void reloadBGMList() {
        TCBGMManager.getInstance().loadBgmList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TCBGMManager.getInstance().setOnLoadBgmListener(null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_ll:
                Intent intent = new Intent(this, TCVideoEditerActivity.class);
                startActivity(intent);
                finish();
                break;
        }
    }
}
