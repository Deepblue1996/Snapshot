package com.deep.snapshot.listener;

import com.clj.fastble.data.BleDevice;

/**
 * Class -
 * <p>
 * Created by Deepblue on 2019/5/20 0020.
 */

public interface BlueScanCallback {

    void onLeScan(BleDevice device, int rssi, byte[] scanRecord);
}
