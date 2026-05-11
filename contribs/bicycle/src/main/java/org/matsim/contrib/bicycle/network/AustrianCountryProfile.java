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
 * Austrian implementation of {@link BicycleCountryProfile}.
 *
 * <p>Encodes Austrian traffic-sign codes from the StVO ("Straßenverkehrsordnung 1960").
 * The Austrian system is structurally similar to the German one — same categories
 * of cycling infrastructure exist — only the sign codes differ. The OSM tagging
 * convention prefixes them with {@code AT:} (e.g. {@code AT:52.17a} for the
 * shared foot/bike path).
 *
 * <p><b>Sign codes referenced here</b> (StVO § 52 = Hinweiszeichen, § 53 = Vorschriftszeichen):
 * <ul>
 *   <li>{@code AT:52.17}  — Radweg (cycleway, dedicated)</li>
 *   <li>{@code AT:52.17a} — Geh- und Radweg, gemeinsam (shared foot+bike path)</li>
 *   <li>{@code AT:52.17b} — Geh- und Radweg, getrennt (segregated foot+bike path)</li>
 *   <li>{@code AT:53.27}  — Fahrradstraße (bicycle road; introduced 2022)</li>
 *   <li>{@code AT:53.28b} — Radweg ohne Benutzungspflicht (non-mandatory cycleway;
 *       seen in Vienna OSM data, e.g. way 1239977333)</li>
 * </ul>
 *
 * <p><b>Caveats — please read before relying on this in production:</b>
 * <ul>
 *   <li><b>Sign codes not validated against taginfo.</b> I picked these codes
 *       from the Austrian StVO and from OSM-AT community discussions, but did
 *       not cross-check actual usage frequency at
 *       <a href="https://taginfo.geofabrik.de/europe/austria/keys/traffic_sign">
 *       taginfo for Austria</a>. Real-world usage may use slightly different
 *       spellings (e.g. {@code AT:52.17a} vs {@code AT:52/17a} — both have
 *       been seen in OSM). Before relying on this for a real Vienna/Graz/Linz
 *       run, verify the top-N values at the taginfo page above.</li>
 *   <li><b>"Anlieger frei" handling.</b> Austria distinguishes "Anrainer frei"
 *       (residents only) from "ausgenommen Anrainer" (excluding residents).
 *       Like the German profile we match on text fragments seen in {@code traffic_sign};
 *       the Austrian variants may need to be added once observed.</li>
 *   <li><b>{@code AT:53.27} (Fahrradstraße)</b> is relatively new (2022) and
 *       tagging adoption is still patchy — many Austrian bicycle roads are
 *       tagged via {@code bicycle_road=yes} alone (which our shared logic
 *       in {@link BicycleInfraClassifier#getBicycleRoadType} already handles).</li>
 *   <li>The {@code SharedFootCyclewaySign} / {@code SegregatedFootCyclewaySign}
 *       checks here look for the literal substring {@code "52.17"}; this is
 *       safe because {@code AT:52.17} only exists in the cycling context.
 *       Using a more specific match (e.g. {@code AT:52.17a} for shared,
 *       {@code AT:52.17b} for segregated) would be more precise but the OSM
 *       data isn't always tagged that strictly.</li>
 * </ul>
 *
 * @author smetzler
 */
public final class AustrianCountryProfile implements BicycleCountryProfile {

	@Override
	public boolean isRightHandTraffic() {
		return true;
	}

	@Override
	public boolean isBicycleRoadSign(String trafficSign) {
		// AT:53.27 — Fahrradstraße (Sign § 53/27, introduced 2022).
		return !isEmpty(trafficSign) && trafficSign.contains("AT:53.27");
	}

	@Override
	public boolean isBicycleRoadVehicleDestinationSign(String trafficSign) {
		if (isEmpty(trafficSign)) return false;
		// Austrian equivalents of "Anlieger frei" / "Kfz frei" subsigns.
		// "Ausgenommen Anrainer" or "Anrainer frei" is the typical Austrian
		// wording; "Kfz frei" / "Pkw frei" also seen in practice.
		return trafficSign.contains("Anrainer frei")
			|| trafficSign.contains("Ausgenommen Anrainer")
			|| trafficSign.contains("Kfz frei")
			|| trafficSign.contains("PKW frei")
			|| trafficSign.contains("Pkw frei");
	}

	@Override
	public boolean isProtectedCyclewaySign(String trafficSign) {
		// AT:52.17 — Radweg. We need to distinguish this from AT:52.17a (shared
		// foot+bike) and AT:52.17b (segregated foot+bike), because matching
		// those here would let `getCyclewayTypeForDirection` fire before
		// `getFootAndCyclewayType` and produce a CYCLEWAY_* category for what
		// is actually a combined foot/bike path. So match the bare code only.
		// Also AT:53.28b — Radweg ohne Benutzungspflicht (seen in Vienna data).
		if (isEmpty(trafficSign)) return false;

		// Bare AT:52.17 = the plain cycleway sign. We accept it if it appears as
		// a standalone segment (start of string, end, or surrounded by typical
		// separators ',', ';') and is NOT immediately followed by a letter, which
		// would indicate the a/b variants.
		if (containsStandaloneCode(trafficSign, "AT:52.17")) return true;

		// AT:53.28b — non-mandatory cycleway; the 'b' here is part of the code
		// itself, not a variant suffix, so `contains` is safe.
		return trafficSign.contains("AT:53.28");
	}

	/**
	 * Returns true if {@code code} appears in {@code haystack} as a standalone
	 * token — i.e. the character immediately after {@code code} (if any) is
	 * NOT a letter or a digit. This prevents {@code "AT:52.17"} from matching
	 * inside {@code "AT:52.17a"} or {@code "AT:52.17b"}.
	 */
	private static boolean containsStandaloneCode(String haystack, String code) {
		int idx = 0;
		while ((idx = haystack.indexOf(code, idx)) >= 0) {
			int after = idx + code.length();
			if (after >= haystack.length()) return true;
			char c = haystack.charAt(after);
			if (!Character.isLetterOrDigit(c)) return true;
			idx = after;
		}
		return false;
	}

	@Override
	public boolean isSharedFootCyclewaySign(String trafficSign) {
		// AT:52.17a — Geh- und Radweg (gemeinsam).
		// Matching with "52.17a" only (not bare "52.17") so we don't false-match
		// the plain cycleway sign or the segregated 52.17b variant.
		return !isEmpty(trafficSign) && trafficSign.contains("52.17a");
	}

	@Override
	public boolean isSegregatedFootCyclewaySign(String trafficSign) {
		// AT:52.17b — Geh- und Radweg (getrennt).
		return !isEmpty(trafficSign) && trafficSign.contains("52.17b");
	}

	@Override
	public boolean isFootwayBicycleAllowedSign(String trafficSign) {
		// Austria has no direct equivalent of DE:1022-10 ("Fahrrad frei" subsign
		// on a footway) as widely-used OSM convention. Most "footway with bicycle
		// allowed" cases in Austria are tagged via `bicycle=yes` directly without
		// a traffic_sign subsign. Returning false here means the classifier falls
		// back to the `bicycle=yes` check in BicycleInfraClassifier, which works
		// fine for Austrian data.
		return false;
	}

	@Override
	public boolean isSharedBusLaneBusWithBikeSign(String trafficSign) {
		// Austria: bus lanes opened to bicycles are typically tagged via
		// `cycleway=share_busway` directly rather than via a specific traffic_sign
		// subsign combination. No widely-used Austrian sign code for this case
		// has been observed in OSM-AT discussions, so returning false here is
		// the safe choice — the upstream `cycleway=share_busway` check still
		// catches the actual infrastructure.
		return false;
	}

	@Override
	public boolean isSharedBusLaneBikeWithBusSign(String trafficSign) {
		// Same reasoning as above — Austria handles this via direct tagging,
		// not via composite traffic_sign codes.
		return false;
	}

	// ------------------------------------------------------------------------

	private static boolean isEmpty(String s) {
		return s == null || s.isBlank();
	}
}
