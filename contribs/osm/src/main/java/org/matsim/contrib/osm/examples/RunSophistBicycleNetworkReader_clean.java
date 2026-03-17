package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.utils.collections.Tuple;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.BiConsumer;

public class RunSophistBicycleNetworkReader_clean {

	//private static final String inputFile = "C://Users/metz_so/Workspace/data/neukoelln.osm.pbf";
	private static final String inputFile = "C://Users/metz_so/Workspace/data/berlin-260122.osm.pbf";
	//private static final String outputFile = "C://Users/metz_so/Workspace/data/matsim-network_nk_bicycle_custom_simp_cleanService.xml.gz";
	private static final String outputFile = "C://Users/metz_so/Workspace/data/matsim-network_berlin_bicycle_simp_cleanService.xml.gz";

	private static final CoordinateTransformation ct =
		TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");

	private static final List<String> TAGS_TO_COPY = List.of(
//		"bicycle",
//		"cycleway",
//		"cycleway:left",
//		"cycleway:right",
//		"cycleway:both",
//		"surface",
//		"smoothness",
//		"oneway",
//		"oneway:bicycle"
	);

	// Option A: wir erzeugen erst "bike" und benennen danach auf "bicycle" um
	private static final String FROM_MODE = TransportMode.bike; // "bike"
	private static final String TO_MODE = "bicycle";

	public static void main(String[] args) {

		var classifier = new BicycleInfraClassifier();
		var tagCopy = new TagCopy(TAGS_TO_COPY, "osm:");
		var policy = new BicycleLinkPolicy(classifier, tagCopy);

		Network network = new OsmBicycleReader.Builder()
			.setCoordinateTransformation(ct)
			.setAfterLinkCreated(policy::apply)
			.build()
			.read(inputFile);

//		// 1) Mode umbenennen (allowedModes + Restriction-Attribut-Key)
//		renameMode(network, FROM_MODE, TO_MODE);
//
//		// 2) optional: simplify/clean
//		NetworkUtils.simplifyNetwork(network);
//		NetworkUtils.cleanNetwork(network, Set.of(TO_MODE)); // oder Set.of(FROM_MODE) wenn du nicht umbenennst

		NetworkUtils.cleanNetwork(network, Set.of(FROM_MODE)); // "bike"

		simplifyWithBikeInfra(network);
		removeNonConnectingService(network);

		//NetworkUtils.simplifyNetwork(network);
		renameMode(network, FROM_MODE, TO_MODE);
//		NetworkUtils.cleanNetwork(network, Set.of(TO_MODE));   // optional
//		NetworkUtils.simplifyNetwork(network);                // optional, wenn du ganz sicher sein willst

		new NetworkWriter(network).write(outputFile);
	}

	/// rename from bike to bicycle
	/// why? would need to change the OsmBicycleReader (?)
	static void renameMode(Network network, String from, String to) {
		for (var link : network.getLinks().values()) {

			// allowedModes: bike -> bicycle
			var modes = new HashSet<>(link.getAllowedModes());
			if (modes.remove(from)) {
				modes.add(to);
				link.setAllowedModes(modes);
			}

			// Attribute-Key: "bike" -> "bicycle" (OsmBicycleReader setzt bicycle-restriction unter key="bike")
			Object val = link.getAttributes().getAttribute(from);
			if (val != null) {
				link.getAttributes().putAttribute(to, val);
				link.getAttributes().removeAttribute(from);
			}
		}
	}

	/// bicycle simplifier
	/// why we need this?
	/// the default simplifier ignors the additional bicycle attributes
	/// TODO: this simplifier does not simplify well enough ("allowed_speed" issue)
	private static final String BICYCLE_INFRA = "bicycle_infra";
	private static final String SURFACE = "surface";
	private static final String TYPE = "type";
	private static final String BICYCLE = "bike"; // vor dem rename
	private static final String SMOOTHNESS = "smoothness";

	static void simplifyWithBikeInfra(Network network) {

		BiPredicate<Link, Link> attrsMustMatch = (a, b) ->
			Objects.equals(a.getAttributes().getAttribute(BICYCLE_INFRA), b.getAttributes().getAttribute(BICYCLE_INFRA))
				&& Objects.equals(a.getAttributes().getAttribute(TYPE), b.getAttributes().getAttribute(TYPE))
				&& Objects.equals(a.getAttributes().getAttribute(SURFACE), b.getAttributes().getAttribute(SURFACE))
				&& Objects.equals(a.getAttributes().getAttribute(BICYCLE), b.getAttributes().getAttribute(BICYCLE))
				&& Objects.equals(a.getAttributes().getAttribute(SMOOTHNESS), b.getAttributes().getAttribute(SMOOTHNESS));

		var simplifier = NetworkSimplifier.createNetworkSimplifier(network);
		simplifier.setMergeLinkStats(false);

		// 1) Merge-Regel
		simplifier.registerIsMergeablePredicate(attrsMustMatch);

		// 2) Attribute auf neuen Link übertragen (da sie gleich sind, reicht von einem)
		simplifier.registerTransferAttributesConsumer((inOut, newLink) -> {
			Link in = inOut.getFirst();

			copyIfPresent(in, newLink, BICYCLE_INFRA);
			copyIfPresent(in, newLink, TYPE);
			copyIfPresent(in, newLink, SURFACE);
			copyIfPresent(in, newLink, BICYCLE);
			copyIfPresent(in, newLink, SMOOTHNESS);
		});

		simplifier.run(network);
	}

