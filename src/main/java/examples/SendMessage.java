package examples;

import com.javiergs.rtde4j.URScriptClient;

/** Send a non-motion URScript message to URSim only. */
public final class SendMessage {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        if (!Boolean.getBoolean("rtde4j.allowScript")) {
            throw new IllegalStateException("Explicit opt-in required: -Drtde4j.allowScript=true (URSim only)");
        }
        try (URScriptClient script = URScriptClient.connect(host)) {
            script.sendCommand("textmsg(\"Hello from RTDE4J\")");
            System.out.println("Message sent; execution is not confirmed.");
        }
    }
}
