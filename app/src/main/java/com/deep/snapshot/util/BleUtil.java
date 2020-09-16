package com.deep.snapshot.util;

import android.app.Application;
import android.bluetooth.BluetoothGatt;
import android.util.Log;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.deep.dpwork.util.Lag;
import com.deep.snapshot.listener.BlueScanCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * BLE工具类
 */
public class BleUtil {

    public final static int FPS_YAN = 30;

    // 服务UUID
    private final static String uuidService = "0000180f-0000-1000-8000-00805f9b34fb";
    // 写入UUID
    private final static String uuidWriteCha = "00002a19-0000-1000-8000-00805f9b34fb";
    // 读取UUID
    private final static String uuidReadCha = "00002a19-0000-1000-8000-00805f9b34fb";
    // 订阅UUID
    private final static String uuidNotifyCha = "00002a19-0000-1000-8000-00805f9b34fb";

    private List<BlueScanCallback> btDeviceListenList = new ArrayList<>();
    private List<BleGattCallback> bleDeviceConnectListenList = new ArrayList<>();
    private List<BleReadCallback> bleDeviceReadListenList = new ArrayList<>();
    private List<BleWriteCallback> bleDeviceWriteListenList = new ArrayList<>();
    private List<BleNotifyCallback> bleDeviceNotiftListenList = new ArrayList<>();
    private List<BleIndicateCallback> bleIndicateListenList = new ArrayList<>();

    private volatile static BleUtil bleUtil;

    // 当前连接设备
    public BleDevice bleDevice;

    /**
     * 单例
     *
     * @return
     */
    public synchronized static BleUtil getInstance() {
        if (bleUtil == null) {
            bleUtil = new BleUtil();
        }
        return bleUtil;
    }

    /**
     * 初始化连接
     *
     * @param application 应用程序上下全文
     */
    public void init(Application application) {

        BleManager.getInstance().init(application);

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setScanTimeOut(10000)
                .setAutoConnect(false)
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
        BleManager.getInstance().setConnectOverTime(2000);
    }

    private BleScanCallback bleDeviceBleScanCallback = new BleScanCallback() {
        @Override
        public void onScanStarted(boolean b) {

        }

        @Override
        public void onScanning(BleDevice device) {
            Log.i("扫描到设备", "" + device.getMac());
            if (btDeviceListenList.size() == 0) {
                return;
            }
            for (int j = 0; j < btDeviceListenList.size(); j++) {
                btDeviceListenList.get(j).onLeScan(device, device.getRssi(), device.getScanRecord());
            }
        }

        @Override
        public void onScanFinished(List<BleDevice> list) {

        }

    };

    private BleGattCallback bleDeviceConnectListen = new BleGattCallback() {
        @Override
        public void onStartConnect() {
            for (int i = 0; i < bleDeviceConnectListenList.size(); i++) {
                bleDeviceConnectListenList.get(i).onStartConnect();
            }
        }

        @Override
        public void onConnectFail(BleDevice bleDevice, BleException e) {
            for (int i = 0; i < bleDeviceConnectListenList.size(); i++) {
                bleDeviceConnectListenList.get(i).onConnectFail(bleDevice, e);
            }
            bleUtil.bleDevice = null;
        }

        @Override
        public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt bluetoothGatt, int j) {
            for (int i = 0; i < bleDeviceConnectListenList.size(); i++) {
                bleDeviceConnectListenList.get(i).onConnectSuccess(bleDevice, bluetoothGatt, j);
            }
            bleUtil.bleDevice = bleDevice;
            Lag.i("BLE 连接成功");
//            if (isConnected()) {
//                Lag.i("BLE 判断已连接，开始订阅");
//                notifyListener();
//            }
        }

