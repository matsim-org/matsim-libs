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

package org.matsim.router;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Route;
import org.matsim.router.util.KeyComparator;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.PreProcessDijkstra;
import org.matsim.router.util.PriorityQueueBucket;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.identifiers.IdI;

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
 * org.matsim.demandmodeling.router.util.PreProcessDijkstra.
 * </p>
 * <br>
 *
 * <h2>Code example:</h2>
 * <p>
 * <code>PreProcessDijkstra preProcessData = new PreProcessDijkstra();<br>
 * preProcessData.run(network);<br>
 * TravelCostI costFunction = ...<br>
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
 * @see org.matsim.router.util.PreProcessDijkstra
 * @see org.matsim.router.AStarEuclidean
 * @see org.matsim.router.AStarLandmarks
 * @author lnicolas
 * @author mrieser
 */
public class Dijkstra implements LeastCostPathCalculator {

	/**
	 * The network on which we find routes.
	 */
	final NetworkLayer network;

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	final TravelCostI costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and time step.
	 */
	final TravelTimeI timeFunction;

	final private HashMap<IdI, DijkstraNodeData> nodeData;

	/**
	 * Provides an unique id (loop number) for each routing request, so we don't
	 * have to reset all nodes at the beginning of each re-routing but can use the
	 * loop number instead.
	 */
	private int iterationID = Integer.MIN_VALUE + 1;

	/**
	 * The role that contains the pre-process data this algorithm uses during
	 * routing.
	 */
	int preProcRoleIndex = -1;

	/**
	 * Temporary field that is only used if dead ends are being pruned during
	 * routing and is updated each time a new route has to be calculated.
	 */
	Node deadEndEntryNode;

	/**
	 * Determines whether we should mark nodes in dead ends during a
	 * pre-processing step so they won't be expanded during routing.
	 */
	private boolean pruneDeadEnds;

	/**
	 * Comparator that defines how to order the nodes in the pending nodes queue
	 * during routing.
	 */
	protected ComparatorDijkstraCost comparator;

	/**
	 * A constant for the export to GDF (Guess file format) of the route.
	 */
	final static int exportedMarkedNodeSize = 1024;

	boolean doGatherInformation = true;

	double avgRouteLength = 0;

	double avgTravelTime = 0;

	int routeCnt = 0;

	int revisitNodeCount = 0;

