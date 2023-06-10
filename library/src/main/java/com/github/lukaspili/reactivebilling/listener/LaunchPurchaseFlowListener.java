package com.github.lukaspili.reactivebilling.listener;

import com.github.lukaspili.reactivebilling.response.Response;

public interface LaunchPurchaseFlowListener {
    void onPurchaseFlowResult(Response response);
}
