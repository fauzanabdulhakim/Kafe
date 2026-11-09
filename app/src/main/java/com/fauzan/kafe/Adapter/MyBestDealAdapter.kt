package com.fauzan.kafe.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.asksira.loopingviewpager.LoopingPagerAdapter
import com.bumptech.glide.Glide
import com.fauzan.kafe.EventBus.BestDealItemClick
import com.fauzan.kafe.Model.BestDealModel
import com.fauzan.kafe.R
import com.fauzan.kafe.databinding.LayoutBestDealItemBinding
import org.greenrobot.eventbus.EventBus

class MyBestDealAdapter(
    context: Context,
    itemList: List<BestDealModel>,
    isInfinite: Boolean
) : LoopingPagerAdapter<BestDealModel>(context, itemList, isInfinite) {

    override fun inflateView(viewType: Int, container: ViewGroup, listPosition: Int): View {
        return LayoutInflater.from(context).inflate(R.layout.layout_best_deal_item, container, false)
    }

    override fun bindView(convertView: View, listPosition: Int, viewType: Int) {
        val binding = LayoutBestDealItemBinding.bind(convertView)
        val item = itemList?.getOrNull(listPosition) ?: return

        Glide.with(convertView).load(item.image).into(binding.imgBestDeal)
        binding.txtBestDeal.text = item.name

        convertView.setOnClickListener {
            EventBus.getDefault().postSticky(BestDealItemClick(item))
        }
    }
}
