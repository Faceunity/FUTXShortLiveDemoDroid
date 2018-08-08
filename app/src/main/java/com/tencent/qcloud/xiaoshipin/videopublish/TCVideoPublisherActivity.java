package com.tencent.qcloud.xiaoshipin.videopublish;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.activity.TCBaseActivity;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.common.utils.TCUtils;
import com.tencent.qcloud.xiaoshipin.login.TCUserMgr;
import com.tencent.qcloud.xiaoshipin.mainui.TCMainActivity;
import com.tencent.qcloud.xiaoshipin.videoupload.TXUGCPublish;
import com.tencent.qcloud.xiaoshipin.videoupload.TXUGCPublishTypeDef;
import com.tencent.rtmp.TXLiveConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by carolsuo on 2017/3/9.
 * UGC发布页面
 */
public class TCVideoPublisherActivity extends TCBaseActivity implements View.OnClickListener, TXUGCPublishTypeDef.ITXVideoPublishListener {
    private String TAG = TCVideoPublisherActivity.class.getName();
    private String mVideoPath = null;

    private String mCoverPath = null;
    private ImageView mBtnBack;

    private ImageView mImageViewBg;

    private TXUGCPublish mVideoPublish = null;

    boolean mIsPlayRecordType = false;
    private boolean mIsFetchCosSig = false;
    private String mCosSignature = null;
    private Handler mHandler = new Handler();

