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
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sumo.SumoNetworkConverter;
import org.matsim.contrib.sumo.SumoNetworkHandler;
import org.matsim.core.network.NetworkUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link SumoBicycleKeepEdges#collect} against a netconvert fixture built
 * <em>without</em> {@code geometry.remove} — the state the first pass produces.
 *
 * <p>The fixture is a straight chain of five residential ways whose SUMO attributes are
 * identical, so netconvert would merge all of them. Only the bicycle tags differ, and
 * they differ in three deliberately different ways: not at all, on one side only, and in
 * the surface. See {@code chain.osm}.
 */
public class SumoBicycleKeepEdgesTest {

	private static final Path DIR =
		Path.of("test/input/org/matsim/contrib/bicycle/network/SumoBicycleKeepEdgesTest");

	private static SumoBicycleKeepEdges.Result run() throws Exception {
		Network network = NetworkUtils.createNetwork();
		SumoNetworkConverter converter = SumoNetworkConverter.newInstance(
			List.of(DIR.resolve("chain.net.xml")), Files.createTempFile("net", ".xml"),
			"EPSG:25832", "EPSG:25832");
		SumoNetworkHandler sumo = converter.convert(network);
		return SumoBicycleKeepEdges.collect(network, sumo, OsmWayTags.read(DIR.resolve("chain.osm")), "de");
	}

	@Test
	void reportsExactlyTheInfrastructureBoundaries() throws Exception {

		SumoBicycleKeepEdges.Result result = run();

		// 2001|3001 forward (a bike lane starts), 4001|5001 and -5001|-4001 (the surface changes)
		assertEquals(List.of("-4001", "-5001", "2001", "3001", "4001", "5001"), result.edgeIds());
		assertEquals(3, result.boundaries());
	}

	/**
	 * The point of comparing directed links rather than junctions: way 3001 carries
	 * {@code cycleway:right=lane}, so travelling along the chain the lane appears at node
	 * 3 — but travelling against it, the relevant side is {@code cycleway:left}, which is
	 * unset on both ways. There is no boundary in that direction, and claiming one would
	 * keep an edge apart for nothing.
	 */
	@Test
	void treatsTheTwoDirectionsSeparately() throws Exception {

		List<String> keep = run().edgeIds();

		assertTrue(keep.contains("2001"), "the lane appears here when travelling along the way");
		assertTrue(keep.contains("3001"));

		assertFalse(keep.contains("-2001"), "against the way both sides are unset, so nothing changes");
		assertFalse(keep.contains("-3001"));
	}

	@Test
	void ignoresNeighboursThatAgree() throws Exception {

		List<String> keep = run().edgeIds();

		// 1001 and 2001 are tagged identically, so their shared node may be dissolved
		assertFalse(keep.contains("1001"));
		assertFalse(keep.contains("-1001"));

		// 3001 and 4001 agree as well; 3001 is only listed because of its other end
		assertFalse(keep.contains("-3001"));
	}

	@Test
	void countsWhatItLookedAt() throws Exception {

		SumoBicycleKeepEdges.Result result = run();

		assertEquals(10, result.linksChecked(), "five ways, both directions");
		// the two links ending at a dead end have no continuation but the turnaround
		assertEquals(8, result.mergeCandidates());
		assertEquals(0, result.linksWithoutClassification());
		assertEquals(0, result.multiWayLinks(), "this fixture was built without geometry.remove");
	}

	@Test
	void writesIdsVerbatimAndSorted() throws Exception {

		List<String> keep = run().edgeIds();

		// netconvert matches these against its own edge ids, so the sign and any '#'
		// suffix have to survive untouched
		assertTrue(keep.contains("-4001"), "the leading minus is part of the id");
		assertEquals(keep.stream().sorted().toList(), keep, "sorted, so two runs agree byte for byte");
		assertEquals(keep.size(), keep.stream().distinct().count(), "no duplicates");
	}
}
