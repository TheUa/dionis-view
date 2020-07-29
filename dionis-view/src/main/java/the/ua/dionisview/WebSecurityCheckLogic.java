

package the.ua.dionisview;

import android.util.ArrayMap;
import android.webkit.WebView;


public interface WebSecurityCheckLogic {
    void dealHoneyComb(WebView view);
    void dealJsInterface(ArrayMap<String, Object> objects, DionisView.SecurityType securityType);
}
