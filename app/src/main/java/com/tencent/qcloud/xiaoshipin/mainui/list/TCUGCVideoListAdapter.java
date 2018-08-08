package com.tencent.qcloud.xiaoshipin.mainui.list;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.TCUtils;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.BaseRecyclerAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 小视频列表的Adapter
 * 列表项布局格式: R.layout.listview_ugc_item
 * 列表项数据格式: TCLiveInfo
 */

public class TCUGCVideoListAdapter extends BaseRecyclerAdapter<TCUGCVideoListAdapter.VideoVideoHolder> {
    private List<TCVideoInfo> mList;

    public TCUGCVideoListAdapter(List<TCVideoInfo> list) {
        mList = list;
    }

    @Override
    public void onBindVH(VideoVideoHolder holder, int position) {
        TCVideoInfo data = mList.get(position);
        //UGC预览图
        String cover = data.frontcover;
        if (TextUtils.isEmpty(cover)) {
            holder.ivCover.setImageResource(R.drawable.bg_ugc);
        } else {
            RequestManager req = Glide.with(holder.itemView.getContext());
            req.load(cover).placeholder(R.drawable.bg_ugc).into(holder.ivCover);
        }
        //主播头像
        TCUtils.showPicWithUrl(holder.itemView.getContext(), holder.ivAvatar, data.headpic, R.drawable.face);
        //主播昵称
        if (TextUtils.isEmpty(data.nickname) || "null".equals(data.nickname)) {
            holder.tvHost.setText(TCUtils.getLimitString(data.userid, 10));
        } else {
            holder.tvHost.setText(TCUtils.getLimitString(data.nickname, 10));
        }
        //小视频创建时间（发布时间）
        holder.tvCreateTime.setText(generateTimeStr(convertTimeToLong(data.createTime)));

        if (data.review_status == TCVideoInfo.REVIEW_STATUS_NORMAL) {
            holder.reviewStatus.setText("正常");
        } else if (data.review_status == TCVideoInfo.REVIEW_STATUS_NOT_REVIEW) { // 审核中
            holder.reviewStatus.setText("审核中");
        } else if (data.review_status == TCVideoInfo.REVIEW_STATUS_PORN) { // 涉黄
            holder.reviewStatus.setText("涉黄");
        }
    }

    public static Long convertTimeToLong(String time) {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            date = sdf.parse(time);
            return date.getTime();
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    @Override
    public VideoVideoHolder onCreateVH(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_ugc_item, null);
        DisplayMetrics dm = parent.getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        itemView.setLayoutParams(new RecyclerView.LayoutParams(width / 2, width / 2));
        return new VideoVideoHolder(itemView);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }


    public String generateTimeStr(long timestamp) {
        String result = "刚刚";
        long timeDistanceinSec = (System.currentTimeMillis() - timestamp) / 1000;
        if (timeDistanceinSec >= 60 && timeDistanceinSec < 3600) {
            result = String.format(Locale.CHINA, "%d", (timeDistanceinSec) / 60) + "分钟前";
        } else if (timeDistanceinSec >= 3600 && timeDistanceinSec < 60 * 60 * 24) {
            result = String.format(Locale.CHINA, "%d", (timeDistanceinSec) / 3600) + "小时前";
        } else if (timeDistanceinSec >= 3600 * 24) {
            result = String.format(Locale.CHINA, "%d", (timeDistanceinSec) / (3600 * 24)) + "天前";
        }
        return result;
    }


    public static class VideoVideoHolder extends RecyclerView.ViewHolder {
        TextView tvHost;
        ImageView ivCover;
        ImageView ivAvatar;
        TextView tvCreateTime;
        TextView reviewStatus;

        public VideoVideoHolder(View itemView) {
            super(itemView);
            ivCover = (ImageView) itemView.findViewById(R.id.cover);
            tvHost = (TextView) itemView.findViewById(R.id.host_name);
            ivAvatar = (ImageView) itemView.findViewById(R.id.avatar);
            tvCreateTime = (TextView) itemView.findViewById(R.id.create_time);
            reviewStatus = (TextView) itemView.findViewById(R.id.review_status);
        }
    }
}


