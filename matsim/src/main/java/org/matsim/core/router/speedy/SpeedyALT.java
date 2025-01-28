package org.matsim.core.router.speedy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A very fast implementation of the ALT algorithm (A*-search with Landmarks and Triangle Inequality).
 *
 * Based on "Computing the Shortest Path: A* Search Meets Graph Theory" by Andrew V. Goldberg and Chris Harrelson, 2005.
 *
 * This implementation always looks at all landmarks and does not filter them, as performance measurements
 * suggest that selecting and re-selecting landmarks regularly actually results in an overhead compared
 * to just calculate the values for each landmark in each step (using a typical value of 16 landmarks).
 * This might be due to the fact that all values for each landmark are just next to each other in the memory,
 * so when accessing the travelcosts to/from one landmark basically already loads the values of all landmarks in
 * the CPU cache, making the calculation for the remaining landmarks very fast.
 *
 * This implementation is not thread-safe. In the case of multi-threading, every thread should use
 * a separate instance. (But the used {@link SpeedyALTData} is thread-safe and can be shared by multiple
 * instances).
 *
 * @author mrieser / Simunto, sponsored by SBB Swiss Federal Railways
 */
public class SpeedyALT implements LeastCostPathCalculator {

	private final static Logger LOG = LogManager.getLogger(SpeedyALT.class);

	private final SpeedyGraph graph;
	private final SpeedyALTData astarData;
	private final TravelTime tt;
	private final TravelDisutility td;
	private final double[] data; // 3 entries per node: cost to node, time, distance
	private int currentIteration = Integer.MIN_VALUE;
	private final int[] iterationIds;
	private final int[] comingFrom;
	private final int[] usedLink;
	private final SpeedyGraph.LinkIterator outLI;
	private final DAryMinHeap pq;

	public SpeedyALT(SpeedyALTData astarData, TravelTime tt, TravelDisutility td) {
		this.graph = astarData.graph;
		this.astarData = astarData;
		this.tt = tt;
		this.td = td;
		this.data = new double[this.graph.nodeCount * 3];
		this.iterationIds = new int[this.graph.nodeCount];
		this.comingFrom = new int[this.graph.nodeCount];
		this.usedLink = new int[this.graph.nodeCount];
		this.pq = new DAryMinHeap(this.graph.nodeCount, 6);
		this.outLI = this.graph.getOutLinkIterator();
		Arrays.fill(this.iterationIds, this.currentIteration);
	}

	public double getCost(int nodeIndex) {
		return this.data[nodeIndex * 3];
	}

	private double getTimeRaw(int nodeIndex) {
		return this.data[nodeIndex * 3 + 1];
	}

	private double getDistance(int nodeIndex) {
		return this.data[nodeIndex * 3 + 2];
	}

	private void setData(int nodeIndex, double cost, double time, double distance) {
		int index = nodeIndex * 3;
		this.data[index] = cost;
		this.data[index + 1] = time;
		this.data[index + 2] = distance;
		this.iterationIds[nodeIndex] = this.currentIteration;
	}

