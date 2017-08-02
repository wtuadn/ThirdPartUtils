package com.wtuadn.thirdpartutils.sina;

import android.app.Activity;
import android.content.Intent;

import com.sina.weibo.sdk.constant.WBConstants;
import com.sina.weibo.sdk.share.WbShareCallback;
import com.wtuadn.thirdpartutils.ThirdPartUtils;

public class WBShareActivity extends Activity implements WbShareCallback {
    protected void onNewIntent(Intent paramIntent) {
        super.onNewIntent(paramIntent);
        ThirdPartUtils.weiboShareHandleIntent(paramIntent, this);
    }

    @Override
    public void onWbShareSuccess() {
        ThirdPartUtils.onWeiboShareCallBack(WBConstants.ErrorCode.ERR_OK);
        finish();
    }

    @Override
    public void onWbShareCancel() {
        ThirdPartUtils.onWeiboShareCallBack(WBConstants.ErrorCode.ERR_CANCEL);
        finish();
    }

    @Override
    public void onWbShareFail() {
        ThirdPartUtils.onWeiboShareCallBack(WBConstants.ErrorCode.ERR_FAIL);
        finish();
    }
}