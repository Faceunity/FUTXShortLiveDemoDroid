package com.tencent.qcloud.xiaoshipin.manager;

import android.content.ContentResolver;
import android.content.Context;

import com.tencent.ugc.TXUGCBase;
import com.tencent.xmagic.XMagicImpl;

public class LicenseManager {
    private static String ugcLicenceUrl = "http://download-1252463788.cossh.myqcloud.com/xiaoshipin/licence_android/RDM_Enterprise.license";
    private static String ugcKey = "9bc74ac7bfd07ea392e8fdff2ba5678a";

    private static String xmagicAuthLicenceUrl = "http://download-1252463788.cossh.myqcloud.com/xiaoshipin/licence_android/RDM_Enterprise.license";
    private static String xmagicAuthKey = "9bc74ac7bfd07ea392e8fdff2ba5678a";

    public static void setUgcLicense(Context context) {
        TXUGCBase.getInstance().setLicence(context, ugcLicenceUrl, ugcKey);
    }

    public static void setXMagicLicense() {
        XMagicImpl.setXmagicAuthKeyAndUrl(xmagicAuthLicenceUrl, xmagicAuthKey);
    }

    public static void setLicense(Context context) {
        TXUGCBase.getInstance().setLicence(context, ugcLicenceUrl, ugcKey);
        XMagicImpl.setXmagicAuthKeyAndUrl(xmagicAuthLicenceUrl, xmagicAuthKey);
    }
}
