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
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * The pieces of bicycle network building that are independent of where the network
 * came from: elevation stamping and the infrastructure distribution table. The link
 * attribute keys live in {@link BicycleUtils}, next to the getters scoring uses.
 *
 * <p>Two pipelines share these — {@link BicycleNetworkPipeline}, which reads OSM
 * directly, and {@link SumoBicycleAttributes}, which post-processes a network that
 * came out of SUMO. Everything here works on a finished {@link Link}, so it does not
 * care which of the two produced it.
 */
final class BicycleNetworkOps {

	private static final Logger log = LogManager.getLogger(BicycleNetworkOps.class);

	private BicycleNetworkOps() {
	}

	// ---- elevation -------------------------------------------------------------

	/**
	 * Stamps the DEM elevation onto a node that has none yet. Synchronized because
	 * the OSM reader calls this from its worker threads.
	 */
	static synchronized void addNodeElevation(Node node, ElevationDataParser parser) {
		addNodeElevation(node, parser::getElevation);
	}

	/**
	 * Same, against any elevation source — lets tests drive it without a DEM file.
	 *
	 * @return whether a Z was stamped; {@code false} when the DEM has no data there
	 */
	static synchronized boolean addNodeElevation(Node node, LinkElevationProfile.ElevationSource elevation) {
		if (node.getCoord().hasZ()) return false;

		double z = elevation.at(node.getCoord());
		if (Double.isNaN(z)) return false;

		node.setCoord(CoordUtils.createCoord(node.getCoord().getX(), node.getCoord().getY(), z));
		return true;
	}

	/**
	 * Writes the five elevation metrics onto the link, rounded to the resolution the
	 * numbers actually carry.
	 *
	 * <p>Writes nothing when the profile came out as NaN, which happens when the DEM has
	 * no data along that link. An absent attribute says "not known" — a rounded NaN, or
	 * worse a raw no-data value, would read as a measurement.
	 *
	 * @return whether the metrics were written
	 */
	static boolean attachElevationMetrics(Link link, LinkElevationProfile.Metrics m) {

		if (Double.isNaN(m.averageElevation()) || Double.isNaN(m.gradient())) {
			return false;
		}

		// Elevations in meters — round to 1 decimal (matches DEM resolution).
		link.getAttributes().putAttribute(BicycleUtils.AVERAGE_ELEVATION, round(m.averageElevation(), 1));
		link.getAttributes().putAttribute(BicycleUtils.ELEVATION_GAIN, round(m.elevationGain(), 1));
		link.getAttributes().putAttribute(BicycleUtils.ELEVATION_LOSS, round(m.elevationLoss(), 1));

		// Dimensionless ratios — 3 decimals = 0.1% resolution.
		link.getAttributes().putAttribute(BicycleUtils.GRADIENT, round(m.gradient(), 3));
		link.getAttributes().putAttribute(BicycleUtils.MAX_GRADIENT, round(m.maxGradient(), 3));
		return true;
	}

	static double round(double v, int decimals) {
		double factor = Math.pow(10, decimals);
		return Math.round(v * factor) / factor;
	}

	// ---- modes -----------------------------------------------------------------

	/**
	 * Renames a mode in every link's allowed-modes set; a no-op when {@code from} equals
	 * {@code to}. Both pipelines run this last, driven by {@code --mode}.
	 *
	 * <p>The OSM {@code bicycle=...} restriction value is unaffected: it is stored under
	 * {@code osm:bicycle}, not under the mode name.
	 *
	 * @return the number of links whose mode set was changed
	 */
	static int renameMode(Network network, String from, String to) {
		if (from.equals(to)) {
			log.info("Network mode is already '{}', no rename needed.", to);
			return 0;
		}
		int renamed = 0;
		for (Link link : network.getLinks().values()) {
			Set<String> modes = new HashSet<>(link.getAllowedModes());
			if (modes.remove(from)) {
				modes.add(to);
				link.setAllowedModes(modes);
				renamed++;
			}
		}
		log.info("Renamed network mode '{}' -> '{}' on {} links.", from, to, renamed);
		return renamed;
	}

	/**
	 * Gives the configured modes exactly the links that allow car.
	 *
	 * <p>The motorised modes describe the same vehicles on the same roads, and scenarios
	 * such as Berlin v7.0 carry them on an identical link set. They drift apart on the way
	 * here: SUMO decides {@code truck} from its own permissions, the Supersonic reader
	 * assigns {@code car} and {@code bike} only, and anything that removes car from a link
	 * (the cleaners, most of all) does not touch the others. Rather than patch each cause,
	 * the modes are re-derived from car once everything that can change the link set has
	 * run — which is why both pipelines call this last.
	 *
	 * <p>A link whose <em>only</em> modes were mirrored ones — a truck-only depot access,
	 * say — loses its last mode here and is removed along with its orphaned nodes, because
	 * nothing cleans afterwards.
	 *
	 * <p>Which modes to mirror is deliberately not hardcoded: Berlin v7.0 uses
	 * {@code freight}, while Dresden v1.1 drops {@code truck} and adds
	 * {@code longDistanceFreight} instead.
	 *
	 * @return the number of links whose mode set was changed
	 */
	static int mirrorCarModes(Network network, Set<String> mirrored) {

		if (mirrored.isEmpty()) return 0;

		int changedLinks = 0;
		List<Id<Link>> emptied = new ArrayList<>();
		for (Link link : network.getLinks().values()) {
			Set<String> modes = new TreeSet<>(link.getAllowedModes());
			boolean car = modes.contains(TransportMode.car);
			boolean changed = car ? modes.addAll(mirrored) : modes.removeAll(mirrored);
			if (changed) {
				link.setAllowedModes(modes);
				changedLinks++;
				if (modes.isEmpty()) emptied.add(link.getId());
			}
		}

		if (!emptied.isEmpty()) {
			emptied.forEach(network::removeLink);
			NetworkUtils.removeNodesWithoutLinks(network);
			log.info("Removed {} link(s) the mirroring left without any mode.", emptied.size());
		}

		log.info("Mirrored car onto {}: {} link(s) changed.", mirrored, changedLinks);
		return changedLinks;
	}

	// ---- reporting -------------------------------------------------------------

	/**
	 * Counts links by their {@code bicycle_infra} attribute and logs a sorted summary
	 * table with link counts, total length in km, and percentages. Useful as a sanity
	 * check during scenario development — if the {@code NONE} or
	 * {@code NEEDS_CLARIFICATION} count jumps unexpectedly between two OSM extracts,
	 * this is where you would notice.
	 *
	 * <p>Categories with zero count are omitted; links whose attribute value matches no
	 * known {@link BicycleInfraCategory} are tallied under "(unparseable)".
	 *
	 * <p>Sorted by total length, descending, ties broken by enum declaration order.
	 *
	 * @param label short context tag for the log header, e.g. {@code "after OSM read"}
	 */
	static void logInfraDistribution(Network network, String label) {
		EnumMap<BicycleInfraCategory, Integer> counts = new EnumMap<>(BicycleInfraCategory.class);
		EnumMap<BicycleInfraCategory, Double> lengthsM = new EnumMap<>(BicycleInfraCategory.class);
		int unparseableCount = 0;
		double unparseableLengthM = 0;
		int totalCount = 0;
		double totalLengthM = 0;

		for (Link link : network.getLinks().values()) {
			Object raw = link.getAttributes().getAttribute(BicycleUtils.BICYCLE_INFRA);
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
}
