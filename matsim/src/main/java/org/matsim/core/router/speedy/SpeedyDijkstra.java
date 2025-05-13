package org.matsim.core.router.speedy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.turnRestrictions.TurnRestrictionsContext;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * A very fast implementation of Dijkstra's shortest path algorithm using a {@link SpeedyGraph}
 * data structure.
 *
 * @author mrieser / Simunto, sponsored by SBB Swiss Federal Railways
 */
public class SpeedyDijkstra implements LeastCostPathCalculator {

	private final static Logger LOG = LogManager.getLogger(SpeedyALT.class);

	private final SpeedyGraph graph;
	private final TravelTime tt;
	private final TravelDisutility td;
	private final double[] data; // 3 entries per node: time, cost, distance
	private int currentIteration = Integer.MIN_VALUE;
	private final int[] iterationIds;
	private final int[] comingFrom;
	private final int[] usedLink;
	private final SpeedyGraph.LinkIterator outLI;
	private final DAryMinHeap pq;

	public SpeedyDijkstra(SpeedyGraph graph, TravelTime tt, TravelDisutility td) {
		this.graph = graph;
		this.tt = tt;
		this.td = td;
		this.data = new double[graph.nodeCount * 3];
		this.iterationIds = new int[graph.nodeCount];
		this.comingFrom = new int[graph.nodeCount];
		this.usedLink = new int[graph.nodeCount];
		this.pq = new DAryMinHeap(graph.nodeCount, 6);
		this.outLI = graph.getOutLinkIterator();
	}

	private double getCost(int nodeIndex) {
		return this.data[nodeIndex * 3];
	}

	private double getTimeRaw(int nodeIndex) {
		return this.data[nodeIndex * 3 + 1];
	}

	public double getDistance(int nodeIndex) {
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
		int startNodeIndex = startNode.getId().index();
		int endNodeIndex = endNode.getId().index();
		Path path = calcLeastCostPathImpl(startNodeIndex, endNodeIndex, startTime, person, vehicle);

		if(path == null) {
			LOG.warn("No route was found from node " + startNode.getId() + " to node " + endNode.getId() + ". Some possible reasons:");
			LOG.warn("  * Network is not connected.  Run NetworkCleaner().");
			LOG.warn("  * Network for considered mode does not even exist.  Modes need to be entered for each link in network.xml.");
			LOG.warn("  * Network for considered mode is not connected to starting or ending point of route.  Setting insertingAccessEgressWalk to true may help.");
			LOG.warn("This will now return null, but it may fail later with a NullPointerException.");
		}
		return path;
	}


	public Path calcLeastCostPath(Link fromLink, Link toLink, double starttime, final Person person, final Vehicle vehicle) {

		int startNodeIndex = fromLink.getToNode().getId().index();
		int endNodeIndex = toLink.getFromNode().getId().index();

		if(graph.getTurnRestrictions().isPresent()) {
			Map<Id<Link>, TurnRestrictionsContext.ColoredLink> replacedLinks = graph.getTurnRestrictions().get().replacedLinks;
			if(replacedLinks.containsKey(fromLink.getId())) {
				startNodeIndex = replacedLinks.get(fromLink.getId()).toColoredNode.index();
			}
		}

		Path path = calcLeastCostPathImpl(startNodeIndex, endNodeIndex, starttime, person, vehicle);
		if(path == null) {
			LOG.warn("No route was found from link " + fromLink.getId() + " to link " + toLink.getId() + ". Some possible reasons:");
			LOG.warn("  * Network is not connected.  Run NetworkCleaner().");
			LOG.warn("  * Network for considered mode does not even exist.  Modes need to be entered for each link in network.xml.");
			LOG.warn("  * Network for considered mode is not connected to starting or ending point of route.  Setting insertingAccessEgressWalk to true may help.");
			LOG.warn("This will now return null, but it may fail later with a NullPointerException.");
		}
		return path;
	}

	private Path calcLeastCostPathImpl(int startNodeIndex, int endNodeIndex, double startTime, Person person, Vehicle vehicle) {
		this.currentIteration++;
		if (this.currentIteration == Integer.MAX_VALUE) {
			// reset iteration as we overflow
			Arrays.fill(this.iterationIds, this.currentIteration);
			this.currentIteration = Integer.MIN_VALUE;
		}

		boolean hasTurnRestrictions = this.graph.getTurnRestrictions().isPresent();

		this.comingFrom[startNodeIndex] = -1;
		setData(startNodeIndex, 0, startTime, 0);
		this.pq.clear();
		this.pq.insert(startNodeIndex, 0);
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

			double currTime = getTimeRaw(nodeIdx);
			if (Double.isInfinite(currTime)) {
				throw new RuntimeException("Undefined Time");
			}
			double currCost = getCost(nodeIdx);
			double currDistance = getDistance(nodeIdx);

			this.outLI.reset(nodeIdx);
			while (this.outLI.next()) {
				int linkIdx = this.outLI.getLinkIndex();
				Link link = this.graph.getLink(linkIdx);
				int toNode = this.outLI.getToNodeIndex();

				double travelTime = this.tt.getLinkTravelTime(link, currTime, person, vehicle);
				double newTime = currTime + travelTime;
				double newCost = currCost + this.td.getLinkTravelDisutility(link, currTime, person, vehicle);

				if (this.iterationIds[toNode] == this.currentIteration) {
					// this node was already visited in this route-query
					double oldCost = getCost(toNode);
					if (newCost < oldCost) {
						this.pq.decreaseKey(toNode, newCost);
						setData(toNode, newCost, newTime, currDistance + link.getLength());
						this.comingFrom[toNode] = nodeIdx;
						this.usedLink[toNode] = linkIdx;
					}
				} else {
					setData(toNode, newCost, newTime, currDistance + link.getLength());
					this.pq.insert(toNode, newCost);
					this.comingFrom[toNode] = nodeIdx;
					this.usedLink[toNode] = linkIdx;
				}
			}
		}

		if (foundEndNode) {
			return constructPath(endNodeIndex, startTime);
		}
		return null;
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
