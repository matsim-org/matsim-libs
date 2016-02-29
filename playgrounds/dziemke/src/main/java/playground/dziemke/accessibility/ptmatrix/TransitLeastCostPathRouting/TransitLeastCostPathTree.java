package playground.dziemke.accessibility.ptmatrix.TransitLeastCostPathRouting;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.DijkstraNodeData;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.PseudoRemovePriorityQueue;
import org.matsim.core.utils.collections.RouterPriorityQueue;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * /**
 * Copied and adjusted from a class in the Matsim core code named MultiNodeDijkstra to keep the nodeData to finaly have
 * a LeastCostPathTree.
 * Original class description:
 *
 * "A variant of Dijkstra's algorithm for route finding that supports multiple
 * nodes as start and end. Each start/end node can contain a specific cost
 * component that describes the cost to reach that node to find the least cost
 * path to some place not part of the network.T"
 *
 * "@author mrieser"
 *
 * @author gthunig
 */
public class TransitLeastCostPathTree {

    private static final Logger log = Logger.getLogger(TransitLeastCostPathTree.class);

    /**
     * Provides an unique id (loop number) for each routing request, so we don't
     * have to reset all nodes at the beginning of each re-routing but can use the
     * loop number instead.
     */
    private int iterationID = Integer.MIN_VALUE + 1;

    /**
     * The network on which we find routes.
     */
    protected Network network;

    /**
     * The cost calculator. Provides the cost for each link and time step.
     */
    final TransitTravelDisutility costFunction;

    /**
     * The travel time calculator. Provides the travel time for each link and time step.
     */
    final TravelTime timeFunction;

    final HashMap<Id<Node>, DijkstraNodeData> nodeData;
    private Person person = null;
    private Vehicle vehicle = null;
    private CustomDataManager customDataManager = new CustomDataManager();
    private Coord origin = null;
    private Map<Node, InitialData> fromNodes = null;

    public TransitLeastCostPathTree(final Network network, final TransitTravelDisutility costFunction, final TravelTime timeFunction) {
        this.network = network;
        this.costFunction = costFunction;
        this.timeFunction = timeFunction;

        this.nodeData = new HashMap<>((int)(network.getNodes().size() * 1.1), 0.95f);
    }

    /**
     * Augments the iterationID and checks whether the visited information in
     * the nodes in the nodes have to be reset.
     */
    private void augmentIterationId() {
        if (getIterationId() == Integer.MAX_VALUE) {
            this.iterationID = Integer.MIN_VALUE + 1;
            resetNetworkVisited();
        } else {
            this.iterationID++;
        }
    }

    private int getIterationId() {
        return this.iterationID;
    }

    /**
     * Resets all nodes in the network as if they have not been visited yet.
     */
    private void resetNetworkVisited() {
        for (Node node : this.network.getNodes().values()) {
            DijkstraNodeData data = getData(node);
            data.resetVisited();
        }
    }

    @SuppressWarnings("unchecked")
    public void calcLeastCostPathTree(final Map<Node, InitialData> fromNodes, final Person person, final Coord fromCoord) {
        this.resetNetworkVisited();
        this.person = person;
        this.customDataManager.reset();
        this.origin = fromCoord;
        this.fromNodes = fromNodes;

        RouterPriorityQueue<Node> pendingNodes = (RouterPriorityQueue<Node>) createRouterPriorityQueue();
        for (Map.Entry<Node, InitialData> entry : fromNodes.entrySet()) {
            DijkstraNodeData data = getData(entry.getKey());
            visitNode(entry.getKey(), data, pendingNodes, entry.getValue().initialTime, entry.getValue().initialCost, null);
        }

        // do the real work
        while (pendingNodes.size() > 0) {
            Node outNode = pendingNodes.poll();
            relaxNode(outNode, pendingNodes);
        }
    }

