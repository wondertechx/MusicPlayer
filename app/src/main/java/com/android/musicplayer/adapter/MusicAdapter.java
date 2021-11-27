package com.android.musicplayer.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.icu.text.Transliterator;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.musicplayer.Model.MusicFiles;
import com.android.musicplayer.MusicPlayer;
import com.android.musicplayer.R;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {

    private Context mContext;
    public static ArrayList<MusicFiles> mfiles;

    public MusicAdapter(Context mContext, ArrayList<MusicFiles> mFiles) {
        this.mfiles = mFiles;
        this.mContext = mContext;
    }

    public MusicAdapter() {
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);


        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.fileName.setText(mfiles.get(position).getTitle());
        byte[] image = getAlbumArt(mfiles.get(position).getPath());
        if (image != null) {
            Glide.with(mContext).asBitmap()
                    .load(image)
                    .into(holder.albumArt);
        } else {
            Glide.with(mContext)
                    .load(R.drawable.ic_music)
                    .into(holder.albumArt);
        }
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, MusicPlayer.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("position", position);
            mContext.startActivity(intent);
        });
        holder.options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                PopupMenu popupMenu = new PopupMenu(mContext,v);
                popupMenu.getMenuInflater().inflate(R.menu.menu,popupMenu.getMenu());
                popupMenu.show();
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {

                        switch (menuItem.getItemId()){
                            case R.id.delete:
                                deleteFile(position,v);
                                break;
                        }
                        return true;
                    }
                });

            }
        });
    }
    private void deleteFile(int position, View v){
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(mfiles.get(position).getId()));

        File file = new File(mfiles.get(position).getPath());
        boolean deleted = file.delete();
        if (deleted){
            mContext.getContentResolver().delete(contentUri, null,null);
            mfiles.remove(position);
             notifyItemRemoved(position);
             notifyItemRangeChanged(position,mfiles.size());
             Snackbar.make(v,"File deleted",Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(v,"Failed to delete, try again",Snackbar.LENGTH_LONG).show();
        }
        notifyItemRemoved(position);
        notifyItemRangeChanged(position,mfiles.size());
        Snackbar.make(v,"File deleted", Snackbar.LENGTH_SHORT).show();
    }


    @Override
    public int getItemCount() {
        return mfiles.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView fileName;
        ImageView albumArt;
        ImageView options;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.music_file_name);
            albumArt = itemView.findViewById(R.id.music_img);
            options = itemView.findViewById(R.id.options);
        }
    }

    private byte[] getAlbumArt(String uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }
    public void updateList(ArrayList<MusicFiles> musicFilesArrayList){
        mfiles = new ArrayList<>();
        mfiles.addAll(musicFilesArrayList);
        notifyDataSetChanged();
    }
}
