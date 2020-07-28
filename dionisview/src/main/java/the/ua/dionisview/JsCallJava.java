

package the.ua.dionisview;

import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;

public class JsCallJava {
    private final static String TAG = "JsCallJava";
    private final static String RETURN_RESULT_FORMAT = "{\"CODE\": %d, \"result\": %s}";
    private static final String MSG_PROMPT_HEADER = "AgentWeb:";
    private static final String KEY_OBJ = "obj";
    private static final String KEY_METHOD = "method";
    private static final String KEY_TYPES = "types";
    private static final String KEY_ARGS = "args";
    private static final String[] IGNORE_UNSAFE_METHODS = {"getClass", "hashCode", "notify", "notifyAll", "equals", "toString", "wait"};
    private HashMap<String, Method> mMethodsMap;
    private Object mInterfaceObj;
    private String mInterfacedName;
    private String mPreloadInterfaceJs;

    public JsCallJava(Object interfaceObj, String interfaceName) {
        try {
            if (TextUtils.isEmpty(interfaceName)) {
                throw new Exception("injected name can not be null");
            }
            mInterfaceObj = interfaceObj;
            mInterfacedName = interfaceName;
            mMethodsMap = new HashMap<String, Method>();
            Method[] methods = mInterfaceObj.getClass().getMethods();
            StringBuilder sb = new StringBuilder("javascript:(function(b){console.log(\"");
            sb.append(mInterfacedName);
            sb.append(" init begin\");var a={queue:[],callback:function(){var d=Array.prototype.slice.call(arguments,0);var c=d.shift();var e=d.shift();this.queue[c].apply(this,d);if(!e){delete this.queue[c]}}};");
            for (Method method : methods) {
                Log.i("Info","method:"+method);
                String sign;
                if ((sign = genJavaMethodSign(method)) == null) {
                    continue;
                }
                mMethodsMap.put(sign, method);
                sb.append(String.format("a.%s=", method.getName()));
            }
            sb.append("function(){var f=Array.prototype.slice.call(arguments,0);if(f.length<1){throw\"");
            sb.append(mInterfacedName);
            sb.append(" call result, message:miss method name\"}var e=[];for(var h=1;h<f.length;h++){var c=f[h];var j=typeof c;e[e.length]=j;if(j==\"function\"){var d=a.queue.length;a.queue[d]=c;f[h]=d}}var k = new Date().getTime();var l = f.shift();var m=prompt('");
            sb.append(MSG_PROMPT_HEADER);
            sb.append("'+JSON.stringify(");
            sb.append(promptMsgFormat("'" + mInterfacedName + "'", "l", "e", "f"));
            sb.append("));console.log(\"invoke \"+l+\", time: \"+(new Date().getTime()-k));var g=JSON.parse(m);if(g.CODE!=200){throw\"");
            sb.append(mInterfacedName);
            sb.append(" call result, CODE:\"+g.CODE+\", message:\"+g.result}return g.result};Object.getOwnPropertyNames(a).forEach(function(d){var c=a[d];if(typeof c===\"function\"&&d!==\"callback\"){a[d]=function(){return c.apply(a,[d].concat(Array.prototype.slice.call(arguments,0)))}}});b.");
            sb.append(mInterfacedName);
            sb.append("=a;console.log(\"");
            sb.append(mInterfacedName);
            sb.append(" init end\")})(window)");
            mPreloadInterfaceJs = sb.toString();
            sb.setLength(0);
        } catch (Exception e) {
            if (LogUtils.isDebug()) {
                Log.e(TAG, "init js result:" + e.getMessage());
            }
        }
    }

    private String genJavaMethodSign(Method method) {
        String sign = method.getName();
        Class[] argsTypes = method.getParameterTypes();
        for (String ignoreMethod : IGNORE_UNSAFE_METHODS) {
            if (ignoreMethod.equals(sign)) {
                if (LogUtils.isDebug()) {
                    Log.w(TAG, "method(" + sign + ") is unsafe, will be pass");
                }
                return null;
            }
        }
        int len = argsTypes.length;
        for (int k = 0; k < len; k++) {
            Class cls = argsTypes[k];
            if (cls == String.class) {
                sign += "_S";
            } else if (cls == int.class ||
                    cls == long.class ||
                    cls == float.class ||
                    cls == double.class) {
                sign += "_N";
            } else if (cls == boolean.class) {
                sign += "_B";
            } else if (cls == JSONObject.class) {
                sign += "_O";
            } else if (cls == JsCallback.class) {
                sign += "_F";
            } else {
                sign += "_P";
            }
        }
        return sign;
    }

