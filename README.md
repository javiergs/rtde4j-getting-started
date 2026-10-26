# RTDE4J — Getting Started

![Java 21+](https://img.shields.io/badge/Java-21%2B-blue)
![Version 1.0.0](https://img.shields.io/badge/version-1.0.0-green)
[![Java CI](https://github.com/jgs-lab/api-rtde4j/actions/workflows/build.yml/badge.svg)](https://github.com/jgs-lab/api-rtde4j/actions/workflows/build.yml)

**RTDE4J** is a Java library for communicating with Universal Robots controllers using the **Real-Time Data Exchange (RTDE)** protocol. This repository hosts the **getting-started documentation and generated JavaDoc**. Library source code and development take place in [jgs-lab/api-rtde4j](https://github.com/jgs-lab/api-rtde4j).

> **Development status:** The core `RtdeClient` currently supports RTDE packet framing and querying the controller software version. Full recipe negotiation, continuous input/output streaming, and robot-motion commands are **not implemented** by this client. The repository also contains migrated robot-state and kinematics classes; do not assume they are integrated into the core client or hardware-validated.

## Requirements

- **Java 21+** and Maven
- A Universal Robots controller or simulator accessible on the network
- RTDE service on TCP port **30004** (default)

## Install

RTDE4J uses the Maven coordinates `com.javiergs:rtde4j:1.0.0`. **Check artifact availability before using this dependency**; the version in the source project does not itself guarantee publication to Maven Central.

```xml
<dependency>
    <groupId>com.javiergs</groupId>
    <artifactId>rtde4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

Until a published artifact is available, clone the [library repository](https://github.com/jgs-lab/api-rtde4j) and build it locally:

```bash
git clone https://github.com/jgs-lab/api-rtde4j.git
cd api-rtde4j
mvn verify
mvn install
```

## First connection: read controller version

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

## What's in the library?

| Component | Classes | Purpose |
| --- | --- | --- |
| RTDE protocol | `RtdeClient`, `RtdePacket` | TCP connection, binary packet encoding/decoding, controller-version query |
| Robot data | `RobotState`, `JointsData`, `CartesianData`, `ModeData`, `CalibrationData`, `KinematicsData` | Represent robot state, joints, poses, operating mode, and calibration |
| Geometry | `Vector3`, `Matrix3x3`, `Matrix4` | Vector and transformation-matrix calculations |
| Legacy/migrated client | `RTDEClient` | Separate implementation migrated from TULIP; not interchangeable with `RtdeClient` |

The two client classes have different APIs and maturity levels. **Use `RtdeClient` for the introductory connection example**; consult the [JavaDoc](docs/javadoc/index.html) and source before using `RTDEClient`.

## API documentation

- **[Generated JavaDoc](docs/javadoc/index.html)** — browsable reference for the Java packages and public API.
- **[RTDE4J source and issues](https://github.com/jgs-lab/api-rtde4j)** — implementation, tests, and contributions.
- **[Universal Robots RTDE protocol guide](https://docs.universal-robots.com/tutorials/communication-protocol-tutorials/rtde-guide.html)** — protocol behavior and message definitions.

The JavaDoc in `docs/javadoc/` is **automatically published** from the library's `main` branch by GitHub Actions. Edit JavaDoc comments in the source repository, not the generated HTML here.

## Examples and guides

This repository currently contains the generated JavaDoc and this getting-started guide; **standalone Maven example projects have not yet been added**. The library repository includes an example under `src/test/java/com/javiergs/rtde4j/Example.java` and offline packet tests.

## Safety and scope

RTDE connects to industrial equipment. Test connectivity and data handling with a simulator or a safely configured controller before using real hardware. Do not use experimental software to command robot motion without appropriate risk assessment and safety controls.

RTDE4J is an independent project; it is **not an official Universal Robots SDK** and is not endorsed by Universal Robots.
