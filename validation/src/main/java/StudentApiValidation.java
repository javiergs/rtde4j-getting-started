import com.javiergs.rtde4j.RtdeClient;
import com.javiergs.rtde4j.ReconnectingRtdeMonitor;
import com.javiergs.rtde4j.URScriptClient;
import com.javiergs.rtde4j.model.CartesianData;
import java.util.Arrays;

/** Compile-only smoke test of the actual published student-facing API. Never connects to hardware. */
public final class StudentApiValidation {
    public static void main(String[] args) throws Exception {
        // Compile all four getting-started experiments against Maven Central's binary.
        if (args.length == 0) return; // Never contact a robot in CI.
        String host = args[0];
        try (RtdeClient rtde = RtdeClient.connect(host)) {
            System.out.println(rtde.getControllerVersion());
            rtde.negotiateProtocolVersion();
            rtde.configureJointAndPoseOutputs(10.0);
            rtde.addPropertyChangeListener(e -> {
                if ("actual_q".equals(e.getPropertyName()) ||
                    "actual_TCP_pose".equals(e.getPropertyName()))
                    System.out.println(Arrays.toString((double[]) e.getNewValue()));
            });
            rtde.start();
            rtde.startMonitoring();
            Thread.sleep(2000);
            rtde.stopMonitoring();
        }
        try (ReconnectingRtdeMonitor monitor = new ReconnectingRtdeMonitor(host, 30004, 10.0, 2, 1000)) {
            monitor.addPropertyChangeListener(e -> System.out.println(e.getPropertyName()));
            monitor.start();
            Thread.sleep(2000);
        }
        try (URScriptClient script = URScriptClient.connect(host)) {
            script.sendCommand("textmsg(\"Hello from RTDE4J\")");
            // Compile-only checks: these movement APIs are intentionally NOT invoked.
            if (false) {
                script.moveJoints(new double[6], 1.2, 0.25);
                script.moveTool(new double[6], 0.5, 0.1);
                script.moveToPose(new CartesianData(), 0.5, 0.1);
                script.moveHomePosition();
                script.jogMoveContinuous(new double[6], 1.2, 0.2);
                script.stopJogging(1.2);
            }
        }
    }
}
