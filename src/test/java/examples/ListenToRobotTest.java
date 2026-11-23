package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.javiergs.rtde4j.ReconnectingRtdeMonitor;
import com.javiergs.rtde4j.RtdeClient;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Experiment B: stream actual_q and actual_TCP_pose over RTDE. Read-only. */
class ListenToRobotTest extends SimulatorTest {

    @Test
    void samplesHaveSixJointsAndSixPoseValues() throws Exception {
        try (RtdeClient client = startStream(10.0)) {
            RtdeClient.RobotSample sample = client.readSample();
            System.out.println("actual_q (rad): " + Arrays.toString(sample.joints()));
            System.out.println("actual_TCP_pose (m, rad): " + Arrays.toString(sample.tcpPose()));
            assertEquals(6, sample.joints().length);
            assertEquals(6, sample.tcpPose().length);
            for (double joint : sample.joints()) {
                assertTrue(Math.abs(joint) <= 2 * Math.PI, "joint angle outside +-2pi rad: " + joint);
            }
            //UR3e reach is 0.5 m, so the tool position can't be further than that from the base
            double reach = Math.sqrt(Math.pow(sample.tcpPose()[0], 2) + Math.pow(sample.tcpPose()[1], 2) + Math.pow(sample.tcpPose()[2], 2));
            assertTrue(reach < 1.0, "tool position is " + reach + " m from base, units may not be meters");
        }
    }

    @Test
    void streamsAtRequestedFrequency() throws Exception {
        try (RtdeClient client = startStream(10.0)) {
            client.readSample();
            long start = System.nanoTime();
            for (int i = 0; i < 20; i++) client.readSample();
            double seconds = (System.nanoTime() - start) / 1e9;
            System.out.println("20 samples at 10 Hz took " + seconds + " s");
            assertTrue(seconds > 1.5 && seconds < 2.5, "expected about 2 s, took " + seconds);
        }
    }

    @Test
    void supportsParserRateOf125Hz() throws Exception {
        try (RtdeClient client = startStream(125.0)) {
            client.readSample();
            long start = System.nanoTime();
            for (int i = 0; i < 125; i++) client.readSample();
            double seconds = (System.nanoTime() - start) / 1e9;
            System.out.println("125 samples at 125 Hz took " + seconds + " s");
            assertTrue(seconds < 1.5, "expected about 1 s, took " + seconds);
        }
    }

    //RtdeClient only fires events when a value changes, so a still arm produces none (MoveArmTest checks a moving one)
    @Test
    void clientListenerIsQuietWhileArmIsStill() throws Exception {
        try (RtdeClient client = startStream(10.0)) {
            AtomicInteger events = new AtomicInteger();
            client.addPropertyChangeListener(event -> events.incrementAndGet());
            client.startMonitoring();
            try {
                Thread.sleep(500);
                events.set(0);
                Thread.sleep(2_000);
                assertNotNull(client.getLatestSample());
                System.out.println("RtdeClient listener: " + events.get() + " events in 2 s at 10 Hz with the arm still");
                assertEquals(0, events.get(), "events fired while the arm was still, don't move it on the pendant during this test");
            } finally {
                client.stopMonitoring();
            }
        }
    }

    //ReconnectingRtdeMonitor fires robot_sample on every sample, changed or not
    @Test
    void monitorListenerFiresEverySample() throws Exception {
        try (ReconnectingRtdeMonitor monitor = new ReconnectingRtdeMonitor(HOST, RTDE_PORT, 10.0, 0, 0)) {
            CountDownLatch samples = new CountDownLatch(20);
            monitor.addPropertyChangeListener(event -> {
                if ("robot_sample".equals(event.getPropertyName())) samples.countDown();
            });
            monitor.start();
            assertTrue(samples.await(4, TimeUnit.SECONDS), "fewer than 20 robot_sample events in 4 s at 10 Hz");
            System.out.println("ReconnectingRtdeMonitor listener: 20 robot_sample events with the arm still");
        }
    }

    @Test
    void readingBeforeStartIsRejected() throws Exception {
        try (RtdeClient client = RtdeClient.connect(HOST)) {
            client.negotiateProtocolVersion();
            client.configureJointAndPoseOutputs(10.0);
            Exception e = assertThrows(Exception.class, client::readSample);
            System.out.println("readSample before start: " + e);
        }
    }
}
