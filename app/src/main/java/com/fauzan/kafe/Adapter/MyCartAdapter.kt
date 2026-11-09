package com.fauzan.kafe.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fauzan.kafe.Database.CartItem
import com.fauzan.kafe.EventBus.UpdateItemInCart
import com.fauzan.kafe.databinding.LayoutCartItemBinding
import org.greenrobot.eventbus.EventBus

class MyCartAdapter(
    private val context: Context,
    val cartItemList: List<CartItem>
) : RecyclerView.Adapter<MyCartAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = LayoutCartItemBinding.inflate(
            LayoutInflater.from(context), parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val cartItem = cartItemList[position]
        Glide.with(context).load(cartItem.foodImage).into(holder.binding.imgCart)
        holder.binding.txtFoodName.text = cartItem.foodName
        val totalPrice = (cartItem.foodPrice ?: 0.0) + (cartItem.foodExtraPrice ?: 0.0)
        holder.binding.txtFoodPrice.text = totalPrice.toString()

        holder.binding.numberButton.number = cartItem.foodQuantity.toString()

        holder.binding.numberButton.setOnValueChangeListener { _, _, newValue ->
            cartItemList[position].foodQuantity = newValue
            EventBus.getDefault().postSticky(UpdateItemInCart(cartItemList[position]))
        }
    }

    override fun getItemCount(): Int = cartItemList.size

    fun getItemAtPosition(pos: Int): CartItem = cartItemList[pos]

    class MyViewHolder(val binding: LayoutCartItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}
