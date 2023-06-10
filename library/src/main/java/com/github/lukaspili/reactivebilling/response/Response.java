package com.github.lukaspili.reactivebilling.response;

import com.android.billingclient.api.BillingClient;

/**
 * Created by lukasz on 06/05/16.
 */
public class Response {

    protected final int responseCode;

    public Response(int responseCode) {
        this.responseCode = responseCode;
    }

    public boolean isSuccess() {
        return responseCode == BillingClient.BillingResponseCode.OK;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
