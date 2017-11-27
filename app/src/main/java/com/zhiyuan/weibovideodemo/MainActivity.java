package com.zhiyuan.weibovideodemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import java.util.ArrayList;
import java.util.List;

import fm.jiecao.jcvideoplayer_lib.JCVideoPlayerStandard;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "";
    private RecyclerView mRecyclerView;
    private int mSreenHeight;
    private LinearLayoutManager linearLayoutManager;
    private ArrayList<LocalVideoBean> datas = new ArrayList();
    private CommonAdapter<LocalVideoBean> mAdapter;
    private int mCurPos;
    private JCVideoPlayerStandard mPlayView;
    /** 上一次播放位置 */
    private int lastPos;
    /** 这一次该播放位置 */
    private int curPos;
    private View curShadow;
    private RelativeLayout curLLplay;
    private RelativeLayout lastLLplay;
    private View lastShadow;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();
        init();
        initData();
    }

    private void initPermission() {
        requestReadExternalPermission();
    }
    @SuppressLint("NewApi")
    private void requestReadExternalPermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ permission IS NOT granted...");

            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {

                Log.d(TAG, "11111111111111");
            } else {
                // 0 是自己定义的请求coude
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                Log.d(TAG, "222222222222");
            }
        } else {
            Log.d(TAG, "READ permission is granted...");
        }
    }
    private void initData() {
        datas.addAll(getList(getApplicationContext()));
        datas.add(new LocalVideoBean());
        mAdapter.notifyDataSetChanged();

        mRecyclerView.smoothScrollBy(0,3);
    }

    private List<LocalVideoBean> getList(Context context) {
        List<LocalVideoBean> sysVideoList = new ArrayList<>();
        // MediaStore.Video.Thumbnails.DATA:视频缩略图的文件路径
        String[] thumbColumns = {MediaStore.Video.Thumbnails.DATA,
                MediaStore.Video.Thumbnails.VIDEO_ID};
        // 视频其他信息的查询条件
        String[] mediaColumns = {MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA, MediaStore.Video.Media.DURATION};

        Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media
                        .EXTERNAL_CONTENT_URI,
                mediaColumns, null, null, null);

        if (cursor == null) {
            return sysVideoList;
        }
        if (cursor.moveToFirst()) {
            do {
                LocalVideoBean info = new LocalVideoBean();

                info.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media
                        .DATA)));
                info.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video
                        .Media.DURATION)));
                sysVideoList.add(info);
            } while (cursor.moveToNext());
        }
        return sysVideoList;
    }


    private void init() {
        mPlayView = new JCVideoPlayerStandard(this);
        mSreenHeight = getResources().getDisplayMetrics().heightPixels;
        mRecyclerView = findViewById(R.id.rv_part_detail);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mAdapter = new CommonAdapter<LocalVideoBean>(getApplicationContext(), R.layout.item,datas) {
            @Override
            protected void convert(ViewHolder holder, LocalVideoBean localVideoBean, int position) {
                if(position == datas.size()-1){
                    ImageView ivPlayIcon = holder.getView(R.id.iv_icon);
                    ivPlayIcon.setVisibility(View.GONE);
                    return;
                }
                ImageView ivCover = holder.getView(R.id.iv_cover);
                Glide.with(getApplicationContext()).load(localVideoBean.getPath()).into(ivCover);
            }
        };
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if(newState==RecyclerView.SCROLL_STATE_IDLE){
                    changeCenter();
                }
            }
        });
    }

    private void changeCenter() {
        curPos = chargeWhichInCenter();

        //切换视频:  上一个还原遮罩,当前添加遮罩；上一个移除播放器，当前项添加播放器
        if(curPos != lastPos){

            if(lastLLplay != null){
                lastLLplay.removeView(mPlayView);
            }

            if(lastShadow != null){
                alphaAnim(lastShadow,0,1);
                lastShadow.setVisibility(View.VISIBLE);
            }

            curShadow = linearLayoutManager.findViewByPosition(curPos).findViewById(R.id.view_shadow);
            curLLplay = linearLayoutManager.findViewByPosition(curPos).findViewById(R.id.rl_curplay);

            curLLplay.addView(mPlayView);
            setRealHeight(mPlayView);
            alphaAnim(curShadow,1,0);
            curShadow.setVisibility(View.GONE);

            mPlayView.setUp(datas.get(curPos).getPath(),JCVideoPlayerStandard.SCREEN_LAYOUT_NORMAL,"标题");
            mPlayView.startVideo();

            lastPos = curPos;
            lastShadow = curShadow;
            lastLLplay = curLLplay;
        }

    }

    private void alphaAnim(View view, float fromAlpha, float toAlpha) {
        AlphaAnimation anim = new AlphaAnimation(fromAlpha,toAlpha);
        anim.setDuration(800);
        view.startAnimation(anim);
    }

    private void setRealHeight(JCVideoPlayerStandard view) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
        layoutParams.height = dp2px(200);
        view.setLayoutParams(layoutParams);
    }

    private int dp2px(int dp) {
        return (int) (getResources().getDisplayMetrics().density*dp+0.5);
    }

    private int chargeWhichInCenter() {
        int firstPosition = linearLayoutManager.findFirstVisibleItemPosition();
        int lastPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();
        int curPosition=firstPosition;
        for(int i=firstPosition;i<lastPosition;i++){
            if(linearLayoutManager.findViewByPosition(i)!=null&&linearLayoutManager.findViewByPosition(i).findViewById(R.id.ll_item)!=null){
                RelativeLayout curLlItem=linearLayoutManager.findViewByPosition(i).findViewById(R.id.ll_item);
                if(curLlItem.getTop()<mSreenHeight/2&&curLlItem.getBottom()>mSreenHeight/2){
                    curPosition=i;
                }
            }
        }
        return curPosition;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d(TAG, "requestCode=" + requestCode + "; --->" + permissions.toString()
                + "; grantResult=" + grantResults.toString());
        switch (requestCode) {
            case 0: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted
                    // request successfully, handle you transactions

                } else {

                    // permission denied
                    // request failed
                }

                return;
            }
            default:
                break;

        }
    }
}
