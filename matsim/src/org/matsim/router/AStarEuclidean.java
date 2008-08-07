/* *********************************************************************** *
 * project: org.matsim.*
 * AStarEuclidean.java
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

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.router.util.PreProcessEuclidean;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

/**
 * Implements the <a href="http://en.wikipedia.org/wiki/A%2A">A* router algorithm</a>
 * for a given NetworkLayer by using the euclidean distance divided by the
 * maximal free speed per length unit as the heuristic estimate during routing.
 *
 * AStarEuclidean about 3 times faster than Dijkstra.<br />
 *
 * <p>For every router, there exists a class which computes some
 * preprocessing data and is passed to the router class
 * constructor in order to accelerate the routing procedure.
 * The one used for AStarEuclidean is org.matsim.demandmodeling.router.util.PreProcessEuclidean.<br />
 *
 * Network conditions:<br />
 * <ul>
 * <li>The same as for Dijkstra: The link cost must be non-negative,
 * otherwise Dijkstra does not work.</li>
 * <li>The length stored in the links must be greater or equal to the euclidean distance of the
 * link's start and end node, otherwise the algorithm is not guaranteed to deliver least-cost paths.
 * In this case PreProcessEuclidean gives out a warning message.</li>
 * <li>The CostCalculator which calculates the cost for each link must implement the TravelMinCost
 * interface, i.e. it must implement the function getLinkMinimumTravelCost(Link link).
 * The TravelTimeCalculator class does implement it.</li></p>
 * <p><code> PreProcessEuclidean.run() </code>is very fast and needs (almost) no additional
 * memory.<code><br />
 * </code> Code Example:<code><br />
 * TravelMinCost costFunction = ...<br />
 * PreProcessEuclidean preProcessData = new PreProcessEuclidean(costFunction);<br />
 * preProcessData.run(network);<br />...<br />
 * LeastCostPathCalculator routingAlgo = new AStarEuclidean(network, preProcessData);<br />
 * ...</code></p>
 * <p>A note about the so-called overdo factor: You can drastically accelerate the routing of
 * AStarEuclidean by providing an overdo factor &gt; 1 (e.g. 1.5, 2 or 3). In this case,
 * AStarEuclidean does not calculate least-cost paths anymore but tends to deliver distance-minimal
 * paths. The greater the overdo factor, the faster the algorithm but the more the calculated routes
 * diverge from the least-cost ones.<br />
 * A typical invocation then looks like this:<br/>
 * <code>LeastCostPathCalculator routingAlgo = new AStarEuclidean(network, preProcessData, 2);</code>
 * <br />
 * @see org.matsim.router.util.PreProcessEuclidean
 * @see org.matsim.router.Dijkstra
 * @see org.matsim.router.AStarLandmarks
 * @author lnicolas
 */
public class AStarEuclidean extends Dijkstra {

	double overdoFactor;

	private double minTravelCostPerLength;
	final private HashMap<Id, AStarNodeData> nodeData;

	/**
	 * Default constructor; sets the overdo factor to 1.
	 * @param network
	 * @param preProcessData
	 * @param timeFunction
	 */
	public AStarEuclidean(NetworkLayer network, PreProcessEuclidean preProcessData,
			TravelTime timeFunction) {
		this(network, preProcessData, timeFunction, 1);
	}

	/**
	 * @param network Where we do the routing.
	 * @param preProcessData The pre-process data (containing the landmarks etc.).
	 * @param timeFunction Determines the travel time on each link.
	 * @param overdoFactor The factor which is multiplied with the output of the
	 * A* heuristic function. The higher the overdo factor the greedier the router,
	 * i.e. it visits less nodes during routing and is thus faster, but for an
	 * overdo factor > 1, it is not guaranteed that the router returns the
	 * least-cost paths. Rather it tends to return distance-minimal paths.
	 */
	public AStarEuclidean(NetworkLayer network, PreProcessEuclidean preProcessData,
			TravelTime timeFunction, double overdoFactor) {
		this(network, preProcessData, preProcessData.getCostFunction(),
				timeFunction, overdoFactor);

	}

	/**
	 * @param network Where we do the routing.
	 * @param preProcessData The pre-process data (containing the landmarks etc.).
	 * @param timeFunction Determines the travel time on each link.
	 * @param costFunction Calculates the travel cost on links.
	 * @param overdoFactor The factor which is multiplied with the output of the
	 * A* heuristic function. The higher the overdo factor the greedier the router,
	 * i.e. it visits less nodes during routing and is thus faster, but for an
	 * overdo factor > 1, it is not guaranteed that the router returns the
	 * least-cost paths. Rather it tends to return distance-minimal paths.
	 */
	public AStarEuclidean(NetworkLayer network,
			PreProcessEuclidean preProcessData,
			TravelCost costFunction, TravelTime timeFunction, double overdoFactor) {
		super(network, costFunction, timeFunction, preProcessData);

		setMinTravelCostPerLength(preProcessData.getMinTravelCostPerLength());

		this.nodeData = new HashMap<Id, AStarNodeData>((int)(network.getNodes().size() * 1.1), 0.95f);
		this.comparator = new ComparatorAStarNodeCost(this.nodeData);
		this.overdoFactor = overdoFactor;
	}

	/**
	 * Initializes the first node of a route.
	 *
	 * @param fromNode
	 *            The Node to be initialized.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param pendingNodes
	 *            The pending nodes so far.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            The time we start routing.
	 */
	@Override
	void initFromNode(final Node fromNode, final Node toNode, final double startTime, final PriorityQueue<Node> pendingNodes) {
		AStarNodeData data = getData(fromNode);
		visitNode(fromNode, data, pendingNodes, startTime, 0, null);
		data.setExpectedRemainingCost(estimateRemainingTravelCost(fromNode, toNode));
	}

