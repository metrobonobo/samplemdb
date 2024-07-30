package com.pax.unattended.mdbdemo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.pax.unattended.mdb.library.DebugLog;
import com.pax.unattended.mdb.library.EventObserver;
import com.pax.unattended.mdb.library.MdbManager;
import com.pax.unattended.mdb.library.MdbState;
import com.pax.unattended.mdb.library.bean.BaseResult;
import com.pax.unattended.mdbdemo.utils.AppUtil;
import com.pax.unifiedsdk.factory.ITransAPI;
import com.pax.unifiedsdk.factory.TransAPIFactory;
import com.pax.unifiedsdk.message.BaseResponse;
import com.pax.unifiedsdk.message.RefundMsg;
import com.pax.unifiedsdk.message.SaleMsg;
import com.pax.unifiedsdk.message.ShowPageMsg;
import com.pax.unifiedsdk.sdkconstants.SdkConstants;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private MdbManager mdbManager;
    private TextView tvPrompts;
    private TextView tvVmcInfo;
    private TextView tvMdbVersion;
    private Button setEnableMdbBtn;
    private Button getEnableMdbBtn;
    private AlertDialog mPromptDlg;
    private AlertDialog mProgressDlg;
    private ITransAPI transAPI;
    private boolean onlyDetectCard;
    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if (mProgressDlg != null && mProgressDlg.isShowing()) {
                        mProgressDlg.dismiss();
                        mProgressDlg = null;
                    }
                    displayWarnDlg(getString(R.string.trans_success));
                    myHandler.sendEmptyMessageDelayed(2, 1 * 1000);
                    break;
                case 2:
                    if (mPromptDlg != null && mPromptDlg.isShowing()) {
                        mPromptDlg.dismiss();
                        mPromptDlg = null;
                    }
                    BaseResult result = new BaseResult();
                    result.setEventState((String) msg.obj);
                    result.setResultCode(0);
                    mdbManager.setEventResult(result);
                    break;

                case 3:
                    DebugLog.e("recv msg to disable reader");
                    VMCTerminateTrans(MdbState.DISABLE_READER);
                    break;
            }

        }
    };
    private EventObserver eventObserver = new EventObserver() {
        @Override
        public void onStateChanged(String req, String jsonDataStr) {
            dismissDlg();
            DebugLog.i("onStateChanged, state:" + req);
            switch (req) {
                case MdbState.READ_CARD:
                    startDetectCard();
                    break;
                case MdbState.START_TRANS:
                    startTrans(jsonDataStr);
                    break;
                case MdbState.FINISH_VENDING:
                    Log.d(TAG, "onStateChanged, end ");
                    vendSucc();
                    break;
                case MdbState.DISPENSE_SUCC:
                    dispenseSucc(req);
                    break;
                case MdbState.DISPENSE_FAIL:
                    dispenseFail(req);
                    break;
                case MdbState.START_REFUND:
                    startRefund();
                    break;
                case MdbState.VMC_TERMINATE_TRANS:
                case MdbState.DISABLE_READER:
                case MdbState.RESET:
                    VMCTerminateTrans(req);
                    break;
                case MdbState.ALLOW_TERMINATE:
                    displayAllowTerminalDlg();
                    break;
                case MdbState.DISPLAY_VMC_INFO:
                    showVMCInfo(req, jsonDataStr);
                    break;
            }
        }
    };

    private void showMdbVersionInfo(String version) {
        tvMdbVersion.setVisibility(View.VISIBLE);
        tvMdbVersion.setText("Mdb Version: " + version);
    }

    private void showVMCInfo(final String req, String jsonDataStr) {
        try {
            JSONObject jsonObject = new JSONObject(jsonDataStr);
            final int featureLevel = jsonObject.getInt("VMC Feature Level");
            final String manufacturerCode = jsonObject.getString("Manufacturer Code");
            MdbDemoApp.getApp().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvVmcInfo.setVisibility(View.VISIBLE);
                    tvVmcInfo.setText("VMC Feature Level:" + featureLevel + "\nManufacturer Code:" + manufacturerCode);
                    BaseResult result = new BaseResult();
                    result.setEventState(req);
                    result.setResultCode(0);
                    mdbManager.setEventResult(result);
                }

            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void dismissDlg() {
        if (mPromptDlg != null && mPromptDlg.isShowing()) {
            mPromptDlg.dismiss();
            mPromptDlg = null;
        }
    }

    private void displayAllowTerminalDlg() {

        MdbDemoApp.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismissDlg();
                if (mPromptDlg == null) {
                    AlertDialog.Builder mProgressDlgBuilder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog);
                    View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dlg_processing, null);
                    ((TextView) view.findViewById(R.id.tv_msg)).setText("Waiting for payment...");
                    view.findViewById(R.id.tv_content).setVisibility(View.VISIBLE);
                    ((TextView) view.findViewById(R.id.tv_content)).setText("Cancel transaction?");
                    mProgressDlgBuilder.setCancelable(false);
                    mProgressDlgBuilder.setView(view);
                    mProgressDlgBuilder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            BaseResult result = new BaseResult();
                            result.setEventState(MdbState.ALLOW_TERMINATE);
                            result.setResultCode(0);
                            mdbManager.setEventResult(result);
                        }
                    });
                    mProgressDlgBuilder.setPositiveButton(" ", null);
                    mPromptDlg = mProgressDlgBuilder.create();

                }
                if (!mPromptDlg.isShowing()) {
                    mPromptDlg.show();
                }
                final WindowManager.LayoutParams params = mPromptDlg.getWindow().getAttributes();
                params.width = 600;
                params.height = 400;
                mPromptDlg.getWindow().setAttributes(params);
                mPromptDlg.getWindow().setBackgroundDrawableResource(android.R.color.background_light);
            }
        });
    }

    private void VMCTerminateTrans(String reqStr) {
        // send broadcast to edc to terminate trans
        String type = "";
        switch (reqStr) {
            case MdbState.VMC_TERMINATE_TRANS:
                type = "VMC_TERMINATE_TRANS";
                break;
            case MdbState.DISABLE_READER:
                type = "DISABLE_READER";
                break;
            case MdbState.RESET:
                type = "RESET";
                break;
        }
        Intent intent = new Intent("com.pax.unattended.mdb.TERMINATE_CMD");
        intent.putExtra("TERMINATE_TYPE", type);
        sendBroadcast(intent);

    }

    private void startDetectCard() {
        SaleMsg.Request request = new SaleMsg.Request();
        request.setAmount(0);
        request.setCategory(SdkConstants.CATEGORY_SALE);
        Bundle bundle = new Bundle();
        onlyDetectCard = true;
        bundle.putBoolean("ONLY_DETECT_CARD", true);
        request.setExtraBundle(bundle);
        transAPI.startTrans(MainActivity.this, request);


//        SaleMsg.Request request = new SaleMsg.Request();
//        request.setAppId("1");
//        request.setAmount(Long.parseLong("1200"));
//         TransAPIFactory.createTransAPI(MainActivity.this).doTrans(request);
    }

    private void startTrans(String jsonDataStr) {
        SaleMsg.Request request = new SaleMsg.Request();
        try {
            JSONObject jsonObject = new JSONObject(jsonDataStr);
            int itemPrice = jsonObject.getInt("Item Price");
            int itemNumber = jsonObject.getInt("Item Number");
            request.setAmount(itemPrice);
            request.setCategory(SdkConstants.CATEGORY_SALE);
            Bundle bundle = new Bundle();
            bundle.putBoolean("ONLY_DETECT_CARD", false);
            request.setExtraBundle(bundle);
            onlyDetectCard = false;
            transAPI.startTrans(MainActivity.this, request);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void startRefund() {
        RefundMsg.Request request = new RefundMsg.Request();
        request.setAmount(0);
        request.setOrgRefNo("");
        request.setCategory(SdkConstants.CATEGORY_REFUND);
        request.setCurrencyCode("USD");
        transAPI.startTrans(MainActivity.this, request);
    }

    private void vendSucc() {
        Log.e(TAG, "vendSucc start ");

        ShowPageMsg.Request request = new ShowPageMsg.Request();
        request.setCategory(SdkConstants.CATEGORY_SHOW_PAGE);
        request.setAppId("");
        request.setPageName("remove_card");
        request.setTimeout(60);
        ShowPageMsg.Request.PageInfo pageInfo = new ShowPageMsg.Request.PageInfo();
        request.setPageInfo(pageInfo);
        transAPI.startTrans(MainActivity.this, request);
    }

    private void dispenseSucc(String req) {
        displayWarnDlg(req.replaceAll("_", " "));
    }

    private void dispenseFail(String req) {
        displayWarnDlg(req.replaceAll("_", " "));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG, "===onCreate=== ");
        AppUtil.syncIsDebug(this.getApplicationContext());
        initView();
        transAPI = TransAPIFactory.createTransAPI();
        if (!AppUtil.isIM30Model()) {
            tvVmcInfo.setVisibility(View.VISIBLE);
            tvVmcInfo.setText("This app should be installed in IM30");
            return;
        }
        reqFileRWPermission();

    }

    private void init() {
        if (mdbManager == null) {
            mdbManager = MdbManager.getInstance();
        }
        if (mdbManager.init()) {
            mdbManager.registerEventObserver(eventObserver);
        } else {
            tvPrompts.setText("Init Failed");
            tvPrompts.setTextColor(getResources().getColor(R.color.colorAccent));
        }
    }


    private void reqFileRWPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//API LEVEL 18
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "request permission start");
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
            } else {
                init();
                showMdbVersionInfo(mdbManager.getMdbLibraryVersion());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGranted = true;
        if (requestCode == 1001) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // 申请成功
                    isGranted = true;
                } else {
                    // 申请失败
                    isGranted = false;
                    break;
                }
            }
            if (!isGranted) {
                Log.e(TAG, "request permission failed");
            }
            reqFileRWPermission();

        }
    }

    private void initView() {
        tvVmcInfo = findViewById(R.id.txt_vmc_info);
        tvMdbVersion = findViewById(R.id.txt_mdb_ver);
        tvMdbVersion.setText("");
        tvVmcInfo.setText("");
        tvPrompts = findViewById(R.id.tv_prompts);
        tvPrompts.setText(R.string.welcome);
        setEnableMdbBtn = findViewById(R.id.btn_set_enable_mdb);
        getEnableMdbBtn = findViewById(R.id.btn_get_enable_mdb);
        Button devModeBtn = findViewById(R.id.btn_setDemoMode);
        Log.w(TAG, "initView,isDebug:" + AppUtil.isDebug());
//        if (AppUtil.isDebug()) {
//            setEnableMdbBtn.setVisibility(View.VISIBLE);
//            getEnableMdbBtn.setVisibility(View.VISIBLE);
//            findViewById(R.id.layout_demo).setVisibility(View.VISIBLE);
//            devModeBtn.setVisibility(View.VISIBLE);
//        }

//        findViewById(R.id.btn_start_service).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                TODO check permission
//                Intent startService = new Intent(MainActivity.this, TransmitService.class);
//                MainActivity.this.startService(startService);
//            }
//        });

        setEnableMdbBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mdbManager != null) {
                    mdbManager.setMdbEnable();
                    Toast.makeText(MainActivity.this, "Mdb is " + mdbManager.getMdbState(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        getEnableMdbBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mdbManager != null) {
                    Toast.makeText(MainActivity.this, "Mdb is " + mdbManager.getMdbState(), Toast.LENGTH_SHORT).show();

                }
            }
        });

        findViewById(R.id.btn_start_detect_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDetectCard();
            }
        });
        findViewById(R.id.btn_start_trans).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tansInfo = "{\n" +
                        "\t\t\"Item Price\": 1212,\n" +
                        "\t\t\"Item Number\":7890\n" +
                        "}";
                startTrans(tansInfo);
            }
        });
        findViewById(R.id.btn_refund).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRefund();
            }
        });
        findViewById(R.id.btn_finish_vending).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vendSucc();
            }
        });

        findViewById(R.id.btn_dispense_success).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispenseSucc(MdbState.DISPENSE_SUCC);
            }
        });
        findViewById(R.id.btn_dispense_fail).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispenseFail(MdbState.DISPENSE_FAIL);
            }
        });
        findViewById(R.id.btn_allow_terminate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayAllowTerminalDlg();
            }
        });

        findViewById(R.id.btn_terminate_trans).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "click VMC terminalte trans, it will terminate trans in 8s", Toast.LENGTH_SHORT).show();
                myHandler.sendEmptyMessageDelayed(3, 8000);

            }
        });

        devModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mdbManager.setDevMode(true);
            }
        });
    }

    private void displayWarnDlg(final String msg) {
        Message message = new Message();
        message.what = 2;
        message.obj = msg;
        myHandler.sendMessageDelayed(message, 3 * 1000);
        MdbDemoApp.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mPromptDlg == null) {
                    AlertDialog.Builder removeDlgBuilder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog);
                    removeDlgBuilder.setCancelable(false);
                    View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dlg_remove_card, null);
                    ((TextView) view.findViewById(R.id.tv_msg)).setText(msg);
                    removeDlgBuilder.setView(view);
                    mPromptDlg = removeDlgBuilder.create();

                }
                if (!mPromptDlg.isShowing()) {
                    mPromptDlg.show();
                }
                final WindowManager.LayoutParams params = mPromptDlg.getWindow().getAttributes();
                params.width = 600;
                params.height = 400;
                mPromptDlg.getWindow().setAttributes(params);
                mPromptDlg.getWindow().setBackgroundDrawableResource(android.R.color.background_light);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult,requestCode: " + requestCode + ", resultCode:" + resultCode + ",data:" + data);
        BaseResponse baseResponse = transAPI.onResult(requestCode, resultCode, data);
        int cmdType = 0;
        if (data != null) {
            cmdType = data.getIntExtra(SdkConstants.COMMAND_TYPE, 0);
            Log.w(TAG, "onActivityResult: cmdType:" + cmdType);
        }
        Log.w(TAG, "onActivityResult: baseResponse:" + baseResponse);
        if (baseResponse != null) {
            String result = "code:" + baseResponse.getRspCode() + ",msg:" + baseResponse.getRspMsg();
            String appId = baseResponse.getAppId();
            result = result + ",appId:" + appId + ",transNO:";
            //extra param
            Log.w(TAG, "onActivityResult, result:" + result);
            Bundle extraBundle = baseResponse.getExtraBundle();
            BaseResult rsp = new BaseResult();
            rsp.setResultCode(baseResponse.getRspCode());
            if (SdkConstants.SALE == cmdType) {

                if (onlyDetectCard) {
                    rsp.setEventState(MdbState.READ_CARD);
                    rsp.setFundsAvailable(-1);
                } else {
                    rsp.setEventState(MdbState.START_TRANS);
                    String amountStr = ((SaleMsg.Response) baseResponse).getAmount();
                    if (!TextUtils.isEmpty(amountStr)) {
                        rsp.setVendAmt(Integer.parseInt(amountStr));
                    }
                }

            } else if (SdkConstants.REFUND == cmdType) {
                rsp.setEventState(MdbState.START_REFUND);
            } else if (SdkConstants.SHOW_PAGE == cmdType) {
                rsp.setEventState(MdbState.FINISH_VENDING);
            }

            Log.w(TAG, "set Result to MdbManager");
            mdbManager.setEventResult(rsp);

        } else {//UnifiedAPI does not process ShowPage Response
            if (SdkConstants.SHOW_PAGE == cmdType) {
                BaseResult rsp = new BaseResult();
                rsp.setResultCode(0);
                rsp.setEventState(MdbState.FINISH_VENDING);
                mdbManager.setEventResult(rsp);
            }
        }


    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "===onResume=== ");
        super.onResume();
    }

    @Override
    protected void onStart() {
        Log.e(TAG, "===onStart=== ");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.e(TAG, "===onStop=== ");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "===onPause=== ");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "===onDestroy=== ");
        if (mdbManager != null) {
            mdbManager.releaseMdb();
            mdbManager.unregisterEventObserver(eventObserver);
        }

    }
}
