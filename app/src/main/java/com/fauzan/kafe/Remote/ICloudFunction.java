package com.fauzan.kafe.Remote;

import com.fauzan.kafe.Model.BraintreeToken;
import com.fauzan.kafe.Model.BraintreeTransaction;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ICloudFunction {
    @GET("token")
    Observable<BraintreeToken> getToken(@HeaderMap Map<String,String> headers);

    @POST("checkout")
    @FormUrlEncoded
    Observable<BraintreeTransaction> submitPayment(
            @HeaderMap Map<String,String> headers,
            @Field("amount") double amount,
            @Field("payment_method_nonce") String nonce);
}
