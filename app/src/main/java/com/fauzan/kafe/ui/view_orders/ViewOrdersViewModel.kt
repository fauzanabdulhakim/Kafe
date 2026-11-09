package com.fauzan.kafe.ui.view_orders

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fauzan.kafe.Model.OrderModel

class ViewOrdersViewModel : ViewModel() {
    val mutableLiveDataOrderList: MutableLiveData<List<OrderModel>> = MutableLiveData()

    fun setMutableLiveDataOrderList(orderList: List<OrderModel>) {
        mutableLiveDataOrderList.value = orderList
    }
}
