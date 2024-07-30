package com.pax.unattended.mdb;

public class Cashless {
    static {
        System.loadLibrary("Cashless");
    }

	private native final static long newInstance();
	private native final void deleteInstance(long handle);
	long handle = 0;
	public Cashless()
    {
        handle = newInstance();     
    }
	public void close()
    {
        deleteInstance(handle);
        handle = 0;
    }

	//version
	public native static String getVersion();
	
    //req
    public native static String reqReset();
    public native static String reqVmcInfo();
    public native static String reqDisableReader();
    public native static String reqEnableReader();
    public native static String reqTransaction();
    public native static String reqRefund();
    public native static String reqDispenseSuccess();
    public native static String reqDispenseFailure();
    public native static String reqFinishVending();
    public native static String reqEnableTerminateVending();

    public native int init();
    public native boolean userIsHasReq();
    public native String userGetReqId();
    public native String userGetReq(String reqId);
    public native String userGetReqData(String reqId);
    public native int userUpdateReqResult(String reqId,String result);
    public native boolean userIsReqTerminated(String reqId);

    public native boolean isDisconnected();
}

