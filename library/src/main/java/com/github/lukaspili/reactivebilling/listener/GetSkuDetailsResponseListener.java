package com.github.lukaspili.reactivebilling.listener;

import com.github.lukaspili.reactivebilling.response.GetSkuDetailsResponse;

public interface GetSkuDetailsResponseListener {
    void onGetSkuDetailsResponse(GetSkuDetailsResponse response);
}
