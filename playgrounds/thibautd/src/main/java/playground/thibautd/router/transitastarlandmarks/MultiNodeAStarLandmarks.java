/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.router.transitastarlandmarks;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.AStarNodeData;
import org.matsim.core.router.util.DijkstraNodeData;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.PseudoRemovePriorityQueue;
import org.matsim.core.utils.collections.RouterPriorityQueue;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Prototype of a fast alternative to MultiNodeDijkstra.
 *
 * @author thibautd
 */
public class MultiNodeAStarLandmarks {

	/**
	 * Provides an unique id (loop number) for each routing request, so we don't
	 * have to reset all nodes at the beginning of each re-routing but can use the
	 * loop number instead.
	 */
	private int iterationID = Integer.MIN_VALUE + 1;

	private int[] activeLandmarkIndexes;

	/**
	 * The network on which we find routes.
	 */
	protected Network network;

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	final TravelDisutility costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and time step.
	 */
	final TravelTime timeFunction;

	final HashMap<Id<Node>, AStarNodeData> nodeData;
	private Person person = null;
	private Vehicle vehicle = null;
	private PreProcessLandmarks preprocess;
	private final double overdoFactor;
	private final int initiallyActiveLandmarks;

	private InternalLandmarkData fromData;
	private InternalLandmarkData toData;
	private int controlCounter = 0;

	private static final int CONTROL_INTERVAL = 40;

