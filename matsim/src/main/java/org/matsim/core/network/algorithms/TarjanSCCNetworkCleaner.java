package org.matsim.core.network.algorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.turnRestrictions.ColoredLink;
import org.matsim.core.router.turnRestrictions.TurnRestrictionsContext;
import org.matsim.core.utils.misc.Counter;

import java.util.*;

/**
 * A network cleaner that uses Tarjan's algorithm to find all strongly
 * connected components (SCCs). It then retains only the largest SCC
 * (by number of nodes) in the Network.
 *
 * Also accounts for disallowed next links. To capture multi step and overlapping restrictions, the algorithm
 * is applied iteratively until the delta in node sizes between two iterations is zero.
 *
 * DISCLAIMER:
 * - This is a recursive implementation. Large or deep networks may cause a StackOverflowError.
 * - For extremely large networks, consider an iterative Tarjan approach or increase JVM stack size (e.g.,'-Xss100m')
 *
 * @author nkuehnel / MOIA
 */
public final class TarjanSCCNetworkCleaner {

    private static final Logger log = LogManager.getLogger(TarjanSCCNetworkCleaner.class);

    // -------------------------
    // Internal Tarjan structures
    // -------------------------
    private List<Set<Id<Node>>> sccList;
    private Map<Id<Node>, List<Id<Node>>> adjacencyList; // adjacency for each node ID
    private Map<Id<Node>, Integer> discoveryTime;         // discovery index/time for each node
    private Map<Id<Node>, Integer> lowLink;               // low-link values for each node
    private Deque<Id<Node>> stack;                        // stack of active nodes
    private Set<Id<Node>> onStack;                        // quick check if a node is on the stack
    private int timeCounter = 0;                           // global "time" counter for DFS
    private TurnRestrictionsContext turnRestrictionsContext;

    // -------------------------
    // The network to be cleaned
    // -------------------------
    private final Network network;

    public TarjanSCCNetworkCleaner(Network network) {
        this.network = network;
    }

    /**
     * Runs Tarjan's algorithm to find strongly connected components,
     * then reduces the network so it only contains the largest SCC.
     */
    public void run() {
        int currentSize = network.getLinks().size();
        int delta = Integer.MAX_VALUE;
        int iteration = 0;

        while (delta > 0) {
            log.info("Running iteration " + iteration);
            runImpl();

            delta = currentSize - network.getLinks().size();
            log.info("Current delta: " + delta);
            currentSize = network.getLinks().size();
            iteration++;
        }
    }

    private void runImpl() {
        turnRestrictionsContext = TurnRestrictionsContext.buildContext(network);

        log.info("Building adjacency list from the MATSim network...");
        buildAdjacencyList();

        log.info("Running Tarjan's algorithm to find SCCs...");
        // Initialize Tarjan data structures
        sccList = new ArrayList<>();
        discoveryTime = new HashMap<>(adjacencyList.size());
        lowLink       = new HashMap<>(adjacencyList.size());
        stack         = new ArrayDeque<>();
        onStack       = new HashSet<>();

        // Run Tarjan DFS on each unvisited node
        for (Id<Node> nodeId : adjacencyList.keySet()) {
            if (!discoveryTime.containsKey(nodeId)) {
                strongConnect(nodeId);
            }
        }

        log.info("Found " + sccList.size() + " strongly connected components in total.");

        // Identify the largest SCC and remove all others
        log.info("Retaining only the largest SCC...");
        List<Set<Id<Node>>> largestSCC = findLargestSCCs(sccList);
        for (Set<Id<Node>> set : largestSCC) {
            reduceToCluster(set);
        }
    }

    // -----------------------------------------------------------------------
    // 1. Build adjacency from the MATSim Network, ignoring turn restrictions
    // -----------------------------------------------------------------------
    private void buildAdjacencyList() {
        adjacencyList = new HashMap<>(network.getNodes().size());

        // Initialize adjacency lists
        for (Node node : network.getNodes().values()) {
            adjacencyList.put(node.getId(), new ArrayList<>());
        }

        Counter counter = new Counter("Node ", "", 10);

        for (Node node : network.getNodes().values()) {
            List<Id<Node>> outNeighbors = adjacencyList.get(node.getId());

            for (Link link : node.getOutLinks().values()) {

                // Collect possible colored links from replacedLinks
                // If there's a replaced link add it to the stack
                Deque<ColoredLink> stack = new ArrayDeque<>();
                if (turnRestrictionsContext.getReplacedLinks().containsKey(link.getId())) {
                    stack.add(turnRestrictionsContext.getReplacedLinks().get(link.getId()));
                } else {
                    outNeighbors.add(link.getToNode().getId());
                }

                // 2) Expand each colored link using a stack approach
                Set<ColoredLink> visitedColoredLinks = new HashSet<>();

                while (!stack.isEmpty()) {
                    ColoredLink currentLink = stack.pop();

                    // Skip if we've already processed this colored link
                    if (!visitedColoredLinks.add(currentLink)) {
                        continue;
                    }

                    // If it leads to another colored node, push out-links
                    if (currentLink.getToColoredNode() != null) {
                        for (ColoredLink outLink : currentLink.getToColoredNode().outLinks()) {
                            stack.push(outLink);
                        }
                    } else {
                        // Else, we reached a real node => add to adjacency
                        Node realToNode = currentLink.getToNode();
                        outNeighbors.add(realToNode.getId());
                    }
                }
            }

            counter.incCounter();
        }
        counter.printCounter();
    }

    private void strongConnect(Id<Node> nodeId) {
        // Initialize discovery time and low-link
        discoveryTime.put(nodeId, timeCounter);
        lowLink.put(nodeId, timeCounter);
        timeCounter++;
        stack.push(nodeId);
        onStack.add(nodeId);

        // Explore neighbors
        for (Id<Node> neighborId : adjacencyList.get(nodeId)) {
            if (!discoveryTime.containsKey(neighborId)) {
                // Neighbor not visited, recurse
                strongConnect(neighborId);
                // Update lowLink
                lowLink.put(nodeId, Math.min(lowLink.get(nodeId), lowLink.get(neighborId)));
            } else if (onStack.contains(neighborId)) {
                // Neighbor is in the current stack, so it's part of the SCC
                lowLink.put(nodeId, Math.min(lowLink.get(nodeId), discoveryTime.get(neighborId)));
            }
        }

        // If nodeId is a root node, pop the stack and generate an SCC
        if (lowLink.get(nodeId).equals(discoveryTime.get(nodeId))) {
            // Start a new SCC
            Set<Id<Node>> scc = new HashSet<>();
            Id<Node> w;
            do {
                w = stack.pop();
                onStack.remove(w);
                scc.add(w);
            } while (!w.equals(nodeId));
            // Add this SCC to the list
            sccList.add(scc);
        }
    }

    private static List<Set<Id<Node>>> findLargestSCCs(List<Set<Id<Node>>> sccList) {
        return sccList.stream().sorted(Comparator.comparingInt((Set<Id<Node>> ids) -> ids.size()).reversed()).limit(1).toList();
    }

    private void reduceToCluster(Set<Id<Node>> biggestCluster) {
        log.info("Biggest SCC has " + biggestCluster.size() + " nodes.");
        log.info("Removing all nodes/links not in the largest SCC...");

        List<Node> allNodes = new ArrayList<>(network.getNodes().values());
        for (Node node : allNodes) {
            if (!biggestCluster.contains(node.getId())) {
                network.removeNode(node.getId());
            }
        }
        log.info("Resulting network has " + network.getNodes().size() + " nodes and "
                + network.getLinks().size() + " links.");
    }
}
