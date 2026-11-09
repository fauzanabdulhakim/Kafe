package com.asksira.loopingviewpager

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

/**
 * Base PagerAdapter supporting looping data presentation.
 *
 * @param T data model type
 */
abstract class LoopingPagerAdapter<T>(
    protected var context: Context,
    itemList: List<T>?,
    protected var isInfinite: Boolean
) : PagerAdapter() {

    var itemList: List<T>? = itemList
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getCount(): Int {
        return itemList?.size ?: 0
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = inflateView(getItemViewType(position), container, position)
        bindView(view, position, getItemViewType(position))
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    open fun getItemViewType(listPosition: Int): Int {
        return 0
    }

    protected abstract fun inflateView(viewType: Int, container: ViewGroup, listPosition: Int): View

    protected abstract fun bindView(convertView: View, listPosition: Int, viewType: Int)
}
