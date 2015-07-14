/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.wagonSim.mobsim.qsim;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author droeder
 *
 */
public class WagonSimQSimFactoryTest {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(WagonSimQSimFactoryTest.class);


	@Test
	public void test() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		EventsManager manager = EventsUtils.createEventsManager(sc.getConfig());
		
		WagonSimQSimFactory factory = new WagonSimQSimFactory(new ObjectAttributes(), null);
		Mobsim sim = factory.createMobsim(sc, manager);
		Assert.assertNotNull(sim);
		Assert.assertTrue(sim instanceof QSim);
//		QSim qSim = (QSim) sim;
	}
}

