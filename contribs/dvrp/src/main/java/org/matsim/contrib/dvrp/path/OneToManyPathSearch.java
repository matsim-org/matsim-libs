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
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import ch.sbb.matsim.routing.graph.Graph;
import ch.sbb.matsim.routing.graph.LeastCostPathTree;

public class OneToManyPathSearch {
	public static OneToManyPathSearch createSearch(Graph graph, IdMap<Node, Node> nodeMap, TravelTime travelTime,
			TravelDisutility travelDisutility) {
		return new OneToManyPathSearch(nodeMap, new LeastCostPathTree(graph, travelTime, travelDisutility));
	}

	public static class PathData {
		final LeastCostPathCalculator.Path path;// shortest path
		private final double firstAndLastLinkTT;// at both the first and last links

		public PathData(LeastCostPathCalculator.Path path, double firstAndLastLinkTT) {
			this.path = new LeastCostPathCalculator.Path(null, ImmutableList.copyOf(path.links), path.travelTime,
					path.travelCost);
			this.firstAndLastLinkTT = firstAndLastLinkTT;
		}

		public double getTravelTime() {
			return path.travelTime + firstAndLastLinkTT;
		}
	}

	private final IdMap<Node, Node> nodeMap;
	private final LeastCostPathTree dijkstraTree;

	private OneToManyPathSearch(IdMap<Node, Node> nodeMap, LeastCostPathTree dijkstraTree) {
		this.nodeMap = nodeMap;
		this.dijkstraTree = dijkstraTree;
	}

	public PathData[] calcPathDataArray(Link fromLink, List<Link> toLinks, double startTime, boolean forward) {
		OneToManyPathCalculator pathConstructor = new OneToManyPathCalculator(nodeMap, dijkstraTree, forward, fromLink,
				startTime);
		pathConstructor.calculateDijkstraTree(toLinks);
		return createPathDataArray(toLinks, pathConstructor);
	}

	public Map<Link, PathData> calcPathDataMap(Link fromLink, Collection<Link> toLinks, double startTime,
			boolean forward) {
		OneToManyPathCalculator pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, forward, fromLink,
				startTime);
		pathCalculator.calculateDijkstraTree(toLinks);
		return createPathDataMap(toLinks, pathCalculator);
	}

	private PathData[] createPathDataArray(List<Link> toLinks, OneToManyPathCalculator pathCalculator) {
		PathData[] pathDataArray = new PathData[toLinks.size()];
		for (int i = 0; i < pathDataArray.length; i++) {
			pathDataArray[i] = pathCalculator.createPathData(toLinks.get(i));
		}
		return pathDataArray;
	}

	private Map<Link, PathData> createPathDataMap(Collection<Link> toLinks, OneToManyPathCalculator pathCalculator) {
		Map<Link, PathData> pathDataMap = Maps.newHashMapWithExpectedSize(toLinks.size());
		for (Link toLink : toLinks) {
			pathDataMap.put(toLink, pathCalculator.createPathData(toLink));
		}
		return pathDataMap;
	}
}
