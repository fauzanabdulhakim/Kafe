package com.fauzan.kafe.ui.comments

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fauzan.kafe.Model.CommentModel

class CommentViewModel : ViewModel() {
    val mutableLiveDataFoodList: MutableLiveData<List<CommentModel>> = MutableLiveData()

    fun setCommentList(commentList: List<CommentModel>) {
        mutableLiveDataFoodList.value = commentList
    }
}
