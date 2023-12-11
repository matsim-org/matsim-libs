/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.counts;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Provider;

/**
 * @author mrieser
 */
public class CountsControlerListenerTest {

	@RegisterExtension private MatsimTestUtils util = new MatsimTestUtils();

	@Test
	void testUseVolumesOfIteration() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		CountsControlerListener ccl = new CountsControlerListener(config.global(), scenario.getNetwork(), config.controller(), config.counts(), null, null, null);

		// test defaults
		Assertions.assertEquals(10, config.counts().getWriteCountsInterval());
		Assertions.assertEquals(5, config.counts().getAverageCountsOverIterations());

		// now the real tests
		Assertions.assertFalse(ccl.useVolumesOfIteration(0, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(1, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(2, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(3, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(4, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(5, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(6, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(7, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(8, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(9, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(10, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(11, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(12, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(13, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(14, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(15, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(16, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(17, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(18, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(19, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(20, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(21, 0));

		// change some values
		config.counts().setWriteCountsInterval(8);
		config.counts().setAverageCountsOverIterations(2);
		Assertions.assertFalse(ccl.useVolumesOfIteration(0, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(1, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(2, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(3, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(4, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(5, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(6, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(7, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(8, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(9, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(10, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(11, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(12, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(13, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(14, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(15, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(16, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(17, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(18, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(19, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(20, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(21, 0));

		// change some values: averaging = 1
		config.counts().setWriteCountsInterval(5);
		config.counts().setAverageCountsOverIterations(1);
		Assertions.assertTrue(ccl.useVolumesOfIteration(0, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(1, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(2, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(3, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(4, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(5, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(6, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(7, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(8, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(9, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(10, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(11, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(12, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(13, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(14, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(15, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(16, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(17, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(18, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(19, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(20, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(21, 0));

		// change some values: averaging = 0
		config.counts().setWriteCountsInterval(5);
		config.counts().setAverageCountsOverIterations(0);
		Assertions.assertTrue(ccl.useVolumesOfIteration(0, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(1, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(2, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(3, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(4, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(5, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(6, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(7, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(8, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(9, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(10, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(11, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(12, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(13, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(14, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(15, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(16, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(17, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(18, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(19, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(20, 0));
		Assertions.assertFalse(ccl.useVolumesOfIteration(21, 0));

		// change some values: interval equal averaging
		config.counts().setWriteCountsInterval(5);
		config.counts().setAverageCountsOverIterations(5);
		Assertions.assertFalse(ccl.useVolumesOfIteration(0, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(1, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(2, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(3, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(4, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(5, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(6, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(7, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(8, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(9, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(10, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(11, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(12, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(13, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(14, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(15, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(16, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(17, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(18, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(19, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(20, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(21, 0));

		// change some values: averaging > interval
		config.counts().setWriteCountsInterval(5);
		config.counts().setAverageCountsOverIterations(6);
		Assertions.assertFalse(ccl.useVolumesOfIteration(0, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(1, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(2, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(3, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(4, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(5, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(6, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(7, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(8, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(9, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(10, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(11, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(12, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(13, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(14, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(15, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(16, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(17, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(18, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(19, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(20, 0));
		Assertions.assertTrue(ccl.useVolumesOfIteration(21, 0));

		// change some values: different firstIteration
		config.counts().setWriteCountsInterval(5);
		config.counts().setAverageCountsOverIterations(3);
		Assertions.assertFalse(ccl.useVolumesOfIteration(4, 4));
		Assertions.assertFalse(ccl.useVolumesOfIteration(5, 4));
		Assertions.assertFalse(ccl.useVolumesOfIteration(6, 4));
		Assertions.assertFalse(ccl.useVolumesOfIteration(7, 4));
		Assertions.assertTrue(ccl.useVolumesOfIteration(8, 4));
		Assertions.assertTrue(ccl.useVolumesOfIteration(9, 4));
		Assertions.assertTrue(ccl.useVolumesOfIteration(10, 4));
		Assertions.assertFalse(ccl.useVolumesOfIteration(11, 4));
		Assertions.assertFalse(ccl.useVolumesOfIteration(12, 4));
		Assertions.assertTrue(ccl.useVolumesOfIteration(13, 4));
		Assertions.assertTrue(ccl.useVolumesOfIteration(14, 4));
		Assertions.assertTrue(ccl.useVolumesOfIteration(15, 4));
		Assertions.assertFalse(ccl.useVolumesOfIteration(16, 4));
		Assertions.assertFalse(ccl.useVolumesOfIteration(17, 4));
		Assertions.assertTrue(ccl.useVolumesOfIteration(18, 4));
		Assertions.assertTrue(ccl.useVolumesOfIteration(19, 4));
		Assertions.assertTrue(ccl.useVolumesOfIteration(20, 4));
		Assertions.assertFalse(ccl.useVolumesOfIteration(21, 4));
	}

	@Test
	void test_writeCountsInterval() {
		Config config = this.util.createConfig(ExamplesUtils.getTestScenarioURL("triangle"));
		CountsConfigGroup cConfig = config.counts();

		cConfig.setWriteCountsInterval(3);
		cConfig.setAverageCountsOverIterations(1);
		cConfig.setOutputFormat("txt");
		cConfig.setInputFile("counts.xml"); // just any file to activate the counts feature

		final Controler controler = new Controler(ScenarioUtils.createScenario(config));
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Mobsim.class).toProvider(DummyMobsimFactory.class);
			}
		});
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(7);

        controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setWriteEventsInterval(0);
		config.controller().setWritePlansInterval(0);
		controler.run();

		Assertions.assertTrue(new File(config.controller().getOutputDirectory() + "ITERS/it.0/0.countscompare.txt").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.1/1.countscompare.txt").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.2/2.countscompare.txt").exists());
		Assertions.assertTrue(new File(config.controller().getOutputDirectory() + "ITERS/it.3/3.countscompare.txt").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.4/4.countscompare.txt").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.5/5.countscompare.txt").exists());
		Assertions.assertTrue(new File(config.controller().getOutputDirectory() + "ITERS/it.6/6.countscompare.txt").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.7/7.countscompare.txt").exists());
	}

	@Test
	void testReset_CorrectlyExecuted() throws IOException {
		Config config = this.util.createConfig(ExamplesUtils.getTestScenarioURL("triangle"));
		config.network().setInputFile("network.xml");	// network file which is used by the counts file

		CountsConfigGroup cConfig = config.counts();

		cConfig.setWriteCountsInterval(3);
		cConfig.setAverageCountsOverIterations(2);
		cConfig.setOutputFormat("txt");
		cConfig.setInputFile("counts.xml"); // just any file to activate the counts feature

		final Controler controler = new Controler(ScenarioUtils.loadScenario(config));
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Mobsim.class).toProvider(DummyMobsimFactory.class);
			}
		});
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(7);

        controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setWriteEventsInterval(0);
		config.controller().setWritePlansInterval(0);
		controler.run();

		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.0/0.countscompareAWTV.txt").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.1/1.countscompareAWTV.txt").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.2/2.countscompareAWTV.txt").exists());
		Assertions.assertTrue(new File(config.controller().getOutputDirectory() + "ITERS/it.3/3.countscompareAWTV.txt").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.4/4.countscompareAWTV.txt").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.5/5.countscompareAWTV.txt").exists());
		Assertions.assertTrue(new File(config.controller().getOutputDirectory() + "ITERS/it.6/6.countscompareAWTV.txt").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.7/7.countscompareAWTV.txt").exists());

		Assertions.assertEquals(3.5, getVolume(config.controller().getOutputDirectory() + "ITERS/it.3/3.countscompareAWTV.txt"), 1e-8);
		Assertions.assertEquals(6.5, getVolume(config.controller().getOutputDirectory() + "ITERS/it.6/6.countscompareAWTV.txt"), 1e-8);
	}

	@Test
	void testFilterAnalyzedModes() throws IOException {
		Config config = util.createConfig(ExamplesUtils.getTestScenarioURL("triangle"));
		config.network().setInputFile("network.xml");	// network file which is used by the counts file

		CountsConfigGroup cConfig = config.counts();

		cConfig.setWriteCountsInterval(3);
		cConfig.setAverageCountsOverIterations(2);
		cConfig.setOutputFormat("txt");
		cConfig.setInputFile("counts.xml"); // just any file to activate the counts feature

		config.controller().setMobsim("dummy");
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(3);

		createAndRunControler(config);
		Assertions.assertEquals(150, getVolume(config.controller().getOutputDirectory() + "ITERS/it.3/3.countscompareAWTV.txt"), 1e-8);

		// enable modes filtering and count only car
		cConfig.setAnalyzedModes(TransportMode.car);
		cConfig.setFilterModes(true);
		createAndRunControler(config);
		Assertions.assertEquals(100, getVolume(config.controller().getOutputDirectory() + "ITERS/it.3/3.countscompareAWTV.txt"), 1e-8);

		// enable modes filtering and count only walk
		cConfig.setAnalyzedModes(TransportMode.walk);
		cConfig.setFilterModes(true);
		createAndRunControler(config);
		Assertions.assertEquals(50, getVolume(config.controller().getOutputDirectory() + "ITERS/it.3/3.countscompareAWTV.txt"), 1e-8);

		// enable modes filtering and count only bike
		cConfig.setAnalyzedModes(TransportMode.bike);
		cConfig.setFilterModes(true);
		createAndRunControler(config);
		Assertions.assertEquals(0, getVolume(config.controller().getOutputDirectory() + "ITERS/it.3/3.countscompareAWTV.txt"), 1e-8);


	}

	private void createAndRunControler(Config config) {
		final Controler controler = new Controler(ScenarioUtils.loadScenario(config));
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Mobsim.class).to(DummyMobsim2.class);
				// use single threaded manager because the dummy mobsim doesn't correctly calls EventsManager interface which may fail for parallel implementations
				bind(EventsManager.class).to(EventsManagerImpl.class).in(Singleton.class);
			}
		});
		controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controller().setWriteEventsInterval(0);
		config.controller().setWritePlansInterval(0);
		controler.run();
	}

	private double getVolume(final String filename) throws IOException {
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		reader.readLine(); // header
		String line = reader.readLine(); // link 100
		if (line == null) {
			return Double.NaN; // should never happen...
		}
		String[] parts = line.split("\t");// [0] = linkId, [1] = Count Station Id, [2] = matsim volume, [3] = real volume, [4] = normalized relative error
		return Double.parseDouble(parts[2]);
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
		@Inject EventsManager eventsManager;

		@Override
		public Mobsim get() {
			return new DummyMobsim(eventsManager, count++);
		}
	}

	private static class DummyMobsim2 implements Mobsim {
		@Inject EventsManager eventsManager;

		@Override
		public void run() {
			Id<Link> linkId = Id.create("100", Link.class);
			for (int i = 0; i < 100; i++) {
				Id<Person> agentId = Id.create(i, Person.class);
				Id<Vehicle> vehId = Id.create(i, Vehicle.class);
				this.eventsManager.processEvent(new PersonDepartureEvent(60.0, agentId, linkId, TransportMode.car, TransportMode.car));
				this.eventsManager.processEvent(new VehicleEntersTrafficEvent(60.0, agentId, linkId, vehId, TransportMode.car, 1.0));
				this.eventsManager.processEvent(new LinkLeaveEvent(60.0, vehId, linkId));
			}
			for (int i = 100; i < 150; i++) {
				Id<Person> agentId = Id.create(i, Person.class);
				Id<Vehicle> vehId = Id.create(i, Vehicle.class);
				this.eventsManager.processEvent(new PersonDepartureEvent(60.0, agentId, linkId, TransportMode.walk, TransportMode.walk));
				this.eventsManager.processEvent(new VehicleEntersTrafficEvent(60.0, agentId, linkId, vehId, TransportMode.walk, 1.0));
				this.eventsManager.processEvent(new LinkLeaveEvent(60.0, vehId, linkId));
			}
		}
	}

}
