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

import org.matsim.core.basic.v01.IdImpl;
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
		config.simulation().setStartTime(0);
		config.simulation().setEndTime(86400);
		config.simulation().setSnapshotPeriod(0);
		config.simulation().setFlowCapFactor(1.0);
		config.simulation().setStorageCapFactor(1.0);

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
		StrategySettings s1 = new StrategySettings(new IdImpl("1"));
		s1.setModuleName("SelectExpBeta");
		s1.setProbability(0.80);
		config.strategy().addStrategySettings(s1);
		//---------------------------------------------------------------------
		StrategySettings s2 = new StrategySettings(new IdImpl("2"));
		s2.setModuleName("ReRoute");
		s2.setProbability(0.10);
		config.strategy().addStrategySettings(s2);
		//---------------------------------------------------------------------
		StrategySettings s3 = new StrategySettings(new IdImpl("3"));
		s3.setModuleName("TimeAllocationMutator");
		s3.setProbability(0.10);
		config.strategy().addStrategySettings(s3);
		//---------------------------------------------------------------------

		
		// Parallel QSim
		QSimConfigGroup qsim = new QSimConfigGroup();
		qsim.setNumberOfThreads(2);
		qsim.setSnapshotPeriod(900);
		config.addQSimConfigGroup(qsim);
	}
	
	public Config getConfig(){
		return this.config;
	}
	
}


