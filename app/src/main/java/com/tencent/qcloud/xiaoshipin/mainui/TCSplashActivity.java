package com.tencent.qcloud.xiaoshipin.mainui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;

import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.login.TCUserMgr;

import java.io.IOException;
import java.lang.ref.WeakReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TCSplashActivity extends Activity {

    private static final String TAG = TCSplashActivity.class.getSimpleName();

    private static final int START_LOGIN = 2873;
    private final MyHandler mHandler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isTaskRoot()
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
                && getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_MAIN)) {

            finish();
            return;
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Message msg = Message.obtain();
        msg.arg1 = START_LOGIN;
        mHandler.sendMessageDelayed(msg, 1000);

        boolean firstRun = isFirstRun(this);
        Log.i("splash", "firstRun:" + firstRun);
        if (firstRun) {
            saveFirstRun(this);
            TCUserMgr.getInstance().uploadLogs(TCConstants.ELK_ACTION_INSTALL, TCUserMgr.getInstance().getUserId(), 0, "首次安装成功", new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                }
            });
        }
    }

    private void saveFirstRun(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("share", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("isFirstRun", false).commit();
    }

    public static boolean isFirstRun(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("share", Context.MODE_PRIVATE);
        boolean isFirstRun = sharedPreferences.getBoolean("isFirstRun", true);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (isFirstRun) {
            editor.putBoolean("isFirstRun", false);
            editor.commit();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        //splashActivity下不允许back键退出
        //super.onBackPressed();
    }

    private void jumpToMainActivity() {
        Intent intent = new Intent(this, TCMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private static class MyHandler extends Handler {
        private final WeakReference<TCSplashActivity> mActivity;

        public MyHandler(TCSplashActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            TCSplashActivity activity = mActivity.get();
            if (activity != null) {
                activity.jumpToMainActivity();
            }
        }
    }

}
