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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.bicycle.network.LinkElevationProfile.ElevationSource;
import org.matsim.contrib.bicycle.network.LinkElevationProfile.Kpis;
import org.matsim.core.network.NetworkUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LinkElevationProfile}. These tests use synthetic
 * {@link ElevationSource} lambdas instead of a real DEM, so they are
 * fast, deterministic, and require no external files.
 *
 * <p>Coverage:
 * <ul>
 *   <li>Flat link → all KPIs zero / equal endpoints</li>
 *   <li>Monotonically rising link → positive gradient, gain &gt; 0, loss = 0</li>
 *   <li>Hill between equal-height endpoints → gradient = 0 but maxGradient &gt; 0</li>
 *   <li>Douglas-Peucker filter: spike below tolerance is dropped, real hill above
 *       tolerance is kept</li>
 *   <li>Reverse direction has flipped signs</li>
 * </ul>
 */
public class LinkElevationProfileTest {

	private static final double EPS = 1e-6;

	private static final double SAMPLE_STEP = 10.0;
	private static final double TOLERANCE = 0.5;


	// =========================================================================
	// 1. Flat link
	// =========================================================================

	@Test
	void flatLink_allKpisZero() {
		Link link = createLink(0, 0, 100);
		ElevationSource flat = c -> 50.0;

		Kpis k = LinkElevationProfile.compute(link, SAMPLE_STEP, TOLERANCE, flat);

		assertEquals(50.0, k.averageElevation(), EPS);
		assertEquals(0.0, k.gradient(), EPS);
		assertEquals(0.0, k.maxGradient(), EPS);
		assertEquals(0.0, k.elevationGain(), EPS);
		assertEquals(0.0, k.elevationLoss(), EPS);
	}


	// =========================================================================
	// 2. Monotonically rising link
	// =========================================================================

	@Test
	void monotonicallyRisingLink_positiveGradientAndGain() {
		// 100 m horizontal, elevation 0 → 10 m, linear: each meter = +0.1 m height
		Link link = createLink(0, 0, 100);
		ElevationSource rising = c -> c.getX() * 0.1;

		Kpis k = LinkElevationProfile.compute(link, SAMPLE_STEP, TOLERANCE, rising);

		// mean gradient = (10 - 0) / 100 = 0.1 = 10 %
		assertEquals(0.1, k.gradient(), EPS);
		assertTrue(k.elevationGain() > 0, "gain must be positive on a rising link");
		assertEquals(0.0, k.elevationLoss(), EPS, "no descent on a monotonically rising link");
		assertTrue(k.maxGradient() > 0, "maxGradient must be positive on a rising link");
		assertEquals(5.0, k.averageElevation(), 1.0, "mean elevation roughly half the climb");
	}


	// =========================================================================
	// 3. Hill between equal-height endpoints
	// =========================================================================

	@Test
	void hillBetweenEqualEndpoints_meanGradientZeroButMaxNotZero() {
		// 100 m horizontal, both endpoints at z = 0, peak at x = 50 with z = 10
		// (triangular hill)
		Link link = createLink(0, 0, 100);
		ElevationSource hill = c -> {
			double x = c.getX();
			return x <= 50.0 ? x * 0.2 : (100.0 - x) * 0.2;   // 0..10..0
		};

		Kpis k = LinkElevationProfile.compute(link, SAMPLE_STEP, TOLERANCE, hill);

		// Endpoints same → end-to-end gradient must be exactly 0
		assertEquals(0.0, k.gradient(), EPS, "endpoints have same elevation");

		// But there's a real hill in between: gain ≈ loss ≈ 10 m
		assertTrue(k.elevationGain() > 5.0,
			"gain should reflect the climb to the peak, was " + k.elevationGain());
		assertTrue(k.elevationLoss() > 5.0,
			"loss should reflect the descent from the peak, was " + k.elevationLoss());
		assertEquals(k.elevationGain(), k.elevationLoss(), 1.0,
			"symmetric triangle → gain ≈ loss");

		// maxGradient must capture the slope of one of the two halves: ±0.2 = ±20 %
		assertTrue(Math.abs(k.maxGradient()) > 0.05,
			"maxGradient should reflect the steep climb/descent on the hill");
	}


	// =========================================================================
	// 4. Douglas-Peucker behavior
	// =========================================================================

	@Test
	void douglasPeucker_dropsSubToleranceSpike() {
		// 100 m link, otherwise flat at z = 50, but a tiny spike at the middle
		// well below tolerance (TOLERANCE = 0.5 m, spike = 0.2 m)
		Link link = createLink(0, 0, 100);
		ElevationSource spike = c -> {
			double x = c.getX();
			return Math.abs(x - 50.0) < 5.0 ? 50.2 : 50.0;
		};

		Kpis k = LinkElevationProfile.compute(link, SAMPLE_STEP, TOLERANCE, spike);

		// With DP filtering at 0.5 m tolerance, the 0.2 m spike must be dropped.
		// Endpoint elevations (50, 50) are always kept → gradient = 0.
		assertEquals(0.0, k.gradient(), EPS);
		assertEquals(0.0, k.elevationGain(), EPS, "spike should have been filtered");
		assertEquals(0.0, k.elevationLoss(), EPS, "spike should have been filtered");
	}

