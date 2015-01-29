/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.mobsim.jdeqsim;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

public class ConfigParameterTest extends MatsimTestCase {

	public void testParametersSetCorrectly() {


		Config config = super.loadConfig(this.getPackageInputDirectory() + "config.xml");
		config.controler().setMobsim(MobsimType.JDEQSim.toString());
		Controler controler = new Controler(config);
		controler.getConfig().controler().setWriteEventsInterval(0);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.run();
		/* make sure, all simulation parameters are set properly from
		 * config xml file */

		assertEquals(360.0, SimulationParameters.getSimulationEndTime(), EPSILON);
		assertEquals(2.0, SimulationParameters.getFlowCapacityFactor(), EPSILON);
		assertEquals(3.0, SimulationParameters.getStorageCapacityFactor(), EPSILON);
		assertEquals(3600.0, SimulationParameters.getMinimumInFlowCapacity(), EPSILON);
		assertEquals(10.0, SimulationParameters.getCarSize(), EPSILON);
		assertEquals(20.0, SimulationParameters.getGapTravelSpeed(), EPSILON);
		assertEquals(9000.0, SimulationParameters.getSqueezeTime(), EPSILON);
	}
}
