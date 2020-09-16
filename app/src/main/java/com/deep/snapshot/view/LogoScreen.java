package com.deep.snapshot.view;

import android.content.Intent;
import android.os.Handler;

import com.deep.dpwork.DpLogoScreen;
import com.deep.dpwork.annotation.DpLayout;
import com.deep.dpwork.annotation.DpStatus;
import com.deep.dpwork.util.DTimeUtil;
import com.deep.snapshot.R;
import com.deep.snapshot.core.WorkCore;

import butterknife.ButterKnife;

/**
 * Class - 启动图
 * <p>
 * Created by Deepblue on 2018/8/23.
 */

@DpStatus(blackFont = true)
@DpLayout(R.layout.logo_screen)
public class LogoScreen extends DpLogoScreen {

    @Override
    protected void initView() {
        ButterKnife.bind(this);
        DTimeUtil.run(2000, this::in);
    }

    public void in() {
        Intent intent = new Intent();
        intent.setClass(LogoScreen.this, WorkCore.class);
        startActivity(intent);

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        new Handler().postDelayed(LogoScreen.this::finish, 100);
    }

}
