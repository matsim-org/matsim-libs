/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes non-connecting service-link components from a network.
 *
 * <p>Service links in OSM (parking aisles, driveways, gas station entrances,
 * loading docks) form many small connected components that don't actually
 * provide useful shortcuts in a routable network. This cleaner walks each
 * connected component made entirely of service links and decides:
 * <ul>
 *   <li>If the component touches the rest of the graph at 0 or 1 nodes
 *       ("docking nodes"), it can't be a shortcut between anything --
 *       remove it entirely.</li>
 *   <li>Otherwise, trim hair-like dead-end branches while keeping whatever
 *       actually connects two or more entry points.</li>
 * </ul>
 *
 * <p>"Service link" is defined by an attribute key + value. Defaults match
 * what {@link org.matsim.contrib.osm.networkReader.OsmBicycleReader} writes:
 * the attribute {@code "type"} carries values like {@code "highway.service"},
 * which this class matches against {@code "service"} after stripping the
 * {@code "highway."} prefix.
 */
public final class ServiceLinkCleaner {

	private final String typeAttribute;
	private final String serviceValue;

	public ServiceLinkCleaner() {
		this("type", "service");
	}

	public ServiceLinkCleaner(String typeAttribute, String serviceValue) {
		this.typeAttribute = typeAttribute;
		this.serviceValue = serviceValue;
	}

	public void run(Network network) {

		Set<Id<Link>> visited = new HashSet<>();
		List<Id<Link>> linksToRemove = new ArrayList<>();

		for (Link seed : network.getLinks().values()) {
			if (!isService(seed) || visited.contains(seed.getId())) continue;

			Deque<Link> q = new ArrayDeque<>();
			q.add(seed);
			visited.add(seed.getId());

			List<Link> componentLinks = new ArrayList<>();
			Set<Id<Node>> dockingNodes = new HashSet<>();

			while (!q.isEmpty()) {
				Link l = q.poll();
				componentLinks.add(l);

				Node a = l.getFromNode();
				Node b = l.getToNode();
				if (hasNonServiceIncidentLink(a)) dockingNodes.add(a.getId());
				if (hasNonServiceIncidentLink(b)) dockingNodes.add(b.getId());

				for (Link nl : a.getInLinks().values()) if (isService(nl) && visited.add(nl.getId())) q.add(nl);
				for (Link nl : a.getOutLinks().values()) if (isService(nl) && visited.add(nl.getId())) q.add(nl);
				for (Link nl : b.getInLinks().values()) if (isService(nl) && visited.add(nl.getId())) q.add(nl);
				for (Link nl : b.getOutLinks().values()) if (isService(nl) && visited.add(nl.getId())) q.add(nl);
			}

			if (dockingNodes.size() <= 1) {
				for (Link l : componentLinks) linksToRemove.add(l.getId());
			} else {
				trimComponent(network, componentLinks, dockingNodes);
			}
		}

		linksToRemove.forEach(network::removeLink);
		NetworkUtils.removeNodesWithoutLinks(network);
	}

	// ----------------------------------------------------------------------

	private boolean isService(Link l) {
		Object t = l.getAttributes().getAttribute(typeAttribute);
		if (t == null) return false;
		String s = t.toString().replaceFirst("^highway\\.", "");
		return serviceValue.equals(s);
	}

	private boolean hasNonServiceIncidentLink(Node n) {
		for (Link l : n.getInLinks().values()) if (!isService(l)) return true;
		for (Link l : n.getOutLinks().values()) if (!isService(l)) return true;
		return false;
	}

	/**
	 * Iteratively peels off leaf nodes (degree <= 1, not a docking node) from
	 * the component. What remains is either empty or a chain/tree connecting
	 * at least two docking nodes.
	 */
	private void trimComponent(Network network, List<Link> componentLinks, Set<Id<Node>> dockingNodes) {

		Map<Id<Node>, Set<Id<Node>>> nbr = new HashMap<>();
		Map<String, List<Id<Link>>> pairToLinks = new HashMap<>();

		for (Link l : componentLinks) {
			Id<Node> u = l.getFromNode().getId();
			Id<Node> v = l.getToNode().getId();
			nbr.computeIfAbsent(u, k -> new HashSet<>()).add(v);
			nbr.computeIfAbsent(v, k -> new HashSet<>()).add(u);
			pairToLinks.computeIfAbsent(edgeKey(u, v), k -> new ArrayList<>()).add(l.getId());
		}

		Deque<Id<Node>> q = new ArrayDeque<>();
		for (var e : nbr.entrySet()) {
			if (e.getValue().size() <= 1 && !dockingNodes.contains(e.getKey())) q.add(e.getKey());
		}

		Set<String> removedPairs = new HashSet<>();
		while (!q.isEmpty()) {
			Id<Node> leaf = q.poll();
			Set<Id<Node>> neigh = nbr.get(leaf);
			if (neigh == null || neigh.size() > 1 || dockingNodes.contains(leaf)) continue;
			if (neigh.isEmpty()) continue;

			Id<Node> other = neigh.iterator().next();
			removedPairs.add(edgeKey(leaf, other));
			nbr.get(leaf).remove(other);
			nbr.get(other).remove(leaf);

			if (nbr.get(other).size() <= 1 && !dockingNodes.contains(other)) q.add(other);
		}

		for (String key : removedPairs) {
			for (Id<Link> lid : pairToLinks.getOrDefault(key, List.of())) {
				network.removeLink(lid);
			}
		}
	}

	private static String edgeKey(Id<Node> a, Id<Node> b) {
		return a.toString().compareTo(b.toString()) < 0 ? a + "_" + b : b + "_" + a;
	}
}
