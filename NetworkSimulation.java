import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;

// Router class 
class Router {
    private int id;
    private List<Edge> outgoingEdges;
    private Queue<Packet> packetQueue;
    protected static final int QUEUE_CAPACITY = 100;
    private long totalDelay;
    private int packetsForwarded;
    private int packetsDropped;
    private TokenBucket tokenBucket;

    public Router(int id, int bucketCapacity, int tokenRate) {
        this.id = id;
        this.outgoingEdges = new ArrayList<>();
        this.packetQueue = new LinkedList<>();
        this.totalDelay = 0;
        this.packetsForwarded = 0;
        this.packetsDropped = 0;
        this.tokenBucket = new TokenBucket(bucketCapacity, tokenRate);
    }

    public int getId() {
        return id;
    }

    public List<Edge> getOutgoingEdges() {
        return outgoingEdges;
    }

    public void addOutgoingEdge(Edge edge) {
        outgoingEdges.add(edge);
    }

    //function for enquing packets
    public boolean enqueuePacket(Packet packet) {
        if (packetQueue.size() < QUEUE_CAPACITY) {
            packetQueue.add(packet);
            return true;
        } else {
            System.out.println("Queue at router " + id + " is full. Dropping packet: " + packet);
            packetsDropped++;
            return false;
        }
    }
     //function for dequeing packets
    public Packet dequeuePacket() {
        return packetQueue.poll();
    }

    public Queue<Packet> getPacketQueue() {
        return packetQueue;
    }

    //main function for processing packets
    public void forwardPackets(Map<Router, Map<Router, Integer>> shortestPaths) {
        List<Packet> forwardedPackets = new ArrayList<>();
        for (Packet packet : packetQueue) {
            if (tokenBucket.tryConsume(1)) { // Consume one token per packet
                Router nextRouter = getNextRouter(packet.getDestination(), shortestPaths);
                if (nextRouter != null) {
                    System.out.println(
                            "Forwarding " + packet + " from router " + id + " to router " + nextRouter.getId());
                    if (nextRouter.enqueuePacket(packet)) {
                        forwardedPackets.add(packet);
                        packetsForwarded++;
                        totalDelay += calculateDelay(packet);
                    }
                }
            } else {
                System.out.println("Insufficient tokens at router " + id + " for packet: " + packet);
            }
        }
        packetQueue.removeAll(forwardedPackets);
    }
    //function for getting next router
    private Router getNextRouter(Router destination, Map<Router, Map<Router, Integer>> shortestPaths) {
        int minDist = Integer.MAX_VALUE;
        Router nextRouter = null;
        for (Edge edge : outgoingEdges) {
            Router neighbor = edge.getDestination();
            int dist = shortestPaths.get(this).get(destination);
            if (dist < minDist) {
                minDist = dist;
                nextRouter = neighbor;
            }
        }
        return nextRouter;
    }

    //metric calculations
    private long calculateDelay(Packet packet) {
        long propagationDelay = 10;
        long transmissionDelay = packet.getData().length() * 2;
        long processingDelay = 5;
        return propagationDelay + transmissionDelay + processingDelay;
    }

    public double calculateUtilization() {
        int totalPackets = packetsForwarded + packetsDropped;
        return totalPackets > 0 ? (double) packetsForwarded / totalPackets : 0.0;
    }

    public double calculatePathEfficiency() {
        double totalDist = 0;
        for (Edge edge : outgoingEdges) {
            totalDist += edge.getWeight();
        }
        return totalDist > 0 ? (double) 1 / totalDist : 0.0;
    }

    public double calculatePacketDeliveryRatio() {
        int totalPackets = packetsForwarded + packetsDropped;
        return totalPackets > 0 ? (double) packetsForwarded / totalPackets : 0.0;
    }

    public int calculateNetworkLoad() {
        return packetQueue.size();
    }

    public long getTotalDelay() {
        return totalDelay;
    }

    public int getPacketsForwarded() {
        return packetsForwarded;
    }

    public int getPacketsDropped() {
        return packetsDropped;
    }
}

// RedRouter class
class RedRouter extends Router {
    private double minThreshold;
    private double maxThreshold;
    private double dropProbability;
    private double averageQueueSize;
    private Random random;

    public RedRouter(int id, int bucketCapacity, int tokenRate, double minThreshold, double maxThreshold,
            double dropProbability, double averageQueueSize) {
        super(id, bucketCapacity, tokenRate);
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
        this.dropProbability = dropProbability;
        this.averageQueueSize = averageQueueSize;
        this.random = new Random();
    }

    @Override
    public boolean enqueuePacket(Packet packet) {
        if (getPacketQueue().size() < QUEUE_CAPACITY) {
            getPacketQueue().add(packet);
            return true;
        } else {
            double queueSize = getPacketQueue().size();
            double dropProbability = calculateDropProbability(queueSize);
            if (random.nextDouble() < dropProbability) {
                System.out.println("Packet dropped due to RED at router " + getId() + ": " + packet);
                return false;
            } else {
                return super.enqueuePacket(packet);
            }
        }
    }

