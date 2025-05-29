# Distributed Snapshot Algorithms Simulation

## Table of Contents

1.  [Overview](#overview)
2.  [Features](#features)
3.  [Implemented Snapshot Algorithms](#implemented-snapshot-algorithms)
    *   [Acharya-Badrinath (AB)](#acharya-badrinath-ab)
    *   [Alagar-Venkatesan (AV)](#alagar-venkatesan-av)
    *   [Coordinated Checkpointing (CC)](#coordinated-checkpointing-cc)
4.  [Running the Examples](#running-the-examples)

## Overview

This is a simulation framework designed for the implementation, execution, and analysis of distributed snapshot algorithms. It offers a practical environment to explore the challenges of capturing consistent global states within a distributed computing context.

The framework simulates a network of servents. Each servent manages a local resource. These servents can engage in resource exchanges via transactions. The central aim is to accurately record the system's global state which includes the local state of each servent and the state of messages currently in transit at a designated moment.

## Features

*   **Multiple Snapshot Algorithm Support:** Implements Acharya-Badrinath (AB), Alagar-Venkatesan (AV), and Coordinated Checkpointing (CC) algorithms.
*   **Configurable Distributed System Simulation:**
    *   Variable number of servents.
    *   Customizable network topology.
*   **Simulated Resource Management:** `BitcakeManager` components track and update the local resource state for each servent.
*   **Dynamic Transaction Simulation:** Servents can perform transaction bursts to generate network traffic and state changes.
*   **Flexible Communication Models:**
    *   **FIFO:** Ensures messages between any two servents are delivered in the order they were sent.
    *   **Causal Broadcast:** (For non-FIFO configurations) Guarantees that messages are delivered in an order consistent with their causal dependencies using Vector Clocks.
*   **Per-Servent Command-Line Interface (CLI):** Enables interactive or scripted control over each servent
*   **Pre-configured Example Scenarios:** Includes ready-to-run examples for each implemented snapshot algorithm, with corresponding input scripts and system property files.

## Implemented Snapshot Algorithms

The framework provides implementations for the following distributed snapshot algorithms:

### Acharya-Badrinath (AB)

*   **Suited For:** Systems with non-FIFO communication channels.
*   **Algorithm Flow:**
    - **Initiation:** A servent (typically triggered by a `bitcake_info` command) initiates the snapshot.
    -  **Local State Recording:** The initiator immediately records its current local state and its current vector clock. It also records the set of messages it has sent and received up to this point.
    -  **Snapshot Request Broadcast:** The initiator sends an `AB_SNAPSHOT_REQUEST` message to all its neighbors. This message includes the initiator's current vector clock.
    -  **Receiving a Snapshot Request:**
        *   When a servent `Pj` receives an `AB_SNAPSHOT_REQUEST` from `Pi` for the first time:
            *   It records its local state.
            *   It records all messages it has sent to `Pi` *before* receiving the request from `Pi`.
            *   It records all messages it has received from `Pi` *before* sending its response back to `Pi`.
            *   It sends an `AB_SNAPSHOT_RESPONSE` message back to the initiator (`Pi`). This response includes its recorded local state, its list of sent messages, and its list of received messages that pertain to channels with the initiator or other processes from which it has already received a request.
            *   It then broadcasts the `AB_SNAPSHOT_REQUEST` to its other neighbors from whom it hasn't yet received a request for this snapshot instance.
    -  **Receiving a Snapshot Response:**
        *   When the initiator (or any servent participating) receives an `AB_SNAPSHOT_RESPONSE`, it stores the reported state.
    -  **Snapshot Completion & Aggregation (at Initiator):**
        *   The snapshot is considered complete at the initiator when it has received `AB_SNAPSHOT_RESPONSE` messages from all other servents in the system.
        *   The initiator then aggregates all collected local states and message logs.
        *   **Consistency Check:** To determine the global state, the initiator analyzes the sent and received message logs. A message `m` sent from `Pi` to `Pj` is considered "in-transit" if `Pi` recorded sending `m` but `Pj` did not record receiving `m` in its collected snapshot information.
        *   The total bitcakes in the system are the sum of all local bitcake amounts plus the sum of bitcakes in all identified in-transit transaction messages.
*   **Example Directory:** `ab-snapshot-example/`

### Alagar-Venkatesan (AV)

*   **Suited For:** Systems with non-FIFO communication channels.
*   **Algorithm Flow:**
    -  **Initiation:** A servent initiator decides to take a snapshot (via `bitcake_info`).
        *   It records its local state.
        *   It initializes its vector clock copy with its current vector clock.
        *   It sends an `AV_MARKER` message to all its neighbors. This message contains the initiator's current vector clock.
    -  **Receiving an `AV_MARKER` Message:**
        *   When a servent `Pj` receives an `AV_MARKER` message from `Pi`:
            *   **First Marker:** If this is the first `AV_MARKER` `Pj` receives for this snapshot instance:
                *   It records its local state.
                *   It sets its vector clock copy to the vector clock received in the `AV_MARKER` message from `Pi`.
                *   It starts recording messages on its incoming channels based on the vector clock copy (messages whose sender's vector clock timestamp for the initiator is less than or equal to the vector clock copy timestamp for the initiator are considered part of the pre-snapshot channel state).
                *   It forwards `AV_MARKER` messages (with its current vector clock) to all its *other* neighbors.
            *   **Subsequent Markers:** If `Pj` has already recorded its state for this snapshot and receives another `AV_MARKER` from `Pk`:
                *   It stops recording messages on the incoming channel from `Pk`. The messages already received on this channel after `Pj` recorded its state but before this marker from `Pk` arrived are considered in-transit from `Pk` to `Pj`.
    -  **Sending `AV_DONE` Message:**
        *   A servent `Pj` sends an `AV_DONE` message to the initiator once it has received `AV_MARKER` messages from all its neighbors (signifying it has processed all relevant pre-snapshot messages on its incoming channels).
    -  **Snapshot Completion & Termination:**
        *   The initiator knows the snapshot collection phase is complete when it has received `AV_DONE` messages from all other participating servents.
        *   The initiator then sends an `AV_TERMINATE` message to all servents.
    -  **Receiving an `AV_TERMINATE` Message:**
        *   Upon receiving `AV_TERMINATE`, each servent:
            *   Finalizes its snapshot information: its recorded local state, the sum of bitcakes in messages recorded on its input channels (messages that "crossed the cut"), and potentially information about messages it sent that might be in transit.
            *   The designated collector can then aggregate the local states and in-transit message values to determine the global state.
*   **Example Directory:** `av-snapshot-example/`

### Coordinated Checkpointing (CC)

*   **Suited For:** Systems with FIFO channels.
*   **Algorithm Flow:**
    -  **Initiation:** An initiator servent (triggered by `bitcake_info`) decides to start a snapshot.
        *   It sends a `CC_SNAPSHOT_REQUEST` message to itself to begin its local snapshot process.
    -  **Handling `CC_SNAPSHOT_REQUEST`:**
        *   When a servent `Pi` receives a `CC_SNAPSHOT_REQUEST` (either from itself or a neighbor):
            *   **First Request:** If `Pi` is not already in snapshot mode for this snapshot instance:
                -  It enters snapshot mode.
                -  It records its local state.
                -  It sends its recorded state in a `CC_SNAPSHOT_RESPONSE` message directly to the initiator.
                -  It forwards the `CC_SNAPSHOT_REQUEST` message to all its neighbors.
            *   **Subsequent Requests:** If `Pi` is already in snapshot mode, it ignores further `CC_SNAPSHOT_REQUEST` messages for this instance.
        *   **Message Handling in Snapshot Mode:** While a servent is in snapshot mode, incoming application messages (`TRANSACTION` messages) are typically queued and not processed immediately.
    -  **Receiving `CC_SNAPSHOT_RESPONSE`:**
        *   The initiator collects `CC_SNAPSHOT_RESPONSE` messages from all other servents. Each response contains the local state of the responding servent.
    -  **Snapshot Completion (at Initiator):**
        *   The snapshot is considered complete when the initiator has received responses from all servents in the system.
        *   At this point, the initiator has all local states. Since it's coordinated and assumes FIFO, the state of channels is implicitly captured by ensuring no messages are lost or duplicated across the checkpointing process. All messages sent before a checkpoint are either received before the receiver's checkpoint or are considered "in-transit" (though in simpler CC, in-transit messages are not explicitly tracked as part of the global state if processes block communication during checkpointing or if channels are flushed).
    -  **Resuming Operation:**
        *   The initiator sends a `CC_RESUME` message to all servents.
    -  **Receiving `CC_RESUME`:**
        *   Upon receiving a `CC_RESUME` message, a servent exits snapshot mode.
        *   It then processes any application messages that were queued while it was in snapshot mode.
*   **Example Directory:** `cc-snapshot-example/`

## System Configuration (servent_list.properties)
This file defines the parameters for the distributed system simulation:

- ```servent_count=<N>```: The total number of servents in the system.
- ```clique=<true|false>```: If true, all servents are considered neighbors of each other. If false, neighbor relationships must be explicitly defined.
- ```fifo=<true|false>```: If true, messages are delivered in FIFO order. If false, causal broadcast is used.
- ```snapshot=<ab|av|cc|none>```: Specifies the snapshot algorithm to be employed.
- ```serventX.port=<port_number>```: The listening port for servent X (e.g., servent0.port=1100).
- ```serventX.neighbors=<id1,id2,...>```: A comma-separated list of servent IDs that are neighbors of servent X. This is only used if clique=false.

## Running the Examples

The simulation is initiated through the `MultipleServentStarter` class. This class is responsible for launching and managing individual servent processes based on a selected configuration.

1.  **Select an Example Scenario:**
    *   Open the file: `MultipleServentStarter.java`.
    *   Within the `main` method, you will find lines dedicated to starting different example scenarios:
        ```java
        public static void main(String[] args) {
            // To run the Acharya-Badrinath example:
            // startServentTest("ab-snapshot-example");

            // To run the Alagar-Venkatesan example:
            startServentTest("av-snapshot-example");

            // To run the Coordinated Checkpointing example:
            // startServentTest("cc-snapshot-example");
        }
        ```
    *   **Activate the desired example:** Uncomment the `startServentTest("example-name");` line for the scenario you wish to run (e.g., `startServentTest("av-snapshot-example");`).
    *   **Deactivate other examples:** Ensure all other `startServentTest(...)` lines are commented out to prevent multiple scenarios from running simultaneously.
    *   The string argument (e.g., `"av-snapshot-example"`) directly corresponds to the directory name within the project that holds the configuration files (`servent_list.properties`, input scripts, etc.) for that specific snapshot algorithm demonstration.

2. **Execute `MultipleServentStarter`:**
    - Open your terminal (Linux/macOS) or command prompt (Windows).
    - Navigate to the root directory of the `KIDS-P2` project (the directory containing `build.gradle.kts`, `gradlew`, etc.).
    - Execute the following command:

        ```bash
        java -cp build/classes/java/main com.kids.app.MultipleServentStarter
        ```

        On Windows, you might prefer using backslashes for the classpath:

        ```bash
        java -cp build\classes\java\main com.kids.app.MultipleServentStarter
        ```

3.  **Monitoring and Interaction During Execution:**
    *   **Servent-Specific Log Files:**
        The detailed activity of each individual servent is not printed to the main console but is redirected to log files for clarity and easier analysis. These files are located within the directory of the example you are running:
        *   **Standard Output:** `EXAMPLE_DIR/output/serventX_out.txt`
            *   Contains timestamped logs of messages sent/received, state changes, snapshot actions, and CLI command executions for servent `X`.
            *   Example: For `av-snapshot-example` and servent `0`, check `av-snapshot-example/output/servent0_out.txt`.
        *   **Standard Error:** `EXAMPLE_DIR/error/serventX_err.txt`
            *   Logs any errors, exceptions, or warning messages generated by servent `X`.
            *   Example: `av-snapshot-example/error/servent0_err.txt`.
    *   **Automatic Command Execution:** Each servent automatically reads and executes the commands listed in its corresponding input file (e.g., `av-snapshot-example/input/servent0_in.txt`).

By following these steps, you can run any of the provided example scenarios and observe the behavior of the different distributed snapshot algorithms in action. Remember to check the respective `output` and `error` directories for detailed logs from each servent.
