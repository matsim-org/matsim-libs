/* *********************************************************************** *
 * project: org.matsim.*
 * FastDijkstra.java
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

package playground.christoph.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.PseudoRemovePriorityQueue;

/**
 * A faster Implementation of org.matsim.core.router.Dijkstra.
 * By using extended, internal Data Structures we avoid lots of
 * Lookups in HashMaps (Id -> getLink, getNode).
 *
 * @see org.matsim.core.router.Dijkstra
 * @author cdobler
 */
//public class FastDijkstra implements LeastCostPathCalculator {
public class FastDijkstra extends Dijkstra {

	private final static Logger log = Logger.getLogger(FastDijkstra.class);

//	private Dijkstra dijsktra;

	/**
	 * The network on which we find routes.
	 */
	protected Network network;

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	final TravelCost costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and time step.
	 */
	final TravelTime timeFunction;

	final private HashMap<Id, DijkstraNode> nodeData;

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
	private final boolean pruneDeadEnds;

	/**
	 * Comparator that defines how to order the nodes in the pending nodes queue
	 * during routing.
	 */

	private final PreProcessDijkstra preProcessData;

//	private TransportMode[] modeRestriction = null;

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
	public FastDijkstra(final Network network, final TravelCost costFunction, final TravelTime timeFunction) {
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
	public FastDijkstra(final Network network, final TravelCost costFunction, final TravelTime timeFunction,
			final PreProcessDijkstra preProcessData) {

		super(network, costFunction, timeFunction, preProcessData);

//		this.dijsktra = new Dijkstra(network, costFunction, timeFunction, preProcessData);

		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;
		this.preProcessData = preProcessData;

		this.nodeData = new HashMap<Id, DijkstraNode>((int)(network.getNodes().size() * 1.1), 0.95f);

		// create all DijkstraNodes
		for (Node node : this.network.getNodes().values())
		{
			this.nodeData.put(node.getId(), new DijkstraNode(node));
		}

		// create all DijkstraLinks
		Map<Id, DijkstraLink> dijkstraLinks = new HashMap<Id, DijkstraLink>((int)(network.getLinks().size() * 1.1), 0.95f);
		for (Link link : this.network.getLinks().values())
		{
			DijkstraNode fromNode = this.nodeData.get(link.getFromNode().getId());
			DijkstraNode toNode = this.nodeData.get(link.getToNode().getId());
			DijkstraLink dijkstraLink = new DijkstraLink(link, fromNode, toNode);
			dijkstraLinks.put(link.getId(), dijkstraLink);
		}

		/*
		 * Connect DijkstraNodes and DijkstraLinks
		 *
		 * Ensure to use the same Link Order as in the original Nodes.
		 * They use a LinkedHashMap to store the Links so we can take their Order.
		 */
		for (DijkstraNode dijkstraNode : this.nodeData.values())
		{
			for (Link link : dijkstraNode.getNode().getInLinks().values())
			{
				dijkstraNode.addInLink(dijkstraLinks.get(link.getId()));
			}
			for (Link link : dijkstraNode.getNode().getOutLinks().values())
			{
				dijkstraNode.addOutLink(dijkstraLinks.get(link.getId()));
			}
		}
		dijkstraLinks.clear();

//		for (Link link : this.network.getLinks().values())
//		{
//			DijkstraNode fromNode = this.nodeData.get(link.getFromNode().getId());
//			DijkstraNode toNode = this.nodeData.get(link.getToNode().getId());
//			DijkstraLink dijkstraLink = new DijkstraLink(link, fromNode, toNode);
//
//			fromNode.addOutLink(dijkstraLink);
//			toNode.addInLink(dijkstraLink);
//		}

		if (preProcessData != null) {
			if (preProcessData.containsData() == false) {
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

//	/**
//	 * Restricts the router to only use links that have at least on of the given modes set as allowed.
//	 * Set to <code>null</code> to disable any restrictions, i.e. to use all available modes.
//	 *
//	 * @param modeRestriction {@link TransportMode}s that can be used to find a route
//	 *
//	 * @see Link#setAllowedModes(Set)
//	 */
//	public void setModeRestriction(final Set<TransportMode> modeRestriction) {
//		if (modeRestriction == null) {
//			this.modeRestriction = null;
//		} else {
//			this.modeRestriction = modeRestriction.toArray(new TransportMode[modeRestriction.size()]);
//		}
//	}

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
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime) {

		double arrivalTime = 0;
		boolean stillSearching = true;

		augmentIterationId();

		DijkstraNode dijkstraFromNode = this.nodeData.get(fromNode.getId());
		DijkstraNode dijkstraToNode = this.nodeData.get(toNode.getId());

		if (this.pruneDeadEnds == true) {
			this.deadEndEntryNode = getPreProcessData(toNode).getDeadEndEntryNode();
		}

		PseudoRemovePriorityQueue<DijkstraNode> pendingNodes = new PseudoRemovePriorityQueue<DijkstraNode>(500);
		initFromNode(dijkstraFromNode, dijkstraToNode, startTime, pendingNodes);

		while (stillSearching) {
			DijkstraNode outNode = pendingNodes.poll();

			if (outNode == null) {
				log.warn("No route was found from node " + fromNode.getId() + " to node " + toNode.getId());
				return null;
			}

			if (outNode == dijkstraToNode) {
				stillSearching = false;
				arrivalTime = outNode.getTime();
			} else {
				relaxNode(outNode, dijkstraToNode, pendingNodes);
			}
		}

		// now construct the path
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();

		nodes.add(0, toNode);
		DijkstraLink tmpDijkstraLink = dijkstraToNode.getPrevLink();
		if (tmpDijkstraLink != null)
		{
//			while (tmpDijkstraLink.getLink().getFromNode() != fromNode)
			while (tmpDijkstraLink.getFromNode() != dijkstraFromNode)
			{
				links.add(0, tmpDijkstraLink.getLink());
				nodes.add(0, tmpDijkstraLink.getLink().getFromNode());
				tmpDijkstraLink = tmpDijkstraLink.getFromNode().getPrevLink();
			}
			links.add(0, tmpDijkstraLink.getLink());
			nodes.add(0, tmpDijkstraLink.getLink().getFromNode());
		}

//		// now construct the path
//		ArrayList<Node> nodes = new ArrayList<Node>();
//		ArrayList<Link> links = new ArrayList<Link>();
//
//		nodes.add(0, toNode);
//		Link tmpLink = getData(toNode).getPrevLink();
//		if (tmpLink != null) {
//			while (tmpLink.getFromNode() != fromNode) {
//				links.add(0, tmpLink);
//				nodes.add(0, tmpLink.getFromNode());
//				tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
//			}
//			links.add(0, tmpLink);
//			nodes.add(0, tmpLink.getFromNode());
//		}
//
//		DijkstraNodeData toNodeData = getData(toNode);
//		Path path = new Path(nodes, links, arrivalTime - startTime, toNodeData.getCost());

		Path path = new Path(nodes, links, arrivalTime - startTime, dijkstraToNode.getCost());

//		Path orgPath = this.dijsktra.calcLeastCostPath(fromNode, toNode, startTime);
//		if (Math.abs(path.travelCost - orgPath.travelCost) > 0.0001)
//		{
//			log.info("Different TravelCosts! " + path.travelCost + " vs. " + orgPath.travelCost);
//		}
//		if (Math.abs(path.travelTime - orgPath.travelTime) > 0.0001)
//		{
//			log.info("Different TravelTimes! " + path.travelTime + " vs. " + orgPath.travelTime);
//		}
//		if (path.links.size() != orgPath.links.size())
//		{
//			log.info("Different LinksCount! " + path.links.size() + " vs. " + orgPath.links.size());
//		}
//		if (path.nodes.size() != orgPath.nodes.size())
//		{
//			log.info("Different NodesCount! " + path.nodes.size() + " vs. " + orgPath.nodes.size());
//		}

//		return orgPath;
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
	/*package*/ void initFromNode(final DijkstraNode fromNode, final DijkstraNode toNode, final double startTime,
			final PseudoRemovePriorityQueue<DijkstraNode> pendingNodes) {
		visitNode(fromNode, pendingNodes, startTime, 0, null);
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
	protected void relaxNode(final DijkstraNode outNode, final DijkstraNode toNode,
			final PseudoRemovePriorityQueue<DijkstraNode> pendingNodes) {

		double currTime = outNode.getTime();
		double currCost = outNode.getCost();
		PreProcessDijkstra.DeadEndData ddOutData = null;
		if (this.pruneDeadEnds) {
			ddOutData = getPreProcessData(outNode);
			for (DijkstraLink l : outNode.getOutLinks().values()) {
				if (canPassLink(l)) {
					DijkstraNode dijkstraNode = l.getToNode();
					PreProcessDijkstra.DeadEndData ddData = getPreProcessData(dijkstraNode);

					/* IF the current node n is not in a dead end
					 * OR it is in the same dead end as the fromNode
					 * OR it is in the same dead end as the toNode
					 * THEN we add the current node to the pending nodes */
					if ((ddData.getDeadEndEntryNode() == null)
							|| (ddOutData.getDeadEndEntryNode() != null)
							|| ((this.deadEndEntryNode != null)
									&& (this.deadEndEntryNode.getId() == ddData.getDeadEndEntryNode().getId()))) {
						addToPendingNodes(l, dijkstraNode, pendingNodes, currTime, currCost, toNode);
					}
				}
			}
		} else { // this.pruneDeadEnds == false
			for (DijkstraLink l : outNode.getOutLinks().values()) {
				if (canPassLink(l)) {
					DijkstraNode dijkstraNode = l.getToNode();
					addToPendingNodes(l, dijkstraNode, pendingNodes, currTime, currCost, toNode);
				}
			}
		}
	}

	/**
	 * Adds some parameters to the given Node then adds it to the set of pending
	 * nodes.
	 *
	 * @param l
	 *            The link from which we came to this Node.
	 * @param dijkstraNode
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
	protected boolean addToPendingNodes(final DijkstraLink l, final DijkstraNode dijkstraNode,
			final PseudoRemovePriorityQueue<DijkstraNode> pendingNodes, final double currTime,
			final double currCost, final DijkstraNode toNode) {

		double travelTime = this.timeFunction.getLinkTravelTime(l.getLink(), currTime);
		double travelCost = this.costFunction.getLinkTravelCost(l.getLink(), currTime);
		double nCost = dijkstraNode.getCost();
		if (!dijkstraNode.isVisited(getIterationId()))
		{
			visitNode(dijkstraNode, pendingNodes, currTime + travelTime, currCost + travelCost, l);
			return true;
		}
		else if (currCost + travelCost < nCost)
		{
			revisitNode(dijkstraNode, pendingNodes, currTime + travelTime, currCost + travelCost, l);
			return true;
		}

		return false;
	}

//	/**
//	 * @param link
//	 * @return <code>true</code> if the link can be passed with respect to a possible mode restriction set
//	 *
//	 * @see #setModeRestriction(Set)
//	 */
//	protected boolean canPassLink(final Link link) {
//		if (this.modeRestriction == null) {
//			return true;
//		}
//		for (TransportMode mode : this.modeRestriction) {
//			if (link.getAllowedModes().contains(mode)) {
//				return true;
//			}
//		}
//		return false;
//	}

	/**
	 * Changes the position of the given Node n in the pendingNodes queue and
	 * updates its time and cost information.
	 *
	 * @param dijkstraNode
	 *            The Node that is revisited.
	 * @param pendingNodes
	 *            The nodes visited and not processed yet.
	 * @param time
	 *            The time of the visit of n.
	 * @param cost
	 *            The accumulated cost at the time of the visit of n.
	 * @param outLink
	 *            The link from which we came visiting n.
	 */
	void revisitNode(final DijkstraNode dijkstraNode, final PseudoRemovePriorityQueue<DijkstraNode> pendingNodes,
			final double time, final double cost, final DijkstraLink outLink) {
		pendingNodes.remove(dijkstraNode);

		dijkstraNode.visit(outLink, cost, time, getIterationId());
		pendingNodes.add(dijkstraNode, getPriority(dijkstraNode));
	}

	/**
	 * Inserts the given Node n into the pendingNodes queue and updates its time
	 * and cost information.
	 *
	 * @param dijkstraNode
	 *            The Node that is revisited.
	 * @param pendingNodes
	 *            The nodes visited and not processed yet.
	 * @param time
	 *            The time of the visit of n.
	 * @param cost
	 *            The accumulated cost at the time of the visit of n.
	 * @param outLink
	 *            The node from which we came visiting n.
	 */
	protected void visitNode(final DijkstraNode dijkstraNode, final PseudoRemovePriorityQueue<DijkstraNode> pendingNodes,
			final double time, final double cost, final DijkstraLink outLink) {
		dijkstraNode.visit(outLink, cost, time, getIterationId());
		pendingNodes.add(dijkstraNode, getPriority(dijkstraNode));
	}

	/**
	 * Augments the iterationID and checks whether the visited information in
	 * the nodes have to be reset.
	 */
	@Override
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
	/*package*/ int getIterationId() {
		return this.iterationID;
	}

	/**
	 * Resets all nodes in the network as if they have not been visited yet.
	 */
	private void resetNetworkVisited() {
		for (DijkstraNode dijkstraNode : this.nodeData.values())
		{
			dijkstraNode.resetVisited();
		}
	}

	/**
	 * The value used to sort the pending nodes during routing.
	 * This implementation compares the total effective travel cost
	 * to sort the nodes in the pending nodes queue during routing.
	 */
	protected double getPriority(final DijkstraNode dijkstraNode) {
		return dijkstraNode.getCost();
	}

	@Override
	protected PreProcessDijkstra.DeadEndData getPreProcessData(final Node n) {
		return this.preProcessData.getNodeData(n);
	}

	/**
	 * A data structure to store temporarily information used
	 * by the Dijkstra-algorithm.
	 */
	protected static class DijkstraNode implements Node{

		private Node node = null;
		private DijkstraLink prev = null;

		private Map<Id, DijkstraLink> inLinks = new LinkedHashMap<Id, DijkstraLink>();
		private Map<Id, DijkstraLink> outLinks = new LinkedHashMap<Id, DijkstraLink>();

		private double cost = 0;
		private double time = 0;

		private int iterationID = Integer.MIN_VALUE;

		public DijkstraNode(Node node)
		{
			this.node = node;
		}

		public Node getNode()
		{
			return this.node;
		}

		public void resetVisited() {
			this.iterationID = Integer.MIN_VALUE;
		}

		public void visit(final DijkstraLink comingFrom, final double cost, final double time,
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

		public DijkstraLink getPrevLink() {
			return this.prev;
		}

		@Override
		public boolean addInLink(Link link) {
			return this.inLinks.put(link.getId(), (DijkstraLink) link) != null;
		}

		@Override
		public boolean addOutLink(Link link) {
			return this.outLinks.put(link.getId(), (DijkstraLink) link) != null;
		}

		@Override
		public Map<Id, DijkstraLink> getInLinks() {
			return this.inLinks;
		}

		@Override
		public Map<Id, DijkstraLink> getOutLinks() {
			return this.outLinks;
		}

		@Override
		public Coord getCoord() {
			return this.node.getCoord();
		}

		@Override
		public Id getId() {
			return this.node.getId();
		}
	}

	protected static class DijkstraLink implements Link{

		private Link link;
		private DijkstraNode fromNode;
		private DijkstraNode toNode;

		public DijkstraLink(Link link, DijkstraNode fromNode, DijkstraNode toNode)
		{
			this.link = link;
			this.fromNode = fromNode;
			this.toNode = toNode;
		}

		public Link getLink()
		{
			return this.link;
		}

		@Override
		public Set<String> getAllowedModes() {
			return this.link.getAllowedModes();
		}

		@Override
		public double getCapacity() {
			return this.link.getCapacity();
		}

		@Override
		public double getCapacity(double time) {
			return this.link.getCapacity(time);
		}

		@Override
		public double getFreespeed() {
			return this.link.getFreespeed();
		}

		@Override
		public double getFreespeed(double time) {
			return this.link.getFreespeed(time);
		}

		@Override
		public DijkstraNode getFromNode() {
			return this.fromNode;
		}

		@Override
		public double getLength() {
			return this.link.getLength();
		}

		@Override
		public double getNumberOfLanes() {
			return this.link.getNumberOfLanes();
		}

		@Override
		public double getNumberOfLanes(double time) {
			return this.link.getNumberOfLanes(time);
		}

		@Override
		public DijkstraNode getToNode() {
			return this.toNode;
		}

		@Override
		public void setAllowedModes(Set<String> modes) {
			this.link.setAllowedModes(modes);
		}

		@Override
		public void setCapacity(double capacity) {
			this.link.setCapacity(capacity);
		}

		@Override
		public void setFreespeed(double freespeed) {
			this.link.setFreespeed(freespeed);
		}

		@Override
		public boolean setFromNode(Node node) {
			return false;
		}

		@Override
		public void setLength(double length) {
			this.link.setLength(length);
		}

		@Override
		public void setNumberOfLanes(double lanes) {
			this.link.setNumberOfLanes(lanes);
		}

		@Override
		public boolean setToNode(Node node) {
			return false;
		}

		@Override
		public Id getId() {
			return this.link.getId();
		}

		@Override
		public Coord getCoord() {
			return this.link.getCoord();
		}

	}

}