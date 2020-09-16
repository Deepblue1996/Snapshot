package com.deep.snapshot.view;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.deep.dpwork.annotation.DpLayout;
import com.deep.dpwork.dialog.DialogScreen;
import com.deep.dpwork.dialog.DpDialogScreen;
import com.deep.dpwork.util.DBUtil;
import com.deep.snapshot.R;
import com.deep.snapshot.base.TBaseScreen;
import com.deep.snapshot.core.CoreApp;
import com.deep.snapshot.event.UpdateViewEvent;
import com.deep.snapshot.util.FileUtil;
import com.deep.snapshot.util.ScreenUtil;
import com.github.florent37.viewanimator.ViewAnimator;
import com.prohua.roundlayout.RoundAngleFrameLayout;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

@DpLayout(R.layout.photo_screen_layout)
public class PhotoScreen extends TBaseScreen {

    @BindView(R.id.viewPager)
    public ViewPager viewPager;// 声明一个ViewPager
    @BindView(R.id.delTouch)
    public LinearLayout delTouch;
    @BindView(R.id.layoutPlay)
    public RelativeLayout layoutPlay;
    @BindView(R.id.iv_pauseView)
    public ImageView iv_pauseView;
    @BindView(R.id.videoView)
    public SurfaceView videoView;
    @BindView(R.id.backTouchLin)
    public LinearLayout backTouchLin;
    @BindView(R.id.connectToastText)
    public TextView connectToastText;
    @BindView(R.id.connectToastLin)
    public RoundAngleFrameLayout connectToastLin;
    private List<View> pageView = new ArrayList<>();// 声明一个存放视图的集合

    private List<File> imgList;

    private MediaPlayer mediaPlayer;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void init() {

        ScreenUtil.switchFullScreen(_dpActivity, false);

        imgList = FileUtil.GetFileName("./sdcard/DCIM/Camera/");

        for (int i = 0; i < imgList.size(); i++) {

            // 获取不同布局文件的View视图
            View viewTemp = LayoutInflater.from(_dpActivity).inflate(
                    R.layout.viewpager_item_layout, null);
            ImageView imageView = viewTemp.findViewById(R.id.imageView);
            ImageView imagePlayView = viewTemp.findViewById(R.id.iv_playView);
            if (imgList.get(i).getName().trim().toLowerCase().endsWith(".mp4")) {
                imagePlayView.setVisibility(View.VISIBLE);
            } else {
                imagePlayView.setVisibility(View.GONE);
            }
            Glide.with(_dpActivity).load(imgList.get(i)).apply((new RequestOptions()).fitCenter().placeholder(0)).into(imageView);

            imagePlayView.setOnClickListener(v -> {
                if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
                    setVideo(imgList.get(viewPager.getCurrentItem()));
                    iv_pauseView.setVisibility(View.VISIBLE);
                } else {
                    freeVideo();
                }
            });

            pageView.add(viewTemp);
        }

