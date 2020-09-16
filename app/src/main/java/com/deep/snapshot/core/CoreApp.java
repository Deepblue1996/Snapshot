package com.deep.snapshot.core;

import com.deep.dpwork.DpWorkApplication;
import com.deep.dpwork.annotation.DpBugly;
import com.deep.dpwork.annotation.DpCrash;
import com.deep.dpwork.annotation.DpDataBase;
import com.deep.dpwork.annotation.DpLang;
import com.deep.dpwork.lang.LanguageType;
import com.deep.snapshot.data.AppData;
import com.deep.snapshot.util.BleUtil;

/**
 * Class - 主类
 * <p>
 * Created by Deepblue on 2019/9/29 0029.
 */
@DpCrash
@DpBugly("0fbc31ba0d")
@DpLang(LanguageType.LANGUAGE_FOLLOW_SYSTEM)
public class CoreApp extends DpWorkApplication {

    @DpDataBase(AppData.class)
    public static AppData appData;

    /**
     * 初始化函数
     * Bugly ID
     */
    @Override
    protected void initApplication() {
        BleUtil.getInstance().init(this);
    }

}
