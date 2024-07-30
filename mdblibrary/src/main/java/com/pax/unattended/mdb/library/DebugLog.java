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
 * 2019/7/3  	         Qinny Zhou           	Create/Add/Modify/Delete
 * ===========================================================================================
 */
package com.pax.unattended.mdb.library;

import android.util.Log;
import com.pax.unattended.mdblibrary.BuildConfig;


public class DebugLog {
    private static final String TAG = "MdbManager";
    private static boolean isDebug = BuildConfig.DEBUG;

    public static void setDebug(boolean enable){
          isDebug = enable;
    }

    public static void d(String msg) {
        if (!isDebug) {
            return;
        }
        Log.d(TAG, msg);
    }

    public static void i(String msg) {
        if (!isDebug) {
            return;
        }
        Log.i(TAG, msg);
    }

    public static void w(String msg) {
        if (!isDebug) {
            return;
        }
        Log.w(TAG, msg);
    }


    public static void e(String msg) {
        if (!isDebug) {
            return;
        }
        Log.e(TAG, msg);
    }



}
