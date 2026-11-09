package com.fauzan.kafe.Callback

import com.fauzan.kafe.Model.OrderModel

interface ILoadTimeFromFirebaseListener {
    fun onLoadTimeSucces(order: OrderModel, estimateTimeInMs: Long)
    fun onLoadTimeFailed(message: String)
}
