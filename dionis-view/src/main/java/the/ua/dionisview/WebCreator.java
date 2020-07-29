

package the.ua.dionisview;

import android.webkit.WebView;
import android.widget.FrameLayout;

public interface WebCreator extends IWebIndicator {
    WebCreator create();

    WebView getWebView();

    FrameLayout getWebParentLayout();

    int getWebViewType();
}
