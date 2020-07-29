

package the.ua.dionisview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import static the.ua.dionisview.ActionActivity.KEY_FROM_INTENTION;


public class DefaultChromeClient extends MiddlewareWebChromeBase {

	private WeakReference<Activity> mActivityWeakReference = null;
	/**
	 * DefaultChromeClient 's TAG
	 */
	private String TAG = DefaultChromeClient.class.getSimpleName();
	/**
	 * Android WebChromeClient path
	 */
	public static final String ANDROID_WEBCHROMECLIENT_PATH = "android.webkit.WebChromeClient";
	/**
	 * WebChromeClient
	 */
	private WebChromeClient mWebChromeClient;
	/**
	 * Flag
	 */
	private boolean mIsWrapper = false;
	/**
	 * Video 处
	 */
	private IVideo mIVideo;
	/**
	 * PermissionInterceptor
	 */
	private PermissionInterceptor mPermissionInterceptor;
	/**
	 * WebView
	 */
	private WebView mWebView;
	/**
	 * Web mOrigin
	 */
	private String mOrigin = null;
	/**
	 * Web  Callback
	 */
	private GeolocationPermissions.Callback mCallback = null;

	public static final int FROM_CODE_INTENTION = 0x18;

	public static final int FROM_CODE_INTENTION_LOCATION = FROM_CODE_INTENTION << 2;
	/**
	 * AbsAgentWebUIController
	 */
	private WeakReference<AbsAgentWebUIController> mAgentWebUIController = null;
	/**
	 * IndicatorController
	 */
	private IndicatorController mIndicatorController;

	private Object mFileChooser;

	DefaultChromeClient(Activity activity,
	                    IndicatorController indicatorController,
	                    WebChromeClient chromeClient,
	                    @Nullable IVideo iVideo,
	                    PermissionInterceptor permissionInterceptor, WebView webView) {
		super(chromeClient);
		this.mIndicatorController = indicatorController;
		mIsWrapper = chromeClient != null ? true : false;
		this.mWebChromeClient = chromeClient;
		mActivityWeakReference = new WeakReference<Activity>(activity);
		this.mIVideo = iVideo;
		this.mPermissionInterceptor = permissionInterceptor;
		this.mWebView = webView;
		mAgentWebUIController = new WeakReference<AbsAgentWebUIController>(AgentWebUtils.getAgentWebUIControllerByWebView(webView));
	}

