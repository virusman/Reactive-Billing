package com.github.lukaspili.reactivebilling.model;

import com.android.billingclient.api.Purchase;

/**
 * Created by lukasz on 06/05/16.
 */
public enum PurchaseState {

    PURCHASED(0), CANCELED(1), REFUNDED(2);

    private final int value;

    PurchaseState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PurchaseState fromBillingPurchaseState(int purchaseState) {
        switch (purchaseState) {
            case Purchase.PurchaseState.PURCHASED:
                return PurchaseState.PURCHASED;
            case Purchase.PurchaseState.UNSPECIFIED_STATE:
                return PurchaseState.CANCELED;
            default:
                throw new IllegalArgumentException("Unknown purchase state: " + purchaseState);
        }
    }
}
