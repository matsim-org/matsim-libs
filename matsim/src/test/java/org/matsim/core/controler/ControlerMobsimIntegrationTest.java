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

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.ActiveQSimBridge;
import org.matsim.core.mobsim.qsim.AgentCounterImpl;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class ControlerMobsimIntegrationTest {

	private final static Logger log = Logger.getLogger(ControlerMobsimIntegrationTest.class);
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunMobsim_customMobsim() {
		Config cfg = this.utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("counting");
		cfg.controler().setWritePlansInterval(0);
		final Controler c = new Controler(cfg);
		final CountingMobsimFactory mf = new CountingMobsimFactory();
		c.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				if (getConfig().controler().getMobsim().equals("counting")) {
					bind(Mobsim.class).toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return mf.createMobsim(c.getScenario(), c.getEvents());
						}
					});
					
					bind(MobsimTimer.class);
					bind(AgentCounter.class).to(AgentCounterImpl.class);
					bind(ActiveQSimBridge.class);
				}
			}
		});
		c.getConfig().controler().setCreateGraphs(false);
		c.getConfig().controler().setDumpDataAtEnd(false);
		c.getConfig().controler().setWriteEventsInterval(0);
		c.run();
		Assert.assertEquals(1, mf.callCount);
	}

	@Test(expected = RuntimeException.class)
	public void testRunMobsim_missingMobsimFactory() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("counting");
		cfg.controler().setWritePlansInterval(0);
		Controler c = new Controler(cfg);
        c.getConfig().controler().setCreateGraphs(false);
		c.getConfig().controler().setDumpDataAtEnd(false);
		c.getConfig().controler().setWriteEventsInterval(0);
		c.run();
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
