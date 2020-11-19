package com.deep.snapshot.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.SensorManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.alibaba.android.mnnkit.actor.FaceDetector;
import com.alibaba.android.mnnkit.entity.FaceDetectConfig;
import com.alibaba.android.mnnkit.entity.FaceDetectionReport;
import com.alibaba.android.mnnkit.entity.MNNCVImageFormat;
import com.alibaba.android.mnnkit.entity.MNNFlipType;
import com.alibaba.android.mnnkit.intf.InstanceCreatedListener;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.deep.dpwork.annotation.DpLayout;
import com.deep.dpwork.annotation.DpMainScreen;
import com.deep.dpwork.dialog.DialogScreen;
import com.deep.dpwork.dialog.DpDialogScreen;
import com.deep.dpwork.itface.RunUi;
import com.deep.dpwork.util.CountDownTimeTextUtil;
import com.deep.dpwork.util.DBUtil;
import com.deep.dpwork.util.DTimeUtil;
import com.deep.dpwork.util.DisplayUtil;
import com.deep.dpwork.util.ImageUtil;
import com.deep.dpwork.util.Lag;
import com.deep.snapshot.R;
import com.deep.snapshot.base.TBaseScreen;
import com.deep.snapshot.core.CoreApp;
import com.deep.snapshot.event.AddDeviceEvent;
import com.deep.snapshot.event.HideBlackAnimEvent;
import com.deep.snapshot.event.OkPermissionEvent;
import com.deep.snapshot.event.SwitchLangEvent;
import com.deep.snapshot.event.UpdateViewEvent;
import com.deep.snapshot.listener.BlueScanCallback;
import com.deep.snapshot.service.ScreenRecordService;
import com.deep.snapshot.util.BleUtil;
import com.deep.snapshot.util.FileUtil;
import com.deep.snapshot.util.JumpUtil;
import com.deep.snapshot.util.NV21ToBitmap;
import com.deep.snapshot.util.ScreenUtil;
import com.deep.snapshot.view.dialog.BluetoothDialogScreen;
import com.deep.snapshot.view.dialog.TextToastDialogScreen;
import com.deep.snapshot.weight.CameraView;
import com.github.florent37.viewanimator.ViewAnimator;
import com.prohua.roundlayout.RoundAngleFrameLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import ffmpeglib.utils.FFmpegKit;

@DpMainScreen
@DpLayout(R.layout.main_screen_layout)
public class MainScreen extends TBaseScreen {

    @BindView(R.id.smallZhiLin)
    public RelativeLayout smallZhiLin;
    @BindView(R.id.cancelZhiBo)
    public TextView cancelZhiBo;
    @BindView(R.id.bleTouch)
    public ImageView bleTouch;
    @BindView(R.id.zhiBoTouch)
    public ImageView zhiBoTouch;
    @BindView(R.id.takeTouch)
    public ImageView takeTouch;
    @BindView(R.id.flashTouch)
    public ImageView flashTouch;
    @BindView(R.id.switchCamera)
    public ImageView switchCamera;
    @BindView(R.id.screenLock)
    public ImageView screenLock;
    @BindView(R.id.menuTouch)
    public ImageView menuTouch;
    @BindView(R.id.localPic)
    public ImageView localPic;
    @BindView(R.id.photoLin)
    public LinearLayout photoLin;
    @BindView(R.id.centerTvLin)
    public LinearLayout centerTvLin;
    @BindView(R.id.connectToastText)
    public TextView connectToastText;
    @BindView(R.id.connectToastLin)
    public RoundAngleFrameLayout connectToastLin;

    // 前置闪光
    @BindView(R.id.flashScreen)
    public View flashScreen;
    @BindView(R.id.settingScreen)
    public View settingScreen;

    @BindView(R.id.photoTouch)
    public TextView photoTouch;
    @BindView(R.id.videoTouch)
    public TextView videoTouch;
    @BindView(R.id.timeLinTextLin)
    public LinearLayout timeLinTextLin;
    @BindView(R.id.timeLinText)
    public TextView timeLinText;
    @BindView(R.id.beginImg)
    public ImageView beginImg;
    @BindView(R.id.beginTouch)
    public LinearLayout beginTouch;

    @BindView(R.id.picImg)
    public ImageView picImg;

    public static int rotateDegree;// 设备旋转角度：0/90/180/360

    private SurfaceHolder mDrawSurfaceHolder;
    protected CameraView mCameraView;

    // 当前渲染画布的尺寸
    protected int mActualPreviewWidth;
    protected int mActualPreviewHeight;

    private FaceDetector mFaceDetector;

    private Paint KeyLinesPaint = new Paint();
    private Paint KeyPointsPaint = new Paint();
    private Paint PointOrderPaint = new Paint();
    private Paint ScorePaint = new Paint();

    private final static int MAX_RESULT = 10;

