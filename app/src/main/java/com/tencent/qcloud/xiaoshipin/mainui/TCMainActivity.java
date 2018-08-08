package com.tencent.qcloud.xiaoshipin.mainui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.FileUtils;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.common.utils.TCUtils;
import com.tencent.qcloud.xiaoshipin.common.widget.ShortVideoDialog;
import com.tencent.qcloud.xiaoshipin.login.TCLoginActivity;
import com.tencent.qcloud.xiaoshipin.login.TCUserMgr;
import com.tencent.qcloud.xiaoshipin.mainui.list.TCLiveListFragment;
import com.tencent.qcloud.xiaoshipin.userinfo.TCUserInfoFragment;

import java.io.File;
import java.io.IOException;

/**
 * 主界面: 短视频列表，用户信息页
 */
public class TCMainActivity extends FragmentActivity implements View.OnClickListener {
    private static final String TAG = "TCMainActivity";

    private Button mBtnVideo, mBtnSelect, mBtnUser;
    private Fragment mCurrentFragment;
    private Fragment mTCLiveListFragment, mTCUserInfoFragment;

    private long mLastClickPubTS = 0;

    private ShortVideoDialog mShortVideoDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        showVideoFragment();

        if (checkPermission()) return;

        copyLicenceToSdcard();
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return true;
                }
            }
        }
        return false;
    }

    private void copyLicenceToSdcard() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String sdcardFolder = getExternalFilesDir(null).getAbsolutePath();
                File sdcardLicenceFile = new File(sdcardFolder + File.separator + TCConstants.UGC_LICENCE_NAME);
                if(sdcardLicenceFile.exists()){
                    return;
                }
                try {
                    FileUtils.copyFromAssetToSdcard(TCMainActivity.this, TCConstants.UGC_LICENCE_NAME, sdcardFolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initView() {
        mShortVideoDialog = new ShortVideoDialog();

        mBtnVideo = (Button) findViewById(R.id.btn_home_left);
        mBtnSelect = (Button) findViewById(R.id.btn_home_select);
        mBtnUser = (Button) findViewById(R.id.btn_home_right);

        mBtnUser.setOnClickListener(this);
        mBtnVideo.setOnClickListener(this);
        mBtnSelect.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (TextUtils.isEmpty(TCUserMgr.getInstance().getUserToken())) {
            if (TCUtils.isNetworkAvailable(this) && TCUserMgr.getInstance().hasUser()) {
                TCUserMgr.getInstance().autoLogin(null);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_home_left:
                showVideoFragment();
                break;
            case R.id.btn_home_select:
                showSelect();
                break;
            case R.id.btn_home_right:
                showUserFragment();
                break;
        }
    }

    private void showSelect() {
        if (!TCUserMgr.getInstance().hasUser()) {
            Intent intent = new Intent(TCMainActivity.this, TCLoginActivity.class);
            startActivity(intent);
        } else {
            // 防止多次点击
            if (System.currentTimeMillis() - mLastClickPubTS > 1000) {
                mLastClickPubTS = System.currentTimeMillis();
                if (mShortVideoDialog.isAdded())
                    mShortVideoDialog.dismiss();
                else
                    mShortVideoDialog.show(getFragmentManager(), "");
            }
        }
    }

    private void showUserFragment() {
        mBtnVideo.setBackgroundResource(R.drawable.ic_home_video_normal);
        mBtnUser.setBackgroundResource(R.drawable.ic_user_selected);
        if (mTCUserInfoFragment == null) {
            mTCUserInfoFragment = new TCUserInfoFragment();
        }
        showFragment(mTCUserInfoFragment, "user_fragment");
    }

    private void showVideoFragment() {
        mBtnVideo.setBackgroundResource(R.drawable.ic_home_video_selected);
        mBtnUser.setBackgroundResource(R.drawable.ic_user_normal);
        if (mTCLiveListFragment == null) {
            mTCLiveListFragment = new TCLiveListFragment();
        }
        showFragment(mTCLiveListFragment, "live_list_fragment");
    }

    private void showFragment(Fragment fragment, String tag) {
        if (fragment == mCurrentFragment) return;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (mCurrentFragment != null) {
            transaction.hide(mCurrentFragment);
        }
        if (!fragment.isAdded()) {
            transaction.add(R.id.contentPanel, fragment, tag);
        } else {
            transaction.show(fragment);
        }
        mCurrentFragment = fragment;
        transaction.commit();
    }

}
