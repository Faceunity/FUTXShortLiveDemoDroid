package com.tencent.qcloud.xiaoshipin.videoeditor.bgm;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.videochoose.TCVideoFileInfo;
import com.tencent.qcloud.xiaoshipin.videoeditor.bgm.utils.TCBGMInfo;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.BaseRecyclerAdapter;

import java.util.List;

/**
 * Created by hanszhli on 2017/6/15.
 */

public class TCMusicAdapter extends BaseRecyclerAdapter<TCMusicAdapter.LinearMusicViewHolder> implements View.OnClickListener {
    private static final String TAG = "TCMusicAdapter";
    private Context mContext;
    private List<TCBGMInfo> mBGMList;
    private int mCurrentPos;
    private int mLastPos = -1;

    public TCMusicAdapter(Context context, List<TCBGMInfo> list) {
        mContext = context;
        mBGMList = list;
    }

    @Override
    public LinearMusicViewHolder onCreateVH(ViewGroup parent, int viewType) {
        return new LinearMusicViewHolder(View.inflate(parent.getContext(), R.layout.item_editer_bgm, null));
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @Override
    public void onBindVH(LinearMusicViewHolder holder, int position) {
        TCBGMInfo info = mBGMList.get(position);

        if (info.status == TCBGMInfo.STATE_UNDOWNLOAD) {
            holder.btnUse.setText(mContext.getString(R.string.download));
        } else if (info.status == TCBGMInfo.STATE_DOWNLOADED) {
            holder.btnUse.setText(mContext.getString(R.string.downloaded));
        } else if (info.status == TCBGMInfo.STATE_USED) {
            holder.btnUse.setText(mContext.getString(R.string.use));
        } else if (info.status == TCBGMInfo.STATE_DOWNLOADING){
            holder.btnUse.setText(mContext.getString(R.string.downloading));
        }
        Log.d(TAG, "onBindVH   info.status:" + info.status);

        holder.tvName.setText(info.name);
        holder.itemView.setTag(position);
        if (mCurrentPos == position) {
            holder.progressBar.setProgress(info.progress);
            Log.d("lyj", "onBindVH   position:" + position);
        }
        if (mOnItemClickListener != null) {
            mCurrentPos = position;
            holder.setOnItemClickListener(mOnItemClickListener, position);
        }
    }

    @Override
    public void onBindViewHolder(LinearMusicViewHolder holder, int position, List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public int getItemCount() {
        return mBGMList.size();
    }

    public void changeUseSelection(int position) {
        if (mLastPos != -1) {
            mBGMList.get(mLastPos).status = TCBGMInfo.STATE_DOWNLOADED;
        }
        notifyItemChanged(mLastPos);

        TCBGMInfo info = mBGMList.get(position);
        info.status = TCBGMInfo.STATE_USED;
        notifyItemChanged(position);

        mLastPos = position;
    }

    public void updateProgress(int progress) {
        TCBGMInfo info = mBGMList.get(mCurrentPos);
        info.status = TCBGMInfo.STATE_DOWNLOADING;
        info.progress = progress;
        notifyItemChanged(mCurrentPos);
        Log.d(TAG, "updateProgress mCurrentPos:" + mCurrentPos + ",progress:" + progress);
    }

    public static class LinearMusicViewHolder extends RecyclerView.ViewHolder {
        private Button btnUse;
        private TextView tvName;
        private ProgressBar progressBar;
        private OnItemClickListener onItemClickListener;
        private int position;

        public LinearMusicViewHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.bgm_tv_name);
            btnUse = (Button) itemView.findViewById(R.id.btn_use);
            btnUse.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null)
                        onItemClickListener.onItemClick(v, position);
                }
            });
            progressBar = (ProgressBar) itemView.findViewById(R.id.bar_progress);
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener, int position) {
            this.onItemClickListener = onItemClickListener;
            this.position = position;
        }
    }


}
