/* *********************************************************************** *
 * project: org.matsim.*
 * Dijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.priorityqueue.WrappedBinaryMinHeap;
import org.matsim.core.router.util.DijkstraNodeData;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.RouterPriorityQueue;
import org.matsim.vehicles.Vehicle;

/**
 * Implementation of <a
 * href="http://en.wikipedia.org/wiki/Dijkstra%27s_algorithm">Dijkstra's
 * shortest-path algorithm</a> on a time-dependent network with arbitrary
 * non-negative cost functions (e.g. negative link cost are not allowed). So
 * 'shortest' in our context actually means 'least-cost'.
 *
 * <p>
 * For every router, there exists a class which computes some preprocessing data
 * and is passed to the router class constructor in order to accelerate the
 * routing procedure. The one used for Dijkstra is
 * {@link org.matsim.core.router.util.PreProcessDijkstra}.
 * </p>
 * <br>
 *
 * <h2>Code example:</h2>
 * <p>
 * <code>PreProcessDijkstra preProcessData = new PreProcessDijkstra();<br>
 * preProcessData.run(network);<br>
 * TravelCost costFunction = ...<br>
 * LeastCostPathCalculator routingAlgo = new Dijkstra(network, costFunction, preProcessData);<br>
 * routingAlgo.calcLeastCostPath(fromNode, toNode, startTime);</code>
 * </p>
 * <p>
 * If you don't want to preprocess the network, you can invoke Dijkstra as
 * follows:
 * </p>
 * <p>
 * <code> LeastCostPathCalculator routingAlgo = new Dijkstra(network, costFunction);</code>
 * </p>
 * 
 * <h2>Important note</h2>
 * This class is NOT thread-safe!
 *
 * @see org.matsim.core.router.util.PreProcessDijkstra
 * @see org.matsim.core.router.AStarEuclidean
 * @see org.matsim.core.router.AStarLandmarks
 * @author lnicolas
 * @author mrieser
 */
 public class Dijkstra implements LeastCostPathCalculator {
 	// yyyyyy I don't think that we should make this class publicly inheritable; as we know, will eventually lead
	// to problems.  kai, feb'18

	private final static Logger log = LogManager.getLogger(Dijkstra.class);

	/**
	 * The network on which we find routes.
	 */
	protected Network network;

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	protected final TravelDisutility costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and time step.
	 */
	protected final TravelTime timeFunction;

	final HashMap<Id<Node>, DijkstraNodeData> nodeData;

	/**
	 * Provides an unique id (loop number) for each routing request, so we don't
	 * have to reset all nodes at the beginning of each re-routing but can use the
	 * loop number instead.
	 */
	private int iterationID = Integer.MIN_VALUE + 1;

	/**
	 * Temporary field that is only used if dead ends are being pruned during
	 * routing and is updated each time a new route has to be calculated.
	 */
	private Node deadEndEntryNode;

	/**
	 * Determines whether we should mark nodes in dead ends during a
	 * pre-processing step so they won't be expanded during routing.
	 */
	/*package*/ final boolean pruneDeadEnds;


	private final PreProcessDijkstra preProcessData;

	private RouterPriorityQueue<Node> heap = null;

	private String[] modeRestriction = null;
	
	/*package*/ Person person = null;
	/*package*/ Vehicle vehicle = null;

	/**
	 * Default constructor.
	 *
	 * @param network
	 *            The network on which to route.
	 * @param costFunction
	 *            Determines the link cost defining the cheapest route.
	 * @param timeFunction
	 *            Determines the travel time on links.
	 */
	// please use DijkstraFactory when you want to create an instance of this
	protected Dijkstra(final Network network, final TravelDisutility costFunction, final TravelTime timeFunction) {
		this(network, costFunction, timeFunction, null);
	}

	/**
	 * Constructor.
	 *
	 * @param network
	 *            The network on which to route.
	 * @param costFunction
	 *            Determines the link cost defining the cheapest route.
	 * @param timeFunction
	 *            Determines the travel time on each link.
	 * @param preProcessData
	 *            The pre processing data used during the routing phase.
	 */
	// please use DijkstraFactory when you want to create an instance of this
	protected Dijkstra(final Network network, final TravelDisutility costFunction, final TravelTime timeFunction,
			final PreProcessDijkstra preProcessData) {

		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;
		this.preProcessData = preProcessData;

		this.nodeData = new HashMap<>((int)(network.getNodes().size() * 1.1), 0.95f);

		if (preProcessData != null) {
			if (!preProcessData.containsData()) {
				this.pruneDeadEnds = false;
				log.warn("The preprocessing data provided to router class Dijkstra contains no data! Please execute its run(...) method first!");
				log.warn("Running without dead-end pruning.");
			} else {
				this.pruneDeadEnds = true;
			}
		} else {
			this.pruneDeadEnds = false;
		}
	}

	/**
	 * @deprecated Use a filtered network instead which only contains the links you want.
	 */
	@Deprecated
	public void setModeRestriction(final Set<String> modeRestriction) {
		if (modeRestriction == null) {
			this.modeRestriction = null;
		} else {
			this.modeRestriction = modeRestriction.toArray(new String[modeRestriction.size()]);
		}
	}

	/**
	 * Calculates the cheapest route from Node 'fromNode' to Node 'toNode' at
	 * starting time 'startTime'.
	 *
	 * @param fromNode
	 *            The Node at which the route should start.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            The time at which the route should start.
	 * @see org.matsim.core.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.api.core.v01.network.Node, org.matsim.api.core.v01.network.Node, double, org.matsim.api.core.v01.population.Person, org.matsim.vehicles.Vehicle)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime, final Person person2, final Vehicle vehicle2) {

		/*
		 * Ensure that the given nodes are part of the network used by the router. Otherwise, the router would
		 * route within the network of the nodes and NOT the one provided when it was created. Previously, this 
		 * caused problems when sub-networks where used.
		 * cdobler, jun'14
		 */
		checkNodeBelongToNetwork(fromNode);
		checkNodeBelongToNetwork(toNode);
		
		augmentIterationId(); // this call makes the class not thread-safe
		this.person = person2;
		this.vehicle = vehicle2;

		if (this.pruneDeadEnds) {
			this.deadEndEntryNode = getPreProcessData(toNode).getDeadEndEntryNode();
		}

		RouterPriorityQueue<Node> pendingNodes = (RouterPriorityQueue<Node>) createRouterPriorityQueue();
		initFromNode(fromNode, toNode, startTime, pendingNodes);

		Node foundToNode = searchLogic(fromNode, toNode, pendingNodes, person2, vehicle2 );
		
		if (foundToNode == null) return null;
		else {
			DijkstraNodeData outData = getData(foundToNode);
			double arrivalTime = outData.getTime();
			
			// now construct and return the path
			return constructPath(fromNode, foundToNode, startTime, arrivalTime);			
		}
	}

	/*
	 * Move this code to a separate method since it needs to be extended by the MultiNodeDijkstra.
	 * cdobler, jun'14
	 */
	/*package*/ void checkNodeBelongToNetwork(Node node) {
		if (this.network.getNodes().get(node.getId()) != node) {
			throw new IllegalArgumentException("The nodes passed as parameters are not part of the network stored by "+
					getClass().getSimpleName() + ": the validity of the results cannot be guaranteed. Aborting!");
		}
	}
	
	/**
	 * Allow replacing the RouterPriorityQueue.
	 */
	/*package*/ RouterPriorityQueue<? extends Node> createRouterPriorityQueue() {
		/*
		 * The (Wrapped)BinaryMinHeap replaces the PseudoRemovePriorityQueue as default
		 * PriorityQueue. It offers a decreaseKey method and uses less memory. As a result,
		 * the routing performance is increased by ~20%.
		 * 
		 * When an ArrayRoutingNetwork is used, an BinaryMinHeap can be used which uses
		 * the getArrayIndex() method of the ArrayRoutingNetworkNodes which further reduces
		 * the memory consumption and increases the performance by another ~10%.
		 */
		/*
		 * Create a WrappedBinaryMinHeap and add all nodes initially once in the same order as
		 * they are in the network. Internally, the heap assigns all elements an index. By adding
		 * all elements initially, we ensure that the indices are in the same order as the nodes
		 * are in the network. In case the router finds two connections with the same costs,
		 * the selected connection depends on the indices.
		 * Moreover, re-use the heap instead of creating it from scratch for each calculated route.
		 * According to findings from the FastDijkstra, this should be faster.
		 * cdobler, sep'17
		 */
		if (this.heap == null) {
			this.heap = new WrappedBinaryMinHeap<>(this.network.getNodes().size());
			for (Node node : this.network.getNodes().values()) this.heap.add(node, 0.);
		}
		this.heap.reset();			
		return this.heap;
//		return new WrappedBinaryMinHeap<>(this.network.getNodes().size());
//		return new PseudoRemovePriorityQueue<>(this.network.getNodes().size());
	}
	
	/**
	 * Logic that was previously located in the calcLeastCostPath(...) method.
	 * Can be overwritten in the MultiModalDijkstra.
	 * Returns the last node of the path. By default this is the to-node.
	 * The MultiNodeDijkstra returns the cheapest of all given to-nodes.
	 */
	/*package*/ Node searchLogic( final Node fromNode, final Node toNode, final RouterPriorityQueue<Node> pendingNodes, Person person, Vehicle vehicle ) {
		// It is a bit overkill to pass Person and Vehicle.  However, since the method is package-private, I think that it is acceptable.  For
		// a more public method, presumably only an "info" string should be passed.  kai, oct'21

		boolean stillSearching = true;
		
		while (stillSearching) {
			Node outNode = pendingNodes.poll();

			if (outNode == null) {
				log.warn("No route was found from node " + fromNode.getId() + " to node " + toNode.getId() + ". " + createInfoMessage( person, vehicle ) + "Some possible reasons:" );
				log.warn("  * Network is not connected.  Run NetworkUtils.cleanNetwork(Network network, Set<String> modes)s.") ;
				log.warn("  * Network for considered mode does not even exist.  Modes need to be entered for each link in network.xml.");
				log.warn("  * Network for considered mode is not connected to starting or ending point of route.  Setting insertingAccessEgressWalk to true may help.");
				log.warn("This will now return null, but it may fail later with a null pointer exception.");
				return null;
			}

			if (outNode == toNode) {
				stillSearching = false;
			} else {
				relaxNode(outNode, toNode, pendingNodes);
			}
		}
		return toNode;
	}
	static StringBuilder createInfoMessage( Person person, Vehicle vehicle ){
		StringBuilder strb = new StringBuilder();
		boolean flag = false ;
		if ( person != null ) {
			strb.append( person.getId() );
			flag = true ;
		}
		if ( vehicle !=null ) {
			strb.append( vehicle.getId() );
			flag = true;
		}
		if ( flag ) {
			strb.append( ". " );
		}
		return strb;
	}

	/**
	 * Constructs the path after the algorithm has been run.
	 *
	 * @param fromNode
	 *            The node where the path starts.
	 * @param toNode
	 *            The node where the path ends.
	 * @param startTime
	 *            The time when the trip starts.
	 */
	protected Path constructPath(Node fromNode, Node toNode, double startTime, double arrivalTime) {
		List<Node> nodes = new ArrayList<>();
		List<Link> links = new ArrayList<>();

		nodes.add(0, toNode);
		Link tmpLink = getData(toNode).getPrevLink();
		if (tmpLink != null) {
			while (tmpLink.getFromNode() != fromNode) {
				links.add(0, tmpLink);
				nodes.add(0, tmpLink.getFromNode());
				tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
			}
			links.add(0, tmpLink);
			nodes.add(0, tmpLink.getFromNode());
			
		}

		DijkstraNodeData toNodeData = getData(toNode);
		Path path = new Path(nodes, links, arrivalTime - startTime, toNodeData.getCost());
		return path;
	}
	
	/**
	 * Initializes the first node of a route.
	 *
	 * @param fromNode
	 *            The Node to be initialized.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            The time we start routing.
	 * @param pendingNodes
	 *            The pending nodes so far.
	 */
	/*package*/ void initFromNode(final Node fromNode, final Node toNode, final double startTime,
			final RouterPriorityQueue<Node> pendingNodes) {
		DijkstraNodeData data = getData(fromNode);
		visitNode(fromNode, data, pendingNodes, startTime, 0, null);
	}

	/**
	 * Expands the given Node in the routing algorithm; may be overridden in
	 * sub-classes.
	 *
	 * @param outNode
	 *            The Node to be expanded.
	 * @param toNode
	 *            The target Node of the route.
	 * @param pendingNodes
	 *            The set of pending nodes so far.
	 */
	protected void relaxNode(final Node outNode, final Node toNode, final RouterPriorityQueue<Node> pendingNodes) {

		DijkstraNodeData outData = getData(outNode);
		double currTime = outData.getTime();
		double currCost = outData.getCost();
		if (this.pruneDeadEnds) {
			PreProcessDijkstra.DeadEndData ddOutData = getPreProcessData(outNode);

			for (Link l : outNode.getOutLinks().values()) {
				relaxNodeLogic(l, pendingNodes, currTime, currCost, toNode, ddOutData);
			}
		} else { // this.pruneDeadEnds == false
			for (Link l : outNode.getOutLinks().values()) {
				relaxNodeLogic(l, pendingNodes, currTime, currCost, toNode, null);
			}				
		}
	}
	
	/**
	 * Logic that was previously located in the relaxNode(...) method. 
	 * By doing so, the FastDijkstra can overwrite relaxNode without copying the logic. 
	 */
	/*package*/ void relaxNodeLogic(final Link l, final RouterPriorityQueue<Node> pendingNodes, final double currTime, 
			final double currCost, final Node toNode, final PreProcessDijkstra.DeadEndData ddOutData) {
		if (this.pruneDeadEnds) {
			if (canPassLink(l)) {
				Node n = l.getToNode();
				PreProcessDijkstra.DeadEndData ddData = getPreProcessData(n);

				/* IF the current node n is not in a dead end
				 * OR it is in the same dead end as the fromNode
				 * OR it is in the same dead end as the toNode
				 * THEN we add the current node to the pending nodes */
				if ((ddData.getDeadEndEntryNode() == null)
						|| (ddOutData.getDeadEndEntryNode() != null)
						|| ((this.deadEndEntryNode != null)
								&& (this.deadEndEntryNode.getId() == ddData.getDeadEndEntryNode().getId()))) {
					
					addToPendingNodes(l, n, pendingNodes, currTime, currCost, toNode);
				}
			}
		} else {
			if (canPassLink(l)) {
				addToPendingNodes(l, l.getToNode(), pendingNodes, currTime, currCost, toNode);
			}
		}
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
	 * @param toNode
	 *            The target Node of the route.
	 * @return true if the node was added to the pending nodes, false otherwise
	 * 		(e.g. when the same node already has an earlier visiting time).
	 */
	protected boolean addToPendingNodes(final Link l, final Node n,
			final RouterPriorityQueue<Node> pendingNodes, final double currTime,
			final double currCost, final Node toNode) {
		
		final double travelTime = this.timeFunction.getLinkTravelTime(l, currTime, this.person, this.vehicle);
		final double travelCost = this.costFunction.getLinkTravelDisutility(l, currTime, this.person, this.vehicle);
		final DijkstraNodeData data = getData(n);
		if (!data.isVisited(getIterationId())) {
			visitNode(n, data, pendingNodes, currTime + travelTime, currCost + travelCost, l);
			return true;
		}
				
		final double nCost = data.getCost();
		final double totalCost = currCost + travelCost;
		if (totalCost < nCost) {
			revisitNode(n, data, pendingNodes, currTime + travelTime, totalCost, l);
			return true;
		} else if (totalCost == nCost) {
			// Special case: a node can be reached from two links with exactly the same costs.
			// Decide based on the linkId which one to take... just have to common criteria to be deterministic.
			Link prevLink = data.getPrevLink();
			if (prevLink != null && prevLink.getId().compareTo(l.getId()) > 0) {
				revisitNode(n, data, pendingNodes, currTime + travelTime, totalCost, l);
				return true;
			}
		}
		return false;
	}

	/**
	 * @return <code>true</code> if the link can be passed with respect to a possible mode restriction set
	 *
	 * @see #setModeRestriction(Set)
	 */
	protected boolean canPassLink(final Link link) {
		if (this.modeRestriction == null) {
			return true;
		}
		for (String mode : this.modeRestriction) {
			if (link.getAllowedModes().contains(mode)) {
				return true;
			}
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
	protected void revisitNode(final Node n, final DijkstraNodeData data, final RouterPriorityQueue<Node> pendingNodes, 
			final double time, final double cost, final Link outLink) {
		data.visit(outLink, cost, time, getIterationId());
		pendingNodes.decreaseKey(n, getPriority(data));
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
	protected void visitNode(final Node n, final DijkstraNodeData data, final RouterPriorityQueue<Node> pendingNodes,
			final double time, final double cost, final Link outLink) {
		data.visit(outLink, cost, time, getIterationId());
		pendingNodes.add(n, getPriority(data));
	}

	/**
	 * Augments the iterationID and checks whether the visited information in
	 * the nodes in the nodes have to be reset.
	 */
	protected void augmentIterationId() {
		if (getIterationId() == Integer.MAX_VALUE) {
			this.iterationID = Integer.MIN_VALUE + 1;
			resetNetworkVisited();
		} else {
			this.iterationID++;
		}
	}

	/**
	 * @return iterationID
	 */
	protected int getIterationId() {
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

	/**
	 * The value used to sort the pending nodes during routing.
	 * This implementation compares the total effective travel cost
	 * to sort the nodes in the pending nodes queue during routing.
	 */
	protected double getPriority(final DijkstraNodeData data) {
		return data.getCost();
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
			r = createNodeData();
			this.nodeData.put(n.getId(), r);
		}
		return r;
	}

	protected DijkstraNodeData createNodeData() {
		return new DijkstraNodeData();
	}
	
	protected PreProcessDijkstra.DeadEndData getPreProcessData(final Node n) {
		return this.preProcessData.getNodeData(n);
	}

	protected final Person getPerson() {
		return this.person;
	}
	
	protected final Vehicle getVehicle() {
		return this.vehicle;
	}
}
