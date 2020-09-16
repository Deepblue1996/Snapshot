package com.deep.snapshot.view;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.deep.dpwork.annotation.DpLayout;
import com.deep.dpwork.dialog.DpSingleListDialogScreen;
import com.deep.dpwork.lang.LanguageType;
import com.deep.dpwork.util.AppUtils;
import com.deep.snapshot.R;
import com.deep.snapshot.base.TBaseScreen;
import com.deep.snapshot.event.HideBlackAnimEvent;
import com.deep.snapshot.event.SwitchLangEvent;
import com.deep.snapshot.util.ScreenUtil;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;

/**
 * Class - 设置
 * <p>
 * Created by Deepblue on 2019/9/29 0029.
 */
@DpLayout(R.layout.setting_screen_layout)
public class SettingScreen extends TBaseScreen {

    @BindView(R.id.backTouchLin)
    LinearLayout backTouchLin;
    @BindView(R.id.settingTouchLin)
    RelativeLayout settingTouchLin;
    @BindView(R.id.numberText)
    TextView numberText;

    @SuppressLint({"SetTextI18n", "ResourceType", "ClickableViewAccessibility"})
    @Override
    public void init() {

        ScreenUtil.switchFullScreen(_dpActivity, false);

        backTouchLin.setOnTouchListener((v, event) -> touchEvent(v, event, this::close));

        settingTouchLin.setOnTouchListener((v, event) ->
                touchEvent(v, event, () -> DpSingleListDialogScreen.create()
                        .setTitle(lang(R.string.shezhiyuyan))
                        .addButton(getContext(), 0, "中文", R.color.colorPrimary, dialogScreen -> {
                            dialogScreen.close(() -> EventBus.getDefault().post(new SwitchLangEvent(LanguageType.LANGUAGE_CHINESE_SIMPLIFIED)));
                        })
                        .addButton(getContext(), 0, "English", R.color.colorPrimary, dialogScreen -> {
                            dialogScreen.close(() -> EventBus.getDefault().post(new SwitchLangEvent(LanguageType.LANGUAGE_EN)));
                        })
                        .open(fragmentManager())));

        numberText.setText("Version " + AppUtils.getVersionName(getContext()) + "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().post(new HideBlackAnimEvent());
    }

    private boolean touchEvent(View v, MotionEvent event, TouchDo touchDo) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.setAlpha(0.5f);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                v.setAlpha(1.0f);
                touchDo.doThing();
                break;
        }
        return true;
    }

    private interface TouchDo {
        void doThing();
    }

    @Override
    public boolean isSwipe() {
        return true;
    }
}
