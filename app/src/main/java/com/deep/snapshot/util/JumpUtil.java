package com.deep.snapshot.util;

import com.deep.dpwork.util.Lag;

import java.util.Timer;
import java.util.TimerTask;

public class JumpUtil {

    private Timer timer;

    private volatile static JumpUtil jumpUtil;

    /**
     * 单例
     *
     * @return
     */
    public synchronized static JumpUtil getInstance() {
        if (jumpUtil == null) {
            jumpUtil = new JumpUtil();
        }
        return jumpUtil;
    }

    public void start() {
        Lag.i("开始心跳");
        stop();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Lag.i("心跳中");
                BleUtil.getInstance().write(new byte[5]);
            }
        }, 0, 1000);
    }

    public void stop() {
        Lag.i("停止心跳");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
