# Set up URSim with Docker

Use **Universal Robots URSim** to practice with a simulated **UR3e** before connecting to physical hardware. This guide provides a shared setup for RTDE4J examples and other Java robotics applications.

## Prerequisites

- Docker Desktop (or a compatible Docker Engine), running.
- A terminal and a web browser.
- Java 21+ and Maven if you plan to run the RTDE4J examples.

On Apple Silicon or other ARM systems, the image may require x86-64 emulation, which can be slower. Ensure Docker has sufficient memory.

## Start the simulator

Run this command in a terminal:

```bash
docker run --rm -it \
  --platform linux/amd64 \
  --name ursim-ur3e \
  -e ROBOT_MODEL=UR3e \
  -p 127.0.0.1:6080:6080 \
  -p 127.0.0.1:30002:30002 \
  -p 127.0.0.1:30004:30004 \
  universalrobots/ursim_e-series:latest
```

The published ports are bound to **localhost** so they are not exposed to other machines. This setup is for a Java application running on the same host as Docker. If the Java application runs in another container or computer, networking will differ; avoid exposing robot-control ports to untrusted networks.

The simulator may take time to initialize. Keep this terminal open while using URSim. Stop the container with **Ctrl+C**. The `--rm` flag removes the container when it stops, so changes inside it are not retained.

## Open the robot interface

Open **http://localhost:6080/vnc.html** in your browser. The host port is **6080** because the command maps `6080:6080`; if you change the host-side port, change the browser URL too.

In PolyScope, initialize the simulated controller and power on the robot as needed. Confirm that the simulator is ready before connecting Java. Interface labels and initialization steps may vary by URSim version.

## Connect from Java

For a Java application running on the Docker host, use **127.0.0.1** (or **localhost**).

| Interface | TCP port | Use |
| --- | --- | --- |
| RTDE | `30004` | Read controller information, joint state, and TCP pose |
| URScript secondary interface | `30002` | Send URScript statements and motion commands |

The primary client interface (`30001`) is **not** exposed by this minimal setup. Add it only when your application requires it, preferably with a localhost-only binding (`-p 127.0.0.1:30001:30001`).

Continue with the [Student Quick Start](student-quick-start.md) to run RTDE4J's Java examples using the simulator.

## Troubleshooting

- **Browser does not load:** Confirm Docker is running, wait for startup, and verify the host port mapping. If port 6080 is already in use, map another host port (for example `-p 127.0.0.1:6081:6080`) and open `http://localhost:6081/vnc.html`.
- **Java cannot connect:** Check the simulator's readiness and the port mapping. `localhost` only works from the same host; a Java container has its own network namespace.
- **Robot appears powered off:** Initialize/power on the simulated controller through PolyScope.
- **Slow startup on ARM:** x86-64 emulation can be slower; wait for startup and check Docker resources.
- **Restart loses settings:** The `--rm` container is ephemeral; use a separately documented persistence strategy if you need saved simulator state.

## Safety and references

**Use URSim for learning and initial validation.** Sending URScript may move a physical robot if you connect to real hardware. This guide does not provide physical robot safety controls, motion verification, or emergency-stop functionality. Do not apply example poses to hardware without a risk assessment and verified safeguards.

- [Universal Robots URSim Docker image](https://hub.docker.com/r/universalrobots/ursim_e-series)
- [Universal Robots RTDE guide](https://docs.universal-robots.com/tutorials/communication-protocol-tutorials/rtde-guide.html)
