# Student quick start: RTDE4J with URSim

This guide introduces **RTDE4J** using a Universal Robots simulator before any physical robot. The library implementation is maintained privately; this public repository contains documentation and examples. You do **not** need access to the private source repository.

> [!IMPORTANT]
> **Artifact check:** Before starting, confirm that `com.javiergs:rtde4j:1.0.0` is available in the configured Maven repository and contains `RtdeClient`, `ReconnectingRtdeMonitor`, and `URScriptClient`. The current source project declares version 1.0.0, but that alone does **not** establish that these APIs are published to Maven Central. If dependency resolution fails, ask the instructor for the correct published version; do not clone the private library.
>
> **Safety:** Work in **URSim only** for these experiments. Sending URScript can move a robot. Do not connect these exercises to physical hardware without instructor authorization and an appropriate safety assessment.

## 1. Prepare your environment

Install Java **21+**, Maven, and a working Universal Robots **URSim** instance. The simulator may run in Docker or another instructor-provided environment. Follow that environment's instructions to start the robot controller and enable its remote interfaces.

Make the simulator reachable from the Java program. You will need its **host/IP address**, which depends on whether Java runs on your host computer, in another container, or on the same Docker network.

The library uses two separate TCP connections:

| Interface | Port | Purpose |
| --- | --- | --- |
| RTDE | `30004` | Read robot state, including joint positions and TCP pose |
| URScript secondary interface | `30002` | Send URScript statements and movement commands |

If using Docker port publishing, ensure these ports are exposed **only as needed** in your trusted development environment. A published port does not guarantee the controller is ready; check URSim startup and networking first.

## 2. Create a Maven project

Create a standard Maven Java project with `src/main/java` and a `pom.xml` containing the dependency below. Set your compiler release to 21.

```xml
<dependency>
    <groupId>com.javiergs</groupId>
    <artifactId>rtde4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

Replace the version if the instructor identifies a different published release. The snippets below can be placed in separate Java classes with a `main` method. Use the simulator's reachable IP address instead of `192.168.0.10`.

## 3. Experiment A — connect and read the controller version

```java
import com.javiergs.rtde4j.RtdeClient;

public class ReadVersion {
    public static void main(String[] args) throws Exception {
        try (RtdeClient client = RtdeClient.connect("192.168.0.10")) {
            System.out.println(client.getControllerVersion());
        }
    }
}
```

This is a connectivity test; it does **not** start streaming or command robot motion.

## 4. Experiment B — listen to RTDE updates

`RtdeClient` supports RTDE protocol version 2 and a **read-only** recipe for `actual_q` (six joint angles in radians) and `actual_TCP_pose` (position in meters and orientation as a rotation vector).

```java
import com.javiergs.rtde4j.RtdeClient;
import java.util.Arrays;

public class ListenToRobot {
    public static void main(String[] args) throws Exception {
        try (RtdeClient client = RtdeClient.connect("192.168.0.10")) {
            client.negotiateProtocolVersion();
            client.configureJointAndPoseOutputs(10.0); // request 10 Hz
            client.addPropertyChangeListener(event -> {
                if ("actual_q".equals(event.getPropertyName())) {
                    System.out.println("Joints: " +
                        Arrays.toString((double[]) event.getNewValue()));
                } else if ("actual_TCP_pose".equals(event.getPropertyName())) {
                    System.out.println("TCP pose: " +
                        Arrays.toString((double[]) event.getNewValue()));
                }
            });
            client.start();
            client.startMonitoring();
            Thread.sleep(10_000); // observe for ten seconds
            client.stopMonitoring();
        }
    }
}
```

Listener callbacks run on the monitoring thread, not on a Swing event-dispatch thread. Keep callbacks short; update graphical interfaces through the appropriate UI thread. Do not call `readSample()` concurrently with background monitoring.

For a longer-running application, examine `ReconnectingRtdeMonitor`, which can re-establish a read-only monitoring session after I/O failures and publishes `connection_state` and `robot_sample` events.

## 5. Experiment C — send a URScript statement

URScript uses a **different connection** from RTDE. Start with a non-motion statement in URSim:

```java
import com.javiergs.rtde4j.URScriptClient;

public class SendScript {
    public static void main(String[] args) throws Exception {
        try (URScriptClient script = URScriptClient.connect("192.168.0.10")) {
            script.sendCommand("textmsg(\"Hello from RTDE4J\")");
        }
    }
}
```

`sendCommand` accepts **one printable-ASCII line**. Successful return means the command bytes were written to the socket; it does **not** confirm acceptance, execution, or completion. Inspect the simulator's logs or interface to determine what happened.

## 6. Experiment D — explore predefined movement APIs (URSim only)

The library provides:

| Method | URScript operation | Notes |
| --- | --- | --- |
| `moveJoints(joints, acceleration, velocity)` | `movej` | Joint angles in radians |
| `moveTool(tcpPose, acceleration, velocity)` | `movel` | TCP position in meters; rotation vector in radians |
| `moveToPose(pose, acceleration, velocity)` | `movel` | Uses `CartesianData` |
| `moveHomePosition()` | `movej` | **UR3 e-Series example preset only**; not a universally safe home |
| `jogMoveContinuous(speeds, acceleration, duration)` | `speedj` | Bounded segment; requires external hold-to-run/dead-man control for repeated use |
| `stopJogging(deceleration)` | `stopj` | Decelerated stop; **not** an emergency stop |

Do **not** assume that sending a command means motion has completed. Observe state through RTDE and validate target positions, collision clearance, robot model, and operating mode in the simulator before experimenting with movement. The home preset is specific to the original **UR3e example**.

## 7. Record your experiments

For each experiment, document:

- URSim version, network setup, Java version, and Maven dependency version.
- Which connection and port you used (`30004` for RTDE, `30002` for URScript).
- RTDE fields and units observed (`actual_q`, `actual_TCP_pose`).
- URScript statements or predefined methods attempted.
- What worked, what did not, and error messages or limitations.
- Whether results were observed **in simulation**; do not claim hardware validation.

## 8. Suggest documentation improvements

If instructions are unclear or an example needs correction, fork this **public** repository, edit the guide or add an example, and open a pull request. Describe how you tested your change. Library implementation changes belong in the private project and require separate instructor review.

> [!NOTE]
> The current public README predates some implemented source features. This guide reflects the current library source API, but availability in the **published Maven artifact** and compatibility with a particular URSim installation must be verified before treating these examples as tested end-to-end.
