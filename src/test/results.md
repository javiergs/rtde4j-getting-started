# RTDE4J Test Results

Results of the JUnit tests in `src/test/java/examples`, run on 2026-11-06. All results were observed in simulation only, not on the physical arm.

## Setup

- Robot: UR3e (URSim e-Series in Docker, container `ur3e_container`), controller version 5.26.1
- Java 21.0.8, Maven 3.9.9, RTDE4J 1.0.0 from Maven Central, JUnit 5.11.4
- Windows 11, URSim on 127.0.0.1
- Ports: 30004 for RTDE (reading robot state), 30002 for URScript (sending commands)

## How to Run

From `C:\Users\shiha\Downloads\rtde4j-testing\rtde4j-getting-started`, with URSim running and the robot powered on with brakes released:

All tests except the reconnect tests:

```powershell
..\tools\apache-maven-3.9.9\bin\mvn.cmd test
```

Only the parser-specific tests:

```powershell
..\tools\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=ParserBehaviorTest"
```

The reconnect tests, which stop and restart the URSim container and leave the robot powered off:

```powershell
..\tools\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=ReconnectTest" "-Dreconnect=true"
```

## Summary

35 tests, all passed: 32 in the normal run and 3 in the reconnect run.

RTDE4J did everything that was tested. It reads the same joint angles as the ROVER parser's own RtdeClient at the same speed, every movement method lands exactly on its target, it handles the way the parser sends commands, and its monitor recovers from a simulator restart. The repo README says streaming and motion are not implemented, but both work.

## ConnectionErrorTest

Checks how RTDE4J reports connection failures. Does not need URSim.

refusedRtdeConnectionFailsFast: connects to a port with nothing listening on it.
Result: passed. Failed straight away with `ConnectException: Connection refused`.

refusedUrscriptConnectionFailsFast: same check for the URScript client.
Result: passed. Failed straight away with `ConnectException: Connection refused`.

unreachableHostDoesNotHang: connects to an address that never answers, with a 10 second limit.
Result: passed. Gave up with `SocketTimeoutException: Connect timed out` instead of hanging.

invalidPortIsRejected: connects to port 70000, which doesn't exist.
Result: passed. Rejected before connecting with `IllegalArgumentException: port out of range:70000`.

## ReadVersionTest (Experiment A)

readsControllerVersion: reads the controller version over RTDE.
Result: passed. Version 5.26.1, which is an e-Series controller.

negotiatesProtocolVersion: does the RTDE protocol handshake.
Result: passed. The controller accepted protocol version 2.

## ListenToRobotTest (Experiment B)

samplesHaveSixJointsAndSixPoseValues: reads one sample and checks its size and units.
Result: passed. 6 joint angles in radians and 6 tool pose values (x, y, z in meters, then rotation in radians).
actual_q: [-1.6007, -1.7271, -2.2030, -0.8080, 1.5951, -0.0310]
actual_TCP_pose: [-0.1440, -0.4356, 0.2020, -0.0012, 3.1163, 0.0389]

streamsAtRequestedFrequency: times 20 samples at 10 Hz.
Result: passed. Took 2.09 s, about the expected 2 s.

supportsParserRateOf125Hz: times 125 samples at 125 Hz, the rate the ROVER parser uses.
Result: passed. Took 1.05 s, so RTDE4J keeps up with the parser's rate.

clientListenerIsQuietWhileArmIsStill: counts RtdeClient listener events for 2 s with the arm still.
Result: passed. 0 events. RtdeClient only fires an event when a value changes, including robot_sample.

monitorListenerFiresEverySample: counts ReconnectingRtdeMonitor robot_sample events with the arm still.
Result: passed. 20 events arrived at 10 Hz. Unlike RtdeClient, the monitor fires on every sample, changed or not.

readingBeforeStartIsRejected: reads a sample before starting the stream.
Result: passed. Rejected with `IllegalStateException: Stream is not started`.

## SendMessageTest (Experiment C)

sendsTextMessage: sends `textmsg("Hello from RTDE4J test")` over URScript.
Result: passed. The message was sent and the client stayed connected.

rejectsBlankCommand: sends a blank command.
Result: passed. Rejected with `IllegalArgumentException: Expected one nonblank URScript line`.

rejectsMultipleLines: sends two commands in one call.
Result: passed. Rejected with the same message, so RTDE4J only sends one line at a time.

rejectsJogLongerThanLimit: asks for a 1.0 s jog.
Result: passed. Rejected with `duration must be in (0, 0.5] seconds`.

rejectsJogFasterThanLimit: asks for a 0.5 rad/s jog.
Result: passed. Rejected with `joint speed exceeds MAX_JOG_SPEED`, which is 0.25 rad/s.

sendingAfterCloseIsRejected: sends a command after closing the client.
Result: passed. Rejected with `IOException: URScript client is closed`.

## MoveArmTest (Experiment D)

Each test moves the arm, waits until it has stopped, reads where it settled over RTDE, then returns it to where it started. Tolerance is 0.001 rad for joints and 0.001 for pose values.

moveJointsReachesTarget: turns the base joint by 0.2 rad with moveJoints (movej).
Result: passed. Settled exactly on the target (off by about 2e-16 rad, which is rounding).

moveToolReachesPose: raises the tool 5 cm with moveTool (movel).
Result: passed. Settled exactly on the target pose.

moveToPoseReachesPose: moves the tool 5 cm sideways with moveToPose (movel).
Result: passed. Settled exactly on the target pose.

moveHomePositionReachesPreset: moves to RTDE4J's UR3e home preset [0, -1.57, 0, -1.57, 0, 0].
Result: passed. Settled exactly on the preset.

