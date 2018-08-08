package com.tencent.qcloud.xiaoshipin.videojoiner;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.TCUtils;
import com.tencent.qcloud.xiaoshipin.videochoose.TCVideoFileInfo;
import com.tencent.qcloud.xiaoshipin.videojoiner.widget.swipemenu.SwipeMenuAdapter;

import java.io.File;
import java.util.ArrayList;

public class MenuAdapter extends SwipeMenuAdapter<MenuAdapter.DefaultViewHolder> {

    private Context mContext;
    private ArrayList<TCVideoFileInfo> mTCVideoFileInfoList;
    private ItemView.OnDeleteListener mOnDeleteListener;

    public MenuAdapter(Context context, ArrayList<TCVideoFileInfo> fileInfos) {
        mContext = context;
        this.mTCVideoFileInfoList = fileInfos;
    }

    public void removeIndex(int position) {
        this.mTCVideoFileInfoList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mTCVideoFileInfoList.size());
    }

    public void addItem(TCVideoFileInfo fileInfo) {
        this.mTCVideoFileInfoList.add(fileInfo);
        notifyItemInserted(mTCVideoFileInfoList.size());
    }

    public void setOnItemDeleteListener(ItemView.OnDeleteListener onDeleteListener) {
        this.mOnDeleteListener = onDeleteListener;
    }

    @Override
    public int getItemCount() {
        return mTCVideoFileInfoList == null ? 0 : mTCVideoFileInfoList.size();
    }

    public TCVideoFileInfo getItem(int position){
        return mTCVideoFileInfoList.get(position);
    }

    @Override
    public View onCreateContentView(ViewGroup parent, int viewType) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.swipe_menu_item, parent, false);
    }

    @Override
    public MenuAdapter.DefaultViewHolder onCompatCreateViewHolder(View realContentView, int viewType) {
        return new DefaultViewHolder(realContentView);
    }

    @Override
    public void onBindViewHolder(MenuAdapter.DefaultViewHolder holder, int position) {
        TCVideoFileInfo fileInfo = mTCVideoFileInfoList.get(position);
        holder.setDuration(TCUtils.duration(fileInfo.getDuration()));
        holder.setOnDeleteListener(mOnDeleteListener);
        Glide.with(mContext).load(Uri.fromFile(new File(fileInfo.getFilePath()))).into(holder.ivThumb);
    }

    public ArrayList<TCVideoFileInfo> getAll() {
        return mTCVideoFileInfoList;
    }

    static class DefaultViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView ivThumb;
        TextView tvDuration;
        ImageView ivDelete;
        ItemView.OnDeleteListener mOnDeleteListener;

        public DefaultViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            ivThumb = (ImageView) itemView.findViewById(R.id.iv_icon);
            tvDuration = (TextView) itemView.findViewById(R.id.tv_duration);
            ivDelete = (ImageView) itemView.findViewById(R.id.iv_close);
            ivDelete.setOnClickListener(this);
        }

        public void setOnDeleteListener(ItemView.OnDeleteListener onDeleteListener) {
            this.mOnDeleteListener = onDeleteListener;
        }

        public void setDuration(String duration) {
            this.tvDuration.setText(duration);
        }

        @Override
        public void onClick(View v) {
            if (mOnDeleteListener != null) {
                mOnDeleteListener.onDelete(getAdapterPosition());
            }
        }
    }

}
