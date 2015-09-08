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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;

public class ControlerConfigGroupTest {

	/**
	 * Ensure that the events-file-format is correctly stored
	 * and returned with the different setters and getters.
	 *
	 * @author mrieser
	 */
	@Test
	public void testEventsFileFormat() {
		ControlerConfigGroup cg = new ControlerConfigGroup();
		Set<EventsFileFormat> formats;
		// test initial value
		formats = cg.getEventsFileFormats();
		Assert.assertEquals(1, formats.size());
		Assert.assertTrue(formats.contains(EventsFileFormat.xml));
		Assert.assertEquals("xml", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting with setEventsFileFormat
		cg.setEventsFileFormats(EnumSet.of(EventsFileFormat.xml));
		formats = cg.getEventsFileFormats();
		Assert.assertEquals(1, formats.size());
		Assert.assertTrue(formats.contains(EventsFileFormat.xml));
		Assert.assertEquals("xml", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting to none
		cg.setEventsFileFormats(EnumSet.noneOf(EventsFileFormat.class));
		formats = cg.getEventsFileFormats();
		Assert.assertEquals(0, formats.size());
		Assert.assertEquals("", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting with addParam
		cg.addParam(ControlerConfigGroup.EVENTS_FILE_FORMAT, "xml");
		formats = cg.getEventsFileFormats();
		Assert.assertEquals(1, formats.size());
		Assert.assertTrue(formats.contains(EventsFileFormat.xml));
		Assert.assertEquals("xml", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting to none
		cg.addParam(ControlerConfigGroup.EVENTS_FILE_FORMAT, "");
		formats = cg.getEventsFileFormats();
		Assert.assertEquals(0, formats.size());
		Assert.assertEquals("", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting with non-conform formatting
		cg.addParam(ControlerConfigGroup.EVENTS_FILE_FORMAT, " xml\t\t  ");
		formats = cg.getEventsFileFormats();
		Assert.assertEquals(1, formats.size());
		Assert.assertTrue(formats.contains(EventsFileFormat.xml));
		Assert.assertEquals("xml", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting to non-conform none
		cg.addParam(ControlerConfigGroup.EVENTS_FILE_FORMAT, "  \t ");
		formats = cg.getEventsFileFormats();
		Assert.assertEquals(0, formats.size());
		Assert.assertEquals("", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
	}

	/**
	 * Ensure that the mobsim-selector is correctly stored
	 * and returned with the different setters and getters.
	 *
	 * @author mrieser
	 */
	@Test
	public void testMobsim() {
		ControlerConfigGroup cg = new ControlerConfigGroup();
		// test initial value
		Assert.assertEquals("qsim", cg.getMobsim());
		Assert.assertEquals("qsim", cg.getValue(ControlerConfigGroup.MOBSIM));
		// test setting to null
		cg.setMobsim(null);
		Assert.assertNull(cg.getMobsim());
		Assert.assertNull(cg.getValue(ControlerConfigGroup.MOBSIM));
		// test setting with addParam
		cg.addParam(ControlerConfigGroup.MOBSIM, "queueSimulation");
		Assert.assertEquals("queueSimulation", cg.getMobsim());
		Assert.assertEquals("queueSimulation", cg.getValue(ControlerConfigGroup.MOBSIM));
	}

	/**
	 * Ensure that the write-plans-interval value is correctly stored
	 * and returned with the different setters and getters.
	 *
	 * @author mrieser
	 */
	@Test
	public void testWritePlansInterval() {
		ControlerConfigGroup cg = new ControlerConfigGroup();
		// test initial value
		Assert.assertEquals(10, cg.getWritePlansInterval());
		// test setting with setMobsim
		cg.setWritePlansInterval(4);
		Assert.assertEquals(4, cg.getWritePlansInterval());
		// test setting with addParam
		cg.addParam("writePlansInterval", "2");
		Assert.assertEquals(2, cg.getWritePlansInterval());
	}

	/**
	 * Ensure that the enableLinkToLinkRouting value is correctly stored and 
	 * returned with the getters and setters.
	 */
	@Test
	public void testLink2LinkRouting(){
		ControlerConfigGroup cg = new ControlerConfigGroup();
		//initial value
		Assert.assertFalse(cg.isLinkToLinkRoutingEnabled());
		//modify by string
		cg.addParam("enableLinkToLinkRouting", "true");
		Assert.assertTrue(cg.isLinkToLinkRoutingEnabled());
		cg.addParam("enableLinkToLinkRouting", "false");
		Assert.assertFalse(cg.isLinkToLinkRoutingEnabled());
		//modify by boolean
		cg.setLinkToLinkRoutingEnabled(true);
		Assert.assertTrue(cg.isLinkToLinkRoutingEnabled());
		Assert.assertEquals("true", cg.getValue("enableLinkToLinkRouting"));
		cg.setLinkToLinkRoutingEnabled(false);
		Assert.assertFalse(cg.isLinkToLinkRoutingEnabled());
		Assert.assertEquals("false", cg.getValue("enableLinkToLinkRouting"));
	}

	/**
	 * Ensure that the writeSnapshotsInterval value is correctly stored and 
	 * returned with the getters and setters.
	 */
	@Test
	public void testWriteSnapshotInterval(){
		ControlerConfigGroup cg = new ControlerConfigGroup();
		//initial value
		Assert.assertEquals(1, cg.getWriteSnapshotsInterval());
		//modify by string
		cg.addParam("writeSnapshotsInterval", "10");
		Assert.assertEquals(10, cg.getWriteSnapshotsInterval());
		//modify by boolean
		cg.setWriteSnapshotsInterval(42);
		Assert.assertEquals("42", cg.getValue("writeSnapshotsInterval"));
		Assert.assertEquals(42, cg.getWriteSnapshotsInterval());
	}

	
}