    public String getPreloadInterfaceJs() {
        return mPreloadInterfaceJs;
    }

    public String call(WebView webView, JSONObject jsonObject) {
        long time = 0;
        if (LogUtils.isDebug()) {
            time = android.os.SystemClock.uptimeMillis();
        }
        if (jsonObject != null) {
            try {
                String methodName = jsonObject.getString(KEY_METHOD);
                JSONArray argsTypes = jsonObject.getJSONArray(KEY_TYPES);
                JSONArray argsVals = jsonObject.getJSONArray(KEY_ARGS);
                String sign = methodName;
                int len = argsTypes.length();
                Object[] values = new Object[len];
                int numIndex = 0;
                String currType;

                for (int k = 0; k < len; k++) {
                    currType = argsTypes.optString(k);
                    if ("string".equals(currType)) {
                        sign += "_S";
                        values[k] = argsVals.isNull(k) ? null : argsVals.getString(k);
                    } else if ("number".equals(currType)) {
                        sign += "_N";
                        numIndex = numIndex * 10 + k + 1;
                    } else if ("boolean".equals(currType)) {
                        sign += "_B";
                        values[k] = argsVals.getBoolean(k);
                    } else if ("object".equals(currType)) {
                        sign += "_O";
                        values[k] = argsVals.isNull(k) ? null : argsVals.getJSONObject(k);
                    } else if ("function".equals(currType)) {
                        sign += "_F";
                        values[k] = new JsCallback(webView, mInterfacedName, argsVals.getInt(k));
                    } else {
                        sign += "_P";
                    }
                }

                Method currMethod = mMethodsMap.get(sign);

                if (currMethod == null) {
                    return getReturn(jsonObject, 500, "not found method(" + sign + ") with valid parameters", time);
                }
                if (numIndex > 0) {
                    Class[] methodTypes = currMethod.getParameterTypes();
                    int currIndex;
                    Class currCls;
                    while (numIndex > 0) {
                        currIndex = numIndex - numIndex / 10 * 10 - 1;
                        currCls = methodTypes[currIndex];
                        if (currCls == int.class) {
                            values[currIndex] = argsVals.getInt(currIndex);
                        } else if (currCls == long.class) {
                            //WARN: argsJson.getLong(k + defValue) will return a bigger incorrect number
                            values[currIndex] = Long.parseLong(argsVals.getString(currIndex));
                        } else {
                            values[currIndex] = argsVals.getDouble(currIndex);
                        }
                        numIndex /= 10;
                    }
                }

                return getReturn(jsonObject, 200, currMethod.invoke(mInterfaceObj, values), time);
            } catch (Exception e) {
                LogUtils.safeCheckCrash(TAG, "call", e);
                if (e.getCause() != null) {
                    return getReturn(jsonObject, 500, "method execute result:" + e.getCause().getMessage(), time);
                }
                return getReturn(jsonObject, 500, "method execute result:" + e.getMessage(), time);
            }
        } else {
            return getReturn(jsonObject, 500, "call data empty", time);
        }
    }

    private String getReturn(JSONObject reqJson, int stateCode, Object result, long time) {
        String insertRes;
        if (result == null) {
            insertRes = "null";
        } else if (result instanceof String) {
            result = ((String) result).replace("\"", "\\\"");
            insertRes = "\"".concat(String.valueOf(result)).concat("\"");
        } else {
            insertRes = String.valueOf(result);

        }
        String resStr = String.format(RETURN_RESULT_FORMAT, stateCode, insertRes);
        if (LogUtils.isDebug()) {
            Log.d(TAG, "call time: " + (android.os.SystemClock.uptimeMillis() - time) + ", request: " + reqJson + ", result:" + resStr);
        }
        return resStr;
    }

    private static String promptMsgFormat(String object, String method, String types, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(KEY_OBJ).append(":").append(object).append(",");
        sb.append(KEY_METHOD).append(":").append(method).append(",");
        sb.append(KEY_TYPES).append(":").append(types).append(",");
        sb.append(KEY_ARGS).append(":").append(args);
        sb.append("}");
        return sb.toString();
    }

    static boolean isSafeWebViewCallMsg(String message) {
        return message.startsWith(MSG_PROMPT_HEADER);
    }

    static JSONObject getMsgJSONObject(String message) {
        message = message.substring(MSG_PROMPT_HEADER.length());
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(message);
        } catch (JSONException e) {
            e.printStackTrace();
            jsonObject = new JSONObject();
        }
        return jsonObject;
    }

    static String getInterfacedName(JSONObject jsonObject) {
        return jsonObject.optString(KEY_OBJ);
    }
}