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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
public class BicycleNetworkPipeline {

	// ---- paths -----------------------------------------------------------------

	//private static final String inputOsmFile = "C://Users/metz_so/Workspace/data/berlin-260122.osm.pbf";
	private static final String inputOsmFile = "C://Users/metz_so/Workspace/data/neukoelln.osm.pbf";
	private static final String inputTiffFile = "C:/Users/metz_so/Downloads/DTM Germany 20m v3b by Sonny.tif";
	private static final String outputFile = "C://Users/metz_so/Workspace/data/networks/matsim-network_berlin_bicycle_full.xml.gz";

	// ---- CRS -------------------------------------------------------------------

	private static final String outputCRS = "EPSG:25832";

	// ---- elevation tunables ----------------------------------------------------

	/**
	 * Distance between elevation samples along a link (meters). See README.
	 */
	private static final double SAMPLE_STEP_M = 10.0;

	/**
	 * Douglas-Peucker vertical tolerance for smoothing the profile (meters).
	 */
	private static final double NOISE_TOLERANCE_M = 3.0;

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

	public static void main(String[] args) {

		var elevationParser = new ElevationDataParser(inputTiffFile, outputCRS);
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
			.read(inputOsmFile);

		// ---- 2. drop isolated components -------------------------------------
		NetworkUtils.cleanNetwork(network, Set.of(FROM_MODE));

		// ---- 3. bicycle-aware simplification ---------------------------------
		simplifyWithBikeInfra(network);

		// ---- 4. remove service dead-ends and hairline branches ---------------
		removeNonConnectingService(network);

		// ---- 5. rename mode: bike -> bicycle ---------------------------------
		renameMode(network, FROM_MODE, TO_MODE);

		// ---- 6. elevation KPIs on the final link set -------------------------
		int counted = 0;
		for (Link link : network.getLinks().values()) {
			attachLinkElevationKpis(link, elevationParser);
			counted++;
		}
		System.out.println("Attached elevation KPIs to " + counted + " links "
			+ "(sample step = " + SAMPLE_STEP_M + " m, noise tolerance = "
			+ NOISE_TOLERANCE_M + " m).");

		// ---- 7. write --------------------------------------------------------
		new NetworkWriter(network).write(outputFile);
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

	private static void attachLinkElevationKpis(Link link, ElevationDataParser parser) {
		LinkElevationProfile.Kpis k = LinkElevationProfile.compute(
			link, SAMPLE_STEP_M, NOISE_TOLERANCE_M, parser);

		// Elevations in meters — round to 1 decimal (matches DEM resolution).
		link.getAttributes().putAttribute(BicycleUtils.AVERAGE_ELEVATION, round(k.averageElevation(), 1));
		link.getAttributes().putAttribute(LINK_ATTR_ELEVATION_GAIN, round(k.elevationGain(), 1));
		link.getAttributes().putAttribute(LINK_ATTR_ELEVATION_LOSS, round(k.elevationLoss(), 1));

		// Dimensionless ratios — 5 decimals = 0.001% resolution.
		link.getAttributes().putAttribute(LINK_ATTR_GRADIENT, round(k.gradient(), 5));
		link.getAttributes().putAttribute(LINK_ATTR_MAX_GRADIENT, round(k.maxGradient(), 5));
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
			Link source = inOut.getFirst();
			for (String key : SIMPLIFY_MATCH_KEYS) {
				Object v = source.getAttributes().getAttribute(key);
				if (v != null) newLink.getAttributes().putAttribute(key, v);
			}
		});

		simplifier.run(network);
	}

	// =========================================================================
	// Service link cleanup
	// =========================================================================

	private static boolean isService(Link l) {
		Object t = l.getAttributes().getAttribute("type");
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
