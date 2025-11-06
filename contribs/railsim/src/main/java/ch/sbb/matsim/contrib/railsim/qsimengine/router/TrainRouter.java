/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine.router;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import com.google.inject.Inject;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopArea;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;

/**
 * Calculates unblocked route between two {@link RailLink}.
 */
public final class TrainRouter {

	private final Network network;
	private final RailResourceManager resources;

	/**
	 * Maps stops to list of loop links that belong to the same stop area.
	 */
	private final Map<Id<TransitStopFacility>, Set<Link>> stopLinks = new HashMap<>();

	@Inject
	public TrainRouter(QSim qsim, RailResourceManager resources) {
		this(qsim.getScenario().getNetwork(), qsim.getScenario().getTransitSchedule(), resources);
	}

	public TrainRouter(Network network, RailResourceManager resources) {
		this(network, null, resources);
	}

	public TrainRouter(Network network, TransitSchedule transitSchedule, RailResourceManager resources) {
		this.network = network;
		this.resources = resources;

		if (transitSchedule != null) {
			Map<Id<TransitStopArea>, List<Id<Link>>> stopAreas = new HashMap<>();

			for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
				Id<TransitStopArea> stopAreaId = stop.getStopAreaId();
				if (stopAreaId != null) {
					stopAreas.computeIfAbsent(stopAreaId, k -> new ArrayList<>()).add(stop.getLinkId());
				}
			}

			for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
				Id<TransitStopArea> stopAreaId = stop.getStopAreaId();

				Set<Link> linkIds = stopLinks.computeIfAbsent(stop.getId(), k -> new HashSet<>());
				if (stopAreaId != null) {
					linkIds.addAll(stopAreas.get(stopAreaId).stream()
						.map(network.getLinks()::get)
						.filter(l -> Objects.equals(l.getFromNode(), l.getToNode()))
						.toList());
				} else {
					Link l = network.getLinks().get(stop.getLinkId());
					if (l != null && Objects.equals(l.getFromNode(), l.getToNode())) {
						linkIds.add(l);
					}
				}
			}
		}
	}

	/**
	 * Calculate the shortest path between two links. This method is not thread-safe.
	 */
	public List<RailLink> calcRoute(TrainPosition position, RailLink from, RailLink to) {
		Link fromLink = network.getLinks().get(from.getLinkId());
		Link toLink = network.getLinks().get(to.getLinkId());

		Set<Link> currentStopLinks = Collections.emptySet();
		var nextStop = position.getNextStop();
		if (nextStop != null) {
			currentStopLinks = stopLinks.getOrDefault(nextStop.getId(), Collections.emptySet());
		}

		List<Link> path = dijkstra(fromLink, toLink, position, currentStopLinks);
		return path.stream().map(l -> resources.getLink(l.getId())).toList();
	}

	/**
	 * Simple Dijkstra algorithm that supports loop links and always traverses stop links.
	 */
	private List<Link> dijkstra(Link fromLink, Link toLink, TrainPosition position, Set<Link> stopLinks) {
		Node startNode = fromLink.getToNode();
		Node endNode = toLink.getFromNode();

		// Maps node -> best cost found so far
		Object2DoubleMap<Id<Node>> nodeCosts = new Object2DoubleOpenHashMap<>();

		// Tracks stop loop links that have been visited (these can be visited once)
		Set<Link> visitedStopLinks = new HashSet<>();

		// Priority queue: (node, cost, pathNode)
		PriorityQueue<QueueEntry> queue = new PriorityQueue<>(Comparator.comparingDouble(e -> e.cost));
		PathNode startPathNode = new PathNode(null, null);
		queue.add(new QueueEntry(startNode, 0.0, startPathNode));
		nodeCosts.put(startNode.getId(), 0.0);

		while (!queue.isEmpty()) {
			QueueEntry current = queue.poll();
			Node currentNode = current.node;
			double currentCost = current.cost;
			PathNode currentPathNode = current.pathNode;

			// Skip if we've already found a better path to this node
			// This handles the case where a node was added multiple times with different costs
			if (currentCost > nodeCosts.getOrDefault(currentNode.getId(), Double.POSITIVE_INFINITY)) {
				continue;
			}

			// Check if we've reached the destination
			if (currentNode.getId().equals(endNode.getId())) {
				return reconstructPath(currentPathNode);
			}

			// Explore all outgoing links from current node
			for (Link outgoingLink : currentNode.getOutLinks().values()) {
				// Check if this is a stop link that was already visited
				boolean isStopLink = stopLinks.contains(outgoingLink);
				if (isStopLink && visitedStopLinks.contains(outgoingLink)) {
					// Skip if we've already traversed this stop link
					continue;
				}

				// Don't traverse links that are opposite of links already used in the path
				if (isOppositeLinkInPath(outgoingLink, currentPathNode)) {
					continue;
				}

				Node nextNode = outgoingLink.getToNode();
				double linkCost = calculateLinkCost(outgoingLink, position, isStopLink);

				// Calculate new cost
				double newCost = currentCost + linkCost;

				// Check if we should update this node
				// For loop links, we allow revisiting the node if we find a better path
				// Standard Dijkstra naturally handles this by comparing costs
				double existingCost = nodeCosts.getOrDefault(nextNode.getId(), Double.NaN);

				// Update if: no cost exists, or we found a better path
				if (Double.isNaN(existingCost) || newCost < existingCost) {
					nodeCosts.put(nextNode.getId(), newCost);

					// Create new path node pointing to the previous one
					PathNode nextPathNode = new PathNode(outgoingLink, currentPathNode);

					// Mark stop link as visited
					if (isStopLink) {
						visitedStopLinks.add(outgoingLink);
					}

					queue.add(new QueueEntry(nextNode, newCost, nextPathNode));
				}
			}
		}

		// No path found
		return Collections.emptyList();
	}

	/**
	 * Check if any of the last two traversed links are opposite of the next one,
	 */
	private boolean isOppositeLinkInPath(Link link, PathNode current) {

		if (current.link != null) {
			RailLink l = resources.getLink(current.link.getId());
			if (l.isOppositeLink(link.getId()))
				return true;
		}

		if (current.previous != null && current.previous.link != null) {
			RailLink l = resources.getLink(current.previous.link.getId());
			if (l.isOppositeLink(link.getId()))
				return true;
		}

		return false;
	}

	/**
	 * Calculate the cost of traversing a link.
	 */
	private double calculateLinkCost(Link link, TrainPosition position, boolean isStopLink) {
		// Stop loop links should always be traversed once, so give them negative cost
		if (isStopLink) {
			return -0.5;
		}

		// Links without capacity have higher cost
		boolean hasCapacity = resources.hasCapacity(0.0, link.getId(), RailResourceManager.ANY_TRACK, position);
		double weight = hasCapacity ? 0.0 : 1.0;

		// Small offset prevents dead-locks
		return weight + 0.00001;
	}

	/**
	 * Reconstruct the path by following PathNode references backwards.
	 */
	private List<Link> reconstructPath(PathNode endPathNode) {
		List<Link> path = new ArrayList<>();

		// Follow the path node chain backwards
		PathNode current = endPathNode;
		while (current != null && current.link != null) {
			path.add(current.link);
			current = current.previous;
		}

		// Reverse to get forward path
		Collections.reverse(path);

		return path;
	}

	/**
	 * Entry for priority queue in Dijkstra algorithm.
	 */
	private record QueueEntry(Node node, double cost, PathNode pathNode) {
	}

	/**
	 * Path node that maintains a chain of references for path reconstruction.
	 */
	private record PathNode(Link link, PathNode previous) {
	}
}
