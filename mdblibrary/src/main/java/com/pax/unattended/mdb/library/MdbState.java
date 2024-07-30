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
 * 2019/11/4  	         Qinny Zhou           	Create/Add/Modify/Delete
 * ===========================================================================================
 */
package com.pax.unattended.mdb.library;

public class MdbState {
    public static final String READ_CARD = "enable_reader";
    public static final String START_TRANS = "transaction";
    public static final String START_REFUND = "refund";
    public static final String DISPENSE_SUCC = "dispense_success";
    public static final String DISPENSE_FAIL = "dispense_failure";
    public static final String FINISH_VENDING = "finish_vending";
    public static final String DISPLAY_VMC_INFO = "vmc_info";
    public static final String DISABLE_READER = "disable_reader";
    public static final String RESET = "reset";
    public static final String ALLOW_TERMINATE = "enable_terminate_vending";
    //API
    public static final String VMC_TERMINATE_TRANS = "userIsReqTerminated";

//    @StringDef({REQ.READ_CARD, REQ.START_TRANS, REQ.START_REFUND, REQ.DISPENSE_SUCC, REQ.DISPENSE_FAIL, REQ.FINISH_VENDING, REQ.VMC_TERMINATE_TRANS, REQ.ALLOW_TERMINATE, REQ.DISPLAY_VMC_INFO, REQ.DISABLE_READER, REQ.RESET,REQ.DISPLAY_MDB_VERSION})
//    @Retention(RetentionPolicy.SOURCE)
//    public @interface REQ {
//        //Req
//        public static final String READ_CARD = "enable_reader";
//        public static final String START_TRANS = "transaction";
//        public static final String START_REFUND = "refund";
//        public static final String DISPENSE_SUCC = "dispense_success";
//        public static final String DISPENSE_FAIL = "dispense_failure";
//        public static final String FINISH_VENDING = "finish_vending";
//        public static final String DISPLAY_VMC_INFO = "vmc_info";
//        public static final String DISABLE_READER = "disable_reader";
//        public static final String RESET = "reset";
//        public static final String ALLOW_TERMINATE = "enable_terminate_vending";
//        //API
//        public static final String VMC_TERMINATE_TRANS = "userIsReqTerminated";
//        public static final String DISPLAY_MDB_VERSION = "mdb_version";
//    }


}
