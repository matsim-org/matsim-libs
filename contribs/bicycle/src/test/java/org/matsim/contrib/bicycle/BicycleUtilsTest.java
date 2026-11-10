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
package org.matsim.contrib.bicycle;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the {@code osm:} fallback of the attribute getters: the OSM reader writes the plain
 * keys ({@code surface}), the network tools in {@code org.matsim.contrib.bicycle.network}
 * write them prefixed ({@code osm:surface}) — scoring has to see both, or a network from
 * the latter silently scores with default factors.
 */
public class BicycleUtilsTest {

	private static Link link() {
		Network network = NetworkUtils.createNetwork();
		Node a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
		Node b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(100, 0));
		return NetworkUtils.createAndAddLink(network, Id.createLinkId("ab"), a, b, 100, 10, 300, 1);
	}

	@Test
	void getSurfaceReadsThePlainKey() {
		Link link = link();
		link.getAttributes().putAttribute(BicycleUtils.SURFACE, "asphalt");

		assertEquals("asphalt", BicycleUtils.getSurface(link));
	}

	@Test
	void getSurfaceFallsBackToThePrefixedKey() {
		Link link = link();
		link.getAttributes().putAttribute(BicycleUtils.OSM_PREFIX + BicycleUtils.SURFACE, "sett");

		assertEquals("sett", BicycleUtils.getSurface(link));
	}

	@Test
	void thePlainKeyWinsOverThePrefixedOne() {
		Link link = link();
		link.getAttributes().putAttribute(BicycleUtils.SURFACE, "asphalt");
		link.getAttributes().putAttribute(BicycleUtils.OSM_PREFIX + BicycleUtils.SURFACE, "sett");

		assertEquals("asphalt", BicycleUtils.getSurface(link));
	}

	@Test
	void getSurfaceIsNullWithoutEitherKey() {
		assertNull(BicycleUtils.getSurface(link()));
	}

	@Test
	void getCyclewaytypeFallsBackToThePrefixedKey() {
		Link link = link();
		link.getAttributes().putAttribute(BicycleUtils.OSM_PREFIX + BicycleUtils.CYCLEWAY, "lane");

		assertEquals("lane", BicycleUtils.getCyclewaytype(link));
	}

	/**
	 * The surface factor of the default speed model reads through the getter, so a
	 * prefixed surface slows the link down instead of being invisible.
	 */
	@Test
	void computeSurfaceFactorSeesThePrefixedSurface() {
		Link link = link();
		link.getAttributes().putAttribute("type", "residential");
		link.getAttributes().putAttribute(BicycleUtils.OSM_PREFIX + BicycleUtils.SURFACE, "sand");

		assertEquals(0.2, new BicycleParamsDefaultImpl().computeSurfaceFactor(link), 1e-9);
	}

	/**
	 * Pins the on-disk attribute names against accidental renames: snake_case, matching
	 * what network-from-sumo writes on the same networks (allowed_speed, restricted_lanes).
	 * Written as literals on purpose — through the constants this would be a tautology.
	 */
	@Test
	void networkAttributeKeysAreSnakeCase() {
		Link link = link();
		link.getAttributes().putAttribute("bicycle_infra", "NONE");
		link.getAttributes().putAttribute("average_elevation", 110.0);
		link.getAttributes().putAttribute("gradient", 0.03);
		link.getAttributes().putAttribute("max_gradient", 0.08);
		link.getAttributes().putAttribute("elevation_gain", 5.0);
		link.getAttributes().putAttribute("elevation_loss", 2.0);
		link.getAttributes().putAttribute("car_link", "ab");
		link.getAttributes().putAttribute("bike_link", "ab_bike");

		assertEquals("NONE", BicycleUtils.getBicycleInfra(link));
		assertEquals(110.0, BicycleUtils.getAverageElevation(link));
		assertEquals(0.03, BicycleUtils.getGradient(link));
		assertEquals(0.08, BicycleUtils.getMaxGradient(link));
		assertEquals(5.0, BicycleUtils.getElevationGain(link));
		assertEquals(2.0, BicycleUtils.getElevationLoss(link));
		assertEquals("ab", BicycleUtils.getCarLink(link));
		assertEquals("ab_bike", BicycleUtils.getBikeLink(link));
	}

	@Test
	void elevationGettersAreNullWithoutMetrics() {
		assertNull(BicycleUtils.getGradient(link()));
		assertNull(BicycleUtils.getBicycleInfra(link()));
	}
}