    private float[] scores = new float[MAX_RESULT];// 置信度
    private float[] rects = new float[MAX_RESULT * 4];// 矩形区域
    private float[] keypts = new float[MAX_RESULT * 2 * 106];// 脸106关键点

    private float[] scores2 = new float[MAX_RESULT];// 置信度
    private float[] rects2 = new float[MAX_RESULT * 4];// 矩形区域
    private float[] keypts2 = new float[MAX_RESULT * 2 * 106];// 脸106关键点

    private OrientationEventListener mOrientationListener;

    private BleGattCallback bleGattCallback;

    private BlueScanCallback blueScanCallback;

    private Rect point43;

    private long timeTurnLong = 0;

    private int screenWidth = 0;

    private boolean flashBool = false;

    private boolean screenLo = false;

    private int noSwitch = 0;

    private BleDevice bleDevice = null;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void init() {

        ScreenUtil.switchFullScreen(_dpActivity, true);

        setBrightness(_dpActivity, 255);
        // 监听屏幕旋转
        detectScreenRotate();

        initPaint();

        // 创建Kit实例
        createKitInstance();

        runInit();

        initWeight();
    }

    private void initPaint() {
        KeyLinesPaint.setColor((Color.parseColor("#66ffff00")));
        KeyLinesPaint.setStyle(Paint.Style.FILL);
        KeyLinesPaint.setStrokeWidth(2);

        KeyPointsPaint.setColor((Color.parseColor("#ffffff")));
        KeyPointsPaint.setStyle(Paint.Style.FILL);
        KeyPointsPaint.setStrokeWidth(2);

        PointOrderPaint.setColor(Color.GREEN);
        PointOrderPaint.setStyle(Paint.Style.STROKE);
        PointOrderPaint.setStrokeWidth(2f);
        PointOrderPaint.setTextSize(18);

        ScorePaint.setColor(Color.WHITE);
        ScorePaint.setStrokeWidth(2f);
        ScorePaint.setTextSize(40);
    }

