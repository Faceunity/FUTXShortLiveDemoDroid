package com.tencent.qcloud.xiaoshipin.common.utils;


/**
 * 静态函数
 */
public class TCConstants {

    //小视频相关配置请参考:https://www.qcloud.com/document/product/454/7999
    //************在腾讯云开通各项服务后，将您的配置替换到如下的几个定义中************
    //业务Server的地址
    public static final String APP_SVR_URL = "http://demo.vod2.myqcloud.com/lite/"; //如果您的服务器没有部署https证书，这里需要用http
//    public static final String APP_SVR_URL = "http://119.29.189.135/lite"; //测试

    public static final String SVR_POST_URL = "https://livedemo.tim.qq.com/interface.php";
    public static final String DEFAULT_ELK_HOST = "http://123.206.118.43";
    // BGM列表地址
    public static final String SVR_BGM_GET_URL = "http://bgm-1252463788.cosgz.myqcloud.com/bgm_list.json";

    //设置第三方平台的appid和appsecrect，大部分平台进行分享操作需要在第三方平台创建应用并提交审核，通过后拿到appid和appsecrect并填入这里，具体申请方式请参考http://dev.umeng.com/social/android/operation
    //有关友盟组件更多资料请参考这里：http://dev.umeng.com/social/android/quick-integration
    public static final String WEIXIN_SHARE_ID = "";
    public static final String WEIXIN_SHARE_SECRECT = "";

    public static final String SINA_WEIBO_SHARE_ID = "";
    public static final String SINA_WEIBO_SHARE_SECRECT = "";
    public static final String SINA_WEIBO_SHARE_REDIRECT_URL = "http://sns.whalecloud.com/sina2/callback";

    public static final String QQZONE_SHARE_ID = "";
    public static final String QQZONE_SHARE_SECRECT = "";

    //bugly组件Appid，bugly为腾讯提供的用于App Crash收集和分析的组件
    public static final String BUGLY_APPID = "0";
    //**********************************************************************

    /**
     * 常量字符串
     */
    public static final String USER_INFO        = "user_info";
    public static final String USER_ID          = "user_id";
    public static final String USER_SIG         = "user_sig";
    public static final String USER_NICK        = "user_nick";
    public static final String USER_SIGN        = "user_sign";
    public static final String USER_HEADPIC     = "user_headpic";
    public static final String USER_COVER       = "user_cover";
    public static final String USER_LOC         = "user_location";
    public static final String SVR_RETURN_CODE  = "returnValue";
    public static final String SVR_RETURN_MSG   = "returnMsg";
    public static final String SVR_RETURN_DATA  = "returnData";

    //主播退出广播字段
    public static final String EXIT_APP         = "EXIT_APP";

    public static final String PUBLISH_URL      = "publish_url";
    public static final String ROOM_ID          = "room_id";
    public static final String ROOM_TITLE       = "room_title";
    public static final String COVER_PIC        = "cover_pic";
    public static final String BITRATE          = "bitrate";
    public static final String GROUP_ID         = "group_id";
    public static final String PLAY_URL         = "play_url";
    public static final String PLAY_TYPE        = "play_type";
    public static final String PUSHER_AVATAR    = "pusher_avatar";
    public static final String PUSHER_ID        = "pusher_id";
    public static final String PUSHER_NAME        = "pusher_name";
    public static final String MEMBER_COUNT     = "member_count";
    public static final String HEART_COUNT      = "heart_count";
    public static final String FILE_ID          = "file_id";
    public static final String TIMESTAMP        = "timestamp";
    public static final String ACTIVITY_RESULT  = "activity_result";
    public static final String SHARE_PLATFORM   = "share_platform";

    public static final String TCLIVE_INFO_LIST = "txlive_info_list";
    public static final String TCLIVE_INFO_POSITION = "txlive_info_position";

    public static final String CMD_KEY          = "userAction";
    public static final String DANMU_TEXT       = "actionParam";

    public static final String NOTIFY_QUERY_USERINFO_RESULT = "notify_query_userinfo_result";

    /**
     * UGC 编辑的的参数
     */
    public static final String VIDEO_EDITER_PATH = "key_video_editer_path"; // 路径的key

    public static final String RECORD_CONFIG_MAX_DURATION = "record_config_max_duration";
    public static final String RECORD_CONFIG_MIN_DURATION = "record_config_min_duration";
    public static final String RECORD_CONFIG_ASPECT_RATIO = "record_config_aspect_ratio";
    public static final String RECORD_CONFIG_RECOMMEND_QUALITY = "record_config_recommend_quality";
    public static final String RECORD_CONFIG_HOME_ORIENTATION = "record_config_home_orientation";
    public static final String RECORD_CONFIG_RESOLUTION = "record_config_resolution";
    public static final String RECORD_CONFIG_BITE_RATE = "record_config_bite_rate";
    public static final String RECORD_CONFIG_FPS = "record_config_fps";
    public static final String RECORD_CONFIG_GOP = "record_config_gop";
    public static final String RECORD_CONFIG_NEED_EDITER = "record_config_go_editer";

