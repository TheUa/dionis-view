

package the.ua.dionisview;

public interface PermissionInterceptor {
    boolean intercept(String url, String[] permissions, String action);
}
