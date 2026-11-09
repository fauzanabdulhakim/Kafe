package com.fauzan.kafe.ui.fooddetail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Model.CommentModel
import com.fauzan.kafe.Model.FoodModel

class FoodDetailViewModel : ViewModel() {

    private val _mutableLiveDataFood = MutableLiveData<FoodModel>()
    val mutableLiveDataFood: MutableLiveData<FoodModel>
        get() {
            _mutableLiveDataFood.value = Common.selectedFood
            return _mutableLiveDataFood
        }

    val mutableLiveDataComment: MutableLiveData<CommentModel> = MutableLiveData()

    fun setCommentModel(commentModel: CommentModel) {
        mutableLiveDataComment.value = commentModel
    }

    fun setFoodModel(foodModel: FoodModel) {
        _mutableLiveDataFood.value = foodModel
    }
}
