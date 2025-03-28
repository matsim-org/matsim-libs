/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.io.File;

/**
 * @author mrieser
 */
public class AnalysisConfigGroupIT {

	@Test
	public void testAnalysisConfigSettings() {
		Config config = this.util.loadConfig((String) null);

		AnalysisConfigGroup ac = config.analysis();

		ac.setLegHistogramInterval(2);
		ac.setLegDurationsInterval(3);

		final Controler controler = new Controler(ScenarioUtils.createScenario(config));
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				if (getConfig().controller().getMobsim().equals("dummy")) {
					bind(Mobsim.class).toProvider(DummyMobsimFactory.class);
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

	@RegisterExtension
	private MatsimTestUtils util = new MatsimTestUtils();

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
			return new DummyMobsim(eventsManager, count++);
		}
	}

}
