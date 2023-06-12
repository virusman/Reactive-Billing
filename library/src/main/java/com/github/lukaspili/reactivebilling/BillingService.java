package com.github.lukaspili.reactivebilling;

import android.app.Activity;
import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.github.lukaspili.reactivebilling.listener.GetPurchasesResponseListener;
import com.github.lukaspili.reactivebilling.listener.GetSkuDetailsResponseListener;
import com.github.lukaspili.reactivebilling.listener.LaunchPurchaseFlowListener;
import com.github.lukaspili.reactivebilling.model.PurchaseType;
import com.github.lukaspili.reactivebilling.model.SkuDetails;
import com.github.lukaspili.reactivebilling.parser.PurchaseParser;
import com.github.lukaspili.reactivebilling.response.GetPurchasesResponse;
import com.github.lukaspili.reactivebilling.response.GetSkuDetailsResponse;
import com.github.lukaspili.reactivebilling.response.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by lukasz on 04/05/16.
 */
public class BillingService {

    private static final int API_VERSION = 3;

    private final Context context;
    private final BillingClient billingClient;

    public BillingService(Context context, BillingClient billingClient) {
        this.context = context;
        this.billingClient = billingClient;
    }

    public Response isBillingSupported(PurchaseType purchaseType) throws RemoteException {
        ReactiveBillingLogger.log("Is billing supported - request (thread %s)", Thread.currentThread().getName());
        String feature = null;
        switch (purchaseType) {
            case PRODUCT:
                feature = BillingClient.FeatureType.PRODUCT_DETAILS;
                break;
            case SUBSCRIPTION:
                feature = BillingClient.FeatureType.SUBSCRIPTIONS;
                break;
        }
        BillingResult response = billingClient.isFeatureSupported(feature);
        ReactiveBillingLogger.log("Is billing supported - response: %d", response);
        return new Response(response.getResponseCode());
    }

    public void acknowledgePurchase(String purchaseToken, AcknowledgePurchaseResponseListener listener) throws RemoteException {
        ReactiveBillingLogger.log("Acknowledge purchase - request (thread %s)", Thread.currentThread().getName());
        billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build(), (billingResult) -> {
            ReactiveBillingLogger.log("Acknowledge purchase - response: %d", billingResult.getResponseCode());
            listener.onAcknowledgePurchaseResponse(billingResult);
        });
    }

    public void consumePurchase(String purchaseToken, ConsumeResponseListener listener) throws RemoteException {
        ReactiveBillingLogger.log("Consume purchase - request (thread %s)", Thread.currentThread().getName());
        billingClient.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build(), (billingResult, s) -> {
            ReactiveBillingLogger.log("Consume purchase - response: %d", billingResult.getResponseCode());
            listener.onConsumeResponse(billingResult, s);
        });
    }

    public void getPurchases(PurchaseType purchaseType, String continuationToken, GetPurchasesResponseListener listener) throws RemoteException {
        ReactiveBillingLogger.log("Get purchases - request (thread %s)", Thread.currentThread().getName());
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(purchaseType.getIdentifier()).build(), (billingResult, purchasesList) -> {
            ReactiveBillingLogger.log("Get purchases - response: %d", billingResult.getResponseCode());
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                listener.onGetPurchasesResponse(new GetPurchasesResponse(billingResult.getResponseCode(), null, null));
                return;
            }
            List<String> productsIds = new ArrayList<>();
            List<String> purchases = new ArrayList<>();
            List<String> signatures = new ArrayList<>();
            for (int i = 0; i < purchasesList.size(); i++) {
                productsIds.add(purchasesList.get(i).getProducts().get(0));
                purchases.add(purchasesList.get(i).getOriginalJson());
                signatures.add(purchasesList.get(i).getSignature());
            }
            List<GetPurchasesResponse.PurchaseResponse> purchaseResponses = new ArrayList<>();
            for (int i = 0; i < productsIds.size(); i++) {
                purchaseResponses.add(new GetPurchasesResponse.PurchaseResponse(
                        productsIds.get(i),
                        purchases.get(i),
                        signatures.get(i),
                        PurchaseParser.parse(purchases.get(i))
                ));
            }

            ReactiveBillingLogger.log("Get purchases - items size: %s", purchaseResponses.size());
            listener.onGetPurchasesResponse(new GetPurchasesResponse(billingResult.getResponseCode(), purchaseResponses, null));
        });
    }

    public void getSkuDetails(PurchaseType purchaseType, GetSkuDetailsResponseListener listener, String... productIds) throws RemoteException {
        if (productIds == null || productIds.length == 0) {
            throw new IllegalArgumentException("Product ids cannot be blank");
        }

        ReactiveBillingLogger.log("Get sku details - request: %s (thread %s)", TextUtils.join(", ", productIds), Thread.currentThread().getName());

        ArrayList<QueryProductDetailsParams.Product> products = new ArrayList<>();
        for (String productId : productIds) {
            products.add(QueryProductDetailsParams.Product.newBuilder().setProductId(productId).setProductType(purchaseType.getIdentifier()).build());
        }

        billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build(), (billingResult, productDetails) -> {
            ReactiveBillingLogger.log("Get sku details - response code: %s", billingResult.getResponseCode());
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                listener.onGetSkuDetailsResponse(new GetSkuDetailsResponse(billingResult.getResponseCode(), null));
                return;
            }

            List<SkuDetails> skuDetailsList = new ArrayList<>();
            for (int i = 0; i < productDetails.size(); i++) {
                skuDetailsList.add(SkuDetails.fromProductDetails(productDetails.get(i)));
            }

            ReactiveBillingLogger.log("Get sku details - list size: %s", skuDetailsList.size());
            listener.onGetSkuDetailsResponse(new GetSkuDetailsResponse(billingResult.getResponseCode(), skuDetailsList));
        });
    }

    public void launchPurchaseFlow(Activity activity, String productId, PurchaseType purchaseType, String developerPayload, LaunchPurchaseFlowListener listener) {
        ReactiveBillingLogger.log("Launch purchase flow - request: %s (thread %s)", productId, Thread.currentThread().getName());

        List<QueryProductDetailsParams.Product> products = Collections.singletonList(QueryProductDetailsParams.Product.newBuilder().setProductId(productId).setProductType(purchaseType.getIdentifier()).build());

        billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build(), (billingResult, productDetails) -> {
            ReactiveBillingLogger.log("Launch purchase flow - query details - response code: %s", billingResult.getResponseCode());
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                ReactiveBillingLogger.log("Launch purchase flow - query details - error: %s", billingResult.getDebugMessage());
                listener.onPurchaseFlowResult(new Response(billingResult.getResponseCode()));
                return;
            }
            if (productDetails.size() == 0) {
                ReactiveBillingLogger.log("Launch purchase flow - query details - error: no product details found");
                listener.onPurchaseFlowResult(new Response(BillingClient.BillingResponseCode.ERROR));
                return;
            }
            BillingFlowParams params = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                            Collections.singletonList(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                            .setProductDetails(productDetails.get(0))
                                            .build()
                            )
                    )
                    .setObfuscatedAccountId(developerPayload)
                    .build();
            BillingResult result = billingClient.launchBillingFlow(activity, params);
            listener.onPurchaseFlowResult(new Response(result.getResponseCode()));
        });
    }
}
