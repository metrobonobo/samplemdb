package com.pax.unattended.mdb.library.bean;
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

import com.pax.unattended.mdb.library.MdbState;

public class BaseResult {
    private int resultCode;
    private String eventState;
    private int fundsAvailable = -1;
    private int vendAmt;

    public BaseResult() {

    }

    public String getEventState() {
        return eventState;
    }


    /**
     * set Original EventState
     * @param eventState refer to {@link MdbState}
     */
    public void setEventState(String eventState) {
        this.eventState = eventState;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int result) {
        this.resultCode = result;
    }

    public int getVendAmt() {
        return vendAmt;
    }

    public void setVendAmt(int vendAmt) {
        this.vendAmt = vendAmt;
    }

    public int getFundsAvailable() {
        return fundsAvailable;
    }

    public void setFundsAvailable(int fundsAvailable) {
        this.fundsAvailable = fundsAvailable;
    }
}
