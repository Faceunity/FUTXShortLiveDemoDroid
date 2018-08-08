package com.tencent.qcloud.xiaoshipin.videoeditor.paster;

import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.BaseRecyclerAdapter;

import java.util.List;


/**
 * Created by vinsonswang on 2017/12/12.
 */

public class AddPasterAdapter extends BaseRecyclerAdapter<AddPasterAdapter.AddPasterViewHolder> {
    public static final int TYPE_FOOTER = 0;  // 带有Footer的
    public static final int TYPE_NORMAL = 1;  // 真实数据

    private View mFooterView;

    private List<TCPasterInfo> mPasterInfoList;
    private int mCurrentSelectedPos = -1;

    public AddPasterAdapter(List<TCPasterInfo> pasterInfoList) {
        mPasterInfoList = pasterInfoList;
    }

    public void setFooterView(View footerView) {
        mFooterView = footerView;
        notifyItemInserted(getItemCount() - 1);
    }

    public void setCurrentSelectedPos(int pos) {
        int tPos = mCurrentSelectedPos;
        mCurrentSelectedPos = pos;
        this.notifyItemChanged(tPos);
        this.notifyItemChanged(mCurrentSelectedPos);
    }

    @Override
    public int getItemViewType(int position) {
        if (mFooterView == null) {
            return TYPE_NORMAL;
        }
        if (position == getItemCount() - 1) {
            return TYPE_FOOTER;
        }
        return TYPE_NORMAL;
    }

    @Override
    public void onBindVH(AddPasterViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_FOOTER) {
            return;
        }
        String pasterPath = mPasterInfoList.get(position).getIconPath();
        if (!TextUtils.isEmpty(pasterPath)) {
            holder.ivAddPaster.setImageBitmap(BitmapFactory.decodeFile(pasterPath));
        }
        holder.tvAddPasterText.setText("贴纸" + String.valueOf(position + 1));
        if (mCurrentSelectedPos == position) {
            holder.ivAddPasterTint.setVisibility(View.VISIBLE);
        } else {
            holder.ivAddPasterTint.setVisibility(View.GONE);
        }
    }

    @Override
    public AddPasterViewHolder onCreateVH(ViewGroup parent, int viewType) {
        if (mFooterView != null && viewType == TYPE_FOOTER) {
            return new AddPasterViewHolder(mFooterView);
        }
        return new AddPasterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_paster, parent, false));
    }

    @Override
    public int getItemCount() {
        if (mFooterView != null) {
            return mPasterInfoList.size() + 1;
        }
        return mPasterInfoList.size();
    }

    public class AddPasterViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAddPaster;
        ImageView ivAddPasterTint;
        TextView tvAddPasterText;

        public AddPasterViewHolder(View itemView) {
            super(itemView);
            if (itemView == mFooterView) {
                return;
            }
            ivAddPaster = (ImageView) itemView.findViewById(R.id.add_paster_image);
            ivAddPasterTint = (ImageView) itemView.findViewById(R.id.add_paster_tint);
            tvAddPasterText = (TextView) itemView.findViewById(R.id.add_paster_tv_name);
        }
    }
}
