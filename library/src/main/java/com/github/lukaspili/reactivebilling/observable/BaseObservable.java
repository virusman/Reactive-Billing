package com.github.lukaspili.reactivebilling.observable;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.github.lukaspili.reactivebilling.BillingService;
import com.github.lukaspili.reactivebilling.ReactiveBilling;
import com.github.lukaspili.reactivebilling.ReactiveBillingLogger;

import java.util.List;
import java.util.concurrent.Semaphore;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;


public abstract class BaseObservable<T> implements Observable.OnSubscribe<T> {

    protected final Context context;
    private final Semaphore semaphore = new Semaphore(0);

    private BillingService billingService;

    BaseObservable(Context context) {
        this.context = context;
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {
        final Intent intent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        intent.setPackage("com.android.vending");

        // don't bother with semaphores in the originating thread is the main thread
        final boolean useSemaphore = Looper.myLooper() != Looper.getMainLooper();
        final Connection connection = new Connection(subscriber, useSemaphore);

        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                ReactiveBillingLogger.log("Unbind service (thread %s)", Thread.currentThread().getName());
                connection.endConnection();
            }
        }));

        if (useSemaphore) {
            // freeze the current RX thread until service is connected
            // because bindService() will call the connection callback on the main thread
            // we want to get back on the current RX thread
            ReactiveBillingLogger.log("Acquire semaphore until service is ready (thread %s)", Thread.currentThread().getName());
            semaphore.acquireUninterruptibly();

            // once the semaphore is released
            // it means that the service is connected and available
            //TODO: what happens if the service is never connected?
            deliverBillingService(subscriber);
        }
    }

    private void deliverBillingService(Observer observer) {
        ReactiveBillingLogger.log("Billing service ready (thread %s)", Thread.currentThread().getName());
        onBillingServiceReady(billingService, observer);
    }

    protected abstract void onBillingServiceReady(BillingService billingService, Observer<? super T> observer);

    private class Connection implements BillingClientStateListener, PurchasesUpdatedListener {

        private final Observer observer;
        private final boolean useSemaphore;
        private final BillingClient billingClient;

        public Connection(Observer observer, boolean useSemaphore) {
            this.observer = observer;
            this.useSemaphore = useSemaphore;
            this.billingClient = BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build();
            billingClient.startConnection(this);
        }

        @Override
        public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
            ReactiveBillingLogger.log("Service connected (thread %s)", Thread.currentThread().getName());
            billingService = new BillingService(context, billingClient);

            if (useSemaphore) {
                // once the service is available, release the semaphore
                // that is blocking the originating thread
                ReactiveBillingLogger.log("Release semaphore (thread %s)", Thread.currentThread().getName());
                semaphore.release();
            } else {
                deliverBillingService(observer);
            }
        }

        @Override
        public void onBillingServiceDisconnected() {
            ReactiveBillingLogger.log("Service disconnected (thread %s)", Thread.currentThread().getName());
            billingService = null;
        }

        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
            ReactiveBilling.getInstance(context).getPurchaseFlowService().onPurchasesUpdated(billingResult, list);
        }

        public void endConnection() {
            billingClient.endConnection();
        }
    }
}
