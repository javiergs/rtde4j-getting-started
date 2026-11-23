package examples;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.javiergs.rtde4j.RtdeClient;
import org.junit.jupiter.api.Test;

/** Experiment A: read the controller version over RTDE. Read-only. */
class ReadVersionTest extends SimulatorTest {

    @Test
    void readsControllerVersion() throws Exception {
        try (RtdeClient client = RtdeClient.connect(HOST)) {
            RtdeClient.ControllerVersion version = client.getControllerVersion();
            System.out.println("Controller version: " + version);
            assertTrue(version.major() >= 5, "e-Series controllers report major version 5 or higher, got " + version);
        }
    }

    @Test
    void negotiatesProtocolVersion() throws Exception {
        try (RtdeClient client = RtdeClient.connect(HOST)) {
            client.negotiateProtocolVersion();
            assertTrue(client.isConnected());
        }
    }
}
