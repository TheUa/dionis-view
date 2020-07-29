

package the.ua.dionisview;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.ArrayMap;
import android.webkit.WebView;


public class WebSecurityLogicImpl implements WebSecurityCheckLogic {
    private String TAG = this.getClass().getSimpleName();
    private int webviewType;

    public static WebSecurityLogicImpl getInstance(int webViewType) {
        return new WebSecurityLogicImpl(webViewType);
    }

    public WebSecurityLogicImpl(int webViewType) {
        this.webviewType = webViewType;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void dealHoneyComb(WebView view) {
    }

    @Override
    public void dealJsInterface(ArrayMap<String, Object> objects, DionisView.SecurityType securityType) {
    }
}