	@Override
	public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
		return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
	}

	@Override
	public void onProgressChanged(WebView view, int newProgress) {
		super.onProgressChanged(view, newProgress);
		if (mIndicatorController != null) {
			mIndicatorController.progress(view, newProgress);
		}
	}

	@Override
	public void onReceivedTitle(WebView view, String title) {
		if (mIsWrapper) {
			super.onReceivedTitle(view, title);
		}
	}

	@Override
	public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
		if (mAgentWebUIController.get() != null) {
			mAgentWebUIController.get().onJsAlert(view, url, message);
		}
		result.confirm();
		return true;
	}


	@Override
	public void onReceivedIcon(WebView view, Bitmap icon) {
		super.onReceivedIcon(view, icon);
	}

	@Override
	public void onGeolocationPermissionsHidePrompt() {
		super.onGeolocationPermissionsHidePrompt();
	}

	//location
	@Override
	public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
		onGeolocationPermissionsShowPromptInternal(origin, callback);
	}

	private void onGeolocationPermissionsShowPromptInternal(String origin, GeolocationPermissions.Callback callback) {
		if (mPermissionInterceptor != null) {
			if (mPermissionInterceptor.intercept(this.mWebView.getUrl(), AgentWebPermissions.LOCATION, "location")) {
				callback.invoke(origin, false, false);
				return;
			}
		}
		Activity mActivity = mActivityWeakReference.get();
		if (mActivity == null) {
			callback.invoke(origin, false, false);
			return;
		}
		List<String> deniedPermissions = null;
		if ((deniedPermissions = AgentWebUtils.getDeniedPermissions(mActivity, AgentWebPermissions.LOCATION)).isEmpty()) {
			LogUtils.i(TAG, "onGeolocationPermissionsShowPromptInternal:" + true);
			callback.invoke(origin, true, false);
		} else {
			Action mAction = Action.createPermissionsAction(deniedPermissions.toArray(new String[]{}));
			mAction.setFromIntention(FROM_CODE_INTENTION_LOCATION);
			ActionActivity.setPermissionListener(mPermissionListener);
			this.mCallback = callback;
			this.mOrigin = origin;
			ActionActivity.start(mActivity, mAction);
		}
	}

	private ActionActivity.PermissionListener mPermissionListener = new ActionActivity.PermissionListener() {
		@Override
		public void onRequestPermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults, Bundle extras) {
			if (extras.getInt(KEY_FROM_INTENTION) == FROM_CODE_INTENTION_LOCATION) {
				boolean hasPermission = AgentWebUtils.hasPermission(mActivityWeakReference.get(), permissions);
				if (mCallback != null) {
					if (hasPermission) {
						mCallback.invoke(mOrigin, true, false);
					} else {
						mCallback.invoke(mOrigin, false, false);
					}
					mCallback = null;
					mOrigin = null;
				}
				if (!hasPermission && null != mAgentWebUIController.get()) {
					mAgentWebUIController
							.get()
							.onPermissionsDeny(
									AgentWebPermissions.LOCATION,
									AgentWebPermissions.ACTION_LOCATION,
									"Location");
				}
			}
		}
	};

	@Override
	public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
		try {
			if (this.mAgentWebUIController.get() != null) {
				this.mAgentWebUIController.get().onJsPrompt(mWebView, url, message, defaultValue, result);
			}
		} catch (Exception e) {
			if (LogUtils.isDebug()) {
				e.printStackTrace();
			}
		}
		return true;
	}

	@Override
	public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
		if (mAgentWebUIController.get() != null) {
			mAgentWebUIController.get().onJsConfirm(view, url, message, result);
		}
		return true;
	}


	@Override
	public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota, long estimatedDatabaseSize, long totalQuota, WebStorage.QuotaUpdater quotaUpdater) {
		quotaUpdater.updateQuota(totalQuota * 2);
	}

	@Override
	public void onReachedMaxAppCacheSize(long requiredStorage, long quota, WebStorage.QuotaUpdater quotaUpdater) {
		quotaUpdater.updateQuota(requiredStorage * 2);
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@Override
	public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
		LogUtils.i(TAG, "openFileChooser>=5.0");
		return openFileChooserAboveL(webView, filePathCallback, fileChooserParams);
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private boolean openFileChooserAboveL(WebView webView, ValueCallback<Uri[]> valueCallbacks, FileChooserParams fileChooserParams) {
		LogUtils.i(TAG, "fileChooserParams:" + fileChooserParams.getAcceptTypes() + "  getTitle:" + fileChooserParams.getTitle() + " accept:" + Arrays.toString(fileChooserParams.getAcceptTypes()) + " length:" + fileChooserParams.getAcceptTypes().length + "  :" + fileChooserParams.isCaptureEnabled() + "  " + fileChooserParams.getFilenameHint() + "  intent:" + fileChooserParams.createIntent().toString() + "   mode:" + fileChooserParams.getMode());
		Activity mActivity = this.mActivityWeakReference.get();
		if (mActivity == null || mActivity.isFinishing()) {
			return false;
		}
		return AgentWebUtils.showFileChooserCompat(mActivity,
				mWebView,
				valueCallbacks,
				fileChooserParams,
				this.mPermissionInterceptor,
				null,
				null,
				null
		);
	}

	@Override
	public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
	    /*believe me , i never want to do this */
		LogUtils.i(TAG, "openFileChooser>=4.1");
		createAndOpenCommonFileChooser(uploadFile, acceptType);
	}

	//  Android < 3.0
	@Override
	public void openFileChooser(ValueCallback<Uri> valueCallback) {
		Log.i(TAG, "openFileChooser<3.0");
		createAndOpenCommonFileChooser(valueCallback, "*/*");
	}

	//  Android  >= 3.0
	@Override
	public void openFileChooser(ValueCallback valueCallback, String acceptType) {
		Log.i(TAG, "openFileChooser>3.0");
		createAndOpenCommonFileChooser(valueCallback, acceptType);
	}


	private void createAndOpenCommonFileChooser(ValueCallback valueCallback, String mimeType) {
		Activity mActivity = this.mActivityWeakReference.get();
		if (mActivity == null || mActivity.isFinishing()) {
			valueCallback.onReceiveValue(new Object());
			return;
		}
		AgentWebUtils.showFileChooserCompat(mActivity,
				mWebView,
				null,
				null,
				this.mPermissionInterceptor,
				valueCallback,
				mimeType,
				null
		);
	}

	@Override
	public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
		super.onConsoleMessage(consoleMessage);
		return true;
	}

	@Override
	public void onShowCustomView(View view, CustomViewCallback callback) {
		if (mIVideo != null) {
			mIVideo.onShowCustomView(view, callback);
		}
	}

	@Override
	public void onHideCustomView() {
		if (mIVideo != null) {
			mIVideo.onHideCustomView();
		}
	}
}
