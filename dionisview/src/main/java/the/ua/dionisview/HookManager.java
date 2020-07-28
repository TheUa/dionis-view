

package the.ua.dionisview;


public class HookManager {

    public static DionisView hookAgentWeb(DionisView dionisView, DionisView.AgentBuilder agentBuilder) {
        return dionisView;
    }

    public static boolean permissionHook(String url,String[]permissions){
        return true;
    }
}
