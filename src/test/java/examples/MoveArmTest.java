package examples;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.javiergs.rtde4j.RtdeClient;
import com.javiergs.rtde4j.URScriptClient;
import com.javiergs.rtde4j.model.CartesianData;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Experiment D: each movement method, checked by reading where the arm settled over RTDE. URSim only; every test returns the arm to where it started. */
class MoveArmTest extends SimulatorTest {
    private static final double JOINT_TOLERANCE = 0.001;
    private static final double POSE_TOLERANCE = 0.001;
    private static final long MOVE_TIMEOUT_MS = 15_000;

    private RtdeClient rtde;
    private URScriptClient script;
    private double[] startJoints;

    @BeforeAll
    static void requireRunningRobot() throws Exception {
        assumeTrue(isReachable(), "URSim is not reachable");
        assumeTrue(robotMode() == ROBOT_MODE_RUNNING, "robot is not running, power it on and release the brakes on the pendant");
    }

    @BeforeEach
    void connect() throws Exception {
        rtde = startStream(10.0);
        rtde.startMonitoring();
        script = URScriptClient.connect(HOST);
        Thread.sleep(500);
        startJoints = joints();
    }

    @AfterEach
    void returnToStart() throws Exception {
        script.moveJoints(startJoints, 1.2, 0.25);
        waitUntilSettled(this::joints, startJoints, MOVE_TIMEOUT_MS);
        rtde.stopMonitoring();
        rtde.close();
        script.close();
    }

    @Test
    void moveJointsReachesTarget() throws Exception {
        double[] target = startJoints.clone();
        target[0] += 0.2;
        script.moveJoints(target, 1.2, 0.25);
        double[] reached = waitUntilSettled(this::joints, target, MOVE_TIMEOUT_MS);
        System.out.println("moveJoints off by " + maxDifference(reached, target) + " rad\n  target  " + Arrays.toString(target) + "\n  reached " + Arrays.toString(reached));
        assertTrue(maxDifference(reached, target) <= JOINT_TOLERANCE, "arm did not settle on the joint target");
    }

    @Test
    void moveToolReachesPose() throws Exception {
        double[] target = pose().clone();
        target[2] += 0.05;
        script.moveTool(target, 0.5, 0.1);
        double[] reached = waitUntilSettled(this::pose, target, MOVE_TIMEOUT_MS);
        System.out.println("moveTool off by " + maxDifference(reached, target) + "\n  target  " + Arrays.toString(target) + "\n  reached " + Arrays.toString(reached));
        assertTrue(maxDifference(reached, target) <= POSE_TOLERANCE, "tool did not settle on the pose 5 cm higher");
    }

    @Test
    void moveToPoseReachesPose() throws Exception {
        double[] target = pose().clone();
        target[1] += 0.05;
        CartesianData pose = new CartesianData().setX(target[0]).setY(target[1]).setZ(target[2])
                .setRx(target[3]).setRy(target[4]).setRz(target[5]);
        script.moveToPose(pose, 0.5, 0.1);
        double[] reached = waitUntilSettled(this::pose, target, MOVE_TIMEOUT_MS);
        System.out.println("moveToPose off by " + maxDifference(reached, target) + "\n  target  " + Arrays.toString(target) + "\n  reached " + Arrays.toString(reached));
        assertTrue(maxDifference(reached, target) <= POSE_TOLERANCE, "tool did not settle on the pose 5 cm over");
    }

    @Test
    void moveHomePositionReachesPreset() throws Exception {
        double[] home = URScriptClient.getHomePosition();
        script.moveHomePosition();
        double[] reached = waitUntilSettled(this::joints, home, MOVE_TIMEOUT_MS);
        System.out.println("moveHomePosition off by " + maxDifference(reached, home) + " rad\n  preset  " + Arrays.toString(home) + "\n  reached " + Arrays.toString(reached));
        assertTrue(maxDifference(reached, home) <= JOINT_TOLERANCE, "arm did not settle on the home preset");
    }

    //counterpart to ListenToRobotTest.clientListenerIsQuietWhileArmIsStill
    @Test
    void clientListenerFiresWhileArmMoves() throws Exception {
        AtomicInteger events = new AtomicInteger();
        PropertyChangeListener listener = event -> {
            if ("actual_q".equals(event.getPropertyName())) events.incrementAndGet();
        };
        rtde.addPropertyChangeListener(listener);
        double[] target = startJoints.clone();
        target[0] += 0.2;
        script.moveJoints(target, 1.2, 0.25);
        waitUntilSettled(this::joints, target, MOVE_TIMEOUT_MS);
        rtde.removePropertyChangeListener(listener);
        System.out.println("RtdeClient listener: " + events.get() + " actual_q events during a 0.2 rad move at 10 Hz");
        assertTrue(events.get() > 5, "listener did not report the joints changing");
    }

    @Test
    void jogMovesThenStops() throws Exception {
        script.jogMoveContinuous(new double[]{0.2, 0, 0, 0, 0, 0}, 1.2, 0.5);
        Thread.sleep(1_000);
        script.stopJogging(1.2);
        Thread.sleep(500);
        double[] afterStop = joints();
        Thread.sleep(500);
        double[] later = joints();
        double moved = Math.abs(afterStop[0] - startJoints[0]);
        System.out.println("jog moved the base " + moved + " rad, drift after stop " + maxDifference(afterStop, later) + " rad");
        assertTrue(moved > 0.01, "jog did not move the base joint");
        assertTrue(maxDifference(afterStop, later) < JOINT_TOLERANCE, "arm kept moving after stopJogging");
    }

    private double[] joints() {
        return rtde.getLatestSample().joints();
    }

    private double[] pose() {
        return rtde.getLatestSample().tcpPose();
    }
}
