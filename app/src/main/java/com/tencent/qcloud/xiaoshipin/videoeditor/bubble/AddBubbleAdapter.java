package com.tencent.qcloud.xiaoshipin.videoeditor.bubble;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.videoeditor.bubble.ui.bubble.TCBubbleViewParams;
import com.tencent.qcloud.xiaoshipin.videoeditor.bubble.ui.popwin.TCWordParamsInfo;
import com.tencent.qcloud.xiaoshipin.videoeditor.bubble.utils.TCBubbleInfo;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.BaseRecyclerAdapter;

import java.io.IOException;
import java.util.List;


/**
 * Created by vinsonswang on 2017/12/12.
 */

public class AddBubbleAdapter extends BaseRecyclerAdapter<AddBubbleAdapter.AddPasterViewHolder> {
    public static final int TYPE_FOOTER = 0;  // 带有Footer的
    public static final int TYPE_NORMAL = 1;  // 真实数据

    private Context mContext;
    private View mFooterView;

    private List<TCBubbleViewParams> mBubbleInfoList;
    private int mCurrentSelectedPos = -1;

    public AddBubbleAdapter(List<TCBubbleViewParams> bubbleInfoList, Context context){
        mBubbleInfoList = bubbleInfoList;
        mContext = context;
    }

    public void setFooterView(View footerView){
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
        if(mFooterView == null){
            return TYPE_NORMAL;
        }
        if(position == getItemCount() - 1){
            return TYPE_FOOTER;
        }
        return TYPE_NORMAL;
    }

    @Override
    public void onBindVH(AddPasterViewHolder holder, int position) {
        if(getItemViewType(position) == TYPE_FOOTER){
            return;
        }
        TCBubbleInfo tcBubbleInfo = null;
        TCBubbleViewParams tcBubbleViewParams = mBubbleInfoList.get(position);
        if(tcBubbleViewParams != null){
            TCWordParamsInfo tcWordParamsInfo = tcBubbleViewParams.wordParamsInfo;
            if(tcWordParamsInfo != null){
                tcBubbleInfo = tcWordParamsInfo.getBubbleInfo();
            }
        }
        String bubblePath = null;
        if(tcBubbleInfo != null){
            bubblePath = tcBubbleInfo.getIconPath();
        }
        if(!TextUtils.isEmpty(bubblePath)){
            try {
                holder.ivAddPaster.setImageBitmap(BitmapFactory.decodeStream(mContext.getAssets().open(bubblePath)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(tcBubbleViewParams != null){
            holder.tvAddPasterText.setText(TextUtils.isEmpty(tcBubbleViewParams.text) ? "" : tcBubbleViewParams.text);
        }

        if(mCurrentSelectedPos == position){
            holder.ivAddPasterTint.setVisibility(View.VISIBLE);
        }else{
            holder.ivAddPasterTint.setVisibility(View.GONE);
        }
    }

    @Override
    public AddPasterViewHolder onCreateVH(ViewGroup parent, int viewType) {
        if(mFooterView != null && viewType == TYPE_FOOTER){
            return new AddPasterViewHolder(mFooterView);
        }
        return new AddPasterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_paster, parent, false));
    }

    @Override
    public int getItemCount() {
        if(mFooterView != null){
            return mBubbleInfoList.size() + 1;
        }
        return mBubbleInfoList.size();
    }

    public class AddPasterViewHolder extends RecyclerView.ViewHolder{
        ImageView ivAddPaster;
        ImageView ivAddPasterTint;
        TextView tvAddPasterText;
        public AddPasterViewHolder(View itemView) {
            super(itemView);
            if(itemView == mFooterView){
                return;
            }
            ivAddPaster = (ImageView) itemView.findViewById(R.id.add_paster_image);
            ivAddPasterTint = (ImageView) itemView.findViewById(R.id.add_paster_tint);
            tvAddPasterText = (TextView) itemView.findViewById(R.id.add_paster_tv_name);
            tvAddPasterText.setSingleLine(true);
            tvAddPasterText.setEllipsize(TextUtils.TruncateAt.END);
            tvAddPasterText.setMaxEms(4);
        }
    }
}
