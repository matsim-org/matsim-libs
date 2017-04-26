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

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.locationchoice.router.BackwardMultiNodePathCalculator;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import com.google.common.collect.Maps;

public class OneToManyPathSearch {
	public static OneToManyPathSearch createForwardSearch(MultiNodePathCalculator forwardMultiNodeDijkstra) {
		return new OneToManyPathSearch(forwardMultiNodeDijkstra, true);
	}

	public static OneToManyPathSearch createBackwardSearch(BackwardMultiNodePathCalculator backwardMultiNodeDijkstra) {
		return new OneToManyPathSearch(backwardMultiNodeDijkstra, false);
	}

	public static class PathData {
		public final Path path;// shortest path
		public final double firstAndLastLinkTT;// at both the first and last links

		public PathData(Path path, double firstAndLastLinkTT) {
			this.path = path;
			this.firstAndLastLinkTT = firstAndLastLinkTT;
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

	private OneToManyPathSearch(MultiNodePathCalculator multiNodeDijkstra, boolean forward) {
		this.multiNodeDijkstra = multiNodeDijkstra;
		this.forward = forward;
	}

	public PathData[] calcPaths(Link fromLink, List<Link> toLinks, double startTime) {
		Node fromNode = getFromNode(fromLink);
		Map<Id<Node>, ToNode> toNodes = createToNodes(fromLink, toLinks);
		calculatePaths(fromNode, toNodes, startTime);
		return createPathDataArray(fromLink, toLinks, startTime, toNodes);
	}

	private Map<Id<Node>, ToNode> createToNodes(Link fromLink, List<Link> toLinks) {
		Map<Id<Node>, ToNode> toNodes = Maps.newHashMapWithExpectedSize(toLinks.size());

		for (int i = 0; i < toLinks.size(); i++) {
			Link toLink = toLinks.get(i);
			if (toLink != fromLink) {
				Node toNode = getToNode(toLink);
				if (!toNodes.containsKey(toNode.getId())) {
					toNodes.put(toNode.getId(), new ToNode(toNode, 0, 0));
				}
			}
		}

		return toNodes;
	}

	private void calculatePaths(Node fromNode, Map<Id<Node>, ToNode> toNodes, double startTime) {
		ImaginaryNode imaginaryNode = MultiNodeDijkstra.createImaginaryNode(toNodes.values());
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
			Link toLink = toLinks.get(i);
			if (toLink == fromLink) {
				pathDataArray[i] = createZeroPath(fromLink);
			} else {
				ToNode toNode = toNodes.get(getToNode(toLink).getId());
				pathDataArray[i] = new PathData(toNode.path,
						getFirstAndLastLinkTT(fromLink, toLink, toNode.path, startTime));
			}
		}

		return pathDataArray;
	}

	private PathData createZeroPath(Link fromLink) {
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
