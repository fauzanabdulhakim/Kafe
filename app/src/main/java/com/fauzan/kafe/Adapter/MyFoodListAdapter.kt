package com.fauzan.kafe.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fauzan.kafe.Callback.RecyclerClickListener
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Database.CartDataSource
import com.fauzan.kafe.Database.CartDatabase
import com.fauzan.kafe.Database.CartItem
import com.fauzan.kafe.Database.LocalCartDataSource
import com.fauzan.kafe.EventBus.CounterCartEvent
import com.fauzan.kafe.EventBus.FoodItemClick
import com.fauzan.kafe.Model.FoodModel
import com.fauzan.kafe.databinding.LayoutFoodItemBinding
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus

class MyFoodListAdapter(
    private val context: Context,
    val foodModelList: List<FoodModel>
) : RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder>() {

    private val compositeDisposable = CompositeDisposable()
    private val cartDataSource: CartDataSource = LocalCartDataSource(CartDatabase.getInstance(context).cartDAO())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = LayoutFoodItemBinding.inflate(
            LayoutInflater.from(context), parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val food = foodModelList[position]
        Glide.with(context).load(food.image).into(holder.binding.imgFoodImage)

        if (!food.isAvailable) {
            holder.binding.txtFoodName.text = "${food.name} [Habis]"
            holder.binding.txtFoodPrice.text = "Rp${food.price} (Stok Habis)"
            holder.binding.imgQuickCart.alpha = 0.35f
        } else {
            holder.binding.txtFoodName.text = food.name
            holder.binding.txtFoodPrice.text = "Rp${food.price}"
            holder.binding.imgQuickCart.alpha = 1.0f
        }

        holder.setListener { _, pos ->
            Common.selectedFood = foodModelList[pos]
            Common.selectedFood?.key = pos.toString()
            EventBus.getDefault().postSticky(FoodItemClick(isSuccess = true, foodModel = foodModelList[pos]))
        }

        holder.binding.imgQuickCart.setOnClickListener {
            if (!food.isAvailable) {
                Toast.makeText(context, "Maaf, menu ${food.name} sedang habis (Out of Stock)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val user = Common.currentUser ?: return@setOnClickListener

            val cartItem = CartItem(
                uid = user.uid ?: "",
                userPhone = user.phone,
                foodId = food.id ?: "",
                foodName = food.name,
                foodImage = food.image,
                foodPrice = (food.price ?: 0L).toDouble(),
                foodQuantity = 1,
                foodExtraPrice = 0.0,
                foodAddon = "Default",
                foodSize = "Default"
            )

            cartDataSource.getItemWithAllOptionsInCart(
                user.uid ?: "",
                cartItem.foodId,
                cartItem.foodSize,
                cartItem.foodAddon
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<CartItem> {
                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onSuccess(cartItemFromDB: CartItem) {
                        if (cartItemFromDB == cartItem) {
                            cartItemFromDB.foodQuantity += cartItem.foodQuantity
                            cartDataSource.updateCartItems(cartItemFromDB)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(object : SingleObserver<Int> {
                                    override fun onSubscribe(d: Disposable) {
                                        compositeDisposable.add(d)
                                    }

                                    override fun onSuccess(integer: Int) {
                                        Toast.makeText(context, "Update Cart success", Toast.LENGTH_SHORT).show()
                                        EventBus.getDefault().postSticky(CounterCartEvent(isSuccess = true))
                                    }

                                    override fun onError(e: Throwable) {
                                        Toast.makeText(context, "[UPDATE CART]${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        } else {
                            insertCartItem(cartItem)
                        }
                    }

                    override fun onError(e: Throwable) {
                        if (e.message?.contains("empty") == true) {
                            insertCartItem(cartItem)
                        } else {
                            Toast.makeText(context, "[GET CART]${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        }
    }

    private fun insertCartItem(cartItem: CartItem) {
        compositeDisposable.add(
            cartDataSource.insertOrReplaceAll(cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(context, "Add to Cart success", Toast.LENGTH_SHORT).show()
                    EventBus.getDefault().postSticky(CounterCartEvent(isSuccess = true))
                }, { throwable ->
                    Toast.makeText(context, "[CART ERROR]${throwable.message}", Toast.LENGTH_SHORT).show()
                })
        )
    }

    override fun getItemCount(): Int = foodModelList.size

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        compositeDisposable.clear()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun clearDisposables() {
        compositeDisposable.clear()
    }

    class MyViewHolder(val binding: LayoutFoodItemBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private var listener: RecyclerClickListener? = null

        fun setListener(listener: RecyclerClickListener) {
            this.listener = listener
        }

        init {
            binding.root.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            listener?.onItemCLickListener(view, adapterPosition)
        }
    }
}
