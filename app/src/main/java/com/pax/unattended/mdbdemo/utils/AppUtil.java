/*
 * ===========================================================================================
 * = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or nondisclosure
 *   agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *   disclosed except in accordance with the terms in that agreement.
 *     Copyright (C) $YEAR-? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 * Description: // Detail description about the function of this module,
 *             // interfaces with the other modules, and dependencies.
 * Revision History:
 * Date	                 Author	                Action
 * 2019/8/28  	         Qinny Zhou           	Create/Add/Modify/Delete
 * ===========================================================================================
 */
package com.pax.unattended.mdbdemo.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;


public class AppUtil {

    /**
     * BuildConfig.java 是编译时自动生成的，并且每个 Module 都会生成一份，以该 Module 的 packageName 为 BuildConfig.java 的 packageName。所以如果你的应用有多个 Module 就会有多个 BuildConfig.java 生成，而上面的 Lib Module import 的是自己的 BuildConfig.java，编译时被依赖的 Module 默认会提供 Release 版给其他 Module 或工程使用，这就导致该 BuildConfig.DEBUG 会始终为 true。
     */
    private static  Boolean isDebug = null;

    public static void syncIsDebug(Context context){
        if (isDebug == null) {
            isDebug = context.getApplicationInfo() != null &&
                    (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        }
    }


    public static boolean isDebug(){
        return isDebug == null?false:isDebug.booleanValue();
    }

    private static String getDeviceModel(){
        return Build.MODEL;
    }

    public static boolean isIM30Model(){
        if ("IM30".equalsIgnoreCase(getDeviceModel())) {
            return true;
        }
        return false;
    }

}
