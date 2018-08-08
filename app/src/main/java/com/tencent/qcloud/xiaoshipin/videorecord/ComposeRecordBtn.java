package com.tencent.qcloud.xiaoshipin.videorecord;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.tencent.qcloud.xiaoshipin.R;


/**
 * Created by vinsonswang on 2017/9/8.
 * 按住拍的录制按钮
 */

public class ComposeRecordBtn extends RelativeLayout implements View.OnTouchListener{
    private Context mContext;
    private ImageView mIvRecordRing;
    private ImageView mIvRecordStart;
    private ImageView mIvRecordPause;
    private IRecordButtonListener mIRecordButtonListener;

    public interface IRecordButtonListener{
        void onButtonStart();
        void onButtonPause();
    }

    public void setOnRecordButtonListener(IRecordButtonListener iRecordButtonListener){
        mIRecordButtonListener = iRecordButtonListener;
    }

    public ComposeRecordBtn(Context context) {
        super(context);
        init(context);
    }

    public ComposeRecordBtn(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ComposeRecordBtn(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.compose_record_btn, this);
        mIvRecordRing = (ImageView) findViewById(R.id.iv_record_ring);
        mIvRecordStart = (ImageView) findViewById(R.id.iv_record);
        mIvRecordPause = (ImageView) findViewById(R.id.iv_record_pause);
        setOnTouchListener(this);
    }

    public void startRecord(){
        ObjectAnimator recordRingZoomOutXAn = ObjectAnimator.ofFloat(mIvRecordRing, "scaleX", 0.8f);
        ObjectAnimator recordRingZoomOutYAn = ObjectAnimator.ofFloat(mIvRecordRing, "scaleY", 0.8f);
        mIvRecordRing.setPivotX(mIvRecordRing.getMeasuredWidth() / 2);
        mIvRecordRing.setPivotY(mIvRecordRing.getMeasuredHeight() / 2);
        mIvRecordRing.invalidate();//显示的调用invalidate

        ObjectAnimator recordStartZoomOutXAn = ObjectAnimator.ofFloat(mIvRecordStart, "scaleX", 0.8f);
        ObjectAnimator recordStartZoomOutYAn = ObjectAnimator.ofFloat(mIvRecordStart, "scaleY", 0.8f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(80);
        animatorSet.setInterpolator(new LinearInterpolator());
        animatorSet.play(recordRingZoomOutXAn).with(recordRingZoomOutYAn).with(recordStartZoomOutXAn).with(recordStartZoomOutYAn);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if(mIRecordButtonListener != null){
                    mIRecordButtonListener.onButtonStart();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();

        mIvRecordPause.setVisibility(View.VISIBLE);
        mIvRecordRing.setImageResource(R.drawable.ugc_record_ring_light);
    }

    public void pauseRecord(){
        ObjectAnimator recordRingZoomInXAn = ObjectAnimator.ofFloat(mIvRecordRing, "scaleX", 1f);
        ObjectAnimator recordRingZoomIntYAn = ObjectAnimator.ofFloat(mIvRecordRing, "scaleY", 1f);
        mIvRecordRing.setPivotX(mIvRecordRing.getMeasuredWidth() / 2);
        mIvRecordRing.setPivotY(mIvRecordRing.getMeasuredHeight() / 2);
        mIvRecordRing.invalidate();//显示的调用invalidate

        ObjectAnimator recordStartZoomInXAn = ObjectAnimator.ofFloat(mIvRecordStart, "scaleX", 1f);
        ObjectAnimator recordStartZoomInYAn = ObjectAnimator.ofFloat(mIvRecordStart, "scaleY", 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(80);
        animatorSet.setInterpolator(new LinearInterpolator());
        animatorSet.play(recordRingZoomInXAn).with(recordRingZoomIntYAn).with(recordStartZoomInXAn).with(recordStartZoomInYAn);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if(mIRecordButtonListener != null){
                    mIRecordButtonListener.onButtonPause();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();

        mIvRecordPause.setVisibility(View.GONE);
        mIvRecordRing.setImageResource(R.drawable.ugc_record_ring_gray);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                startRecord();
                break;

            case MotionEvent.ACTION_UP:
                pauseRecord();
                break;
        }
        return true;
    }
}
