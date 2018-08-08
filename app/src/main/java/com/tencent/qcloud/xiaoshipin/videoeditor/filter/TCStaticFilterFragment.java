package com.tencent.qcloud.xiaoshipin.videoeditor.filter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.videoeditor.BaseEditFragment;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoEditerWrapper;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.BaseRecyclerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hans on 2017/11/6.
 * 静态滤镜的Fragment
 */
public class TCStaticFilterFragment extends BaseEditFragment implements BaseRecyclerAdapter.OnItemClickListener {
    private static final int[] FILTER_ARR = {
            R.drawable.filter_langman, R.drawable.filter_qingxin,
            R.drawable.filter_weimei, R.drawable.filter_fennen,
            R.drawable.filter_huaijiu, R.drawable.filter_landiao,
            R.drawable.filter_qingliang, R.drawable.filter_rixi};

    private List<Integer> mFilterList;
    private List<String> mFilerNameList;
    private RecyclerView mRvFilter;
    private StaticFilterAdapter mAdapter;
    private int mCurrentPosition = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_static_filter, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFilterList = new ArrayList<Integer>();
        mFilterList.add(R.drawable.orginal);
        mFilterList.add(R.drawable.langman);
        mFilterList.add(R.drawable.qingxin_fu);
        mFilterList.add(R.drawable.weimei);
        mFilterList.add(R.drawable.fennen);
        mFilterList.add(R.drawable.huaijiu);
        mFilterList.add(R.drawable.landiao);
        mFilterList.add(R.drawable.qingliang);
        mFilterList.add(R.drawable.rixi);


        mFilerNameList = new ArrayList<>();
        mFilerNameList.add("原图");
        mFilerNameList.add("浪漫");
        mFilerNameList.add("清新");
        mFilerNameList.add("唯美");
        mFilerNameList.add("粉嫩");
        mFilerNameList.add("怀旧");
        mFilerNameList.add("蓝调");
        mFilerNameList.add("清凉");
        mFilerNameList.add("日系");

        mRvFilter = (RecyclerView) view.findViewById(R.id.paster_rv_list);
        mRvFilter.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mAdapter = new StaticFilterAdapter(mFilterList, mFilerNameList);
        mAdapter.setOnItemClickListener(this);
        mRvFilter.setAdapter(mAdapter);

        mCurrentPosition = TCStaticFilterViewInfoManager.getInstance().getCurrentPosition();
        mAdapter.setCurrentSelectedPos(mCurrentPosition);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        TCStaticFilterViewInfoManager.getInstance().setCurrentPosition(mCurrentPosition);
    }

    @Override
    public void onItemClick(View view, int position) {
        Bitmap bitmap = null;
        if (position == 0) {
            bitmap = null;  // 没有滤镜
        } else {
            bitmap = BitmapFactory.decodeResource(getResources(), FILTER_ARR[position - 1]);
        }
        mAdapter.setCurrentSelectedPos(position);
        // 设置滤镜图片
        TCVideoEditerWrapper.getInstance().getEditer().setFilter(bitmap);

        mCurrentPosition = position;
    }

}
