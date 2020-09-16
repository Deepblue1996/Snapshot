package com.deep.snapshot.view.dialog;

import android.bluetooth.BluetoothGatt;
import android.widget.TextView;

import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.deep.dpwork.annotation.DpLayout;
import com.deep.dpwork.annotation.DpNullAnim;
import com.deep.dpwork.util.DTimeUtil;
import com.deep.dpwork.util.Lag;
import com.deep.snapshot.R;
import com.deep.snapshot.base.TDialogScreen;
import com.deep.snapshot.listener.BlueScanCallback;
import com.deep.snapshot.util.BleUtil;

import butterknife.BindView;

@DpNullAnim
@DpLayout(R.layout.blue_loading_layout)
public class BluetoothDialogScreen extends TDialogScreen implements BlueScanCallback {

    private final static String deviceName = "RX-Face";
    private final static int connectSo = 3;

    @BindView(R.id.connectText)
    public TextView connectText;
    private BleDevice bleDevice;

    private BleGattCallback bleGattCallback;

    private int connectCount = 0;

    @Override
    public void init() {

        bleGattCallback = new BleGattCallback() {
            @Override
            public void onStartConnect() {

            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException e) {
                Lag.e(e.getDescription());
                DTimeUtil.stop();
                BleUtil.getInstance().disconncetDeivce();
                if (connectCount < connectSo) {
                    DTimeUtil.run(500, () -> {
                        connect();
                        connectCount++;
                    });
                } else {
                    connectText.setText(lang(R.string.device_connect_faile));
                    DTimeUtil.run(2000, BluetoothDialogScreen.this::closeEx);
                }
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt bluetoothGatt, int i) {
                DTimeUtil.stop();
                connectText.setText(lang(R.string.device_connect_success));
                DTimeUtil.run(2000, BluetoothDialogScreen.this::closeEx);
            }

            @Override
            public void onDisConnected(boolean b, BleDevice bleDevice, BluetoothGatt bluetoothGatt, int i) {
                DTimeUtil.stop();
                BleUtil.getInstance().disconncetDeivce();
                if (connectCount < connectSo) {
                    DTimeUtil.run(500, () -> {
                        connect();
                        connectCount++;
                    });
                } else {
                    connectText.setText(lang(R.string.device_disconnect));
                    DTimeUtil.run(2000, BluetoothDialogScreen.this::closeEx);
                }
            }
        };

        BleUtil.getInstance().addDeviceScanListener(this);
        BleUtil.getInstance().addDeviceConnectListener(bleGattCallback);
        BleUtil.getInstance().startScan();

        DTimeUtil.run(3000, () -> {
            connectText.setText(lang(R.string.device_connect_time));
            DTimeUtil.run(2000, BluetoothDialogScreen.this::closeEx);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BleUtil.getInstance().stopScan();
        BleUtil.getInstance().removeDeviceScanListener(this);
        BleUtil.getInstance().removeDeviceConnectListener(bleGattCallback);
    }

    @Override
    public void onLeScan(BleDevice device, int rssi, byte[] scanRecord) {
        if (device.getName() != null && device.getName().equals(deviceName) && bleDevice == null) {
            BleUtil.getInstance().stopScan();
            bleDevice = device;
            connect();
        }
    }

    private void connect() {
        BleUtil.getInstance().connectDevice(bleDevice);
    }

}
