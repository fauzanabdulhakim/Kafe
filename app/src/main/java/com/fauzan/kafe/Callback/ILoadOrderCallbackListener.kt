package com.fauzan.kafe.Callback

import com.fauzan.kafe.Model.OrderModel

interface ILoadOrderCallbackListener {
    fun onLoadOrderSuccess(orderList: List<OrderModel>)
    fun onLoadOrderFailed(message: String)
}