	public MultiNodeAStarLandmarks(
			final int initiallyActiveLandmarks,
			final double overdoFactor,
			final Network network,
			final PreProcessLandmarks preprocess,
			final TravelDisutility costFunction,
			final TravelTime timeFunction ) {
		if ( overdoFactor < 1 ) throw new IllegalArgumentException( "Overdo factor below 1 makes no sense!" );
		this.overdoFactor = overdoFactor;

		if ( initiallyActiveLandmarks < 1 ) throw new IllegalArgumentException( "initially active landmarks below 1 makes no sense!" );
		this.initiallyActiveLandmarks = initiallyActiveLandmarks;

		this.preprocess = preprocess;
		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this.nodeData = new HashMap<>( (int) ( network.getNodes().size() * 1.1 ), 0.95f );
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
	public LeastCostPathCalculator.Path calcLeastCostPath(
			final Iterable<InitialNode> fromNodes,
			final Iterable<InitialNode> toNodes,
			final Person person) {
		initializeActiveLandmarks(
				fromNodes,
				toNodes,
				Math.min(
						initiallyActiveLandmarks,
						preprocess.getLandmarks().length ) );
		this.person = person;

		final Set<Node> endNodes = new HashSet<>( );
		final Map<Node, InitialNode> toNodesMap = new HashMap<>( );
		for ( InitialNode n : toNodes ) {
			endNodes.add( n.node );
			toNodesMap.put( n.node , n );
		}
		augmentIterationId();

		final RouterPriorityQueue<Node> pendingNodes = (RouterPriorityQueue<Node>) createRouterPriorityQueue();
		for ( InitialNode from : fromNodes ) {
			final double cost =
				estimateRemainingTravelCost(
						new InternalLandmarkData( from.node ) );

			AStarNodeData data = getData( from.node );
			visitNode(
					from.node,
					data,
					pendingNodes,
					from.initialTime,
					from.initialCost,
					cost,
					null );
		}

		// find out which one is the cheapest end node
		double minCost = Double.POSITIVE_INFINITY;
		Node minCostNode = null;

		// do the real work
		while ( !endNodes.isEmpty() ) {
			final Node outNode = pendingNodes.poll();

			if (outNode == null) {
				// seems we have no more nodes left, but not yet reached all endNodes...
				break;
			}

			final DijkstraNodeData data = getData(outNode);
			final boolean isEndNode = endNodes.remove(outNode);
			if (isEndNode) {
				final InitialNode initData = toNodesMap.get(outNode);
				final double cost = data.getCost() + initData.initialCost;
				if (cost < minCost) {
					minCost = cost;
					minCostNode = outNode;
				}
			}

			if (data.getCost() > minCost) {
				break;
			}

			relaxNode(outNode, pendingNodes);

		}

		if (minCostNode == null) {
			return null;
		}
		Node toNode = minCostNode;

		// now construct the path
		List<Node> nodes = new LinkedList<>();
		List<Link> links = new LinkedList<>();

		nodes.add(0, toNode);
		Link tmpLink = getData(toNode).getPrevLink();
		while (tmpLink != null) {
			links.add(0, tmpLink);
			nodes.add(0, tmpLink.getFromNode());
			tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
		}
		DijkstraNodeData startNodeData = getData(nodes.get(0));
		DijkstraNodeData toNodeData = getData(toNode);
		return new LeastCostPathCalculator.Path(nodes, links, toNodeData.getTime() - startNodeData.getTime(), toNodeData.getCost() - startNodeData.getCost());
	}

	private void initializeActiveLandmarks(
			final Iterable<InitialNode> fromNodes,
			final Iterable<InitialNode> toNodes,
			final int actLandmarkCount) {
		fromData = new InternalLandmarkData( fromNodes );
		toData = new InternalLandmarkData( toNodes );

		// Sort the landmarks according to the accuracy of their
		// distance estimation they yield
		double[] estTravelTimes = new double[actLandmarkCount];
		this.activeLandmarkIndexes = new int[actLandmarkCount];
		for (int i = 0; i < estTravelTimes.length; i++) {
			estTravelTimes[i] = Time.UNDEFINED_TIME;
		}
		double tmpTravTime;
		for (int i = 0; i < preprocess.getLandmarks().length; i++) {
			tmpTravTime = estimateRemainingTravelCost(fromData, i);
			for (int j = 0; j < estTravelTimes.length; j++) {
				if (tmpTravTime > estTravelTimes[j]) {
					for (int k = estTravelTimes.length - 1; k > j; k--) {
						estTravelTimes[k] = estTravelTimes[k - 1];
						this.activeLandmarkIndexes[k] = this.activeLandmarkIndexes[k - 1];
					}
					estTravelTimes[j] = tmpTravTime;
					this.activeLandmarkIndexes[j] = i;
					break;
				}
			}
		}
	}

	private double estimateRemainingTravelCost(
			final InternalLandmarkData fromNode) {
		// avoid instanciation: only one instance
		double travCost = 0;
		for (int landmark : this.activeLandmarkIndexes) {
			final double tmpTravCost = estimateRemainingTravelCost(
					fromNode,
					landmark );
			// this is all lower bounds, so the highest the better
			if (tmpTravCost > travCost) {
				travCost = tmpTravCost;
			}
		}
		//return Math.min( travCost ,
		//		estimateRemainingTravelCostEuclidean(
		//				fromNode ) + toNode.initialCost );
		return travCost;
	}

	private double estimateRemainingTravelCostEuclidean(
			final Node fromNode,
			final Node toNode) {
		double dist = CoordUtils.calcDistance( fromNode.getCoord(), toNode.getCoord() )
				* preprocess.getMinTravelCostPerLength();
		return dist * this.overdoFactor;
	}

	/**
	 * Checks whether there is a landmark in the set of the non-active
	 * landmarks that yields a better estimation than the best active
	 * landmark. If there is, this landmark is added to the set of active
	 * landmark and its index is returned.
	 * @param fromNode The node for which we estimate the remaining travel
	 * time to the toNode.
	 * @return The index of the landmark that has been added to the set of
	 * active landmarks, or -1 if no landmark was added.
	 */
	private int checkToAddLandmark(final InternalLandmarkData fromNode) {
		if ( activeLandmarkIndexes.length == preprocess.getLandmarks().length ) {
			return -1;
		}

		double bestTravCostEst = estimateRemainingTravelCost(fromNode);

		int bestIndex = -1;
		for (int i = 0; i < preprocess.getLandmarks().length; i++) {
			// this also goes through active landmarks, which is kind of silly
			final double tmpTravTime = estimateRemainingTravelCost( fromNode , i);

			if (tmpTravTime > bestTravCostEst) {
				bestIndex = i;
				bestTravCostEst = tmpTravTime;
			}
		}

		if (bestIndex != -1) {
			final int[] newActiveLandmarks = new int[this.activeLandmarkIndexes.length + 1];
			System.arraycopy(this.activeLandmarkIndexes, 0, newActiveLandmarks, 0, this.activeLandmarkIndexes.length);
			newActiveLandmarks[this.activeLandmarkIndexes.length] = bestIndex;
			this.activeLandmarkIndexes = newActiveLandmarks;
		}
		return bestIndex;
	}

	/**
	 * Estimates the remaining travel cost from fromNode to toNode
	 * using the landmark given by index.
	 * @param fromRole The first node/role.
	 * @param index The index of the landmarks that should be used for
	 * the estimation of the travel cost.
	 * @return The travel cost when traveling between the two given nodes.
	 */
	protected double estimateRemainingTravelCost(
			final InternalLandmarkData fromRole,
			final int index) {
		// I have to admit I do not really undertand the deal with min and max here.
		// min and max are the min/max of the two possible directions.
		// This might have to do with trying to get the highest lower bound possible,
		// but I would have to pass some time with paper and pencil to check that...
		double tmpTravTime;
		final double fromMinLandmarkTravelTime = fromRole.getMinLandmarkTravelTime(index);
		final double toMaxLandmarkTravelTime = toData.getMaxLandmarkTravelTime(index);
		tmpTravTime = fromMinLandmarkTravelTime - toMaxLandmarkTravelTime;
		if (tmpTravTime < 0) {
			tmpTravTime = toData.getMinLandmarkTravelTime(index) - fromRole.getMaxLandmarkTravelTime(index);
			if (tmpTravTime <= 0) {
				return 0;
			}
		}
		return tmpTravTime * this.overdoFactor;
	}

	/**
	 * Allow replacing the RouterPriorityQueue.
	 */
	@SuppressWarnings("static-method")
	/*package*/ RouterPriorityQueue<? extends Node> createRouterPriorityQueue() {
		return new PseudoRemovePriorityQueue<>( 500 );
	}

	/**
	 * Inserts the given Node n into the pendingNodes queue and updates its time
	 * and cost information.
	 *  @param n
	 *            The Node that is revisited.
	 * @param data
	 *            The data for n.
	 * @param pendingNodes
	 *            The nodes visited and not processed yet.
	 * @param time
	 *            The time of the visit of n.
	 * @param cost
	 *            The accumulated cost at the time of the visit of n.
	 */
	protected void visitNode( final Node n, final AStarNodeData data,
			final RouterPriorityQueue<Node> pendingNodes, final double time, final double cost,
			double expectedRemainingCost, final Link outLink ) {
		data.setExpectedRemainingCost(expectedRemainingCost);
		data.visit(outLink, cost, time, getIterationId());
		pendingNodes.add(n, getPriority(data));
	}

	/**
	 * Expands the given Node in the routing algorithm; may be overridden in
	 * sub-classes.
	 *  @param outNode
	 *            The Node to be expanded.
	 * @param pendingNodes
	 */
	protected void relaxNode(final Node outNode, final RouterPriorityQueue<Node> pendingNodes) {
		this.controlCounter++;
		if ( this.controlCounter == CONTROL_INTERVAL ) {
			int newLandmarkIndex = checkToAddLandmark( new InternalLandmarkData( outNode ) );
			if (newLandmarkIndex >= 0) {
				updatePendingNodes(newLandmarkIndex, pendingNodes);
			}
			this.controlCounter = 0;
		}

		DijkstraNodeData outData = getData(outNode);
		double currTime = outData.getTime();
		double currCost = outData.getCost();
		for (Link l : outNode.getOutLinks().values()) {
			relaxNodeLogic(l, pendingNodes, currTime, currCost);
		}
	}

	/**
	 * If a landmark has been added to the set of the active landmarks,
	 * this function re-evaluates the estimated remaining travel time based
	 * on the new set of active landmarks of the nodes contained in
	 * pendingNodes. If this estimation improved, the node's position in
	 * the pendingNodes queue is updated.
	 * @param newLandmarkIndex The index of the new active landmark.
	 * @param pendingNodes The nodes visited so far.
	 */
	private void updatePendingNodes(
			final int newLandmarkIndex,
			final RouterPriorityQueue<Node> pendingNodes) {
		final List<Double> newEstRemTravCosts = new ArrayList<>();
		final List<Node> nodesToBeUpdated = new ArrayList<>();
		final InternalLandmarkData landmarkData = new InternalLandmarkData(  );

		for ( Node node : pendingNodes ) {
			final AStarNodeData role = getData(node);
			final PreProcessLandmarks.LandmarksData ppRole = preprocess.getNodeData(node);

			landmarkData.setDelegate( ppRole );
			final double newEstRemTravCost =
					estimateRemainingTravelCost(
							landmarkData,
							newLandmarkIndex);

			if (newEstRemTravCost > role.getExpectedRemainingCost() ) {
				nodesToBeUpdated.add(node);
				newEstRemTravCosts.add(newEstRemTravCost);
			}
		}

		for (Node node : nodesToBeUpdated) {
			pendingNodes.remove(node);
		}

		for (int i = 0; i < nodesToBeUpdated.size(); i++) {
			final Node node = nodesToBeUpdated.get(i);
			final AStarNodeData data = getData(node);
			data.setExpectedRemainingCost(newEstRemTravCosts.get(i));
			pendingNodes.add(node, getPriority(data));
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

	private boolean addToPendingNodes(
			final Link l,
			final Node n,
			final RouterPriorityQueue<Node> pendingNodes,
			final double currTime,
			final double currCost) {

		final double travelTime = this.timeFunction.getLinkTravelTime( l, currTime, person, vehicle );
		final double travelCost = this.costFunction.getLinkTravelDisutility(
				l,
				currTime,
				person,
				vehicle );
		final AStarNodeData data = getData( n );

		final double remainingTravelCost =
				estimateRemainingTravelCost(
						new InternalLandmarkData( n ) );
		if (!data.isVisited(getIterationId())) {
			visitNode(n,
					data,
					pendingNodes,
					currTime + travelTime,
					currCost + travelCost,
					remainingTravelCost,
					l);
			return true;
		}
		else if (currCost + travelCost + remainingTravelCost < data.getExpectedCost()) {
			revisitNode(
					n,
					data,
					pendingNodes,
					currTime + travelTime,
					currCost + travelCost,
					remainingTravelCost,
					l);
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
	void revisitNode(
			final Node n,
			final AStarNodeData data,
			final RouterPriorityQueue<Node> pendingNodes,
			final double time,
			final double cost,
			final double expectedRemainingCost,
			final Link outLink) {
		pendingNodes.remove(n);

		data.visit(outLink, cost, time, getIterationId());
		data.setExpectedRemainingCost( expectedRemainingCost );
		pendingNodes.add(n, getPriority(data));
	}

	/**
	 * The value used to sort the pending nodes during routing.
	 * This implementation compares the total effective travel cost
	 * to sort the nodes in the pending nodes queue during routing.
	 */
	private double getPriority(final AStarNodeData data) {
		return data.getExpectedCost();
	}

	public static class InitialNode {
		public final Node node;
		public final double initialCost;
		public final double initialTime;
		public InitialNode( final Node node, final double initialCost, final double initialTime ) {
			this.node = node;
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
	protected AStarNodeData getData(final Node n) {
		AStarNodeData r = this.nodeData.get(n.getId());
		if (null == r) {
			r = new AStarNodeData();
			this.nodeData.put(n.getId(), r);
		}
		return r;
	}

	/**
	 * Reconstructed landmark data for origin or destination "multi-node"
	 */
	private class InternalLandmarkData {
		private PreProcessLandmarks.LandmarksData delegate;
		private final double[] minTravelTimes;
		private final double[] maxTravelTimes;

		public InternalLandmarkData( final Node node ) {
			minTravelTimes = null;
			maxTravelTimes = null;
			this.delegate = preprocess.getNodeData( node );
		}

		public InternalLandmarkData( ) {
			minTravelTimes = null;
			maxTravelTimes = null;
			this.delegate = null;
		}

		public InternalLandmarkData(
				final Iterable<InitialNode> nodes ) {
			delegate = null;
			// store the MINIMUM of the min and max travel times (min and max are min and max between the two directions.
			// but they are both shortest paths. Not REALLY sure why this is done, but I am assuming the person who implemented
			// AStar in MATSim did more research on this than I did...) td
			minTravelTimes = new double[ preprocess.getLandmarks().length ];
			maxTravelTimes = new double[ preprocess.getLandmarks().length ];

			for ( int i=0; i < minTravelTimes.length; i++ ) {
				minTravelTimes[ i ] = Double.POSITIVE_INFINITY;
				maxTravelTimes[ i ] = Double.POSITIVE_INFINITY;
			}

			// This becomes a bit strange here: min and max are the min and max of both directions.
			// But now, as we parse several possible paths, they might correspond to the same direction.
			// I do not know the implications...
			for ( InitialNode n : nodes ) {
				final PreProcessLandmarks.LandmarksData data = preprocess.getNodeData( n.node );

				for ( int i=0; i < minTravelTimes.length; i++ ) {
					final double max = data.getMaxLandmarkTravelTime( i ) + n.initialCost;
					final double min = data.getMinLandmarkTravelTime( i ) + n.initialCost;

					if ( max < maxTravelTimes[ i ] ) maxTravelTimes[ i ] = max;
					if ( min < minTravelTimes[ i ] ) minTravelTimes[ i ] = min;
				}
			}
		}

		public void setDelegate( PreProcessLandmarks.LandmarksData delegate ) {
			this.delegate = delegate;
		}

		public double getMaxLandmarkTravelTime( int landmark ) {
			if ( delegate != null ) return delegate.getMaxLandmarkTravelTime( landmark );
			return maxTravelTimes[ landmark ];
		}

		public double getMinLandmarkTravelTime( int landmark ) {
			if ( delegate != null ) return delegate.getMinLandmarkTravelTime( landmark );
			return minTravelTimes[ landmark ];
		}
	}
}
