package com.fauzan.kafe.Callback;

import com.fauzan.kafe.Model.OrderModel;

public interface ILoadTimeFromFirebaseListener {
    void onLoadTimeSucces(OrderModel order, long estimateTimeInMs);
    void onLoadTimeFailed(String message);
}
