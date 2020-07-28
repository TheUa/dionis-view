

package the.ua.dionisview;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.webkit.DownloadListener;
import android.webkit.WebView;


public class AgentWebSettingsImpl extends AbsAgentWebSettings {
    private DionisView mDionisView;

    @Override
    protected void bindAgentWebSupport(DionisView dionisView) {
        this.mDionisView = dionisView;
    }

    @Override
    public WebListenerManager setDownloader(WebView webView, DownloadListener downloadListener) {
        if (downloadListener == null) {
            downloadListener = DefaultDownloadImpl.create(mDionisView.getActivity(), webView, mDionisView.getPermissionInterceptor());
        }
        return super.setDownloader(webView, downloadListener);
    }

    private Activity getActivityByContext(Context context) {
        if (context instanceof Activity) return (Activity) context;
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

}
