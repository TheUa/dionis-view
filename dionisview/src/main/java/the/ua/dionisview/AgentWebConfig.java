

package the.ua.dionisview;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import androidx.annotation.Nullable;
import com.download.library.BuildConfig;

import java.io.File;

import static the.ua.dionisview.AgentWebUtils.getAgentWebFilePath;


public class AgentWebConfig {

    static final String FILE_CACHE_PATH = "agentweb-cache";
    static final String AGENTWEB_CACHE_PATCH = File.separator + "agentweb-cache";

    static String AGENTWEB_FILE_PATH;

    public static boolean DEBUG = false;


    static final boolean IS_KITKAT_OR_BELOW_KITKAT = Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT;
    /**
     *  WebView   。
     */
    public static final int WEBVIEW_DEFAULT_TYPE = 1;
    /**
     *  AgentWebView
     */
    public static final int WEBVIEW_AGENTWEB_SAFE_TYPE = 2;
    /**
     *  WebView
     */
    public static final int WEBVIEW_CUSTOM_TYPE = 3;
    private static volatile boolean IS_INITIALIZED = false;
    private static final String TAG = AgentWebConfig.class.getSimpleName();
    public static final String AGENTWEB_NAME = "AgentWeb";
    /**
     * AgentWeb
     */
    public static final String AGENTWEB_VERSION = AGENTWEB_NAME + "/" + BuildConfig.VERSION_NAME;
    /**
     *  OutOfMemoryError
     */
    public static int MAX_FILE_LENGTH = 1024 * 1024 * 5;


    public static String getCookiesByUrl(String url) {
        return CookieManager.getInstance() == null ? null : CookieManager.getInstance().getCookie(url);
    }

    public static void debug() {
        DEBUG = true;
        WebView.setWebContentsDebuggingEnabled(true);
    }

    /**
     *  Cookies
     */
    public static void removeExpiredCookies() {
        CookieManager mCookieManager = null;
        if ((mCookieManager = CookieManager.getInstance()) != null) {
            mCookieManager.removeExpiredCookie();
            toSyncCookies();
        }
    }

    /**
     *  Cookies
     */
    public static void removeAllCookies() {
        removeAllCookies(null);
    }

    public static void removeSessionCookies() {
        removeSessionCookies(null);
    }

    /**
     * cookie
     *
     * @param url
     * @param cookies
     */
    public static void syncCookie(String url, String cookies) {
        CookieManager mCookieManager = CookieManager.getInstance();
        if (mCookieManager != null) {
            mCookieManager.setCookie(url, cookies);
            toSyncCookies();
        }
    }

    public static void removeSessionCookies(ValueCallback<Boolean> callback) {
        if (callback == null) {
            callback = getDefaultIgnoreCallback();
        }
        if (CookieManager.getInstance() == null) {
            callback.onReceiveValue(Boolean.FALSE);
            return;
        }
        CookieManager.getInstance().removeSessionCookies(callback);
        toSyncCookies();
    }

    /**
     * @param context
     * @return WebView
     */
    public static String getCachePath(Context context) {
        return context.getCacheDir().getAbsolutePath() + AGENTWEB_CACHE_PATCH;
    }

    /**
     * @param context
     * @return AgentWeb
     */
    public static String getExternalCachePath(Context context) {
        return getAgentWebFilePath(context);
    }


    //Android  4.4  NoSuchMethodError: android.webkit.CookieManager.removeAllCookies
    public static void removeAllCookies(@Nullable ValueCallback<Boolean> callback) {
        if (callback == null) {
            callback = getDefaultIgnoreCallback();
        }
        CookieManager.getInstance().removeAllCookies(callback);
        toSyncCookies();
    }

    /**
     *
     *
     * @param context
     */
    public static synchronized void clearDiskCache(Context context) {
        try {
            AgentWebUtils.clearCacheFolder(new File(getCachePath(context)), 0);
            String path = getExternalCachePath(context);
            if (!TextUtils.isEmpty(path)) {
                File mFile = new File(path);
                AgentWebUtils.clearCacheFolder(mFile, 0);
            }
        } catch (Throwable throwable) {
            if (LogUtils.isDebug()) {
                throwable.printStackTrace();
            }
        }
    }


    static synchronized void initCookiesManager(Context context) {
        if (!IS_INITIALIZED) {
            createCookiesSyncInstance(context);
            IS_INITIALIZED = true;
        }
    }

    private static void createCookiesSyncInstance(Context context) {
    }

    private static void toSyncCookies() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                CookieManager.getInstance().flush();
            }
        });
    }

    static String getDatabasesCachePath(Context context) {
        return context.getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
    }

    private static ValueCallback<Boolean> getDefaultIgnoreCallback() {
        return new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean ignore) {
                LogUtils.i(TAG, "removeExpiredCookies:" + ignore);
            }
        };
    }
}
