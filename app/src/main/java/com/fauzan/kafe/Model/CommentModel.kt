package com.fauzan.kafe.Model

data class CommentModel(
    var ratingValue: Float = 0f,
    var comment: String? = null,
    var name: String? = null,
    var uid: String? = null,
    var commentTimeStamp: MutableMap<String, Any>? = null
)
