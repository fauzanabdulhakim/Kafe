package com.fauzan.kafe.ui.cart

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Database.CartDataSource
import com.fauzan.kafe.Database.CartDatabase
import com.fauzan.kafe.Database.CartItem
import com.fauzan.kafe.Database.LocalCartDataSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class CartViewModel : ViewModel() {

    private val compositeDisposable = CompositeDisposable()
    private var cartDataSource: CartDataSource? = null
    private var _mutableLiveDataCartItems: MutableLiveData<List<CartItem>>? = null

    fun initCartDataSource(context: Context) {
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context).cartDAO())
    }

    fun onStop() {
        compositeDisposable.clear()
    }

    fun getMutableLiveDataCartItems(): MutableLiveData<List<CartItem>> {
        if (_mutableLiveDataCartItems == null) {
            _mutableLiveDataCartItems = MutableLiveData()
        }
        getAllCartItems()
        return _mutableLiveDataCartItems!!
    }

    private fun getAllCartItems() {
        val uid = Common.currentUser?.uid ?: return
        val ds = cartDataSource ?: return
        compositeDisposable.add(
            ds.getAllCart(uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ cartItems ->
                    _mutableLiveDataCartItems?.value = cartItems
                }, {
                    _mutableLiveDataCartItems?.value = null
                })
        )
    }

    override fun onCleared() {
        compositeDisposable.clear()
        super.onCleared()
    }
}
