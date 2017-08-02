package com.wtuadn.thirdpartutils.wx;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.wtuadn.thirdpartutils.ThirdPartUtils;

public abstract class WXCallbackActivity extends Activity implements IWXAPIEventHandler {
    public WXCallbackActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThirdPartUtils.init(getApplicationContext());
        ThirdPartUtils.wxHandleIntent(getIntent(), this);
    }

    protected void onNewIntent(Intent paramIntent) {
        super.onNewIntent(paramIntent);
        this.setIntent(paramIntent);
        ThirdPartUtils.init(getApplicationContext());
        ThirdPartUtils.wxHandleIntent(paramIntent, this);
    }

    @Override
    public void onResp(BaseResp resp) {
        ThirdPartUtils.onWxCallBack(resp);
        finish();
    }

    @Override
    public void onReq(BaseReq req) {
    }
}