        iv_pauseView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                freeVideo();
            }
        });

        // 数据适配器
        PagerAdapter mPagerAdapter = new PagerAdapter() {

            @Override
            // 获取当前窗体界面数
            public int getCount() {
                // TODO Auto-generated method stub
                return pageView.size();
            }

            @Override
            public int getItemPosition(@NonNull Object object) {
                return POSITION_NONE;
            }

            @Override
            // 断是否由对象生成界面
            public boolean isViewFromObject(View arg0, Object arg1) {
                // TODO Auto-generated method stub
                return arg0 == arg1;
            }

            // 是从ViewGroup中移出当前View
            public void destroyItem(View arg0, int arg1, Object arg2) {
                ((ViewPager) arg0).removeView(pageView.get(arg1));
            }

            // 返回一个对象，这个对象表明了PagerAdapter适配器选择哪个对象放在当前的ViewPager中
            public Object instantiateItem(View arg0, int arg1) {
                ((ViewPager) arg0).addView(pageView.get(arg1));
                return pageView.get(arg1);
            }

        };

        // 绑定适配器
        viewPager.setAdapter(mPagerAdapter);

        delTouch.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    delTouch.setAlpha(0.5f);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    delTouch.setAlpha(1.0f);
                    break;
                case MotionEvent.ACTION_UP:
                    delTouch.setAlpha(1.0f);
                    freeVideo();
                    DpDialogScreen.create().setMsg(getString(R.string.del_photo))
                            .addButton(getContext(), getString(R.string.ok), R.color.dialogColor,
                                    dialogScreen -> dialogScreen.close(() -> {
                                        if (FileUtil.deleteFile(imgList.get(viewPager.getCurrentItem()).getAbsolutePath())) {
                                            //ToastUtil.showSuccess(lang(R.string.shanchu_success));
                                            toast(lang(R.string.shanchu_success));
                                            imgList.remove(viewPager.getCurrentItem());
                                            pageView.remove(viewPager.getCurrentItem());
                                            mPagerAdapter.notifyDataSetChanged();
                                            CoreApp.appData.photoPath = imgList.get(0).getAbsolutePath();
                                            DBUtil.save(CoreApp.appData);
                                            EventBus.getDefault().post(new UpdateViewEvent());
                                        } else {
                                            //ToastUtil.showError(lang(R.string.shanchu_failed));
                                            toast(lang(R.string.shanchu_failed));
                                        }
                                    }))
                            .addButton(getContext(), getString(R.string.no), R.color.dialogColor, DialogScreen::close).open(getFragmentManager());
                    break;
            }
            return true;
        });

        backTouchLin.setOnClickListener(v -> close());
    }

    private class SurfaceViewLis implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            //setVideo(imgList.get(viewPager.getCurrentItem()));
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }

    }

    public void freeVideo() {
        videoView.setVisibility(View.GONE);
        layoutPlay.setVisibility(View.GONE);
        iv_pauseView.setVisibility(View.GONE);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * 更换视频背景
     */
    public void setVideo(File file) {
        videoView.setVisibility(View.VISIBLE);
        layoutPlay.setVisibility(View.VISIBLE);

        final Uri uri = Uri.fromFile(file);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();
        videoView.getHolder().setKeepScreenOn(true);
        videoView.getHolder().addCallback(new SurfaceViewLis());

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setLooping(false);
        mediaPlayer.seekTo(0);

        try {
            mediaPlayer.setDataSource(_dpActivity, uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.prepareAsync();

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                try {
                    //mediaPlayer.setVolume(0, 0);
                    mediaPlayer.setDisplay(videoView.getHolder());

                    // 首先取得video的宽和高
                    int vWidth = mediaPlayer.getVideoWidth();
                    int vHeight = mediaPlayer.getVideoHeight();

                    int lw = layoutPlay.getWidth();
                    int lh = layoutPlay.getHeight();

                    if (vWidth < lw || vHeight < lh) {
                        // 如果video的宽或者高超出了当前屏幕的大小，则要进行缩放
                        float wRatio = (float) lw / (float) vWidth;
                        float hRatio = (float) lh / (float) vHeight;

                        // 选择大的一个进行缩放
                        float ratio = Math.max(wRatio, hRatio);
                        vWidth = (int) Math.ceil((float) vWidth * ratio);
                        vHeight = (int) Math.ceil((float) vHeight * ratio);

                        // 设置surfaceView的布局参数
                        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(vWidth, vHeight);
                        videoView.setLayoutParams(lp);
                    }
                    // 然后开始播放视频
                    mediaPlayer.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mp.getCurrentPosition() > 0) {
                    freeVideo();
                }
            }
        });
    }

    public void toast(String msg) {
        connectToastText.setText(msg);
        ViewAnimator.animate(connectToastLin).alpha(0, 1, 1, 1, 1, 0).duration(2000).start();
        //TextToastDialogScreen.prepare(TextToastDialogScreen.class, new TextBaseBean(msg)).open(fragmentManager());
    }
}
