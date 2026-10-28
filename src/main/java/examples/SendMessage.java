package examples;

import com.javiergs.rtde4j.URScriptClient;

/** Send a non-motion URScript message to URSim only. */
public final class SendMessage {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        if (args.length < 2 || !"SIMULATOR".equals(args[1])) {
            throw new IllegalStateException("Explicit opt-in required: HOST SIMULATOR (URSim only)");
        }
        try (URScriptClient script = URScriptClient.connect(host)) {
            script.sendCommand("textmsg(\"Hello from RTDE4J\")");
            System.out.println("Message sent; execution is not confirmed.");
        }
    }
}
