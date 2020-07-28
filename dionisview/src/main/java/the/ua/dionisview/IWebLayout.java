

package the.ua.dionisview;

import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public interface IWebLayout<T extends WebView,V extends ViewGroup> {

    /**
     *
     * @return WebView
     */
    @NonNull
    V getLayout();


    @Nullable
    T getWebView();
}