	@Override
	public Path calcLeastCostPath(Node startNode, Node endNode, double startTime, Person person, Vehicle vehicle) {
		this.currentIteration++;
		if (this.currentIteration == Integer.MAX_VALUE) {
			// reset iteration as we overflow
			Arrays.fill(this.iterationIds, this.currentIteration);
			this.currentIteration = Integer.MIN_VALUE;
		}
		boolean hasTurnRestrictions = this.graph.hasTurnRestrictions();
		int startNodeIndex = startNode.getId().index();
		int endNodeIndex = endNode.getId().index();

		int startDeadend = this.astarData.getNodeDeadend(startNodeIndex);
		int endDeadend = this.astarData.getNodeDeadend(endNodeIndex);

		double estimation = estimateMinTravelcostToDestination(startNodeIndex, endNodeIndex);

		this.comingFrom[startNodeIndex] = -1;
		setData(startNodeIndex, 0, startTime, 0);
		this.pq.clear();
		this.pq.insert(startNodeIndex, 0 + estimation);
		boolean foundEndNode = false;

		while (!this.pq.isEmpty()) {
			final int nodeIdx = this.pq.poll();
			if (nodeIdx == endNodeIndex) {
				foundEndNode = true;
				break;
			}
			// if turn restrictions are used, we might be on a colored node, so check for the original node
			if (hasTurnRestrictions && this.graph.getNode(nodeIdx).getId().index() == endNodeIndex) {
				foundEndNode = true;
				endNodeIndex = nodeIdx;
				break;
			}

			// ignore dead-ends
			int deadend = this.astarData.getNodeDeadend(nodeIdx);
			if (deadend >= 0 && deadend != startDeadend && deadend != endDeadend) {
				continue; // it's a dead-end we're not interested in
			}

			double currTime = getTimeRaw(nodeIdx);
			double currCost = getCost(nodeIdx);
			double currDistance = getDistance(nodeIdx);

			this.outLI.reset(nodeIdx);
			while (this.outLI.next()) {
				int linkIdx = this.outLI.getLinkIndex();
				Link link = this.graph.getLink(linkIdx);
				int toNode = this.outLI.getToNodeIndex();

				double travelTime = this.tt.getLinkTravelTime(link, currTime, person, vehicle);
				double newTime = currTime + travelTime;
				double travelCost = this.td.getLinkTravelDisutility(link, currTime, person, vehicle);
				double newCost = currCost + travelCost;

				if (this.iterationIds[toNode] == this.currentIteration) {
					// this node was already visited in this route-query
					double oldCost = getCost(toNode);
					if (newCost < oldCost) {
						estimation = estimateMinTravelcostToDestination(toNode, endNodeIndex);
						this.pq.decreaseKey(toNode, newCost + estimation);
						setData(toNode, newCost, newTime, currDistance + link.getLength());
						this.comingFrom[toNode] = nodeIdx;
						this.usedLink[toNode] = linkIdx;
					}
				} else {
					estimation = estimateMinTravelcostToDestination(toNode, endNodeIndex);
					setData(toNode, newCost, newTime, currDistance + link.getLength());
					this.pq.insert(toNode, newCost + estimation);
					this.comingFrom[toNode] = nodeIdx;
					this.usedLink[toNode] = linkIdx;
				}
			}
		}

		if (foundEndNode) {
			return constructPath(endNodeIndex, startTime);
		}
		LOG.warn("No route was found from node " + startNode.getId() + " to node " + endNode.getId() + ". Some possible reasons:");
		LOG.warn("  * Network is not connected.  Run NetworkCleaner().") ;
		LOG.warn("  * Network for considered mode does not even exist.  Modes need to be entered for each link in network.xml.");
		LOG.warn("  * Network for considered mode is not connected to starting or ending point of route.  Setting insertingAccessEgressWalk to true may help.");
		LOG.warn("This will now return null, but it may fail later with a NullPointerException.");
		return null;
	}

	private double estimateMinTravelcostToDestination(int nodeIdx, int destinationIdx) {
		/* The ALT algorithm uses two lower bounds for each Landmark:
		 * given: source node S, target node T, landmark L
		 * then, due to the triangle inequality:
		 *  1) ST + TL >= SL --> ST >= SL - TL
		 *  2) LS + ST >= LT --> ST >= LT - LS
		 * The algorithm is interested in the largest possible value of (SL-TL) and (LT-LS),
		 * as this gives the closest approximation for the minimal travel time required to
		 * go from S to T.
		 */
		double best = 0;
		for (int i = 0, n = this.astarData.getLandmarksCount(); i < n; i++) {
			double estimate = estimateMinTravelcostToDestinationForLandmark(nodeIdx, destinationIdx, i);
			if (estimate > best) {
				best = estimate;
			}
		}
		return best;
	}

	private double estimateMinTravelcostToDestinationForLandmark(int nodeIdx, int destinationIdx, int landmarkIdx) {
		double sl = this.astarData.getTravelCostToLandmark(nodeIdx, landmarkIdx);
		double ls = this.astarData.getTravelCostFromLandmark(nodeIdx, landmarkIdx);
		double tl = this.astarData.getTravelCostToLandmark(destinationIdx, landmarkIdx);
		double lt = this.astarData.getTravelCostFromLandmark(destinationIdx, landmarkIdx);
		double sltl = sl - tl;
		double ltls = lt - ls;
		return Math.max(sltl, ltls);
	}

	private Path constructPath(int endNodeIndex, double startTime) {
		double travelCost = getCost(endNodeIndex);
		double arrivalTime = getTimeRaw(endNodeIndex);
		if (Double.isInfinite(arrivalTime)) {
			throw new RuntimeException("Undefined time on end node");
		}
		double travelTime = arrivalTime - startTime;

		List<Node> nodes = new ArrayList<>();
		List<Link> links = new ArrayList<>();

		int nodeIndex = endNodeIndex;

		nodes.add(this.graph.getNode(nodeIndex));

		int linkIndex = this.usedLink[nodeIndex];
		nodeIndex = this.comingFrom[nodeIndex];

		while (nodeIndex >= 0) {
			nodes.add(this.graph.getNode(nodeIndex));
			links.add(this.graph.getLink(linkIndex));

			linkIndex = this.usedLink[nodeIndex];
			nodeIndex = this.comingFrom[nodeIndex];
		}

		Collections.reverse(nodes);
		Collections.reverse(links);

		return new Path(nodes, links, travelTime, travelCost);
	}

}
