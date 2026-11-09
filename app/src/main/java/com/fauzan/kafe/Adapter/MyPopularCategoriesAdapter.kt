package com.fauzan.kafe.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fauzan.kafe.Callback.RecyclerClickListener
import com.fauzan.kafe.EventBus.PopularCategoryClick
import com.fauzan.kafe.Model.PopularCategoryModel
import com.fauzan.kafe.databinding.LayoutPopularCategoriesItemBinding
import org.greenrobot.eventbus.EventBus

class MyPopularCategoriesAdapter(
    private val context: Context,
    val popularCategoryModelList: List<PopularCategoryModel>
) : RecyclerView.Adapter<MyPopularCategoriesAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = LayoutPopularCategoriesItemBinding.inflate(
            LayoutInflater.from(context), parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = popularCategoryModelList[position]
        Glide.with(context).load(item.image).into(holder.binding.categoryImage)
        holder.binding.txtCategoryName.text = item.name

        holder.setListener { _, pos ->
            EventBus.getDefault().postSticky(PopularCategoryClick(popularCategoryModelList[pos]))
        }
    }

    override fun getItemCount(): Int = popularCategoryModelList.size

    class MyViewHolder(val binding: LayoutPopularCategoriesItemBinding) :
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
