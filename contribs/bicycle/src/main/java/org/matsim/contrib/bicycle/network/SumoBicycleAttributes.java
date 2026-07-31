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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.sumo.SumoNetworkHandler;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.matsim.contrib.bicycle.network.BicycleNetworkOps.LINK_ATTR_BICYCLE_INFRA;
import static org.matsim.contrib.bicycle.network.BicycleNetworkOps.OSM_PREFIX;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.ACCESS;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.BICYCLE;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.CUSTOMER;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.DESIGNATED;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.HIGHWAY;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.HW_FOOTWAY;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.HW_PEDESTRIAN;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.NO;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.PRIVATE;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.SERVICE;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.SURFACE;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.SV_PARKING_AISLE;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.YES;

/**
 * Attaches cycling infrastructure categories, OSM tags and elevation metrics to a
 * MATSim network that came out of SUMO.
 *
 * <p>Runs after {@code network-from-sumo} and {@code clean-network}, on the finished
 * network. It needs three inputs that all describe the same thing at different stages:
 * the MATSim network, the {@code sumo.net.xml} it was converted from, and the
 * {@code network.osm} netconvert consumed.
 *
 * <p><b>Why all three.</b> A MATSim link carries the SUMO edge id verbatim, and the
 * SUMO edge knows the OSM way id(s) it was built from. That chain is what lets the
 * classifier see the original way tags. Reading the tags netconvert exported as edge
 * params instead would be cheaper but wrong on merged edges: netconvert ignores OSM
 * tags when deciding whether to join two edges, so a way tagged
 * {@code cycleway=lane} merged with an untagged one yields a single edge tagged
 * {@code cycleway=lane} over twice the length, with nothing to indicate that half of
 * it is a plain road. The {@code sumo.net.xml} also supplies the edge polyline, which
 * is what the elevation metrics are sampled along.
 *
 * <p><b>What it does not do.</b> It never merges or splits links. Whether consecutive
 * links of differing infrastructure end up as one link is decided earlier, by
 * netconvert's {@code geometry.remove}. Links whose constituent ways disagree are
 * recorded as {@link BicycleInfraCategory#NEEDS_CLARIFICATION}, marked with
 * {@link BicycleNetworkOps#LINK_ATTR_BICYCLE_INFRA_MIXED} and counted — never guessed at.
 *
 * @author smetzler
 */
