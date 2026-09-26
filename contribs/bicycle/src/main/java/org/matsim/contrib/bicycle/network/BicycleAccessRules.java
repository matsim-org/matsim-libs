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

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.matsim.contrib.bicycle.network.BicycleOsmTags.*;

/**
 * Who may ride a way, decided from its OSM tags alone — the single rule order both
 * build paths obey.
 *
 * <p>The rules used to live twice, once in {@link BicycleLinkPolicy} (Supersonic/OSM)
 * and once in {@code SumoBicycleAttributes} (SUMO), and the copies had already drifted:
 * the OSM side evaluated {@code bicycle=no} before {@code access}, and returned right
 * after it. A way tagged {@code access=private} + {@code bicycle=no} therefore survived
 * as a car link on one path and was dropped on the other, and
 * {@code --drop-ways-without-infra} never even ran on a {@code bicycle=no} way there.
 * Both paths now call {@link #evaluate}, so the order cannot diverge again without a
 * failing test.
 *
 * <p>Pure functions over tags: no link is touched here, no network state is read.
 * Applying the verdict — emptying the modes, bumping a counter — stays with the caller,
 * because that is the one thing the two paths legitimately do differently.
 *
 * <h2>Why a list of ways</h2>
 *
 * A Supersonic link comes from exactly one OSM way; a SUMO link may come from several,
 * because netconvert merges edges without looking at the tags. The rules fold over the
 * ways, and for a single way every fold degenerates to the plain predicate — which is
 * what makes one implementation serve both.
 *
 * <p>The fold is {@code anyMatch} for the drop rules — half a parking aisle is not a
 * through route — with one deliberate exception: the {@code --drop-ways-without-infra}
 * rule needs {@code allMatch} on the way type. A link merged from a track and a
 * residential road is half a real road, and discarding it on the strength of the track
 * half would be wrong.
 *
 * @author smetzler
 */
public final class BicycleAccessRules {

	private BicycleAccessRules() {
	}

	/** Why a link may not stay in the network. */
	public enum DropReason {
		/** {@code service=parking_aisle} — a parking-lot aisle, not a cycling route. */
		PARKING_AISLE,
		/** {@code access=no/private/customer/...} without a bicycle-specific override. */
		ACCESS_RESTRICTED,
		/** {@code highway=footway/pedestrian} without {@code bicycle=yes/designated}. */
		FOOTWAY_WITHOUT_BIKE,
		/** A {@code --drop-ways-without-infra} type that classified as {@code NONE}. */
		MINOR_WAY_WITHOUT_INFRA
	}

	/**
	 * What the rules have to say about a link.
	 *
	 * @param bikeForbidden {@code bicycle=no} on at least one of the ways: the bike mode
	 *                      has to go, but the link itself stays open to the other modes —
	 *                      a {@code highway=primary} survives as a car link. Reported
	 *                      even alongside a {@code dropReason}, so a caller that counts
	 *                      both sees the same numbers as before.
	 * @param dropReason    why the link has to go entirely, or {@code null} if it stays
	 */
	public record Verdict(boolean bikeForbidden, DropReason dropReason) {

		/** Whether the link has to go entirely. */
		public boolean dropped() {
			return dropReason != null;
		}
	}

	private static final Verdict KEEP = new Verdict(false, null);
	private static final Verdict KEEP_WITHOUT_BIKE = new Verdict(true, null);

	/**
	 * The verdict for a link built from a single OSM way — the Supersonic case.
	 *
	 * @see #evaluate(List, BicycleInfraCategory, Set)
	 */
	public static Verdict evaluate(Map<String, String> tags, BicycleInfraCategory infra,
								   Set<String> dropWaysWithoutInfra) {
		return evaluate(List.of(tags), infra, dropWaysWithoutInfra);
	}

