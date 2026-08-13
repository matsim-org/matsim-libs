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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.matsim.contrib.bicycle.BicycleUtils.BICYCLE_INFRA;
import static org.matsim.contrib.bicycle.BicycleUtils.BICYCLE_INFRA_MIXED;
import static org.matsim.contrib.bicycle.BicycleUtils.BIKE_LINK;
import static org.matsim.contrib.bicycle.BicycleUtils.CAR_LINK;

/**
 * Splits a parallel bike-only link off every car link whose cycling infrastructure is
 * tagged on the road itself — a lane on the carriageway, or a track mapped on the road's
 * centerline instead of as its own OSM way.
 *
 * <p><b>Why.</b> On such links MATSim puts cars and bikes into one queue: when the cars
 * back up at the junction, the bike is stuck at the same queue front although its lane or
 * track is free ({@code PassingVehicleQ} only reorders free-flow traffic, it does not
 * dissolve a jam). A separate link with its own queue is the fix. Roads whose separate
 * cycleway was mapped as its own OSM way never had this problem — they are separate links
 * already, which is why the classification plus the mode set is a sufficient trigger: a
 * link carrying car <em>and</em> bike <em>and</em> a dedicated-infrastructure category
 * got that category from tags on the road way itself.
 *
 * <p><b>What a split does.</b> The bike link {@code <id>_bike} shares the car link's
 * nodes and exact length (downstream interaction models compare traversal-time windows
 * across the pair). It takes over {@code bicycle_infra} and the bicycle speed factor,
 * inherits every other attribute as a copy, and gets bike-typical freespeed and capacity.
 * The car link loses the bike mode, so routing has no choice to make. The pair is tied
 * together by the attributes {@link BicycleUtils#CAR_LINK} and
 * {@link BicycleUtils#BIKE_LINK}; the {@code _bike} id suffix is a readable convention,
 * not an interface — consumers build maps from the attributes.
 *
 * <p><b>Structure, not policy.</b> Splitting is deliberately a different question from
 * scoring motorized interaction. Every qualifying link is split the same way, including
 * physically protected lanes and centerline-tagged tracks, and no attribute prescribes
 * whether interaction applies. The scoring side decides that per
 * {@code bicycle_infra} category of the bike link — which is also what makes it possible
 * to <em>count</em> the car encounters an infrastructure avoided without scoring them.
 *
 * <p><b>What is not split.</b> {@code SHARED_MOTOR_VEHICLE_LANE} (a sharrow has no space
 * of its own — the bike genuinely rides in the car queue), bicycle roads (there the
 * carriageway <em>is</em> the bike infrastructure and cars are guests), pedestrian
 * areas, crossings and cycleway links (connector pieces, nothing runs parallel), and
 * {@code NEEDS_CLARIFICATION} (never guess). Mixed traffic stays as it is; making it
 * passable for bikes in a jam is the mobsim's job (seepage), not the network's.
 *
 * <p><b>This is the pipelines' default final step.</b> Both {@code bicycle-attributes}
 * and {@code bicycle-network} split as the last thing they do — after the simplifier
 * (which would merge bike twins and tear the pair references) and after the elevation
 * metrics (which the twins inherit as copies); {@code --no-split-bike-links} turns it
 * off. The standalone command exists for networks built earlier or elsewhere.
 *
 * <p><b>Calibration.</b> Twins deliberately get no row in the feature companion, and a
 * later {@code apply-network-params} run should pass
 * {@code --junction-types traffic_light,right_before_left,priority}: the twins' default
 * feature has an empty junction type, so that filter skips them silently — otherwise
 * they carry their road's {@code type} and a road-type filter would mistake them for
 * car links and cap the bike speed.
 *
 * @author smetzler
 */
