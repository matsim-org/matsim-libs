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

/**
 * Country-agnostic profile: every traffic-sign predicate returns {@code false}.
 *
 * <p>Useful when running the pipeline on OSM data from a country for which
 * no dedicated profile exists yet (Spain, Italy, Switzerland, …). In that
 * case the classifier falls back to the tag-only logic — {@code highway=*},
 * {@code cycleway=*}, {@code bicycle=*}, {@code foot=*}, {@code segregated=*},
 * {@code is_sidepath}, {@code separation:*}, sidewalk subtags, etc. That covers
 * the bulk of OSM cycling-infrastructure data in most countries, because
 * detailed {@code traffic_sign=*} tagging is the exception outside of
 * Germany, Austria, and parts of the Netherlands.
 *
 * <p>Compared to {@link GermanCountryProfile}, this profile loses:
 * <ul>
 *   <li>Bicycle road detection that relies solely on signs (e.g. DE:244 on
 *       a road still tagged {@code highway=residential} with no
 *       {@code bicycle_road=yes}). The {@code bicycle_road=yes} path still
 *       works.</li>
 *   <li>Shared-bus-lane detection via composite sign codes (e.g. DE:245 +
 *       1022-10). The {@code cycleway=share_busway} path still works.</li>
 *   <li>Sign-based shared/segregated foot+bike-path detection (DE:240,
 *       DE:241). The {@code segregated=yes/no} + {@code bicycle=designated} +
 *       {@code foot=designated} fallback still works.</li>
 *   <li>The {@code 1022-10}-based "footway with bicycle allowed" path. The
 *       direct {@code bicycle=yes} check still works.</li>
 * </ul>
 *
 * <p>If your country has well-established traffic-sign tagging in OSM,
 * consider implementing a dedicated {@link BicycleCountryProfile} (use
 * {@link GermanCountryProfile} or {@link AustrianCountryProfile} as a
 * template). This generic profile is a sensible default but not optimal.
 *
 * @author smetzler
 */
public final class GenericCountryProfile implements BicycleCountryProfile {

	@Override
	public boolean isRightHandTraffic() {
		// Most of the world drives on the right. Left-hand traffic countries
		// (UK, IE, MT, CY, AU, NZ, JP, …) would need their own profile anyway
		// because the classifier still hard-codes a number of right-hand
		// assumptions beyond this flag — see the TODO in BicycleInfraClassifier.
		return true;
	}

	@Override public boolean isBicycleRoadSign(String trafficSign) { return false; }
	@Override public boolean isBicycleRoadVehicleDestinationSign(String trafficSign) { return false; }
	@Override public boolean isProtectedCyclewaySign(String trafficSign) { return false; }
	@Override public boolean isSharedFootCyclewaySign(String trafficSign) { return false; }
	@Override public boolean isSegregatedFootCyclewaySign(String trafficSign) { return false; }
	@Override public boolean isFootwayBicycleAllowedSign(String trafficSign) { return false; }
	@Override public boolean isSharedBusLaneBusWithBikeSign(String trafficSign) { return false; }
	@Override public boolean isSharedBusLaneBikeWithBusSign(String trafficSign) { return false; }
}
