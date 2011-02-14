/* *********************************************************************** *
 * project: org.matsim.*
 * DijkstraWithTightTurnPenalty.java
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

package playground.yu.replanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.PseudoRemovePriorityQueue;

import playground.yu.utils.math.SimpleTrigonometric;

/**
 * Implementation of <a href=
 * "http://en.wikipedia.org/wiki/DijkstraWithTightTurnPenalty%27s_algorithm"
 * >DijkstraWithTightTurnPenalty's shortest-path algorithm</a> on a
 * time-dependent network with arbitrary non-negative cost functions (e.g.
 * negative link cost are not allowed). So 'shortest' in our context actually
 * means 'least-cost'.
 * 
 * <p>
 * For every router, there exists a class which computes some preprocessing data
 * and is passed to the router class constructor in order to accelerate the
 * routing procedure. The one used for DijkstraWithTightTurnPenalty is
 * {@link org.matsim.core.router.util.PreProcessDijkstra}.
 * </p>
 * <br>
 * 
 * <h2>Code example:</h2>
 * <p>
 * <code>PreProcessDijkstra preProcessData = new PreProcessDijkstra();<br>
 * preProcessData.run(network);<br>
 * TravelCost costFunction = ...<br>
 * LeastCostPathCalculator routingAlgo = new DijkstraWithTightTurnPenalty(network, costFunction, preProcessData);<br>
 * routingAlgo.calcLeastCostPath(fromNode, toNode, startTime);</code>
 * </p>
 * <p>
 * If you don't want to preprocess the network, you can invoke
 * DijkstraWithTightTurnPenalty as follows:
 * </p>
 * <p>
 * <code> LeastCostPathCalculator routingAlgo = new DijkstraWithTightTurnPenalty(network, costFunction);</code>
 * </p>
 * 
 * @see org.matsim.core.router.util.PreProcessDijkstra
 * @see org.matsim.core.router.AStarEuclidean
 * @see org.matsim.core.router.AStarLandmarks
 * @author lnicolas
 * @author mrieser
 */
