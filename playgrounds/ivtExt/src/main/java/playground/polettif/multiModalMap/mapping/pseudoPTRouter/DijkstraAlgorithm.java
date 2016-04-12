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


package playground.polettif.multiModalMap.mapping.pseudoPTRouter;

import java.util.*;

public class DijkstraAlgorithm {
	
	private final List<LinkCandidate> nodes;
	private final List<LinkCandidatePath> edges;
	private final LinkCandidate destination;
	private final LinkCandidate source;
	private Set<LinkCandidate> settledNodes;
	private Set<LinkCandidate> unSettledNodes;
	private Map<LinkCandidate, LinkCandidate> predecessors;
	private Map<LinkCandidate, Double> distance;

	public DijkstraAlgorithm(PseudoGraph graph) {
		this.nodes = graph.getNodes();
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
			LinkCandidate node = getMinimum(unSettledNodes);
			settledNodes.add(node);
			unSettledNodes.remove(node);
			findMinimalDistances(node);
		}
	}

	private void findMinimalDistances(LinkCandidate node) {
		List<LinkCandidate> adjacentNodes = getNeighbors(node);
		for (LinkCandidate target : adjacentNodes) {
			if (getShortestDistance(target) > getShortestDistance(node)
					+ getDistance(node, target)) {
				distance.put(target, getShortestDistance(node)
						+ getDistance(node, target));
				predecessors.put(target, node);
				unSettledNodes.add(target);
			}
		}

	}

	private double getDistance(LinkCandidate node, LinkCandidate target) {
		for (LinkCandidatePath LinkCandidatePath : edges) {
			if (LinkCandidatePath.getFromLinkCandidate().equals(node)
					&& LinkCandidatePath.getToLinkCandidate().equals(target)) {
				return LinkCandidatePath.getWeight();
			}
		}
		return Double.MAX_VALUE;
	}

	private List<LinkCandidate> getNeighbors(LinkCandidate node) {
		List<LinkCandidate> neighbors = new ArrayList<>();
		for (LinkCandidatePath LinkCandidatePath : edges) {
			if (LinkCandidatePath.getFromLinkCandidate().equals(node)
					&& !isSettled(LinkCandidatePath.getToLinkCandidate())) {
				neighbors.add(LinkCandidatePath.getToLinkCandidate());
			}
		}
		return neighbors;
	}

	private LinkCandidate getMinimum(Set<LinkCandidate> LinkCandidatees) {
		LinkCandidate minimum = null;
		for (LinkCandidate LinkCandidate : LinkCandidatees) {
			if (minimum == null) {
				minimum = LinkCandidate;
			} else {
				if (getShortestDistance(LinkCandidate) < getShortestDistance(minimum)) {
					minimum = LinkCandidate;
				}
			}
		}
		return minimum;
	}

	private boolean isSettled(LinkCandidate LinkCandidate) {
		return settledNodes.contains(LinkCandidate);
	}

	private double getShortestDistance(LinkCandidate destination) {
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
	public LinkedList<LinkCandidate> getBestLinkCandidates() {
		LinkedList<LinkCandidate> path = new LinkedList<>();
		LinkCandidate step = destination;
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
