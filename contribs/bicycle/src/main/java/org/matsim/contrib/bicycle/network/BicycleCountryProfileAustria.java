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
 * <p><b>Status: example / reference implementation.</b> This profile exists
 * primarily to demonstrate how a non-German country can be plugged into the
 * classifier; it is <i>not</i> production-validated. The sign codes below
 * were chosen from an Austria-wide aggregation of {@code traffic_sign=*}
 * values (~13,600 tagged ways) but there is no end-to-end test against a
 * real Austrian scenario yet. Expect edge cases. Treat this file as a
 * starting point alongside {@link BicycleCountryProfileGermany} when adding
 * further countries, not as a finished AT profile.
 *
 * <p>Codes matched, with observed OSM-AT counts:
 * <ul>
 *   <li>{@code AT:53.26}    — Fahrradstraße. Covers ~93% of {@code bicycle_road=yes}
 *       ways. Common variant: with a {@code 54[Durchfahrt gestattet/erlaubt]}
 *       subsign permitting motor through-traffic.</li>
 *   <li>{@code AT:52.17}    — Radweg (~160).</li>
 *   <li>{@code AT:52.17a-a} — Geh- und Radweg, gemeinsam (~1300, most common).</li>
 *   <li>{@code AT:52.17a}   — short form of the above (~90).</li>
 *   <li>{@code AT:52.17a-b} — Geh- und Radweg, getrennt (~70).</li>
 *   <li>{@code AT:53.28b}   — Radweg ohne Benutzungspflicht (~11).</li>
 * </ul>
 *
 * <p>Two notation quirks to be aware of:
 * <ol>
 *   <li>The Fahrradstraße sign is StVO § 53 Z <b>26</b> (not 27).</li>
 *   <li>{@code 52.17a-a} / {@code 52.17a-b} is an OSM-AT convention where
 *       the trailing {@code -a}/{@code -b} distinguishes shared from
 *       segregated. The bare StVO form {@code AT:52.17b} does not occur in
 *       the data.</li>
 * </ol>
 *
 * @author smetzler
 */
public final class BicycleCountryProfileAustria implements BicycleCountryProfile {

	@Override
	public boolean isRightHandTraffic() {
		return true;
	}

	@Override
	public boolean isBicycleRoadSign(String trafficSign) {
		// AT:53.26 — Fahrradstraße. Unambiguous prefix in the AT sign space.
		return !isEmpty(trafficSign) && trafficSign.contains("AT:53.26");
	}

	@Override
	public boolean isBicycleRoadVehicleDestinationSign(String trafficSign) {
		// 54[…] subsign that allows motor through-traffic on the Fahrradstraße,
		// analogous to the German "Kfz frei" / "Anlieger frei" 1020-30 subsign
		// on a DE:244. We match both the two dominant wordings seen on actual
		// AT:53.26 ways ("Durchfahrt gestattet/erlaubt") and the generic
		// Austrian "Anrainer/Kfz frei" fragments — the latter rarely appear on
		// AT:53.26 in the sample data but are the semantic match and cost
		// nothing to include (this predicate is only consulted once we already
		// know the link is a Fahrradstraße, so false-positives are bounded).
		if (isEmpty(trafficSign)) return false;
		return trafficSign.contains("Durchfahrt gestattet")
			|| trafficSign.contains("Durchfahrt erlaubt")
			|| trafficSign.contains("Anrainer frei")
			|| trafficSign.contains("Ausgenommen Anrainer")
			|| trafficSign.contains("Kfz frei")
			|| trafficSign.contains("PKW frei")
			|| trafficSign.contains("Pkw frei");
	}

	@Override
	public boolean isProtectedCyclewaySign(String trafficSign) {
		// AT:52.17 (Radweg) — bare form only, to not match the 52.17a* foot+bike variants.
		// AT:53.28b (Radweg ohne Benutzungspflicht); NOT AT:53.28a, which is the cancellation sign.
		if (isEmpty(trafficSign)) return false;
		if (containsStandaloneCode(trafficSign, "AT:52.17")) return true;
		return trafficSign.contains("AT:53.28b");
	}

	@Override
	public boolean isSharedFootCyclewaySign(String trafficSign) {
		// Geh- und Radweg, gemeinsam — AT:52.17a-a (long form) or AT:52.17a (short).
		// The short form must not match the segregated AT:52.17a-b.
		if (isEmpty(trafficSign)) return false;
		if (trafficSign.contains("AT:52.17a-a")) return true;
		return containsCodeNotFollowedBy(trafficSign, "AT:52.17a", "-b");
	}

	@Override
	public boolean isSegregatedFootCyclewaySign(String trafficSign) {
		// Geh- und Radweg, getrennt — AT:52.17a-b (OSM-AT convention; bare AT:52.17b is not used).
		return !isEmpty(trafficSign) && trafficSign.contains("AT:52.17a-b");
	}

	@Override
	public boolean isFootwayBicycleAllowedSign(String trafficSign) {
		// No widely-used AT subsign equivalent of DE:1022-10; classifier falls back to bicycle=yes.
		return false;
	}

	@Override
	public boolean isSharedBusLaneBusWithBikeSign(String trafficSign) {
		// OSM-AT handles this via cycleway=share_busway directly.
		return false;
	}

	@Override
	public boolean isSharedBusLaneBikeWithBusSign(String trafficSign) {
		return false;
	}

	// ------------------------------------------------------------------------

	/** True iff {@code code} occurs followed by a non-letter/non-digit (or end of string). */
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

	/** True iff {@code code} occurs and is NOT immediately followed by {@code forbiddenSuffix}. */
	private static boolean containsCodeNotFollowedBy(String haystack, String code, String forbiddenSuffix) {
		int idx = 0;
		while ((idx = haystack.indexOf(code, idx)) >= 0) {
			int after = idx + code.length();
			boolean followedByForbidden = haystack.regionMatches(after, forbiddenSuffix, 0, forbiddenSuffix.length());
			if (!followedByForbidden) return true;
			idx = after;
		}
		return false;
	}

	private static boolean isEmpty(String s) {
		return s == null || s.isBlank();
	}
}
