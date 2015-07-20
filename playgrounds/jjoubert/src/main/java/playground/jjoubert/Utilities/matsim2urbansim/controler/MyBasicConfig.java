/* *********************************************************************** *
 * project: org.matsim.*
 * MyBasicConfig.java
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

package playground.jjoubert.Utilities.matsim2urbansim.controler;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

public class MyBasicConfig {
	private Config config;

	public MyBasicConfig() {
		config = new Config();
		config.addCoreModules();

		// Global.
		config.global().setCoordinateSystem("WGS84");
		config.global().setRandomSeed(1234);
		config.global().setNumberOfThreads(2);

		// Network.
		config.network().setInputFile("./input/network.xml.gz");

		// Plans.
		config.plans().setInputFile("./input/plans.xml.gz");

		// Controler.
		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
		config.controler().setLinkToLinkRoutingEnabled(false);
		config.controler().setWriteEventsInterval(20);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		config.controler().setSnapshotFormat(Arrays.asList("googleearth"));
		config.controler().setOutputDirectory("./output/");

		// Simulation.
//		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setStartTime(0);
//		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setEndTime(86400);
//		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setSnapshotPeriod(0);
//		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setFlowCapFactor(1.0);
//		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setStorageCapFactor(1.0);
		Logger.getLogger("dummy").fatal("SimulationConfigGroup is no longer there.  Since `simulation' has been gone for some time now, "
				+ "I cannot see how the above may have worked so I am not fixing it.  kai, jul'15");
		System.exit(-1);

		// PlanCalcScore
		config.planCalcScore().setLearningRate(1.0);
		config.planCalcScore().setBrainExpBeta(2.0);
		config.planCalcScore().setLateArrival_utils_hr(-18.0);
		config.planCalcScore().setEarlyDeparture_utils_hr(-18.0);
		config.planCalcScore().setPerforming_utils_hr(6.0);
		config.planCalcScore().setTraveling_utils_hr(-6.0);
		//---------------------------------------------------------------------
		ActivityParams home = new ActivityParams("home");
		home.setPriority(1.0);
		home.setMinimalDuration(28800); // 8 hours
		home.setTypicalDuration(43200); // 12 hours
		config.planCalcScore().addActivityParams(home);
		//---------------------------------------------------------------------
		ActivityParams work = new ActivityParams("work");
		work.setPriority(1.0);
		work.setMinimalDuration(25200); // 7 hours
		work.setTypicalDuration(32400); // 9 hours
		work.setOpeningTime(25200);		// 07:00:00
		work.setLatestStartTime(32400); // 09:00:00
		work.setClosingTime(64800);		// 18:00:00
		config.planCalcScore().addActivityParams(work);
		//---------------------------------------------------------------------

		// Strategy.
		config.strategy().setMaxAgentPlanMemorySize(5);
		//---------------------------------------------------------------------
		StrategySettings s1 = new StrategySettings(Id.create("1", StrategySettings.class));
		s1.setStrategyName("SelectExpBeta");
		s1.setWeight(0.80);
		config.strategy().addStrategySettings(s1);
		//---------------------------------------------------------------------
		StrategySettings s2 = new StrategySettings(Id.create("2", StrategySettings.class));
		s2.setStrategyName("ReRoute");
		s2.setWeight(0.10);
		config.strategy().addStrategySettings(s2);
		//---------------------------------------------------------------------
		StrategySettings s3 = new StrategySettings(Id.create("3", StrategySettings.class));
		s3.setStrategyName("TimeAllocationMutator");
		s3.setWeight(0.10);
		config.strategy().addStrategySettings(s3);
		//---------------------------------------------------------------------

		
		// Parallel QSim
		QSimConfigGroup qsim = config.qsim();
		qsim.setNumberOfThreads(2);
		qsim.setSnapshotPeriod(900);
	}
	
	public Config getConfig(){
		return this.config;
	}
	
}


