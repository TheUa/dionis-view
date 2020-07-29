

package the.ua.dionisview;

import android.app.Activity;
import android.webkit.WebChromeClient;


public class MiddlewareWebChromeBase extends WebChromeClientDelegate {

    private MiddlewareWebChromeBase mMiddlewareWebChromeBase;


    protected MiddlewareWebChromeBase(WebChromeClient webChromeClient) {
        super(webChromeClient);
    }

    public MiddlewareWebChromeBase() {
        super(null);
    }


//    public MiddlewareWebChromeBase() {
//        super(null, null);
//    }

//    protected MiddlewareWebChromeBase(WebChromeClient webChromeClient) {
//        super(webChromeClient);
//    }

//    protected MiddlewareWebChromeBase() {
//        super(null);
//    }

    @Override
    final void setDelegate(WebChromeClient delegate) {
        super.setDelegate(delegate);
    }

    final MiddlewareWebChromeBase enq(MiddlewareWebChromeBase middlewareWebChromeBase) {
        setDelegate(middlewareWebChromeBase);
        this.mMiddlewareWebChromeBase = middlewareWebChromeBase;
        return this.mMiddlewareWebChromeBase;
    }


    final MiddlewareWebChromeBase next() {
        return this.mMiddlewareWebChromeBase;
    }

}
