package com.tencent.qcloud.xiaoshipin.videoeditor.time.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.tencent.qcloud.xiaoshipin.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuejiaoli on 2017/4/30.
 */

public class TCVideoEditerAdapter extends RecyclerView.Adapter<TCVideoEditerAdapter.ViewHolder> {
    private final Context mContext;
    private ArrayList<Bitmap> data = new ArrayList<Bitmap>();

    public TCVideoEditerAdapter(Context context) {
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int height = mContext.getResources().getDimensionPixelOffset(R.dimen.ugc_item_thumb_height);
        ImageView view = new ImageView(parent.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(height, height));
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.thumb.setImageBitmap(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void add(int position, Bitmap b) {
        data.add(b);
        notifyItemInserted(position);
    }

    public void clearAllBitmap() {
        data.clear();
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumb;

        public ViewHolder(View itemView) {
            super(itemView);
            thumb = (ImageView) itemView;
        }
    }

    public void setBitmapList(List<Bitmap> bitmap) {
        data.clear();
        data.addAll(bitmap);
        notifyDataSetChanged();
    }
}
