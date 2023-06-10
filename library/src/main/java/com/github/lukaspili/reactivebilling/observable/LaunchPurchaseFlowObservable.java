package com.github.lukaspili.reactivebilling.observable;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.github.lukaspili.reactivebilling.BillingService;
import com.github.lukaspili.reactivebilling.PurchaseFlowService;
import com.github.lukaspili.reactivebilling.ReactiveBillingLogger;
import com.github.lukaspili.reactivebilling.model.PurchaseType;
import com.github.lukaspili.reactivebilling.response.GetBuyIntentResponse;
import com.github.lukaspili.reactivebilling.response.Response;

import rx.Observable;
import rx.Observer;

public class LaunchPurchaseFlowObservable extends BaseObservable<Response> {

    public static Observable<Response> create(Activity activity, PurchaseFlowService purchaseFlowService, String productId, PurchaseType purchaseType, String developerPayload, Bundle extras) {
        return Observable.create(new LaunchPurchaseFlowObservable(activity, purchaseFlowService, productId, purchaseType, developerPayload, extras));
    }
    private final Activity activity;
    private final PurchaseFlowService purchaseFlowService;
    private final String productId;
    private final PurchaseType purchaseType;
    private final String developerPayload;
    private final Bundle extras;

    protected LaunchPurchaseFlowObservable(Activity activity, PurchaseFlowService purchaseFlowService, String productId, PurchaseType purchaseType, String developerPayload, Bundle extras) {
        super(activity);
        this.activity = activity;
        this.purchaseFlowService = purchaseFlowService;
        this.productId = productId;
        this.purchaseType = purchaseType;
        this.developerPayload = developerPayload;
        this.extras = extras;
    }

    @Override
    protected void onBillingServiceReady(BillingService billingService, Observer<? super Response> observer) {
        if (purchaseFlowService.canStartFlow()) {
            billingService.launchPurchaseFlow(activity, productId, purchaseType, developerPayload, (response) -> {
                observer.onNext(response);
                observer.onCompleted();
                ReactiveBillingLogger.log("Purchase flow finished: %b (thread %s)", response.isSuccess(), Thread.currentThread().getName());
            });
        }
    }
}
