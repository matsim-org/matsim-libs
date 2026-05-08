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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
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
 *   <li>Move OSM-derived attributes under the {@code osm:} prefix to clearly
 *       separate them from pipeline-internal attributes.</li>
 *   <li>{@link NetworkUtils#cleanNetwork} drops isolated components.</li>
 *   <li>Bicycle-aware simplification merges consecutive links only when their
 *       infra-relevant attributes match (standard simplifier ignores those).</li>
 *   <li>Service-link cleanup removes service dead-ends and hairline branches
 *       that don't actually connect anything useful.</li>
 *   <li>Optionally rename the network mode {@code "bike"} to whatever the
 *       caller requested via {@code --mode}.</li>
 *   <li>For every surviving link, sample its elevation profile and attach the
 *       five KPIs (averageElevation, gradient, maxGradient, elevationGain,
 *       elevationLoss).</li>
 *   <li>Write the MATSim XML.</li>
 * </ol>
 *
 * <p>Elevation KPIs are deliberately computed <em>after</em> the simplifier has
 * run: after merging, link lengths are longer and there are fewer of them, so
 * we sample only what survives.
 *
 * <p>TODO: also move the remaining OSM-derived attributes under the {@code osm:}
 * prefix once their renames are sorted out:
 * <ul>
 *   <li>{@code "type"} → {@code "osm:highway"} (still carries the {@code "highway."}
 *       value prefix; see {@link ServiceLinkCleaner})</li>
 *   <li>{@code "origid"} → {@code "osm:way_id"} (carries merge semantics from
 *       {@link NetworkSimplifier})</li>
 * </ul>
 * Both touch additional code paths and are deferred to a separate commit.
 */
@Command(
	name = "bicycle-network",
	description = "Builds a MATSim bicycle network from OSM + DEM elevation data.",
	showDefaultValues = true,
	mixinStandardHelpOptions = true
)
public class BicycleNetworkPipeline implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(BicycleNetworkPipeline.class);

	// ---- CLI options -----------------------------------------------------------

	@Option(names = "--input", required = true, description = "Path to OSM input file (.osm.pbf)")
	private Path input;

	@Option(names = "--dem", required = true, description = "Path to DEM GeoTIFF")
	private Path dem;

	@Option(names = "--dem-crs", required = true,
		description = "CRS of the DEM GeoTIFF, e.g. EPSG:32632 for Sonny's German DTM")
	private String demCRS;

	@Option(names = "--output", required = true, description = "Path to output network (.xml.gz)")
	private Path output;

	// TODO maybe switch to CrsOptions mixin from matsim-application later.
	@Option(names = "--crs", required = true, description = "Output CRS (e.g. EPSG:25832)")
	private String outputCRS;

	@Option(names = "--mode",
		description = "Network mode name to assign to cyclable links. Default: ${DEFAULT-VALUE}.",
		defaultValue = "bike")
	private String mode;

	@Option(names = "--ele-sample-step",
		description = "Distance between elevation samples along a link in meters (default: ${DEFAULT-VALUE})",
		defaultValue = "10.0")
	private double eleSampleStepM;

	@Option(names = "--ele-noise-tolerance",
		description = "Douglas-Peucker vertical tolerance for smoothing the profile in meters (default: ${DEFAULT-VALUE})",
		defaultValue = "3.0")
	private double eleNoiseToleranceM;


	// ---- attribute keys --------------------------------------------------------

	public static final String LINK_ATTR_BICYCLE_INFRA = "bicycle_infra";
	public static final String LINK_ATTR_GRADIENT = "gradient";
	public static final String LINK_ATTR_MAX_GRADIENT = "maxGradient";
	public static final String LINK_ATTR_ELEVATION_GAIN = "elevationGain";
	public static final String LINK_ATTR_ELEVATION_LOSS = "elevationLoss";

	private static final String OSM_PREFIX = "osm:";

	/**
	 * OSM tag values that {@link OsmBicycleReader} writes verbatim into link
	 * attributes; these are moved under the "osm:" prefix in step 1b.
	 *
	 * <p>Note: "type" and "origid" are intentionally NOT in this list yet --
	 * see the TODO in the class JavaDoc.
	 */
	private static final List<String> OSM_TAG_ATTR_KEYS = List.of(
		"bicycle", "surface", "smoothness", "cycleway"
	);

	/**
	 * Optional raw OSM tags to copy onto links via {@link TagCopy} (with "osm:"
	 * prefix). Empty by default; populate to forward additional OSM tags that
	 * {@link OsmBicycleReader} doesn't write itself.
	 */
	private static final List<String> TAGS_TO_COPY = List.of();

	/**
	 * Attribute keys used by the bicycle-aware simplifier to decide if two
	 * links may merge. If they differ on any of these, keep them separate.
	 *
	 * <p>"type" stays unprefixed for now (see TODO above).
	 */
	private static final String[] SIMPLIFY_MATCH_KEYS = {
		LINK_ATTR_BICYCLE_INFRA,
		"type",
		OSM_PREFIX + "surface",
		OSM_PREFIX + "bicycle",
		OSM_PREFIX + "smoothness"
	};


	// ============================================================================

	/**
	 * @author smetzler
	 */
	public static void main(String[] args) {
		new BicycleNetworkPipeline().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		var elevationParser = new ElevationDataParser(dem.toString(), outputCRS, demCRS);
		var transformation = TransformationFactory.getCoordinateTransformation(
			TransformationFactory.WGS84, outputCRS);

		var classifier = new BicycleInfraClassifier();
		var tagCopy = new TagCopy(TAGS_TO_COPY, OSM_PREFIX);
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
			.read(input.toString());
		log.info("After OSM read: {} nodes, {} links",
			network.getNodes().size(), network.getLinks().size());
		logBicycleInfraDistribution(network, "after OSM read");

		// ---- 1b. move OSM-derived attributes under "osm:" prefix ------------
		prefixOsmAttributes(network);

		// ---- 2. drop isolated components -------------------------------------
		NetworkUtils.cleanNetwork(network, Set.of(TransportMode.bike));
		log.info("After cleanNetwork: {} links", network.getLinks().size());

		// ---- 3. bicycle-aware simplification ---------------------------------
		simplifyWithBikeInfra(network);
		log.info("After simplification (1st pass): {} links", network.getLinks().size());

		// ---- 4. remove service dead-ends and hairline branches ---------------
		new ServiceLinkCleaner().run(network);
		log.info("After service-link cleanup: {} links", network.getLinks().size());

		// ---- 5. second simplification pass; service cleanup may have created
		//        new merge candidates -----------------------------------------
		simplifyWithBikeInfra(network);
		log.info("After simplification (2nd pass): {} links", network.getLinks().size());

		// ---- 6. rename mode if requested (no-op when --mode bike) -----------
		renameMode(network, TransportMode.bike, mode);

		// ---- 7. elevation KPIs on the final link set -------------------------
		int counted = 0;
		for (Link link : network.getLinks().values()) {
			attachLinkElevationKpis(link, elevationParser, eleSampleStepM, eleNoiseToleranceM);
			counted++;
		}
		log.info("Attached elevation KPIs to {} links (sample step = {} m, noise tolerance = {} m).",
			counted, eleSampleStepM, eleNoiseToleranceM);
		logBicycleInfraDistribution(network, "in final network");

		// ---- 8. write --------------------------------------------------------
		new NetworkWriter(network).write(output.toString());

		return 0;
	}


	// =========================================================================
	// OSM attribute prefixing
	// =========================================================================

	/**
	 * Move OSM-derived attributes (those listed in {@link #OSM_TAG_ATTR_KEYS})
	 * under the "osm:" prefix to make their provenance explicit and to keep
	 * them separate from pipeline-internal attributes.
	 */
	private static void prefixOsmAttributes(Network network) {
		for (Link link : network.getLinks().values()) {
			for (String key : OSM_TAG_ATTR_KEYS) {
				Object value = link.getAttributes().getAttribute(key);
				if (value != null) {
					link.getAttributes().putAttribute(OSM_PREFIX + key, value);
					link.getAttributes().removeAttribute(key);
				}
			}
		}
	}


	// =========================================================================
	// Bicycle infra distribution
	// =========================================================================

	/**
	 * Counts links by their {@code bicycle_infra} attribute and logs a sorted
	 * summary table including link counts, total length in km, and percentages.
	 * Useful as a sanity check during scenario development -- if the {@code NONE}
	 * count or {@code NEEDS_CLARIFICATION} count jumps unexpectedly between two
	 * OSM extracts, this is the place where you'd notice.
	 *
	 * <p>Categories with zero count are omitted; links whose attribute value
	 * doesn't match any known {@link BicycleInfraCategory} are tallied
	 * separately under "(unparseable)".
	 *
	 * <p>Sorted by total length, descending. Ties broken by enum declaration
	 * order.
	 *
	 * @param label short context tag included in the log header,
	 *              e.g. {@code "after OSM read"} or {@code "in final network"}
	 */
	private static void logBicycleInfraDistribution(Network network, String label) {
		EnumMap<BicycleInfraCategory, Integer> counts = new EnumMap<>(BicycleInfraCategory.class);
		EnumMap<BicycleInfraCategory, Double> lengthsM = new EnumMap<>(BicycleInfraCategory.class);
		int unparseableCount = 0;
		double unparseableLengthM = 0;
		int totalCount = 0;
		double totalLengthM = 0;

		for (Link link : network.getLinks().values()) {
			Object raw = link.getAttributes().getAttribute(LINK_ATTR_BICYCLE_INFRA);
			double len = link.getLength();
			totalCount++;
			totalLengthM += len;
			if (raw == null) {
				unparseableCount++;
				unparseableLengthM += len;
				continue;
			}
			try {
				BicycleInfraCategory cat = BicycleInfraCategory.valueOf(raw.toString());
				counts.merge(cat, 1, Integer::sum);
				lengthsM.merge(cat, len, Double::sum);
			} catch (IllegalArgumentException e) {
				unparseableCount++;
				unparseableLengthM += len;
			}
		}

		if (totalCount == 0) {
			log.info("Bicycle infra distribution ({}): no links.", label);
			return;
		}

		// Find the longest category name for column alignment.
		int nameWidth = 0;
		for (BicycleInfraCategory cat : counts.keySet()) {
			nameWidth = Math.max(nameWidth, cat.name().length());
		}
		if (unparseableCount > 0) {
			nameWidth = Math.max(nameWidth, "(unparseable)".length());
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Bicycle infra distribution (").append(label).append("):\n");

		// Sort by descending total length, ties broken by enum declaration order.
		List<BicycleInfraCategory> sorted = new ArrayList<>(counts.keySet());
		sorted.sort((a, b) -> {
			int byLength = Double.compare(lengthsM.getOrDefault(b, 0.0), lengthsM.getOrDefault(a, 0.0));
			return byLength != 0 ? byLength : a.compareTo(b);
		});
		for (BicycleInfraCategory cat : sorted) {
			sb.append(formatRow(cat.name(), counts.get(cat), totalCount,
				lengthsM.getOrDefault(cat, 0.0), totalLengthM, nameWidth));
		}

		if (unparseableCount > 0) {
			sb.append(formatRow("(unparseable)", unparseableCount, totalCount,
				unparseableLengthM, totalLengthM, nameWidth));
		}
		sb.append(formatRow("Total", totalCount, totalCount, totalLengthM, totalLengthM, nameWidth));

		log.info(sb.toString());
	}

	private static String formatRow(String name, int count, int totalCount,
									double lengthM, double totalLengthM, int nameWidth) {
		double countPct = 100.0 * count / totalCount;
		double lengthPct = totalLengthM > 0 ? 100.0 * lengthM / totalLengthM : 0.0;
		return String.format(Locale.ROOT,
			"  %-" + nameWidth + "s  %8d (%5.1f%%)  %9.1f km (%5.1f%%)%n",
			name, count, countPct, lengthM / 1000.0, lengthPct);
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
	 * Rename a mode in each link's allowed-modes set. If {@code from} equals
	 * {@code to}, this is a no-op.
	 *
	 * <p>Note: the OSM {@code bicycle=...} restriction value is no longer
	 * stored under the mode-name key -- it now lives at "osm:bicycle" and is
	 * unaffected by the mode rename.
	 */
	private static void renameMode(Network network, String from, String to) {
		if (from.equals(to)) {
			log.info("Network mode is already '{}', no rename needed.", to);
			return;
		}
		log.info("Renaming network mode '{}' -> '{}'.", from, to);
		for (Link link : network.getLinks().values()) {
			Set<String> modes = new HashSet<>(link.getAllowedModes());
			if (modes.remove(from)) {
				modes.add(to);
				link.setAllowedModes(modes);
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

			// Bicycle-relevant attributes are already identical on both
			// inputs by construction of the predicate.
			for (String key : SIMPLIFY_MATCH_KEYS) {
				Object v = a.getAttributes().getAttribute(key);
				if (v != null) newLink.getAttributes().putAttribute(key, v);
			}

			// Merge origid values, deduplicating across the two inputs.
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
	 * Merge two origid values into a single hyphen-separated string in which
	 * each way ID appears at most once and original order is preserved. Both
	 * inputs may themselves already be hyphen-separated multi-IDs from an
	 * earlier merge.
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
}
