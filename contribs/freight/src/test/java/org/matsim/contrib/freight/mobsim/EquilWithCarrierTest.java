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
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
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
		config.controler().setLastIteration(1);
        config.network().setInputFile(NETWORK_FILENAME);
      //  config.plans().setInputFile(PLANS_FILENAME);
		config.addQSimConfigGroup(new QSimConfigGroup());
		Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.addControlerListener(new RunMobSimWithCarrier( getInputDirectory() + "carrierPlans.xml"));
		controler.setOverwriteFiles(true);
		controler.run();
    }

}
