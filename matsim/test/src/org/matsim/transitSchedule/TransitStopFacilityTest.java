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

package org.matsim.transitSchedule;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser
 */
public class TransitStopFacilityTest extends MatsimTestCase {

	/**
	 * In case we once should have more than one implementation of
	 * {@link TransitStopFacility}, simply inherit from this test and overwrite
	 * this method to return your own implementation.
	 *
	 * @param id
	 * @param coord
	 * @return a new instance of a TransitStopFacility with the given attributes
	 */
	protected TransitStopFacility createTransitStopFacility(final Id id, final Coord coord, final boolean isBlockingLane) {
		return new TransitStopFacilityImpl(id, coord, isBlockingLane);
	}

	public void testInitialization() {
		Id id = new IdImpl(2491);
		Coord coord = new CoordImpl(30, 5);
		TransitStopFacility stop = createTransitStopFacility(id, coord, false);
		assertEquals(id.toString(), stop.getId().toString());
		assertEquals(coord.getX(), stop.getCoord().getX(), EPSILON);
		assertEquals(coord.getY(), stop.getCoord().getY(), EPSILON);
		assertFalse(stop.getIsBlockingLane());
	}

	public void testBlockingStop() {
		Id id = new IdImpl(2491);
		Coord coord = new CoordImpl(30, 5);
		TransitStopFacility stop = createTransitStopFacility(id, coord, false);
		assertFalse(stop.getIsBlockingLane());
		stop = createTransitStopFacility(id, coord, true);
		assertTrue(stop.getIsBlockingLane());
	}

	public void testLink() {
		Id id = new IdImpl(2491);
		Coord coord = new CoordImpl(30, 5);
		TransitStopFacility stop = createTransitStopFacility(id, coord, false);
		assertNull(stop.getLink());
		assertNull(stop.getLinkId());
		Link link = new FakeLink(new IdImpl(99), null, null);
		stop.setLink(link);
		assertEquals(link, stop.getLink());
		assertEquals(link.getId(), stop.getLinkId());
		stop.setLink(null);
		assertNull(stop.getLink());
		assertNull(stop.getLinkId());
	}
	
	public void testName() {
		Id id = new IdImpl(9791);
		Coord coord = new CoordImpl(10, 5);
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
