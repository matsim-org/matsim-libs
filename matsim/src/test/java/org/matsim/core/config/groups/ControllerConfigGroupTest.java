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

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

import com.google.inject.Provider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup.EventsFileFormat;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

public class ControllerConfigGroupTest {

	@RegisterExtension
	private MatsimTestUtils util = new MatsimTestUtils();

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

	@Test
	public void testAnalysisConfigSettings() {
		Config config = this.util.loadConfig((String) null);

		ControllerConfigGroup ac = config.controller();

		ac.setLegHistogramInterval(2);
		ac.setLegDurationsInterval(3);

		final Controler controler = new Controler(ScenarioUtils.createScenario(config));
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				if (getConfig().controller().getMobsim().equals("dummy")) {
					bind(Mobsim.class).toProvider(ControllerConfigGroupTest.DummyMobsimFactory.class);
				}
			}
		});
		int maxIterations = 10;
		config.controller().setMobsim("dummy");
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(maxIterations);

		controler.getConfig().controller().setCreateGraphsInterval(0);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setWriteEventsInterval(0);
		config.controller().setWritePlansInterval(0);
		controler.run();

		assertFileStatus(maxIterations, config.controller().getOutputDirectory(), ac.getLegHistogramInterval(), "legHistogram.txt");
		assertFileStatus(maxIterations, config.controller().getOutputDirectory(), ac.getLegDurationsInterval(), "legdurations.txt");
	}

	private void assertFileStatus(int maxIterations, String outputDirectory, int interval, String filename) {
		for (int iteration = 0; iteration < maxIterations; iteration++) {
			boolean exists = (iteration % interval) == 0;
			Assertions.assertEquals(exists, new File(outputDirectory + "ITERS/it." + iteration + "/" + iteration + "." + filename).exists());
		}
		// it should always exist in the last iteration
		Assertions.assertTrue(new File(outputDirectory + "ITERS/it." + maxIterations + "/" + maxIterations + "." + filename).exists());
	}

	private static class DummyMobsim implements Mobsim {
		private final EventsManager eventsManager;
		private final int nOfEvents;

		public DummyMobsim(EventsManager eventsManager, final int nOfEvents) {
			this.eventsManager = eventsManager;
			this.nOfEvents = nOfEvents;
		}

		@Override
		public void run() {
			Id<Link> linkId = Id.create("100", Link.class);
			for (int i = 0; i < this.nOfEvents; i++) {
				this.eventsManager.processEvent(new LinkLeaveEvent(60.0, Id.create(i, Vehicle.class), linkId));
			}
		}
	}

	@Singleton
	private static class DummyMobsimFactory implements Provider<Mobsim> {
		private int count = 1;

		@Inject
		EventsManager eventsManager;

		@Override
		public Mobsim get() {
			return new ControllerConfigGroupTest.DummyMobsim(eventsManager, count++);
		}
	}


}
