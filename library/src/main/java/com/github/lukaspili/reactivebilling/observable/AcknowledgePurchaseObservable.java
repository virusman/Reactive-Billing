package com.github.lukaspili.reactivebilling.observable;

import android.content.Context;
import android.os.RemoteException;

import com.github.lukaspili.reactivebilling.BillingService;
import com.github.lukaspili.reactivebilling.response.Response;

import rx.Observable;
import rx.Observer;

public class AcknowledgePurchaseObservable extends BaseObservable<Response> {

    public static Observable<Response> create(Context context, String purchaseToken) {
        return Observable.create(new AcknowledgePurchaseObservable(context, purchaseToken));
    }

    private final String purchaseToken;

    private AcknowledgePurchaseObservable(Context context, String purchaseToken) {
        super(context);
        this.purchaseToken = purchaseToken;
    }

    @Override
    protected void onBillingServiceReady(BillingService billingService, Observer<? super Response> observer) {
        try {
            billingService.acknowledgePurchase(purchaseToken, (billingResult) -> {
                Response response = new Response(billingResult.getResponseCode());
                observer.onNext(response);
                observer.onCompleted();
            });
        } catch (RemoteException e) {
            observer.onError(e);
        }
    }
}
