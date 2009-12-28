/* *********************************************************************** *
 * project: org.matsim.*
 * TransitConfigGroup.java
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

package playground.mrieser.pt.config;

import java.util.EnumSet;
import java.util.Set;

import junit.framework.TestCase;

import org.matsim.api.core.v01.TransportMode;

import playground.mrieser.pt.config.TransitConfigGroup;

/**
 * @author mrieser
 */
public class TransitConfigGroupTest extends TestCase {

	/**
	 * Test {@link TransitConfigGroup#setTransitScheduleFile(String)},
	 * {@link TransitConfigGroup#getTransitScheduleFile()},
	 * {@link TransitConfigGroup#addParam(String, String)},
	 * {@link TransitConfigGroup#getValue(String)} and
	 * {@link TransitConfigGroup#getParams()} for setting and getting
	 * the transit schedule input file.
	 */
	public void testTransitScheduleFile() {
		TransitConfigGroup cg = new TransitConfigGroup();
		// test initial value
		assertNull(cg.getTransitScheduleFile());
		assertNull(cg.getValue(TransitConfigGroup.TRANSIT_SCHEDULE_FILE));
		assertEquals("null", cg.getParams().get(TransitConfigGroup.TRANSIT_SCHEDULE_FILE));
		// set to non-null value
		String filename = "/path/to/some/file.xml";
		cg.setTransitScheduleFile(filename);
		assertEquals(filename, cg.getTransitScheduleFile());
		assertEquals(filename, cg.getValue(TransitConfigGroup.TRANSIT_SCHEDULE_FILE));
		assertEquals(filename, cg.getParams().get(TransitConfigGroup.TRANSIT_SCHEDULE_FILE));
		// overwrite value
		filename = "C:\\some\\other\\file.txt";
		cg.setTransitScheduleFile(filename);
		assertEquals(filename, cg.getTransitScheduleFile());
		assertEquals(filename, cg.getValue(TransitConfigGroup.TRANSIT_SCHEDULE_FILE));
		assertEquals(filename, cg.getParams().get(TransitConfigGroup.TRANSIT_SCHEDULE_FILE));
		// set to null value
		cg.setTransitScheduleFile(null);
		assertNull(cg.getTransitScheduleFile());
		assertNull(cg.getValue(TransitConfigGroup.TRANSIT_SCHEDULE_FILE));
		assertEquals("null", cg.getParams().get(TransitConfigGroup.TRANSIT_SCHEDULE_FILE));
		// test with addParam instead of setter
		cg.addParam(TransitConfigGroup.TRANSIT_SCHEDULE_FILE, filename);
		assertEquals(filename, cg.getTransitScheduleFile());
		assertEquals(filename, cg.getValue(TransitConfigGroup.TRANSIT_SCHEDULE_FILE));
		assertEquals(filename, cg.getParams().get(TransitConfigGroup.TRANSIT_SCHEDULE_FILE));
	}

	public void testVehiclesFile() {
		TransitConfigGroup cg = new TransitConfigGroup();
		// test initial value
		assertNull(cg.getVehiclesFile());
		assertNull(cg.getValue(TransitConfigGroup.VEHICLES_FILE));
		assertEquals("null", cg.getParams().get(TransitConfigGroup.VEHICLES_FILE));
		// set to non-null value
		String filename = "/path/to/some/file.xml";
		cg.setVehiclesFile(filename);
		assertEquals(filename, cg.getVehiclesFile());
		assertEquals(filename, cg.getValue(TransitConfigGroup.VEHICLES_FILE));
		assertEquals(filename, cg.getParams().get(TransitConfigGroup.VEHICLES_FILE));
		// overwrite value
		filename = "C:\\some\\other\\file.txt";
		cg.setVehiclesFile(filename);
		assertEquals(filename, cg.getVehiclesFile());
		assertEquals(filename, cg.getValue(TransitConfigGroup.VEHICLES_FILE));
		assertEquals(filename, cg.getParams().get(TransitConfigGroup.VEHICLES_FILE));
		// set to null value
		cg.setVehiclesFile(null);
		assertNull(cg.getTransitScheduleFile());
		assertNull(cg.getValue(TransitConfigGroup.VEHICLES_FILE));
		assertEquals("null", cg.getParams().get(TransitConfigGroup.VEHICLES_FILE));
		// test with addParam instead of setter
		cg.addParam(TransitConfigGroup.VEHICLES_FILE, filename);
		assertEquals(filename, cg.getVehiclesFile());
		assertEquals(filename, cg.getValue(TransitConfigGroup.VEHICLES_FILE));
		assertEquals(filename, cg.getParams().get(TransitConfigGroup.VEHICLES_FILE));
	}

	public void testTransitModes() {
		TransitConfigGroup cg = new TransitConfigGroup();
		Set<TransportMode> modes;
		// test initial value
		modes = cg.getTransitModes();
		assertEquals(4, modes.size());
		assertTrue(modes.contains(TransportMode.pt));
		assertTrue(modes.contains(TransportMode.bus));
		assertTrue(modes.contains(TransportMode.train));
		assertTrue(modes.contains(TransportMode.tram));
		assertEquals("pt,train,bus,tram", cg.getValue(TransitConfigGroup.TRANSIT_MODES));
		// test setting with setTransitModes
		cg.setTransitModes(EnumSet.of(TransportMode.bus, TransportMode.train));
		modes = cg.getTransitModes();
		assertEquals(2, modes.size());
		assertTrue(modes.contains(TransportMode.bus));
		assertTrue(modes.contains(TransportMode.train));
		assertEquals("train,bus", cg.getValue(TransitConfigGroup.TRANSIT_MODES));
		// test setting to none
		cg.setTransitModes(EnumSet.noneOf(TransportMode.class));
		modes = cg.getTransitModes();
		assertEquals(0, modes.size());
		assertEquals("", cg.getValue(TransitConfigGroup.TRANSIT_MODES));
		// test setting with addParam
		cg.addParam(TransitConfigGroup.TRANSIT_MODES, "tram,bus,train");
		modes = cg.getTransitModes();
		assertEquals(3, modes.size());
		assertTrue(modes.contains(TransportMode.bus));
		assertTrue(modes.contains(TransportMode.tram));
		assertTrue(modes.contains(TransportMode.train));
		assertEquals("train,bus,tram", cg.getValue(TransitConfigGroup.TRANSIT_MODES));
		// test setting to none
		cg.addParam(TransitConfigGroup.TRANSIT_MODES, "");
		modes = cg.getTransitModes();
		assertEquals(0, modes.size());
		assertEquals("", cg.getValue(TransitConfigGroup.TRANSIT_MODES));
		// test setting with non-conform formatting
		cg.addParam(TransitConfigGroup.TRANSIT_MODES, " tram, pt,bus ,\ttrain ");
		modes = cg.getTransitModes();
		assertEquals(4, modes.size());
		assertTrue(modes.contains(TransportMode.pt));
		assertTrue(modes.contains(TransportMode.bus));
		assertTrue(modes.contains(TransportMode.tram));
		assertTrue(modes.contains(TransportMode.train));
		assertEquals("pt,train,bus,tram", cg.getValue(TransitConfigGroup.TRANSIT_MODES));
		// test setting to non-conform none
		cg.addParam(TransitConfigGroup.TRANSIT_MODES, "  \t ");
		modes = cg.getTransitModes();
		assertEquals(0, modes.size());
		assertEquals("", cg.getValue(TransitConfigGroup.TRANSIT_MODES));
	}

}
