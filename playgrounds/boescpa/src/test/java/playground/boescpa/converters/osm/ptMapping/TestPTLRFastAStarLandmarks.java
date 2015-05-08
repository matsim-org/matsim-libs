/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm.ptMapping;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.testcases.MatsimTestUtils;
import playground.boescpa.lib.tools.networkModification.NetworkUtils;

import static org.junit.Assert.assertEquals;
import static playground.boescpa.converters.osm.scheduleCreator.hafasCreator.PtRouteFPLAN.BUS;
import static playground.boescpa.converters.osm.scheduleCreator.hafasCreator.PtRouteFPLAN.TRAM;

/**
 * Provide tests for PTLRFastAStarLandmarks.
 *
 * @author boescpa
 */
public class TestPTLRFastAStarLandmarks {

	Network network = null;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepareTests() {
		this.network = NetworkUtils.readNetwork(utils.getClassInputDirectory()+"ZurichCentre.xml");
	}

	@Test
	public void testBaseRouting() {
		PTLRouter router = new PTLRFastAStarLandmarksWeighting(this.network);
		Node fromNode = this.network.getNodes().get(Id.create(300391782l, Node.class));
		Node toNode = this.network.getNodes().get(Id.create(3116119759l, Node.class));
		LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromNode, toNode, "car", null);
		assertEquals("Base Route longer than 1 link.", 1, path.links.size());
		assertEquals("Base Route different from link 1.", Id.createLinkId(1), path.links.get(0).getId());
	}

	@Test
	public void testBaseRoutingBus() {
		PTLRouter router = new PTLRFastAStarLandmarksWeighting(this.network);
		Node fromNode = this.network.getNodes().get(Id.create(390486263l, Node.class));
		Node toNode = this.network.getNodes().get(Id.create(1630517605l, Node.class));
		LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromNode, toNode, BUS, null);
		assertEquals("Base Route not 4 links.", 4, path.links.size());
		assertEquals("Base Route not passing link 263.", Id.createLinkId(263), path.links.get(0).getId());
	}

	@Test
	public void testBaseRoutingTram() {
		PTLRouter router = new PTLRFastAStarLandmarksWeighting(this.network);
		Node fromNode = this.network.getNodes().get(Id.create(390486263l, Node.class));
		Node toNode = this.network.getNodes().get(Id.create(1630517605l, Node.class));
		LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromNode, toNode, TRAM, null);
		assertEquals("Base Route not 2 links.", 2, path.links.size());
		assertEquals("Base Route not passing link 228.", Id.createLinkId(228), path.links.get(0).getId());
	}


}
