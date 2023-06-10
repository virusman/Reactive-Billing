package com.github.lukaspili.reactivebilling.model;

import com.android.billingclient.api.ProductDetails;
import com.github.lukaspili.reactivebilling.parser.PurchaseTypeParser;

/**
 * Created by lukasz on 06/05/16.
 */
public class SkuDetails {

    private final ProductDetails productDetails;

    private final String productId;

    private final long priceAmountMicros;

    private final PurchaseType purchaseType;

    private final String price;

    private final String priceCurrencyCode;

    private final String title;

    private final String description;

    public SkuDetails(ProductDetails productDetails, String productId, long priceAmountMicros, PurchaseType purchaseType, String price, String priceCurrencyCode, String title, String description) {
        this.productDetails = productDetails;
        this.productId = productId;
        this.priceAmountMicros = priceAmountMicros;
        this.purchaseType = purchaseType;
        this.price = price;
        this.priceCurrencyCode = priceCurrencyCode;
        this.title = title;
        this.description = description;
    }

    public String getProductId() {
        return productId;
    }

    public PurchaseType getPurchaseType() {
        return purchaseType;
    }

    public String getPrice() {
        return price;
    }

    public long getPriceAmountMicros() {
        return priceAmountMicros;
    }

    public String getPriceCurrencyCode() {
        return priceCurrencyCode;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }


    public static SkuDetails fromProductDetails(ProductDetails productDetails) {
        return new SkuDetails(
                productDetails,
                productDetails.getProductId(),
                productDetails.getOneTimePurchaseOfferDetails().getPriceAmountMicros(),
                PurchaseTypeParser.parse(productDetails.getProductType()),
                productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice(),
                productDetails.getOneTimePurchaseOfferDetails().getPriceCurrencyCode(),
                productDetails.getTitle(),
                productDetails.getDescription()
        );
    }
}