	@Test
	void douglasPeucker_keepsAboveToleranceHill() {
		// Same link, but the hill in the middle is 5 m -- well above the 0.5 m
		// DP tolerance, so it must be kept.
		Link link = createLink(0, 0, 100);
		ElevationSource hill = c -> {
			double x = c.getX();
			return Math.abs(x - 50.0) < 10.0 ? 55.0 : 50.0;
		};

		Kpis k = LinkElevationProfile.compute(link, SAMPLE_STEP, TOLERANCE, hill);

		assertTrue(k.elevationGain() > 1.0,
			"5 m hill above tolerance must be kept, gain was " + k.elevationGain());
		assertTrue(k.elevationLoss() > 1.0,
			"5 m hill above tolerance must produce a descent, loss was " + k.elevationLoss());
	}


	// =========================================================================
	// 5. Direction sensitivity
	// =========================================================================

	@Test
	void reverseLink_hasFlippedSigns() {
		// Forward link: 0 → 10 m over 100 m
		Link forward = createLink(0, 0, 100);
		// Reverse link: 100 → 0 over 100 m, height profile mirrored
		Link reverse = createReverseLink(0, 0, 100);

		ElevationSource rising = c -> c.getX() * 0.1;

		Kpis kf = LinkElevationProfile.compute(forward, SAMPLE_STEP, TOLERANCE, rising);
		Kpis kr = LinkElevationProfile.compute(reverse, SAMPLE_STEP, TOLERANCE, rising);

		assertEquals(kf.gradient(), -kr.gradient(), EPS,
			"reverse link has opposite-sign mean gradient");
		assertEquals(kf.elevationGain(), kr.elevationLoss(), EPS,
			"forward gain becomes reverse loss");
		assertEquals(kf.elevationLoss(), kr.elevationGain(), EPS,
			"forward loss becomes reverse gain");
	}


	// =========================================================================
	// 6. Endpoint Z is honored
	// =========================================================================

	@Test
	void endpointsWithZ_pinFirstAndLastSamplesToZ() {
		// Link with explicit Z values that disagree with the elevation source --
		// the profile must pin its first and last sample to the link's node Z values.
		Network net = NetworkUtils.createNetwork();
		Node from = net.getFactory().createNode(Id.createNodeId("from"), new Coord(0, 0, 100.0));
		Node to = net.getFactory().createNode(Id.createNodeId("to"), new Coord(100, 0, 100.0));
		net.addNode(from);
		net.addNode(to);
		Link link = NetworkUtils.createAndAddLink(net, Id.createLinkId("l"), from, to,
			100.0, 1000.0, 1000.0, 1.0);

		// Lying source: claims the endpoints are at 0 m, but the nodes carry Z=100.
		// The profile should override the endpoints with the node Z values.
		ElevationSource liesAtEndpoints = c -> 0.0;

		Kpis k = LinkElevationProfile.compute(link, SAMPLE_STEP, TOLERANCE, liesAtEndpoints);

		// Mean gradient is computed from endpoint heights: both pinned to 100.
		assertEquals(0.0, k.gradient(), EPS,
			"both endpoints pinned to 100 m → end-to-end gradient is zero");
	}


	// =========================================================================
	// helpers
	// =========================================================================

	/** Forward link, length 100 m on the X axis from (x0, y0) to (x0 + length, y0). */
	private static Link createLink(double x0, double y0, double length) {
		Network net = NetworkUtils.createNetwork();
		Node from = net.getFactory().createNode(Id.createNodeId("from"), new Coord(x0, y0));
		Node to = net.getFactory().createNode(Id.createNodeId("to"), new Coord(x0 + length, y0));
		net.addNode(from);
		net.addNode(to);
		return NetworkUtils.createAndAddLink(net, Id.createLinkId("l"), from, to,
			length, 1000.0, 1000.0, 1.0);
	}

	/** Reverse direction of {@link #createLink}. */
	private static Link createReverseLink(double x0, double y0, double length) {
		Network net = NetworkUtils.createNetwork();
		Node from = net.getFactory().createNode(Id.createNodeId("from"), new Coord(x0 + length, y0));
		Node to = net.getFactory().createNode(Id.createNodeId("to"), new Coord(x0, y0));
		net.addNode(from);
		net.addNode(to);
		return NetworkUtils.createAndAddLink(net, Id.createLinkId("l"), from, to,
			length, 1000.0, 1000.0, 1.0);
	}
}
