package examples;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.javiergs.rtde4j.RtdeClient;
import com.javiergs.rtde4j.URScriptClient;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** How RTDE4J reports connection failures. Does not need URSim. */
class ConnectionErrorTest {

    @Test
    void refusedRtdeConnectionFailsFast() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            IOException e = assertThrows(IOException.class, () -> RtdeClient.connect("127.0.0.1", 1));
            System.out.println("RTDE refused: " + e);
        });
    }

    @Test
    void refusedUrscriptConnectionFailsFast() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            IOException e = assertThrows(IOException.class, () -> URScriptClient.connect("127.0.0.1", 1));
            System.out.println("URScript refused: " + e);
        });
    }

    //10.255.255.1 is a non-routable address, so the connection neither succeeds nor is refused
    @Test
    void unreachableHostDoesNotHang() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            IOException e = assertThrows(IOException.class, () -> RtdeClient.connect("10.255.255.1"));
            System.out.println("unreachable host: " + e);
        });
    }

    @Test
    void invalidPortIsRejected() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> RtdeClient.connect("127.0.0.1", 70000));
        System.out.println("port 70000: " + e);
    }
}
