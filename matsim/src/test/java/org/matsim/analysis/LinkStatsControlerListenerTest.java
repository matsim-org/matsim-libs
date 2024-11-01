/* **********************************************4************************ *
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

package org.matsim.analysis;

import com.google.inject.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.LinkStatsConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mrieser
 */
public class LinkStatsControlerListenerTest {

	@RegisterExtension
	private MatsimTestUtils util = new MatsimTestUtils();

	@Test
	void testlinksOutputCSV() throws IOException {
		String outputDirectory = util.getOutputDirectory();

		Config config = this.util.loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controller().setLastIteration(10);
		config.controller().setOutputDirectory(outputDirectory);
		Controler c = new Controler(config);

		c.run();

		File csv = new File(outputDirectory, "output_links.csv.gz");

		assertThat(csv).exists();

		assertThat(new GZIPInputStream(new FileInputStream(csv)))
				.asString(StandardCharsets.UTF_8)
				.startsWith("link;from_node;to_node;length;freespeed;capacity;lanes;modes;vol_car;storageCapacityUsedInQsim;geometry");

	}

	@Test
	void testUseVolumesOfIteration() {
		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(util.getOutputDirectory());
		final Scenario scenario = ScenarioUtils.createScenario(config);
		com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new LinkStatsModule());
				install(new VolumesAnalyzerModule());
				install(new EventsManagerModule());
				install(new ScenarioByInstanceModule(scenario));
				bind(OutputDirectoryHierarchy.class).asEagerSingleton();
				bind(IterationStopWatch.class).asEagerSingleton();
			}
		});
		LinkStatsControlerListener lscl = injector.getInstance(LinkStatsControlerListener.class);

		config.linkStats().setWriteLinkStatsInterval(10);

		// test defaults
		Assertions.assertEquals(10, config.linkStats().getWriteLinkStatsInterval());
		Assertions.assertEquals(5, config.linkStats().getAverageLinkStatsOverIterations());

		// now the real tests
		Assertions.assertFalse(lscl.useVolumesOfIteration(0, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(1, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(2, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(3, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(4, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(5, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(6, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(7, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(8, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(9, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(10, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(11, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(12, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(13, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(14, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(15, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(16, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(17, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(18, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(19, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(20, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(21, 0));

		// change some values
		config.linkStats().setWriteLinkStatsInterval(8);
		config.linkStats().setAverageLinkStatsOverIterations(2);
		Assertions.assertFalse(lscl.useVolumesOfIteration(0, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(1, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(2, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(3, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(4, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(5, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(6, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(7, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(8, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(9, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(10, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(11, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(12, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(13, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(14, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(15, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(16, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(17, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(18, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(19, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(20, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(21, 0));

		// change some values: averaging = 1
		config.linkStats().setWriteLinkStatsInterval(5);
		config.linkStats().setAverageLinkStatsOverIterations(1);
		Assertions.assertTrue(lscl.useVolumesOfIteration(0, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(1, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(2, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(3, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(4, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(5, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(6, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(7, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(8, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(9, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(10, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(11, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(12, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(13, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(14, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(15, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(16, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(17, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(18, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(19, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(20, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(21, 0));

		// change some values: averaging = 0
		config.linkStats().setWriteLinkStatsInterval(5);
		config.linkStats().setAverageLinkStatsOverIterations(0);
		Assertions.assertTrue(lscl.useVolumesOfIteration(0, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(1, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(2, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(3, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(4, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(5, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(6, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(7, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(8, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(9, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(10, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(11, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(12, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(13, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(14, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(15, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(16, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(17, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(18, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(19, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(20, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(21, 0));

		// change some values: interval = 0
		config.linkStats().setWriteLinkStatsInterval(0);
		config.linkStats().setAverageLinkStatsOverIterations(2);
		Assertions.assertFalse(lscl.useVolumesOfIteration(0, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(1, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(2, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(3, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(4, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(5, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(6, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(7, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(8, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(9, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(10, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(11, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(12, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(13, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(14, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(15, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(16, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(17, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(18, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(19, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(20, 0));
		Assertions.assertFalse(lscl.useVolumesOfIteration(21, 0));

		// change some values: interval equal averaging
		config.linkStats().setWriteLinkStatsInterval(5);
		config.linkStats().setAverageLinkStatsOverIterations(5);
		Assertions.assertFalse(lscl.useVolumesOfIteration(0, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(1, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(2, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(3, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(4, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(5, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(6, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(7, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(8, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(9, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(10, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(11, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(12, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(13, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(14, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(15, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(16, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(17, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(18, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(19, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(20, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(21, 0));

		// change some values: averaging > interval
		config.linkStats().setWriteLinkStatsInterval(5);
		config.linkStats().setAverageLinkStatsOverIterations(6);
		Assertions.assertFalse(lscl.useVolumesOfIteration(0, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(1, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(2, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(3, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(4, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(5, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(6, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(7, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(8, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(9, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(10, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(11, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(12, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(13, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(14, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(15, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(16, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(17, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(18, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(19, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(20, 0));
		Assertions.assertTrue(lscl.useVolumesOfIteration(21, 0));

		// change some values: different firstIteration
		config.linkStats().setWriteLinkStatsInterval(5);
		config.linkStats().setAverageLinkStatsOverIterations(3);
		Assertions.assertFalse(lscl.useVolumesOfIteration(4, 4));
		Assertions.assertFalse(lscl.useVolumesOfIteration(5, 4));
		Assertions.assertFalse(lscl.useVolumesOfIteration(6, 4));
		Assertions.assertFalse(lscl.useVolumesOfIteration(7, 4));
		Assertions.assertTrue(lscl.useVolumesOfIteration(8, 4));
		Assertions.assertTrue(lscl.useVolumesOfIteration(9, 4));
		Assertions.assertTrue(lscl.useVolumesOfIteration(10, 4));
		Assertions.assertFalse(lscl.useVolumesOfIteration(11, 4));
		Assertions.assertFalse(lscl.useVolumesOfIteration(12, 4));
		Assertions.assertTrue(lscl.useVolumesOfIteration(13, 4));
		Assertions.assertTrue(lscl.useVolumesOfIteration(14, 4));
		Assertions.assertTrue(lscl.useVolumesOfIteration(15, 4));
		Assertions.assertFalse(lscl.useVolumesOfIteration(16, 4));
		Assertions.assertFalse(lscl.useVolumesOfIteration(17, 4));
		Assertions.assertTrue(lscl.useVolumesOfIteration(18, 4));
		Assertions.assertTrue(lscl.useVolumesOfIteration(19, 4));
		Assertions.assertTrue(lscl.useVolumesOfIteration(20, 4));
		Assertions.assertFalse(lscl.useVolumesOfIteration(21, 4));
	}

	@Test
	void test_writeLinkStatsInterval() {
		Config config = this.util.loadConfig((String) null);
		LinkStatsConfigGroup lsConfig = config.linkStats();

		lsConfig.setWriteLinkStatsInterval(3);
		lsConfig.setAverageLinkStatsOverIterations(1);

		final Controler controler = new Controler(ScenarioUtils.createScenario(config));
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				if (getConfig().controller().getMobsim().equals("dummy")) {
					bind(Mobsim.class).toProvider(DummyMobsimFactory.class);
				}
			}
		});
		config.controller().setMobsim("dummy");
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(7);

		controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setWriteEventsInterval(0);
		config.controller().setWritePlansInterval(0);
		controler.run();

		Assertions.assertTrue(new File(config.controller().getOutputDirectory() + "ITERS/it.0/0.linkstats.txt.gz").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.1/1.linkstats.txt.gz").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.2/2.linkstats.txt.gz").exists());
		Assertions.assertTrue(new File(config.controller().getOutputDirectory() + "ITERS/it.3/3.linkstats.txt.gz").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.4/4.linkstats.txt.gz").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.5/5.linkstats.txt.gz").exists());
		Assertions.assertTrue(new File(config.controller().getOutputDirectory() + "ITERS/it.6/6.linkstats.txt.gz").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.7/7.linkstats.txt.gz").exists());
	}

	@Test
	void testReset_CorrectlyExecuted() throws IOException {
		Config config = this.util.loadConfig((String) null);
		config.controller().setMobsim("dummy");
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(7);
		config.controller().setWritePlansInterval(0);
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		LinkStatsConfigGroup lsConfig = config.linkStats();

		lsConfig.setWriteLinkStatsInterval(3);
		lsConfig.setAverageLinkStatsOverIterations(2);
		Scenario scenario = ScenarioUtils.createScenario(config);
		Node node1 = scenario.getNetwork().getFactory().createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = scenario.getNetwork().getFactory().createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		Link link = scenario.getNetwork().getFactory().createLink(Id.create("100", Link.class), node1, node2);
		scenario.getNetwork().addLink(link);
		final Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				if (getConfig().controller().getMobsim().equals("dummy")) {
					bind(Mobsim.class).toProvider(DummyMobsimFactory.class);
				}
			}
		});

		controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setWriteEventsInterval(0);
		controler.run();

		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.0/0.linkstats.txt.gz").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.1/1.linkstats.txt.gz").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.2/2.linkstats.txt.gz").exists());
		Assertions.assertTrue(new File(config.controller().getOutputDirectory() + "ITERS/it.3/3.linkstats.txt.gz").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.4/4.linkstats.txt.gz").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.5/5.linkstats.txt.gz").exists());
		Assertions.assertTrue(new File(config.controller().getOutputDirectory() + "ITERS/it.6/6.linkstats.txt.gz").exists());
		Assertions.assertFalse(new File(config.controller().getOutputDirectory() + "ITERS/it.7/7.linkstats.txt.gz").exists());

		double[] volumes = getVolumes(config.controller().getOutputDirectory() + "ITERS/it.3/3.linkstats.txt");
		Assertions.assertEquals(3, volumes[0], 1e-8);
		Assertions.assertEquals(3.5, volumes[1], 1e-8);
		Assertions.assertEquals(4, volumes[2], 1e-8);
		volumes = getVolumes(config.controller().getOutputDirectory() + "ITERS/it.6/6.linkstats.txt");
		Assertions.assertEquals(6, volumes[0], 1e-8);
		Assertions.assertEquals(6.5, volumes[1], 1e-8);
		Assertions.assertEquals(7, volumes[2], 1e-8);
	}

	private double[] getVolumes(final String filename) throws IOException {
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		reader.readLine(); // header
		String line = reader.readLine(); // link 100
		if (line == null) {
			// should never happen...
			return new double[]{Double.NaN, Double.NaN, Double.NaN};
		}
		String[] parts = line.split("\t");// [0] = linkId, [1] = matsim volume, [2] = real volume
		return new double[]{
				Double.parseDouble(parts[7]), // min
				Double.parseDouble(parts[8]),    // avg
				Double.parseDouble(parts[9])    // max
		};
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
			return new DummyMobsim(eventsManager, count++);
		}
	}
}
