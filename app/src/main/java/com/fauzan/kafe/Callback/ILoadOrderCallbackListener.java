package com.fauzan.kafe.Callback;

import com.fauzan.kafe.Model.Order;

import java.util.List;

public interface ILoadOrderCallbackListener {
    void onLoadOrderSuccess(List<Order> orderList);
    void onLoadOrderFailed(String message);
}
