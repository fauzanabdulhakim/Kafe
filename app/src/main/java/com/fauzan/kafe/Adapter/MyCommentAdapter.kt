package com.fauzan.kafe.Adapter

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fauzan.kafe.Model.CommentModel
import com.fauzan.kafe.databinding.LayoutCommentItemBinding

class MyCommentAdapter(
    private val context: Context,
    val commentModelList: List<CommentModel>
) : RecyclerView.Adapter<MyCommentAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = LayoutCommentItemBinding.inflate(
            LayoutInflater.from(context), parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val comment = commentModelList[position]
        val timeStampObj = comment.commentTimeStamp?.get("timeStamp")
        if (timeStampObj != null) {
            val timeStamp = (timeStampObj as? Number)?.toLong() ?: 0L
            val timeAgo = DateUtils.getRelativeTimeSpanString(timeStamp).toString()
            holder.binding.txtCommentDate.text = timeAgo
        }

        holder.binding.txtCommentName.text = comment.name
        holder.binding.txtComment.text = comment.comment
        holder.binding.ratingBar.rating = comment.ratingValue
    }

    override fun getItemCount(): Int = commentModelList.size

    class MyViewHolder(val binding: LayoutCommentItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}