@Command(
	name = "bicycle-split-links",
	description = "Splits parallel bike links off car links with centerline-tagged cycling infrastructure.",
	showDefaultValues = true,
	mixinStandardHelpOptions = true
)
public class SplitBikeLinks implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(SplitBikeLinks.class);

	/** The readable id convention for split-off bike links; the attributes are the interface. */
	static final String BIKE_SUFFIX = "_bike";

	// Single source for the CLI defaults here and the values the two pipelines use when
	// splitting as part of the build; annotation defaults must be compile-time Strings.
	static final String DEFAULT_BIKE_FREESPEED = "5.56";
	static final String DEFAULT_BIKE_CAPACITY = "1500";

	/**
	 * Categories whose presence on a car+bike link means the infrastructure is tagged on
	 * the road way itself, and riding it should not mean queueing behind cars.
	 */
	static final EnumSet<BicycleInfraCategory> SPLIT_CATEGORIES = EnumSet.of(
		BicycleInfraCategory.CYCLEWAY_ON_HIGHWAY_PROTECTED,
		BicycleInfraCategory.CYCLEWAY_ON_HIGHWAY_BETWEEN_LANES,
		BicycleInfraCategory.CYCLEWAY_ON_HIGHWAY_ADVISORY,
		BicycleInfraCategory.CYCLEWAY_ON_HIGHWAY_EXCLUSIVE,
		BicycleInfraCategory.CYCLEWAY_ON_HIGHWAY_ADVISORY_OR_EXCLUSIVE,
		// bikes may use the bus lane and pass queueing cars; interaction with buses is
		// the scoring side's question
		BicycleInfraCategory.SHARED_BUS_LANE_BUS_WITH_BIKE,
		BicycleInfraCategory.SHARED_BUS_LANE_BIKE_WITH_BUS,
		// tracks and paths tagged on the centerline; with car on the link they cannot be
		// anything else
		BicycleInfraCategory.CYCLEWAY_ADJOINING,
		BicycleInfraCategory.CYCLEWAY_ISOLATED,
		BicycleInfraCategory.CYCLEWAY_ADJOINING_OR_ISOLATED,
		BicycleInfraCategory.FOOT_AND_CYCLEWAY_SHARED_ADJOINING,
		BicycleInfraCategory.FOOT_AND_CYCLEWAY_SHARED_ISOLATED,
		BicycleInfraCategory.FOOT_AND_CYCLEWAY_SHARED_ADJOINING_OR_ISOLATED,
		BicycleInfraCategory.FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING,
		BicycleInfraCategory.FOOT_AND_CYCLEWAY_SEGREGATED_ISOLATED,
		BicycleInfraCategory.FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING_OR_ISOLATED,
		BicycleInfraCategory.FOOTWAY_BICYCLE_YES_ADJOINING,
		BicycleInfraCategory.FOOTWAY_BICYCLE_YES_ISOLATED,
		BicycleInfraCategory.FOOTWAY_BICYCLE_YES_ADJOINING_OR_ISOLATED
	);

	/** Attributes that describe the car side and must not travel onto the bike link. */
	private static final List<String> CAR_ONLY_ATTRIBUTES = List.of("restricted_lanes", "speed_factor");

	// ---- CLI options -----------------------------------------------------------

	@Option(names = "--network", required = true, description = "Finished bicycle network (either build path)")
	private Path networkFile;

	@Option(names = "--output", required = true, description = "Path to the output network")
	private Path output;

	@Option(names = "--mode", defaultValue = TransportMode.bike,
		description = "Name of the bike mode in this network")
	private String bikeMode;

	@Option(names = "--bike-freespeed", defaultValue = DEFAULT_BIKE_FREESPEED,
		description = "Freespeed of the split-off bike links in m/s. The default is the 20 km/h "
			+ "netconvert convention for cycleways; BicycleLinkSpeedCalculator caps the actual "
			+ "bike speed at the link freespeed, so too low a value would slow cyclists down.")
	private double bikeFreespeed;

	@Option(names = "--bike-capacity", defaultValue = DEFAULT_BIKE_CAPACITY,
		description = "Capacity of the split-off bike links in vehicles/h")
	private double bikeCapacity;

	@Option(names = "--link-geometries",
		description = "Optional geometry companion CSV of the input network. When given, a copy "
			+ "with one added row per bike link (the car link's polyline) is written next to the "
			+ "output network, so the twins do not render as straight lines.")
	private Path linkGeometries;

	public static void main(String[] args) {
		new SplitBikeLinks().execute(args);
	}

	@Override
	public Integer call() {

		Network network = NetworkUtils.createNetwork();
		NetworkUtils.readNetwork(network, networkFile.toString());
		log.info("Read network: {} nodes, {} links", network.getNodes().size(), network.getLinks().size());

		Map<Id<Link>, Id<Link>> pairs = process(network, bikeMode, bikeFreespeed, bikeCapacity);

		new NetworkWriter(network).write(output.toString());
		log.info("Wrote {} links to {}", network.getLinks().size(), output);

		if (linkGeometries != null) {
			Path out = SumoBicycleAttributes.companion(output, "-linkGeometries.csv");
			extendGeometries(linkGeometries, out, pairs);
		}

		return 0;
	}

	/**
	 * The actual split, mutating the network in place.
	 *
	 * @return car link id → bike link id for every created pair
	 */
	static Map<Id<Link>, Id<Link>> process(Network network, String bikeMode,
										   double bikeFreespeed, double bikeCapacity) {

		List<Link> candidates = new ArrayList<>();
		for (Link link : network.getLinks().values()) {
			if (!link.getAllowedModes().contains(TransportMode.car)
				|| !link.getAllowedModes().contains(bikeMode)) continue;
			BicycleInfraCategory category = categoryOf(link);
			if (category != null && SPLIT_CATEGORIES.contains(category)) candidates.add(link);
		}

		Map<Id<Link>, Id<Link>> pairs = new LinkedHashMap<>();
		EnumMap<BicycleInfraCategory, Integer> byCategory = new EnumMap<>(BicycleInfraCategory.class);

		for (Link link : candidates) {
			Id<Link> bikeId = Id.createLinkId(link.getId() + BIKE_SUFFIX);
			if (network.getLinks().containsKey(bikeId)) {
				throw new IllegalStateException("Link id " + bikeId + " already exists - either the "
					+ "network was split before, or '" + BIKE_SUFFIX + "' is not free to reserve here.");
			}

			Link bike = network.getFactory().createLink(bikeId, link.getFromNode(), link.getToNode());
			bike.setLength(link.getLength());
			bike.setFreespeed(bikeFreespeed);
			bike.setCapacity(bikeCapacity);
			bike.setNumberOfLanes(1);
			bike.setAllowedModes(Set.of(bikeMode));

			// Everything the scoring reads on the bike side - surface, smoothness,
			// cyclewaytype, the elevation metrics - plus provenance like origid and the
			// stored geometry travel along as copies; only car bookkeeping stays behind.
			AttributesUtils.copyAttributesFromTo(link, bike);
			CAR_ONLY_ATTRIBUTES.forEach(a -> bike.getAttributes().removeAttribute(a));

			// The category MOVES: it now describes the bike link, and leaving it on the
			// car link would double-count the infrastructure in every statistic.
			link.getAttributes().removeAttribute(BICYCLE_INFRA);
			link.getAttributes().removeAttribute(BICYCLE_INFRA_MIXED);

			bike.getAttributes().putAttribute(CAR_LINK, link.getId().toString());
			link.getAttributes().putAttribute(BIKE_LINK, bikeId.toString());

			Set<String> modes = new HashSet<>(link.getAllowedModes());
			modes.remove(bikeMode);
			link.setAllowedModes(modes);

			network.addLink(bike);
			pairs.put(link.getId(), bikeId);
			byCategory.merge(categoryOf(bike), 1, Integer::sum);
		}

		// Connectivity needs no repair pass: each bike link replicates its car link's
		// from/to connectivity exactly, so every bike path that existed still exists.
		log.info("Split {} bike link(s) off their car links; network now has {} links.",
			pairs.size(), network.getLinks().size());
		byCategory.forEach((cat, n) -> log.info("  {} {}", String.format("%6d", n), cat));

		return pairs;
	}

	private static BicycleInfraCategory categoryOf(Link link) {
		Object raw = link.getAttributes().getAttribute(BICYCLE_INFRA);
		if (raw == null) return null;
		try {
			return BicycleInfraCategory.valueOf(raw.toString());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Copies the geometry companion, appending one row per bike link with its car link's
	 * polyline — the pair shares the road's course by construction.
	 */
	private static void extendGeometries(Path in, Path out, Map<Id<Link>, Id<Link>> pairs) {

		// IOUtils picks gzip from the file extension on both ends, matching the
		// companion-naming convention the rest of the toolchain uses.
		try (CSVParser parser = CSVParser.parse(IOUtils.getBufferedReader(in.toString()),
			CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build());
			 CSVPrinter printer = new CSVPrinter(
				 IOUtils.getBufferedWriter(out.toString()), CSVFormat.DEFAULT)) {

			printer.printRecord((Object[]) parser.getHeaderNames().toArray(new String[0]));
			for (CSVRecord record : parser) {
				printer.printRecord(record);
				Id<Link> bikeId = pairs.get(Id.createLinkId(record.get(0)));
				if (bikeId != null) {
					List<String> row = new ArrayList<>(record.toList());
					row.set(0, bikeId.toString());
					printer.printRecord(row);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		log.info("Wrote geometry companion with bike-link rows to {}", out);
	}
}
