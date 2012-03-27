/* *********************************************************************** *
 * project: org.matsim.*
 * EquilWithCarrierTest.java
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

package org.matsim.contrib.freight.mobsim;

import org.matsim.contrib.freight.controler.RunMobSimWithCarrier;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;


public class EquilWithCarrierTest extends MatsimTestCase {

	public void testMobsimWithCarrier()  {
		String NETWORK_FILENAME = getInputDirectory() + "network.xml";
		String PLANS_FILENAME = getInputDirectory() + "plans100.xml";
		Config config = new Config();
		config.addCoreModules();
		ActivityParams workParams = new ActivityParams("w");
		workParams.setTypicalDuration(60*60*8);
		config.planCalcScore().addActivityParams(workParams);
		ActivityParams homeParams = new ActivityParams("h");
		homeParams.setTypicalDuration(16*60*60);
		config.planCalcScore().addActivityParams(homeParams);
		config.global().setCoordinateSystem("EPSG:32632");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(2);
		config.network().setInputFile(NETWORK_FILENAME);
		config.plans().setInputFile(PLANS_FILENAME);
		config.addQSimConfigGroup(new QSimConfigGroup());
		StrategySettings bestScore = new StrategySettings(new IdImpl("1"));
		bestScore.setModuleName("BestScore");
		bestScore.setProbability(0.9);
		StrategySettings reRoute = new StrategySettings(new IdImpl("2"));
		reRoute.setModuleName("ReRoute");
		reRoute.setProbability(0.1);
		reRoute.setDisableAfter(300);
		config.strategy().setMaxAgentPlanMemorySize(5);
		config.strategy().addStrategySettings(bestScore);
		config.strategy().addStrategySettings(reRoute);
		Controler controler = new Controler(config);
		controler.setWriteEventsInterval(1);
		controler.setCreateGraphs(false);
		String CARRIER_PLANS = getInputDirectory() + "carrierPlans.xml";
		controler.addControlerListener(new RunMobSimWithCarrier(CARRIER_PLANS));
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
