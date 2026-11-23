package examples;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.javiergs.rtde4j.ReconnectingRtdeMonitor;
import com.javiergs.rtde4j.ReconnectingRtdeMonitor.State;
import com.javiergs.rtde4j.URScriptClient;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Stops and restarts the URSim container to test connection loss. Takes a few minutes, so it only runs with -Dreconnect=true. Leaves the robot powered off. */
@EnabledIfSystemProperty(named = "reconnect", matches = "true")
class ReconnectTest extends SimulatorTest {
    private static final String CONTAINER = System.getProperty("ursim.container", "ur3e_container");

    //every test ends with the simulator running again so the next one starts the same way
    @AfterEach
    void restoreSimulator() throws Exception {
        docker("start");
        waitForSimulator();
    }

    @Test
    void monitorReconnectsAfterSimulatorRestart() throws Exception {
        List<State> states = new CopyOnWriteArrayList<>();
        //host, port, frequency in Hz, max retries, delay between retries in ms
        try (ReconnectingRtdeMonitor monitor = new ReconnectingRtdeMonitor(HOST, RTDE_PORT, 2.0, 60, 2000)) {
            monitor.addPropertyChangeListener(event -> {
                if ("connection_state".equals(event.getPropertyName())) {
                    System.out.println("connection_state: " + event.getOldValue() + " -> " + event.getNewValue());
                    states.add((State) event.getNewValue());
                }
            });
            monitor.start();
            assertTrue(waitForState(monitor, State.CONNECTED, 10_000), "monitor never connected");

            docker("stop");
            assertTrue(waitForState(monitor, State.RETRYING, 30_000), "monitor did not notice the simulator stopping, state " + monitor.getState());
            System.out.println("last failure: " + monitor.getLastFailure());

            docker("start");
            long restartedAt = System.currentTimeMillis();
            assertTrue(waitForState(monitor, State.CONNECTED, 120_000), "monitor did not reconnect, state " + monitor.getState());
            System.out.println("reconnected " + (System.currentTimeMillis() - restartedAt) + " ms after docker start finished");
            Object before = monitor.getLatestSample();
            Thread.sleep(2_000);
            assertTrue(monitor.getLatestSample() != before, "no new samples after reconnecting");
            System.out.println("state history: " + states);
        }
        int mode = robotMode();
        System.out.println("robot_mode after restart: " + mode + " (7 = running)");
        assertTrue(mode != ROBOT_MODE_RUNNING, "expected the robot to come back powered off after a restart");
    }

    @Test
    void monitorStopsAfterRetriesRunOut() throws Exception {
        //3 retries 500 ms apart, so it should give up about 1.5 s after the simulator goes down
        try (ReconnectingRtdeMonitor monitor = new ReconnectingRtdeMonitor(HOST, RTDE_PORT, 2.0, 3, 500)) {
            monitor.addPropertyChangeListener(event -> {
                if ("connection_state".equals(event.getPropertyName())) {
                    System.out.println("connection_state: " + event.getOldValue() + " -> " + event.getNewValue());
                }
            });
            monitor.start();
            assertTrue(waitForState(monitor, State.CONNECTED, 10_000), "monitor never connected");

            docker("stop");
            assertTrue(waitForState(monitor, State.STOPPED, 30_000), "monitor kept retrying past its limit, state " + monitor.getState());
            System.out.println("gave up with last failure: " + monitor.getLastFailure());

            docker("start");
            waitForSimulator();
            Thread.sleep(5_000);
            System.out.println("state 5 s after the simulator came back: " + monitor.getState());
            assertTrue(monitor.getState() == State.STOPPED, "monitor restarted on its own after giving up");
        }
    }

    //the parser keeps one URScript socket open and resends movej on it, so it has to notice when that socket dies
    @Test
    void urscriptClientAfterSimulatorStop() throws Exception {
        URScriptClient script = URScriptClient.connect(HOST);
        script.sendCommand("textmsg(\"before stop\")");
        docker("stop");

        IOException failure = null;
        int attempt = 0;
        while (failure == null && attempt < 20) {
            attempt++;
            try {
                script.sendCommand("textmsg(\"after stop\")");
                Thread.sleep(200);
            } catch (IOException e) {
                failure = e;
            }
        }
        System.out.println("after stop: send " + (failure == null ? "never failed in 20 attempts" : "failed on attempt " + attempt + " with " + failure)
                + ", isConnected " + script.isConnected());
        assertTrue(failure != null, "sending to a stopped simulator never reported an error");

        docker("start");
        waitForSimulator();
        try (URScriptClient fresh = URScriptClient.connect(HOST)) {
            fresh.sendCommand("textmsg(\"after restart\")");
            System.out.println("a new URScriptClient connected and sent after the restart");
        }
        script.close();
    }

    private static boolean waitForState(ReconnectingRtdeMonitor monitor, State state, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (monitor.getState() != state && System.currentTimeMillis() < deadline) Thread.sleep(200);
        return monitor.getState() == state;
    }

    //the ports open before the controller is ready, so this waits until RTDE actually returns data
    private static void waitForSimulator() throws Exception {
        long deadline = System.currentTimeMillis() + 120_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (isReachable() && robotMode() >= 0) return;
            } catch (Exception e) {
                //controller still starting
            }
            Thread.sleep(1_000);
        }
        throw new IllegalStateException("URSim did not come back within 2 minutes");
    }

    //output is discarded because Maven's test runner treats anything written straight to the console as corruption
    private static void docker(String command) throws Exception {
        System.out.println("docker " + command + " " + CONTAINER);
        Process process = new ProcessBuilder("docker", command, CONTAINER)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        if (process.waitFor() != 0) throw new IllegalStateException("docker " + command + " failed");
    }
}