    @SuppressWarnings("unchecked")
    public LeastCostPathCalculator.Path getPath(final Map<Node, InitialData> toNodes) {

        augmentIterationId();

        //find the best node
        double minCost = Double.POSITIVE_INFINITY;
        Node minCostNode = null;
        for (Node currentNode: toNodes.keySet()) {
            DijkstraNodeData data = getData(currentNode);
            InitialData initData = toNodes.get(currentNode);
            double cost = data.getCost() + initData.initialCost;
            if (cost == 0.0 && !fromNodes.containsKey(currentNode)) {
                continue;
            }
            if (cost < minCost) {
                minCost = cost;
                minCostNode = currentNode;
            }
        }

        // now construct the path
        List<Node> nodes = new LinkedList<>();
        List<Link> links = new LinkedList<>();

        nodes.add(0, minCostNode);
        Link tmpLink = getData(minCostNode).getPrevLink();
        while (tmpLink != null) {
            links.add(0, tmpLink);
            nodes.add(0, tmpLink.getFromNode());
            tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
        }
        DijkstraNodeData startNodeData = getData(nodes.get(0));
        DijkstraNodeData toNodeData = getData(minCostNode);

        return new LeastCostPathCalculator.Path(nodes, links, toNodeData.getTime() - startNodeData.getTime(),
                toNodeData.getCost() - startNodeData.getCost());
    }

    @SuppressWarnings("unchecked")
    public LeastCostPathCalculator.Path calcLeastCostPath(final Map<Node, InitialData> fromNodes, final Map<Node, InitialData> toNodes, final Person person) {
        this.person = person;
        this.customDataManager.reset();

        Set<Node> endNodes = new HashSet<Node>(toNodes.keySet());

        augmentIterationId();

        RouterPriorityQueue<Node> pendingNodes = (RouterPriorityQueue<Node>) createRouterPriorityQueue();
        for (Map.Entry<Node, InitialData> entry : fromNodes.entrySet()) {
            DijkstraNodeData data = getData(entry.getKey());
            visitNode(entry.getKey(), data, pendingNodes, entry.getValue().initialTime, entry.getValue().initialCost, null);
        }

        // find out which one is the cheapest end node
        double minCost = Double.POSITIVE_INFINITY;
        Node minCostNode = null;

        // do the real work
        while (endNodes.size() > 0) {
            Node outNode = pendingNodes.poll();

            if (outNode == null) {
                // seems we have no more nodes left, but not yet reached all endNodes...
                endNodes.clear();
            } else {
                DijkstraNodeData data = getData(outNode);
                boolean isEndNode = endNodes.remove(outNode);
                if (isEndNode) {
                    InitialData initData = toNodes.get(outNode);
                    double cost = data.getCost() + initData.initialCost;
                    if (cost < minCost) {
                        minCost = cost;
                        minCostNode = outNode;
                    }
                }
                if (data.getCost() > minCost) {
                    endNodes.clear(); // we can't get any better now
                } else {
                    relaxNode(outNode, pendingNodes);
                }
            }
        }

        if (minCostNode == null) {
            log.warn("No route was found");
            return null;
        }
        Node toNode = minCostNode;

        // now construct the path
        List<Node> nodes = new LinkedList<Node>();
        List<Link> links = new LinkedList<Link>();

        nodes.add(0, toNode);
        Link tmpLink = getData(toNode).getPrevLink();
        while (tmpLink != null) {
            links.add(0, tmpLink);
            nodes.add(0, tmpLink.getFromNode());
            tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
        }
        DijkstraNodeData startNodeData = getData(nodes.get(0));
        DijkstraNodeData toNodeData = getData(toNode);
        LeastCostPathCalculator.Path path = new LeastCostPathCalculator.Path(nodes, links, toNodeData.getTime() - startNodeData.getTime(), toNodeData.getCost() - startNodeData.getCost());

        return path;
    }

    /**
     * Allow replacing the RouterPriorityQueue.
     */
    @SuppressWarnings("static-method")
	/*package*/ RouterPriorityQueue<? extends Node> createRouterPriorityQueue() {
        return new PseudoRemovePriorityQueue<>(500);
    }

    /**
     * Inserts the given Node n into the pendingNodes queue and updates its time
     * and cost information.
     *
     * @param n
     *            The Node that is revisited.
     * @param data
     *            The data for n.
     * @param pendingNodes
     *            The nodes visited and not processed yet.
     * @param time
     *            The time of the visit of n.
     * @param cost
     *            The accumulated cost at the time of the visit of n.
     * @param outLink
     *            The node from which we came visiting n.
     */
    protected void visitNode(final Node n, final DijkstraNodeData data,
                             final RouterPriorityQueue<Node> pendingNodes, final double time, final double cost,
                             final Link outLink) {
        data.visit(outLink, cost, time, getIterationId());
        pendingNodes.add(n, getPriority(data));
    }

