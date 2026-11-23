package examples;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.javiergs.rtde4j.RtdeClient;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Reads the same robot with RTDE4J and with the ROVER parser's own RtdeClient (copied into src/test/java/rover) and checks they agree. Read-only. */
class CompareClientsTest extends SimulatorTest {
    private static final double TOLERANCE = 0.001;

    @Test
    void bothClientsReadSameJointsAndPose() throws Exception {
        rover.RtdeClient rover = new rover.RtdeClient(HOST, RTDE_PORT);
        try (RtdeClient rtde4j = startStream(10.0)) {
            rover.connect();
            rover.sendOutputSetup(List.of("actual_q", "actual_TCP_pose"), List.of("VECTOR6D", "VECTOR6D"));
            rover.sendStart();

            RtdeClient.RobotSample fromRtde4j = rtde4j.readSample();
            Map<String, double[]> fromRover = rover.receive();
            assertNotNull(fromRover, "rover client received no data");

            System.out.println("rtde4j actual_q: " + Arrays.toString(fromRtde4j.joints()));
            System.out.println("rover  actual_q: " + Arrays.toString(fromRover.get("actual_q")));
            assertTrue(maxDifference(fromRtde4j.joints(), fromRover.get("actual_q")) <= TOLERANCE, "joint angles differ");
            assertTrue(maxDifference(fromRtde4j.tcpPose(), fromRover.get("actual_TCP_pose")) <= TOLERANCE, "tool pose differs");
        } finally {
            rover.disconnect();
        }
    }

    @Test
    void bothClientsKeepUpAt125Hz() throws Exception {
        rover.RtdeClient rover = new rover.RtdeClient(HOST, RTDE_PORT);
        try (RtdeClient rtde4j = startStream(125.0)) {
            rover.connect();
            rover.sendOutputSetup(List.of("actual_q"), List.of("VECTOR6D"));
            rover.sendStart();

            long start = System.nanoTime();
            for (int i = 0; i < 125; i++) rtde4j.readSample();
            double rtde4jSeconds = (System.nanoTime() - start) / 1e9;

            //rover's receive() skips to the newest package, so it is timed per call rather than per package
            start = System.nanoTime();
            for (int i = 0; i < 125; i++) assertNotNull(rover.receive());
            double roverSeconds = (System.nanoTime() - start) / 1e9;

            System.out.println("125 reads: rtde4j " + rtde4jSeconds + " s, rover " + roverSeconds + " s");
            assertTrue(rtde4jSeconds < 1.5 && roverSeconds < 1.5, "a client fell behind the 125 Hz stream");
        } finally {
            rover.disconnect();
        }
    }
}
