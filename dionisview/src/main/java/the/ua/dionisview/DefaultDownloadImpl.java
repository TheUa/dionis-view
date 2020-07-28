

package the.ua.dionisview;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.download.library.DownloadImpl;
import com.download.library.DownloadListenerAdapter;
import com.download.library.Extra;
import com.download.library.ResourceRequest;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultDownloadImpl implements android.webkit.DownloadListener {
    /**
     * Application Context
     */
    protected Context mContext;
    protected ConcurrentHashMap<String, ResourceRequest> mDownloadTasks = new ConcurrentHashMap<>();
    /**
     * Activity
     */
    protected WeakReference<Activity> mActivityWeakReference = null;

    private static final String TAG = DefaultDownloadImpl.class.getSimpleName();

    protected PermissionInterceptor mPermissionListener = null;
    /**
     * AbsAgentWebUIController
     */
    protected WeakReference<AbsAgentWebUIController> mAgentWebUIController;

    private static Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean isInstallDownloader;

    protected DefaultDownloadImpl(Activity activity, WebView webView, PermissionInterceptor permissionInterceptor) {
        this.mContext = activity.getApplicationContext();
        this.mActivityWeakReference = new WeakReference<Activity>(activity);
        this.mPermissionListener = permissionInterceptor;
        this.mAgentWebUIController = new WeakReference<AbsAgentWebUIController>(AgentWebUtils.getAgentWebUIControllerByWebView(webView));
        try {
            DownloadImpl.getInstance().with(this.mContext);
            isInstallDownloader = true;
        } catch (Throwable throwable) {
            LogUtils.e(TAG, "implementation 'com.download.library:Downloader:x.x.x'");
            if (LogUtils.isDebug()) {
                throwable.printStackTrace();
            }
            isInstallDownloader = false;
        }
    }


    @Override
    public void onDownloadStart(final String url, final String userAgent, final String contentDisposition, final String mimetype, final long contentLength) {
        if (!isInstallDownloader) {
            LogUtils.e(TAG, "unable start download " + url + "; implementation 'com.download.library:Downloader:x.x.x'");
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                onDownloadStartInternal(url, userAgent, contentDisposition, mimetype, contentLength);
            }
        });
    }

    protected void onDownloadStartInternal(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        if (null == mActivityWeakReference.get() || mActivityWeakReference.get().isFinishing()) {
            return;
        }
        if (null != this.mPermissionListener) {
            if (this.mPermissionListener.intercept(url, AgentWebPermissions.STORAGE, "download")) {
                return;
            }
        }
        ResourceRequest resourceRequest = createResourceRequest(url);
        this.mDownloadTasks.put(url, resourceRequest);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> mList = null;
            if ((mList = checkNeedPermission()).isEmpty()) {
                preDownload(url);
            } else {
                Action mAction = Action.createPermissionsAction(mList.toArray(new String[]{}));
                ActionActivity.setPermissionListener(getPermissionListener(url));
                ActionActivity.start(mActivityWeakReference.get(), mAction);
            }
        } else {
            preDownload(url);
        }
    }

    protected ResourceRequest createResourceRequest(String url) {
        return DownloadImpl.getInstance().with(url).setEnableIndicator(true).autoOpenIgnoreMD5();
    }

    protected ActionActivity.PermissionListener getPermissionListener(final String url) {
        return new ActionActivity.PermissionListener() {
            @Override
            public void onRequestPermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults, Bundle extras) {
                if (checkNeedPermission().isEmpty()) {
                    preDownload(url);
                } else {
                    if (null != mAgentWebUIController.get()) {
                        mAgentWebUIController
                                .get()
                                .onPermissionsDeny(
                                        checkNeedPermission().
                                                toArray(new String[]{}),
                                        AgentWebPermissions.ACTION_STORAGE, "Download");
                    }
                }

            }
        };
    }

    protected List<String> checkNeedPermission() {
        List<String> deniedPermissions = new ArrayList<>();
        if (!AgentWebUtils.hasPermission(mActivityWeakReference.get(), AgentWebPermissions.STORAGE)) {
            deniedPermissions.addAll(Arrays.asList(AgentWebPermissions.STORAGE));
        }
        return deniedPermissions;
    }

    protected void preDownload(String url) {
        if (!isForceRequest(url) &&
                AgentWebUtils.checkNetworkType(mContext) > 1) {
            showDialog(url);
            return;
        }
        performDownload(url);
    }

    protected boolean isForceRequest(String url) {
        ResourceRequest resourceRequest = mDownloadTasks.get(url);
        if (null != resourceRequest) {
            return resourceRequest.getDownloadTask().isForceDownload();
        }
        return false;
    }

    protected void forceDownload(final String url) {
        ResourceRequest resourceRequest = mDownloadTasks.get(url);
        assert resourceRequest != null;
        resourceRequest.setForceDownload(true);
        performDownload(url);
    }

    protected void showDialog(final String url) {
        Activity mActivity;
        if (null == (mActivity = mActivityWeakReference.get()) || mActivity.isFinishing()) {
            return;
        }
        AbsAgentWebUIController mAgentWebUIController;
        if (null != (mAgentWebUIController = this.mAgentWebUIController.get())) {
            mAgentWebUIController.onForceDownloadAlert(url, createCallback(url));
        }
    }

    protected Handler.Callback createCallback(final String url) {
        return new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                forceDownload(url);
                return true;
            }
        };
    }

    protected void performDownload(String url) {
        try {
            LogUtils.e(TAG, "performDownload:" + url + " exist:" + DownloadImpl.getInstance().exist(url));
            if (DownloadImpl.getInstance().exist(url)) {
                if (null != mAgentWebUIController.get()) {
                    mAgentWebUIController.get().onShowMessage(
                            mActivityWeakReference.get()
                                    .getString(R.string.agentweb_download_task_has_been_exist), "preDownload");
                }
                return;
            }
            ResourceRequest resourceRequest = mDownloadTasks.get(url);
            assert resourceRequest != null;
            resourceRequest.addHeader("Cookie", AgentWebConfig.getCookiesByUrl(url));
            taskEnqueue(resourceRequest);
        } catch (Throwable ignore) {
            if (LogUtils.isDebug()) {
                ignore.printStackTrace();
            }
        }
    }

    protected void taskEnqueue(ResourceRequest resourceRequest) {
        resourceRequest.enqueue(new DownloadListenerAdapter() {
            @Override
            public boolean onResult(Throwable throwable, Uri path, String url, Extra extra) {
                mDownloadTasks.remove(url);
                return super.onResult(throwable, path, url, extra);
            }
        });
    }

    public static DefaultDownloadImpl create(@NonNull Activity activity,
                                             @NonNull WebView webView,
                                             @Nullable PermissionInterceptor permissionInterceptor) {
        return new DefaultDownloadImpl(activity, webView, permissionInterceptor);
    }
}
