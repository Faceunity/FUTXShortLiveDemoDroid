package com.faceunity.nama;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import com.faceunity.core.callback.OperateCallback;
import com.faceunity.core.entity.FURenderInputData;
import com.faceunity.core.entity.FURenderOutputData;
import com.faceunity.core.enumeration.CameraFacingEnum;
import com.faceunity.core.enumeration.FUAIProcessorEnum;
import com.faceunity.core.enumeration.FUAITypeEnum;
import com.faceunity.core.enumeration.FUTransformMatrixEnum;
import com.faceunity.core.faceunity.FUAIKit;
import com.faceunity.core.faceunity.FURenderConfig;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.faceunity.FURenderManager;
import com.faceunity.core.utils.CameraUtils;
import com.faceunity.core.utils.FULogger;
import com.faceunity.nama.listener.FURendererListener;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * DESC：
 * Created on 2021/4/26
 */
public class FURenderer extends IFURenderer {


    public volatile static FURenderer INSTANCE;

    public static FURenderer getInstance() {
        if (INSTANCE == null) {
            synchronized (FURenderer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FURenderer();
                    INSTANCE.mFURenderKit = FURenderKit.getInstance();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 状态回调监听
     */
    private FURendererListener mFURendererListener;


    /* 特效FURenderKit*/
    private FURenderKit mFURenderKit = FURenderKit.getInstance();
    /* 特效FUAIKit*/
    private FUAIKit mFUAIKit = FUAIKit.getInstance();

    /* AI道具*/
    private String BUNDLE_AI_FACE = "model" + File.separator + "ai_face_processor.bundle";
    private String BUNDLE_AI_HUMAN = "model" + File.separator + "ai_human_processor.bundle";

    /* 相机角度-朝向映射 */
    private HashMap<CameraFacingEnum, Integer> cameraOrientationMap = new HashMap<>();

    /*检测类型*/
    private FUAIProcessorEnum aIProcess = FUAIProcessorEnum.FACE_PROCESSOR;
    /*检测标识*/
    private int aIProcessTrackStatus = -1;

    public String getVersion() {
        return mFURenderKit.getVersion();
    }

    /**
     * 初始化鉴权
     *
     * @param context
     */
    @Override
    public void setup(Context context) {
        FURenderManager.setKitDebug(FULogger.LogLevel.TRACE);
        FURenderManager.setCoreDebug(FULogger.LogLevel.ERROR);
        FURenderManager.registerFURender(context, authpack.A(), new OperateCallback() {
            @Override
            public void onSuccess(int i, @NotNull String s) {
                if (i == FURenderConfig.OPERATE_SUCCESS_AUTH) {
                    mFUAIKit.loadAIProcessor(BUNDLE_AI_FACE, FUAITypeEnum.FUAITYPE_FACEPROCESSOR);
                    mFUAIKit.loadAIProcessor(BUNDLE_AI_HUMAN, FUAITypeEnum.FUAITYPE_HUMAN_PROCESSOR);
                    int cameraFrontOrientation = CameraUtils.INSTANCE.getCameraOrientation(Camera.CameraInfo.CAMERA_FACING_FRONT);
                    int cameraBackOrientation = CameraUtils.INSTANCE.getCameraOrientation(Camera.CameraInfo.CAMERA_FACING_BACK);
                    cameraOrientationMap.put(CameraFacingEnum.CAMERA_FRONT, cameraFrontOrientation);
                    cameraOrientationMap.put(CameraFacingEnum.CAMERA_BACK, cameraBackOrientation);
                }
            }

            @Override
            public void onFail(int i, @NotNull String s) {
            }
        });
    }

    /**
     * 开启合成状态
     */
    @Override
    public void bindListener(FURendererListener mFURendererListener) {
        this.mFURendererListener = mFURendererListener;
    }


    /**
     * 单输入接口， 必须在具有 GL 环境的线程调用
     *
     * @param texId  纹理 ID
     * @param width  宽
     * @param height 高
     * @return
     */
    @Override
    public int onDrawFrameSingleTex(int texId, int width, int height) {
        prepareDrawFrame();
        FURenderInputData inputData = new FURenderInputData(width, height);
        inputData.setTexture(new FURenderInputData.FUTexture(inputTextureType, texId));
        FURenderInputData.FURenderConfig config = inputData.getRenderConfig();
        config.setExternalInputType(externalInputType);
        config.setInputOrientation(180);
        config.setDeviceOrientation(deviceOrientation);
        config.setInputBufferMatrix(inputBufferMatrix);
        config.setInputTextureMatrix(inputTextureMatrix);
        config.setOutputMatrix(outputMatrix);
        config.setCameraFacing(cameraFacing);
        FURenderOutputData outputData = mFURenderKit.renderWithInput(inputData);
        if (outputData.getTexture() != null && outputData.getTexture().getTexId() > 0) {
            return outputData.getTexture().getTexId();
        }
        return texId;
    }


    /**
     * 释放资源
     */
    @Override
    public void release() {
        mFURenderKit.release();
        aIProcessTrackStatus = -1;
        mFURendererListener = null;
    }


    /**
     * 渲染前置执行
     *
     * @return
     */
    private void prepareDrawFrame() {
        benchmarkFPS();
        // AI检测
        trackStatus();
    }

    //region AI识别


    /**
     * 设置输入数据朝向
     *
     * @param cameraFacing
     */
    @Override
    public void setCameraFacing(CameraFacingEnum cameraFacing) {
        if (cameraOrientationMap.containsKey(cameraFacing)) {
            int cameraOrientation = cameraOrientationMap.get(cameraFacing);
            setInputOrientation(cameraOrientation);
        }
        super.setCameraFacing(cameraFacing);
    }

    @Override
    public void setDeviceOrientation(int deviceOrientation) {
        if (cameraFacing == CameraFacingEnum.CAMERA_FRONT) {
            if (deviceOrientation == 0 || deviceOrientation == 180) {
                deviceOrientation = (deviceOrientation + 180) % 360;
            }
            super.setDeviceOrientation(deviceOrientation);
        }else {
            super.setDeviceOrientation(deviceOrientation);
        }
    }

    /**
     * 设置检测类型
     *
     * @param type
     */
    @Override
    public void setAIProcessTrackType(FUAIProcessorEnum type) {
        aIProcess = type;
        aIProcessTrackStatus = -1;
    }

    /**
     * 设置FPS检测
     *
     * @param enable
     */
    @Override
    public void setMarkFPSEnable(boolean enable) {
        mIsRunBenchmark = enable;
    }


    /**
     * AI识别数目检测
     */
    private void trackStatus() {
        int trackCount;
        if (aIProcess == FUAIProcessorEnum.HAND_GESTURE_PROCESSOR) {
            trackCount = mFURenderKit.getFUAIController().handProcessorGetNumResults();
        } else if (aIProcess == FUAIProcessorEnum.HUMAN_PROCESSOR) {
            trackCount = mFURenderKit.getFUAIController().humanProcessorGetNumResults();
        } else {
            trackCount = mFURenderKit.getFUAIController().isTracking();
        }
        if (trackCount != aIProcessTrackStatus) {
            aIProcessTrackStatus = trackCount;
        } else {
            return;
        }
        if (mFURendererListener != null) {
            mFURendererListener.onTrackStatusChanged(aIProcess, trackCount);
        }
    }
    //endregion AI识别

    //------------------------------FPS 渲染时长回调相关定义------------------------------------

    private static final int NANO_IN_ONE_MILLI_SECOND = 1_000_000;
    private static final int NANO_IN_ONE_SECOND = 1_000_000_000;
    private static final int FRAME_COUNT = 20;
    private boolean mIsRunBenchmark = false;
    private int mCurrentFrameCount;
    private long mLastFrameTimestamp;
    private long mSumCallTime;
    private long mCallStartTime;

    private void benchmarkFPS() {
        if (!mIsRunBenchmark) {
            return;
        }
        if (++mCurrentFrameCount == FRAME_COUNT) {
            long tmp = System.nanoTime();
            double fps = (double) NANO_IN_ONE_SECOND / ((double) (tmp - mLastFrameTimestamp) / FRAME_COUNT);
            double renderTime = (double) mSumCallTime / FRAME_COUNT / NANO_IN_ONE_MILLI_SECOND;
            mLastFrameTimestamp = tmp;
            mSumCallTime = 0;
            mCurrentFrameCount = 0;

            if (mFURendererListener != null) {
                mFURendererListener.onFpsChanged(fps, renderTime);
            }
        }
    }


}
