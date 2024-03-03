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

package org.matsim.core.controler;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class ControlerMobsimIntegrationTest {

	private final static Logger log = LogManager.getLogger(ControlerMobsimIntegrationTest.class);
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testRunMobsim_customMobsim() {
		Config cfg = this.utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		cfg.controller().setLastIteration(0);
		cfg.controller().setMobsim("counting");
		cfg.controller().setWritePlansInterval(0);
		final Controler c = new Controler(cfg);
		final CountingMobsimFactory mf = new CountingMobsimFactory();
		c.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				if (getConfig().controller().getMobsim().equals("counting")) {
					bind(Mobsim.class).toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return mf.createMobsim(c.getScenario(), c.getEvents());
						}
					});
				}
			}
		});
		c.getConfig().controller().setCreateGraphs(false);
		c.getConfig().controller().setDumpDataAtEnd(false);
		c.getConfig().controller().setWriteEventsInterval(0);
		c.run();
		Assertions.assertEquals(1, mf.callCount);
	}

	@Test
	void testRunMobsim_missingMobsimFactory() {
		assertThrows(RuntimeException.class, () -> {
			Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
			cfg.controller().setLastIteration(0);
			cfg.controller().setMobsim("counting");
			cfg.controller().setWritePlansInterval(0);
			Controler c = new Controler(cfg);
			c.getConfig().controller().setCreateGraphs(false);
			c.getConfig().controller().setDumpDataAtEnd(false);
			c.getConfig().controller().setWriteEventsInterval(0);
			c.run();
		});
	}

	private static class CountingMobsimFactory implements MobsimFactory {

		/*package*/ int callCount = 0;

		@Override
		public Mobsim createMobsim(final Scenario sc, final EventsManager eventsManager) {
			this.callCount++;
			return new FakeSimulation();
		}
	}

	private static class FakeSimulation implements Mobsim {
		@Override
		public void run() {
		}
	}
}