        @Override
        public void onDisConnected(boolean b, BleDevice bleDevice, BluetoothGatt bluetoothGatt, int j) {
            for (int i = 0; i < bleDeviceConnectListenList.size(); i++) {
                bleDeviceConnectListenList.get(i).onDisConnected(b, bleDevice, bluetoothGatt, j);
            }
            bleUtil.bleDevice = null;
        }
    };

    private BleReadCallback bleDeviceReadListen = new BleReadCallback() {

        @Override
        public void onReadSuccess(byte[] bytes) {
            for (int i = 0; i < bleDeviceReadListenList.size(); i++) {
                bleDeviceReadListenList.get(i).onReadSuccess(bytes);
            }
        }

        @Override
        public void onReadFailure(BleException e) {
            for (int i = 0; i < bleDeviceReadListenList.size(); i++) {
                bleDeviceReadListenList.get(i).onReadFailure(e);
            }
        }
    };

    private volatile boolean isWriteing = false;

    private int writeCountNum = 0;
    private byte[] writeDataTemp;

    private BleWriteCallback bleDeviceWriteListen = new BleWriteCallback() {

        @Override
        public void onWriteSuccess(int j, int i1, byte[] bytes) {
            for (int i = 0; i < bleDeviceWriteListenList.size(); i++) {
                bleDeviceWriteListenList.get(i).onWriteSuccess(j, i1, bytes);
            }
            Lag.i("BLE 蓝牙写入成功");
            writeCountNum = 0;
            isWriteing = false;
        }

        @Override
        public void onWriteFailure(BleException e) {
            for (int i = 0; i < bleDeviceWriteListenList.size(); i++) {
                bleDeviceWriteListenList.get(i).onWriteFailure(e);
            }
            Lag.i("BLE 蓝牙写入失败e:" + e.getDescription() + " code:" + e.getCode());
//            if (writeCountNum < 3) {
//                if (writeDataTemp != null) {
//                    write(writeDataTemp);
//                }
//                Lag.i("蓝牙写入重试:" + writeCountNum);
//                writeCountNum++;
//            }
            isWriteing = false;
        }
    };

    private BleNotifyCallback bleDeviceBleNotiftCallback = new BleNotifyCallback() {

        @Override
        public void onNotifySuccess() {
            for (int i = 0; i < bleDeviceNotiftListenList.size(); i++) {
                bleDeviceNotiftListenList.get(i).onNotifySuccess();
            }
            Lag.i("BLE 订阅成功");
        }

        @Override
        public void onNotifyFailure(BleException e) {

            for (int i = 0; i < bleDeviceNotiftListenList.size(); i++) {
                bleDeviceNotiftListenList.get(i).onNotifyFailure(e);
            }
            Lag.i("BLE 订阅失败");
        }

        @Override
        public void onCharacteristicChanged(byte[] bytes) {
            for (int i = 0; i < bleDeviceNotiftListenList.size(); i++) {
                bleDeviceNotiftListenList.get(i).onCharacteristicChanged(bytes);
            }
            Lag.i("BLE 数据更新:" + toHexString(bytes));
        }

    };

    private BleIndicateCallback bleDeviceBleIndicateCallback = new BleIndicateCallback() {

        @Override
        public void onIndicateSuccess() {
            for (int i = 0; i < bleDeviceNotiftListenList.size(); i++) {
                bleIndicateListenList.get(i).onIndicateSuccess();
            }
        }

        @Override
        public void onIndicateFailure(BleException e) {
            for (int i = 0; i < bleDeviceNotiftListenList.size(); i++) {
                bleIndicateListenList.get(i).onIndicateFailure(e);
            }
        }

        @Override
        public void onCharacteristicChanged(byte[] bytes) {
            for (int i = 0; i < bleDeviceNotiftListenList.size(); i++) {
                bleIndicateListenList.get(i).onCharacteristicChanged(bytes);
            }
        }

    };

    public void startScan() {
        BleManager.getInstance().scan(bleDeviceBleScanCallback);
    }

    public void stopScan() {
        try {
            if (BleManager.getInstance().getScanSate() == BleScanState.STATE_SCANNING) {
                BleManager.getInstance().cancelScan();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        if (bleDevice != null) {
            return BleManager.getInstance().isConnected(bleDevice);
        }
        return false;
    }

    public void addDeviceScanListener(BlueScanCallback bleScanCallback) {
        for (int i = 0; i < btDeviceListenList.size(); i++) {
            if (btDeviceListenList.get(i) == bleScanCallback) {
                btDeviceListenList.set(i, bleScanCallback);
                return;
            }
        }
        btDeviceListenList.add(bleScanCallback);
    }

    public void removeDeviceScanListener(BlueScanCallback bleScanCallback) {
        btDeviceListenList.remove(bleScanCallback);
    }

    public void addDeviceConnectListener(BleGattCallback bleConnectCallback) {
        for (int i = 0; i < bleDeviceConnectListenList.size(); i++) {
            if (bleDeviceConnectListenList.get(i) == bleConnectCallback) {
                bleDeviceConnectListenList.set(i, bleConnectCallback);
                return;
            }
        }
        bleDeviceConnectListenList.add(bleConnectCallback);
    }

    public void removeDeviceConnectListener(BleGattCallback bleConnectCallback) {
        bleDeviceConnectListenList.remove(bleConnectCallback);
    }


    public void addDeviceReadListener(BleReadCallback bleReadCallback) {
        for (int i = 0; i < bleDeviceReadListenList.size(); i++) {
            if (bleDeviceReadListenList.get(i) == bleReadCallback) {
                bleDeviceReadListenList.set(i, bleReadCallback);
                return;
            }
        }
        bleDeviceReadListenList.add(bleReadCallback);
    }

    public void removeDeviceReadListener(BleReadCallback bleReadCallback) {
        bleDeviceReadListenList.remove(bleReadCallback);
    }


    public void addDeviceWriteListener(BleWriteCallback bleWriteCallback) {
        for (int i = 0; i < bleDeviceWriteListenList.size(); i++) {
            if (bleDeviceWriteListenList.get(i) == bleWriteCallback) {
                bleDeviceWriteListenList.set(i, bleWriteCallback);
                return;
            }
        }
        bleDeviceWriteListenList.add(bleWriteCallback);
    }

    public void removeDeviceWriteListener(BleWriteCallback bleWriteCallback) {
        bleDeviceWriteListenList.remove(bleWriteCallback);
    }

    public void addBleDeviceBleNotiftCallback(BleNotifyCallback bleDeviceBleNotiftCallback) {
        for (int i = 0; i < bleDeviceNotiftListenList.size(); i++) {
            if (bleDeviceNotiftListenList.get(i) == bleDeviceBleNotiftCallback) {
                bleDeviceNotiftListenList.set(i, bleDeviceBleNotiftCallback);
                return;
            }
        }
        bleDeviceNotiftListenList.add(bleDeviceBleNotiftCallback);
    }

    public void removeBleDeviceBleNotiftCallback(BleNotifyCallback bleNotiftCallback) {
        bleDeviceNotiftListenList.remove(bleNotiftCallback);
    }

    public void addBleDeviceBleIndicateCallback(BleIndicateCallback bleIndicateCallback) {
        for (int i = 0; i < bleIndicateListenList.size(); i++) {
            if (bleIndicateListenList.get(i) == bleIndicateCallback) {
                bleIndicateListenList.set(i, bleIndicateCallback);
                return;
            }
        }
        bleIndicateListenList.add(bleIndicateCallback);
    }

    public void removeBleDeviceBleIndicateCallback(BleIndicateCallback bleIndicateCallback) {
        bleIndicateListenList.remove(bleIndicateCallback);
    }

    /**
     * 字节数组转成16进制表示格式的字符串
     *
     * @param byteArray 需要转换的字节数组
     * @return 16进制表示格式的字符串
     **/
    private String toHexString(byte[] byteArray) {
        if (byteArray == null || byteArray.length < 1)
            throw new IllegalArgumentException("this byteArray must not be null or empty");

        final StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < byteArray.length; i++) {
            if ((byteArray[i] & 0xff) < 0x10)//0~F前面不零
                hexString.append("0");

            hexString.append(Integer.toHexString(0xFF & byteArray[i]));
        }
        return hexString.toString().toLowerCase();
    }

    public void write(byte[] data) {
        Lag.i("写入数据2:" + toHexString(data));
        if (bleDevice != null && !isWriteing) {
            writeDataTemp = data;
//            ThreadPoolManager.newInstance().addExecuteTask(new Runnable() {
//                @Override
//                public void run() {
            isWriteing = true;
            BleManager.getInstance().write(bleDevice, uuidService, uuidWriteCha, data, bleDeviceWriteListen);
//                }
//            });
        } else {
            Lag.i("BLE 写入正忙，过滤处理");
        }
    }

    public void read() {
        if (bleDevice != null) {
            BleManager.getInstance().read(bleDevice, uuidService, uuidReadCha, bleDeviceReadListen);
        }
    }

    public void notifyListener() {
        if (bleDevice != null) {
            BleManager.getInstance().notify(bleDevice, uuidService, uuidNotifyCha, bleDeviceBleNotiftCallback);
        }
    }

    public void connectDevice(BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, bleDeviceConnectListen);
    }

    public void disconncetDeivce() {
        if (bleDevice != null) {
            BleManager.getInstance().disconnect(bleDevice);
        }
        BleManager.getInstance().disconnectAllDevice();
    }

    public void released() {
        BleManager.getInstance().disconnectAllDevice();

        BleManager.getInstance().destroy();
    }

}
