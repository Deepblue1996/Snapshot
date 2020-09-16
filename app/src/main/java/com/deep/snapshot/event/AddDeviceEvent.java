package com.deep.snapshot.event;

/**
 * Class -
 * <p>
 * Created by Deepblue on 2019/7/13 0013.
 */

public class AddDeviceEvent {
    public String mac;
    public String s;

    public AddDeviceEvent(String mac, String s) {
        this.mac = mac;
        this.s = s;
    }
}
