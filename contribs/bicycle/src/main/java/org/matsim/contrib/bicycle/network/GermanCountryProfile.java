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
 * German implementation of {@link BicycleCountryProfile}.
 *
 * <p>Encodes the German traffic-sign codes (StVO / VzKat) referenced by
 * {@link BicycleInfraClassifier}. This is the default profile and the
 * behaviour matches the pre-profile implementation byte-for-byte.
 *
 * <p>The {@code traffic_sign} values arriving here are raw OSM strings,
 * possibly with concatenated subsigns (e.g. {@code "DE:244,1020-30"} or
 * {@code "DE:245,1022-10,1026-30"}). Matching is via {@code contains} /
 * {@code startsWith} on the raw string — same approach as the previous
 * inline checks, same false-positive surface (e.g. a hypothetical
 * {@code "DE:1244"} would match a {@code "DE:244"} probe, but no such
 * sign exists in the catalogue).
 *
 * @author smetzler
 */
public final class GermanCountryProfile implements BicycleCountryProfile {

	@Override
	public boolean isRightHandTraffic() {
		return true;
	}

	@Override
	public boolean isBicycleRoadSign(String trafficSign) {
		// DE:244 — Fahrradstraße
		return !isEmpty(trafficSign) && trafficSign.contains("DE:244");
	}

	@Override
	public boolean isBicycleRoadVehicleDestinationSign(String trafficSign) {
		if (isEmpty(trafficSign)) return false;
		// 1020-30 — "Anlieger frei", and the various textual "Kfz frei" spellings
		// seen in OSM (see osm-traffic-sign-tool issue #51).
		return trafficSign.contains("1020-30")
			|| trafficSign.contains("Kraftfahrzeuge-frei")
			|| trafficSign.contains("Kfz-Verkehr frei")
			|| trafficSign.contains("KFZ frei");
	}

	@Override
	public boolean isProtectedCyclewaySign(String trafficSign) {
		// DE:237 — Radweg
		return !isEmpty(trafficSign) && trafficSign.contains("DE:237");
	}

	@Override
	public boolean isSharedFootCyclewaySign(String trafficSign) {
		// DE:240 — Gemeinsamer Geh-/Radweg. The classifier checks via
		// `contains("240")` rather than `contains("DE:240")` for historical
		// reasons (parity with the FixMyCity Lua); kept as-is here.
		return !isEmpty(trafficSign) && trafficSign.contains("240");
	}

	@Override
	public boolean isSegregatedFootCyclewaySign(String trafficSign) {
		// DE:241 — Getrennter Geh-/Radweg (also DE:241-30, DE:241-31).
		return !isEmpty(trafficSign) && trafficSign.contains("241");
	}

	@Override
	public boolean isFootwayBicycleAllowedSign(String trafficSign) {
		// 1022-10 — "Fahrrad frei"
		return !isEmpty(trafficSign) && trafficSign.contains("1022-10");
	}

	@Override
	public boolean isSharedBusLaneBusWithBikeSign(String trafficSign) {
		// DE:245 (Bussonderfahrstreifen) + 1022-10 ("Fahrrad frei")
		// or 1022-14 ("Fahrrad & Mofa frei").
		if (isEmpty(trafficSign) || !trafficSign.startsWith("DE:245")) return false;
		return trafficSign.contains("1022-10") || trafficSign.contains("1022-14");
	}

	@Override
	public boolean isSharedBusLaneBikeWithBusSign(String trafficSign) {
		// DE:237 (Radweg) + 1024-14 ("Bus frei") or 1026-32 ("Linienverkehr frei").
		if (isEmpty(trafficSign) || !trafficSign.startsWith("DE:237")) return false;
		return trafficSign.contains("1024-14") || trafficSign.contains("1026-32");
	}

	// ------------------------------------------------------------------------

	private static boolean isEmpty(String s) {
		return s == null || s.isBlank();
	}
}