clientListenerFiresWhileArmMoves: counts RtdeClient actual_q events during a 0.2 rad move at 10 Hz.
Result: passed. 12 events, so the listener reports joint changes while the arm moves.

jogMovesThenStops: jogs the base at 0.2 rad/s for 0.5 s, then calls stopJogging.
Result: passed. The base moved 0.084 rad and didn't drift at all after the stop.

## CompareClientsTest

Compares RTDE4J with the ROVER parser's own RtdeClient (copied into `src/test/java/rover`).

bothClientsReadSameJointsAndPose: reads actual_q and actual_TCP_pose with both clients at the same time.
Result: passed. Both returned exactly the same values, to the last digit.

bothClientsKeepUpAt125Hz: times 125 reads at 125 Hz with each client.
Result: passed. RTDE4J took 1.037 s and the rover client took 1.034 s, so they are the same speed.

## ParserBehaviorTest

Checks RTDE4J against how the ROVER parser drives the arm: movej with a=1.2 and v=0.5, sent again whenever a new MQTT target arrives.

moveJointsAtParserSpeed: turns the base 0.3 rad with moveJoints at the parser's a=1.2, v=0.5.
Result: passed. Settled exactly on the target in 1.73 s (about 0.5 s of that is the test confirming the arm stopped).

newTargetReplacesMoveInProgress: sends a +0.6 rad target, then 400 ms later a +0.1 rad target, like a new MQTT command arriving mid-move.
Result: passed. The arm switched to the new target. It never went past +0.1 rad and settled exactly on it, so a new moveJoints replaces the one in progress instead of finishing it first.

streamOfTargetsEndsOnLastTarget: sends 20 small targets 50 ms apart (0.3 rad total), like an app dragging a slider.
Result: passed. Settled exactly on the last target, 2.86 s after the first command.

## ReconnectTest

Stops and restarts the URSim container. Each test ends with the simulator running again.

monitorReconnectsAfterSimulatorRestart: runs ReconnectingRtdeMonitor (60 retries, 2 s apart) through a stop and start.
Result: passed. The states went CONNECTING, CONNECTED, RETRYING, CONNECTED. It noticed the stop straight away (last failure `Connection refused`), reconnected 1.4 s after the container started, and new samples were flowing again. After the restart robot_mode was 3 (powered off), so reading works again but the robot has to be powered on before it will move.

monitorStopsAfterRetriesRunOut: runs the monitor with only 3 retries 500 ms apart, then stops the simulator.
Result: passed. The states went CONNECTED, RETRYING, STOPPED once the retries ran out. It stayed STOPPED after the simulator came back, so a monitor that has given up does not restart on its own.

urscriptClientAfterSimulatorStop: keeps a URScriptClient open while the simulator stops, then keeps sending.
Result: passed. The first send after the stop did not report an error, so that command was lost without notice. The second send failed with `SocketException: An established connection was aborted`. isConnected still returned true after the failure. A new URScriptClient connected and sent normally after the restart.

During restarts the controller printed `SafetySetup has not been confirmed yet, therefore no data will be send yet` while it was starting up. It went away once the controller was ready.

## Comparison with the ROVER RtdeClient

Compares RTDE4J with the ROVER parser's own client (`app-desktop/ur-arm/ur-robot-arm-parser/parser-java`). That client is `RtdeClient.java` for reading, plus the raw URScript socket and reconnect loop in `Parser.java`.

The same in both:
- Reading actual_q and actual_TCP_pose returns exactly the same values.
- Both keep up with the parser's 125 Hz stream at the same speed.
- Both send movej over port 30002. RTDE4J's moveJoints(q, 1.2, 0.5) is the same command the parser builds by hand, and it handled the parser's pattern of sending a new target mid-move.
What RTDE4J has that the ROVER client doesn't:
- A URScript client with ready-made movement methods (moveJoints, moveTool, moveToPose, moveHomePosition, jogMoveContinuous, stopJogging). The parser writes movej strings to a plain socket.
- Checks on commands before they are sent: blank or multi-line commands, and jog speed and duration limits. The parser sends whatever it builds.
- Listeners and background monitoring, so a program can react to new samples instead of looping on receive().
- A built-in reconnecting monitor for the reading side. The ROVER client has none itself; the reconnect loop lives in Parser.java.
- A choice of stream frequency. The ROVER client is fixed at 125 Hz.
- It returns the controller version. The ROVER client asks for it but discards the answer.
- It is published on Maven Central, so another Java project only needs one dependency line instead of a copy of the source files.

What the ROVER client has that RTDE4J doesn't:
- Any RTDE field can be read by listing it in rtde_config.xml, such as robot_mode. RTDE4J only streams actual_q and actual_TCP_pose. The tests used the ROVER client to check that the robot was powered on for this reason.
- It prints the controller's warning and error messages, such as the SafetySetup message seen during restarts. RTDE4J printed nothing like this in these tests.
- The parser's reconnect loop retries forever. ReconnectingRtdeMonitor stops for good once its retries run out and has to be replaced with a new monitor.
- After a failed URScript send, the parser closes the socket and reconnects. URScriptClient has no reconnect, and isConnected still returns true after a failed send, so the program has to catch the error and create a new URScriptClient.

Differences to keep in mind when switching:
- RtdeClient listeners only fire when a value changes. ReconnectingRtdeMonitor fires on every sample.
- The ROVER client's receive() skips to the newest package. With RTDE4J the same thing comes from startMonitoring() plus getLatestSample(), while readSample() reads packages in order.
