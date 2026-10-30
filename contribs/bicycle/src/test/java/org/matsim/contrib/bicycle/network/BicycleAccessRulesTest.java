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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BicycleAccessRules} — the rule order both build paths share.
 *
 * <p>The two cases at the top are the reason this class exists: the rules used to be
 * implemented twice, and the copies had drifted in exactly these two tag combinations.
 * They are regression tests in the strict sense — before the rules were merged, the OSM
 * path and the SUMO path gave different answers for them.
 *
 * @author smetzler
 */
public class BicycleAccessRulesTest {

	private static final Set<String> NO_DROP_TYPES = Set.of();
	private static final Set<String> DROPS_TRACKS = Set.of("track", "path");

	// =========================================================================
	// The two combinations the two paths used to disagree about
	// =========================================================================

	/**
	 * {@code access=private} + {@code bicycle=no}. The OSM path used to evaluate
	 * {@code bicycle=no} first and return right after it, so the link survived as a
	 * car-only link; the SUMO path dropped it. Dropping is the right answer:
	 * {@code bicycle=no} is not a bicycle-specific <em>permission</em>, so it never
	 * lifts the general access restriction, and a private road nobody may enter has no
	 * business in the network under any mode.
	 */
	@Test
	void accessRestrictedAndBicycleNo_isDropped() {
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			tags("highway", "service", "access", "private", "bicycle", "no"),
			BicycleInfraCategory.NONE, NO_DROP_TYPES);