    /**
     * Expands the given Node in the routing algorithm; may be overridden in
     * sub-classes.
     *
     * @param outNode
     *            The Node to be expanded.
     * @param pendingNodes
     *            The set of pending nodes so far.
     */
    protected void relaxNode(final Node outNode, final RouterPriorityQueue<Node> pendingNodes) {

        DijkstraNodeData outData = getData(outNode);
        double currTime = outData.getTime();
        double currCost = outData.getCost();
        for (Link l : outNode.getOutLinks().values()) {
            relaxNodeLogic(l, pendingNodes, currTime, currCost);
        }
    }

    /**
     * Logic that was previously located in the relaxNode(...) method.
     * By doing so, the FastDijkstra can overwrite relaxNode without copying the logic.
     */
	/*package*/ void relaxNodeLogic(final Link l, final RouterPriorityQueue<Node> pendingNodes,
                                    final double currTime, final double currCost) {
        addToPendingNodes(l, l.getToNode(), pendingNodes, currTime, currCost);
    }

    /**
     * Adds some parameters to the given Node then adds it to the set of pending
     * nodes.
     *
     * @param l
     *            The link from which we came to this Node.
     * @param n
     *            The Node to add to the pending nodes.
     * @param pendingNodes
     *            The set of pending nodes.
     * @param currTime
     *            The time at which we started to traverse l.
     * @param currCost
     *            The cost at the time we started to traverse l.
     * @return true if the node was added to the pending nodes, false otherwise
     * 		(e.g. when the same node already has an earlier visiting time).
     */
    protected boolean addToPendingNodes(final Link l, final Node n,
                                        final RouterPriorityQueue<Node> pendingNodes, final double currTime,
                                        final double currCost) {

        this.customDataManager.initForLink(l);
        double travelTime = this.timeFunction.getLinkTravelTime(l, currTime, this.person, this.vehicle);
        double travelCost = this.costFunction.getLinkTravelDisutility(l, currTime, this.person, this.vehicle, this.customDataManager);
        DijkstraNodeData data = getData(n);
        double nCost = data.getCost();
        if (!data.isVisited(getIterationId())) {
            visitNode(n, data, pendingNodes, currTime + travelTime, currCost + travelCost, l);
            this.customDataManager.storeTmpData();
            return true;
        }
        double totalCost = currCost + travelCost;
        if (totalCost < nCost) {
            revisitNode(n, data, pendingNodes, currTime + travelTime, totalCost, l);
            this.customDataManager.storeTmpData();
            return true;
        }

        return false;
    }

    /**
     * Changes the position of the given Node n in the pendingNodes queue and
     * updates its time and cost information.
     *
     * @param n
     *            The Node that is revisited.
     * @param data
     *            The data for n.
     * @param pendingNodes
     *            The nodes visited and not processed yet.
     * @param time
     *            The time of the visit of n.
     * @param cost
     *            The accumulated cost at the time of the visit of n.
     * @param outLink
     *            The link from which we came visiting n.
     */
    void revisitNode(final Node n, final DijkstraNodeData data,
                     final RouterPriorityQueue<Node> pendingNodes, final double time, final double cost,
                     final Link outLink) {
        pendingNodes.remove(n);

        data.visit(outLink, cost, time, getIterationId());
        pendingNodes.add(n, getPriority(data));
    }

    /**
     * The value used to sort the pending nodes during routing.
     * This implementation compares the total effective travel cost
     * to sort the nodes in the pending nodes queue during routing.
     */
    private double getPriority(final DijkstraNodeData data) {
        return data.getCost();
    }

    public static class InitialData {
        public final double initialCost;
        public final double initialTime;
        public InitialData(final double initialCost, final double initialTime) {
            this.initialCost = initialCost;
            this.initialTime = initialTime;
        }
    }

    /**
     * Returns the data for the given node. Creates a new NodeData if none exists
     * yet.
     *
     * @param n
     *            The Node for which to return the data.
     * @return The data for the given Node
     */
    protected DijkstraNodeData getData(final Node n) {
        DijkstraNodeData r = this.nodeData.get(n.getId());
        if (null == r) {
            r = new DijkstraNodeData();
            this.nodeData.put(n.getId(), r);
        }
        return r;
    }

    public Coord getOrigin() { return origin; }

}
