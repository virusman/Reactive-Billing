package com.github.lukaspili.reactivebilling;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.github.lukaspili.reactivebilling.model.Purchase;
import com.github.lukaspili.reactivebilling.response.PurchaseResponse;
import com.jakewharton.rxrelay.PublishRelay;

import java.util.List;

import rx.Observable;
import rx.functions.Action0;

/**
 * Created by lukasz on 06/05/16.
 */
public class PurchaseFlowService implements BillingClientStateListener, PurchasesUpdatedListener {

    private BillingClient billingClient;
    private final Context context;
    private final PublishRelay<PurchaseResponse> subject = PublishRelay.create();
    private final Observable<PurchaseResponse> observable = subject.doOnSubscribe(new Action0() {
        @Override
        public void call() {
            if (hasSubscription) {
                throw new IllegalStateException("Already has subscription");
            }

            billingClient = BillingClient.newBuilder(context).setListener(PurchaseFlowService.this).enablePendingPurchases().build();
            billingClient.startConnection(PurchaseFlowService.this);

            ReactiveBillingLogger.log("Purchase flow - subscribe (thread %s)", Thread.currentThread().getName());
            hasSubscription = true;
        }
    }).doOnUnsubscribe(new Action0() {
        @Override
        public void call() {
            if (!hasSubscription) {
                throw new IllegalStateException("Doesn't have any subscription");
            }

            billingClient.endConnection();
            billingClient = null;

            ReactiveBillingLogger.log("Purchase flow - unsubscribe (thread %s)", Thread.currentThread().getName());
            hasSubscription = false;
        }
    });

    private boolean hasSubscription;

    PurchaseFlowService(Context context) {
        this.context = context;
    }

    Observable<PurchaseResponse> getObservable() {
        return observable;
    }

    public boolean canStartFlow() {
        if (!hasSubscription) {
            throw new IllegalStateException("Cannot start flow without subscribers");
        }
        return true;
    }

    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<com.android.billingclient.api.Purchase> list) {
        if (!hasSubscription) {
            throw new IllegalStateException("Subject cannot be null when receiving purchase result");
        }

        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            ReactiveBillingLogger.log("Purchase flow result - OK (thread %s)", Thread.currentThread().getName());
            com.android.billingclient.api.Purchase billingPurchase = list.get(0);
            Purchase purchase = Purchase.fromBillingPurchase(billingPurchase);
            subject.call(new PurchaseResponse(billingResult.getResponseCode(), purchase, billingPurchase.getOriginalJson(), billingPurchase.getSignature(), null, false));
        } else {
            ReactiveBillingLogger.log("Purchase flow result - CANCELED (thread %s)", Thread.currentThread().getName());
            subject.call(new PurchaseResponse(-1, null, null, null, null, true));
        }
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

    }

    @Override
    public void onBillingServiceDisconnected() {

    }
}
