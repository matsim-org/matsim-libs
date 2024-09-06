/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleTest.java
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser
 */
public class TransitScheduleTest {

	private static final Logger log = LogManager.getLogger(TransitScheduleTest.class);

	@Test
	void testInitialization() {
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = new TransitScheduleImpl(builder);
		assertEquals(builder, schedule.getFactory());
	}

	@Test
	void testAddTransitLine() {
		TransitSchedule schedule = new TransitScheduleImpl(new TransitScheduleFactoryImpl());
		TransitLine line1 = new TransitLineImpl(Id.create(1, TransitLine.class));
		TransitLine line2 = new TransitLineImpl(Id.create(2, TransitLine.class));
		assertEquals(0, schedule.getTransitLines().size());
		schedule.addTransitLine(line1);
		assertEquals(1, schedule.getTransitLines().size());
		assertEquals(line1, schedule.getTransitLines().get(line1.getId()));
		schedule.addTransitLine(line2);
		assertEquals(2, schedule.getTransitLines().size());
		assertEquals(line1, schedule.getTransitLines().get(line1.getId()));
		assertEquals(line2, schedule.getTransitLines().get(line2.getId()));
	}

	@Test
	void testAddTransitLineException() {
		TransitSchedule schedule = new TransitScheduleImpl(new TransitScheduleFactoryImpl());
		TransitLine line1a = new TransitLineImpl(Id.create(1, TransitLine.class));
		TransitLine line1b = new TransitLineImpl(Id.create(1, TransitLine.class));
		assertEquals(0, schedule.getTransitLines().size());
		schedule.addTransitLine(line1a);
		assertEquals(1, schedule.getTransitLines().size());
		assertEquals(line1a, schedule.getTransitLines().get(line1a.getId()));
		try { // try to add a line with same id
			schedule.addTransitLine(line1b);
			fail("missing exception.");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception.", e);
		}
		assertEquals(1, schedule.getTransitLines().size());
		assertEquals(line1a, schedule.getTransitLines().get(line1a.getId()));
		try { // try to add a line a second time
			schedule.addTransitLine(line1a);
			fail("missing exception.");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception.", e);
		}
		assertEquals(1, schedule.getTransitLines().size());
		assertEquals(line1a, schedule.getTransitLines().get(line1a.getId()));
	}

	@Test
	void testAddStopFacility() {
		TransitSchedule schedule = new TransitScheduleImpl(new TransitScheduleFactoryImpl());
		TransitStopFacility stop1 = new TransitStopFacilityImpl(Id.create(1, TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		TransitStopFacility stop2 = new TransitStopFacilityImpl(Id.create(2, TransitStopFacility.class), new Coord((double) 1, (double) 1), false);
		assertEquals(0, schedule.getFacilities().size());
		schedule.addStopFacility(stop1);
		assertEquals(1, schedule.getFacilities().size());
		assertEquals(stop1, schedule.getFacilities().get(stop1.getId()));
		schedule.addStopFacility(stop2);
		assertEquals(2, schedule.getFacilities().size());
		assertEquals(stop1, schedule.getFacilities().get(stop1.getId()));
		assertEquals(stop2, schedule.getFacilities().get(stop2.getId()));
	}

	@Test
	void testAddStopFacilityException() {
		TransitSchedule schedule = new TransitScheduleImpl(new TransitScheduleFactoryImpl());
		TransitStopFacility stop1a = new TransitStopFacilityImpl(Id.create(1, TransitStopFacility.class), new Coord((double) 2, (double) 2), false);
		TransitStopFacility stop1b = new TransitStopFacilityImpl(Id.create(1, TransitStopFacility.class), new Coord((double) 3, (double) 3), false);
		assertEquals(0, schedule.getFacilities().size());
		schedule.addStopFacility(stop1a);
		assertEquals(1, schedule.getFacilities().size());
		assertEquals(stop1a, schedule.getFacilities().get(stop1a.getId()));
		try { // try to add a line with same id
			schedule.addStopFacility(stop1b);
			fail("missing exception.");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception.", e);
		}
		assertEquals(1, schedule.getFacilities().size());
		assertEquals(stop1a, schedule.getFacilities().get(stop1a.getId()));
		try { // try to add a line a second time
			schedule.addStopFacility(stop1a);
			fail("missing exception.");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception.", e);
		}
		assertEquals(1, schedule.getFacilities().size());
		assertEquals(stop1a, schedule.getFacilities().get(stop1a.getId()));
	}

	@Test
	void testGetTransitLinesImmutable() {
		TransitSchedule schedule = new TransitScheduleImpl(new TransitScheduleFactoryImpl());
		TransitLine line1 = new TransitLineImpl(Id.create(1, TransitLine.class));
		try {
			schedule.getTransitLines().put(line1.getId(), line1);
			fail("missing exception.");
		}
		catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}
	}

	@Test
	void testGetFacilitiesImmutable() {
		TransitSchedule schedule = new TransitScheduleImpl(new TransitScheduleFactoryImpl());
		TransitStopFacility stop1 = new TransitStopFacilityImpl(Id.create(1, TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		try {
			schedule.getFacilities().put(stop1.getId(), stop1);
			fail("missing exception.");
		}
		catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}
	}

	@Test
	void testRemoveStopFacility() {
		TransitSchedule schedule = new TransitScheduleImpl(new TransitScheduleFactoryImpl());
		TransitStopFacility stop1 = new TransitStopFacilityImpl(Id.create(1, TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		TransitStopFacility stop1b = new TransitStopFacilityImpl(Id.create(1, TransitStopFacility.class), new Coord((double) 10, (double) 10), false);
		schedule.addStopFacility(stop1);
		Assertions.assertFalse(schedule.removeStopFacility(stop1b));
		Assertions.assertTrue(schedule.removeStopFacility(stop1));
		Assertions.assertFalse(schedule.removeStopFacility(stop1));
	}

	@Test
	void testRemoveTransitLine() {
		TransitSchedule schedule = new TransitScheduleImpl(new TransitScheduleFactoryImpl());
		TransitLine line1 = new TransitLineImpl(Id.create(1, TransitLine.class));
		TransitLine line1b = new TransitLineImpl(Id.create(1, TransitLine.class));
		schedule.addTransitLine(line1);
		Assertions.assertFalse(schedule.removeTransitLine(line1b));
		Assertions.assertTrue(schedule.removeTransitLine(line1));
		Assertions.assertFalse(schedule.removeTransitLine(line1));
	}
	
}
