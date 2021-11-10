package com.jamgu.hwstatistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jamgu.hwstatistics.databinding.ItemStatisticLayoutBinding

/**
 * Created by jamgu on 2021/10/15
 */

class StatisticAdapter: RecyclerView.Adapter<StatisticTextVH>() {

    private var mData: ArrayList<String>? = null

    fun setData(data: ArrayList<String>?) {
        mData = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticTextVH {
        val binding = ItemStatisticLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StatisticTextVH(binding)
    }

    override fun onBindViewHolder(holder: StatisticTextVH, position: Int) {
        mData?.let {
            val data = it[position]
            val binding = holder.binding
            binding.vStatisticText.text = data
        }
    }

    override fun getItemCount(): Int = mData?.size ?: 0

}

class StatisticTextVH(val binding: ItemStatisticLayoutBinding): RecyclerView.ViewHolder(binding.root)