package examples;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.javiergs.rtde4j.RtdeClient;
import com.javiergs.rtde4j.URScriptClient;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks RTDE4J against how the ROVER parser drives the arm: movej at a=1.2, v=0.5, resent whenever a new MQTT target arrives. URSim only. */
class ParserBehaviorTest extends SimulatorTest {
    //same values the parser sends: movej(q, a=1.2, v=0.5)
    private static final double PARSER_ACCELERATION = 1.2;
    private static final double PARSER_VELOCITY = 0.5;
    private static final double JOINT_TOLERANCE = 0.001;
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
        rtde = startStream(125.0);
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
    void moveJointsAtParserSpeed() throws Exception {
        double[] target = startJoints.clone();
        target[0] += 0.3;
        long start = System.currentTimeMillis();
        script.moveJoints(target, PARSER_ACCELERATION, PARSER_VELOCITY);
        double[] reached = waitUntilSettled(this::joints, target, MOVE_TIMEOUT_MS);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("parser speed: 0.3 rad move settled in " + elapsed + " ms (includes ~500 ms to confirm stopped), off by " + maxDifference(reached, target) + " rad");
        assertTrue(maxDifference(reached, target) <= JOINT_TOLERANCE, "arm did not settle on the target at parser speed");
    }

    //the parser sends a new movej as soon as a new MQTT target arrives, even if the last move hasn't finished
    @Test
    void newTargetReplacesMoveInProgress() throws Exception {
        double[] first = startJoints.clone();
        first[0] += 0.6;
        double[] second = startJoints.clone();
        second[0] += 0.1;
        script.moveJoints(first, PARSER_ACCELERATION, PARSER_VELOCITY);
        double furthest = 0;
        long switchAt = System.currentTimeMillis() + 400;
        while (System.currentTimeMillis() < switchAt) {
            furthest = Math.max(furthest, joints()[0] - startJoints[0]);
            Thread.sleep(10);
        }
        script.moveJoints(second, PARSER_ACCELERATION, PARSER_VELOCITY);
        long deadline = System.currentTimeMillis() + 3_000;
        while (System.currentTimeMillis() < deadline) {
            furthest = Math.max(furthest, joints()[0] - startJoints[0]);
            Thread.sleep(10);
        }
        double[] reached = waitUntilSettled(this::joints, second, MOVE_TIMEOUT_MS);
        System.out.println("first target +0.6 rad, replaced after 400 ms by +0.1 rad; furthest the base got +" + furthest
                + " rad, settled off by " + maxDifference(reached, second) + " rad");
        assertTrue(furthest < 0.6 - JOINT_TOLERANCE, "arm finished the first move instead of switching to the new target");
        assertTrue(maxDifference(reached, second) <= JOINT_TOLERANCE, "arm did not settle on the new target");
    }

    //like an app dragging a slider: a small new target every 50 ms, the parser forwarding each as a movej
    @Test
    void streamOfTargetsEndsOnLastTarget() throws Exception {
        double[] target = startJoints.clone();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            target[0] += 0.015;
            script.moveJoints(target, PARSER_ACCELERATION, PARSER_VELOCITY);
            Thread.sleep(50);
        }
        double[] reached = waitUntilSettled(this::joints, target, MOVE_TIMEOUT_MS);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("20 targets 50 ms apart (0.3 rad total) settled in " + elapsed + " ms, off by " + maxDifference(reached, target) + " rad");
        assertTrue(maxDifference(reached, target) <= JOINT_TOLERANCE, "arm did not settle on the last target of the stream");
    }

    private double[] joints() {
        return rtde.getLatestSample().joints();
    }
}
