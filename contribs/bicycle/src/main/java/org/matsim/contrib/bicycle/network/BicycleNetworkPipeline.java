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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import org.matsim.application.MATSimAppCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.function.BiPredicate;


/**
 * End-to-end pipeline for building a MATSim bicycle network from an OSM file,
 * enriched with cycling infrastructure classification and DEM-based elevation
 * KPIs.
 *
 * <p>Pipeline order:
 * <ol>
 *   <li>Read OSM with {@link OsmBicycleReader}. During read, each link's
 *       endpoints get a Z coordinate from the DEM, and {@link BicycleLinkPolicy}
 *       stamps the {@code bicycle_infra} attribute and enforces access rules.</li>
 *   <li>{@link NetworkUtils#cleanNetwork} drops isolated components.</li>
 *   <li>Bicycle-aware simplification merges consecutive links only when their
 *       infra-relevant attributes match (standard simplifier ignores those).</li>
 *   <li>Service-link cleanup removes service dead-ends and hairline branches
 *       that don't actually connect anything useful.</li>
 *   <li>Rename the mode {@link TransportMode#bike} → {@code "bicycle"} so
 *       downstream bicycle-contrib code picks it up.</li>
 *   <li>For every surviving link, sample its elevation profile and attach the
 *       five KPIs (averageElevation, gradient, maxGradient, elevationGain,
 *       elevationLoss).</li>
 *   <li>Write the MATSim XML.</li>
 * </ol>
 *
 * <p>Elevation KPIs are deliberately computed <em>after</em> the simplifier has
 * run: after merging, link lengths are longer and there are fewer of them, so
 * we sample only what survives. A simpler counterpart that skips the infra and
 * simplification steps is {@link CreateBicycleNetworkWithElevation}.
 */
@Command(
	name = "bicycle-network",
	description = "Builds a MATSim bicycle network from OSM + DEM elevation data.",
	showDefaultValues = true,
	mixinStandardHelpOptions = true
)
public class BicycleNetworkPipeline implements MATSimAppCommand {

	@Option(names = "--mode",
		description = "Network mode name to assign to cyclable links. Default: ${DEFAULT-VALUE}.",
		defaultValue = "bike")
	private String mode;

	@Option(names = "--input", required = true, description = "Path to OSM input file (.osm.pbf)")
	private Path input;

	@Option(names = "--dem", required = true, description = "Path to DEM GeoTIFF")
	private Path dem;

	@Option(names = "--dem-crs", required = true,
		description = "CRS of the DEM GeoTIFF, e.g. EPSG:32632 for Sonny's German DTM")
	private String demCRS;

	@Option(names = "--output", required = true, description = "Path to output network (.xml.gz)")
	private Path output;

	// TODO maybe change to this later
	// @Mixin
	//private CrsOptions crs = new CrsOptions();   // gibt --target-crs / --input-crs etc.
	@Option(names = "--crs", required = true, description = "Output CRS (e.g. EPSG:25832)")
	private String outputCRS;


	// ---- elevation tunables ----------------------------------------------------

	@Option(names = "--ele-sample-step",
		description = "Distance between elevation samples along a link in meters (default: ${DEFAULT-VALUE})",
		defaultValue = "10.0")
	private double eleSampleStepM;

	@Option(names = "--ele-noise-tolerance",
		description = "Douglas-Peucker vertical tolerance for smoothing the profile in meters (default: ${DEFAULT-VALUE})",
		defaultValue = "3.0")
	private double eleNoiseToleranceM;


	// ---- mode renaming ---------------------------------------------------------

	/**
	 * OsmBicycleReader emits this; we rename to TO_MODE before writing.
	 */
	private static final String FROM_MODE = TransportMode.bike; // "bike"
	private static final String TO_MODE = "bicycle";

	// ---- tags ------------------------------------------------------------------

	/**
	 * Optional raw OSM tags to copy onto links (with "osm:" prefix). Empty = none.
	 */
	private static final List<String> TAGS_TO_COPY = List.of();

	// ---- link attribute keys ---------------------------------------------------

	public static final String LINK_ATTR_BICYCLE_INFRA = "bicycle_infra";
	public static final String LINK_ATTR_GRADIENT = "gradient";
	public static final String LINK_ATTR_MAX_GRADIENT = "maxGradient";
	public static final String LINK_ATTR_ELEVATION_GAIN = "elevationGain";
	public static final String LINK_ATTR_ELEVATION_LOSS = "elevationLoss";

