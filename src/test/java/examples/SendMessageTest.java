package examples;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.javiergs.rtde4j.URScriptClient;
import org.junit.jupiter.api.Test;

/** Experiment C: send URScript over port 30002, plus the input checks URScriptClient does before sending. No motion. */
class SendMessageTest extends SimulatorTest {

    @Test
    void sendsTextMessage() throws Exception {
        try (URScriptClient script = URScriptClient.connect(HOST)) {
            script.sendCommand("textmsg(\"Hello from RTDE4J test\")");
            System.out.println("Check the pendant Log tab for: Hello from RTDE4J test");
            assertTrue(script.isConnected());
        }
    }

    @Test
    void rejectsBlankCommand() throws Exception {
        try (URScriptClient script = URScriptClient.connect(HOST)) {
            Exception e = assertThrows(Exception.class, () -> script.sendCommand("   "));
            System.out.println("blank command: " + e);
        }
    }

    @Test
    void rejectsMultipleLines() throws Exception {
        try (URScriptClient script = URScriptClient.connect(HOST)) {
            Exception e = assertThrows(Exception.class, () -> script.sendCommand("textmsg(\"a\")\ntextmsg(\"b\")"));
            System.out.println("two-line command: " + e);
        }
    }

    @Test
    void rejectsJogLongerThanLimit() throws Exception {
        try (URScriptClient script = URScriptClient.connect(HOST)) {
            Exception e = assertThrows(Exception.class, () -> script.jogMoveContinuous(new double[6], 1.2, 1.0));
            System.out.println("jog duration 1.0 s (max " + URScriptClient.MAX_JOG_DURATION + "): " + e);
        }
    }

    @Test
    void rejectsJogFasterThanLimit() throws Exception {
        try (URScriptClient script = URScriptClient.connect(HOST)) {
            double[] speeds = {URScriptClient.MAX_JOG_SPEED * 2, 0, 0, 0, 0, 0};
            Exception e = assertThrows(Exception.class, () -> script.jogMoveContinuous(speeds, 1.2, 0.1));
            System.out.println("jog speed " + speeds[0] + " rad/s (max " + URScriptClient.MAX_JOG_SPEED + "): " + e);
        }
    }

    @Test
    void sendingAfterCloseIsRejected() throws Exception {
        URScriptClient script = URScriptClient.connect(HOST);
        script.close();
        Exception e = assertThrows(Exception.class, () -> script.sendCommand("textmsg(\"closed\")"));
        System.out.println("send after close: " + e);
    }
}
