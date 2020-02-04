package com.fauzan.kafe.Callback;

import com.fauzan.kafe.Model.Order;

public interface ILoadTimeFromFirebaseListener {
    void onLoadTimeSucces(Order order,long estimateTimeInMs);
    void onLoadTimeFailed(String message);
}
