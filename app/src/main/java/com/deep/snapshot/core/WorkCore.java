package com.deep.snapshot.core;

import android.Manifest;
import android.view.WindowManager;

import com.deep.dpwork.DpWorkCore;
import com.deep.dpwork.annotation.DpBluetooth;
import com.deep.dpwork.annotation.DpPermission;
import com.deep.dpwork.util.Lag;
import com.deep.dpwork.util.ToastUtil;
import com.deep.snapshot.R;
import com.deep.snapshot.event.OkPermissionEvent;
import com.intelligence.blue.Bun;
import com.intelligence.bluedata.BlueSettings;

import org.greenrobot.eventbus.EventBus;

/**
 * Class - 主活动类
 * <p>
 * Created by Deepblue on 2019/9/29 0029.
 */
@DpPermission({
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.INTERNET,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE
})
@DpBluetooth
public class WorkCore extends DpWorkCore {

    @Override
    protected void initCore() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void initBluetooth(boolean have) {
        if (have) {
            startInitBluetooth();
        } else {
            finish();
        }
    }

    private void startInitBluetooth() {

        // ---------------- 初始化配置 ------------------

        startScan();

        Bun.get().setBluetoothListener(open -> {
            String string;
            if (open) {
                string = "蓝牙已打开";
                ToastUtil.showSuccess(string);
                startScan();
            } else {
                string = "蓝牙已关闭";
                ToastUtil.showError(string);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Bun.get().stopDataScan();
            Bun.get().stopDataSend();
            Bun.get().onDestroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startScan() {

        /**
         * 初始化配置
         */
        BlueSettings blueSettings = new BlueSettings.Builder()
                .setActivity(this)
                .setBroadcastDuration(500)
                .build();

        /**
         * 蓝牙初始化
         */
        Bun.init(blueSettings);

    }

    @Override
    protected void permissionComplete(boolean b) {
        if (!b) {
            ToastUtil.showError(getString(R.string.bixudakaiquanxian));
        } else {
            EventBus.getDefault().post(new OkPermissionEvent());
        }
    }

}
