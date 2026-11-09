package com.fauzan.kafe.Remote

import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitCloudClient {
    private var instance: Retrofit? = null

    @JvmStatic
    fun getInstance(): Retrofit {
        return instance ?: Retrofit.Builder()
            .baseUrl("https://us-central1-kafe-mock-app.cloudfunctions.net/widgets/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().also { instance = it }
    }
}