	// Attribute keys used by the bicycle-aware simplifier to decide if two links
	// may merge. If they differ on any of these, keep them separate.
	private static final String[] SIMPLIFY_MATCH_KEYS = {
		LINK_ATTR_BICYCLE_INFRA, "type", "surface", FROM_MODE, "smoothness"
	};


	// ============================================================================

	private static final Logger log = LogManager.getLogger(BicycleNetworkPipeline.class);

	public static void main(String[] args) {
		new BicycleNetworkPipeline().execute(args);
	}

	@Override
	public Integer call() throws Exception {


		var elevationParser = new ElevationDataParser(dem.toString(), outputCRS, demCRS);
		var transformation = TransformationFactory.getCoordinateTransformation(
			TransformationFactory.WGS84, outputCRS);

		var classifier = new BicycleInfraClassifier();
		var tagCopy = new TagCopy(TAGS_TO_COPY, "osm:");
		var policy = new BicycleLinkPolicy(classifier, tagCopy);

		// ---- 1. OSM read: stamps node elevations + infra on each new link ----
		Network network = new OsmBicycleReader.Builder()
			.setCoordinateTransformation(transformation)
			.setAfterLinkCreated((link, tags, direction) -> {
				addNodeElevation(link.getFromNode(), elevationParser);
				addNodeElevation(link.getToNode(), elevationParser);
				policy.apply(link, tags, direction);
			})
			.build()
			.read(input);

//		// ---- 1b. raw OSM attributes prefixen ---------------------------------
//		renameLinkAttributes(network, Map.of(
//			"type", "osm:type",
//			"surface", "osm:surface",
//			"smoothness", "osm:smoothness"
//			//	"origid", "osm:way_id"        // siehe Punkt unten: Dedup
//		));

		// ---- 2. drop isolated components -------------------------------------
		NetworkUtils.cleanNetwork(network, Set.of(FROM_MODE));

		// ---- 3. bicycle-aware simplification ---------------------------------
		simplifyWithBikeInfra(network);

		// ---- 3b. origid -> osm:way_id -----------------------------------------
		//renameLinkAttributes(network, Map.of("origid", "osm:way_id"));

		// ---- 4. remove service dead-ends and hairline branches ---------------
		removeNonConnectingService(network);

		// ---- 4b. second pass: service cleanup may have created new isolated components
		simplifyWithBikeInfra(network);

		// ---- 5. rename mode: bike -> bicycle ---------------------------------
		renameMode(network, FROM_MODE, TO_MODE);

		// ---- 6. elevation KPIs on the final link set -------------------------
		int counted = 0;
		for (Link link : network.getLinks().values()) {
			attachLinkElevationKpis(link, elevationParser, eleSampleStepM, eleNoiseToleranceM);
			counted++;
		}

		log.info("Attached elevation KPIs to {} links (sample step = {} m, noise tolerance = {} m).",
			counted, eleSampleStepM, eleNoiseToleranceM);

		// ---- 7. write --------------------------------------------------------
		new NetworkWriter(network).write(output.toString());

		return 0;
	}


	// =========================================================================
	// Elevation
	// =========================================================================

	private static synchronized void addNodeElevation(Node node, ElevationDataParser parser) {
		if (!node.getCoord().hasZ()) {
			double z = parser.getElevation(node.getCoord());
			node.setCoord(CoordUtils.createCoord(node.getCoord().getX(), node.getCoord().getY(), z));
		}
	}

	private static void attachLinkElevationKpis(Link link, ElevationDataParser parser,
												double sampleStep, double noiseTolerance) {
		LinkElevationProfile.Kpis k = LinkElevationProfile.compute(
			link, sampleStep, noiseTolerance, parser);

		// Elevations in meters — round to 1 decimal (matches DEM resolution).
		link.getAttributes().putAttribute(BicycleUtils.AVERAGE_ELEVATION, round(k.averageElevation(), 1));
		link.getAttributes().putAttribute(LINK_ATTR_ELEVATION_GAIN, round(k.elevationGain(), 1));
		link.getAttributes().putAttribute(LINK_ATTR_ELEVATION_LOSS, round(k.elevationLoss(), 1));

		// Dimensionless ratios — 3 decimals = 0.1% resolution.
		link.getAttributes().putAttribute(LINK_ATTR_GRADIENT, round(k.gradient(), 3));
		link.getAttributes().putAttribute(LINK_ATTR_MAX_GRADIENT, round(k.maxGradient(), 3));
	}

	private static double round(double v, int decimals) {
		double factor = Math.pow(10, decimals);
		return Math.round(v * factor) / factor;
	}

	// =========================================================================
	// Mode rename
	// =========================================================================

