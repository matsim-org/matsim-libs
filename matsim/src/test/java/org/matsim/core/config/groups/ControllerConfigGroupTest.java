/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerConfigGroupTest.java
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

package org.matsim.core.config.groups;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.core.config.groups.ControllerConfigGroup.EventsFileFormat;

public class ControllerConfigGroupTest {

	/**
	 * Ensure that the events-file-format is correctly stored
	 * and returned with the different setters and getters.
	 *
	 * @author mrieser
	 */
	@Test
	void testEventsFileFormat() {
		ControllerConfigGroup cg = new ControllerConfigGroup();
		Set<EventsFileFormat> formats;
		// test initial value
		formats = cg.getEventsFileFormats();
		Assertions.assertEquals(1, formats.size());
		Assertions.assertTrue(formats.contains(EventsFileFormat.xml));
		Assertions.assertEquals("xml", cg.getValue(ControllerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting with setEventsFileFormat
		cg.setEventsFileFormats(EnumSet.of(EventsFileFormat.xml));
		formats = cg.getEventsFileFormats();
		Assertions.assertEquals(1, formats.size());
		Assertions.assertTrue(formats.contains(EventsFileFormat.xml));
		Assertions.assertEquals("xml", cg.getValue(ControllerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting to none
		cg.setEventsFileFormats(EnumSet.noneOf(EventsFileFormat.class));
		formats = cg.getEventsFileFormats();
		Assertions.assertEquals(0, formats.size());
		Assertions.assertEquals("", cg.getValue(ControllerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting with addParam
		cg.addParam(ControllerConfigGroup.EVENTS_FILE_FORMAT, "xml");
		formats = cg.getEventsFileFormats();
		Assertions.assertEquals(1, formats.size());
		Assertions.assertTrue(formats.contains(EventsFileFormat.xml));
		Assertions.assertEquals("xml", cg.getValue(ControllerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting to none
		cg.addParam(ControllerConfigGroup.EVENTS_FILE_FORMAT, "");
		formats = cg.getEventsFileFormats();
		Assertions.assertEquals(0, formats.size());
		Assertions.assertEquals("", cg.getValue(ControllerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting with non-conform formatting
		cg.addParam(ControllerConfigGroup.EVENTS_FILE_FORMAT, " xml\t\t  ");
		formats = cg.getEventsFileFormats();
		Assertions.assertEquals(1, formats.size());
		Assertions.assertTrue(formats.contains(EventsFileFormat.xml));
		Assertions.assertEquals("xml", cg.getValue(ControllerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting to non-conform none
		cg.addParam(ControllerConfigGroup.EVENTS_FILE_FORMAT, "  \t ");
		formats = cg.getEventsFileFormats();
		Assertions.assertEquals(0, formats.size());
		Assertions.assertEquals("", cg.getValue(ControllerConfigGroup.EVENTS_FILE_FORMAT));
	}

	/**
	 * Ensure that the mobsim-selector is correctly stored
	 * and returned with the different setters and getters.
	 *
	 * @author mrieser
	 */
	@Test
	void testMobsim() {
		ControllerConfigGroup cg = new ControllerConfigGroup();
		// test initial value
		Assertions.assertEquals("qsim", cg.getMobsim());
		Assertions.assertEquals("qsim", cg.getValue(ControllerConfigGroup.MOBSIM));
		// test setting to null
		cg.setMobsim(null);
		Assertions.assertNull(cg.getMobsim());
		Assertions.assertNull(cg.getValue(ControllerConfigGroup.MOBSIM));
		// test setting with addParam
		cg.addParam(ControllerConfigGroup.MOBSIM, "queueSimulation");
		Assertions.assertEquals("queueSimulation", cg.getMobsim());
		Assertions.assertEquals("queueSimulation", cg.getValue(ControllerConfigGroup.MOBSIM));
	}

	/**
	 * Ensure that the write-plans-interval value is correctly stored
	 * and returned with the different setters and getters.
	 *
	 * @author mrieser
	 */
	@Test
	void testWritePlansInterval() {
		ControllerConfigGroup cg = new ControllerConfigGroup();
		// test initial value
		Assertions.assertEquals(50, cg.getWritePlansInterval());
		// test setting with setMobsim
		cg.setWritePlansInterval(4);
		Assertions.assertEquals(4, cg.getWritePlansInterval());
		// test setting with addParam
		cg.addParam("writePlansInterval", "2");
		Assertions.assertEquals(2, cg.getWritePlansInterval());
	}

	/**
	 * Ensure that the enableLinkToLinkRouting value is correctly stored and
	 * returned with the getters and setters.
	 */
	@Test
	void testLink2LinkRouting(){
		ControllerConfigGroup cg = new ControllerConfigGroup();
		//initial value
		Assertions.assertFalse(cg.isLinkToLinkRoutingEnabled());
		//modify by string
		cg.addParam("enableLinkToLinkRouting", "true");
		Assertions.assertTrue(cg.isLinkToLinkRoutingEnabled());
		cg.addParam("enableLinkToLinkRouting", "false");
		Assertions.assertFalse(cg.isLinkToLinkRoutingEnabled());
		//modify by boolean
		cg.setLinkToLinkRoutingEnabled(true);
		Assertions.assertTrue(cg.isLinkToLinkRoutingEnabled());
		Assertions.assertEquals("true", cg.getValue("enableLinkToLinkRouting"));
		cg.setLinkToLinkRoutingEnabled(false);
		Assertions.assertFalse(cg.isLinkToLinkRoutingEnabled());
		Assertions.assertEquals("false", cg.getValue("enableLinkToLinkRouting"));
	}

	/**
	 * Ensure that the writeSnapshotsInterval value is correctly stored and
	 * returned with the getters and setters.
	 */
	@Test
	void testWriteSnapshotInterval(){
		ControllerConfigGroup cg = new ControllerConfigGroup();
		//initial value
		Assertions.assertEquals(1, cg.getWriteSnapshotsInterval());
		//modify by string
		cg.addParam("writeSnapshotsInterval", "10");
		Assertions.assertEquals(10, cg.getWriteSnapshotsInterval());
		//modify by boolean
		cg.setWriteSnapshotsInterval(42);
		Assertions.assertEquals("42", cg.getValue("writeSnapshotsInterval"));
		Assertions.assertEquals(42, cg.getWriteSnapshotsInterval());
	}

	@Test
	public void testCreateGraphsInterval() {
		ControllerConfigGroup cg = new ControllerConfigGroup();
		//initial value
		Assertions.assertEquals(1, cg.getCreateGraphsInterval());
		//modify by string
		cg.addParam("createGraphsInterval", "10");
		Assertions.assertEquals(10, cg.getCreateGraphsInterval());
		//modify by setter
		cg.setCreateGraphsInterval(42);
		Assertions.assertEquals("42", cg.getValue("createGraphsInterval"));
		Assertions.assertEquals(42, cg.getCreateGraphsInterval());
		//modify by deprecated setter
		cg.setCreateGraphs(true);
		Assertions.assertEquals(1, cg.getCreateGraphsInterval());
	}


}