    private double calculateDropProbability(double currentQueueSize) {
        if (currentQueueSize < minThreshold * averageQueueSize) {
            return 0.0;
        } else if (currentQueueSize < maxThreshold * averageQueueSize) {
            return dropProbability * (currentQueueSize - minThreshold * averageQueueSize)
                    / (averageQueueSize * (maxThreshold - minThreshold));
        } else {
            return dropProbability;
        }
    }
}

// Graph class 
class Graph {
    private Map<Integer, Router> routers;

    public Graph() {
        this.routers = new HashMap<>();
    }

    public void addRouter(int id, int bucketCapacity, int tokenRate) {
        routers.put(id, new Router(id, bucketCapacity, tokenRate));
    }

    public void addRedRouter(int id, int bucketCapacity, int tokenRate, double minThreshold, double maxThreshold,
            double dropProbability, double averageQueueSize) {
        routers.put(id, new RedRouter(id, bucketCapacity, tokenRate, minThreshold, maxThreshold, dropProbability,
                averageQueueSize));
    }

    public void addEdge(int sourceId, int destinationId, int weight) {
        Router source = routers.get(sourceId);
        Router destination = routers.get(destinationId);
        if (source == null || destination == null) {
            throw new IllegalArgumentException("Source or Destination router does not exist.");
        }
        Edge edge = new Edge(source, destination, weight);
        source.addOutgoingEdge(edge);
    }

    public Router getRouter(int id) {
        return routers.get(id);
    }

    public Collection<Router> getRouters() {
        return routers.values();
    }

    // Method to calculate shortest paths using Dijkstra's algorithm
    public Map<Router, Map<Router, Integer>> calculateShortestPaths() {
        Map<Router, Map<Router, Integer>> shortestPaths = new HashMap<>();
        for (Router router : routers.values()) {
            shortestPaths.put(router, calculateShortestPathFrom(router));
        }
        return shortestPaths;
    }

    //implementation of dijkstra algo
    private Map<Router, Integer> calculateShortestPathFrom(Router source) {
        Map<Router, Integer> distances = new HashMap<>();
        PriorityQueue<Router> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        for (Router router : routers.values()) {
            distances.put(router, Integer.MAX_VALUE);
        }
        distances.put(source, 0);
        queue.add(source);

        while (!queue.isEmpty()) {
            Router current = queue.poll();
            int currentDistance = distances.get(current);

            for (Edge edge : current.getOutgoingEdges()) {
                Router neighbor = edge.getDestination();
                int newDistance = currentDistance + edge.getWeight();
                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    queue.add(neighbor);
                }
            }
        }

        return distances;
    }
}

// Packet class
class Packet {
    private Router source;
    private Router destination;
    private String data;

    public Packet(Router source, Router destination, String data) {
        this.source = source;
        this.destination = destination;
        this.data = data;
    }

    public Router getSource() {
        return source;
    }

    public Router getDestination() {
        return destination;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "source=" + source.getId() +
                ", destination=" + destination.getId() +
                ", data='" + data + '\'' +
                '}';
    }
}

// Edge class representing connections between routers
class Edge {
    private Router source;
    private Router destination;
    private int weight;

    public Edge(Router source, Router destination, int weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    public Router getSource() {
        return source;
    }

    public Router getDestination() {
        return destination;
    }

    public int getWeight() {
        return weight;
    }
}

// TokenBucket class for token-based congestion control
class TokenBucket {
    private final int bucketCapacity;
    private final int tokenRate;
    private int availableTokens;
    private long lastUpdateTime;

    public TokenBucket(int bucketCapacity, int tokenRate) {
        this.bucketCapacity = bucketCapacity;
        this.tokenRate = tokenRate;
        this.availableTokens = bucketCapacity;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    // Method to update token availability
    public synchronized boolean tryConsume(int tokens) {
        updateTokens();
        if (tokens <= availableTokens) {
            availableTokens -= tokens;
            return true;
        }
        return false;
    }

    // Method to refill tokens based on token rate and time elapsed
    private synchronized void updateTokens() {
        long now = System.currentTimeMillis();
        long elapsedTime = now - lastUpdateTime;
        int newTokens = (int) (elapsedTime / 1000.0 * tokenRate);
        if (newTokens > 0) {
            availableTokens = Math.min(bucketCapacity, availableTokens + newTokens);
            lastUpdateTime = now;
        }
    }
}

// Main class to run the simulation
public class NetworkSimulation {
    private static final Scanner scanner = new Scanner(System.in);
    private static Graph graph;
    private static List<Packet> packets;
    private static long startTime;

