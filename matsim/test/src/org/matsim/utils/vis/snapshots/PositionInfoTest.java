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

package org.matsim.utils.vis.snapshots;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;

public class PositionInfoTest extends MatsimTestCase {

	private static final double epsilon = 1e-8; // accuracy of double-comparisons

	/**
	 * Test the calculation of a vehicles position if the actual link length is
	 * shorter than the euklidean distance.
	 *
	 * @author mrieser
	 */
	public void testDistanceOnLink_shortLink() {

		NetworkLayer network = new NetworkLayer();
		Gbl.getWorld().setNetworkLayer(network);
		network.createNode("1", "0", "0", null);
		network.createNode("2", "1000", "1000", null);
		Link link1 = network.createLink("1", "1", "2", "1000", "10", "9999", "1", null, null);

		// place the vehicle at one quarter of the link
		PositionInfo posInfo = new PositionInfo(new Id(1), link1, 250, 0, 10, PositionInfo.VehicleState.Driving, null);
		assertEquals(250.0, posInfo.getEasting(), epsilon);
		assertEquals(250.0, posInfo.getNorthing(), epsilon);
	}

	/**
	 * Test the calculation of a vehicles position if the actual link length is
	 * longer than the euklidean distance.
	 *
	 * @author mrieser
	 */
	public void testDistanceOnLink_longLink() {

		NetworkLayer network = new NetworkLayer();
		Gbl.getWorld().setNetworkLayer(network);
		network.createNode("1", "0", "0", null);
		network.createNode("2", "1000", "1000", null);
		Link link1 = network.createLink("1", "1", "2", "2000", "10", "9999", "1", null, null);

		// place the vehicle at one quarter of the link
		PositionInfo posInfo = new PositionInfo(new Id(1), link1, 500, 0, 10, PositionInfo.VehicleState.Driving, null);
		assertEquals(250.0, posInfo.getEasting(), epsilon);
		assertEquals(250.0, posInfo.getNorthing(), epsilon);
	}

}
