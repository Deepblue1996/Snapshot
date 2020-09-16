package com.deep.snapshot.view.dialog;

import android.widget.TextView;

import com.deep.dpwork.annotation.DpLayout;
import com.deep.dpwork.annotation.DpNullAnim;
import com.deep.dpwork.util.DTimeUtil;
import com.deep.snapshot.R;
import com.deep.snapshot.base.TDialogScreen;
import com.deep.snapshot.bean.TextBaseBean;

import butterknife.BindView;

@DpNullAnim
@DpLayout(R.layout.blue_loading_layout)
public class TextToastDialogScreen extends TDialogScreen {

    @BindView(R.id.connectText)
    public TextView connectText;

    private String msg = "";

    @Override
    public void init() {
        connectText.setText(msg);
    }

    public void setConnectText(String msg) {
        this.msg = msg;
    }
}
