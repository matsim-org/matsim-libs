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

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link OsmWayTags} against a hand-written OSM fixture.
 */
public class OsmWayTagsTest {

	private static final Path WAYS =
		Path.of("test/input/org/matsim/contrib/bicycle/network/OsmWayTagsTest/ways.osm");

	@Test
	void readsWhitelistedTagsPerWay() {

		OsmWayTags tags = OsmWayTags.read(WAYS);

		Map<String, String> road = tags.get(1001);
		assertEquals("residential", road.get(BicycleOsmTags.HIGHWAY));
		assertEquals("lane", road.get(BicycleOsmTags.CYCLEWAY_RIGHT));
		assertEquals("no", road.get(BicycleOsmTags.CYCLEWAY_LEFT));
		assertEquals("asphalt", road.get(BicycleOsmTags.SURFACE));

		Map<String, String> path = tags.get(3001);
		assertEquals("path", path.get(BicycleOsmTags.HIGHWAY));
		assertEquals("designated", path.get(BicycleOsmTags.BICYCLE));
		assertEquals("designated", path.get(BicycleOsmTags.FOOT));
		assertEquals("no", path.get(BicycleOsmTags.SEGREGATED));
		assertEquals("yes", path.get(BicycleOsmTags.IS_SIDEPATH));
		assertEquals("DE:240", path.get(BicycleOsmTags.TRAFFIC_SIGN));
	}

	@Test
	void discardsTagsOutsideTheWhitelist() {

		OsmWayTags tags = OsmWayTags.read(WAYS);

		// name and maxspeed are on way 1001 in the fixture but not classification-relevant
		assertNull(tags.get(1001).get("name"));
		assertNull(tags.get(1001).get("maxspeed"));

		// a way whose every tag is outside the whitelist is not stored at all
		assertFalse(tags.contains(2001), "Way without a whitelisted tag must not be stored");
		assertTrue(tags.get(2001).isEmpty(), "An unknown way yields an empty map, not null");
	}

	@Test
	void ignoresTagsOnNodesAndRelations() {

		OsmWayTags tags = OsmWayTags.read(WAYS);

		// the fixture's relation carries highway=motorway + bicycle=no, its nodes carry
		// highway=traffic_signals - none of that may show up as a way
		assertFalse(tags.contains(9001), "Relation must not be read as a way");
		assertFalse(tags.contains(1), "Node must not be read as a way");

		// and it must not have bled into the ways either
		assertEquals("residential", tags.get(1001).get(BicycleOsmTags.HIGHWAY));
		assertNull(tags.get(1001).get(BicycleOsmTags.BICYCLE));
	}

	@Test
	void honoursACustomKeySet() {

		// an area marker is not part of the default whitelist, so callers have to add it
		assertFalse(BicycleOsmTags.classificationKeys().contains("city_center"));
		assertNull(OsmWayTags.read(WAYS).get(4001).get("city_center"));

		Set<String> keys = Set.of(BicycleOsmTags.HIGHWAY, "city_center");
		OsmWayTags tags = OsmWayTags.read(WAYS, keys);

		assertEquals("yes", tags.get(4001).get("city_center"));
		assertEquals("cycleway", tags.get(4001).get(BicycleOsmTags.HIGHWAY));

		// keys outside the custom set are gone, even the ones the default would keep
		assertNull(tags.get(1001).get(BicycleOsmTags.SURFACE));
		assertEquals(1, tags.get(3001).size(), "Only highway survives the custom key set");
	}

	@Test
	void countsOnlyStoredWays() {

		// 1001, 3001 and 4001 carry a whitelisted tag; 2001 does not
		assertEquals(3, OsmWayTags.read(WAYS).size());
	}
}
