package com.fauzan.kafe.ui.foodlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Model.FoodModel

class FoodListViewModel : ViewModel() {

    private val _mutableLiveDataFoodList = MutableLiveData<List<FoodModel>>()
    val mutableLiveDataFoodList: MutableLiveData<List<FoodModel>>
        get() {
            _mutableLiveDataFoodList.value = Common.categorySelected?.foods
            return _mutableLiveDataFoodList
        }
}
