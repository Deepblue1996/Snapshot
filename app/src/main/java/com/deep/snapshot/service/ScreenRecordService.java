package com.deep.snapshot.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.deep.dpwork.util.Lag;
import com.deep.snapshot.R;
import com.deep.snapshot.core.CoreApp;

import java.nio.ByteBuffer;

public class ScreenRecordService extends Service {

    private static final String TAG = "ScreenRecordingService";

    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    private int mResultCode;
    private Intent mResultData;
    /**
     * 是否为标清视频
     */
    private boolean isVideoSd;
    /**
     * 是否开启音频录制
     */
    private boolean isAudio;

    private boolean running;

    private Handler mHandler;

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;

    public static ScreenRecordService screenRecordService;

    public static ListenerServiceInter listenerServiceInter;

    public interface ListenerServiceInter {
        void initDo();
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        mHandler = new Handler();
        Log.i(TAG, "Service onCreate() is called");
    }

    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, CoreApp.class); //点击后跳转的界面，可以设置跳转数据

        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("is running......") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        Log.i(TAG, "Service onStartCommand() is called");
        createNotificationChannel();

        mResultCode = intent.getIntExtra("code", -1);
        mResultData = intent.getParcelableExtra("data");
        mScreenWidth = intent.getIntExtra("width", 720);
        mScreenHeight = intent.getIntExtra("height", 1280);
        mScreenDensity = intent.getIntExtra("density", 1);
        isVideoSd = intent.getBooleanExtra("quality", true);
        isAudio = intent.getBooleanExtra("audio", true);

        mMediaProjection = createMediaProjection();
        mVirtualDisplay = createVirtualDisplay(); // 必须在mediaRecorder.prepare() 之后调用，否则报错"fail to get surface"

        screenRecordService = this;

        ScreenRecordService.listenerServiceInter.initDo();

        startRecord();

//        YuvUtil.init(mScreenWidth, mScreenHeight, mScreenWidth, mScreenHeight);

        return super.onStartCommand(intent, flags, startId);
        //return Service.START_NOT_STICKY;
    }

    private MediaProjection createMediaProjection() {
        Log.i(TAG, "Create MediaProjection");
        return ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE)).getMediaProjection(mResultCode, mResultData);
    }

    @SuppressLint("WrongConstant")
    private VirtualDisplay createVirtualDisplay() {

        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 3);

        Log.i(TAG, "Create VirtualDisplay");
        return mMediaProjection.createVirtualDisplay(TAG, mScreenWidth, mScreenHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
    }

    public boolean startRecord() {
        if (mMediaProjection == null || running) {
            Lag.i("报错: mMediaProjection:" + mMediaProjection + " running:" + running);
            return false;
        }

        mVirtualDisplay = createVirtualDisplay();

        Lag.i("开始录屏");

        initRecorder();

        running = true;
        return true;
    }

    public boolean isRunning() {
        return running;
    }

    private boolean hasInit = false;

    private void initRecorder() {
        mImageReader.setOnImageAvailableListener(reader -> {
            Lag.i("录屏每一帧");
            Image image;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    final Image.Plane[] planes = image.getPlanes();
                    if (planes.length > 0) {
                        final ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * mScreenWidth;

                        int dw = mScreenWidth + rowPadding / pixelStride;
                        int dh = mScreenHeight;

                        Lag.i("onImageAvailable width:" + dw + " height:" + dh);

                        byte[] rgbaBytes = new byte[buffer.remaining()];
                        buffer.get(rgbaBytes);

                        if (bytesListener != null) {
                            bytesListener.listenByteRGBA(rgbaBytes, dw, dh, 0);
                        }
                    }
                }
                if (image != null) {
                    image.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, mHandler);
    }

    private BitmapListener bitmapListener;

    public void setBitmapListener(BitmapListener bitmapListener) {
        this.bitmapListener = bitmapListener;
    }

    public interface BitmapListener {
        void listenBitmap(Bitmap bitmap);
    }

    private BytesListener bytesListener;

    public void setBytesListener(BytesListener bytesListener) {
        this.bytesListener = bytesListener;
    }

    public interface BytesListener {
        void listenByteRGBA(byte[] data, int width, int height, int cameraOrientation);
    }

    public boolean stopRecord() {
        if (!running) {
            return false;
        }
        running = false;

        mImageReader.close();

        mVirtualDisplay.release();
        //mediaProjection.stop();

        return true;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.i(TAG, "Service onDestroy");
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

}
