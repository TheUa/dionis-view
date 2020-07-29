

package the.ua.dionisview;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.webkit.DownloadListener;
import android.webkit.WebView;


public class AgentWebSettingsImpl extends AbsAgentWebSettings {

    @Override
    protected void bindAgentWebSupport(DionisView dionisView) {
    }

    @Override
    public WebListenerManager setDownloader(WebView webView, DownloadListener downloadListener) {
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
