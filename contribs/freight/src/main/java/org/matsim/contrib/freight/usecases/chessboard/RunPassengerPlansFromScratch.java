/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.usecases.chessboard;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

@Deprecated
final class RunPassengerPlansFromScratch {
	// yyyy this does not work anymore.  Not secured by a testcase.  I think that it is only there to have an example to run
	// matsim _without_ freight.  --> imo, remove.  kai, jan'19

	public static void main(String[] args) {
		String NETWORK_FILENAME = "input/usecases/chessboard/network/grid9x9.xml";
		String PLANS_FILENAME = "input/usecases/chessboard/passenger/passengerPlans.xml";
		Config config = new Config();
		config.addCoreModules();

		ActivityParams workParams = new ActivityParams("work");
		workParams.setTypicalDuration(60 * 60 * 8);
		config.planCalcScore().addActivityParams(workParams);
		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(16 * 60 * 60);
		config.planCalcScore().addActivityParams(homeParams);
		config.global().setCoordinateSystem("EPSG:32632");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(2);
		config.network().setInputFile(NETWORK_FILENAME);
		config.plans().setInputFile(PLANS_FILENAME);

		StrategySettings bestScore = new StrategySettings();
		bestScore.setStrategyName("BestScore");
		bestScore.setWeight(0.5);

		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.5);
		//		reRoute.setDisableAfter(300);

		config.strategy().setMaxAgentPlanMemorySize(5);
		config.strategy().addStrategySettings(bestScore);
		config.strategy().addStrategySettings(reRoute);
		//
		Controler controler = new Controler(config);
		controler.getConfig().controler().setWriteEventsInterval(1);
		controler.getConfig().controler().setCreateGraphs(false);
		//Select how to react of not empty output directory
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		//		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);

		controler.run();

	}

}
