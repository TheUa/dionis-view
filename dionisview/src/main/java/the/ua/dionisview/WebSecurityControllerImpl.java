

package the.ua.dionisview;

import android.util.ArrayMap;
import android.webkit.WebView;


public class WebSecurityControllerImpl implements WebSecurityController<WebSecurityCheckLogic> {

	private WebView mWebView;
	private ArrayMap<String, Object> mMap;
	private DionisView.SecurityType mSecurityType;

	public WebSecurityControllerImpl(WebView view, ArrayMap<String, Object> map, DionisView.SecurityType securityType) {
		this.mWebView = view;
		this.mMap = map;
		this.mSecurityType = securityType;
	}

	@Override
	public void check(WebSecurityCheckLogic webSecurityCheckLogic) {
		webSecurityCheckLogic.dealHoneyComb(mWebView);
		if (mMap != null && mSecurityType == DionisView.SecurityType.STRICT_CHECK && !mMap.isEmpty()) {
			webSecurityCheckLogic.dealJsInterface(mMap, mSecurityType);
		}
	}
}
