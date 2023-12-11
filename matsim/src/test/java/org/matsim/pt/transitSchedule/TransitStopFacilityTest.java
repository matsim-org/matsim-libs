/* *********************************************************************** *
 * project: org.matsim.*
 * TransitStopFacilityTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.transitSchedule;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.fakes.FakeLink;

/**
 * @author mrieser
 */
public class TransitStopFacilityTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * In case we once should have more than one implementation of
	 * {@link TransitStopFacility}, simply inherit from this test and overwrite
	 * this method to return your own implementation.
	 *
	 * @param id
	 * @param coord
	 * @return a new instance of a TransitStopFacility with the given attributes
	 */
	protected TransitStopFacility createTransitStopFacility(final Id<TransitStopFacility> id, final Coord coord, final boolean isBlockingLane) {
		return new TransitStopFacilityImpl(id, coord, isBlockingLane);
	}

	@Test
	void testInitialization() {
		Id<TransitStopFacility> id = Id.create(2491, TransitStopFacility.class);
		Coord coord = new Coord((double) 30, (double) 5);
		TransitStopFacility stop = createTransitStopFacility(id, coord, false);
		assertEquals(id.toString(), stop.getId().toString());
		assertEquals(coord.getX(), stop.getCoord().getX(), MatsimTestUtils.EPSILON);
		assertEquals(coord.getY(), stop.getCoord().getY(), MatsimTestUtils.EPSILON);
		assertFalse(stop.getIsBlockingLane());
	}

	@Test
	void testBlockingStop() {
		Id<TransitStopFacility> id = Id.create(2491, TransitStopFacility.class);
		Coord coord = new Coord((double) 30, (double) 5);
		TransitStopFacility stop = createTransitStopFacility(id, coord, false);
		assertFalse(stop.getIsBlockingLane());
		stop = createTransitStopFacility(id, coord, true);
		assertTrue(stop.getIsBlockingLane());
	}

	@Test
	void testLink() {
		Id<TransitStopFacility> id = Id.create(2491, TransitStopFacility.class);
		Coord coord = new Coord((double) 30, (double) 5);
		TransitStopFacility stop = createTransitStopFacility(id, coord, false);
		assertNull(stop.getLinkId());
		Link link = new FakeLink(Id.create(99, Link.class), null, null);
		stop.setLinkId(link.getId());
		assertEquals(link.getId(), stop.getLinkId());
		stop.setLinkId(null);
		assertNull(stop.getLinkId());
	}

	@Test
	void testName() {
		Id<TransitStopFacility> id = Id.create(9791, TransitStopFacility.class);
		Coord coord = new Coord((double) 10, (double) 5);
		TransitStopFacility stop = createTransitStopFacility(id, coord, false);
		assertNull(stop.getName());
		String name = "just some name.";
		stop.setName(name);
		assertEquals(name, stop.getName());
		name += " updated.";
		stop.setName(name);
		assertEquals(name, stop.getName());
		stop.setName(null);
		assertNull(stop.getName());
	}
}
