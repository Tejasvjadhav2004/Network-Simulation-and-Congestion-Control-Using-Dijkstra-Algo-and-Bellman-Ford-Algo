# Network Simulation and Congestion Control Project

## Overview

This Java-based project pioneers network creation using robust graph data structures and diverse topologies. It employs advanced algorithms like Dijkstra's and Bellman-Ford for optimal pathfinding. Addressing the congestion control problem, it implements the token bucket technique for precise traffic shaping and deploys Random Early Detection (RED) for proactive queue management. These techniques enhance the performance and stability of the network, which are the main aims of any computer network system.

## Features

- **Network Creation**: Create networks manually or using predefined topologies (Bus, Star, Ring, Mesh, Tree).
- **Pathfinding Algorithms**: Utilize Dijkstra's and Bellman-Ford algorithms for finding the shortest path.
- **Congestion Control**:
  - **Traffic Shaping using Token Bucket Technique**: Regulates the rate at which packets are sent into the network to prevent congestion.
  - **Congestion Avoidance using Random Early Detection (RED)**: Proactively manages the queue to avoid congestion before it becomes problematic.
- **Performance Metrics**: Collect and display metrics such as delay, utilization, path efficiency, packet delivery ratio, network load, throughput, latency, and total packets dropped.

## Installation

### Prerequisites

- Java Development Kit (JDK) 8 or higher
- An IDE or text editor of your choice (e.g., IntelliJ IDEA, Eclipse, VSCode)

### Clone the Repository

1. Clone the repository using Git:
   ```sh
   git clone https://github.com/Tejasvjadhav2004/Network-Simulation-and-Congestion-Control-Using-Dijkstra-Algo-and-Bellman-Ford-Algo.git

- ## Usage

1. **Creating a Network**:
   - Run `NetworkSimulation.java`.
   - Follow the prompts to create a network manually or select a predefined topology.
   - Choose between Dijkstra's or Bellman-Ford algorithm for pathfinding.
   - Specify the number of packets and their source and destination.

2. **Simulating Traffic**:
   - After creating the network, start the traffic simulation.
   - The simulation will run and collect performance metrics.

3. **Showing Statistics**:
   - From the main menu, choose to display statistics either router-wise or for the whole network.
   - Metrics include total execution time, total memory usage, throughput, latency, and total packets dropped.

## Congestion Control

### Traffic Shaping using Token Bucket Technique

The token bucket technique is used for controlling the amount of data that is injected into the network. It allows for bursts of traffic while regulating the overall rate of traffic flow. Tokens are added to the bucket at a fixed rate, and for a packet to be sent, it must capture a token. This ensures that the network does not become overloaded with excessive traffic, thereby reducing congestion.

### Congestion Avoidance using Random Early Detection (RED)

Random Early Detection (RED) is a proactive queue management algorithm that helps avoid congestion before it becomes severe. It works by monitoring the average queue size and dropping packets probabilistically when congestion is detected. By doing so, it prevents the queue from becoming too full and helps maintain optimal network performance. RED ensures a balance between throughput and delay, improving overall network stability and performance.

## Classes and Components

- **Network**: Manages the network creation and operations.
- **Router**: Represents nodes in the network, capable of handling packets.
- **Packet**: Represents the data being transferred across the network.
- **TokenBucket**: Implements the token bucket algorithm for congestion control.
- **REDRouter**: Implements RED for proactive queue management.
- **Dijkstra**: Implements Dijkstra's shortest path algorithm.
- **BellmanFord**: Implements Bellman-Ford's shortest path algorithm.
