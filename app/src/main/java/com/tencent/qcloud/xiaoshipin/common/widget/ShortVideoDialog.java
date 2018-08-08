package com.tencent.qcloud.xiaoshipin.common.widget;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.DownloadUtil;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.videochoose.TCPictureChooseActivity;
import com.tencent.qcloud.xiaoshipin.videochoose.TCVideoChooseActivity;
import com.tencent.qcloud.xiaoshipin.videorecord.TCVideoRecordActivity;
import com.tencent.ugc.TXRecordCommon;
import com.tencent.ugc.TXVideoEditConstants;
import com.tencent.ugc.TXVideoInfoReader;

import java.io.File;

/**
 * 短视频选择界面
 */
public class ShortVideoDialog extends DialogFragment implements View.OnClickListener {

    private TextView mTVVideo;
    private ImageView mIVClose;
    private TextView mTVEditer;
    private TextView mTVPicture;
    private TextView mTVChorus;

    private ProgressDialog mDownloadProgressDialog;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Dialog dialog = new Dialog(getActivity(), R.style.BottomDialog);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_short_video);
        dialog.setCanceledOnTouchOutside(true);

        mTVVideo = (TextView) dialog.findViewById(R.id.tv_record);
        mTVEditer = (TextView) dialog.findViewById(R.id.tv_editer);
        mTVPicture = (TextView) dialog.findViewById(R.id.tv_picture);
        mTVChorus = (TextView) dialog.findViewById(R.id.tv_chorus);
        mIVClose = (ImageView) dialog.findViewById(R.id.iv_close);

        mTVVideo.setOnClickListener(this);
        mTVEditer.setOnClickListener(this);
        mTVPicture.setOnClickListener(this);
        mTVChorus.setOnClickListener(this);
        mIVClose.setOnClickListener(this);

        // 设置宽度为屏宽, 靠近屏幕底部。
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.BOTTOM;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT; // 宽度持平
        window.setAttributes(lp);

        mDownloadProgressDialog = new ProgressDialog(getActivity());
        mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // 设置进度条的形式为圆形转动的进度条
        mDownloadProgressDialog.setCancelable(false);                           // 设置是否可以通过点击Back键取消
        mDownloadProgressDialog.setCanceledOnTouchOutside(false);               // 设置在点击Dialog外是否取消Dialog进度条

        return dialog;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_record:
                dismissDialog();
                startActivity(new Intent(getActivity(), TCVideoRecordActivity.class));
                break;
            case R.id.tv_editer:
                dismissDialog();

                Intent intent = new Intent(getActivity(), TCVideoChooseActivity.class);
                startActivity(intent);
                break;
            case R.id.tv_picture:
                dismissDialog();

                Intent intent2 = new Intent(getActivity(), TCPictureChooseActivity.class);
                startActivity(intent2);
                break;
            case R.id.tv_chorus:
                prepareToDownload();
                break;
            case R.id.iv_close:
                dismissDialog();
                break;
        }
    }

    private void prepareToDownload() {
        File downloadFileFolder = new File(Environment.getExternalStorageDirectory(), TCConstants.OUTPUT_DIR_NAME);
        File downloadFile = new File(downloadFileFolder, DownloadUtil.getNameFromUrl(TCConstants.CHORUS_URL));
        if(downloadFile.exists()){
            mDownloadProgressDialog.dismiss();
            TXVideoEditConstants.TXVideoInfo txVideoInfo = TXVideoInfoReader.getInstance().getVideoFileInfo(downloadFile.getAbsolutePath());
            startRecordActivity(downloadFile.getAbsolutePath(), (int) txVideoInfo.fps, txVideoInfo.audioSampleRate, txVideoInfo.duration);
            return;
        }
        if(mDownloadProgressDialog != null) {
            mDownloadProgressDialog.show();
        }
        DownloadUtil.get().download(TCConstants.CHORUS_URL, TCConstants.OUTPUT_DIR_NAME, new DownloadUtil.DownloadListener() {
            @Override
            public void onDownloadSuccess(final String path) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDownloadProgressDialog.dismiss();
                        TXVideoEditConstants.TXVideoInfo txVideoInfo = TXVideoInfoReader.getInstance().getVideoFileInfo(path);
                        startRecordActivity(path, (int) txVideoInfo.fps, txVideoInfo.audioSampleRate, txVideoInfo.duration);
                    }
                });
            }

            @Override
            public void onDownloading(final int progress) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDownloadProgressDialog.setMessage("正在下载..." + progress + "%");
                    }
                });
            }

            @Override
            public void onDownloadFailed() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDownloadProgressDialog.dismiss();
                        Toast.makeText(getActivity(), "下载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void startRecordActivity(String path, int fps, int audioSampleRate, long duration) {
        if(fps <= 0){
            fps = 20;
        }
        int audioSampleRateType = TXRecordCommon.AUDIO_SAMPLERATE_48000;
        if(audioSampleRate == 8000){
            audioSampleRateType = TXRecordCommon.AUDIO_SAMPLERATE_8000;
        }else if(audioSampleRate == 16000){
            audioSampleRateType = TXRecordCommon.AUDIO_SAMPLERATE_16000;
        }else if(audioSampleRate == 32000){
            audioSampleRateType = TXRecordCommon.AUDIO_SAMPLERATE_32000;
        }else if(audioSampleRate == 44100){
            audioSampleRateType = TXRecordCommon.AUDIO_SAMPLERATE_44100;
        }else{
            audioSampleRateType = TXRecordCommon.AUDIO_SAMPLERATE_48000;
        }
        Intent intent = new Intent(getActivity(), TCVideoRecordActivity.class);
        intent.putExtra(TCConstants.VIDEO_RECORD_TYPE, TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT);
        intent.putExtra(TCConstants.VIDEO_EDITER_PATH, path);
        intent.putExtra(TCConstants.VIDEO_RECORD_DURATION, (float) duration / 1000);
        intent.putExtra(TCConstants.VIDEO_RECORD_AUDIO_SAMPLE_RATE_TYPE, audioSampleRateType);
        intent.putExtra(TCConstants.RECORD_CONFIG_FPS, fps);
        startActivity(intent);
    }

    private void dismissDialog() {
        if (ShortVideoDialog.this.isAdded()) {
            ShortVideoDialog.this.dismiss();
        }
    }
}