	private static void copyIfPresent(Link from, Link to, String key) {
		Object v = from.getAttributes().getAttribute(key);
		if (v != null) to.getAttributes().putAttribute(key, v);
	}

	///
	///
	///  clean the service links if not necessary
	static boolean isService(Link l) {
		Object t = l.getAttributes().getAttribute("type"); // oder NetworkUtils.getType(l)
		if (t == null) return false;
		String s = t.toString();
		s = s.replaceFirst("^highway\\.", ""); // falls "highway.service"
		return "service".equals(s);
	}

	static boolean hasNonServiceIncidentLink(Node n) {
		for (Link l : n.getInLinks().values()) if (!isService(l)) return true;
		for (Link l : n.getOutLinks().values()) if (!isService(l)) return true;
		return false;
	}

	static void removeNonConnectingService(Network network) {

		Set<Id<Link>> visited = new HashSet<>();
		List<Id<Link>> linksToRemove = new ArrayList<>();

		for (Link seed : network.getLinks().values()) {
			if (!isService(seed) || visited.contains(seed.getId())) continue;

			// BFS über Service-Komponente
			Deque<Link> q = new ArrayDeque<>();
			q.add(seed);
			visited.add(seed.getId());

			List<Link> componentLinks = new ArrayList<>();
			Set<Id<Node>> componentNodes = new HashSet<>();
			Set<Id<Node>> dockingNodes = new HashSet<>();

			while (!q.isEmpty()) {
				Link l = q.poll();
				componentLinks.add(l);

				Node a = l.getFromNode();
				Node b = l.getToNode();
				componentNodes.add(a.getId());
				componentNodes.add(b.getId());

				// Docking: Knoten berührt irgendeinen Nicht-Service-Link
				if (hasNonServiceIncidentLink(a)) dockingNodes.add(a.getId());
				if (hasNonServiceIncidentLink(b)) dockingNodes.add(b.getId());

				// Nachbarn: alle Service-Links, die an a oder b hängen
				for (Link nl : a.getInLinks().values()) if (isService(nl) && visited.add(nl.getId())) q.add(nl);
				for (Link nl : a.getOutLinks().values()) if (isService(nl) && visited.add(nl.getId())) q.add(nl);
				for (Link nl : b.getInLinks().values()) if (isService(nl) && visited.add(nl.getId())) q.add(nl);
				for (Link nl : b.getOutLinks().values()) if (isService(nl) && visited.add(nl.getId())) q.add(nl);
			}

			// Entscheidung: “keine Verbindungswirkung” wenn <= 1 Docking-Knoten
			if (dockingNodes.size() <= 1) {
				for (Link l : componentLinks) linksToRemove.add(l.getId());
			} else {
				trimServiceComponent(network, componentLinks, dockingNodes);
			}
		}

		// Entfernen
		linksToRemove.forEach(network::removeLink);

		// Aufräumen: verwaiste Knoten raus
		NetworkUtils.removeNodesWithoutLinks(network);
	}

	static void trimServiceComponent(Network network, List<Link> componentLinks, Set<Id<Node>> dockingNodes) {

		// Node -> set of neighbor nodes (ungerichtet, UNIQUE)
		Map<Id<Node>, Set<Id<Node>>> nbr = new HashMap<>();
		// NodePair -> link ids (kann mehrere parallel links enthalten)
		Map<String, List<Id<Link>>> pairToLinks = new HashMap<>();

		for (Link l : componentLinks) {
			Id<Node> u = l.getFromNode().getId();
			Id<Node> v = l.getToNode().getId();

			nbr.computeIfAbsent(u, k -> new HashSet<>()).add(v);
			nbr.computeIfAbsent(v, k -> new HashSet<>()).add(u);

			String key = edgeKey(u, v);
			pairToLinks.computeIfAbsent(key, k -> new ArrayList<>()).add(l.getId());
		}

		Deque<Id<Node>> q = new ArrayDeque<>();

		for (var e : nbr.entrySet()) {
			if (e.getValue().size() <= 1 && !dockingNodes.contains(e.getKey())) {
				q.add(e.getKey());
			}
		}

		Set<String> removedPairs = new HashSet<>();

		while (!q.isEmpty()) {
			Id<Node> leaf = q.poll();
			Set<Id<Node>> neigh = nbr.get(leaf);
			if (neigh == null || neigh.size() > 1 || dockingNodes.contains(leaf)) continue;

			if (neigh.isEmpty()) continue;

			Id<Node> other = neigh.iterator().next();

			String key = edgeKey(leaf, other);
			removedPairs.add(key);

			// remove connection both sides
			nbr.get(leaf).remove(other);
			nbr.get(other).remove(leaf);

			if (nbr.get(other).size() <= 1 && !dockingNodes.contains(other)) {
				q.add(other);
			}
		}

		// remove all links belonging to removed node pairs
		for (String key : removedPairs) {
			for (Id<Link> lid : pairToLinks.getOrDefault(key, List.of())) {
				network.removeLink(lid);
			}
		}
	}

	private static String edgeKey(Id<Node> a, Id<Node> b) {
		return a.toString().compareTo(b.toString()) < 0
			? a + "_" + b
			: b + "_" + a;
	}


}
