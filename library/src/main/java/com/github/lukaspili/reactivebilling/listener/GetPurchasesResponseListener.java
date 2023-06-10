package com.github.lukaspili.reactivebilling.listener;

import com.github.lukaspili.reactivebilling.response.GetPurchasesResponse;

public interface GetPurchasesResponseListener {
    void onGetPurchasesResponse(GetPurchasesResponse response);
}
