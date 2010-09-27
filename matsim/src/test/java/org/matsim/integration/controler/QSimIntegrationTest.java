/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.integration.controler;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class QSimIntegrationTest {

	@Rule public MatsimTestUtils util = new MatsimTestUtils();

	@Test
	public void testUseQSimFactory() {
		final Config config = this.util.loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(0);
		Assert.assertNull(config.getQSimConfigGroup());
		config.setQSimConfigGroup(new QSimConfigGroup());

		final QSimTestController controler = new QSimTestController(config);
		controler.setCreateGraphs(false);
		controler.run();
		Assert.assertEquals(1, controler.callCounter);
	}

	/*package*/ static class QSimTestController extends Controler {
		/*package*/ int callCounter = 0;
		public QSimTestController(Config config) {
			super(config);
		}
		@Override
		protected void runMobSim() {
			Assert.assertTrue(this.getMobsimFactory() instanceof QSimFactory);
			this.callCounter++;
		}
	}
}
