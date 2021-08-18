## 对接第三方 Demo 的 faceunity 模块

本工程是第三方 Demo 依赖的 faceunity 模块，每次升级 SDK 时会优先在这里改动，然后同步到各个第三方 Demo 中。

当前的 Nama SDK 版本是 **7.4.1.0**。

--------

## 集成方法

### 一、添加 SDK

将 faceunity 模块添加到工程中，下面是对库文件的说明。

- assets/sticker 文件夹下 \*.bundle 是特效贴纸文件。
- assets/makeup 文件夹下 \*.bundle 是美妆素材文件。
- com/faceunity/nama/authpack.java 是鉴权证书文件，必须提供有效的证书才能运行 Demo，请联系技术支持获取。

通过 Maven 依赖最新版 SDK，方便升级，推荐使用。

```java
allprojects {
    repositories {
        ...
        maven { url 'http://maven.faceunity.com/repository/maven-public/' }
        ...
  }
}
```
```java
dependencies {
...
implementation 'com.faceunity:core:7.4.1.0' // 实现代码
implementation 'com.faceunity:model:7.4.1.0' // 道具以及AI bundle
...
}
```

```java
dependencies {
...
implementation 'com.faceunity:nama:7.4.1.0' //底层库-标准版
implementation 'com.faceunity:nama-lite:7.4.1.0' //底层库-lite版
...
}
```

其中，AAR 包含以下内容：

```
    +libs                                  
      -nama.jar                        // JNI 接口
    +assets
      +graphic                         // 图形效果道具
        -body_slim.bundle              // 美体道具
        -controller.bundle             // Avatar 道具
        -face_beautification.bundle    // 美颜道具
        -face_makeup.bundle            // 美妆道具
        -fuzzytoonfilter.bundle        // 动漫滤镜道具
        -fxaa.bundle                   // 3D 绘制抗锯齿
        -tongue.bundle                 // 舌头跟踪数据包
      +model                           // 算法能力模型
        -ai_face_processor.bundle      // 人脸识别AI能力模型，需要默认加载
        -ai_face_processor_lite.bundle // 人脸识别AI能力模型，轻量版
        -ai_hand_processor.bundle      // 手势识别AI能力模型
        -ai_human_processor.bundle     // 人体点位AI能力模型
    +jni                               // CNama fuai 库
      +armeabi-v7a
        -libCNamaSDK.so
        -libfuai.so
      +arm64-v8a
        -libCNamaSDK.so
        -libfuai.so
      +x86
        -libCNamaSDK.so
        -libfuai.so
      +x86_64
        -libCNamaSDK.so
        -libfuai.so
```

如需指定应用的 so 架构，请修改 app 模块 build.gradle：

```groovy
android {
    // ...
    defaultConfig {
        // ...
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a'
        }
    }
}
```

如需剔除不必要的 assets 文件，请修改 app 模块 build.gradle：

```groovy
android {
    // ...
    applicationVariants.all { variant ->
        variant.mergeAssetsProvider.configure {
            doLast {
                delete(fileTree(dir: outputDir, includes: ['model/ai_face_processor_lite.bundle',
                                                           'model/ai_hand_processor.bundle',
                                                           'graphics/controller.bundle',
                                                           'graphics/fuzzytoonfilter.bundle',
                                                           'graphics/fxaa.bundle',
                                                           'graphics/tongue.bundle']))
            }
        }
    }
}
```


### 二、使用 SDK

#### 1. 初始化

调用 `FURenderer` 类的  `setup` 方法初始化 SDK，可以在工作线程调用，应用启动后仅需调用一次。

在TCVideoRecordActivity类中执行FURenderer.setup(this);根据是否需要开启美颜来决定是否调用方法。

#### 2.创建

在 `FaceUnityDataFactory` 类 的  `bindCurrentRenderer` 方法是对 FaceUnity SDK 每次使用前数据初始化的封装。

在 TCVideoRecordActivity 类中 设置 VideoCustomProcessListener回调方法，且在onTextureCustomProcess方法中执行。


```java
mTXCameraRecord.setVideoProcessListener(new TXUGCRecord.VideoCustomProcessListener() {

    @Override
    public int onTextureCustomProcess(int texId, int width, int height) {
        if (!mIsOpenFuBeauty) {
            return texId;
        }
        if (mIsFirstFrame) {
            mFaceUnityDataFactory.bindCurrentRenderer();
            initCsvUtil(TCVideoRecordActivity.this);
            mIsFirstFrame = false;
            openTime = System.currentTimeMillis();
            return 0;
        }
        return mFURenderer.onDrawFrameSingleTex(texId, width, height);
    }

    @Override
    public void onDetectFacePoints(float[] floats) {
    }

    @Override
    public void onTextureDestroyed() {
        if (mFURenderer != null) {
            mFURenderer.release();
        }
        mIsFirstFrame = true;
    }
});
```

#### 3. 图像处理

调用 `FURenderer` 类的  `onDrawFrameXXX` 方法进行图像处理，有许多重载方法适用于不同数据类型的需求。

在 TCVideoRecordActivity类中，实现VideoCustomProcessListener接口，在onTextureCustomProcess回调方法中执行美颜操作（代码见上一小节）。

onDrawFrameSingleTex 是单输入，输入图像纹理Id，输出纹理Id

腾讯短视频支持单输入： onDrawFrameSingleInput 

#### 4. 销毁

调用 `FURenderer` 类的  `release` 方法在 SDK 结束前释放占用的资源。

在 TCVideoRecordActivity 类中，实现VideoCustomProcessListener接口，在onTextureDestroyed回调方法中执行FURenderer.release();

#### 5. 切换相机

调用 `FURenderer` 类 的  `setCameraFacing` 方法，用于重新为 SDK 设置参数。

在 TCVideoRecordActivity 类，onClick()方法下 R.id.btn_switch_camera 控件的点击事件中执行

#### 6. 旋转手机

调用 `FURenderer` 类 的  `setDeviceOrientation` 方法，用于重新为 SDK 设置参数。

使用方法：TCVideoRecordActivity 中可见

```java
1.implements SensorEventListener
2. initFURender()    
mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

3.
@Override
protected void onDestroy() {
    super.onDestroy();
    // 清理相关资源
    if (null != mSensorManager) {
        mSensorManager.unregisterListener(this);
    }
}
4. 
//实现接口
@Override
public void onSensorChanged(SensorEvent event) {
    //具体代码见 TCVideoRecordActivity 类
}

```

**注意：** 上面一系列方法的使用，可以前往对应类查看，参考该代码示例接入即可。

### 三、接口介绍

- IFURenderer 是核心接口，提供了创建、销毁、渲染等接口。
- FaceUnityDataFactory 控制四个功能模块，用于功能模块的切换，初始化
- FaceBeautyDataFactory 是美颜业务工厂，用于调整美颜参数。
- PropDataFactory 是道具业务工厂，用于加载贴纸效果。
- MakeupDataFactory 是美妆业务工厂，用于加载美妆效果。
- BodyBeautyDataFactory 是美体业务工厂，用于调整美体参数。

关于 SDK 的更多详细说明，请参看 **[FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/)**。如有对接问题，请联系技术支持。