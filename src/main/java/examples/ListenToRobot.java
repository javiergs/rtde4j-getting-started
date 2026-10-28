package examples;

import com.javiergs.rtde4j.RtdeClient;
import java.util.Arrays;

/** Monitor joint angles and tool pose for ten seconds. Read-only. */
public final class ListenToRobot {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        try (RtdeClient client = RtdeClient.connect(host)) {
            client.negotiateProtocolVersion();
            client.configureJointAndPoseOutputs(10.0);
            client.addPropertyChangeListener(event -> {
                if ("actual_q".equals(event.getPropertyName()) ||
                    "actual_TCP_pose".equals(event.getPropertyName())) {
                    System.out.println(event.getPropertyName() + ": " +
                        Arrays.toString((double[]) event.getNewValue()));
                }
            });
            client.start();
            client.startMonitoring();
            try {
                Thread.sleep(10_000);
            } finally {
                client.stopMonitoring();
            }
        }
    }
}
