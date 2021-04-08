package com.example.linkcontainer;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.Serializable;
import java.util.ArrayList;


public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {

    private static final int DESCRIPTION_MAX_LENGTH = 80;
    private ArrayList<Bookmark> bookmarks;
    private MainActivity mainActivity;
    private DatabaseHandler db;

    public RecyclerAdapter(ArrayList<Bookmark> bookmarkList, MainActivity mainActivity) {
        this.bookmarks = bookmarkList;
        this.mainActivity = mainActivity;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
      TextView link, title, description;
      ImageView image;
      ImageButton shareButton;
      View view;

        public MyViewHolder(final View itemView, MainActivity mainActivity) {
            super(itemView);
            link = itemView.findViewById(R.id.url);
            title = itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.description);
            image = itemView.findViewById(R.id.image);
            shareButton = itemView.findViewById(R.id.share);
            view = itemView;
            view.setOnLongClickListener(mainActivity);
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.bookmark_list, parent, false);

        return new MyViewHolder(itemView, mainActivity);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        String description = bookmarks.get(position).getDescription();
        String text = "<a href=" + bookmarks.get(position).getLink() + ">" + bookmarks.get(position).getLink()  +" </a>";
        holder.link.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
        holder.title.setText(bookmarks.get(position).getTitle());

        if (description != null) {
            if (description.length() > DESCRIPTION_MAX_LENGTH) {
                description = description.substring(0, DESCRIPTION_MAX_LENGTH) + "...";
            }
            holder.description.setText(description);
        } else {
            holder.description.setText("");
        }

        Picasso.get().load(bookmarks.get(position).getImage())
                .fit()
                .centerCrop()
                .into(holder.image);

        holder.link.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(bookmarks.get(position).getLink()));
            mainActivity.startActivity(i);
        });

        holder.image.setOnClickListener(v -> {
                final Dialog nagDialog = new Dialog(mainActivity);
                nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                nagDialog.setCancelable(true);
                nagDialog.setContentView(R.layout.preview_image);
                ImageView ivPreview = nagDialog.findViewById(R.id.preview_image);
                Picasso.get().load(bookmarks.get(position).getImage())
                        .fit()
                        .centerCrop()
                        .into(ivPreview);

                ivPreview.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        nagDialog.dismiss();
                    }
                });
                nagDialog.show();
                nagDialog.getWindow().setLayout(1000, 600);
            });

        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, bookmarks.get(position).getLink());
                shareIntent.setType("text/plain");
                mainActivity.startActivity(shareIntent);
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                db = DatabaseHandler.getInstance(mainActivity);
                String category = db.getCategoryById(bookmarks.get(position).getCategory());
                Intent intent = new Intent(mainActivity, InsertLink.class);
                intent.putExtra("bookmark", bookmarks.get(position));
                intent.putExtra("category", category);
                mainActivity.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (bookmarks == null) {
            return 0;
        } else {
            return bookmarks.size();
        }
    }
}
