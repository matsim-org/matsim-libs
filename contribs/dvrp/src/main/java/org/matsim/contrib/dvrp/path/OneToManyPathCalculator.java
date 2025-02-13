/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.path;

import static java.util.stream.Collectors.toList;
import static org.matsim.contrib.dvrp.path.LeastCostPathTreeStopCriteria.allEndNodesReached;
import static org.matsim.contrib.dvrp.path.LeastCostPathTreeStopCriteria.withMaxTravelTime;
import static org.matsim.contrib.dvrp.path.VrpPaths.FIRST_LINK_TT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.core.router.speedy.LeastCostPathTree;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * @author Michal Maciejewski (michalm)
 * @author Sebastian Hörl, IRT SystemX (sebhoerl)
 */
class OneToManyPathCalculator {
	private final IdMap<Node, Node> nodeMap;
	private final LeastCostPathTree dijkstraTree;
	private final TravelTime travelTime;
	private final boolean forwardSearch;
	private final Link fromLink;
	private final double startTime;

	OneToManyPathCalculator(IdMap<Node, Node> nodeMap, LeastCostPathTree dijkstraTree, TravelTime travelTime,
							boolean forwardSearch, Link fromLink, double startTime) {
		this.nodeMap = nodeMap;
		this.dijkstraTree = dijkstraTree;
		this.travelTime = travelTime;
		this.forwardSearch = forwardSearch;
		this.fromLink = fromLink;
		this.startTime = startTime;
	}

	void calculateDijkstraTree(Collection<Link> toLinks) {
		calculateDijkstraTree(toLinks, Double.POSITIVE_INFINITY);
	}

	void calculateDijkstraTree(Collection<Link> toLinks, double maxTravelTime) {
		var toNodes = toLinks.stream().filter(link -> link != fromLink).map(this::getEndNode).collect(toList());
		if (toNodes.size() == 0) {
			return;
		}

		int fromNodeIdx = getStartNode(fromLink).getId().index();
		var stopCriterion = withMaxTravelTime(allEndNodesReached(toNodes), maxTravelTime);

		if (forwardSearch) {
			dijkstraTree.calculate(fromNodeIdx, startTime, null, null, stopCriterion);
		} else {
			dijkstraTree.calculateBackwards(fromNodeIdx, startTime, null, null, stopCriterion);
		}
	}

	PathData createPathDataLazily(Link toLink) {
		if (toLink == fromLink) {
			return PathData.EMPTY;
		} else {
			Node endNode = getEndNode(toLink);
			double pathTravelTime = getTravelTime(endNode.getId().index());
			if (pathTravelTime == Double.POSITIVE_INFINITY) {
				return PathData.INFEASIBLE;
			}
			Supplier<Path> pathSupplier = () -> createPath(endNode);
			return new PathData(pathSupplier, pathTravelTime,
					getFirstAndLastLinkTT(fromLink, toLink, pathTravelTime, startTime));
		}
	}

	PathData createPathDataEagerly(Link toLink) {
		if (toLink == fromLink) {
			return PathData.EMPTY;
		} else {
			Node endNode = getEndNode(toLink);
			if (dijkstraTree.getTime(endNode.getId().index()).isUndefined()) {
				return PathData.INFEASIBLE;
			}
			Path path = createPath(endNode);
			return new PathData(path, getFirstAndLastLinkTT(fromLink, toLink, path.travelTime, startTime));
		}
	}

	@Nullable
	Path createPath(Node toNode) {
		int toNodeIndex = toNode.getId().index();
		double travelTime = getTravelTime(toNodeIndex);
		if (travelTime == Double.POSITIVE_INFINITY) {
			return null;
		}
		var nodes = constructNodeSequence(dijkstraTree, toNode, forwardSearch);
		var links = constructLinkSequence(dijkstraTree, toNode, forwardSearch);
		double cost = dijkstraTree.getCost(toNodeIndex);
		return new Path(nodes, links, travelTime, cost);
	}

	private double getTravelTime(int toNodeIndex) {
		OptionalTime endTime = dijkstraTree.getTime(toNodeIndex);
		if (endTime.isUndefined()) {
			return Double.POSITIVE_INFINITY;
		}
		int travelTimeMultiplier = forwardSearch ? 1 : -1;
		return travelTimeMultiplier * (dijkstraTree.getTime(toNodeIndex).seconds() - startTime);
	}

	private List<Node> constructNodeSequence(LeastCostPathTree dijkstraTree, Node toNode, boolean forward) {
		ArrayList<Node> nodes = new ArrayList<>();
		nodes.add(toNode);

		LeastCostPathTree.PathIterator pathIterator = dijkstraTree.getNodePathIterator(toNode);
		pathIterator.forEachRemaining(nodes::add);

		if (forward) {
			Collections.reverse(nodes);
		}
		return nodes;
	}

	private List<Link> constructLinkSequence(LeastCostPathTree dijkstraTree, Node toNode, boolean forward) {
		ArrayList<Link> links = new ArrayList<>();

		LeastCostPathTree.LinkPathIterator pathIterator = dijkstraTree.getLinkPathIterator(toNode);
		pathIterator.forEachRemaining(links::add);

		if (forward) {
			Collections.reverse(links);
		}
		return links;
	}

	private Node getEndNode(Link link) {
		return forwardSearch ? link.getFromNode() : link.getToNode();
	}

	private Node getStartNode(Link link) {
		return forwardSearch ? link.getToNode() : link.getFromNode();
	}

	private double getFirstAndLastLinkTT(Link fromLink, Link toLink, double pathTravelTime, double time) {
		double lastLinkTT = forwardSearch ?
				VrpPaths.getLastLinkTT(travelTime, toLink, time + pathTravelTime) :
				VrpPaths.getLastLinkTT(travelTime, fromLink, time);
		return FIRST_LINK_TT + lastLinkTT;
	}
}
