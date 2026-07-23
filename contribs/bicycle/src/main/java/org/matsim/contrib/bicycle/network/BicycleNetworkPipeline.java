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
import org.matsim.api.core.v01.Coord;
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
 * metrics.
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
 *       five metrics (averageElevation, gradient, maxGradient, elevationGain,
 *       elevationLoss).</li>
 *   <li>Write the MATSim XML.</li>
 * </ol>
 *
 * <p>Elevation metrics are deliberately computed <em>after</em> the simplifier has
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
 *
 * @author smetzler
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

	@Option(names = "--country",
		description = "Country profile for traffic-sign interpretation. "
			+ "Supported: de, at, generic. Default: ${DEFAULT-VALUE}. "
			+ "Use 'generic' if your country isn't listed; it skips traffic-sign matching "
			+ "and relies on tag-based classification only (cycleway=*, segregated=*, etc.).",
		defaultValue = "de")
	private String country;

	@Option(names = "--ele-sample-step",
		description = "Distance between elevation samples along a link in meters (default: ${DEFAULT-VALUE})",
		defaultValue = "10.0")
	private double eleSampleStepM;

	@Option(names = "--ele-noise-tolerance",
		description = "Douglas-Peucker vertical tolerance for smoothing the profile in meters (default: ${DEFAULT-VALUE})",
		defaultValue = "3.0")
	private double eleNoiseToleranceM;

	@Option(names = "--store-original-geometry", negatable = true,
		description = "Store the true OSM road course in the 'origgeom' link attribute so "
			+ "links keep their real shape through simplification. Use "
			+ "--no-store-original-geometry to switch it off. Default: ${DEFAULT-VALUE}.",
		defaultValue = "false")
	private boolean storeOriginalGeometry;


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
		BicycleOsmTags.BICYCLE,
		BicycleOsmTags.SURFACE,
		BicycleOsmTags.SMOOTHNESS,
		BicycleOsmTags.CYCLEWAY
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
	private static final List<String> SIMPLIFY_MATCH_KEYS = List.of(
		LINK_ATTR_BICYCLE_INFRA,
		"type",
		OSM_PREFIX + "surface",
		//OSM_PREFIX + "bicycle", // this could be removed in the future as "bicycle_infra" should be enough
		OSM_PREFIX + "smoothness",
		NetworkUtils.ALLOWED_SPEED
	);


	// ============================================================================

	public static void main(String[] args) {
		new BicycleNetworkPipeline().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		var elevationParser = new ElevationDataParser(dem.toString(), outputCRS, demCRS);
		var transformation = TransformationFactory.getCoordinateTransformation(
			TransformationFactory.WGS84, outputCRS);

		var profile = BicycleCountryProfiles.forCode(country);
		log.info("Using country profile: {}", profile.getClass().getSimpleName());
		var classifier = new BicycleInfraClassifier(profile);
		var tagCopy = new TagCopy(TAGS_TO_COPY, OSM_PREFIX);
		var policy = new BicycleLinkPolicy(classifier, tagCopy);

		// ---- 1. OSM read: stamps node elevations + infra on each new link ----
		Network network = new OsmBicycleReader.Builder()
			.setCoordinateTransformation(transformation)
			.setStoreOriginalGeometry(storeOriginalGeometry)
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
		normalizeOrigIdType(network);
		prefixOsmAttributes(network);

		// ---- 1c. repair reversed geometry on synthetic bike-reverse links -----
		if (storeOriginalGeometry) {
			int repaired = repairReversedGeometry(network);
			log.info("Repaired reversed link geometry on {} links.", repaired);
		}

		// ---- 2. drop isolated components -------------------------------------
		NetworkUtils.cleanNetwork(network, Set.of(TransportMode.bike));
		log.info("After cleanNetwork: {} links", network.getLinks().size());

		// ---- 3. bicycle-aware simplification ---------------------------------
		simplifyWithBikeInfra(network, storeOriginalGeometry);
		log.info("After simplification (1st pass): {} links", network.getLinks().size());

		// ---- 4. remove service dead-ends and hairline branches ---------------
		new ServiceLinkCleaner().run(network);
		log.info("After service-link cleanup: {} links", network.getLinks().size());

		// ---- 5. second simplification pass; service cleanup may have created
		//        new merge candidates -----------------------------------------
		simplifyWithBikeInfra(network, storeOriginalGeometry);
		log.info("After simplification (2nd pass): {} links", network.getLinks().size());

		// ---- 5b. geometry sanity check --------------------------------------
		if (storeOriginalGeometry) {
			logGeometryConsistency(network);
		}

		// ---- 6. rename mode if requested (no-op when --mode bike) -----------
		renameMode(network, TransportMode.bike, mode);

		// ---- 7. elevation metrics on the final link set ----------------------
		for (Link link : network.getLinks().values()) {
			attachElevationMetrics(link, elevationParser, eleSampleStepM, eleNoiseToleranceM);
		}
		log.info("Attached elevation metrics to {} links (sample step = {} m, noise tolerance = {} m).",
			network.getLinks().size(), eleSampleStepM, eleNoiseToleranceM);
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
	// Geometry consistency
	// =========================================================================

	/**
	 * Every link's stored geometry has to add up to its own length. A forgotten
	 * shared node, a reversed part or a dropped segment all break this, and
	 * nothing else in the pipeline would notice.
	 *
	 * <p>Warns rather than throws: the reader currently produces reversed
	 * geometry on {@code *_bike-reverse} links, so a hard failure here would
	 * block every build until that is fixed separately.
	 */
	private static void logGeometryConsistency(Network network) {
		int withGeometry = 0;
		List<Id<Link>> offenders = new ArrayList<>();

		for (Link link : network.getLinks().values()) {
			List<Node> geometry = NetworkUtils.getOriginalGeometry(link);
			// size 2 means no stored geometry -- a straight link, nothing to check
			if (geometry.size() < 3 || link.getLength() <= 0) continue;
			withGeometry++;

			double drawn = 0;
			for (int i = 1; i < geometry.size(); i++) {
				// projected: the end nodes carry a Z from the DEM, the points parsed
				// from origgeom do not. A 3D distance would compare different things.
				drawn += CoordUtils.calcProjectedEuclideanDistance(
					geometry.get(i - 1).getCoord(), geometry.get(i).getCoord());
			}
			if (Math.abs(drawn - link.getLength()) / link.getLength() > 0.02) {
				offenders.add(link.getId());
			}
		}

		log.info("Links with stored geometry: {} of {}", withGeometry, network.getLinks().size());
		if (!offenders.isEmpty()) {
			log.warn("{} links whose stored geometry does not match their length, e.g. {}",
				offenders.size(), offenders.subList(0, Math.min(5, offenders.size())));
		}
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

	private static void attachElevationMetrics(Link link, ElevationDataParser parser,
											   double sampleStep, double noiseTolerance) {
		LinkElevationProfile.Metrics m = LinkElevationProfile.compute(
			link, sampleStep, noiseTolerance, parser);

		// Elevations in meters — round to 1 decimal (matches DEM resolution).
		link.getAttributes().putAttribute(BicycleUtils.AVERAGE_ELEVATION, round(m.averageElevation(), 1));
		link.getAttributes().putAttribute(LINK_ATTR_ELEVATION_GAIN, round(m.elevationGain(), 1));
		link.getAttributes().putAttribute(LINK_ATTR_ELEVATION_LOSS, round(m.elevationLoss(), 1));

		// Dimensionless ratios — 3 decimals = 0.1% resolution.
		link.getAttributes().putAttribute(LINK_ATTR_GRADIENT, round(m.gradient(), 3));
		link.getAttributes().putAttribute(LINK_ATTR_MAX_GRADIENT, round(m.maxGradient(), 3));
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
	// Reversed geometry repair
	// =========================================================================

	/**
	 * Mirrors stored geometry that runs against its own link direction.
	 *
	 * <p>{@link OsmBicycleReader} copies every attribute onto its synthetic
	 * {@code *_bike-reverse} links, geometry included, but those links run the other
	 * way: their support points end up listed backwards and the link renders as a
	 * zig-zag. Detection is geometric rather than by link id, so it does not depend
	 * on the reader's naming and is a no-op once the geometry is correct.
	 *
	 * <p>Must run before the first simplification pass. Once two such links are
	 * merged, the reversal is no longer detectable.
	 *
	 * <p>TODO belongs in {@link OsmBicycleReader #createReverseBicycleLink}, where the
	 * correct order is known rather than inferred.
	 *
	 * @return the number of links whose geometry was mirrored
	 */
	private static int repairReversedGeometry(Network network) {
		int repaired = 0;
		for (Link link : network.getLinks().values()) {
			Object value = link.getAttributes().getAttribute(NetworkUtils.ORIG_GEOM);
			if (value == null) continue;

			String[] points = value.toString().trim().split("\\s+");
			// a single support point carries no order to get wrong
			if (points.length < 2) continue;

			Coord first = parseSupportPoint(points[0]);
			Coord last = parseSupportPoint(points[points.length - 1]);
			if (first == null || last == null) continue;

			// projected: the end nodes carry a z from the DEM, the parsed support
			// points do not. The 3D variant would fall back to 2D anyway, but log a
			// warning for every link it sees.
			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();
			double asIs = CoordUtils.calcProjectedEuclideanDistance(first, from)
				+ CoordUtils.calcProjectedEuclideanDistance(last, to);
			double mirrored = CoordUtils.calcProjectedEuclideanDistance(first, to)
				+ CoordUtils.calcProjectedEuclideanDistance(last, from);
			if (mirrored >= asIs) continue;

			StringBuilder reversed = new StringBuilder();
			for (int i = points.length - 1; i >= 0; i--) {
				reversed.append(points[i]).append(' ');
			}
			link.getAttributes().putAttribute(NetworkUtils.ORIG_GEOM, reversed.toString());
			repaired++;
		}
		return repaired;
	}

	/** One {@code nodeId,x,y} triple from an origgeom value, or null if malformed. */
	private static Coord parseSupportPoint(String token) {
		String[] parts = token.split(",");
		if (parts.length != 3) return null;
		try {
			return new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
		} catch (NumberFormatException e) {
			return null;
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
	private static void simplifyWithBikeInfra(Network network, boolean storeOriginalGeometry) {

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

			for (String key : SIMPLIFY_MATCH_KEYS) {
				Object v = a.getAttributes().getAttribute(key);
				if (v != null) newLink.getAttributes().putAttribute(key, v);
			}

			// allowed_speed ist bei gleichem freespeed identisch, wird aber bisher verworfen
			Object speed = a.getAttributes().getAttribute(NetworkUtils.ALLOWED_SPEED);
			if (speed != null) newLink.getAttributes().putAttribute(NetworkUtils.ALLOWED_SPEED, speed);

			String merged = mergeOrigIds(a.getAttributes().getAttribute("origid"),
				b.getAttributes().getAttribute("origid"));
			if (merged != null) newLink.getAttributes().putAttribute("origid", merged);

			if (storeOriginalGeometry) {
				mergeOrigGeom(a, b, newLink);
			}
		});

		simplifier.run(network);
	}

	/**
	 * The OSM reader stores {@code origid} as {@link Long}, but
	 * {@link NetworkUtils#getOrigId} -- called by {@link NetworkSimplifier}
	 * whenever it merges two links -- casts the attribute to {@link String} and
	 * would throw a {@link ClassCastException} on the first merge. Convert once
	 * up front; this also gives the attribute a single consistent type in the
	 * output (previously unmerged links carried Long, merged links String).
	 */
	private static void normalizeOrigIdType(Network network) {
		for (Link link : network.getLinks().values()) {
			Object origid = link.getAttributes().getAttribute("origid");
			if (origid != null && !(origid instanceof String)) {
				link.getAttributes().putAttribute("origid", origid.toString());
			}
		}
	}

	/**
	 * Concatenates the stored geometries of both merged links. The node where they
	 * used to meet disappears from the network here, so it has to become a support
	 * point -- otherwise the merged link cuts the corner at exactly that spot.
	 * Links without stored geometry contribute nothing but the shared node, which
	 * is correct: they were straight.
	 *
	 * <p>Only the x/y components of the shared node are written. The end nodes
	 * carry a Z from the DEM, but {@link NetworkUtils#getOriginalGeometry} expects
	 * exactly three fields per point and throws on a fourth.
	 */
	private static void mergeOrigGeom(Link in, Link out, Link merged) {
		Node shared = in.getToNode();
		StringBuilder sb = new StringBuilder();
		appendGeom(sb, in);
		Coord c = shared.getCoord();
		sb.append(shared.getId()).append(',').append(c.getX()).append(',').append(c.getY()).append(' ');
		appendGeom(sb, out);
		merged.getAttributes().putAttribute(NetworkUtils.ORIG_GEOM, sb.toString());
	}

	private static void appendGeom(StringBuilder sb, Link link) {
		Object v = link.getAttributes().getAttribute(NetworkUtils.ORIG_GEOM);
		if (v == null) return;
		String s = v.toString().trim();
		if (!s.isEmpty()) sb.append(s).append(' ');
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
