



package the.ua.dionisview;

import android.annotation.SuppressLint;
import android.util.Log;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
public class JsCallback {
    private static final String CALLBACK_JS_FORMAT = "javascript:%s.callback(%d, %d %s);";
    private int mIndex;
    private boolean mCouldGoOn;
    private WeakReference<WebView> mWebViewRef;
    private int mIsPermanent;
    private String mInjectedName;

    public JsCallback(WebView view, String injectedName, int index) {
        mCouldGoOn = true;
        mWebViewRef = new WeakReference<WebView>(view);
        mInjectedName = injectedName;
        mIndex = index;
    }

    public void apply (Object... args) throws JsCallbackException {
        if (mWebViewRef.get() == null) {
            throw new JsCallbackException("the WebView related to the JsCallback has been recycled");
        }
        if (!mCouldGoOn) {
            throw new JsCallbackException("the JsCallback isn't permanent,cannot be called more than once");
        }
        StringBuilder sb = new StringBuilder();
        for (Object arg : args){
            sb.append(",");
            boolean isStrArg = arg instanceof String;
            boolean isObjArg = isJavaScriptObject(arg);
            if (isStrArg && !isObjArg) {
                sb.append("\"");
            }
            sb.append(String.valueOf(arg));
            if (isStrArg && !isObjArg) {
                sb.append("\"");
            }
        }
        @SuppressLint("DefaultLocale") String execJs = String.format(CALLBACK_JS_FORMAT, mInjectedName, mIndex, mIsPermanent, sb.toString());
        if (LogUtils.isDebug()) {
            Log.d("JsCallBack", execJs);
        }
        mWebViewRef.get().loadUrl(execJs);
        mCouldGoOn = mIsPermanent > 0;
    }

    private boolean isJavaScriptObject(Object obj) {
        if (obj instanceof JSONObject || obj instanceof JSONArray) {
            return true;
        } else {
            String json = obj.toString();
            try {
                new JSONObject(json);
            } catch (JSONException e) {
                try {
                    new JSONArray(json);
                } catch (JSONException e1) {
                    return false;
                }
            }
            return true;
        }
    }


    public void setPermanent (boolean value) {
        mIsPermanent = value ? 1 : 0;
    }

    public static class JsCallbackException extends Exception {
        public JsCallbackException (String msg) {
            super(msg);
        }
    }
}