public class DijkstraWithTightTurnPenalty implements
		IntermodalLeastCostPathCalculator {

	private final static Logger log = Logger
			.getLogger(DijkstraWithTightTurnPenalty.class);

	/**
	 * The network on which we find routes.
	 */
	protected Network network;

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	final TravelCost costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and
	 * time step.
	 */
	final TravelTime timeFunction;

	final private HashMap<Id, DijkstraNodeData> nodeData;

	/**
	 * Provides an unique id (loop number) for each routing request, so we don't
	 * have to reset all nodes at the beginning of each re-routing but can use
	 * the loop number instead.
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
	private final boolean pruneDeadEnds;

	/**
	 * Comparator that defines how to order the nodes in the pending nodes queue
	 * during routing.
	 */

	private final PreProcessDijkstra preProcessData;

	private String[] modeRestriction = null;

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
	public DijkstraWithTightTurnPenalty(final Network network,
			final TravelCost costFunction, final TravelTime timeFunction) {
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
	public DijkstraWithTightTurnPenalty(final Network network,
			final TravelCost costFunction, final TravelTime timeFunction,
			final PreProcessDijkstra preProcessData) {

		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;
		this.preProcessData = preProcessData;

		nodeData = new HashMap<Id, DijkstraNodeData>((int) (network.getNodes()
				.size() * 1.1), 0.95f);

		if (preProcessData != null) {
			if (preProcessData.containsData() == false) {
				pruneDeadEnds = false;
				log
						.warn("The preprocessing data provided to router class DijkstraWithTightTurnPenalty contains no data! Please execute its run(...) method first!");
				log.warn("Running without dead-end pruning.");
			} else {
				pruneDeadEnds = true;
			}
		} else {
			pruneDeadEnds = false;
		}
	}

	@Override
	public void setModeRestriction(final Set<String> modeRestriction) {
		if (modeRestriction == null) {
			this.modeRestriction = null;
		} else {
			this.modeRestriction = modeRestriction
					.toArray(new String[modeRestriction.size()]);
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
	 * @see org.matsim.core.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.core.network.Node,
	 *      org.matsim.core.network.Node, double)
	 */
	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode,
			final double startTime) {

		double arrivalTime = 0;
		boolean stillSearching = true;

		augmentIterationId();

		if (pruneDeadEnds == true) {
			deadEndEntryNode = getPreProcessData(toNode).getDeadEndEntryNode();
		}

		PseudoRemovePriorityQueue<Node> pendingNodes = new PseudoRemovePriorityQueue<Node>(
				500);
		initFromNode(fromNode, toNode, startTime, pendingNodes);

		while (stillSearching) {
			Node outNode = pendingNodes.poll();

			if (outNode == null) {
				log.warn("No route was found from node " + fromNode.getId()
						+ " to node " + toNode.getId());
				return null;
			}

			if (outNode == toNode) {
				stillSearching = false;
				DijkstraNodeData outData = getData(outNode);
				arrivalTime = outData.getTime();
			} else {
				relaxNode(outNode, toNode, pendingNodes);
			}
		}

		// now construct the path
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();

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
		Path path = new Path(nodes, links, arrivalTime - startTime, toNodeData
				.getCost());

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
	/* package */void initFromNode(final Node fromNode, final Node toNode,
			final double startTime,
			final PseudoRemovePriorityQueue<Node> pendingNodes) {
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
	protected void relaxNode(final Node outNode, final Node toNode,
			final PseudoRemovePriorityQueue<Node> pendingNodes) {

		DijkstraNodeData outData = getData(outNode);
		double currTime = outData.getTime();
		double currCost = outData.getCost();
		// V-----------TESTS-----------V
		Link prevLink = outData.getPrevLink();
		// A-----------TESTS-----------A
		if (pruneDeadEnds) {
			PreProcessDijkstra.DeadEndData ddOutData = getPreProcessData(outNode);
			for (Link l : outNode.getOutLinks().values()) {
				if (canPassLink(l)) {
					Node n = l.getToNode();
					PreProcessDijkstra.DeadEndData ddData = getPreProcessData(n);

					/*
					 * IF the current node n is not in a dead end OR it is in
					 * the same dead end as the fromNode OR it is in the same
					 * dead end as the toNode THEN we add the current node to
					 * the pending nodes
					 */
					if (ddData.getDeadEndEntryNode() == null
							|| ddOutData.getDeadEndEntryNode() != null
							|| deadEndEntryNode != null
							&& deadEndEntryNode.getId() == ddData
									.getDeadEndEntryNode().getId()) {
						// V-----------TESTS-----------V
						if (prevLink != null) {
							double factor = getTightTurnFactor(prevLink
									.getFromNode().getCoord(), n.getCoord(),
									outNode.getCoord());
							currCost *= factor;
						}
						// A-----------TESTS-----------A
						addToPendingNodes(l, n, pendingNodes, currTime,
								currCost, toNode);
					}
				}
			}
		} else { // this.pruneDeadEnds == false
			for (Link l : outNode.getOutLinks().values()) {
				if (canPassLink(l)) {
					// V-----------TESTS-----------V
					Node n = l.getToNode();

					if (prevLink != null) {
						double factor = getTightTurnFactor(prevLink
								.getFromNode().getCoord(), n.getCoord(),
								outNode.getCoord());
						currCost *= factor;
					}
					// A-----------TESTS-----------A
					addToPendingNodes(l, n, pendingNodes, currTime, currCost,
							toNode);
				}
			}
		}
	}

	protected double getTightTurnFactor(Coord fromCoord, Coord nextCoord,
			Coord outCoord) {
		return (3d + SimpleTrigonometric.getCosineCFrom3Coords(fromCoord,
				nextCoord, outCoord)) / 2d;
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
	 *         (e.g. when the same node already has an earlier visiting time).
	 */
	protected boolean addToPendingNodes(final Link l, final Node n,
			final PseudoRemovePriorityQueue<Node> pendingNodes,
			final double currTime, final double currCost, final Node toNode) {

		double travelTime = timeFunction.getLinkTravelTime(l, currTime);
		double travelCost = costFunction.getLinkGeneralizedTravelCost(l,
				currTime);
		DijkstraNodeData data = getData(n);
		double nCost = data.getCost();
		if (!data.isVisited(getIterationId())) {
			visitNode(n, data, pendingNodes, currTime + travelTime, currCost
					+ travelCost, l);
			return true;
		}
		double totalCost = currCost + travelCost;
		if (totalCost < nCost) {
			revisitNode(n, data, pendingNodes, currTime + travelTime,
					totalCost, l);
			return true;
		}

		return false;
	}

	/**
	 * @param link
	 * @return <code>true</code> if the link can be passed with respect to a
	 *         possible mode restriction set
	 * 
	 * @see #setModeRestriction(Set)
	 */
	protected boolean canPassLink(final Link link) {
		if (modeRestriction == null) {
			return true;
		}
		for (String mode : modeRestriction) {
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
	void revisitNode(final Node n, final DijkstraNodeData data,
			final PseudoRemovePriorityQueue<Node> pendingNodes,
			final double time, final double cost, final Link outLink) {
		pendingNodes.remove(n);

		data.visit(outLink, cost, time, getIterationId());
		pendingNodes.add(n, getPriority(data));
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
			final PseudoRemovePriorityQueue<Node> pendingNodes,
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
			iterationID = Integer.MIN_VALUE + 1;
			resetNetworkVisited();
		} else {
			iterationID++;
		}
	}

	/**
	 * @return iterationID
	 */
	/* package */int getIterationId() {
		return iterationID;
	}

	/**
	 * Resets all nodes in the network as if they have not been visited yet.
	 */
	private void resetNetworkVisited() {
		for (Node node : network.getNodes().values()) {
			DijkstraNodeData data = getData(node);
			data.resetVisited();
		}
	}

	/**
	 * The value used to sort the pending nodes during routing. This
	 * implementation compares the total effective travel cost to sort the nodes
	 * in the pending nodes queue during routing.
	 */
	protected double getPriority(final DijkstraNodeData data) {
		return data.getCost();
	}

	/**
	 * Returns the data for the given node. Creates a new NodeData if none
	 * exists yet.
	 * 
	 * @param n
	 *            The Node for which to return the data.
	 * @return The data for the given Node
	 */
	protected DijkstraNodeData getData(final Node n) {
		DijkstraNodeData r = nodeData.get(n.getId());
		if (null == r) {
			r = new DijkstraNodeData();
			nodeData.put(n.getId(), r);
		}
		return r;
	}

	protected PreProcessDijkstra.DeadEndData getPreProcessData(final Node n) {
		return preProcessData.getNodeData(n);
	}

	/**
	 * A data structure to store temporarily information used by the
	 * DijkstraWithTightTurnPenalty-algorithm.
	 */
	protected static class DijkstraNodeData {

		private Link prev = null;

		private double cost = 0;

		private double time = 0;

		private int iterationID = Integer.MIN_VALUE;

		public void resetVisited() {
			iterationID = Integer.MIN_VALUE;
		}

		public void visit(final Link comingFrom, final double cost,
				final double time, final int iterID) {
			prev = comingFrom;
			this.cost = cost;
			this.time = time;
			iterationID = iterID;
		}

		public boolean isVisited(final int iterID) {
			return iterID == iterationID;
		}

		public double getCost() {
			return cost;
		}

		public double getTime() {
			return time;
		}

		public Link getPrevLink() {
			return prev;
		}
	}

}