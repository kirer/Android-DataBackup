package com.xayah.databackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.drakeet.multitype.ItemViewDelegate
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.AppEntity
import com.xayah.databackup.databinding.AdapterAppListBinding
import com.xayah.design.util.dp

open class AppListAdapterBase(open val context: Context) :
    ItemViewDelegate<AppEntity, AppListAdapterBase.ViewHolder>() {

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            AdapterAppListBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, item: AppEntity) {
        val binding = holder.binding
        binding.appIcon.setImageDrawable(item.icon)
        binding.appName.text = item.appName
        binding.appPackage.text = item.packageName

        val isFirst = (adapterItems as MutableList<*>).indexOf(item) == 0
        App.log.onObserveLast(context as FragmentActivity) {
            it?.apply {
                if (isFirst && item.isProcessing)
                    binding.appPackage.text = this
                else
                    binding.appPackage.text = item.packageName
            }
        }

        if (holder.bindingAdapterPosition == adapterItems.size - 1) {
            binding.materialCardView.apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                        .apply {
                            topMargin = 16.dp
                            bottomMargin = 16.dp
                            marginStart = 20.dp
                            marginEnd = 20.dp
                        }
            }
        } else {
            binding.materialCardView.apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                        .apply {
                            topMargin = 16.dp
                            marginStart = 20.dp
                            marginEnd = 20.dp
                        }
            }
        }

        // -------------Deprecated-------------
        if (item.isProcessing) {
            binding.appPackage.text = item.progress
            binding.chipApplication.isClickable = false
            binding.chipData.isClickable = false
        }
        if (item.onProcessingApp) {
            binding.chipApplication.apply {
                checkedIcon = null
                chipIcon = CircularProgressDrawable(context).apply {
                    setStyle(CircularProgressDrawable.DEFAULT)
                    centerRadius = 5.dp.toFloat()
                    strokeWidth = 2.dp.toFloat()
                    start()
                }
            }
        } else {
            binding.chipApplication.apply {
                checkedIcon = AppCompatResources.getDrawable(context, R.drawable.ic_round_check)
                chipIcon = null
            }
        }
        if (item.onProcessingData) {
            binding.chipData.apply {
                checkedIcon = null
                chipIcon = CircularProgressDrawable(context).apply {
                    setStyle(CircularProgressDrawable.DEFAULT)
                    centerRadius = 5.dp.toFloat()
                    strokeWidth = 2.dp.toFloat()
                    start()
                }
            }
        } else {
            binding.chipData.apply {
                checkedIcon = AppCompatResources.getDrawable(context, R.drawable.ic_round_check)
                chipIcon = null
            }
        }

        if (item.isProcessing && !item.backupApp) {
            binding.chipApplication.visibility = View.GONE
        } else {
            binding.chipApplication.visibility = View.VISIBLE
        }
        if (item.isProcessing && !item.backupData) {
            binding.chipData.visibility = View.GONE
        } else {
            binding.chipData.visibility = View.VISIBLE
        }
        // -------------Deprecated-------------
    }

    class ViewHolder(val binding: AdapterAppListBinding) : RecyclerView.ViewHolder(binding.root)
}