	/**
	 * The verdict for a link built from one or more OSM ways.
	 *
	 * <p>Rule order, and it is the order that matters:
	 * <ol>
	 *   <li>{@code service=parking_aisle} → drop,</li>
	 *   <li>restricted {@code access} → drop,</li>
	 *   <li>footway/pedestrian without bike permission → drop,</li>
	 *   <li>{@code bicycle=no} → the bike mode goes, the link stays,</li>
	 *   <li>{@code --drop-ways-without-infra} → drop.</li>
	 * </ol>
	 *
	 * <p>Rule 4 does not end the evaluation: a {@code bicycle=no} way of a minor type
	 * that carries no infrastructure is exactly what rule 5 is meant to discard, and
	 * returning early would have kept it. Rule 5 stays last so the more specific reasons
	 * above keep their counters.
	 *
	 * @param ways                the tags of every OSM way behind the link, never empty
	 * @param infra               the link's classification, needed by rule 5
	 * @param dropWaysWithoutInfra the {@code --drop-ways-without-infra} highway types;
	 *                             empty switches rule 5 off
	 */
	public static Verdict evaluate(List<Map<String, String>> ways, BicycleInfraCategory infra,
								   Set<String> dropWaysWithoutInfra) {

		if (ways.stream().anyMatch(t -> SV_PARKING_AISLE.equals(t.get(SERVICE)))) {
			return new Verdict(false, DropReason.PARKING_AISLE);
		}

		if (ways.stream().anyMatch(BicycleAccessRules::isAccessRestricted)) {
			return new Verdict(false, DropReason.ACCESS_RESTRICTED);
		}

		if (ways.stream().anyMatch(BicycleAccessRules::isFootwayWithoutBikePermission)) {
			return new Verdict(false, DropReason.FOOTWAY_WITHOUT_BIKE);
		}

		boolean bikeForbidden = ways.stream().anyMatch(t -> NO.equals(t.get(BICYCLE)));

		if (isMinorWayWithoutInfra(ways, infra, dropWaysWithoutInfra)) {
			return new Verdict(bikeForbidden, DropReason.MINOR_WAY_WITHOUT_INFRA);
		}

		return bikeForbidden ? KEEP_WITHOUT_BIKE : KEEP;
	}

	// ------------------------------------------------------------------------
	// The predicates, one copy
	// ------------------------------------------------------------------------

	/**
	 * Whether every way is one of the configured minor types, the link ended up without
	 * cycling infrastructure, and no way carries a bicycle-specific permission.
	 *
	 * <p>Two guards keep this rule out of trouble. The classification check spares a
	 * signposted cycle route that happens to run over a {@code highway=track} — traffic
	 * sign DE:237 or a shared foot/cycleway classifies it, so it is never {@code NONE}.
	 * The {@code bicycle=yes/designated} check spares the rest: plain tracks that OSM
	 * marks as open to bikes, which the classifier alone leaves at {@code NONE} — on
	 * rural ground the backbone of everyday cycling.
	 */
	private static boolean isMinorWayWithoutInfra(List<Map<String, String>> ways, BicycleInfraCategory infra,
												  Set<String> dropWaysWithoutInfra) {
		if (dropWaysWithoutInfra.isEmpty() || infra != BicycleInfraCategory.NONE) return false;
		if (ways.stream().anyMatch(BicycleAccessRules::bicycleExplicitlyAllowed)) return false;
		// A way without a highway tag counts as "not a minor way", which stops the
		// allMatch. Keeping it out of the set lookup also spares Set.of()'s NPE on null.
		return ways.stream().allMatch(t -> {
			String highway = t.get(HIGHWAY);
			return highway != null && dropWaysWithoutInfra.contains(highway);
		});
	}

	/**
	 * A restricted general {@code access} with no bicycle-specific permission overriding
	 * it. Null-safe: raw OSM tag maps usually have no {@code access} key at all.
	 */
	static boolean isAccessRestricted(Map<String, String> tags) {
		String access = tags.get(ACCESS);
		boolean restricted = access != null && ACCESS_RESTRICTED.contains(access);
		// OSM: the more specific tag wins, so access=private + bicycle=designated stays
		// cyclable.
		return restricted && !bicycleExplicitlyAllowed(tags);
	}

	/** A footway or pedestrian way that bikes are not explicitly let onto. */
	static boolean isFootwayWithoutBikePermission(Map<String, String> tags) {
		String highway = tags.get(HIGHWAY);
		return (HW_FOOTWAY.equals(highway) || HW_PEDESTRIAN.equals(highway))
			&& !bicycleExplicitlyAllowed(tags);
	}

	/**
	 * Whether the tags carry a bicycle-specific permission that grants cycling over a
	 * more general restriction — the footway/pedestrian default, or a restricted
	 * {@code access}. Only {@code bicycle=yes} / {@code =designated} count for now; we
	 * might want to keep {@code bicycle=permissive} / {@code =dismount} as well.
	 */
	static boolean bicycleExplicitlyAllowed(Map<String, String> tags) {
		String bicycle = tags.get(BICYCLE);
		return YES.equals(bicycle) || DESIGNATED.equals(bicycle);
	}
}
