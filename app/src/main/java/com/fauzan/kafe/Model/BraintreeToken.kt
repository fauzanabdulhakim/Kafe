package com.fauzan.kafe.Model

data class BraintreeToken(
    var isError: Boolean = false,
    var token: String? = null
)
