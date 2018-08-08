package com.tencent.qcloud.xiaoshipin.videojoiner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.common.widget.VideoWorkProgressFragment;
import com.tencent.qcloud.xiaoshipin.login.TCUserMgr;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoCutterActivity;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoEditerWrapper;
import com.tencent.qcloud.xiaoshipin.videoeditor.utils.PlayState;
import com.tencent.qcloud.xiaoshipin.videoeditor.utils.TCEditerUtil;
import com.tencent.ugc.TXVideoEditConstants;
import com.tencent.ugc.TXVideoEditer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TCPictureJoinActivity extends FragmentActivity implements TCVideoEditerWrapper.TXVideoPreviewListenerWrapper, View.OnClickListener, TXVideoEditer.TXVideoGenerateListener {

    private static final String TAG = TCPictureJoinActivity.class.getSimpleName();

    private TXVideoEditer mTXVideoEditer;
    private TCVideoEditerWrapper wrapper;
    private ArrayList<String> picPathList;
    private ArrayList<Bitmap> bitmapList;
    private long mVideoDuration;
    private ImageView mBtnBack;
    private Button mBtnNext;
    private FrameLayout mPlayer;
    private int mCurrentState = PlayState.STATE_STOP;       // 播放器当前状态
    private TextView mTvTransition1;
    private TextView mTvTransition2;
    private TextView mTvTransition3;
    private TextView mTvTransition4;
    private TextView mTvTransition5;
    private TextView mTvTransition6;
    private String mVideoOutputPath;
    private VideoWorkProgressFragment mWorkLoadingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_join);

        picPathList = getIntent().getStringArrayListExtra(TCConstants.INTENT_KEY_MULTI_PIC_LIST);
        if (picPathList == null || picPathList.size() == 0) {
            finish();
            return;
        }
        wrapper = TCVideoEditerWrapper.getInstance();
        wrapper.addTXVideoPreviewListenerWrapper(this);
        mTXVideoEditer = new TXVideoEditer(this);
        wrapper.setEditer(mTXVideoEditer);

        decodeFileToBitmap(picPathList);
        int result = mTXVideoEditer.setPictureList(bitmapList, 20);
        if (result == TXVideoEditConstants.PICTURE_TRANSITION_FAILED) {
            Toast.makeText(this, "图片设置异常，结束编辑", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 注意：
        // 1、接口调用顺序：setPictureList在前，setPicTransferType在后，必须顺序调用
        // 1、图片转视频的时长需要设置转场类型后获取，因为不同的转场类型时长会不一样
        // 2、宽高信息按照第一张图片的宽高读取，在加片尾水印的时候算归一化坐标用到。
        mVideoDuration = mTXVideoEditer.setPictureTransition(TXVideoEditConstants.TX_TRANSITION_TYPE_LEFT_RIGHT_SLIPPING);
        TXVideoEditConstants.TXVideoInfo txVideoInfo = new TXVideoEditConstants.TXVideoInfo();
        txVideoInfo.duration = mVideoDuration;
        txVideoInfo.width = bitmapList.get(0).getWidth();
        txVideoInfo.height = bitmapList.get(0).getHeight();
        wrapper.setTXVideoInfo(txVideoInfo);

        initViews();
        initPlayerLayout();
    }

    private void initPlayerLayout() {
        TXVideoEditConstants.TXPreviewParam param = new TXVideoEditConstants.TXPreviewParam();
        param.videoView = mPlayer;
        param.renderMode = TXVideoEditConstants.PREVIEW_RENDER_MODE_FILL_EDGE;
        mTXVideoEditer.initWithPreview(param);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TCVideoEditerWrapper.getInstance().addTXVideoPreviewListenerWrapper(this);
        if (mCurrentState == PlayState.STATE_STOP) {
            if (mTXVideoEditer != null) {
                mTXVideoEditer.startPlayFromTime(0, mVideoDuration);
                mCurrentState = PlayState.STATE_PLAY;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        TCVideoEditerWrapper.getInstance().removeTXVideoPreviewListenerWrapper(this);
    }

    private void initViews() {
        mBtnBack = (ImageView) findViewById(R.id.btn_back);
        mBtnNext = (Button) findViewById(R.id.btn_next);
        mPlayer = (FrameLayout) findViewById(R.id.layout_palyer);
        mBtnBack.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);

        mTvTransition1 = (TextView) findViewById(R.id.transition1);
        mTvTransition2 = (TextView) findViewById(R.id.transition2);
        mTvTransition3 = (TextView) findViewById(R.id.transition3);
        mTvTransition4 = (TextView) findViewById(R.id.transition4);
        mTvTransition5 = (TextView) findViewById(R.id.transition5);
        mTvTransition6 = (TextView) findViewById(R.id.transition6);

        mTvTransition1.setOnClickListener(this);
        mTvTransition2.setOnClickListener(this);
        mTvTransition3.setOnClickListener(this);
        mTvTransition4.setOnClickListener(this);
        mTvTransition5.setOnClickListener(this);
        mTvTransition6.setOnClickListener(this);
    }

    private void decodeFileToBitmap(List<String> picPathList) {
        bitmapList = new ArrayList<>();
        for (int i = 0; i < picPathList.size(); i++) {
            String filePath = picPathList.get(i);
            Bitmap bitmap = TCEditerUtil.decodeSampledBitmapFromFile(filePath, 720, 1280);
            bitmapList.add(bitmap);
            TCVideoEditerWrapper.getInstance().addThumbnailBitmap(0, bitmap);
        }
    }


    @Override
    public void onPreviewProgressWrapper(int time) {
//        mTXVideoEditer.startPlayFromTime(0, mVideoDuration);
//        mCurrentState = PlayState.STATE_PLAY;
    }

    @Override
    public void onPreviewFinishedWrapper() {

    }

    @Override
    public void onClick(View v) {
        long duration = mVideoDuration;
        mTXVideoEditer.stopPlay();
        switch (v.getId()) {
            case R.id.btn_back:// 返回
                onBackPressed();
                finish();
                break;
            case R.id.btn_next:// 开始预处理
                startGenerateVideo();
                break;
            case R.id.transition1: //左右
                duration = mTXVideoEditer.setPictureTransition(TXVideoEditConstants.TX_TRANSITION_TYPE_LEFT_RIGHT_SLIPPING);
                break;
            case R.id.transition2: //上下
                duration = mTXVideoEditer.setPictureTransition(TXVideoEditConstants.TX_TRANSITION_TYPE_UP_DOWN_SLIPPING);
                break;
            case R.id.transition3: //放大
                duration = mTXVideoEditer.setPictureTransition(TXVideoEditConstants.TX_TRANSITION_TYPE_ENLARGE);
                break;
            case R.id.transition4: //缩小
                duration = mTXVideoEditer.setPictureTransition(TXVideoEditConstants.TX_TRANSITION_TYPE_NARROW);
                break;
            case R.id.transition5: //旋转
                duration = mTXVideoEditer.setPictureTransition(TXVideoEditConstants.TX_TRANSITION_TYPE_ROTATIONAL_SCALING);
                break;
            case R.id.transition6: //淡入淡出
                duration = mTXVideoEditer.setPictureTransition(TXVideoEditConstants.TX_TRANSITION_TYPE_FADEIN_FADEOUT);
                break;
        }
        mTXVideoEditer.startPlayFromTime(0, duration);
    }

    private void startGenerateVideo() {
        mTXVideoEditer.stopPlay(); // 停止播放

        mCurrentState = PlayState.STATE_GENERATE;
        mVideoOutputPath = TCEditerUtil.generateVideoPath();

        if (mWorkLoadingProgress == null) {
            initWorkLoadingProgress();
        }
        mWorkLoadingProgress.setProgress(0);
        mWorkLoadingProgress.setCancelable(false);
        mWorkLoadingProgress.show(getSupportFragmentManager(), "progress_dialog");

        mTXVideoEditer.setVideoGenerateListener(this);
        mTXVideoEditer.generateVideo(TXVideoEditConstants.VIDEO_COMPRESSED_720P, mVideoOutputPath);
    }

    private void initWorkLoadingProgress() {
        if (mWorkLoadingProgress == null) {
            mWorkLoadingProgress = new VideoWorkProgressFragment();
            mWorkLoadingProgress.setOnClickStopListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopGenerate();
                }
            });
        }
        mWorkLoadingProgress.setProgress(0);
    }

    private void stopGenerate() {
        if (mCurrentState == PlayState.STATE_GENERATE) {
            mWorkLoadingProgress.dismiss();
            Toast.makeText(this, "取消视频生成", Toast.LENGTH_SHORT).show();
            mWorkLoadingProgress.setProgress(0);
            mCurrentState = PlayState.STATE_NONE;
            if (mTXVideoEditer != null) {
                mTXVideoEditer.cancel();
            }
        }
    }

    @Override // 生成进度回调
    public void onGenerateProgress(final float progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWorkLoadingProgress.setProgress((int) (progress * 100));
            }
        });
    }

    @Override // 生成完成
    public void onGenerateComplete(final TXVideoEditConstants.TXGenerateResult result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (result.retCode == TXVideoEditConstants.GENERATE_RESULT_OK) {
                    startCutActivity();
                }
                mCurrentState = PlayState.STATE_NONE;
                TCUserMgr.getInstance().uploadLogs(TCConstants.ELK_ACTION_PICTURE_EDIT, TCUserMgr.getInstance().getUserId(), result.retCode, result.descMsg, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                    }
                });
            }
        });
    }

    private void startCutActivity() {
        mTXVideoEditer.release();
        Intent intent = new Intent(this, TCVideoCutterActivity.class);
        intent.putExtra(TCConstants.VIDEO_EDITER_PATH, mVideoOutputPath);
        Log.i("lyj", "mVideoOutputPath:" + mVideoOutputPath);
        startActivity(intent);
        finish();
    }
}