	/**
	 * Rename a mode both in each link's allowed-modes set and in any attribute
	 * stored under the old mode name as key (the OsmBicycleReader writes a
	 * bicycle-restriction attribute under key "bike").
	 */
	private static void renameMode(Network network, String from, String to) {
		for (Link link : network.getLinks().values()) {
			Set<String> modes = new HashSet<>(link.getAllowedModes());
			if (modes.remove(from)) {
				modes.add(to);
				link.setAllowedModes(modes);
			}

			Object val = link.getAttributes().getAttribute(from);
			if (val != null) {
				link.getAttributes().putAttribute(to, val);
				link.getAttributes().removeAttribute(from);
			}
		}
	}

	// =========================================================================
	// Bicycle-aware simplification
	// =========================================================================

	/**
	 * Merges consecutive links via {@link NetworkSimplifier} only when they
	 * agree on the bicycle-relevant attributes. The default simplifier would
	 * happily merge across infra changes, losing that information.
	 */
	private static void simplifyWithBikeInfra(Network network) {

		BiPredicate<Link, Link> attrsMustMatch = (a, b) -> {
			for (String key : SIMPLIFY_MATCH_KEYS) {
				if (!Objects.equals(a.getAttributes().getAttribute(key),
					b.getAttributes().getAttribute(key))) return false;
			}
			return true;
		};

		var simplifier = NetworkSimplifier.createNetworkSimplifier(network);
		simplifier.setMergeLinkStats(false);
		simplifier.registerIsMergeablePredicate(attrsMustMatch);

		// When two links merge, carry over the attributes from the first one
		// (they're identical to the second by construction of the predicate).
		simplifier.registerTransferAttributesConsumer((inOut, newLink) -> {
			Link a = inOut.getFirst();
			Link b = inOut.getSecond();

			// bicycle-relevante Attribute: sind per Predicate bereits identisch
			for (String key : SIMPLIFY_MATCH_KEYS) {
				Object v = a.getAttributes().getAttribute(key);
				if (v != null) newLink.getAttributes().putAttribute(key, v);
			}

			// origid merge mit Dedup
			Object idA = a.getAttributes().getAttribute("origid");
			Object idB = b.getAttributes().getAttribute("origid");
			String merged = mergeOrigIds(idA, idB);
			if (merged != null) {
				newLink.getAttributes().putAttribute("origid", merged);
			}
		});

		simplifier.run(network);
	}

	/**
	 * Merged zwei origid-Werte zu einem String, in dem jede Way-ID nur einmal
	 * vorkommt und die Reihenfolge erhalten bleibt. Beide Inputs können selbst
	 * schon mit '-' verkettete Mehrfach-IDs aus einem früheren Merge sein.
	 */
	static String mergeOrigIds(Object idA, Object idB) {
		Set<String> seen = new LinkedHashSet<>();
		addOrigIds(seen, idA);
		addOrigIds(seen, idB);
		if (seen.isEmpty()) return null;
		return String.join("-", seen);
	}

	private static void addOrigIds(Set<String> acc, Object id) {
		if (id == null) return;
		String s = id.toString();
		if (s.isBlank()) return;
		for (String part : s.split("-")) {
			if (!part.isBlank()) acc.add(part);
		}
	}

	// =========================================================================
	// Service link cleanup
	// =========================================================================

	private static boolean isService(Link l) {
		Object t = l.getAttributes().getAttribute("type");  //"osm:type"
		if (t == null) return false;
		String s = t.toString().replaceFirst("^highway\\.", "");
		return "service".equals(s);
	}

	private static boolean hasNonServiceIncidentLink(Node n) {
		for (Link l : n.getInLinks().values()) if (!isService(l)) return true;
		for (Link l : n.getOutLinks().values()) if (!isService(l)) return true;
		return false;
	}

	/**
	 * Walks the connected components made of service links and decides what to
	 * do with each: if a component touches the rest of the graph at 0-1 nodes,
	 * it can't provide a useful shortcut and is removed entirely. Otherwise,
	 * we trim off hair-like dead-end branches while keeping whatever actually
	 * connects two entry points.
	 */
	private static void removeNonConnectingService(Network network) {

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
				trimServiceComponent(network, componentLinks, dockingNodes);
			}
		}

		linksToRemove.forEach(network::removeLink);
		NetworkUtils.removeNodesWithoutLinks(network);
	}

	/**
	 * Iteratively peels off leaf nodes (degree <= 1, not a docking node) from
	 * the service component. What remains is either empty or a chain/tree
	 * connecting at least two docking nodes.
	 */
	private static void trimServiceComponent(Network network, List<Link> componentLinks, Set<Id<Node>> dockingNodes) {

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



