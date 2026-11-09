package com.fauzan.kafe.Callback

import com.fauzan.kafe.Model.CommentModel

interface CommentCallbackListener {
    fun onCommentLoadSuccess(commentModels: List<CommentModel>)
    fun onCommentLoadFailed(message: String)
}
