/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.path;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstraFactory;
import org.matsim.contrib.locationchoice.router.BackwardMultiNodePathCalculator;
import org.matsim.core.router.FastMultiNodeDijkstraFactory;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.MultiNodePathCalculator;
import org.matsim.core.router.RoutingNetworkImaginaryNode;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class OneToManyPathSearch {
	public static OneToManyPathSearch createForwardSearch(Network network, TravelTime travelTime,
			TravelDisutility travelDisutility) {
		return create((MultiNodePathCalculator)new FastMultiNodeDijkstraFactory(true).createPathCalculator(network,
				travelDisutility, travelTime));
	}

	public static OneToManyPathSearch createBackwardSearch(Network network, TravelTime travelTime,
			TravelDisutility travelDisutility) {
		return create((BackwardMultiNodePathCalculator)new BackwardFastMultiNodeDijkstraFactory(true)
				.createPathCalculator(network, travelDisutility, travelTime));
	}

	public static OneToManyPathSearch create(MultiNodePathCalculator multiNodeDijkstra) {
		return new OneToManyPathSearch(multiNodeDijkstra);
	}

	public static class PathData {
		final Path path;// shortest path
		private final double firstAndLastLinkTT;// at both the first and last links

		public PathData(Path path, double firstAndLastLinkTT) {
			this.path = new Path(null, ImmutableList.copyOf(path.links), path.travelTime, path.travelCost);
			this.firstAndLastLinkTT = firstAndLastLinkTT;
		}

		public double getTravelTime() {
			return path.travelTime + firstAndLastLinkTT;
		}
	}

	private static class ToNode extends InitialNode {
		private Path path;

		private ToNode(Node node, double initialCost, double initialTime) {
			super(node, initialCost, initialTime);
		}
	}

	private final MultiNodePathCalculator multiNodeDijkstra;// forward or backward
	private final boolean forward;

	private OneToManyPathSearch(MultiNodePathCalculator multiNodeDijkstra) {
		this.multiNodeDijkstra = multiNodeDijkstra;
		this.forward = !(multiNodeDijkstra instanceof BackwardFastMultiNodeDijkstra);
	}

	public PathData[] calcPathDataArray(Link fromLink, List<Link> toLinks, double startTime) {
		Node fromNode = getFromNode(fromLink);
		Map<Id<Node>, ToNode> toNodes = createToNodes(fromLink, toLinks);
		calculatePaths(fromNode, toNodes, startTime);
		return createPathDataArray(fromLink, toLinks, startTime, toNodes);
	}

	public Map<Id<Link>, PathData> calcPathDataMap(Link fromLink, Collection<Link> toLinks, double startTime) {
		Node fromNode = getFromNode(fromLink);
		Map<Id<Node>, ToNode> toNodes = createToNodes(fromLink, toLinks);
		calculatePaths(fromNode, toNodes, startTime);
		return createPathDataMap(fromLink, toLinks, startTime, toNodes);
	}

	private Map<Id<Node>, ToNode> createToNodes(Link fromLink, Collection<Link> toLinks) {
		Map<Id<Node>, ToNode> toNodes = Maps.newHashMapWithExpectedSize(toLinks.size());
		for (Link toLink : toLinks) {
			if (toLink != fromLink) {
				Node toNode = getToNode(toLink);
				toNodes.putIfAbsent(toNode.getId(), new ToNode(toNode, 0, 0));
			}
		}
		return toNodes;
	}

	private void calculatePaths(Node fromNode, Map<Id<Node>, ToNode> toNodes, double startTime) {
		RoutingNetworkImaginaryNode imaginaryNode = new RoutingNetworkImaginaryNode(toNodes.values());
		multiNodeDijkstra.setSearchAllEndNodes(true);
		multiNodeDijkstra.calcLeastCostPath(fromNode, imaginaryNode, startTime, null, null);

		// get path for each ToNode
		for (ToNode toNode : toNodes.values()) {
			toNode.path = multiNodeDijkstra.constructPath(fromNode, toNode.node, startTime);
		}
	}

	private PathData[] createPathDataArray(Link fromLink, List<Link> toLinks, double startTime,
			Map<Id<Node>, ToNode> toNodes) {
		PathData[] pathDataArray = new PathData[toLinks.size()];
		for (int i = 0; i < pathDataArray.length; i++) {
			pathDataArray[i] = createPathData(fromLink, toLinks.get(i), startTime, toNodes);
		}
		return pathDataArray;
	}

	private Map<Id<Link>, PathData> createPathDataMap(Link fromLink, Collection<Link> toLinks, double startTime,
			Map<Id<Node>, ToNode> toNodes) {
		Map<Id<Link>, PathData> pathDataMap = Maps.newHashMapWithExpectedSize(toLinks.size());
		for (Link toLink : toLinks) {
			pathDataMap.put(toLink.getId(), createPathData(fromLink, toLink, startTime, toNodes));
		}
		return pathDataMap;
	}

	private PathData createPathData(Link fromLink, Link toLink, double startTime, Map<Id<Node>, ToNode> toNodes) {
		if (toLink == fromLink) {
			return createZeroPathData(fromLink);
		} else {
			ToNode toNode = toNodes.get(getToNode(toLink).getId());
			return new PathData(toNode.path, getFirstAndLastLinkTT(fromLink, toLink, toNode.path, startTime));
		}
	}

	private PathData createZeroPathData(Link fromLink) {
		List<Node> singleNodeList = Collections.singletonList(getFromNode(fromLink));
		List<Link> emptyLinkList = Collections.emptyList();
		return new PathData(new Path(singleNodeList, emptyLinkList, 0, 0), 0);
	}

	private Node getToNode(Link toLink) {
		return forward ? toLink.getFromNode() : toLink.getToNode();
	}

	private Node getFromNode(Link fromLink) {
		return forward ? fromLink.getToNode() : fromLink.getFromNode();
	}

	private double getFirstAndLastLinkTT(Link fromLink, Link toLink, Path path, double time) {
		double lastLinkTT = forward ? //
				VrpPaths.getLastLinkTT(toLink, time + path.travelTime) : VrpPaths.getLastLinkTT(fromLink, time);
		return VrpPaths.FIRST_LINK_TT + lastLinkTT;
	}
}
