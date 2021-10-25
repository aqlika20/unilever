package com.macan.guestbookkemendagri

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.macan.guestbookkemendagri.databinding.DetailDestinationItemBinding
import com.macan.guestbookkemendagri.databinding.PrimaryDestinationItemBinding
import com.macan.guestbookkemendagri.models.destination.DetailDestinationItem
import com.macan.guestbookkemendagri.models.destination.ListDestinationItem
import com.macan.guestbookkemendagri.models.destination.PrimaryDestinationItem

class DestinationAdapter(var context: Context, var sourceData: List<ListDestinationItem>, var onDetailSelected : (item : DetailDestinationItem) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val detailLayoutWrappers : ArrayList<LinearLayout> = ArrayList()
    private val detailTextViews : ArrayList<TextView> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ListDestinationItem.TYPE_PRIMARY ->
                PrimaryViewHolder(PrimaryDestinationItemBinding.inflate(layoutInflater))
            else ->
                DetailViewHolder(DetailDestinationItemBinding.inflate(layoutInflater))
        }
    }

    inner class PrimaryViewHolder(private val binding: PrimaryDestinationItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PrimaryDestinationItem) {
            binding.tvPrimary.text = item.name
        }
    }

    inner class DetailViewHolder(private val binding: DetailDestinationItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(item: DetailDestinationItem){
            detailLayoutWrappers.add(binding.linearLayoutWrapper)
            detailTextViews.add(binding.tvDetail)

            binding.tvDetail.text= item.name

            binding.linearLayoutWrapper.setOnClickListener{
                for(detailLayoutWrapper in detailLayoutWrappers){
                    detailLayoutWrapper.setBackgroundColor(context.resources.getColor(R.color.design_default_color_background))
                }
                for(detailTextView in detailTextViews){
                    detailTextView.setTextColor(context.resources.getColor(R.color.default_text_view_color))
                }

                binding.linearLayoutWrapper.setBackgroundColor(context.resources.getColor(R.color.teal_700))
                binding.tvDetail.setTextColor(context.resources.getColor(R.color.white))
                onDetailSelected.invoke(item)

            }

        }
    }

    override fun getItemViewType(position: Int): Int {
        return sourceData[position].type
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            ListDestinationItem.TYPE_PRIMARY -> (holder as PrimaryViewHolder).bind(
                item = sourceData[position] as PrimaryDestinationItem,
            )
            ListDestinationItem.TYPE_DETAIL -> (holder as DetailViewHolder).bind(
                item = sourceData[position] as DetailDestinationItem
            )
        }
    }

    override fun getItemCount(): Int {
        return sourceData.size
    }
}