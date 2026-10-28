package examples;

import com.javiergs.rtde4j.RtdeClient;

/** Read the controller version without commanding motion. */
public final class ReadVersion {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        try (RtdeClient client = RtdeClient.connect(host)) {
            System.out.println("Controller version: " + client.getControllerVersion());
        }
    }
}