    private void setBrightness(Activity activity, int brightness) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.screenBrightness = (float) brightness * (1f / 255f);
        activity.getWindow().setAttributes(lp);
    }

    private TextToastDialogScreen textToastDialogScreen;

    @SuppressLint("ClickableViewAccessibility")
    private void initWeight() {
        screenWidth = DisplayUtil.getMobileWidth(_dpActivity);
        takeTouch.setOnTouchListener((v, event) -> {
            if (noSwitch == 0) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        takeTouch.setImageResource(R.mipmap.ic_take_touch);
                        takeTouch.setAlpha(0.5f);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        takeTouch.setImageResource(R.mipmap.ic_take);
                        takeTouch.setAlpha(1.0f);
                        if (mCameraView.isFrontCamera() && flashBool) {
                            // 效果
                            flashScreen.setVisibility(View.VISIBLE);
                            DTimeUtil.run(100, () -> {
                                flashScreen.setVisibility(View.INVISIBLE);
                                DTimeUtil.run(100, () -> {
                                    flashScreen.setVisibility(View.VISIBLE);
                                    DTimeUtil.run(100, () -> flashScreen.setVisibility(View.INVISIBLE));
                                });
                            });
                        } else {
                            // 效果
                            settingScreen.setVisibility(View.VISIBLE);
                            DTimeUtil.run(100, () -> settingScreen.setVisibility(View.INVISIBLE));
                        }
                        mCameraView.cake(path -> {
                            CoreApp.appData.photoPath = path;
                            DBUtil.save(CoreApp.appData);
                            runUi(new RunUi() {
                                @Override
                                public void run() {
                                    ImageUtil.show(_dpActivity, new File(CoreApp.appData.photoPath), localPic);
                                }
                            });
                        });
                        break;
                }
            } else {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        takeTouch.setAlpha(0.5f);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        takeTouch.setAlpha(1.0f);
                        if (mCameraView.isRecord()) {
                            takeTouch.setImageResource(R.mipmap.ic_take_video);
                        } else {
                            takeTouch.setImageResource(R.mipmap.ic_take_video_pause);
                        }
                        mCameraView.starRecordVideo(kitInterface);
                        break;
                }
            }
            return true;
        });
        if (!CoreApp.appData.photoPath.equals("")) {
            ImageUtil.show(_dpActivity, new File(CoreApp.appData.photoPath), localPic);
        }
        bleTouch.setOnClickListener(v -> {
            if (BleUtil.getInstance().isConnected()) {
                BleUtil.getInstance().disconncetDeivce();
            } else {
                BleUtil.getInstance().disconncetDeivce();
                BluetoothDialogScreen.prepare(BluetoothDialogScreen.class).open(fragmentManager());
            }
        });
        cancelZhiBo.setOnClickListener(v -> {
            DpDialogScreen.create().setMsg(lang(R.string.quxiaokaiqizhibogensui))
                    .addButton(_dpActivity, lang(R.string.dp_ok), dialogScreen -> dialogScreen.closeEx(this::stopScreenRecording))
                    .addButton(_dpActivity, lang(R.string.dp_no), DialogScreen::closeEx).open(fragmentManager());
        });
        zhiBoTouch.setOnClickListener(v -> {
            if (!isStarted) {
                DpDialogScreen.create().setMsg(lang(R.string.quedingkaiqizhibogensui))
                        .addButton(_dpActivity, lang(R.string.dp_ok), dialogScreen -> dialogScreen.closeEx(this::startScreenRecording))
                        .addButton(_dpActivity, lang(R.string.dp_no), DialogScreen::closeEx).open(fragmentManager());
            } else {
                DpDialogScreen.create().setMsg(lang(R.string.quxiaokaiqizhibogensui))
                        .addButton(_dpActivity, lang(R.string.dp_ok), dialogScreen -> dialogScreen.closeEx(this::stopScreenRecording))
                        .addButton(_dpActivity, lang(R.string.dp_no), DialogScreen::closeEx).open(fragmentManager());
            }
        });
        switchCamera.setOnClickListener(v -> {
            mCameraView.switchCamera();
            ViewAnimator.animate(switchCamera).rotation(0, 180).onStop(() -> switchCamera.setRotation(0)).duration(500).start();

            flashBool = false;
            flashTouch.setImageResource(R.mipmap.ic_shan);
        });
        photoLin.setOnClickListener(v -> {
            if (mCameraView.isRecord()) {
                takeTouch.setImageResource(R.mipmap.ic_take_video);
                mCameraView.starRecordVideo(kitInterface);
            }
            open(PhotoScreen.class);
        });
        screenLock.setOnClickListener(v -> {
            screenLo = !screenLo;
            if (screenLo) {
                screenLock.setImageResource(R.mipmap.ic_lock_un);
                toast(lang(R.string.aiphoto_closed));
            } else {
                screenLock.setImageResource(R.mipmap.ic_lock);
                toast(lang(R.string.aiphoto_opened));
            }
        });
        menuTouch.setOnClickListener(v -> {
            mCameraView.onPause();
            settingScreen.setVisibility(View.VISIBLE);
            open(SettingScreen.class);
        });
        bleGattCallback = new BleGattCallback() {
            @Override
            public void onStartConnect() {

            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException e) {
                Lag.i("蓝牙连接失败");
                bleTouch.setImageResource(R.mipmap.ic_blue);
                BleUtil.getInstance().disconncetDeivce();
                toast(lang(R.string.shebeiyidkai));
                BleUtil.getInstance().startScan();
                JumpUtil.getInstance().stop();
                MainScreen.this.bleDevice = null;
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt bluetoothGatt, int i) {
                Lag.i("蓝牙连接成功");
                bleTouch.setImageResource(R.mipmap.ic_blue_connect);
                toast(lang(R.string.device_connect_success));
                JumpUtil.getInstance().start();
            }

            @Override
            public void onDisConnected(boolean b, BleDevice bleDevice, BluetoothGatt bluetoothGatt, int i) {
                Lag.i("蓝牙断开");
                bleTouch.setImageResource(R.mipmap.ic_blue);
                BleUtil.getInstance().disconncetDeivce();
                toast(lang(R.string.shebeiyidkai));
                BleUtil.getInstance().startScan();
                JumpUtil.getInstance().stop();
                MainScreen.this.bleDevice = null;
            }
        };
        BleUtil.getInstance().addDeviceConnectListener(bleGattCallback);

        blueScanCallback = (device, rssi, scanRecord) -> {
            Lag.i("扫描到设备, name: " + device.getName() + " ble:" + bleDevice);
            if (bleDevice != null) {
                return;
            }
            if (device.getName() != null && device.getName().equals("RX-Face")) {
                Lag.i("已扫描到设备");
                bleDevice = device;
                BleUtil.getInstance().stopScan();
                BleUtil.getInstance().connectDevice(bleDevice);
            }
        };
        BleUtil.getInstance().addDeviceScanListener(blueScanCallback);
        BleUtil.getInstance().startScan();

        flashTouch.setOnClickListener(v -> {
            flashBool = !flashBool;
            if (!mCameraView.isFrontCamera()) {
                mCameraView.flashOn(flashBool);
            }
            if (flashBool) {
                flashTouch.setImageResource(R.mipmap.ic_shan_on);
                toast(lang(R.string.shanguang_opened));
            } else {
                flashTouch.setImageResource(R.mipmap.ic_shan);
                toast(lang(R.string.shanguang_closed));
            }
        });

        photoTouch.setOnClickListener(v -> {
            noSwitch = 0;
            photoTouch.setTextColor(Color.parseColor("#ffff00"));
            videoTouch.setTextColor(Color.parseColor("#ffffff"));
            takeTouch.setImageResource(R.mipmap.ic_take);
            ViewAnimator.animate(centerTvLin).translationX(DisplayUtil.dip2px(_dpActivity, 27)).duration(200).start();
        });

        videoTouch.setOnClickListener(v -> {
            noSwitch = 1;
            photoTouch.setTextColor(Color.parseColor("#ffffff"));
            videoTouch.setTextColor(Color.parseColor("#ffff00"));
            takeTouch.setImageResource(R.mipmap.ic_take_video);
            ViewAnimator.animate(centerTvLin).translationX(-DisplayUtil.dip2px(_dpActivity, 27)).duration(200).start();
        });

        mCameraView.setCameraViewRecorderListener(new CameraView.CameraViewRecorderListener() {
            @Override
            public void start() {
                runUi(() -> {
                    switchCamera.setVisibility(View.GONE);
                    ViewAnimator.animate(timeLinTextLin).alpha(0, 1).duration(500).start();
                    takeTouch.setImageResource(R.mipmap.ic_take_video_pause);
                    timeLinText.setText(CountDownTimeTextUtil.getTimerString(0));
                });
            }

            @Override
            public void update(long time) {
                runUi(() -> {
                    timeLinText.setText(CountDownTimeTextUtil.getTimerString(time));
                });
            }

            @Override
            public void stop(long time) {
            }
        });
        if(CoreApp.appData.isFirst) {
            beginTouch.setVisibility(View.VISIBLE);
            beginImg.setVisibility(View.VISIBLE);
        } else {
            beginTouch.setVisibility(View.GONE);
            beginImg.setVisibility(View.GONE);
        }
        beginTouch.setOnClickListener(v -> {
            CoreApp.appData.isFirst = false;
            DBUtil.save(CoreApp.appData);
            beginTouch.setVisibility(View.GONE);
            beginImg.setVisibility(View.GONE);
        });
    }

    /**
     * FFmpeg转码监听
     */
    private FFmpegKit.KitInterface kitInterface = new FFmpegKit.KitInterface() {
        @Override
        public void onStart() {
            runUi(() -> {
                textToastDialogScreen = new TextToastDialogScreen();
                textToastDialogScreen.setConnectText(lang(R.string.screen_ro_success_z));
                textToastDialogScreen.open(fragmentManager());
            });
        }

        @Override
        public void onProgress(int progress) {

        }

        @Override
        public void onEnd(int result) {
            runUi(() ->
                    textToastDialogScreen.closeEx(() -> {
                        CoreApp.appData.photoPath = mCameraView.tempPathName;
                        DBUtil.save(CoreApp.appData);
                        switchCamera.setVisibility(View.VISIBLE);
                        ViewAnimator.animate(timeLinTextLin).alpha(1, 0).duration(500).start();
                        takeTouch.setImageResource(R.mipmap.ic_take_video);
                        toast(lang(R.string.screen_ro_success));
                        ImageUtil.show(_dpActivity, new File(CoreApp.appData.photoPath), localPic);
                    }));
        }
    };

    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;

    /**
     * 是否已经开启视频录制
     */
    private boolean isStarted = false;
    /**
     * 是否为标清视频
     */
    private boolean isVideoSd = true;
    /**
     * 是否开启音频录制
     */
    private boolean isAudio = true;

    /**
     * 获取屏幕录制的权限
     */
    private void startScreenRecording() {
        mScreenWidth = DisplayUtil.getMobileWidth(_dpActivity);
        mScreenHeight = DisplayUtil.getMobileHeight(_dpActivity);
        mScreenDensity = 1;
        // TODO Auto-generated method stub
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) _dpActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, 16895);
    }

    private ViewAnimator viewAnimator;

    /**
     * 监听后台服务
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 16895) {
            if (resultCode == RESULT_OK) {
                // 获得权限，启动Service开始录制
                Intent service = new Intent(_dpActivity, ScreenRecordService.class);
                service.putExtra("code", resultCode);
                service.putExtra("data", data);
                service.putExtra("audio", isAudio);
                service.putExtra("width", mScreenWidth);
                service.putExtra("height", mScreenHeight);
                service.putExtra("density", mScreenDensity);
                service.putExtra("quality", isVideoSd);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    _dpActivity.startForegroundService(service);
                } else {
                    _dpActivity.startService(service);
                }
                ScreenRecordService.listenerServiceInter = () -> {
                    runUi(() -> {
                        isStarted = !isStarted;
                        smallZhiLin.setVisibility(View.VISIBLE);
                        viewAnimator = ViewAnimator.animate(zhiBoTouch).alpha(1.0f, 0.2f, 1.0f).duration(1000).repeatCount(-1).start();
                        mCameraView.onPause();
                    });
//                    ScreenRecordService.screenRecordService.setBitmapListener(bitmap -> {
//                        Lag.i("屏幕刷新");
//                        runUi(() -> nowBitmap.setImageBitmap(bitmap));
//                    });
                    ScreenRecordService.screenRecordService.setBytesListener(this::refreshFaceBack);
                };
                //refreshOptimal(mScreenWidth,mScreenHeight,0, 0);
                // 已经开始屏幕录制，修改UI状态
                Lag.i("Started screen recording");
            } else {
                Lag.i("User cancelled");
            }
        }

    }

    /**
     * 关闭屏幕录制，即停止录制Service
     */
    private void stopScreenRecording() {
        // TODO Auto-generated method stub
        smallZhiLin.setVisibility(View.GONE);
        viewAnimator.cancel();
        zhiBoTouch.setAlpha(1.0f);
        Intent service = new Intent(_dpActivity, ScreenRecordService.class);
        _dpActivity.stopService(service);
        isStarted = !isStarted;
        mCameraView.onResume();
    }

    public void toast(String msg) {
        connectToastText.setText(msg);
        ViewAnimator.animate(connectToastLin).alpha(0, 1, 1, 1, 1, 0).duration(2000).start();
        //TextToastDialogScreen.prepare(TextToastDialogScreen.class, new TextBaseBean(msg)).open(fragmentManager());
    }

    private int nowRotateDegree = 0;

    private void roView(int rotateDegree) {
        if (nowRotateDegree == rotateDegree) {
            return;
        } else {
            nowRotateDegree = rotateDegree;
        }
        //toast(lang(R.string.screen_ro));
        int temp = 0;
        if (rotateDegree == 270) {
            temp = 90;
        } else if (rotateDegree == 90) {
            temp = 270;
        } else {
            temp = rotateDegree;
        }
        connectToastLin.setRotation(temp);
        ViewAnimator.animate(bleTouch, flashTouch, screenLock, zhiBoTouch, menuTouch, localPic).rotation(temp).duration(200).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleUtil.getInstance().disconncetDeivce();
        BleUtil.getInstance().stopScan();
        BleUtil.getInstance().removeDeviceConnectListener(bleGattCallback);
    }

    // 系统是否开启自动旋转
    protected boolean screenAutoRotate() {

        boolean autoRotate = false;
        try {
            autoRotate = 1 == Settings.System.getInt(_dpActivity.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        return autoRotate;
    }

    /**
     * 监听屏幕旋转
     */
    private void detectScreenRotate() {
        mOrientationListener = new OrientationEventListener(_dpActivity,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {

                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;  //手机平放时，检测不到有效的角度
                }

                //可以根据不同角度检测处理，这里只检测四个角度的改变
                orientation = (orientation + 45) / 90 * 90;

                if (screenAutoRotate() && orientation % 360 == 180) {
                    return;
                }

                rotateDegree = orientation % 360;

                Lag.i("旋转角度:" + rotateDegree);

                roView(rotateDegree);
            }
        };

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        } else {
            mOrientationListener.disable();
        }
    }

    /**
     * 初始化AI引擎
     */
    private void createKitInstance() {
        FaceDetector.FaceDetectorCreateConfig createConfig = new FaceDetector.FaceDetectorCreateConfig();
        createConfig.mode = FaceDetector.FaceDetectMode.MOBILE_DETECT_MODE_VIDEO;
        FaceDetector.createInstanceAsync(getContext(), createConfig, new InstanceCreatedListener<FaceDetector>() {
            @Override
            public void onSucceeded(FaceDetector faceDetector) {
                mFaceDetector = faceDetector;
            }

            @Override
            public void onFailed(int i, Error error) {
                Lag.e("create face detetector failed: " + error);
            }
        });
    }

    private NV21ToBitmap nv21ToBitmap;

    private void runInit() {

        nv21ToBitmap = new NV21ToBitmap(_dpActivity);

        // point view
        SurfaceView drawView = superView.findViewById(R.id.points_view);
        drawView.setZOrderOnTop(false);
        drawView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mDrawSurfaceHolder = drawView.getHolder();

        mCameraView = superView.findViewById(R.id.camera_view);

        mCameraView.setPreviewCallback(new CameraView.PreviewCallback() {
            @Override
            public void onGetPreviewOptimalSize(int optimalWidth, int optimalHeight, int cameraOrientation, int deviecAutoRotateAngle) {
                refreshOptimal(optimalWidth, optimalHeight, cameraOrientation, deviecAutoRotateAngle);
            }

            @Override
            public void onPreviewFrame(byte[] data, int width, int height, int cameraOrientation) {
                //Bitmap bitmap = nv21ToBitmap.nv21ToBitmap(data, width, height);
                //picImg.setImageBitmap(bitmap);
                refreshFace(data, width, height, cameraOrientation);
            }

        });
    }

    private void refreshOptimal(int optimalWidth, int optimalHeight, int cameraOrientation, int deviecAutoRotateAngle) {

        // w为图像短边，h为长边
        int w = optimalWidth;
        int h = optimalHeight;
        if (cameraOrientation == 90 || cameraOrientation == 270) {
            w = optimalHeight;
            h = optimalWidth;
        }

        // 屏幕长宽
        DisplayMetrics metric = new DisplayMetrics();
        _dpActivity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        int screenW = metric.widthPixels;
        int screenH = metric.heightPixels;

        int contentTop = _dpActivity.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
        Rect frame = new Rect();
        _dpActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;

        ConstraintLayout layoutVideo = superView.findViewById(R.id.videoLayout);
        FrameLayout frameLayout = layoutVideo.findViewById(R.id.videoContentLayout);
        if (deviecAutoRotateAngle == 0 || deviecAutoRotateAngle == 180) {

            int fixedScreenH = screenW * h / w;// 宽度不变，等比缩放的高度

            ViewGroup.LayoutParams params = frameLayout.getLayoutParams();
            params.height = fixedScreenH;
            frameLayout.setLayoutParams(params);

            mActualPreviewWidth = screenW;
            mActualPreviewHeight = fixedScreenH;
        } else {

            int previewHeight = screenH - contentTop - statusBarHeight;
            int fixedScreenW = previewHeight * h / w;// 高度不变，等比缩放的宽

            ViewGroup.LayoutParams params = frameLayout.getLayoutParams();
            params.width = fixedScreenW;
            frameLayout.setLayoutParams(params);

            mActualPreviewWidth = fixedScreenW;
            mActualPreviewHeight = previewHeight;
        }
    }

    private void refreshFace(byte[] data, int width, int height, int cameraOrientation) {
        if (mFaceDetector == null) {
            return;
        }

        // 输入角度
        int inAngle = mCameraView.isFrontCamera() ? (cameraOrientation + 360 - rotateDegree) % 360 : (cameraOrientation + rotateDegree) % 360;
        // 输出角度
        int outAngle = 0;

        if (!screenAutoRotate()) {
            outAngle = mCameraView.isFrontCamera() ? (360 - rotateDegree) % 360 : rotateDegree % 360;
        }

        long start = System.currentTimeMillis();
        long detectConfig = FaceDetectConfig.ACTIONTYPE_EYE_BLINK | FaceDetectConfig.ACTIONTYPE_MOUTH_AH | FaceDetectConfig.ACTIONTYPE_HEAD_YAW | FaceDetectConfig.ACTIONTYPE_HEAD_PITCH | FaceDetectConfig.ACTIONTYPE_BROW_JUMP;
        FaceDetectionReport[] results = mFaceDetector.inference(data, width, height, MNNCVImageFormat.YUV_NV21, detectConfig, inAngle, outAngle, mCameraView.isFrontCamera() ? MNNFlipType.FLIP_Y : MNNFlipType.FLIP_NONE);

        String timeCostText = "0 ms";
        String yprText = "";
        String faceActionText = "";

        int faceCount = 0;
        if (results != null && results.length > 0) {
            faceCount = results.length;

            // time cost
            timeCostText = (System.currentTimeMillis() - start) + "ms";
            // ypr
            FaceDetectionReport firstReport = results[0];
            yprText = "yaw: " + firstReport.yaw + "\npitch: " + firstReport.pitch + "\nroll: " + firstReport.roll + "\n";

            for (int i = 0; i < results.length && i < MAX_RESULT; i++) {
                // key points
                System.arraycopy(results[i].keyPoints, 0, keypts, i * 106 * 2, 106 * 2);
                // face rect
                rects[i * 4] = results[i].rect.left;
                rects[i * 4 + 1] = results[i].rect.top;
                rects[i * 4 + 2] = results[i].rect.right;
                rects[i * 4 + 3] = results[i].rect.bottom;
                // score
                scores[i] = results[i].score;
            }

            faceActionText = faceActionDesc(firstReport.faceActionMap);

        }

        //Lag.i("照相机 延迟:" + timeCostText + " 信息:" + yprText + " 动作:" + faceActionText);

        DrawResult(scores, rects, keypts, faceCount, cameraOrientation, rotateDegree);
    }

    private void refreshFaceBack(byte[] data, int width, int height, int cameraOrientation) {
        if (mFaceDetector == null) {
            return;
        }

        // 输入角度
        int inAngle = 0;
        // 输出角度
        int outAngle = 0;

        long start = System.currentTimeMillis();
        long detectConfig = FaceDetectConfig.ACTIONTYPE_EYE_BLINK | FaceDetectConfig.ACTIONTYPE_MOUTH_AH | FaceDetectConfig.ACTIONTYPE_HEAD_YAW | FaceDetectConfig.ACTIONTYPE_HEAD_PITCH | FaceDetectConfig.ACTIONTYPE_BROW_JUMP;
        FaceDetectionReport[] results = mFaceDetector.inference(data, width, height, MNNCVImageFormat.RGBA, detectConfig, inAngle, outAngle, MNNFlipType.FLIP_NONE);

        String timeCostText = "0 ms";
        String yprText = "";
        String faceActionText = "";

        int faceCount = 0;
        if (results != null && results.length > 0) {

            Lag.i("识别的数量:" + results.length);

            faceCount = results.length;

            // time cost
            timeCostText = (System.currentTimeMillis() - start) + "ms";
            // ypr
            FaceDetectionReport firstReport = results[0];
            yprText = "yaw: " + firstReport.yaw + "\npitch: " + firstReport.pitch + "\nroll: " + firstReport.roll + "\n";

            for (int i = 0; i < results.length && i < MAX_RESULT; i++) {
                // key points
                System.arraycopy(results[i].keyPoints, 0, keypts2, i * 106 * 2, 106 * 2);
                // face rect
                rects2[i * 4] = results[i].rect.left;
                rects2[i * 4 + 1] = results[i].rect.top;
                rects2[i * 4 + 2] = results[i].rect.right;
                rects2[i * 4 + 3] = results[i].rect.bottom;
                // score
                scores2[i] = results[i].score;
            }

            faceActionText = faceActionDesc(firstReport.faceActionMap);

        }

        //Lag.i("录屏 延迟:" + timeCostText + " 信息:" + yprText + " 动作:" + faceActionText);

        notResult(rects2, keypts2, faceCount);
    }

    private String faceActionDesc(Map<String, Boolean> faceActionMap) {

        StringBuilder desc = new StringBuilder();
        if (faceActionMap.size() == 0) {
            return desc.toString();
        }

        List<String> actions = new ArrayList<>();

        for (Map.Entry<String, Boolean> entry : faceActionMap.entrySet()) {
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());

            Boolean bActing = entry.getValue();
            if (!bActing) continue;

            if (entry.getKey().equals("HeadYaw")) {
                actions.add("摇头");
            }
            if (entry.getKey().equals("BrowJump")) {
                actions.add("眉毛挑动");
            }
            if (entry.getKey().equals("EyeBlink")) {
                actions.add("眨眼");
            }
            if (entry.getKey().equals("MouthAh")) {
                actions.add("嘴巴大张");
            }
            if (entry.getKey().equals("HeadPitch")) {
                actions.add("点头");
            }
        }

        for (int i = 0; i < actions.size(); i++) {
            String action = actions.get(i);
            if (i > 0) {
                desc.append("、").append(action);
                continue;
            }
            desc = new StringBuilder(action);
        }

        return desc.toString();
    }

    private void DrawResult(float[] scores, float[] rects, float[] facePoints,
                            int faceCount, int cameraOrientation, int rotateDegree) {
        Canvas canvas = null;
        try {
            canvas = mDrawSurfaceHolder.lockCanvas();
            if (canvas == null) {
                return;
            }
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            float kx = 0.0f, ky = 0.0f;

            // 这里只写了摄像头正向为90/270度的一般情况，如果有其他情况，自行枚举
            if (90 == cameraOrientation || 270 == cameraOrientation) {

                if (!screenAutoRotate()) {
                    kx = ((float) mActualPreviewWidth) / mCameraView.getPreviewSize().height;
                    ky = (float) mActualPreviewHeight / mCameraView.getPreviewSize().width;
                } else {

                    if ((0 == rotateDegree) || (180 == rotateDegree)) {// 屏幕竖直方向翻转
                        kx = ((float) mActualPreviewWidth) / mCameraView.getPreviewSize().height;
                        ky = ((float) mActualPreviewHeight) / mCameraView.getPreviewSize().width;
                    } else if (90 == rotateDegree || 270 == rotateDegree) {// 屏幕水平方向翻转
                        kx = ((float) mActualPreviewWidth) / mCameraView.getPreviewSize().width;
                        ky = ((float) mActualPreviewHeight) / mCameraView.getPreviewSize().height;
                    }
                }
//                if (!screenAutoRotate()) {
//                    kx = ((float) mActualPreviewWidth) / mCameraView.getPreviewSize().getHeight();
//                    ky = (float) mActualPreviewHeight / mCameraView.getPreviewSize().getWidth();
//                } else {
//
//                    if ((0 == rotateDegree) || (180 == rotateDegree)) {// 屏幕竖直方向翻转
//                        kx = ((float) mActualPreviewWidth) / mCameraView.getPreviewSize().getHeight();
//                        ky = ((float) mActualPreviewHeight) / mCameraView.getPreviewSize().getWidth();
//                    } else if (90 == rotateDegree || 270 == rotateDegree) {// 屏幕水平方向翻转
//                        kx = ((float) mActualPreviewWidth) / mCameraView.getPreviewSize().getWidth();
//                        ky = ((float) mActualPreviewHeight) / mCameraView.getPreviewSize().getHeight();
//                    }
//                }
            } else {
                kx = 1;
                ky = 1;
            }

            if (screenLo) {
                return;
            }
            // 绘制人脸关键点
            for (int i = 0; i < faceCount; i++) {
                for (int j = 0; j < 106; j++) {
                    float keyX = facePoints[i * 106 * 2 + j * 2];
                    float keyY = facePoints[i * 106 * 2 + j * 2 + 1];
                    canvas.drawCircle(keyX * kx, keyY * ky, 4.0f, KeyPointsPaint);
//                    if (true) {
//                        canvas.drawText(j + "", keyX * kx, keyY * ky, PointOrderPaint); //标注106点的索引位置
//                    }
                }

                float left = rects[0];
                float top = rects[1];
                float right = rects[2];
                float bottom = rects[3];
                canvas.drawLine(left * kx, top * ky,
                        right * kx, top * ky, KeyLinesPaint);
                canvas.drawLine(right * kx, top * ky,
                        right * kx, bottom * ky, KeyLinesPaint);
                canvas.drawLine(right * kx, bottom * ky,
                        left * kx, bottom * ky, KeyLinesPaint);
                canvas.drawLine(left * kx, bottom * ky,
                        left * kx, top * ky, KeyLinesPaint);

                if (timeTurnLong < System.currentTimeMillis() - 50) {
                    point43 = new Rect((int) (left), 0, (int) (right), 0);
                    Lag.i("人脸 1:" + point43.left + " 31:" + point43.right);
                    timeTurnLong = System.currentTimeMillis();
                    sendWrite();
                }
                Lag.i("left x:" + left * kx + " y:" + top * ky);
                //canvas.drawText(scores[i] + "", left * kx, top * ky - 10, ScorePaint);
            }

        } catch (Throwable t) {
            Lag.e("Draw result error: " + t);
        } finally {
            if (canvas != null) {
                mDrawSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void notResult(float[] rects, float[] facePoints, int faceCount) {

        float kx = 1.0f, ky = 1.0f;

        // 绘制人脸关键点
        for (int i = 0; i < faceCount; i++) {
            if (timeTurnLong < System.currentTimeMillis() - 50) {
                float left = rects[0];
                float right = rects[2];
                point43 = new Rect((int) (left), 0, (int) (right), 0);
                Lag.i("人脸 1:" + point43.left + " 31:" + point43.right);
                timeTurnLong = System.currentTimeMillis();
                sendWrite();
            }
        }
    }

    private void sendWrite() {
        byte[] data = new byte[1];
        // 屏幕大小
        int dw = screenWidth;
        // 头大小
        int tw = point43.right - point43.left;
        // 计算的规定 x 位置
        int jl = DisplayUtil.dip2px(_dpActivity, 40);
        int tlx = dw / 2 - tw / 2 - jl;
        int tlr = dw / 2 + tw / 2 + jl;
        Lag.i("人脸 左边最左位置:" + tlx + " 当前位置:" + point43.left);
        Lag.i("人脸 左边最右位置:" + tlr + " 当前位置:" + point43.right);
        if (point43.left < tlx) {
            data[0] = 1;
        } else if (point43.right > tlr) {
            data[0] = 2;
        }
        BleUtil.getInstance().write(data);
    }

    @Override
    public void showScreen() {
        super.showScreen();
        ScreenUtil.switchFullScreen(_dpActivity, true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isStarted) {
            mCameraView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!isStarted) {
            mCameraView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mFaceDetector != null) {
            mFaceDetector.release();
        }
    }

    @Override
    public void onBack() {
        DpDialogScreen.create().setMsg(getString(R.string.quedingtuichuyingyongchengxu)).addButton(getContext(), getString(R.string.ok), R.color.dialogColor,
                dialogScreen -> dialogScreen.close(() -> _dpActivity.finish()))
                .addButton(getContext(), getString(R.string.no), R.color.dialogColor, DialogScreen::close).open(getFragmentManager());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SwitchLangEvent event) {
        setLang(this, event.lang);
        _dpActivity.recreate();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AddDeviceEvent event) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UpdateViewEvent event) {
        ImageUtil.show(_dpActivity, new File(CoreApp.appData.photoPath), localPic);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(OkPermissionEvent event) {
        runInit();
        mCameraView.onResume();

        if (CoreApp.appData.photoPath.equals("")) {
            List<File> strings = FileUtil.GetFileName(FileUtil.getSDPath() + "/DCIM/Camera/");
            if (strings.size() > 0) {
                ImageUtil.show(_dpActivity, strings.get(strings.size() - 1), localPic);
                CoreApp.appData.photoPath = strings.get(strings.size() - 1).getAbsolutePath();
                DBUtil.save(CoreApp.appData);
            }
        } else {
            ImageUtil.show(_dpActivity, new File(CoreApp.appData.photoPath), localPic);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(HideBlackAnimEvent event) {
        mCameraView.onResume();
        DTimeUtil.run(500, () -> settingScreen.setVisibility(View.INVISIBLE));
    }
}
