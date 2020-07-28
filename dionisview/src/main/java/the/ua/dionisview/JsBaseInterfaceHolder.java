

package the.ua.dionisview;

import android.os.Build;
import android.webkit.JavascriptInterface;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public abstract class JsBaseInterfaceHolder implements JsInterfaceHolder {

    private DionisView.SecurityType mSecurityType;
    private WebCreator mWebCreator;

    protected JsBaseInterfaceHolder(WebCreator webCreator, DionisView.SecurityType securityType) {
        this.mSecurityType = securityType;
        this.mWebCreator = webCreator;
    }

    @Override
    public boolean checkObject(Object v) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return true;
        }
        if (mWebCreator.getWebViewType() == AgentWebConfig.WEBVIEW_AGENTWEB_SAFE_TYPE) {
            return true;
        }
        boolean tag = false;
        Class clazz = v.getClass();
        Method[] mMethods = clazz.getMethods();
        for (Method mMethod : mMethods) {
            Annotation[] mAnnotations = mMethod.getAnnotations();
            for (Annotation mAnnotation : mAnnotations) {
                if (mAnnotation instanceof JavascriptInterface) {
                    tag = true;
                    break;
                }
            }
            if (tag) {
                break;
            }
        }
        return tag;
    }

    protected boolean checkSecurity() {
        if (mSecurityType == DionisView.SecurityType.STRICT_CHECK) {
            mWebCreator.getWebViewType();
        }
        return true;
    }

}
