package com.wtuadn.thirdpartutils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;

import com.sina.weibo.sdk.WbSdk;
import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WebpageObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WbAuthListener;
import com.sina.weibo.sdk.auth.WbConnectErrorMessage;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.constant.WBConstants;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.sina.weibo.sdk.share.WbShareCallback;
import com.sina.weibo.sdk.share.WbShareHandler;
import com.tencent.connect.UserInfo;
import com.tencent.connect.common.Constants;
import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzoneShare;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;
import com.wtuadn.thirdpartutils.sina.UsersAPI;
import com.wtuadn.thirdpartutils.sina.WBShareActivity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by wtuadn on 20161122.
 */

public class ThirdPartUtils {
    private static final String WX_APP_ID = "wx9f92b6a3b74c85d8";// todo 替换
    private static final String WX_SECRET = "b7b2619e9232cd150bf1a6ebe3edfe34";// todo 替换
    private static final String WX_SCOPE = "snsapi_userinfo";
    private static final String QQ_APP_ID = "1105185789";//记得替换manifest里的 // todo 替换
    private static final String QQ_SCOPE = "get_user_info";
    private static final String WEIBO_APP_KEY = "859166362";// todo 替换
    private static final String WEIBO_REDIRECT_URL = "https://www.google.com/";// todo 替换
    private static final String WEIBO_SCOPE = "";

    /**
     * 微信实例
     */
    private static IWXAPI mWX;
    /**
     * qq实例
     */
    private static Tencent mQQ;
    /**
     * 微博分享实例
     */
    private static WbShareHandler mWeiboShareHandler;

    /**
     * 登录时保存的监听
     */
    private static OnLoginListener mLoginListener;
    /**
     * 分享时保存的监听
     */
    private static OnShareListener mShareListener;
    /**
     * qq登录时保存的回调实例
     */
    private static IUiListener mIUiListener;
    /**
     * 微博登录时保存的handler
     */
    private static SsoHandler mSsoHandler;
    /**
     * okHttp实例
     */
    private static OkHttpClient okHttpClient;

    /**
     * 必须在使用之前至少调用一次，例如某个需要用到的activity的onCreate里，或者直接在application里初始化
     */
    public static void init(Context context) {
        if (!isInited()) {
            context = context.getApplicationContext();

            mWX = WXAPIFactory.createWXAPI(context, WX_APP_ID, true);
            mWX.registerApp(WX_APP_ID);

            mQQ = Tencent.createInstance(QQ_APP_ID, context);

            WbSdk.install(context, new AuthInfo(context, WEIBO_APP_KEY, WEIBO_REDIRECT_URL, WEIBO_SCOPE));

            okHttpClient = new OkHttpClient();
        }
    }

    private static boolean isInited() {
        if (mWX == null || mQQ == null) {
            return false;
        }
        return true;
    }

    public static void wxHandleIntent(Intent intent, IWXAPIEventHandler iwxapiEventHandler) {
        if (!isInited()) return;
        mWX.handleIntent(intent, iwxapiEventHandler);
    }

    public static void onWxCallBack(BaseResp resp) {
        if (resp == null) return;
        if (resp.errCode == BaseResp.ErrCode.ERR_OK) {
            if (mLoginListener != null) {
                if (resp instanceof SendAuth.Resp) {
                    onWXLogin((SendAuth.Resp) resp);
                }
            }
            if (mShareListener != null) {
                onSuccess(mLoginListener, mShareListener, null);
            }
        } else if (resp.errCode == BaseResp.ErrCode.ERR_USER_CANCEL) {
            onCancel(mLoginListener, mShareListener);
        } else {
            onFail(mLoginListener, mShareListener);
        }
    }

    public static void weiboShareHandleIntent(Intent intent, WbShareCallback callback) {
        if (!isInited()) return;
        if (mWeiboShareHandler != null) mWeiboShareHandler.doResultIntent(intent, callback);
    }

    public static void onWeiboShareCallBack(int errCode) {
        if (errCode == WBConstants.ErrorCode.ERR_OK) {
            onSuccess(mLoginListener, mShareListener, null);
        } else if (errCode == WBConstants.ErrorCode.ERR_CANCEL) {
            onCancel(mLoginListener, mShareListener);
        } else {
            onFail(mLoginListener, mShareListener);
        }
    }