@Command(
	name = "bicycle-attributes",
	description = "Attaches bicycle infrastructure categories and elevation metrics to a SUMO-converted network.",
	showDefaultValues = true,
	mixinStandardHelpOptions = true
)
public class SumoBicycleAttributes implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(SumoBicycleAttributes.class);

	private static final String DEFAULT_ELE_SAMPLE_STEP = "20.0";
	private static final String DEFAULT_ELE_NOISE_TOLERANCE = "3.0";

	/** OSM highway values that are paved by default, used by the surface fallback. */
	private static final List<String> ASPHALT_BY_DEFAULT = List.of("primary", "secondary");

	// ---- CLI options -----------------------------------------------------------

	@Option(names = "--network", required = true,
		description = "MATSim network produced by network-from-sumo (and usually clean-network)")
	private Path networkFile;

	@Option(names = "--sumo-network", required = true,
		description = "The sumo.net.xml the network was converted from. Supplies the OSM way ids "
			+ "behind each link and the edge polylines used for elevation sampling.")
	private Path sumoNetworkFile;

	@Option(names = "--osm", required = true,
		description = "The .osm file netconvert consumed. Source of the way tags; using the very "
			+ "same file guarantees the way ids match.")
	private Path osmFile;

	@Option(names = "--output", required = true, description = "Path to the output network")
	private Path output;

	@Option(names = "--dem",
		description = "DEM GeoTIFF. Optional: without it no elevation metrics are attached. "
			+ "Requires --dem-crs when given.")
	private Path dem;

	@Option(names = "--dem-crs", description = "CRS of the DEM, e.g. EPSG:32632. Required only with --dem.")
	private String demCRS;

	@Option(names = "--country", defaultValue = "de",
		description = "Country profile for traffic-sign interpretation: de, at, or generic.")
	private String country;

	@Option(names = "--mode", defaultValue = TransportMode.bike,
		description = "Network mode name for cyclable links. Renamed from 'bike' at the very end.")
	private String mode;

	@Option(names = "--bike-area-marker",
		description = "OSM tag selecting the ways that get the full bicycle treatment, as 'key' or "
			+ "'key=value'. Ways without it keep their modes but get no bicycle attributes, no "
			+ "classification and no elevation. Omit to treat every way as cyclable.")
	private String bikeAreaMarker;

	@Option(names = "--ele-sample-step", defaultValue = DEFAULT_ELE_SAMPLE_STEP,
		description = "Distance between elevation samples along a link, in m")
	private double eleSampleStepM;

	@Option(names = "--ele-noise-tolerance", defaultValue = DEFAULT_ELE_NOISE_TOLERANCE,
		description = "Douglas-Peucker vertical tolerance for smoothing the profile, in m")
	private double eleNoiseToleranceM;

	public static void main(String[] args) {
		new SumoBicycleAttributes().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (dem != null && demCRS == null) {
			throw new IllegalArgumentException("--dem-crs is required when --dem is given.");
		}

		Network network = NetworkUtils.readNetwork(networkFile.toString());
		log.info("Read network: {} nodes, {} links", network.getNodes().size(), network.getLinks().size());

		SumoNetworkHandler sumo = SumoNetworkHandler.read(sumoNetworkFile.toFile());
		log.info("Read SUMO network: {} edges", sumo.getEdges().size());

		BicycleLinkPolicy.AreaMarker areaMarker =
			bikeAreaMarker != null ? BicycleLinkPolicy.AreaMarker.parse(bikeAreaMarker) : null;

		// The marker key is not part of the classification keys, so it has to be added
		// explicitly or OsmWayTags would drop it and every way would look unmarked.
		Set<String> keys = new HashSet<>(BicycleOsmTags.classificationKeys());
		if (areaMarker != null) {
			keys.add(areaMarker.key());
			log.info("Bicycle-area marker '{}': only matching ways get the full bicycle treatment.", areaMarker);
		}

		OsmWayTags wayTags = OsmWayTags.read(osmFile, keys);

		String networkCRS = org.matsim.core.scenario.ProjectionUtils.getCRS(network);
		ElevationDataParser elevationParser = null;
		if (dem != null) {
			if (networkCRS == null) {
				throw new IllegalArgumentException(
					"The network carries no coordinateReferenceSystem attribute, so the DEM cannot be "
						+ "reprojected onto it. Re-run network-from-sumo with --target-crs, or drop --dem.");
			}
			elevationParser = new ElevationDataParser(dem.toString(), networkCRS, demCRS);
		} else {
			log.info("No --dem given: no elevation metrics will be attached.");
		}

		Params params = new Params(country, mode, areaMarker, eleSampleStepM, eleNoiseToleranceM);
		Stats stats = process(network, sumo, wayTags,
			elevationParser != null ? elevationParser::getElevation : null, params);

		log.info(stats.format());
		BicycleNetworkOps.logInfraDistribution(network, "in final network");

		new NetworkWriter(network).write(output.toString());
		return 0;
	}

	/**
	 * Everything between reading the inputs and writing the network. Performs no file
	 * access and reads no CLI state, so it can be driven from a test with a synthetic
	 * elevation source. The network is mutated in place.
	 *
	 * @param elevation elevation source for the metrics, or {@code null} to skip them
	 */
	static Stats process(Network network, SumoNetworkHandler sumo, OsmWayTags wayTags,
						 LinkElevationProfile.ElevationSource elevation, Params params) {

		requireBikeLinks(network);

		BicycleInfraClassifier classifier =
			new BicycleInfraClassifier(BicycleCountryProfiles.forCode(params.country()));
		Stats stats = new Stats();

		for (Link link : network.getLinks().values()) {

			SumoNetworkHandler.Edge edge = sumo.getEdges().get(link.getId().toString());
			if (edge == null) {
				stats.linksWithoutEdge++;
				continue;
			}

			List<Map<String, String>> ways = wayTagsOf(edge, wayTags);
			if (ways.isEmpty()) {
				stats.linksWithoutWayTags++;
				continue;
			}

			// Outside the marked area a link keeps its modes -- bikes may still ride it --
			// but gets no bicycle detail, and no elevation later, which keys off the category.
			if (params.areaMarker() != null && ways.stream().noneMatch(t -> params.areaMarker().matches(t))) {
				stats.outsideArea++;
				continue;
			}

			Direction direction = directionOf(link);
			link.getAttributes().putAttribute(LINK_ATTR_BICYCLE_INFRA,
				classify(classifier, ways, direction, link, stats).name());

			stampOsmTags(link, ways, stats);
			applySurfaceFallback(link, ways);
			applyRestPolicy(link, ways, stats);

			stats.classified++;
		}

		int serviceLinksRemoved = new ServiceLinkCleaner().run(network);
		log.info("Service-link cleanup removed {} link(s); {} remain.",
			serviceLinksRemoved, network.getLinks().size());

		if (elevation != null) {
			attachElevation(network, sumo, elevation, params, stats);
		}

		// Picks up the links the rest policy emptied as well as anything the service
		// cleanup disconnected.
		NetworkUtils.cleanNetwork(network, Set.of(TransportMode.car, TransportMode.bike));
		log.info("After cleanNetwork: {} nodes, {} links", network.getNodes().size(), network.getLinks().size());

		warnAboutMissingLaneRestrictions(network, stats);

		stats.modesRenamed = renameMode(network, TransportMode.bike, params.mode());

		return stats;
	}

	// ------------------------------------------------------------------------
	// Link to OSM ways
	// ------------------------------------------------------------------------

	/**
	 * Travel direction of a link relative to its OSM way. SUMO names the edge of a way
	 * {@code 5678#0} in the way's own direction and {@code -5678#0} against it, and the
	 * converter reuses the edge id as the link id — so the leading minus is the whole
	 * rule. It holds for the contraflow edges {@code osm.bike-access} adds, too.
	 */
	static Direction directionOf(Link link) {
		return link.getId().toString().startsWith("-") ? Direction.REVERSE : Direction.FORWARD;
	}

	/**
	 * The tag maps of the OSM ways behind an edge. More than one when netconvert merged
	 * ways; empty when the edge has no {@code origId} (a merged-in foreign network) or
	 * none of its ways carried a tag we keep.
	 */
	private static List<Map<String, String>> wayTagsOf(SumoNetworkHandler.Edge edge, OsmWayTags wayTags) {

		String origId = edge.getOrigId();
		if (origId == null || origId.isBlank()) return List.of();

		List<Map<String, String>> result = new ArrayList<>(2);
		for (String token : origId.split(" ")) {
			if (token.isBlank()) continue;
			try {
				Map<String, String> tags = wayTags.get(Long.parseLong(token));
				if (!tags.isEmpty()) result.add(tags);
			} catch (NumberFormatException e) {
				// origId is a plain OSM way id in every network we have seen; anything
				// else is not something we can look up, so skip it rather than guess.
			}
		}
		return result;
	}

	// ------------------------------------------------------------------------
	// Classification
	// ------------------------------------------------------------------------

	/**
	 * One category per link. With a single way this is just the classifier. With several
	 * — netconvert merged them — each way is classified on its own and the result is only
	 * used when they agree; otherwise the link is honestly marked as unclear rather than
	 * given the category of whichever way happened to win.
	 *
	 * <p>A disagreement also sets {@link BicycleNetworkOps#LINK_ATTR_BICYCLE_INFRA_MIXED},
	 * because the classifier produces {@code NEEDS_CLARIFICATION} on its own too — a bare
	 * {@code highway=cycleway} does — and the two causes need telling apart: one is a
	 * property of the OSM data, the other of how the network was built.
	 */
	private static BicycleInfraCategory classify(BicycleInfraClassifier classifier,
												 List<Map<String, String>> ways,
												 Direction direction, Link link, Stats stats) {

		BicycleInfraCategory first = classifier.classify(ways.get(0), direction);
		for (int i = 1; i < ways.size(); i++) {
			if (classifier.classify(ways.get(i), direction) != first) {
				stats.mixedMultiWay++;
				link.getAttributes().putAttribute(BicycleNetworkOps.LINK_ATTR_BICYCLE_INFRA_MIXED, true);
				return BicycleInfraCategory.NEEDS_CLARIFICATION;
			}
		}
		if (ways.size() > 1) stats.agreeingMultiWay++;
		return first;
	}

	// ------------------------------------------------------------------------
	// Attributes
	// ------------------------------------------------------------------------

	/**
	 * Copies the way tags onto the link under the {@code osm:} prefix. On a merged link
	 * only values all constituent ways agree on are written — a disagreement would be a
	 * property of the merge, not of the road, and silently picking one is exactly the
	 * failure mode this whole route exists to avoid.
	 */
	private static void stampOsmTags(Link link, List<Map<String, String>> ways, Stats stats) {

		if (ways.size() == 1) {
			ways.get(0).forEach((k, v) -> link.getAttributes().putAttribute(OSM_PREFIX + k, v));
			return;
		}

		Set<String> allKeys = new TreeSet<>();
		ways.forEach(t -> allKeys.addAll(t.keySet()));

		for (String key : allKeys) {
			String value = ways.get(0).get(key);
			boolean unanimous = value != null && ways.stream().allMatch(t -> value.equals(t.get(key)));
			if (unanimous) {
				link.getAttributes().putAttribute(OSM_PREFIX + key, value);
			} else {
				stats.tagsDroppedAsAmbiguous++;
			}
		}
	}

	/**
	 * Bigger roads without an explicit {@code surface} tag are asphalt in practice.
	 * {@code OsmBicycleReader} assumes the same, and {@code BicycleUtils.getSurface}
	 * consumes it for scoring — without this the two paths would score differently for
	 * no visible reason.
	 *
	 * <p>Keys off the tags rather than the stamped attribute, so it does not matter
	 * whether this runs before or after {@link #stampOsmTags}.
	 */
	private static void applySurfaceFallback(Link link, List<Map<String, String>> ways) {

		if (ways.stream().anyMatch(t -> t.containsKey(SURFACE))) return;

		String highway = ways.get(0).get(HIGHWAY);
		if (highway != null && ASPHALT_BY_DEFAULT.stream().anyMatch(highway::startsWith)) {
			link.getAttributes().putAttribute(OSM_PREFIX + SURFACE, "asphalt");
		}
	}

	// ------------------------------------------------------------------------
	// Access rules
	// ------------------------------------------------------------------------

	/**
	 * The two access rules netconvert does not cover. Measured against netconvert 1.27.1:
	 * it drops {@code access=no} and honours {@code bicycle=no} and the footway
	 * whitelist, but ignores {@code access=private} and {@code access=customer}
	 * entirely — those roads stay fully routable — and it keeps parking aisles as
	 * bike-accessible.
	 *
	 * <p>The natively covered rules are only counted here, never re-applied: a second
	 * opinion that always agrees is noise, and one that disagrees would be a bug worth
	 * seeing in the numbers.
	 *
	 * <p>"Drop" empties the modes and zeroes the capacity; {@code cleanNetwork} prunes
	 * the link afterwards. On a merged link a single offending way is enough — half a
	 * parking aisle is not a through route.
	 */
	private static void applyRestPolicy(Link link, List<Map<String, String>> ways, Stats stats) {

		if (ways.stream().anyMatch(t -> SV_PARKING_AISLE.equals(t.get(SERVICE)))) {
			drop(link);
			stats.droppedParkingAisle++;
			return;
		}

		if (ways.stream().anyMatch(SumoBicycleAttributes::isAccessRestricted)) {
			drop(link);
			stats.droppedRestrictedAccess++;
			return;
		}

		// counters for what netconvert already handled
		if (ways.stream().anyMatch(t -> NO.equals(t.get(BICYCLE)))
			&& link.getAllowedModes().contains(TransportMode.bike)) {
			stats.bicycleNoStillRideable++;
		}
		if (ways.stream().anyMatch(t -> NO.equals(t.get(ACCESS)))) {
			stats.accessNoSurvived++;
		}
		if (ways.stream().anyMatch(SumoBicycleAttributes::isFootwayWithoutBikePermission)
			&& link.getAllowedModes().contains(TransportMode.bike)) {
			stats.footwayWithoutPermissionRideable++;
		}
	}

	/**
	 * {@code access=private} / {@code =customer} without a bicycle-specific permission.
	 * {@code access=no} is left out because netconvert already removes those links.
	 */
	private static boolean isAccessRestricted(Map<String, String> tags) {
		String access = tags.get(ACCESS);
		return (PRIVATE.equals(access) || CUSTOMER.equals(access)) && !bicycleExplicitlyAllowed(tags);
	}

	private static boolean isFootwayWithoutBikePermission(Map<String, String> tags) {
		String highway = tags.get(HIGHWAY);
		return (HW_FOOTWAY.equals(highway) || HW_PEDESTRIAN.equals(highway)) && !bicycleExplicitlyAllowed(tags);
	}

	/** OSM: the more specific tag wins over the general restriction. */
	private static boolean bicycleExplicitlyAllowed(Map<String, String> tags) {
		String bicycle = tags.get(BICYCLE);
		return YES.equals(bicycle) || DESIGNATED.equals(bicycle);
	}

	private static void drop(Link link) {
		link.setAllowedModes(Set.of());
		link.setCapacity(0);
	}

	// ------------------------------------------------------------------------
	// Elevation
	// ------------------------------------------------------------------------

	private static void attachElevation(Network network, SumoNetworkHandler sumo,
										LinkElevationProfile.ElevationSource elevation,
										Params params, Stats stats) {

		for (Node node : network.getNodes().values()) {
			BicycleNetworkOps.addNodeElevation(node, elevation);
		}

		for (Link link : network.getLinks().values()) {
			// Elevation is part of the full bicycle treatment, so it follows the
			// classification: no category means the link was never looked at.
			if (link.getAttributes().getAttribute(LINK_ATTR_BICYCLE_INFRA) == null) continue;

			SumoNetworkHandler.Edge edge = sumo.getEdges().get(link.getId().toString());
			List<Coord> shape = shapeOf(link, edge, sumo);
			if (shape.size() > 2) stats.linksWithTrueShape++;
			else stats.linksSampledAlongChord++;

			BicycleNetworkOps.attachElevationMetrics(link,
				LinkElevationProfile.compute(link, shape, params.eleSampleStep(), params.eleNoiseTolerance(), elevation));
			stats.withElevation++;
		}
	}

	/**
	 * The link's true course: the SUMO edge polyline, which holds the geometry nodes
	 * {@code geometry.remove} folded in. A straight edge carries no shape, in which case
	 * the chord between the link's nodes is the true course anyway.
	 */
	private static List<Coord> shapeOf(Link link, SumoNetworkHandler.Edge edge, SumoNetworkHandler sumo) {

		if (edge != null && edge.getShape().size() >= 2) {
			List<Coord> shape = new ArrayList<>(edge.getShape().size());
			for (double[] xy : edge.getShape()) shape.add(sumo.createCoord(xy));
			return shape;
		}
		return List.of(link.getFromNode().getCoord(), link.getToNode().getCoord());
	}

	// ------------------------------------------------------------------------
	// Checks and housekeeping
	// ------------------------------------------------------------------------

	/**
	 * Without bike links there is nothing to do, and the most likely reason is a
	 * {@code clean-network} run whose {@code --modes} forgot {@code bike} — which would
	 * otherwise show up much later as an empty distribution table.
	 */
	private static void requireBikeLinks(Network network) {
		boolean any = network.getLinks().values().stream()
			.anyMatch(l -> l.getAllowedModes().contains(TransportMode.bike));
		if (!any) {
			throw new IllegalArgumentException(
				"The network has no link with mode '" + TransportMode.bike + "'. Check that clean-network "
					+ "was run with bike among its --modes, and that netconvert kept bicycle in "
					+ "--keep-edges.by-vclass.");
		}
	}

	/**
	 * Bike lanes that netconvert modelled as lanes inflate the car capacity unless
	 * {@code network-from-sumo} ran with {@code --lane-restrictions REDUCE_CAR_LANES},
	 * which marks what it took out via the {@code restricted_lanes} attribute.
	 */
	private static void warnAboutMissingLaneRestrictions(Network network, Stats stats) {

		boolean anyLaneCycleway = network.getLinks().values().stream()
			.anyMatch(l -> BicycleOsmTags.CW_LANE.equals(l.getAttributes().getAttribute(OSM_PREFIX + BicycleOsmTags.CYCLEWAY)));
		if (!anyLaneCycleway) return;

		boolean anyRestricted = network.getLinks().values().stream()
			.anyMatch(l -> l.getAttributes().getAttribute("restricted_lanes") != null);
		if (!anyRestricted) {
			stats.laneRestrictionsMissing = true;
			log.warn("Links tagged cycleway=lane exist, but no link carries 'restricted_lanes'. "
				+ "network-from-sumo was probably run without --lane-restrictions REDUCE_CAR_LANES, "
				+ "so those bike lanes are still counted as car lanes.");
		}
	}

	/** Renames the bike mode on every link; a no-op when both names are equal. */
	private static int renameMode(Network network, String from, String to) {
		if (from.equals(to)) return 0;

		int renamed = 0;
		for (Link link : network.getLinks().values()) {
			if (!link.getAllowedModes().contains(from)) continue;
			Set<String> modes = new HashSet<>(link.getAllowedModes());
			modes.remove(from);
			modes.add(to);
			link.setAllowedModes(modes);
			renamed++;
		}
		return renamed;
	}

	// ------------------------------------------------------------------------

	/**
	 * The non-I/O parameters, decoupled from the picocli fields so a test can build them.
	 */
	record Params(String country, String mode, BicycleLinkPolicy.AreaMarker areaMarker,
				  double eleSampleStep, double eleNoiseTolerance) {

		static Params defaults() {
			return new Params("de", TransportMode.bike, null,
				Double.parseDouble(DEFAULT_ELE_SAMPLE_STEP),
				Double.parseDouble(DEFAULT_ELE_NOISE_TOLERANCE));
		}
	}

	/**
	 * Per-run counters. Everything that could quietly go wrong gets a number here rather
	 * than a log line nobody reads.
	 */
	static final class Stats {

		int classified;
		int linksWithoutEdge;
		int linksWithoutWayTags;
		int outsideArea;

		/** Merged links whose ways agreed on the category. */
		int agreeingMultiWay;
		/** Merged links whose ways disagreed, recorded as NEEDS_CLARIFICATION. */
		int mixedMultiWay;
		/** Tag values dropped because the merged ways disagreed on them. */
		int tagsDroppedAsAmbiguous;

		int droppedParkingAisle;
		int droppedRestrictedAccess;

		/** Rules netconvert should have handled; a non-zero count means it did not. */
		int bicycleNoStillRideable;
		int accessNoSurvived;
		int footwayWithoutPermissionRideable;

		int withElevation;
		int linksWithTrueShape;
		int linksSampledAlongChord;

		int modesRenamed;
		boolean laneRestrictionsMissing;

		String format() {
			return """
				bicycle-attributes summary:
				  classified links                     %d
				  links without a matching SUMO edge   %d
				  links without OSM way tags           %d
				  links outside the bicycle area       %d
				  merged links, ways agreed            %d
				  merged links, ways disagreed         %d  (recorded as NEEDS_CLARIFICATION)
				  tag values dropped as ambiguous      %d
				  dropped: service=parking_aisle       %d
				  dropped: access=private/customer     %d
				  bicycle=no but still rideable        %d  (expected 0, netconvert handles this)
				  access=no but link survived          %d  (expected 0, netconvert handles this)
				  footway w/o permission, rideable     %d  (expected 0, netconvert handles this)
				  links with elevation metrics         %d
				    sampled along the true polyline    %d
				    sampled along the chord            %d
				  links renamed to the target mode     %d"""
				.formatted(classified, linksWithoutEdge, linksWithoutWayTags, outsideArea,
					agreeingMultiWay, mixedMultiWay, tagsDroppedAsAmbiguous,
					droppedParkingAisle, droppedRestrictedAccess,
					bicycleNoStillRideable, accessNoSurvived, footwayWithoutPermissionRideable,
					withElevation, linksWithTrueShape, linksSampledAlongChord, modesRenamed);
		}
	}
}
