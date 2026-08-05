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

import org.matsim.api.core.v01.TransportMode;
import picocli.CommandLine.Option;

import java.util.Set;

/**
 * The options shared by the two network-building commands ({@code bicycle-attributes},
 * {@code bicycle-network}), as a picocli mixin: declared once, embedded via
 * {@code @CommandLine.Mixin}, so the commands cannot drift apart in names, defaults or
 * help texts. {@code bicycle-keep-edges} stays out on purpose — it needs only
 * {@code --country} and must not grow the other options.
 */
final class BicycleBuildOptions {

	// Also the source for both commands' Params.defaults(); annotation defaults have to
	// be compile-time String constants.
	static final String DEFAULT_ELE_SAMPLE_STEP = "20.0";
	static final String DEFAULT_ELE_NOISE_TOLERANCE = "3.0";

	@Option(names = "--country", defaultValue = "de",
		description = "Country profile for traffic-sign interpretation: de, at, or generic. "
			+ "Use 'generic' for a country without a profile; it skips traffic-sign matching "
			+ "and relies on tag-based classification only.")
	private String country;

	@Option(names = "--mode", defaultValue = TransportMode.bike,
		description = "Network mode name for cyclable links; the network is built with 'bike' "
			+ "and renamed at the very end.")
	private String mode;

	@Option(names = "--bike-area-marker",
		description = "OSM tag selecting the ways that get the full bicycle treatment, as 'key' "
			+ "or 'key=value' (e.g. 'city_center=yes'). Ways without it keep their modes -- bikes "
			+ "may still ride them -- but get no bicycle attributes, no classification and no "
			+ "elevation metrics. Omit to treat every way as cyclable.")
	private String bikeAreaMarker;

	@Option(names = "--drop-ways-without-infra", split = ",", paramLabel = "HIGHWAY_TYPE",
		description = "Minor way types - typically 'track,path' - to drop where the link ended up "
			+ "with bicycle_infra=NONE. A link goes only if ALL of its OSM ways are of these types, "
			+ "it classified as NONE, and none of them carries bicycle=yes/designated, so signposted "
			+ "and explicitly opened field tracks survive. Values are OSM highway types (note that "
			+ "'unclassified' is one of them: a minor road, not 'without classification'). Meant for "
			+ "city models with a rural ring, where field and forest tracks add many links and little "
			+ "cycling network; leave empty for rural models, where those tracks are the network.")
	private Set<String> dropWaysWithoutInfra = Set.of();

	@Option(names = "--ele-sample-step", defaultValue = DEFAULT_ELE_SAMPLE_STEP,
		description = "Distance between elevation samples along a link, in m")
	private double eleSampleStepM;

	@Option(names = "--ele-noise-tolerance", defaultValue = DEFAULT_ELE_NOISE_TOLERANCE,
		description = "Douglas-Peucker vertical tolerance for smoothing the profile, in m")
	private double eleNoiseToleranceM;

	String country() {
		return country;
	}

	String mode() {
		return mode;
	}

	double eleSampleStep() {
		return eleSampleStepM;
	}

	double eleNoiseTolerance() {
		return eleNoiseToleranceM;
	}

	/** The parsed {@code --bike-area-marker}, or {@code null} when every way is cyclable. */
	BicycleLinkPolicy.AreaMarker areaMarkerOrNull() {
		return bikeAreaMarker != null ? BicycleLinkPolicy.AreaMarker.parse(bikeAreaMarker) : null;
	}

	/**
	 * The {@code --drop-ways-without-infra} highway types; empty means the rule is off.
	 * These are minor way types such as {@code track} and {@code path} — not OSM's
	 * {@code highway=unclassified}, which is an ordinary minor road.
	 */
	Set<String> dropWaysWithoutInfra() {
		return dropWaysWithoutInfra == null ? Set.of() : dropWaysWithoutInfra;
	}
}