    private static void onWXLogin(SendAuth.Resp resp) {
        SendAuth.Resp authResp = resp;
        Request request = new Request.Builder()
                .url("https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + WX_APP_ID
                        + "&secret=" + WX_SECRET + "&code=" + authResp.code + "&grant_type=authorization_code")
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                onFail(mLoginListener, null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                try {
                    JSONObject obj = new JSONObject(json);
                    String accessToken = obj.optString("access_token");
                    final String openid = obj.optString("openid");
                    Request request = new Request.Builder()
                            .url("https://api.weixin.qq.com/sns/userinfo?" +
                                    "access_token=" + accessToken + "&openid=" + openid)
                            .build();
                    okHttpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            onFail(mLoginListener, null);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String json = response.body().string();
                            try {
                                JSONObject obj = new JSONObject(json);
                                Map<String, Object> map = new HashMap<>();
                                map.put("platform", "weixin");
                                map.put("key", openid);
                                map.put("name", obj.optString("nickname"));
                                String headimgurl = obj.optString("headimgurl");
                                if (!TextUtils.isEmpty(headimgurl) && headimgurl.endsWith("/0")) {
                                    headimgurl = headimgurl.substring(0, headimgurl.length() - 1) + "132";
                                }
                                map.put("portrait", headimgurl);
                                int sex = obj.optInt("gender");
                                map.put("sex", sex);
                                onSuccess(mLoginListener, null, map);
                            } catch (Exception e) {
                                onFail(mLoginListener, null);
                            }
                        }
                    });
                } catch (Exception e) {
                    onFail(mLoginListener, null);
                }
            }
        });
    }

    /**
     * 登录成功后用户信息通过OnLoginListener返回一个map
     * 格式：
     * {
     * "platform":"QQ"、"weixin"、"weibo", 平台
     * "key":"", 用户在该平台的唯一标识
     * "name":"", 昵称
     * "portrait":"", 头像
     * "sex":0、1、2  0未知，1男，2女
     * }
     */
    public static void wxLogin(Context context, final OnLoginListener loginListener) {
        if (!isInited()) return;
        if (!isWeixinInstall(context)) {
            onFail(loginListener, null);
            return;
        }
        mLoginListener = loginListener;
        final SendAuth.Req req = new SendAuth.Req();
        req.scope = WX_SCOPE;
        req.state = "";
        mWX.sendReq(req);
    }

    /**
     * @see #wxLogin(Context, OnLoginListener)
     */
    public static void qqLogin(Activity activity, OnLoginListener loginListener) {
        if (!isInited()) return;
        if (!isQQInstall(activity)) {
            onFail(loginListener, null);
            return;
        }
        final Context context = activity.getApplicationContext();
        final WeakReference<OnLoginListener> loginListenerWeakReference = new WeakReference<>(loginListener);
        IUiListener iUiListener = new IUiListener() {
            @Override
            public void onComplete(Object o) {
                if (o instanceof JSONObject) {
                    JSONObject obj = (JSONObject) o;
                    if (obj.optInt("ret", -1) == 0) {
                        String token = obj.optString(Constants.PARAM_ACCESS_TOKEN);
                        String expires = obj.optString(Constants.PARAM_EXPIRES_IN);
                        final String openID = obj.optString(Constants.PARAM_OPEN_ID);
                        //**下面这两步设置很重要,如果没有设置,返回为空**
                        mQQ.setOpenId(openID);
                        mQQ.setAccessToken(token, expires);
                        new UserInfo(context, mQQ.getQQToken()).getUserInfo(new IUiListener() {
                            @Override
                            public void onComplete(Object o) {
                                JSONObject obj = (JSONObject) o;
                                if (obj.optInt("ret", -1) == 0) {
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("platform", "QQ");
                                    map.put("key", openID);
                                    map.put("name", obj.optString("nickname"));
                                    map.put("portrait", obj.optString("figureurl_qq_2"));
                                    String sex = obj.optString("gender");
                                    if (sex.equals("男")) {
                                        map.put("sex", 1);
                                    } else if (sex.equals("女")) {
                                        map.put("sex", 2);
                                    } else {
                                        map.put("sex", 0);
                                    }
                                    onSuccess(loginListenerWeakReference.get(), null, map);
                                } else {
                                    onFail(loginListenerWeakReference.get(), null);
                                }
                            }

                            @Override
                            public void onError(UiError uiError) {
                                onFail(loginListenerWeakReference.get(), null);
                            }

                            @Override
                            public void onCancel() {
                                ThirdPartUtils.onCancel(loginListenerWeakReference.get(), null);
                            }
                        });
                    }
                } else {
                    onFail(loginListenerWeakReference.get(), null);
                }
            }

            @Override
            public void onError(UiError uiError) {
                onFail(loginListenerWeakReference.get(), null);
            }

            @Override
            public void onCancel() {
                ThirdPartUtils.onCancel(loginListenerWeakReference.get(), null);
            }
        };
        mQQ.login(activity, QQ_SCOPE, iUiListener);
        mIUiListener = iUiListener;
    }

    /**
     * @see #wxLogin(Context, OnLoginListener)
     */
    public static void weiboLogin(Activity activity, OnLoginListener loginListener) {
        if (!isInited()) return;
        if (!isWeiboInstall(activity)) {
            onFail(loginListener, null);
            return;
        }
        final Context context = activity.getApplicationContext();
        final SsoHandler ssoHandler = new SsoHandler(activity);
        final WeakReference<OnLoginListener> loginListenerWeakReference = new WeakReference<>(loginListener);
        ssoHandler.authorize(new WbAuthListener() {
            @Override
            public void onSuccess(Oauth2AccessToken accessToken) {
                if (accessToken != null && accessToken.isSessionValid()) {
                    final String uid = accessToken.getUid();
                    if (uid != null && TextUtils.isDigitsOnly(uid)) {
                        new UsersAPI(context, WEIBO_APP_KEY, accessToken)
                                .show(Long.parseLong(accessToken.getUid()), new RequestListener() {
                                    @Override
                                    public void onComplete(String s) {
                                        try {
                                            JSONObject obj = new JSONObject(s);
                                            Map<String, Object> map = new HashMap<>();
                                            map.put("platform", "weibo");
                                            map.put("key", uid);
                                            map.put("name", obj.optString("screen_name"));
                                            map.put("portrait", obj.optString("profile_image_url"));
                                            String sex = obj.optString("gender");
                                            if (sex.equals("m")) {
                                                map.put("sex", 1);
                                            } else if (sex.equals("f")) {
                                                map.put("sex", 2);
                                            } else {
                                                map.put("sex", 0);
                                            }
                                            ThirdPartUtils.onSuccess(loginListenerWeakReference.get(), null, map);
                                        } catch (Exception e) {
                                            onFail(loginListenerWeakReference.get(), null);
                                        }
                                    }

                                    @Override
                                    public void onWeiboException(WeiboException e) {
                                        onFail(loginListenerWeakReference.get(), null);
                                    }
                                });
                    } else {
                        onFail(loginListenerWeakReference.get(), null);
                    }
                } else {
                    onFail(loginListenerWeakReference.get(), null);
                }
            }

            @Override
            public void onFailure(WbConnectErrorMessage errorMessage) {
                onFail(loginListenerWeakReference.get(), null);
            }

            @Override
            public void cancel() {
                onCancel(loginListenerWeakReference.get(), null);
            }
        });
        mSsoHandler = ssoHandler;
    }

    /**
     * 1.网页分享：url必传
     * 2.纯图分享：不能传url，bitmap必传
     * 3.文字分享：只传text
     */
    public static void wxShare(Context context, Bitmap bitmap, String title, String text, String url,
                               boolean isWXCircle, OnShareListener onShareListener) {
        if (!isInited()) return;
        if (!isWeixinInstall(context)) {
            onFail(null, onShareListener);
            return;
        }
        mShareListener = onShareListener;
        WXMediaMessage msg = new WXMediaMessage();
        WXMediaMessage.IMediaObject mediaObject;
        if (!TextUtils.isEmpty(url)) {
            mediaObject = new WXWebpageObject(url);
            msg.thumbData = getThumbByte(bitmap);
        } else if (bitmap != null) {
            WXImageObject imageObject = new WXImageObject();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            imageObject.imageData = stream.toByteArray();
            mediaObject = imageObject;
        } else {
            mediaObject = new WXTextObject(text);
        }
        msg.mediaObject = mediaObject;
        msg.title = title;
        msg.description = text;
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.message = msg;
        req.scene = isWXCircle ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
        mWX.sendReq(req);
    }

    /**
     * 分享图片需要sd卡权限,否则图片不能正常显示
     * 1.纯图分享：只用传bitmap 空间不支持纯图分享
     * 2.图文分享：必传title和url
     */
    public static void qqShare(Activity activity, Bitmap bitmap, String title, String text, String url,
                               boolean isQZone, final OnShareListener onShareListener) {
        if (!isInited()) return;
        if (!isQQInstall(activity)) {
            onFail(null, onShareListener);
            return;
        }
        String imgUrl = getLocalImgUrl(activity, bitmap);
        IUiListener iUiListener = new IUiListener() {
            @Override
            public void onComplete(Object o) {
                onSuccess(null, onShareListener, null);
            }

            @Override
            public void onError(UiError uiError) {
                onFail(null, onShareListener);
            }

            @Override
            public void onCancel() {
                ThirdPartUtils.onCancel(null, onShareListener);
            }
        };
        mIUiListener = iUiListener;
        final Bundle params = new Bundle();
        if (isQZone) {
            params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT);
            params.putString(QzoneShare.SHARE_TO_QQ_TITLE, title);//必填
            params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, text);//选填
            params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, url);//必填
            ArrayList<String> urls = new ArrayList<>();
            urls.add(imgUrl);
            params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, urls);
            mQQ.shareToQzone(activity, params, iUiListener);
        } else {
            if (!TextUtils.isEmpty(title)) {
                params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
                params.putString(QQShare.SHARE_TO_QQ_TITLE, title);//必填
                params.putString(QQShare.SHARE_TO_QQ_SUMMARY, text);//选填
                params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, url);//必填
                params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, imgUrl);
            } else {
                params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, imgUrl);
                params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
            }
            mQQ.shareToQQ(activity, params, iUiListener);
        }
    }

    /**
     * 1.网页分享：url必传
     * 2.图文分享：不能传url
     */
    public static void weiboShare(Activity activity, Bitmap bitmap, String title, String text, String url, OnShareListener onShareListener) {
        if (!isInited()) return;
//        if (!isWeiboInstall(activity)) {
//            onFail(null, onShareListener);
//            return;
//        }
        mShareListener = onShareListener;
        final WeiboMultiMessage weiboMessage = new WeiboMultiMessage();
        if (!TextUtils.isEmpty(url)) {
            WebpageObject webpageObject = new WebpageObject();
            webpageObject.actionUrl = url;
            webpageObject.title = title;
            webpageObject.description = text;
            webpageObject.schema = "";
            webpageObject.identify = "";
            webpageObject.thumbData = getThumbByte(bitmap);
            weiboMessage.mediaObject = webpageObject;
        } else {
            if (bitmap != null) {
                ImageObject imageObject = new ImageObject();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
                imageObject.imageData = stream.toByteArray();
                weiboMessage.imageObject = imageObject;
            }
            if (!TextUtils.isEmpty(text)) {
                TextObject textObject = new TextObject();
                textObject.text = text;
                weiboMessage.textObject = textObject;
            }
        }
        activity.getApplication().registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallback() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (activity instanceof WBShareActivity) {
                    mWeiboShareHandler = new WbShareHandler(activity);
                    mWeiboShareHandler.registerApp();
                    mWeiboShareHandler.shareMessage(weiboMessage, false);
                }
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (activity instanceof WBShareActivity) {
                    activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                }
            }
        });
        activity.startActivity(new Intent(activity, WBShareActivity.class));
    }

    public static String getLocalImgUrl(Context context, Bitmap bitmap) {
        String imgPath = null;
        try {
            File externalCacheDir = context.getExternalCacheDir();
            if (!externalCacheDir.exists()) {
                externalCacheDir.mkdirs();
            }
            File tempFile = new File(externalCacheDir.getAbsolutePath() + "/share_cache.png");
            imgPath = tempFile.getAbsolutePath();
            FileOutputStream fOut = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fOut);
            fOut.flush();
            fOut.close();
            System.gc();
        } catch (Exception ignore) {
        }
        return imgPath;
    }

    private static byte[] getThumbByte(Bitmap bitmap) {
        if (bitmap == null) return null;
        final Bitmap result = getThumbBitmap(bitmap);

        int maxLength = 32768;
        byte[] thumbData = null;
        int quality = 85;
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            for (int i = 0; i < 9; i++) {
                result.compress(Bitmap.CompressFormat.JPEG, quality, bs);
                thumbData = bs.toByteArray();
                if (thumbData.length <= maxLength) break;
                quality -= 10;
                bs.reset();
                if (quality <= 0) break;
            }
            bs.close();
            result.recycle();
            System.gc();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (thumbData.length > maxLength) {
            thumbData = null;
        }
        return thumbData;
    }

    private static Bitmap getThumbBitmap(Bitmap bitmap) {
        final int thumbWidth = 200, thumbHeight = 200;
        final float scale;
        float dx = 0, dy = 0;
        Matrix m = new Matrix();
        if (bitmap.getWidth() * thumbHeight > thumbWidth * bitmap.getHeight()) {
            scale = (float) thumbHeight / (float) bitmap.getHeight();
            dx = (thumbWidth - bitmap.getWidth() * scale) * 0.5f;
        } else {
            scale = (float) thumbWidth / (float) bitmap.getWidth();
            dy = (thumbHeight - bitmap.getHeight() * scale) * 0.5f;
        }
        m.setScale(scale, scale);
        m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        final Bitmap result = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bitmap, m, paint);
        return result;
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mSsoHandler != null) {
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
            mSsoHandler = null;
        }
        if (mIUiListener != null) {
            Tencent.onActivityResultData(requestCode, resultCode, data, mIUiListener);
            mIUiListener = null;
        }
    }

    private static void onSuccess(OnLoginListener onLoginListener, OnShareListener onShareListener, Map<String, Object> map) {
        if (onLoginListener != null) {
            onLoginListener.onSuccess(map);
        }
        if (onShareListener != null) {
            onShareListener.onSuccess();
        }
        mWeiboShareHandler = null;
        mSsoHandler = null;
        mIUiListener = null;
        mLoginListener = null;
        mShareListener = null;
    }

    private static void onFail(OnLoginListener onLoginListener, OnShareListener onShareListener) {
        if (onLoginListener != null) {
            onLoginListener.onFail(false);
        }
        if (onShareListener != null) {
            onShareListener.onFail(false);
        }
        mWeiboShareHandler = null;
        mSsoHandler = null;
        mIUiListener = null;
        mLoginListener = null;
        mShareListener = null;
    }

    private static void onCancel(OnLoginListener onLoginListener, OnShareListener onShareListener) {
        if (onLoginListener != null) {
            onLoginListener.onFail(true);
        }
        if (onShareListener != null) {
            onShareListener.onFail(true);
        }
        mWeiboShareHandler = null;
        mSsoHandler = null;
        mIUiListener = null;
        mLoginListener = null;
        mShareListener = null;
    }

    public static void destroy() {
        mLoginListener = null;
        mShareListener = null;
        mSsoHandler = null;
        mIUiListener = null;
        mWX = null;
        mQQ = null;
        mWeiboShareHandler = null;
        okHttpClient = null;
    }

    public static boolean isWeixinInstall(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++) {
                if ("com.tencent.mm".equals(pinfo.get(i).packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isQQInstall(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++) {
                if ("com.tencent.mobileqq".equals(pinfo.get(i).packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isWeiboInstall(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++) {
                if ("com.sina.weibo".equals(pinfo.get(i).packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 回调有可能是后台线程
     */
    public interface OnLoginListener {
        void onSuccess(Map<String, Object> map);

        void onFail(boolean isCancel);
    }

    public interface OnShareListener {
        void onSuccess();

        void onFail(boolean isCancel);
    }

    private static abstract class EmptyActivityLifecycleCallback implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }
}
