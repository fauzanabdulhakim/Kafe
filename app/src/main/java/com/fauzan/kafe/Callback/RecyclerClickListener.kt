package com.fauzan.kafe.Callback

import android.view.View

fun interface RecyclerClickListener {
    fun onItemCLickListener(view: View, pos: Int)
}
