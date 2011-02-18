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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.ptproject.qsim.ParallelQSimulation;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class ControlerMobsimIntegrationTest {

	private final static Logger log = Logger.getLogger(ControlerMobsimIntegrationTest.class);
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunMobsim_qsim() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("qsim");
		cfg.addQSimConfigGroup(new QSimConfigGroup());
		FakeControler c = new FakeControler(cfg);
		c.setCreateGraphs(false);
		c.run();
		Assert.assertTrue(c.sim instanceof QSim);
		Assert.assertNull(((QSim) c.sim).getMultiModalSimEngine());
	}

	@Test
	public void testRunMobsim_qsim_parallel() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("qsim");
		cfg.addQSimConfigGroup(new QSimConfigGroup());
		cfg.getQSimConfigGroup().setNumberOfThreads(3);
		FakeControler c = new FakeControler(cfg);
		c.setCreateGraphs(false);
		c.run();
		Assert.assertTrue(c.sim instanceof ParallelQSimulation);
	}

	@Test
	public void testRunMobsim_queueSimulation() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("queueSimulation");
		FakeControler c = new FakeControler(cfg);
		c.setCreateGraphs(false);
		c.run();
		Assert.assertTrue(c.sim instanceof QueueSimulation);
	}

	@Test
	public void testRunMobsim_jdeqsim() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("jdeqsim");
		FakeControler c = new FakeControler(cfg);
		c.setCreateGraphs(false);
		c.run();
		Assert.assertTrue(c.sim instanceof JDEQSimulation);
	}

	@Test
	public void testRunMobsim_multimodalQSim() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("multimodalQSim");
		cfg.addQSimConfigGroup(new QSimConfigGroup());
		cfg.multiModal().setMultiModalSimulationEnabled(true);
		FakeControler c = new FakeControler(cfg);
		c.setCreateGraphs(false);
		c.run();
		System.out.println(c.sim.getClass().getCanonicalName());
		Assert.assertTrue(c.sim instanceof QSim);
		Assert.assertNotNull(((QSim) c.sim).getMultiModalSimEngine());
	}

	@Test
	public void testRunMobsim_customMobsim() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("counting");
		Controler c = new Controler(cfg);
		CountingMobsimFactory mf = new CountingMobsimFactory();
		c.addMobsimFactory("counting", mf);
		c.setCreateGraphs(false);
		c.run();
		Assert.assertEquals(1, mf.callCount);
	}

	@Test
	public void testRunMobsim_missingMobsimFactory() {
		Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		cfg.controler().setLastIteration(0);
		cfg.controler().setMobsim("counting");
		Controler c = new Controler(cfg);
		try {
			c.run();
			Assert.fail("expected exception, but there was none.");
		} catch (IllegalArgumentException e) {
			log.info("catched expected exception.", e);
		}
	}

	private static class FakeControler extends Controler {
		/*package*/ Simulation sim = null;
		public FakeControler(final Config cfg) {
			super(cfg);
		}
		@Override
		Simulation getNewMobsim() {
			// remember the created sim, but return a dummy
			this.sim = super.getNewMobsim();
			return new Simulation() {
				@Override
				public void run() {
				}
			};
		}
	}

	private static class CountingMobsimFactory implements MobsimFactory {

		/*package*/ int callCount = 0;

		@Override
		public Simulation createMobsim(Scenario sc, EventsManager eventsManager) {
			this.callCount++;
			return new Simulation() {
				@Override
				public void run() {
				}
			};
		}

	}
}
