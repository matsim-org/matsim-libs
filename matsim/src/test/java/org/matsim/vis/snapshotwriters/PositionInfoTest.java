/* *********************************************************************** *
 * project: org.matsim.*
 * PositionInfoTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.vis.snapshotwriters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

public class PositionInfoTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final double epsilon = 1e-8; // accuracy of double-comparisons

	/**
	 * Test the calculation of a vehicles position if the actual link length is
	 * shorter than the euklidean distance.
	 *
	 * @author mrieser
	 */
	@Test
	void testDistanceOnLink_shortLink() {

		Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(1000, 1000));
		Link link1 = NetworkUtils.createAndAddLink(network, Id.create("1", Link.class), node1, node2, 1000, 10, 9999, 1);

		// place the vehicle at one quarter of the link
		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();

		var posInfo = new PositionInfo.LinkBasedBuilder()
				.setLinkWidthCalculator(linkWidthCalculator)
				.setPersonId(Id.createPersonId(1))
				.setLinkId(link1.getId())
				.setFromCoord(link1.getFromNode().getCoord())
				.setToCoord(link1.getToNode().getCoord())
				.setLinkLength(link1.getLength())
				.setDistanceOnLink(250)
				.setLane(0)
				.build();


		//AgentSnapshotInfo posInfo = new AgentSnapshotInfoFactory(linkWidthCalculator).createAgentSnapshotInfo(Id.create(1, Person.class), link1, 250, 0);
		assertEquals(260.60660171779824, posInfo.getEasting(), epsilon);
		assertEquals(239.3933982822018, posInfo.getNorthing(), epsilon);
		// These numbers became a little weird when I moved vehicles away from the center of a link. Kai, Dec/08
		// These numbers changed again when I mad the orthogonal offset adaptive.  kai, aug/10
		// Made the orthogonal offsets non-adaptive again.  kai, feb'13
	}

	/**
	 * Test the calculation of a vehicles position if the actual link length is
	 * longer than the euklidean distance.
	 *
	 * @author mrieser
	 */
	@Test
	void testDistanceOnLink_longLink() {

        Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(1000, 1000));
		Link link1 = NetworkUtils.createAndAddLink(network, Id.create("1", Link.class), node1, node2, 2000, 10, 9999, 1);

		// place the vehicle at one quarter of the link
		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		var posInfo = new PositionInfo.LinkBasedBuilder()
				.setLinkWidthCalculator(linkWidthCalculator)
				.setPersonId(Id.createPersonId(1))
				.setLinkId(link1.getId())
				.setFromCoord(link1.getFromNode().getCoord())
				.setToCoord(link1.getToNode().getCoord())
				.setLinkLength(link1.getLength())
				.setDistanceOnLink(500)
				.setLane(0)
				.build();

		//AgentSnapshotInfo posInfo = new AgentSnapshotInfoFactory(linkWidthCalculator).createAgentSnapshotInfo(Id.create(1, Person.class), link1, 500, 0);
		assertEquals(260.60660171779824, posInfo.getEasting(), epsilon);
		assertEquals(239.3933982822018, posInfo.getNorthing(), epsilon);
		// These numbers became a little weird when I moved vehicles away from the center of a link. Kai, Dec/08
		// These numbers changed again when I mad the orthogonal offset adaptive.  kai, aug/10
		// Made the orthogonal offsets non-adaptive again.  kai, feb'13
	}

}
