package com.dantsu.thermalprinter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.dantsu.thermalprinter.ItemAdapter.ItemViewHolder
import com.dantsu.thermalprinter.data.Comment
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ItemAdapter(private val items: List<Comment>) : RecyclerView.Adapter<ItemViewHolder>() {
    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return ItemViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val currentItem = items[holder.bindingAdapterPosition]
        holder.titleTextView.text = currentItem.nickname
        holder.descriptionTextView.text = currentItem.comment
        Picasso.get()
            .load(currentItem.profilePictureUrl)
            .fit()
            .into(holder.avatar)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ItemViewHolder(itemView: View, listener: OnItemClickListener?) : ViewHolder(itemView) {
        var titleTextView: TextView
        var descriptionTextView: TextView
        var avatar: CircleImageView
        var printButton: Button

        init {
            titleTextView = itemView.findViewById(R.id.textViewTitle)
            descriptionTextView = itemView.findViewById(R.id.textViewDescription)
            avatar = itemView.findViewById(R.id.avatarImageView)
            printButton = itemView.findViewById(R.id.trailingButton)
            printButton.setOnClickListener {
                if (listener != null) {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position)
                    }
                }
            }
        }
    }
}