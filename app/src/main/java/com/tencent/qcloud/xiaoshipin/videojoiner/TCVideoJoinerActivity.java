package com.tencent.qcloud.xiaoshipin.videojoiner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;

import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.common.widget.VideoWorkProgressFragment;
import com.tencent.qcloud.xiaoshipin.login.TCUserMgr;
import com.tencent.qcloud.xiaoshipin.videochoose.TCVideoFileInfo;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoCutterActivity;
import com.tencent.qcloud.xiaoshipin.videoeditor.utils.TCEditerUtil;
import com.tencent.rtmp.TXLog;
import com.tencent.ugc.TXVideoEditConstants;
import com.tencent.ugc.TXVideoJoiner;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TCVideoJoinerActivity extends FragmentActivity {

    private static final String TAG = TCVideoJoinerActivity.class.getSimpleName();

    private ArrayList<TCVideoFileInfo> mTCVideoFileInfoList;

    private TXVideoJoiner mTXVideoJoiner;
    private ArrayList<String> mVideoSourceList;
    private VideoWorkProgressFragment mWorkProgressFragment;
    private boolean mGenerateSuccess;
    private String mOutputPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTCVideoFileInfoList = (ArrayList<TCVideoFileInfo>) getIntent().getSerializableExtra(TCConstants.INTENT_KEY_MULTI_CHOOSE);
        if (mTCVideoFileInfoList == null || mTCVideoFileInfoList.size() == 0) {
            finish();
            return;
        }
        startJoin();
    }

    private void initWorkLoadingProgress() {
        if (mWorkProgressFragment == null) {
            mWorkProgressFragment = VideoWorkProgressFragment.newInstance(getResources().getString(R.string.video_joining));
            mWorkProgressFragment.setOnClickStopListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cancelJoin();
                }
            });
        }
        mWorkProgressFragment.setProgress(0);
        mWorkProgressFragment.show(getSupportFragmentManager(), "work_progress");
    }

    private void cancelJoin() {
        if (!mGenerateSuccess) {
            if (mWorkProgressFragment != null)
                mWorkProgressFragment.dismiss();
            Toast.makeText(this, R.string.cancel_joining, Toast.LENGTH_SHORT).show();

            if (mTXVideoJoiner != null)
                mTXVideoJoiner.cancel();

            finish();
        }
    }

    private void startJoin() {
        mTXVideoJoiner = new TXVideoJoiner(this);
        mVideoSourceList = new ArrayList<>();
        for (int i = 0; i < mTCVideoFileInfoList.size(); i++) {
            mVideoSourceList.add(mTCVideoFileInfoList.get(i).getFilePath());
        }
        int ret = mTXVideoJoiner.setVideoPathList(mVideoSourceList);
        mTXVideoJoiner.setVideoJoinerListener(mJoinerListener);
        initWorkLoadingProgress();
        mOutputPath = TCEditerUtil.generateVideoPath();
        mTXVideoJoiner.joinVideo(TXVideoEditConstants.VIDEO_COMPRESSED_540P, mOutputPath);
    }

    private TXVideoJoiner.TXVideoJoinerListener mJoinerListener = new TXVideoJoiner.TXVideoJoinerListener() {
        @Override
        public void onJoinProgress(float progress) {
            int prog = (int) (progress * 100);
            TXLog.d(TAG, "composer progress = " + prog);
            mWorkProgressFragment.setProgress(prog);
        }

        @Override
        public void onJoinComplete(TXVideoEditConstants.TXJoinerResult result) {
            String desc = null;
            switch (result.retCode) {
                case TXVideoEditConstants.GENERATE_RESULT_OK:
                    desc = "视频合成成功";
                    break;
                case TXVideoEditConstants.GENERATE_RESULT_FAILED:
                    desc = "视频合成失败";
                    break;
                case TXVideoEditConstants.GENERATE_RESULT_LICENCE_VERIFICATION_FAILED:
                    desc = "licence校验失败";
                    break;
            }
            TCUserMgr.getInstance().uploadLogs(TCConstants.ELK_ACTION_VIDEO_JOINER, TCUserMgr.getInstance().getUserId(), result.retCode, desc, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                }
            });
            mWorkProgressFragment.dismiss();
            if (result.retCode == TXVideoEditConstants.JOIN_RESULT_OK) {
                startCutActivity();
                mGenerateSuccess = true;
            }
        }
    };

    private void startCutActivity() {
        Intent intent = new Intent(TCVideoJoinerActivity.this, TCVideoCutterActivity.class);
        intent.putExtra(TCConstants.VIDEO_EDITER_PATH, mOutputPath);
        startActivity(intent);
        finish();
    }

}
