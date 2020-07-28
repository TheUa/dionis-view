package the.ua.dionisview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Message;
import android.util.Log;
import android.webkit.*;
import android.webkit.WebViewClient;
import the.ua.dionisview.WebChromeClient;

public class OpenNewWindowWebChromeClient extends WebChromeClient {

    Activity activity;

    public OpenNewWindowWebChromeClient(Activity contex) {
        this.activity = contex;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {

        Log.e("onCreateWindow", " !");
        if (activity!=null) {
            final WebView newWebView = new WebView(activity) {
                @Override
                public boolean onCheckIsTextEditor() {
                    return true;
                }
            };

            Log.e("onCreateWindow2", " !");

            AlertDialog.Builder alert = new AlertDialog.Builder(activity);
            newWebView.getSettings().setJavaScriptEnabled(true);
            newWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
            newWebView.getSettings().setSupportMultipleWindows(true);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            alert.setView(newWebView);
            alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog1, int id) {
                    dialog1.dismiss();
                }
            });
            AlertDialog dialog = alert.create();
            dialog.show();
            transport.setWebView(newWebView);
            resultMsg.sendToTarget();

            newWebView.setWebViewClient(new WebViewClient() {

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

//                if (request.getUrl().toString().contains(zadanie)) {
//                    dialog.dismiss();
//                    agentWeb.getUrlLoader().reload();
//                    return true;
//                }
                    return super.shouldOverrideUrlLoading(view, request);
                }
            });

            newWebView.setWebChromeClient(new android.webkit.WebChromeClient() {
                @Override
                public void onCloseWindow(WebView window) {
                    super.onCloseWindow(window);
                    if (window != null) {
                        newWebView.removeView(window);
                    }
                }

                @Override
                public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
                    Log.e("onJsBeforeUnload", "+");
                    return super.onJsBeforeUnload(view, url, message, result);
                }

                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    Log.e("Errr", consoleMessage.message());
                    return super.onConsoleMessage(consoleMessage);
                }

                @Override
                public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                    Log.e("onJsPrompt", "+");
                    return super.onJsPrompt(view, url, message, defaultValue, result);
                }

                @Override
                public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                    Log.e("onJsAlert", "+");
                    return super.onJsAlert(view, url, message, result);
                }

                @Override
                public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                    result.confirm();
                    return super.onJsConfirm(view, url, message, result);
                }
            });
        }
        return true;
    }
}
