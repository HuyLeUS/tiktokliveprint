package com.dantsu.thermalprinter;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dantsu.thermalprinter.data.Comment;
import com.dantsu.thermalprinter.data.CommentData;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private List<Comment> items;

    private OnItemClickListener listener;


    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }



    public ItemAdapter(List<Comment> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new ItemViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Comment currentItem = items.get(holder.getBindingAdapterPosition());
        holder.titleTextView.setText(currentItem.nickname);
        holder.descriptionTextView.setText(currentItem.comment);


        holder.countOrder.setText(String.valueOf(currentItem.countOrder));
        Picasso.get()
                .load(currentItem.profilePictureUrl)
                .fit()
                .into(holder.avatar);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView descriptionTextView;

        public TextView countOrder;
        public CircleImageView avatar;

        public Button printButton;

        public ItemViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.textViewTitle);
            descriptionTextView = itemView.findViewById(R.id.textViewDescription);
            countOrder = itemView.findViewById(R.id.tvCountOrder);
            avatar = itemView.findViewById(R.id.avatarImageView);
            printButton = itemView.findViewById(R.id.trailingButton);
            printButton.setOnClickListener(view -> {
                if(listener!=null){
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }
}

