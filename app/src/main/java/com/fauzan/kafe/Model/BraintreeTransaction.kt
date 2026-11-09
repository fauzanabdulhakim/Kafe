package com.fauzan.kafe.Model

data class BraintreeTransaction(
    var isSuccess: Boolean = false,
    var transaction: Transaction? = null
)
