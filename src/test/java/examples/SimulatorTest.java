package examples;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.javiergs.rtde4j.RtdeClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeAll;

/** Shared setup for tests that need URSim. They are skipped, not failed, when URSim is not running. */
abstract class SimulatorTest {
    static final String HOST = System.getProperty("ursim.host", "127.0.0.1");
    static final int RTDE_PORT = 30004;
    static final int ROBOT_MODE_RUNNING = 7;

    @BeforeAll
    static void requireSimulator() {
        assumeTrue(isReachable(), "URSim is not reachable on " + HOST + ":" + RTDE_PORT + ", start ur3e_container");
    }

    static boolean isReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, RTDE_PORT), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    //rtde4j only streams joints and pose, so the rover client is used to read robot_mode (7 = RUNNING, brakes released)
    static int robotMode() throws Exception {
        rover.RtdeClient client = new rover.RtdeClient(HOST, RTDE_PORT);
        try {
            client.connect();
            client.sendOutputSetup(List.of("robot_mode"), List.of("INT32"));
            client.sendStart();
            Map<String, double[]> state = client.receive();
            return state == null ? -1 : (int) state.get("robot_mode")[0];
        } finally {
            client.disconnect();
        }
    }

    static RtdeClient startStream(double frequencyHz) throws IOException {
        RtdeClient client = RtdeClient.connect(HOST);
        client.negotiateProtocolVersion();
        client.configureJointAndPoseOutputs(frequencyHz);
        client.start();
        return client;
    }

    static double maxDifference(double[] a, double[] b) {
        double max = 0;
        for (int i = 0; i < a.length; i++) max = Math.max(max, Math.abs(a[i] - b[i]));
        return max;
    }

    //polls until the values are within tolerance of the target, returns the last values read
    static double[] waitFor(Supplier<double[]> read, double[] target, double tolerance, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        double[] values = read.get();
        while (maxDifference(values, target) > tolerance && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
            values = read.get();
        }
        return values;
    }

    //waits until the arm is near the target, then until it stops moving, so the check runs on where it settled
    static double[] waitUntilSettled(Supplier<double[]> read, double[] target, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        waitFor(read, target, 0.05, timeoutMs);
        double[] previous = read.get();
        int stillReads = 0;
        while (stillReads < 5 && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
            double[] values = read.get();
            stillReads = maxDifference(values, previous) < 1e-5 ? stillReads + 1 : 0;
            previous = values;
        }
        return previous;
    }
}