		assertTrue(v.dropped(), "access=private is not lifted by bicycle=no");
		assertEquals(BicycleAccessRules.DropReason.ACCESS_RESTRICTED, v.dropReason());
	}

	/**
	 * {@code highway=track} + {@code bicycle=no} under {@code --drop-ways-without-infra
	 * track}. The OSM path's early return after {@code bicycle=no} meant the minor-way
	 * rule never ran, so the track stayed; the SUMO path dropped it. A track that carries
	 * no infrastructure and forbids cycling is precisely what the option is for.
	 */
	@Test
	void minorWayWithBicycleNo_isDropped() {
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			tags("highway", "track", "bicycle", "no"),
			BicycleInfraCategory.NONE, DROPS_TRACKS);

		assertTrue(v.dropped(), "bicycle=no must not short-circuit the minor-way rule");
		assertEquals(BicycleAccessRules.DropReason.MINOR_WAY_WITHOUT_INFRA, v.dropReason());
		assertTrue(v.bikeForbidden(),
			"the bicycle=no is still reported, so a caller counting both sees it");
	}

	// =========================================================================
	// Rule order
	// =========================================================================

	/** A parking aisle goes no matter what else it is tagged with — the first rule wins. */
	@Test
	void parkingAisle_outranksEveryOtherRule() {
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			tags("highway", "service", "service", "parking_aisle",
				"access", "private", "bicycle", "designated"),
			BicycleInfraCategory.NONE, DROPS_TRACKS);

		assertEquals(BicycleAccessRules.DropReason.PARKING_AISLE, v.dropReason(),
			"parking_aisle is rule 1 and reports its own reason");
	}

	/**
	 * A footway with a restricted access reports the access, not the footway: the more
	 * specific counter would otherwise absorb cases that have nothing to do with foot
	 * traffic.
	 */
	@Test
	void restrictedAccess_outranksTheFootwayRule() {
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			tags("highway", "footway", "access", "private"),
			BicycleInfraCategory.NONE, NO_DROP_TYPES);

		assertEquals(BicycleAccessRules.DropReason.ACCESS_RESTRICTED, v.dropReason());
	}

	// =========================================================================
	// bicycle=no on its own
	// =========================================================================

	@Test
	void bicycleNo_forbidsTheBikeButKeepsTheLink() {
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			tags("highway", "primary", "bicycle", "no"),
			BicycleInfraCategory.NONE, NO_DROP_TYPES);

		assertTrue(v.bikeForbidden());
		assertFalse(v.dropped(), "a primary road stays open to cars");
		assertNull(v.dropReason());
	}

	@Test
	void plainWay_isKeptUntouched() {
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			tags("highway", "residential"), BicycleInfraCategory.NONE, DROPS_TRACKS);

		assertFalse(v.bikeForbidden());
		assertFalse(v.dropped());
	}

	// =========================================================================
	// The bicycle-specific override
	// =========================================================================

	@ParameterizedTest
	@ValueSource(strings = {"yes", "designated"})
	void bicyclePermission_liftsTheAccessRestriction(String bicycle) {
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			tags("highway", "service", "access", "private", "bicycle", bicycle),
			BicycleInfraCategory.NONE, NO_DROP_TYPES);

		assertFalse(v.dropped(), "bicycle=" + bicycle + " overrides access=private");
	}

	@ParameterizedTest
	@ValueSource(strings = {"yes", "designated"})
	void bicyclePermission_liftsTheFootwayRule(String bicycle) {
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			tags("highway", "footway", "bicycle", bicycle),
			BicycleInfraCategory.NONE, NO_DROP_TYPES);

		assertFalse(v.dropped());
	}

	/** {@code bicycle=no} is not a permission and must not lift anything. */
	@Test
	void bicycleNo_liftsNothing() {
		assertTrue(BicycleAccessRules.evaluate(tags("highway", "footway", "bicycle", "no"),
			BicycleInfraCategory.NONE, NO_DROP_TYPES).dropped());
	}

	// =========================================================================
	// The minor-way rule's two guards
	// =========================================================================

	@Test
	void minorWayThatClassified_isKept() {
		// A signposted cycle route over a track never reaches NONE, so the rule spares it.
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			tags("highway", "track"), BicycleInfraCategory.CYCLEWAY_ADJOINING, DROPS_TRACKS);

		assertFalse(v.dropped(), "the classification guard keeps a classified track");
	}

	@Test
	void minorWayWithoutTheOption_isKept() {
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			tags("highway", "track"), BicycleInfraCategory.NONE, NO_DROP_TYPES);

		assertFalse(v.dropped(), "an empty --drop-ways-without-infra switches the rule off");
	}

	@Test
	void wayWithoutHighwayTag_isNoMinorWay() {
		// No highway tag at all must not be read as "matches every configured type";
		// it also keeps the value out of a Set.of(), whose contains(null) throws.
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			tags("surface", "gravel"), BicycleInfraCategory.NONE, DROPS_TRACKS);

		assertFalse(v.dropped());
	}

	// =========================================================================
	// Folding over several ways -- the SUMO case
	// =========================================================================

	/** One offending way is enough: half a parking aisle is not a through route. */
	@Test
	void dropRules_fireWhenAnyWayOffends() {
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			List.of(tags("highway", "residential"),
				tags("highway", "service", "service", "parking_aisle")),
			BicycleInfraCategory.NONE, NO_DROP_TYPES);

		assertEquals(BicycleAccessRules.DropReason.PARKING_AISLE, v.dropReason());
	}

	/**
	 * ...but the minor-way rule wants every way to be minor. A link merged from a track
	 * and a residential road is half a real road, and discarding it on the strength of
	 * the track half would be wrong.
	 */
	@Test
	void minorWayRule_needsEveryWayToBeMinor() {
		BicycleAccessRules.Verdict mixed = BicycleAccessRules.evaluate(
			List.of(tags("highway", "track"), tags("highway", "residential")),
			BicycleInfraCategory.NONE, DROPS_TRACKS);
		assertFalse(mixed.dropped(), "one residential half keeps the merged link");

		BicycleAccessRules.Verdict allMinor = BicycleAccessRules.evaluate(
			List.of(tags("highway", "track"), tags("highway", "path")),
			BicycleInfraCategory.NONE, DROPS_TRACKS);
		assertTrue(allMinor.dropped(), "track + path are both listed, so the link goes");
	}

	/** A permission on any one of the merged ways spares the whole link. */
	@Test
	void minorWayRule_yieldsToAPermissionOnAnyWay() {
		BicycleAccessRules.Verdict v = BicycleAccessRules.evaluate(
			List.of(tags("highway", "track"), tags("highway", "track", "bicycle", "designated")),
			BicycleInfraCategory.NONE, DROPS_TRACKS);

		assertFalse(v.dropped());
	}

	/**
	 * A single way and a one-element list have to give the same answer — that identity is
	 * what lets the Supersonic path and the SUMO path share one implementation.
	 */
	@Test
	void singleWayOverload_matchesTheOneElementFold() {
		Map<String, String> tags = tags("highway", "track", "bicycle", "no");

		assertEquals(BicycleAccessRules.evaluate(List.of(tags), BicycleInfraCategory.NONE, DROPS_TRACKS),
			BicycleAccessRules.evaluate(tags, BicycleInfraCategory.NONE, DROPS_TRACKS));
	}

	// =========================================================================
	// helpers
	// =========================================================================

	private static Map<String, String> tags(String... kv) {
		Map<String, String> m = new HashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			m.put(kv[i], kv[i + 1]);
		}
		return m;
	}
}
