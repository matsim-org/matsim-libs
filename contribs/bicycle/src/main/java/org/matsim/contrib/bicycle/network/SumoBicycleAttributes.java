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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
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
import org.matsim.contrib.sumo.SumoNetworkHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.matsim.contrib.bicycle.BicycleUtils.BICYCLE_AREA;
import static org.matsim.contrib.bicycle.BicycleUtils.BICYCLE_INFRA;
import static org.matsim.contrib.bicycle.BicycleUtils.BICYCLE_INFRA_MIXED;
import static org.matsim.contrib.bicycle.BicycleUtils.OSM_PREFIX;
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
 * <p><b>What it does not do.</b> It never splits links, and it never merges across
 * differing infrastructure. Whether consecutive links of differing infrastructure end
 * up as one link is decided earlier, by netconvert's {@code geometry.remove}. Links
 * whose constituent ways disagree are recorded as
 * {@link BicycleInfraCategory#NEEDS_CLARIFICATION}, marked with
 * {@link BicycleUtils#BICYCLE_INFRA_MIXED} and counted — never guessed at.
 *
 * <p><b>{@code --simplify}.</b> Optionally merges consecutive links that agree on the
 * bicycle attributes (and on modes, lanes, freespeed and capacity), with the same
 * rules the Supersonic pipeline uses. The merge runs after the cleanups and before
 * the elevation metrics, so gradients are computed over the merged polylines, and the
 * companion files describe the merged links: the geometry CSV gets the concatenated
 * SUMO shapes, the feature CSV a row inherited from the downstream constituent (whose
 * to-junction the merged link now ends at), with the length column corrected.
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

	/** OSM highway values that are paved by default, used by the surface fallback. */
	private static final List<String> ASPHALT_BY_DEFAULT = List.of("primary", "secondary");

	/** How many network nodes to probe the DEM with, and how many of them must have data. */
	private static final int DEM_PROBE_SIZE = 200;
	private static final double MIN_DEM_COVERAGE = 0.5;

	/** Companion file naming, as used by {@code network-from-sumo}. */
	private static final String GEOMETRY_SUFFIX = "-linkGeometries.csv";
	private static final String FEATURE_SUFFIX = "-ft.csv";

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

	@Option(names = "--simplify", defaultValue = "false",
		description = "Merge consecutive links that agree on the bicycle attributes (and on "
			+ "modes, lanes, freespeed and capacity), with the Supersonic pipeline's rules. "
			+ "Runs after the cleanups and before the elevation metrics, so gradients cover "
			+ "the merged polylines and the companion files describe the merged links.")
	private boolean simplify;

	@Mixin
	private final BicycleBuildOptions buildOptions = new BicycleBuildOptions();

	@Mixin
	private final DemOptions demOptions = new DemOptions();

	public static void main(String[] args) {
		new SumoBicycleAttributes().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		demOptions.validate();

		Network network = NetworkUtils.readNetwork(networkFile.toString());
		log.info("Read network: {} nodes, {} links", network.getNodes().size(), network.getLinks().size());

		SumoNetworkHandler sumo = SumoNetworkHandler.read(sumoNetworkFile.toFile());
		log.info("Read SUMO network: {} edges", sumo.getEdges().size());

		BicycleLinkPolicy.AreaMarker areaMarker = buildOptions.areaMarkerOrNull();

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
		if (demOptions.isSet()) {
			if (networkCRS == null) {
				throw new IllegalArgumentException(
					"The network carries no coordinateReferenceSystem attribute, so the DEM cannot be "
						+ "reprojected onto it. Re-run network-from-sumo with --target-crs, or drop --dem.");
			}
			elevationParser = demOptions.createParser(networkCRS);
			// Fail now rather than after half an hour of work: a DEM in the wrong CRS
			// reads as no-data everywhere, which downstream is indistinguishable from
			// perfectly flat terrain.
			elevationParser.requireCoverageOf(probeCoords(network), MIN_DEM_COVERAGE);
		} else {
			log.info("No --dem given: no elevation metrics will be attached.");
		}

		Params params = new Params(buildOptions.country(), buildOptions.mode(), areaMarker,
			buildOptions.eleSampleStep(), buildOptions.eleNoiseTolerance(), simplify);
		MergeCarry carry = new MergeCarry();
		Stats stats = process(network, sumo, wayTags,
			elevationParser != null ? elevationParser::getElevation : null, params, carry);

		log.info(stats.format());
		BicycleNetworkOps.logInfraDistribution(network, "in final network");

		new NetworkWriter(network).write(output.toString());
		writeGeometries(network, sumo, carry.shapes, companion(output, GEOMETRY_SUFFIX));
		filterFeatures(network, carry.featureSource, companion(networkFile, FEATURE_SUFFIX), companion(output, FEATURE_SUFFIX));
		return 0;
	}

	// ------------------------------------------------------------------------
	// Companion files
	// ------------------------------------------------------------------------

	/**
	 * The path {@code network-from-sumo} would use for a companion file of that network:
	 * next to it, with {@code .xml} swapped for the suffix. A compression extension after
	 * the {@code .xml} stays put, so the companions inherit the network's own compression:
	 * {@code net.xml.gz} gets {@code net-ft.csv.gz}, a plain {@code net.xml} plain CSVs.
	 * {@link IOUtils} picks the codec from the extension when reading and writing them.
	 */
	static Path companion(Path network, String suffix) {
		String name = network.getFileName().toString().replace(".xml", suffix);
		Path dir = network.getParent();
		return dir != null ? dir.resolve(name) : Path.of(name);
	}

	/**
	 * Writes one polyline per link, in the format {@code network-from-sumo} uses, so the
	 * annotated network arrives with a companion file under the matching name.
	 *
	 * <p>The file {@code network-from-sumo} wrote does cover these links — this command
	 * only ever removes links, never renames or adds them — but it is named after the
	 * network before annotation and still carries the rows of everything since dropped.
	 *
	 * <p>Generated from the SUMO edges rather than copied, using the same course the
	 * elevation sampling used, so the two cannot disagree about a link's shape.
	 */
	static void writeGeometries(Network network, SumoNetworkHandler sumo, Path path) {
		writeGeometries(network, sumo, Map.of(), path);
	}

	/**
	 * Like {@link #writeGeometries(Network, SumoNetworkHandler, Path)}, but consults the
	 * {@code --simplify} shape carry first — merged link ids resolve to their concatenated
	 * polylines instead of falling back to the chord.
	 */
	static void writeGeometries(Network network, SumoNetworkHandler sumo,
								Map<Id<Link>, List<Coord>> mergedShapes, Path path) {

		try (CSVPrinter out = new CSVPrinter(IOUtils.getBufferedWriter(path.toString()),
			CSVFormat.DEFAULT.withHeader("LinkId", "Geometry"))) {

			for (Link link : network.getLinks().values()) {
				List<Coord> shape = resolveShape(link, sumo, mergedShapes);
				out.printRecord(link.getId().toString(), shape.stream()
					.map(c -> String.format(Locale.US, "(%f,%f)", c.getX(), c.getY()))
					.collect(Collectors.joining(",")));
			}
			log.info("Wrote geometries of {} link(s) to {}", network.getLinks().size(), path);

		} catch (IOException e) {
			throw new UncheckedIOException("Could not write link geometries to " + path, e);
		}
	}

	/**
	 * Copies the link features next to the annotated network, dropping the rows of links
	 * that did not survive.
	 *
	 * <p>Not regenerated: the features describe the SUMO network and are
	 * {@code SumoNetworkFeatureExtractor}'s business. Filtering keeps them in step with
	 * the network they now sit beside, which is what {@code apply-network-params} needs.
	 *
	 * <p>Links {@code --simplify} merged get a row inherited from their downstream
	 * constituent: the junction columns describe the exit junction at the to-node, and
	 * after the merge that junction belongs to the downstream link. Only the id and the
	 * length column are rewritten; speed and lane counts are identical on both
	 * constituents by the merge predicate.
	 *
	 * <p>Silently skipped when there is nothing to filter — the features are optional,
	 * and a scenario that does not calibrate never asks for them.
	 */
	static void filterFeatures(Network network, Map<Id<Link>, Id<Link>> featureSource,
							   Path in, Path out) {

		if (!Files.exists(in)) {
			log.info("No link features at {}; nothing to carry over.", in);
			return;
		}

		Set<String> surviving = network.getLinks().keySet().stream()
			.map(Object::toString).collect(Collectors.toSet());

		try (CSVParser parser = CSVParser.parse(IOUtils.getBufferedReader(in.toString()),
			CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

			List<String> header = parser.getHeaderNames();
			int lengthColumn = header.indexOf("length");
			int kept = 0;
			int dropped = 0;
			int synthesized = 0;

			// The rows of the merge constituents are needed after the pass over the file,
			// so remember every row that could serve as a source. Cheap: it is bounded by
			// the number of merges.
			Set<String> wantedSources = featureSource.values().stream()
				.map(Object::toString).collect(Collectors.toSet());
			Map<String, CSVRecord> sources = new HashMap<>();

			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(out.toString()),
				CSVFormat.DEFAULT.withHeader(header.toArray(new String[0])))) {

				for (CSVRecord record : parser) {
					if (wantedSources.contains(record.get(0))) {
						sources.put(record.get(0), record);
					}
					if (surviving.contains(record.get(0))) {
						printer.printRecord(record);
						kept++;
					} else {
						dropped++;
					}
				}

				List<Id<Link>> mergedIds = featureSource.keySet().stream()
					.filter(id -> network.getLinks().containsKey(id))
					.sorted(Comparator.comparing(Id::toString))
					.toList();
				for (Id<Link> id : mergedIds) {
					CSVRecord source = sources.get(featureSource.get(id).toString());
					if (source == null) continue;
					List<String> row = new ArrayList<>(source.toList());
					row.set(0, id.toString());
					if (lengthColumn >= 0) {
						row.set(lengthColumn, String.format(Locale.US, "%.2f",
							network.getLinks().get(id).getLength()));
					}
					printer.printRecord(row);
					synthesized++;
				}
			}
			log.info("Carried over {} of {} link feature row(s) to {} ({} dropped with their links, "
					+ "{} synthesized for merged links).",
				kept, kept + dropped, out, dropped, synthesized);

		} catch (IOException e) {
			throw new UncheckedIOException("Could not filter link features from " + in, e);
		}
	}

	/**
	 * What the {@code --simplify} merge hands the companion writers: the concatenated
	 * SUMO shape per merged link (consumed by the geometry CSV and the elevation
	 * sampling), and the id of the downstream constituent whose feature row the merged
	 * link inherits. Both empty when {@code --simplify} is off.
	 */
	static final class MergeCarry {
		final Map<Id<Link>, List<Coord>> shapes = new HashMap<>();
		final Map<Id<Link>, Id<Link>> featureSource = new HashMap<>();
	}

	/** @see #process(Network, SumoNetworkHandler, OsmWayTags, LinkElevationProfile.ElevationSource, Params, MergeCarry) */
	static Stats process(Network network, SumoNetworkHandler sumo, OsmWayTags wayTags,
						 LinkElevationProfile.ElevationSource elevation, Params params) {
		return process(network, sumo, wayTags, elevation, params, new MergeCarry());
	}

	/**
	 * Everything between reading the inputs and writing the network. Performs no file
	 * access and reads no CLI state, so it can be driven from a test with a synthetic
	 * elevation source. The network is mutated in place.
	 *
	 * @param elevation elevation source for the metrics, or {@code null} to skip them
	 * @param carry     filled by the {@code --simplify} merge, for the companion writers
	 */
	static Stats process(Network network, SumoNetworkHandler sumo, OsmWayTags wayTags,
						 LinkElevationProfile.ElevationSource elevation, Params params, MergeCarry carry) {

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
			// Which side a link fell on is recorded either way, so downstream can filter on
			// the area itself instead of inferring it from a missing category.
			if (params.areaMarker() != null) {
				boolean inside = ways.stream().anyMatch(t -> params.areaMarker().matches(t));
				link.getAttributes().putAttribute(BICYCLE_AREA, inside);
				if (!inside) {
					stats.outsideArea++;
					continue;
				}
			}

			OsmWayDirection direction = directionOf(link);
			link.getAttributes().putAttribute(BICYCLE_INFRA,
				classify(classifier, ways, direction, link, stats).name());

			stampOsmTags(link, ways, stats);
			applySurfaceFallback(link, ways);
			applyRestPolicy(link, ways, stats);

			stats.classified++;
		}

		int serviceLinksRemoved = new ServiceLinkCleaner().run(network);
		log.info("Service-link cleanup removed {} link(s); {} remain.",
			serviceLinksRemoved, network.getLinks().size());

		// Picks up the links the rest policy emptied as well as anything the service
		// cleanup disconnected. Before the elevation (and the simplify) on purpose:
		// what dies here should neither block a merge nor burn elevation samples.
		NetworkUtils.cleanNetwork(network, Set.of(TransportMode.car, TransportMode.bike));
		log.info("After cleanNetwork: {} nodes, {} links", network.getNodes().size(), network.getLinks().size());

		if (params.simplify()) {
			stats.mergedBySimplify = simplifyAndTrack(network, sumo, carry);
		}

		if (elevation != null) {
			attachElevation(network, sumo, carry.shapes, elevation, params, stats);
		}

		warnAboutMissingLaneRestrictions(network, stats);

		stats.modesRenamed = BicycleNetworkOps.renameMode(network, TransportMode.bike, params.mode());

		return stats;
	}

	/**
	 * The {@code --simplify} merge: {@link BicycleNetworkPipeline#simplifyUntilStable}
	 * with a transfer consumer that keeps the companion data usable. For every merge it
	 * concatenates the constituents' polylines (the vanished middle node stays a support
	 * point — SUMO shapes contain their endpoints, so the tail is appended without its
	 * first point), records the downstream constituent as the feature-row source
	 * (resolved transitively, so cascaded merges still point at a real SUMO edge), and
	 * carries {@code name} and {@code restricted_lanes} over when both sides agree —
	 * they are not merge criteria, but dropping equal values would lose information
	 * for no reason.
	 */
	static int simplifyAndTrack(Network network, SumoNetworkHandler sumo, MergeCarry carry) {

		BiConsumer<Tuple<Link, Link>, Link> carryOver = (inOut, merged) -> {
			Link in = inOut.getFirst();
			Link out = inOut.getSecond();

			List<Coord> shape = new ArrayList<>(resolveShape(in, sumo, carry.shapes));
			List<Coord> tail = resolveShape(out, sumo, carry.shapes);
			shape.addAll(tail.subList(1, tail.size()));
			carry.shapes.put(merged.getId(), shape);

			carry.featureSource.put(merged.getId(),
				carry.featureSource.getOrDefault(out.getId(), out.getId()));

			copyIfEqual(in, out, merged, "name");
			copyIfEqual(in, out, merged, "restricted_lanes");
		};

		int before = network.getLinks().size();
		int removed = BicycleNetworkPipeline.simplifyUntilStable(network, false, carryOver);
		// NetworkSimplifier removes the merged links but leaves the merged-through
		// nodes behind as orphans - visible as stray points in any GIS export, and
		// the node-Z stamping would waste samples on them.
		NetworkUtils.removeNodesWithoutLinks(network);
		log.info("--simplify merged away {} link(s): {} -> {} ({} nodes remain)",
			removed, before, network.getLinks().size(), network.getNodes().size());

		// Only merged links that survived to the end matter for the companions; a link
		// that was merged again re-registered under its new id.
		carry.shapes.keySet().retainAll(network.getLinks().keySet());
		carry.featureSource.keySet().retainAll(network.getLinks().keySet());
		return removed;
	}

	private static void copyIfEqual(Link a, Link b, Link merged, String key) {
		Object v = a.getAttributes().getAttribute(key);
		if (v != null && Objects.equals(v, b.getAttributes().getAttribute(key))) {
			merged.getAttributes().putAttribute(key, v);
		}
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
	static OsmWayDirection directionOf(Link link) {
		return link.getId().toString().startsWith("-") ? OsmWayDirection.REVERSE : OsmWayDirection.FORWARD;
	}

	/**
	 * The tag maps of the OSM ways behind an edge. More than one when netconvert merged
	 * ways; empty when the edge has no {@code origId} (a merged-in foreign network) or
	 * none of its ways carried a tag we keep.
	 */
	static List<Map<String, String>> wayTagsOf(SumoNetworkHandler.Edge edge, OsmWayTags wayTags) {

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
	 * <p>A disagreement also sets {@link BicycleUtils#BICYCLE_INFRA_MIXED},
	 * because the classifier produces {@code NEEDS_CLARIFICATION} on its own too — a bare
	 * {@code highway=cycleway} does — and the two causes need telling apart: one is a
	 * property of the OSM data, the other of how the network was built.
	 */
	private static BicycleInfraCategory classify(BicycleInfraClassifier classifier,
												 List<Map<String, String>> ways,
												 OsmWayDirection direction, Link link, Stats stats) {

		BicycleInfraCategory first = classifier.classify(ways.get(0), direction);
		for (int i = 1; i < ways.size(); i++) {
			if (classifier.classify(ways.get(i), direction) != first) {
				stats.mixedMultiWay++;
				link.getAttributes().putAttribute(BICYCLE_INFRA_MIXED, true);
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
	 * The access rules, ported from {@link BicycleLinkPolicy}.
	 *
	 * <p>netconvert covers some of these itself, but not reliably. On a synthetic test it
	 * dropped {@code access=no} on a residential road and removed a footway without bike
	 * permission — yet on a real Leipzig extract 68 links with {@code access=no} and no
	 * bicycle override survived (mostly {@code highway=service}), as did 47 footways and
	 * 10 links tagged {@code bicycle=no} that kept their bike mode. Applying the rules
	 * unconditionally is idempotent where netconvert already did the work, and closes the
	 * gap where it did not — which is why the counters below report what this command
	 * actually removed rather than second-guessing netconvert.
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

		if (ways.stream().anyMatch(SumoBicycleAttributes::isFootwayWithoutBikePermission)) {
			drop(link);
			stats.droppedFootwayWithoutBike++;
			return;
		}

		// bicycle=no forbids cycling but leaves the road open to everything else, so a
		// highway=primary survives as a car link rather than disappearing.
		if (ways.stream().anyMatch(t -> NO.equals(t.get(BICYCLE)))
			&& link.getAllowedModes().contains(TransportMode.bike)) {
			Set<String> modes = new HashSet<>(link.getAllowedModes());
			modes.remove(TransportMode.bike);
			link.setAllowedModes(modes);
			stats.bikeModeRemoved++;
		}
	}

	/**
	 * A restricted general {@code access} without a bicycle-specific permission
	 * overriding it. Same predicate as {@link BicycleLinkPolicy}.
	 */
	private static boolean isAccessRestricted(Map<String, String> tags) {
		String access = tags.get(ACCESS);
		boolean restricted = NO.equals(access) || PRIVATE.equals(access) || CUSTOMER.equals(access);
		return restricted && !bicycleExplicitlyAllowed(tags);
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

	static void attachElevation(Network network, SumoNetworkHandler sumo,
								Map<Id<Link>, List<Coord>> mergedShapes,
								LinkElevationProfile.ElevationSource elevation,
								Params params, Stats stats) {

		for (Node node : network.getNodes().values()) {
			if (!BicycleNetworkOps.addNodeElevation(node, elevation)) stats.nodesWithoutElevation++;
		}

		for (Link link : network.getLinks().values()) {
			// Elevation is part of the full bicycle treatment, so it follows the
			// classification: no category means the link was never looked at.
			if (link.getAttributes().getAttribute(BICYCLE_INFRA) == null) continue;

			List<Coord> shape = resolveShape(link, sumo, mergedShapes);
			if (shape.size() > 2) stats.linksWithTrueShape++;
			else stats.linksSampledAlongChord++;

			boolean written = BicycleNetworkOps.attachElevationMetrics(link,
				LinkElevationProfile.compute(link, shape, params.eleSampleStep(), params.eleNoiseTolerance(), elevation));
			if (written) stats.withElevation++;
			else stats.linksWithoutElevationData++;
		}
	}

	/**
	 * The link's true course: the {@code --simplify} carry when the link is a merge
	 * product, otherwise the SUMO edge polyline. Never the silent chord for a merged
	 * id — that is exactly the fallback this indirection exists to avoid.
	 */
	static List<Coord> resolveShape(Link link, SumoNetworkHandler sumo, Map<Id<Link>, List<Coord>> mergedShapes) {
		List<Coord> carried = mergedShapes.get(link.getId());
		if (carried != null) return carried;
		return shapeOf(link, sumo.getEdges().get(link.getId().toString()), sumo);
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

	/**
	 * A spread-out sample of node coordinates to probe the DEM with. Every n-th node
	 * rather than the first n, so the probe covers the whole area instead of whichever
	 * corner the iteration happens to start in.
	 */
	private static List<Coord> probeCoords(Network network) {
		List<? extends Node> nodes = List.copyOf(network.getNodes().values());
		if (nodes.isEmpty()) return List.of();

		int stride = Math.max(1, nodes.size() / DEM_PROBE_SIZE);
		List<Coord> probe = new ArrayList<>();
		for (int i = 0; i < nodes.size(); i += stride) {
			probe.add(nodes.get(i).getCoord());
		}
		return probe;
	}

	// ------------------------------------------------------------------------

	/**
	 * The non-I/O parameters, decoupled from the picocli fields so a test can build them.
	 */
	record Params(String country, String mode, BicycleLinkPolicy.AreaMarker areaMarker,
				  double eleSampleStep, double eleNoiseTolerance, boolean simplify) {

		static Params defaults() {
			return new Params("de", TransportMode.bike, null,
				Double.parseDouble(BicycleBuildOptions.DEFAULT_ELE_SAMPLE_STEP),
				Double.parseDouble(BicycleBuildOptions.DEFAULT_ELE_NOISE_TOLERANCE), false);
		}

		Params withSimplify() {
			return new Params(country, mode, areaMarker, eleSampleStep, eleNoiseTolerance, true);
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
		int droppedFootwayWithoutBike;
		int bikeModeRemoved;

		int withElevation;
		int linksWithTrueShape;
		int linksSampledAlongChord;
		/** Links the DEM had no data for; they keep no elevation attributes at all. */
		int linksWithoutElevationData;
		int nodesWithoutElevation;

		int modesRenamed;
		boolean laneRestrictionsMissing;
		/** Links removed by the {@code --simplify} merge; 0 when the option is off. */
		int mergedBySimplify;

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
				  dropped: access=no/private/customer  %d
				  dropped: footway without bike        %d
				  bike mode removed (bicycle=no)       %d
				  links merged away by --simplify      %d
				  links with elevation metrics         %d
				    sampled along the true polyline    %d
				    sampled along the chord            %d
				  links the DEM had no data for        %d
				  nodes left without a Z               %d
				  links renamed to the target mode     %d"""
				.formatted(classified, linksWithoutEdge, linksWithoutWayTags, outsideArea,
					agreeingMultiWay, mixedMultiWay, tagsDroppedAsAmbiguous,
					droppedParkingAisle, droppedRestrictedAccess, droppedFootwayWithoutBike,
					bikeModeRemoved, mergedBySimplify,
					withElevation, linksWithTrueShape, linksSampledAlongChord,
					linksWithoutElevationData, nodesWithoutElevation, modesRenamed);
		}
	}
}