    private boolean mAllDone = false;
    private NetchangeReceiver mNetchangeReceiver = null;
    private int mRotation;
    private boolean mDisableCache;
    private String mLocalVideoPath;
    private ProgressBar mProgressBar;
    private TextView mTvProgress;
    private RelativeLayout mLayoutResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_publisher2);

        mVideoPath = getIntent().getStringExtra(TCConstants.VIDEO_RECORD_VIDEPATH);
        mCoverPath = getIntent().getStringExtra(TCConstants.VIDEO_RECORD_COVERPATH);
        mRotation = getIntent().getIntExtra(TCConstants.VIDEO_RECORD_ROTATION, TXLiveConstants.RENDER_ROTATION_PORTRAIT);
        mDisableCache = getIntent().getBooleanExtra(TCConstants.VIDEO_RECORD_NO_CACHE, false);
        mLocalVideoPath = getIntent().getStringExtra(TCConstants.VIDEO_RECORD_VIDEPATH);

        mIsPlayRecordType = getIntent().getIntExtra(TCConstants.VIDEO_RECORD_TYPE, 0) == TCConstants.VIDEO_RECORD_TYPE_PLAY;

        mBtnBack = (ImageView) findViewById(R.id.btn_back);
        mBtnBack.setOnClickListener(this);

        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mTvProgress = (TextView) findViewById(R.id.tv_progress);
        mImageViewBg = (ImageView) findViewById(R.id.bg_iv);

        mLayoutResult = (RelativeLayout) findViewById(R.id.layout_publish_success);
        mLayoutResult.setOnClickListener(this);

        if (mCoverPath != null)
            Glide.with(this).load(Uri.fromFile(new File(mCoverPath))).into(mImageViewBg);
        publishVideo();
    }

    private void fetchSignature() {
        if (mIsFetchCosSig)
            return;
        mIsFetchCosSig = true;

        TCUserMgr.getInstance().getVodSig(new TCUserMgr.Callback() {
            @Override
            public void onSuccess(JSONObject data) {
                try {
                    mCosSignature = data.getString("signature");
                    startPublish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                TCUserMgr.getInstance().uploadLogs(TCConstants.ELK_ACTION_VIDEO_SIGN, TCUserMgr.getInstance().getUserId(), TCUserMgr.SUCCESS_CODE, "获取签名成功", new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                    }
                });
            }

            @Override
            public void onFailure(int code, final String msg) {
                TCUserMgr.getInstance().uploadLogs(TCConstants.ELK_ACTION_VIDEO_SIGN, TCUserMgr.getInstance().getUserId(), code, "获取签名失败", new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                    }
                });
                TCVideoPublisherActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        mTVPublish.setText("网络连接断开，视频上传失败");
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                back();
                break;
            case R.id.btn_publish:
                publishVideo();
                break;
            case R.id.layout_publish_success:
                Intent intent = new Intent(this, TCMainActivity.class);
                startActivity(intent);
                finish();
                break;
            default:
                break;
        }
    }

    private void back() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alertDialog = builder.setTitle(getString(R.string.cancel_publish_title)).setCancelable(false).setMessage(R.string.cancel_publish_msg)
                .setPositiveButton(R.string.cancel_publish_title, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .setNegativeButton(getString(R.string.wrong_click), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        alertDialog.show();
    }

    private void publishVideo() {
        if (mAllDone) {
            Intent intent = new Intent(TCVideoPublisherActivity.this, TCMainActivity.class);
            startActivity(intent);
        } else {
            if (!TCUtils.isNetworkAvailable(this)) {
                Toast.makeText(getApplicationContext(), "当前无网络连接", Toast.LENGTH_SHORT).show();
                return;
            }
            fetchSignature();
        }
    }

    void startPublish() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mVideoPublish == null)
                    mVideoPublish = new TXUGCPublish(TCVideoPublisherActivity.this.getApplicationContext(), TCUserMgr.getInstance().getUserId());
                mVideoPublish.setListener(TCVideoPublisherActivity.this);

                TXUGCPublishTypeDef.TXPublishParam param = new TXUGCPublishTypeDef.TXPublishParam();
                param.signature = mCosSignature;
                param.videoPath = mVideoPath;
                param.coverPath = mCoverPath;
                int publishCode = mVideoPublish.publishVideo(param);
                if (publishCode != 0) {
//                    mTVPublish.setText("发布失败，错误码：" + publishCode);
                }

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                if (null == mNetchangeReceiver) {
                    mNetchangeReceiver = new NetchangeReceiver();
                }
                TCVideoPublisherActivity.this.getApplicationContext().registerReceiver(mNetchangeReceiver, intentFilter);
            }
        });
    }

    @Override
    public void onPublishProgress(long uploadBytes, long totalBytes) {
        int progress = (int) (uploadBytes * 100 / totalBytes);
        Log.d(TAG, "onPublishProgress:" + progress);
        mProgressBar.setProgress(progress);
        mTvProgress.setText("正在上传" + progress + "%");
    }

    @Override
    public void onPublishComplete(TXUGCPublishTypeDef.TXPublishResult txPublishResult) {
        Log.d(TAG, "onPublishComplete:" + txPublishResult.retCode);

        String desc = null;
        if (txPublishResult.retCode == TXUGCPublishTypeDef.PUBLISH_RESULT_OK) {
            desc = "视频发布成功";
        } else {
            desc = "视频发布失败onPublishComplete:" + txPublishResult.descMsg;
        }
        TCUserMgr.getInstance().uploadLogs(TCConstants.ELK_ACTION_VIDEO_UPLOAD_VOD, TCUserMgr.getInstance().getUserId(), txPublishResult.retCode, desc, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });
        if (txPublishResult.retCode == TXUGCPublishTypeDef.PUBLISH_RESULT_OK) {
            mBtnBack.setVisibility(View.GONE);
//            mIVPublishing.setImageResource(R.drawable.publish_success);
//            mTVPublish.setText("发布成功啦！");
            UploadUGCVideo(txPublishResult.videoId, txPublishResult.videoURL, txPublishResult.coverURL);
        } else {
//            mIVPublishing.setVisibility(View.INVISIBLE);
            if (txPublishResult.descMsg.contains("java.net.UnknownHostException") || txPublishResult.descMsg.contains("java.net.ConnectException")) {
                mTvProgress.setText("网络连接断开，视频上传失败");
            } else {
                mTvProgress.setText(txPublishResult.descMsg);
            }
            Log.e(TAG, txPublishResult.descMsg);
        }
    }

    private void deleteCache() {
        if (mDisableCache) {
            File file = new File(mVideoPath);
            if (file.exists()) {
                file.delete();
            }
            if (!TextUtils.isEmpty(mCoverPath)) {
                file = new File(mCoverPath);
                if (file.exists()) {
                    file.delete();
                }
            }
            if (mLocalVideoPath != null) {
                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scanIntent.setData(Uri.fromFile(new File(mLocalVideoPath)));
                sendBroadcast(scanIntent);
            }
        }
    }

    private void UploadUGCVideo(final String videoId, final String videoURL, final String coverURL) {
//        String title = mTitleEditText.getText().toString();
        String title = null; //TODO:传入本地视频文件名称
        if (TextUtils.isEmpty(title)) {
            title = "小视频";
        }
        try {
            JSONObject body = new JSONObject().put("file_id", videoId)
                    .put("title", title)
                    .put("frontcover", coverURL)
                    .put("location", "未知")
                    .put("play_url", videoURL);
            TCUserMgr.getInstance().request("/upload_ugc", body, new TCUserMgr.HttpCallback("upload_ugc", new TCUserMgr.Callback() {
                @Override
                public void onSuccess(JSONObject data) {
                    TCUserMgr.getInstance().uploadLogs(TCConstants.ELK_ACTION_VIDEO_UPLOAD_SERVER, TCUserMgr.getInstance().getUserId(), TCUserMgr.SUCCESS_CODE, "UploadUGCVideo Sucess", new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {

                        }
                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLayoutResult.setVisibility(View.VISIBLE);

                            Intent intent = new Intent(TCVideoPublisherActivity.this, TCMainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                }

                @Override
                public void onFailure(int code, final String msg) {
                    TCUserMgr.getInstance().uploadLogs(TCConstants.ELK_ACTION_VIDEO_UPLOAD_SERVER, TCUserMgr.getInstance().getUserId(), code, msg, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {

                        }
                    });
                }
            }));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mNetchangeReceiver != null) {
            this.getApplicationContext().unregisterReceiver(mNetchangeReceiver);
        }

        deleteCache();
    }

    public class NetchangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                if (!TCUtils.isNetworkAvailable(TCVideoPublisherActivity.this)) {
//                    mIVPublishing.setVisibility(View.INVISIBLE);
                    mTvProgress.setText("网络连接断开，视频上传失败");
                }
            }
        }
    }
}