    /**
     * UGC小视频录制信息
     */
    public static final String VIDEO_RECORD_TYPE                = "type";
    public static final String VIDEO_RECORD_RESULT              = "result";
    public static final String VIDEO_RECORD_DESCMSG             = "descmsg";
    public static final String VIDEO_RECORD_VIDEPATH            = "path";
    public static final String VIDEO_RECORD_COVERPATH           = "coverpath";
    public static final String VIDEO_RECORD_ROTATION            = "rotation";
    public static final String VIDEO_RECORD_NO_CACHE            = "nocache";
    public static final String VIDEO_RECORD_DURATION            =  "duration";
    public static final String VIDEO_RECORD_RESOLUTION          = "resolution";
    public static final String VIDEO_RECORD_AUDIO_SAMPLE_RATE_TYPE = "audio_sample_rate"; // 音频采样率

    public static final int VIDEO_RECORD_TYPE_PUBLISH       = 1;   // 推流端录制
    public static final int VIDEO_RECORD_TYPE_PLAY          = 2;   // 播放端录制
    public static final int VIDEO_RECORD_TYPE_UGC_RECORD    = 3;   // 短视频录制
    public static final int VIDEO_RECORD_TYPE_EDIT          = 4;   // 短视频编辑
    public static final int VIDEO_RECORD_TYPE_FOLLOW_SHOT   = 5;   // 短视频合拍

    public static final String OUTPUT_DIR_NAME = "TXUGC";

    /**
     * 用户可见的错误提示语
     */
    public static final String ERROR_MSG_NET_DISCONNECTED    = "网络异常，请检查网络";

    //网络类型
    public static final int NETTYPE_NONE = 0;
    public static final int NETTYPE_WIFI = 1;
    public static final int NETTYPE_4G   = 2;
    public static final int NETTYPE_3G   = 3;
    public static final int NETTYPE_2G   = 4;

    // UGCEditer
    public static final String ACTION_UGC_SINGLE_CHOOSE  = "com.tencent.qcloud.xiaozhibo.single";
    public static final String ACTION_UGC_MULTI_CHOOSE   = "com.tencent.qcloud.xiaozhibo.multi";
    public static final String INTENT_KEY_MULTI_PIC_CHOOSE = "multi_pic";
    public static final String INTENT_KEY_MULTI_PIC_LIST = "pic_list"; // 图片列表
    public static final String INTENT_KEY_SINGLE_CHOOSE  = "single_video";
    public static final String INTENT_KEY_MULTI_CHOOSE   = "multi_video";

    public static final String DEFAULT_MEDIA_PACK_FOLDER = "txrtmp";      // UGC编辑器输出目录
    public static final int THUMB_COUNT = 10;

    public static final int MALE    = 0;
    public static final int FEMALE  = 1;

    // bgm activity request code and intent extra
    public static final int ACTIVITY_BGM_REQUEST_CODE = 1;
    public static final String BGM_POSITION = "bgm_position";
    public static final String BGM_PATH = "bgm_path";

    public static final int ACTIVITY_OTHER_REQUEST_CODE = 2;

    public static final String KEY_FRAGMENT = "fragment_type";
    public static final int TYPE_EDITER_BGM = 1;
    public static final int TYPE_EDITER_MOTION = 2;
    public static final int TYPE_EDITER_SPEED = 3;
    public static final int TYPE_EDITER_FILTER = 4;
    public static final int TYPE_EDITER_PASTER = 5;
    public static final int TYPE_EDITER_SUBTITLE = 6;

    // 短视频licence名称
    public static final String UGC_LICENCE_NAME = "TXUgcSDK.licence";

    // ELK上报事件
    public static final String ELK_ACTION_START_UP = "startup";
    public static final String ELK_ACTION_STAY_TIME = "staytime";
    public static final String ELK_ACTION_PICTURE_EDIT = "pictureedit";
    public static final String ELK_ACTION_REGISTER = "register";
    public static final String ELK_ACTION_INSTALL = "install";
    public static final String ELK_ACTION_LOGIN = "login";
    public static final String ELK_ACTION_VIDEO_EDIT = "videoedit";
    public static final String ELK_ACTION_VIDEO_JOINER = "videojoiner";
    public static final String ELK_ACTION_VIDEO_SIGN = "videosign";
    public static final String ELK_ACTION_VIDEO_UPLOAD_VOD = "videouploadvod";
    public static final String ELK_ACTION_VIDEO_UPLOAD_SERVER = "videouploadserver";
    public static final String ELK_ACTION_START_RECORD = "startrecord";
    public static final String ELK_ACTION_VIDEO_RECORD = "videorecord";
    public static final String ELK_ACTION_VOD_PLAY = "vodplay";
    public static final String ELK_ACTION_VIDEO_CHORUS = "videochorus";

    // 合唱演示视频地址
    public static final String CHORUS_URL = "http://1400100725.vod2.myqcloud.com/8b7d5993vodgzp1400100725/d864a3545285890780576877210/ss2W2I8oIn4A.mp4";
//    public static final String
}