	int visitNodeCount = 0;

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
	public Dijkstra(final NetworkLayer network, final TravelCostI costFunction, final TravelTimeI timeFunction) {
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
	public Dijkstra(final NetworkLayer network, final TravelCostI costFunction, final TravelTimeI timeFunction,
			final PreProcessDijkstra preProcessData) {

		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this.nodeData = new HashMap<IdI, DijkstraNodeData>((int)(network.getNodes().size() * 1.1), 0.95f);
		this.comparator = new ComparatorDijkstraCost(this.nodeData);

		this.pruneDeadEnds = false;
		if (preProcessData != null) {
			if (preProcessData.containsData() == false) {
				Gbl.errorMsg("The preprocessing data provided to router "
						+ "class Dijkstra contains no data! Please execute "
						+ "its run(...) method first!");
			} else {
				this.pruneDeadEnds = true;
				this.preProcRoleIndex = preProcessData.roleIndex();
			}
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
	 * @see org.matsim.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.network.Node,
	 *      org.matsim.network.Node, double)
	 */
	public Route calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime) {

		double arrivalTime = 0;
		boolean stillSearching = true;

		augmentIterationID();

		if (this.pruneDeadEnds == true) {
			this.deadEndEntryNode = getPreProcessRole(toNode).getDeadEndEntryNode();
		}

		PriorityQueueBucket<Node> pendingNodes = new PriorityQueueBucket<Node>(
				this.comparator);
		initFromNode(fromNode, toNode, startTime, pendingNodes);

		while (stillSearching) {
			Node outNode = pendingNodes.poll();

			if (outNode == null) {
				Gbl.warningMsg(this.getClass(), "calcLeastCostPath()",
						"No route was found from node " + fromNode.getId()
								+ " to node " + toNode.getId());
				return null;
			}

			if (outNode.getId() == toNode.getId()) {
				stillSearching = false;
				DijkstraNodeData outData = getData(outNode);
				arrivalTime = outData.getTime();
			} else {
				relaxNode(outNode, toNode, pendingNodes);
			}
		}

		// now construct the route
		ArrayList<Node> routeNodes = new ArrayList<Node>();
		Node tmpNode = toNode;
		while (tmpNode.getId() != fromNode.getId()) {
			routeNodes.add(0, tmpNode);
			DijkstraNodeData tmpData = getData(tmpNode);
			tmpNode = tmpData.getPrevNode();
		}
		routeNodes.add(0, tmpNode); // add the fromNode at the beginning of the list

		DijkstraNodeData toNodeData = getData(toNode);
		Route route = new Route();
		route.setRoute(routeNodes, (int) (arrivalTime - startTime), toNodeData.cost);

		if (this.doGatherInformation) {
			this.avgTravelTime = (this.routeCnt * this.avgTravelTime + route
					.getTravTime()) / (this.routeCnt + 1);
			this.avgRouteLength = (this.routeCnt * this.avgRouteLength + route
					.getDist()) / (this.routeCnt + 1);
			this.routeCnt++;
		}

		return route;
	}

	/**
	 * Initialises the first node of a route.
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
	void initFromNode(final Node fromNode, final Node toNode, final double startTime,
			final PriorityQueueBucket<Node> pendingNodes) {
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
	void relaxNode(final Node outNode, final Node toNode, final PriorityQueueBucket<Node> pendingNodes) {

		DijkstraNodeData outData = getData(outNode);
		double currTime = outData.getTime();
		double currCost = outData.getCost();
		PreProcessDijkstra.DeadEndRole ddOutData = null;
		if (this.pruneDeadEnds == true) {
			ddOutData = getPreProcessRole(outNode);
		}
		for (Link l : outNode.getOutLinks().values()) {
			Node n = l.getToNode();
			if (this.pruneDeadEnds == true) {
				PreProcessDijkstra.DeadEndRole ddData = getPreProcessRole(n);

				// IF the current node n is not in a dead end
				// OR it is in the same dead end as the fromNode
				// OR it is in the same dead end as the toNode
				// THEN we add the current node to the pending nodes
				if (ddData.getDeadEndEntryNode() == null
						|| ddOutData.getDeadEndEntryNode() != null
						|| (this.deadEndEntryNode != null && this.deadEndEntryNode
								.getId() == ddData.getDeadEndEntryNode()
								.getId())) {
					addToPendingNodes(l, n, pendingNodes, currTime, currCost,
							outNode, toNode);
				}
			} else {
				addToPendingNodes(l, n, pendingNodes, currTime, currCost,
						outNode, toNode);
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
	 * @param outNode
	 *            The Node from which we came to n.
	 * @param toNode
	 *            The target Node of the route.
	 */
	protected boolean addToPendingNodes(final Link l, final Node n,
			final PriorityQueueBucket<Node> pendingNodes, final double currTime,
			final double currCost, final Node outNode, final Node toNode) {
		double travelTime = this.timeFunction.getLinkTravelTime(l, currTime);
		double travelCost = this.costFunction.getLinkTravelCost(l, currTime);
		DijkstraNodeData data = getData(n);
		double nCost = data.getCost();
		if (!data.isVisited(getIterationID())) {
			visitNode(n, data, pendingNodes, currTime + travelTime, currCost
					+ travelCost, outNode);
			return true;
		} else if (currCost + travelCost < nCost) {
			revisitNode(n, data, pendingNodes, currTime + travelTime, currCost
					+ travelCost, outNode);
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
	 * @param outNode
	 *            The node from which we came visiting n.
	 */
	void revisitNode(final Node n, final DijkstraNodeData data,
			final PriorityQueueBucket<Node> pendingNodes, final double time, final double cost,
			final Node outNode) {
		/* PriorityQueueBucket.remove() uses the comparator given at instantiating
		 * to find the matching Object. This can lead to removing a wrong object
		 * which happens to have the same key for comparison, but is a completely
		 * different object... Thus we tell the comparator to check the IDs too if
		 * two objects are considered "equal" */
		this.comparator.setCheckIDs(true);
		pendingNodes.remove(n);
		this.comparator.setCheckIDs(false);

		data.visit(outNode, cost, time, getIterationID());
		pendingNodes.add(n);
		this.revisitNodeCount++;
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
	 * @param outNode
	 *            The node from which we came visiting n.
	 */
	void visitNode(final Node n, final DijkstraNodeData data,
			final PriorityQueueBucket<Node> pendingNodes, final double time, final double cost,
			final Node outNode) {
		data.visit(outNode, cost, time, getIterationID());
		pendingNodes.add(n);
		this.visitNodeCount++;
	}

	/**
	 * Augments the iterationID and checks whether the visited information in
	 * the nodes in the nodes have to be reset.
	 */
	private void augmentIterationID() {
		if (getIterationID() == Integer.MAX_VALUE) {
			this.iterationID = Integer.MIN_VALUE + 1;
		} else {
			this.iterationID++;
		}

		checkToResetNetworkVisited();
	}

	/**
	 * @return iterationID
	 */
	int getIterationID() {
		return this.iterationID;
	}

	/**
	 * Resets all nodes in the network as if they have not been visited yet if
	 * the iterationID is equal to Integer.MIN_VALUE + 1.
	 */
	void checkToResetNetworkVisited() {
		// If the re-planning id passed the maximal possible value
		// and has the same value as at the 'beginning', we reset all nodes of
		// the network to avoid identifying a node falsely as visited for the
		// current iteration
		if (getIterationID() == Integer.MIN_VALUE + 1) {
			resetNetworkVisited();
		}
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

	PreProcessDijkstra.DeadEndRole getPreProcessRole(final Node n) {
		return (PreProcessDijkstra.DeadEndRole) n.getRole(this.preProcRoleIndex);
	}

	/**
	 * Prints out some very simple statistical values calculated during routing.
	 */
	public void printInformation() {
		if (this.doGatherInformation) {
			System.out.println("Avg revisited count per route: "
					+ (double) this.revisitNodeCount / this.routeCnt);
			System.out.println("Avg visited count per route: "
					+ (double) this.visitNodeCount / this.routeCnt);
			System.out.println("Number of routes: " + this.routeCnt);
			System.out.println("Average route length: " + this.avgRouteLength);
			System.out.println("Average travel time per route: "
					+ this.avgTravelTime);
		}
	}

	public void cleanUp() {
	}

	/**
	 * A data structure to store temporarily information used
	 * by the Dijkstra-algorithm.
	 */
	static class DijkstraNodeData {

		private Node prev = null;

		private double cost = 0;

		private double time = 0;

		private int iterationID = Integer.MIN_VALUE;

		public void resetVisited() {
			this.prev = null;
			this.iterationID = Integer.MIN_VALUE;
		}

		public void visit(final Node comingFrom, final double cost, final double time,
				final int iterID) {
			this.prev = comingFrom;
			this.cost = cost;
			this.time = time;
			this.iterationID = iterID;
		}

		public boolean isVisited(final int iterID) {
			return (iterID == this.iterationID);
		}

		public double getCost() {
			return this.cost;
		}

		public double getTime() {
			return this.time;
		}

		public Node getPrevNode() {
			return this.prev;
		}
	};

	public static class ComparatorDijkstraCost implements KeyComparator<Node>, Serializable {

		private static final long serialVersionUID = 1L;

		private boolean checkIDs = false;

		protected Map<IdI, ? extends DijkstraNodeData> nodeData;

		public ComparatorDijkstraCost(final Map<IdI, ? extends DijkstraNodeData> nodeData) {
			this.nodeData = nodeData;
		}

		public int compare(final Node n1, final Node n2) {
			double c1 = getKey(n1);
			double c2 = getKey(n2);

			return compare(n1, c1, n2, c2);
		}

		private int compare(final Node n1, final double c1, final Node n2, final double c2) {
			if (c1 < c2) return -1;
			if (c1 > c2) return +1;
			return (this.checkIDs) ? n1.compareTo(n2) : 0;
		}

		public void setCheckIDs(final boolean flag) {
			this.checkIDs = flag;
		}

		public double getKey(final Node node) {
			return this.nodeData.get(node.getId()).getCost();
		}
	}
}