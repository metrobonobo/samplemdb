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

import android.os.Build;
import android.os.Handler;
import android.os.Message;

import com.pax.unattended.mdb.Cashless;
import com.pax.unattended.mdb.library.bean.BaseResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public class MdbManager {
    private boolean isDemoMode = false;
    private MyTimeTask queryReqTask;
    private Disposable disposable = null;
    private String mdbLibraryVer = "";

    private volatile boolean hasStopReq;
    private EventObserver mEventObserver;
    private String reqReset = "";
    private String reqFinishVending = "";
    private String reqDisableReader = "";
    private String reqEnableReader = "";
    private String reqStartTrans = "";
    private String reqRefund = "";
    private String reqDispenseSucc = "";
    private String reqDispenseFail = "";
    private String reqVmcInfo = "";
    private String reqEnableTerminateVending = "";
    private String currentState;
    private Cashless mCashless;
    private String reqId;

    private boolean needResponse;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            postEvent(MdbState.VMC_TERMINATE_TRANS);
        }
    };


    private MdbManager() {
    }

    public static MdbManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static String getProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            value = (String) (get.invoke(c, key, "unknown"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
        return value;
    }

    private static void setProperty(String key, String value) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method set = c.getMethod("set", String.class, String.class);
            set.invoke(c, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDevMode(boolean isDemoMode) {
        this.isDemoMode = isDemoMode;
    }

    public boolean init() {
        if (disposable != null) {
            releaseMdb();
        }
        mCashless = new Cashless();
        DebugLog.i("mdb init");
        int initRet = mCashless.init();
        if (initRet < 0) {
            return false;
        }
        disposable = Observable.interval(50, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.newThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        mdbLibraryVer = Cashless.getVersion();
                        DebugLog.i("mdb version:" + mdbLibraryVer + ", start get req name");
                        getReqStr();
                    }
                }).subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
//                        DebugLog.d("get req==>"+aLong + ", hasStopReq:"+hasStopReq);
                        if (hasStopReq) {
//                            DebugLog.w("do nothing");
                        } else {
//                            DebugLog.w("get Req...");
                            mdbProcess();
                        }

                    }
                });
        return true;
    }

    private void stopMdbReq() {
        hasStopReq = true;
    }

    private void getReqStr() {
        reqReset = Cashless.reqReset();
        reqFinishVending = Cashless.reqFinishVending();
        reqDisableReader = Cashless.reqDisableReader();
        reqEnableReader = Cashless.reqEnableReader();
        reqStartTrans = Cashless.reqTransaction();
        reqRefund = Cashless.reqRefund();
        reqDispenseSucc = Cashless.reqDispenseSuccess();
        reqDispenseFail = Cashless.reqDispenseFailure();
        reqVmcInfo = Cashless.reqVmcInfo();
        reqEnableTerminateVending = Cashless.reqEnableTerminateVending();
        DebugLog.w("reqReset->" + reqReset + ", reqFinishVending->" + reqFinishVending + ", reqDisableReader->" + reqDisableReader + ", reqEnableReader->" + reqEnableReader + ", reqStartTrans->" + reqStartTrans);
        DebugLog.w("reqRefund->" + reqRefund + ", reqDispenseSucc->" + reqDispenseSucc + ", reqDispenseFail->" + reqDispenseFail + ", reqVmcInfo->" + reqVmcInfo + ", reqEnableTerminateVending->" + reqEnableTerminateVending);

    }

    private void startQueryTerminateCmdTimer(final String reqId) {
        if (queryReqTask != null) {
            queryReqTask.stop();
            queryReqTask = null;
        }
        DebugLog.i("start jude if req terminated -> " + reqId);
        queryReqTask = new MyTimeTask(50, new TimerTask() {
            @Override
            public void run() {
                if (mCashless.userIsReqTerminated(reqId)) {
                    queryReqTask.stop();
                    DebugLog.e("get terminate cmd from vmc, current reqId:" + reqId);
                    mHandler.sendEmptyMessage(1);
                }
            }
        });
        queryReqTask.start();
    }

    private void mdbProcess() {
//        DebugLog.i("get REQ....");
        if (mCashless.userIsHasReq()) {
            needResponse = false;
            reqId = mCashless.userGetReqId();
            String reqStr = mCashless.userGetReq(reqId);
            DebugLog.d("req--->" + reqStr + ", reqId:" + reqId);


            if (reqReset.equalsIgnoreCase(reqStr)) { //1.启动ccTalk startPolling 2.点击setup Reader
                // 跟 VMC 请求中止交易 的处理一样
                // TODO 目前不处理RESET 和 Reader Disable
//                postEvent(MdbState.RESET);
                DebugLog.w("Vmc ask to reset,终止当前任务，停止检卡，并回到初始化状态和界面");
            } else if (reqDisableReader.equalsIgnoreCase(reqStr)) {//②点击了Setup Reader
                // 跟 VMC 请求中止交易 的处理一样
                // TODO 目前不处理RESET 和 Reader Disable
//                postEvent(MdbState.DISABLE_READER);
                DebugLog.w("Vmc ask to disable reader，如果应用层正在检卡，则停止检卡，等待VMC的检卡请求");
            } else if (reqEnableTerminateVending.equalsIgnoreCase(reqStr)) {//④  ⑦    //dispense succ 后 也会受到 reqEnableTerminateVending
                DebugLog.w("enable terminate vending...");
                needResponse = true;
                postEvent(MdbState.ALLOW_TERMINATE);
            } else {
                stopMdbReq();
                if (reqEnableReader.equalsIgnoreCase(reqStr)) {//③
                    needResponse = true;
                    //start detect card
                    postEvent(MdbState.READ_CARD);
                } else if (reqDispenseSucc.equalsIgnoreCase(reqStr)) {//⑥点击了vend success
                    //finish  current transaction, reset reader and emv
//                    stopMdbReq(); // 1106 2015
                    postEvent(MdbState.DISPENSE_SUCC);
                } else if (reqDispenseFail.equalsIgnoreCase(reqStr)) {//⑥点击了vend failure
//                    stopMdbReq(); // 1106 2015
                    postEvent(MdbState.DISPENSE_FAIL);
                    DebugLog.w("dispense failed");
                } else if (reqFinishVending.equalsIgnoreCase(reqStr)) {//⑧点击了session complete
                    DebugLog.w("交易完成 提示移卡 带走卡片 Say goodbye! 回到欢迎页面");
//                    stopMdbReq(); // 1106 2015
                    postEvent(MdbState.FINISH_VENDING);
                } else if (reqVmcInfo.equalsIgnoreCase(reqStr)) {//①收到复位请求和禁止读卡请求后，收到reqVmcInfo
                    JSONObject jo = null;
                    String vmcInfoJsonStr = null;
                    try {
                        vmcInfoJsonStr = mCashless.userGetReqData(reqId);
                        jo = new JSONObject(vmcInfoJsonStr);
                        int featureLevel = jo.getInt("VMC Feature Level");
                        String manufacturerCode = jo.getString("Manufacturer Code");
                        DebugLog.w("get Vmc Info\n"
                                + "VMC Feature Level: " + featureLevel + "\n"
                                + "Manufacturer Code: " + manufacturerCode + "\n"
                        );
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    postEvent(MdbState.DISPLAY_VMC_INFO, vmcInfoJsonStr);

                } else if (reqRefund.equalsIgnoreCase(reqStr)) {// 点击vend failure 后， 接着会收到refund
                    DebugLog.w("交易异常,start Refund...");
                    needResponse = true;
                    postEvent(MdbState.START_REFUND);
                } else if (reqStartTrans.equalsIgnoreCase(reqStr)) {//⑤ 点击了Vend Request
                    needResponse = true;
                    // start emv process
                    try {
                        String transJsonStr = mCashless.userGetReqData(reqId);
                        JSONObject jsonObject = new JSONObject(transJsonStr);
                        int itemPrice = jsonObject.getInt("Item Price");
                        int itemNumber = jsonObject.getInt("Item Number");
                        DebugLog.w("Start Transaction...\n"
                                + "Item Price: " + itemPrice + "\n"
                                + "Item Number: " + itemNumber + "\n");
                        postEvent(MdbState.START_TRANS, transJsonStr);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            //对有响应的请求  进行轮询查询 req是否被 VMC中止交易
            if (needResponse) {
                startQueryTerminateCmdTimer(reqId);
            }

        }
    }

    public String getMdbLibraryVersion(){
       return  mdbLibraryVer;
    }

    public void releaseMdb() {
        if (disposable != null) {
            if (disposable.isDisposed()) {
                disposable.dispose();
                disposable = null;
            }
        }
//        deleteInstance
    }

    private void postEvent(String req) {
        DebugLog.w("current req:" + currentState + ", new req:" + req);
        if (mEventObserver != null && !req.equals(currentState)) {
            currentState = req;
            mEventObserver.onStateChanged(req, null);
        }
    }

    private void postEvent(String req, String jsonDataStr) {
        DebugLog.w("current req:" + currentState + ", new req:" + req);
        if (mEventObserver != null && !req.equals(currentState)) {
            currentState = req;
            mEventObserver.onStateChanged(req, jsonDataStr);
        }
    }

    public void registerEventObserver(EventObserver observer) {
        mEventObserver = observer;
    }



    public void unregisterEventObserver(EventObserver observer) {
        mEventObserver = null;
    }

    public void setEventResult(BaseResult resultData) {
        DebugLog.i("set Result->" + resultData.getEventState() + ", result:" + resultData.getResultCode());
        if (queryReqTask != null) {
            queryReqTask.stop();
            queryReqTask = null;
        }
        if (!"IM30".equals(Build.MODEL) || isDemoMode) {
            return;
        }

        if (resultData.getResultCode() == -50) {//terminate by vmc
            DebugLog.e("REQ was terminated by VMC(Not need to update result to vmc)");
        } else {
            switch (resultData.getEventState()) {
                case MdbState.READ_CARD:
                    if (resultData.getResultCode() == 0) {
                        if (mCashless != null) {
                            try {
                                mCashless.userUpdateReqResult(reqId, new JSONObject().put("Funds Avaiable", resultData.getFundsAvailable()).toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } //检卡失败不返回结果，也不查询新请求。继续检卡就是了. 如果一直检卡不成功，应用层直接让用户退卡，继续检卡就行了。 检卡期间一直查询检卡请求是否被VMC终止就好了
                    break;
                case MdbState.START_TRANS:
                    //  收到扣款请求后，会实时查询userIsReqTerminated()，中途有中止指令，则EDC会reversal
                    try {
                        if (resultData.getResultCode() == 0) {

                            mCashless.userUpdateReqResult(reqId,
                                    new JSONObject().put("Vend Result", "Approved").put("Vend Amount", resultData.getVendAmt()).toString()
                            );

                        } else {
                            mCashless.userUpdateReqResult(reqId,
                                    new JSONObject().put("Vend Result", "Denied").toString()
                            );
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case MdbState.START_REFUND:
                    try {
                        if (resultData.getResultCode() == 0) {

                            mCashless.userUpdateReqResult(reqId,
                                    new JSONObject().put("Refund Result", "Success").toString()
                            );

                        } else {
                            mCashless.userUpdateReqResult(reqId,
                                    new JSONObject().put("Refund Result", "Failure").toString()
                            );
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case MdbState.ALLOW_TERMINATE:
                    if (resultData.getResultCode() == 0) {
                        mCashless.userUpdateReqResult(reqId,
                                "");

                    }
                    break;
                case MdbState.DISABLE_READER:
                case MdbState.DISPLAY_VMC_INFO:
                case MdbState.RESET:
                    break;
                case MdbState.DISPENSE_SUCC:
                case MdbState.DISPENSE_FAIL:
                case MdbState.FINISH_VENDING:
                    hasStopReq = false;
                    break;

            }
        }
        currentState = null;
        DebugLog.e("isDisconnected:" + mCashless.isDisconnected());
        hasStopReq = false;
    }

    public void setMdbEnable() {
        setProperty("persist.sys.mdb", "enable");
    }

    public String getMdbState() {
        return getProperty("persist.sys.mdb", "");
    }

    private static class LazyHolder {
        private static final MdbManager INSTANCE = new MdbManager();
    }

}
