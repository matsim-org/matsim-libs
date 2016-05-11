/* *********************************************************************** *
 * project: org.matsim.*
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


package playground.polettif.publicTransitMapping.mapping.pseudoPTRouter;

import java.util.*;

/**
 * A basic dijkstra algorithm used to calculate the shortes
 * path for a transit route via link candidates of route stops.
 *
 * @author polettif
 */
public class DijkstraAlgorithm {
	
	private final Set<PseudoRoutePath> edges;
	private final PseudoRouteStop destination;
	private final PseudoRouteStop source;
	private Set<PseudoRouteStop> settledNodes;
	private Set<PseudoRouteStop> unSettledNodes;
	private Map<PseudoRouteStop, PseudoRouteStop> predecessors;
	private Map<PseudoRouteStop, Double> distance;

	public DijkstraAlgorithm(PseudoGraph graph) {
		this.edges = graph.getEdges();
		this.source = graph.getSource();
		this.destination = graph.getDestination();
	}

	public void run() {
		settledNodes = new HashSet<>();
		unSettledNodes = new HashSet<>();
		distance = new HashMap<>();
		predecessors = new HashMap<>();
		distance.put(source, 0.0);
		unSettledNodes.add(source);
		while (unSettledNodes.size() > 0) {
			PseudoRouteStop node = getMinimum(unSettledNodes);
			settledNodes.add(node);
			unSettledNodes.remove(node);
			findMinimalDistances(node);
		}
	}

	private void findMinimalDistances(PseudoRouteStop node) {
		List<PseudoRouteStop> adjacentNodes = getNeighbors(node);
		for (PseudoRouteStop target : adjacentNodes) {
			if (getShortestDistance(target) > getShortestDistance(node) + getDistance(node, target)) {
				distance.put(target, getShortestDistance(node) + getDistance(node, target));
				predecessors.put(target, node);
				unSettledNodes.add(target);
			}
		}

	}

	private double getDistance(PseudoRouteStop node, PseudoRouteStop target) {
		for (PseudoRoutePath pseudoRoutePath : edges) {
			if (pseudoRoutePath.getFromPseudoStop().equals(node)
					&& pseudoRoutePath.getToPseudoStop().equals(target)) {
				return pseudoRoutePath.getWeight();
			}
		}
		throw new RuntimeException("Should not happen");
	}

	private List<PseudoRouteStop> getNeighbors(PseudoRouteStop node) {
		List<PseudoRouteStop> neighbors = new ArrayList<>();
		for (PseudoRoutePath pseudoRoutePath : edges) {
			if (pseudoRoutePath.getFromPseudoStop().equals(node)
					&& !isSettled(pseudoRoutePath.getToPseudoStop())) {
				neighbors.add(pseudoRoutePath.getToPseudoStop());
			}
		}
		return neighbors;
	}

	private PseudoRouteStop getMinimum(Set<PseudoRouteStop> pseudoRouteStopes) {
		PseudoRouteStop minimum = null;
		for (PseudoRouteStop pseudoRouteStop : pseudoRouteStopes) {
			if (minimum == null) {
				minimum = pseudoRouteStop;
			} else {
				if (getShortestDistance(pseudoRouteStop) < getShortestDistance(minimum)) {
					minimum = pseudoRouteStop;
				}
			}
		}
		return minimum;
	}

	private boolean isSettled(PseudoRouteStop pseudoRouteStop) {
		return settledNodes.contains(pseudoRouteStop);
	}

	private Double getShortestDistance(PseudoRouteStop destination) {
		Double d = distance.get(destination);
		if (d == null) {
			return Double.MAX_VALUE;
		} else {
			return d;
		}
	}


	/**
	 * @return A list of LinkCandidates for the shortest path of the route
	 */
	public LinkedList<PseudoRouteStop> getShortesPseudoPath() {
		LinkedList<PseudoRouteStop> path = new LinkedList<>();
		PseudoRouteStop step = destination;

		// check if a path exists
		if (predecessors.get(step) == null) {
			return null;
		}
		path.add(step);
		while (predecessors.get(step) != null) {
			step = predecessors.get(step);
			path.add(step);
		}

		// Put it into the correct order
		Collections.reverse(path);

		// remove dummies
		path.removeFirst();
		path.removeLast();

		return path;
	}

}
