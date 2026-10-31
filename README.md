# Getting Started with RTDE4J

![Java 21+](https://img.shields.io/badge/Java-21%2B-blue)
![Version 1.0.0](https://img.shields.io/badge/version-1.0.0-green)
[![Java CI](https://github.com/javiergs/rtde4j-getting-started/actions/workflows/java-ci.yml/badge.svg)](https://github.com/javiergs/rtde4j-getting-started/actions/workflows/java-ci.yml)

**RTDE4J** is a Java library for communicating with Universal Robots controllers. Use this repository to learn RTDE controller-version queries and explore robot-state concepts using Java 21, Maven, and URSim. The library is published on Maven Central.

<img width="1672" height="941" alt="RTDE4J overview illustration" src="https://github.com/user-attachments/assets/87dd8116-1593-484c-bd17-e4840d542ae5" />

> [!NOTE]
> **Development status:** The core `RtdeClient` currently supports RTDE packet framing and querying the controller software version. Full recipe negotiation, continuous input/output streaming, and robot-motion commands are **not implemented** by this client. The library includes robot-state and kinematics classes that are not necessarily integrated into the core client or validated on hardware.

## Requirements

- **Java 21+** and Maven
- A Universal Robots controller or simulator accessible on the network
- RTDE service on TCP port **30004** (default)

## Install with Maven

RTDE4J uses the Maven coordinates `com.javiergs:rtde4j:1.0.0`. Version **1.0.0 is published on Maven Central** and can be used directly as a Maven dependency.

```xml
<dependency>
    <groupId>com.javiergs</groupId>
    <artifactId>rtde4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

Maven downloads RTDE4J automatically when you build your project. You do **not** need to build the library from source.

**New student?** Follow the [Student Quick Start](docs/student-quick-start.md) for a simulator-first walkthrough and runnable Java examples. The examples use the published Maven dependency.

## Quick start

### First connection: read controller version

The minimal `RtdeClient` opens a TCP connection, requests the controller version, and closes the connection automatically.

```java
import com.javiergs.rtde4j.RtdeClient;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        try (RtdeClient client = RtdeClient.connect("192.168.0.10")) {
            RtdeClient.ControllerVersion version = client.getControllerVersion();
            System.out.println("Controller version: " + version);
        }
    }
}
```

Replace `192.168.0.10` with your robot or simulator address. You can also specify a port using `RtdeClient.connect(host, port)`. The controller must be reachable, and network or protocol errors may raise `IOException`.

## Library components

| Component | Classes | Purpose |
| --- | --- | --- |
| RTDE protocol | `RtdeClient`, `RtdePacket` | TCP connection, binary packet encoding/decoding, controller-version query |
| Robot data | `RobotState`, `JointsData`, `CartesianData`, `ModeData`, `CalibrationData`, `KinematicsData` | Represent robot state, joints, poses, operating mode, and calibration |
| Geometry | `Vector3`, `Matrix3x3`, `Matrix4` | Vector and transformation-matrix calculations |
| Legacy/migrated client | `RTDEClient` | Separate implementation migrated from TULIP; not interchangeable with `RtdeClient` |

The two client classes have different APIs and maturity levels. **Use `RtdeClient` for the introductory connection example**; consult the [public JavaDoc](docs/javadoc/index.html) before using `RTDEClient`.

## API documentation

- **[Generated JavaDoc](docs/javadoc/index.html)** — browsable reference for the Java packages and public API.
- **[Universal Robots RTDE protocol guide](https://docs.universal-robots.com/tutorials/communication-protocol-tutorials/rtde-guide.html)** — protocol behavior and message definitions.

The generated JavaDoc is published automatically; its HTML is not intended for manual editing.

## Examples and guides

This public repository includes a standalone Maven project and three runnable examples in `src/main/java/examples/`: `ReadVersion`, `ListenToRobot`, and `SendMessage` (URSim only). Start with the [Student Quick Start](docs/student-quick-start.md). 

## Safety and scope

RTDE connects to industrial equipment. Test connectivity and data handling with a simulator or a safely configured controller before using real hardware. Do not use experimental software to command robot motion without appropriate risk assessment and safety controls.

RTDE4J is an independent project; it is **not an official Universal Robots SDK** and is not endorsed by Universal Robots.

## Troubleshooting

- **Connection refused or timed out:** Confirm URSim or the controller is running and reachable at the configured IP address.
- **Wrong port:** RTDE normally uses TCP port `30004`; URScript commands use a different interface, normally port `30002`.
- **Build failure:** Confirm `mvn -version` reports Java 21 and Maven can reach Maven Central.
- **No streaming data:** The introductory `RtdeClient` does not yet support continuous recipe-based RTDE streaming; see the development-status note above.

For contribution instructions, see the repository's GitHub **Contributing** guidance.
