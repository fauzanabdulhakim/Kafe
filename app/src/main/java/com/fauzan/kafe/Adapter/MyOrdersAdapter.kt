package com.fauzan.kafe.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Model.OrderModel
import com.fauzan.kafe.databinding.LayoutOrderItemBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MyOrdersAdapter(
    private val context: Context,
    val orderList: MutableList<OrderModel>
) : RecyclerView.Adapter<MyOrdersAdapter.MyViewHolder>() {

    private val calendar: Calendar = Calendar.getInstance()
    private val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

    fun getItemAtPosition(pos: Int): OrderModel = orderList[pos]

    fun setItemAtPosition(pos: Int, item: OrderModel) {
        orderList[pos] = item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = LayoutOrderItemBinding.inflate(
            LayoutInflater.from(context), parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val order = orderList[position]
        if (!order.cartItemList.isNullOrEmpty()) {
            Glide.with(context)
                .load(order.cartItemList!![0].foodImage)
                .into(holder.binding.imgOrder)
        }

        calendar.timeInMillis = order.createDate
        val date = Date(order.createDate)
        holder.binding.txtOrderDate.text = "${Common.getDateOfWeek(calendar.get(Calendar.DAY_OF_WEEK))} ${simpleDateFormat.format(date)}"
        holder.binding.txtOrderNumber.text = "Order No: ${order.orderNumber ?: ""}"
        holder.binding.txtOrderComment.text = "Comment: ${order.comment ?: ""}"
        holder.binding.txtOrderStatus.text = "Status: ${Common.convertStatusToText(order.orderStatus)}"
    }

    override fun getItemCount(): Int = orderList.size

    class MyViewHolder(val binding: LayoutOrderItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}