    public static void main(String[] args) {
        graph = new Graph();
        packets = new ArrayList<>();
        System.out.println();
        printBoxedMessage("Welcome to World of Network Simulation");
        System.out.println();

        while (true) {
            System.out.println("Main Menu:");
            System.out.println("1. Create a network");
            System.out.println("2. Simulate Traffic");
            System.out.println("3. Show Statistics");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    createNetwork();
                    break;
                case 2:
                    simulateTraffic();
                    break;
                case 3:
                    showStatistics();
                    break;
                case 4:
                    System.exit(0);
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    private static void printBoxedMessage(String message) {
        int messageLength = message.length();
        int boxWidth = messageLength + 4;

        // Print top border
        for (int i = 0; i < boxWidth; i++) {
            System.out.print("*");
        }
        System.out.println();

        // Print message with borders
        System.out.println("* " + message + " *");

        // Print bottom border
        for (int i = 0; i < boxWidth; i++) {
            System.out.print("*");
        }
        System.out.println();
    }

    private static void createNetwork() {
        System.out.println("Create a network:");
        System.out.println("1. Manual");
        System.out.println("2. Using Topology");
        System.out.print("Choose an option: ");
        int choice = scanner.nextInt();

        switch (choice) {
            case 1:
                createManualNetwork();
                break;
            case 2:
                createTopologyNetwork();
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }

    private static void createManualNetwork() {
        System.out.print("Enter number of routers: ");
        int numRouters = scanner.nextInt();
        
        for (int i = 1; i <= numRouters; i++) {
            System.out.println("Router " + i + ":");
            System.out.println("1. Simple Router");
            System.out.println("2. RED Router");
            System.out.print("Choose router type: ");
            int routerTypeChoice = scanner.nextInt();
            
            System.out.print("Enter bucket capacity for router " + i + ": ");
            int bucketCapacity = scanner.nextInt();
            System.out.print("Enter token rate for router " + i + ": ");
            int tokenRate = scanner.nextInt();
            
            if (routerTypeChoice == 1) {
                graph.addRouter(i, bucketCapacity, tokenRate);
            } else if (routerTypeChoice == 2) {
                System.out.print("Enter min threshold for RED at router " + i + ": ");
                double minThreshold = scanner.nextDouble();
                System.out.print("Enter max threshold for RED at router " + i + ": ");
                double maxThreshold = scanner.nextDouble();
                System.out.print("Enter drop probability for RED at router " + i + ": ");
                double dropProbability = scanner.nextDouble();
                System.out.print("Enter average queue size for RED at router " + i + ": ");
                double averageQueueSize = scanner.nextDouble();
                
                graph.addRedRouter(i, bucketCapacity, tokenRate, minThreshold, maxThreshold, dropProbability, averageQueueSize);
            } else {
                System.out.println("Invalid router type choice. Defaulting to Simple Router.");
                graph.addRouter(i, bucketCapacity, tokenRate); // Default to simple router
            }
        }
    
        System.out.print("Enter number of edges: ");
        int numEdges = scanner.nextInt();
        for (int i = 0; i < numEdges; i++) {
            System.out.print("Enter source router, destination router, and weight for edge " + (i + 1) + ": ");
            int sourceId = scanner.nextInt();
            int destinationId = scanner.nextInt();
            int weight = scanner.nextInt();
            graph.addEdge(sourceId, destinationId, weight);
        }
    
        System.out.print("Enter number of packets: ");
        int numPackets = scanner.nextInt();
        for (int i = 0; i < numPackets; i++) {
            System.out.print("Enter source router, destination router, and data for packet " + (i + 1) + ": ");
            int sourceId = scanner.nextInt();
            int destinationId = scanner.nextInt();
            String data = scanner.next();
            Router source = graph.getRouter(sourceId);
            Router destination = graph.getRouter(destinationId);
            packets.add(new Packet(source, destination, data));
        }
    }
    
    private static void createTopologyNetwork() {
        System.out.println("Available topologies:");
        System.out.println("1. Bus");
        System.out.println("2. Star");
        System.out.println("3. Ring");
        System.out.println("4. Mesh");
        System.out.println("5. Tree");
        System.out.print("Choose a topology: ");
        int choice = scanner.nextInt();

        System.out.print("Enter number of routers: ");
        int numRouters = scanner.nextInt();

        System.out.print("Enter number of packets: ");
        int numPackets = scanner.nextInt();

        for (int i = 1; i <= numRouters; i++) {
            System.out.print("Enter bucket capacity for router " + i + ": ");
            int bucketCapacity = scanner.nextInt();
            System.out.print("Enter token rate for router " + i + ": ");
            int tokenRate = scanner.nextInt();
            graph.addRouter(i, bucketCapacity, tokenRate);
        }

        switch (choice) {
            case 1:
                createBusTopology(numRouters);
                break;
            case 2:
                createStarTopology(numRouters);
                break;
            case 3:
                createRingTopology(numRouters);
                break;
            case 4:
                createMeshTopology(numRouters);
                break;
            case 5:
                createTreeTopology(numRouters);
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                return;
        }

        for (int i = 0; i < numPackets; i++) {
            System.out.print("Enter source router, destination router, and data for packet " + (i + 1) + ": ");
            int sourceId = scanner.nextInt();
            int destinationId = scanner.nextInt();
            String data = scanner.next();
            Router source = graph.getRouter(sourceId);
            Router destination = graph.getRouter(destinationId);
            packets.add(new Packet(source, destination, data));
        }
    }

    private static void createBusTopology(int numRouters) {
        for (int i = 1; i < numRouters; i++) {
            graph.addEdge(i, i + 1, 1);
            graph.addEdge(i + 1, i, 1);
        }
    }

    private static void createStarTopology(int numRouters) {
        for (int i = 2; i <= numRouters; i++) {
            graph.addEdge(1, i, 1);
            graph.addEdge(i, 1, 1);
        }
    }

    private static void createRingTopology(int numRouters) {
        for (int i = 1; i < numRouters; i++) {
            graph.addEdge(i, i + 1, 1);
            graph.addEdge(i + 1, i, 1);
        }
        graph.addEdge(numRouters, 1, 1);
        graph.addEdge(1, numRouters, 1);
    }

    private static void createMeshTopology(int numRouters) {
        for (int i = 1; i <= numRouters; i++) {
            for (int j = i + 1; j <= numRouters; j++) {
                graph.addEdge(i, j, 1);
                graph.addEdge(j, i, 1);
            }
        }
    }

    private static void createTreeTopology(int numRouters) {
        for (int i = 1; i <= numRouters / 2; i++) {
            graph.addEdge(i, 2 * i, 1);
            graph.addEdge(i, 2 * i + 1, 1);
        }
    }

    private static void simulateTraffic() {
        System.out.println("Simulating traffic...");
        startTime = System.currentTimeMillis(); // Start time for execution time calculation
        Map<Router, Map<Router, Integer>> shortestPaths = graph.calculateShortestPaths();
        for (Packet packet : packets) {
            Router source = packet.getSource();
            if (!source.enqueuePacket(packet)) {
                System.out.println("Packet dropped at source router " + source.getId() + ": " + packet);
            }
        }
        for (Router router : graph.getRouters()) {
            router.forwardPackets(shortestPaths);
        }
        System.out.println("Traffic simulation completed.");
    }

    private static void showStatistics() {
        System.out.println("Show Statistics:");
        System.out.println("1. Router-wise Statistics");
        System.out.println("2. Network-wide Statistics");
        System.out.print("Choose an option: ");
        int choice = scanner.nextInt();

        switch (choice) {
            case 1:
                showRouterStatistics();
                break;
            case 2:
                showNetworkStatistics();
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }

    private static void showRouterStatistics() {
        System.out.println("Showing router-wise statistics...");
        for (Router router : graph.getRouters()) {
            System.out.println("Router " + router.getId() + " statistics:");
            System.out.println("Total delay: " + router.getTotalDelay());
            System.out.println("Packets forwarded: " + router.getPacketsForwarded());
            System.out.println("Packets dropped: " + router.getPacketsDropped());
            System.out.println("Utilization: " + router.calculateUtilization());
            System.out.println("Path efficiency: " + router.calculatePathEfficiency());
            System.out.println("Packet delivery ratio: " + router.calculatePacketDeliveryRatio());
            System.out.println("Network load: " + router.calculateNetworkLoad());
            System.out.println();
        }
    }

    private static void showNetworkStatistics() {
        System.out.println("Showing network-wide statistics...");
        long totalExecutionTime = System.currentTimeMillis() - startTime;
        long totalMemoryUsage = getTotalMemoryUsage();
        int totalPacketsDropped = 0;
        int totalPacketsForwarded = 0;
        long totalDelay = 0;

        for (Router router : graph.getRouters()) {
            totalPacketsDropped += router.getPacketsDropped();
            totalPacketsForwarded += router.getPacketsForwarded();
            totalDelay += router.getTotalDelay();
        }

        double throughput = (double) totalPacketsForwarded / totalExecutionTime;
        double latency = (double) totalDelay / totalPacketsForwarded;

        System.out.println("Total execution time: " + totalExecutionTime + " ms");
        System.out.println("Total memory usage: " + totalMemoryUsage + " bytes");
        System.out.println("Throughput: " + throughput + " packets/ms");
        System.out.println("Latency: " + latency + " ms/packet");
        System.out.println("Total packets dropped: " + totalPacketsDropped);
    }

    private static long getTotalMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        return heapUsage.getUsed() + nonHeapUsage.getUsed();
    }
}