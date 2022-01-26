package org.matsim.core.router.speedy;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * This is a copy of SpeedyALT adapted for many-to-one search.
 */
public class SpeedyMultiSourceALT {

	private final static Logger LOG = LogManager.getLogger(SpeedyMultiSourceALT.class);

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
	private final SpeedyGraph.LinkIterator inLI;
	private final DAryMinHeap pq;

	public SpeedyMultiSourceALT(SpeedyALTData astarData, TravelTime tt, TravelDisutility td) {
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
		this.inLI = this.graph.getInLinkIterator();
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

	public static class StartNode {
		public final Node node;
		public final double cost;
		public final double time;

		public StartNode(Node node, double cost, double time) {
			this.node = node;
			this.cost = cost;
			this.time = time;
		}
	}

	public Path calcLeastCostPath(Collection<StartNode> startNodes, Node endNode, Person person, Vehicle vehicle,
			boolean backward) {
		this.currentIteration++;
		if (this.currentIteration == Integer.MAX_VALUE) {
			// reset iteration as we overflow
			Arrays.fill(this.iterationIds, this.currentIteration);
			this.currentIteration = Integer.MIN_VALUE;
		}

		int endNodeIndex = endNode.getId().index();
		// TODO add support for dead ends
		//  int endDeadend = this.astarData.getNodeDeadend(endNodeIndex);

		this.pq.clear();
		for (StartNode startNode : startNodes) {
			int startNodeIndex = startNode.node.getId().index();
			// TODO add support for dead ends
			//  int startDeadend = this.astarData.getNodeDeadend(startNodeIndex);
			double estimation = estimateMinTravelcostToDestination(startNodeIndex, endNodeIndex, backward);

			this.comingFrom[startNodeIndex] = -1;
			setData(startNodeIndex, startNode.cost, startNode.time, 0);
			this.pq.insert(startNodeIndex, startNode.cost + estimation);
		}

		boolean foundEndNode = false;

		while (!this.pq.isEmpty()) {
			final int nodeIdx = this.pq.poll();
			if (nodeIdx == endNodeIndex) {
				foundEndNode = true;
				break;
			}

			// TODO add support for dead ends
			//	int deadend = this.astarData.getNodeDeadend(nodeIdx);
			//	if (deadend >= 0 && deadend != startDeadend && deadend != endDeadend) {
			//		continue; // it's a dead-end we're not interested in
			//	}

			double currTime = getTimeRaw(nodeIdx);
			double currCost = getCost(nodeIdx);
			double currDistance = getDistance(nodeIdx);

			var li = backward ? this.inLI : this.outLI;
			li.reset(nodeIdx);
			while (li.next()) {
				int linkIdx = li.getLinkIndex();
				Link link = this.graph.getLink(linkIdx);
				int toNode = backward ? li.getFromNodeIndex() : li.getToNodeIndex();

				double travelTime = this.tt.getLinkTravelTime(link, currTime, person, vehicle);
				double newTime = backward ? currTime - travelTime : currTime + travelTime;
				double travelCost = this.td.getLinkTravelDisutility(link, currTime, person, vehicle);
				double newCost = currCost + travelCost;

				if (this.iterationIds[toNode] == this.currentIteration) {
					// this node was already visited in this route-query
					double oldCost = getCost(toNode);
					if (newCost < oldCost) {
						double estimation = estimateMinTravelcostToDestination(toNode, endNodeIndex, backward);
						this.pq.decreaseKey(toNode, newCost + estimation);
						setData(toNode, newCost, newTime, currDistance + link.getLength());
						this.comingFrom[toNode] = nodeIdx;
						this.usedLink[toNode] = linkIdx;
					}
				} else {
					double estimation = estimateMinTravelcostToDestination(toNode, endNodeIndex, backward);
					setData(toNode, newCost, newTime, currDistance + link.getLength());
					this.pq.insert(toNode, newCost + estimation);
					this.comingFrom[toNode] = nodeIdx;
					this.usedLink[toNode] = linkIdx;
				}
			}
		}

		if (foundEndNode) {
			return constructPath(endNodeIndex, backward);
		}
		LOG.warn("No route was found from nodes [" + startNodes.stream()
				.map(n -> n.node.getId() + "")
				.collect(joining(",")) + "] to node " + endNode.getId() + ". Some possible reasons:");
		LOG.warn("  * Network is not connected.  Run NetworkCleaner().");
		LOG.warn(
				"  * Network for considered mode does not even exist.  Modes need to be entered for each link in network.xml.");
		LOG.warn(
				"  * Network for considered mode is not connected to starting or ending point of route.  Setting insertingAccessEgressWalk to true may help.");
		LOG.warn("This will now return null, but it may fail later with a NullPointerException.");
		return null;
	}

	private double estimateMinTravelcostToDestination(int nodeIdx, int destinationIdx, boolean backward) {
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
			double estimate = backward ?
					estimateMinTravelcostToDestinationForLandmarkbackward(nodeIdx, destinationIdx, i) :
					estimateMinTravelcostToDestinationForLandmark(nodeIdx, destinationIdx, i);
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

	private double estimateMinTravelcostToDestinationForLandmarkbackward(int nodeIdx, int destinationIdx,
			int landmarkIdx) {
		double sl = this.astarData.getTravelCostFromLandmark(nodeIdx, landmarkIdx);
		double ls = this.astarData.getTravelCostToLandmark(nodeIdx, landmarkIdx);
		double tl = this.astarData.getTravelCostFromLandmark(destinationIdx, landmarkIdx);
		double lt = this.astarData.getTravelCostToLandmark(destinationIdx, landmarkIdx);
		double sltl = sl - tl;
		double ltls = lt - ls;
		return Math.max(sltl, ltls);
	}

	private Path constructPath(int endNodeIndex, boolean backward) {
		double travelCost = getCost(endNodeIndex);
		double endTime = getTimeRaw(endNodeIndex);
		if (Double.isInfinite(endTime)) {
			throw new RuntimeException("Undefined time on end node");
		}

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

		double startTime;
		if (backward) {
			startTime = getTimeRaw(nodes.get(nodes.size() - 1).getId().index());
		} else {
			Collections.reverse(nodes);
			Collections.reverse(links);
			startTime = getTimeRaw(nodes.get(0).getId().index());
		}

		double travelTime = backward ? startTime - endTime : endTime - startTime;

		return new Path(nodes, links, travelTime, travelCost);
	}
}