	/**
	 * @see org.matsim.router.Dijkstra#addToPendingNodes(org.matsim.network.LinkImpl, org.matsim.network.Node, org.matsim.router.util.PriorityQueue, double, double, org.matsim.network.Node, org.matsim.network.Node)
	 */
	@Override
	protected
	boolean addToPendingNodes(final Link l, final Node n, final PriorityQueue<Node> pendingNodes,
			final double currTime, final double currCost, final Node outNode, final Node toNode) {

		double travelTime = this.timeFunction.getLinkTravelTime(l, currTime);
		double travelCost = this.costFunction.getLinkTravelCost(l, currTime);
		AStarNodeData data = getData(n);
		double nCost = data.getCost();
		if (!data.isVisited(getIterationID())) {
			double remainingTravelCost = estimateRemainingTravelCost(n, toNode);
			visitNode(n, data, pendingNodes, currTime + travelTime, currCost
					+ travelCost, remainingTravelCost, outNode);
			return true;
		} else if (currCost + travelCost < nCost) {
			revisitNode(n, data, pendingNodes, currTime + travelTime, currCost
					+ travelCost, outNode);
			return true;
		}
		return false;
	}

	/**
	 * Inserts the given Node n into the pendingNodes queue
	 * and updates its time and cost information.
	 * @param n The Node that is revisited.
	 * @param data The data for node.
	 * @param pendingNodes The nodes visited and not processed yet.
	 * @param time The time of the visit of n.
	 * @param cost The accumulated cost at the time of the visit of n.
	 * @param expectedRemainingCost The expected remaining travel cost when
	 * traveling from n to the target node of the route.
	 * @param outNode The node from which we came visiting n.
	 */
	private void visitNode(final Node n, final AStarNodeData data,
			final PriorityQueue<Node> pendingNodes, final double time, final double cost,
			final double expectedRemainingCost, final Node outNode) {
		data.setExpectedRemainingCost(expectedRemainingCost);
		visitNode(n, data, pendingNodes, time, cost, outNode);
	}

	/**
	 * @return The overdo factor used.
	 */
	public double getOverdoFactor() {
		return this.overdoFactor;
	}

	/**
	 * Estimates the remaining travel cost from fromNode to toNode
	 * using the euclidean distance between them.
	 * @param fromNode The first node.
	 * @param toNode The second node.
	 * @return The travel cost when traveling between the two given nodes.
	 */
	double estimateRemainingTravelCost(Node fromNode, Node toNode) {
		double dist = fromNode.getCoord().calcDistance(toNode.getCoord())
				* getMinTravelCostPerLength();
		return dist * this.overdoFactor;
	}

	/**
	 * Returns the data for the given Node. Creates a new AStarNodeData if none
	 * exists yet.
	 *
	 * @param n The node for which to return the data for..
	 * @return The data to the given Node
	 */
	@Override
	protected AStarNodeData getData(Node n) {
		AStarNodeData r = this.nodeData.get(n.getId());
		if (null == r) {
			r = new AStarNodeData();
			this.nodeData.put(n.getId(), r);
		}
		return r;
	}

	/**
	 * Sets minTravelCostPerLength to the given value.
	 * @param minTravelCostPerLength
	 *            the minTravelCostPerLength to set
	 */
	void setMinTravelCostPerLength(double minTravelCostPerLength) {
		this.minTravelCostPerLength = minTravelCostPerLength;
	}

	/**
	 * Returns the minimal travel cost per length unit on a link in the
	 * network.
	 * @return the minimal travel cost per length unit on a link in the
	 * network.
	 */
	public double getMinTravelCostPerLength() {
		return this.minTravelCostPerLength;
	}

	/**
	 * @see org.matsim.router.Dijkstra#printInformation()
	 */
	@Override
	public void printInformation() {
		System.out.println("Used an overdo factor of " + this.overdoFactor);
		super.printInformation();
	}

	/**
	 * Holds AStarEuclidean specific information used during routing
	 * associated with each node in the network.
	 */
	class AStarNodeData extends DijkstraNodeData {

		private double expectedRemainingCost;

		/**
		 * @return The expected total travel cost from the start
		 * node to the target node of the route when the associated node
		 * is on that route.
		 */
		public double getExpectedCost() {
			return this.expectedRemainingCost + getCost();
		}

		/**
		 * Sets the expected travel cost from the associated
		 * node to the target node of the route.
		 *
		 * @param expectedCost the expected cost
		 */
		public void setExpectedRemainingCost(double expectedCost) {
			this.expectedRemainingCost = expectedCost;
		}

		/**
		 * @return The expected travel cost from the associated
		 * node to the target node of the route.
		 */
		public double getExpectedRemainingCost() {
			return this.expectedRemainingCost;
		}
	};

	/**
	 * The comparator used to sort the pending nodes during routing.
	 * This comparator compares the total estimated remaining travel cost
	 * to sort the nodes in the pending nodes queue during routing.
	 * @author lnicolas
	 */
	public static class ComparatorAStarNodeCost extends ComparatorDijkstraCost {

		private static final long serialVersionUID = 1L;

		/**
		 * @param data A map to look up the data with information about cost.
		 */
		public ComparatorAStarNodeCost(Map<Id, AStarNodeData> data) {
			super(data);
		}

		/**
		 * @see org.matsim.router.Dijkstra.ComparatorDijkstraCost#getCost(org.matsim.network.Node)
		 */
		@Override
		protected double getCost(final Node node) {
			return ((AStarNodeData)this.nodeData.get(node.getId())).getExpectedCost();
		}
	}
}
