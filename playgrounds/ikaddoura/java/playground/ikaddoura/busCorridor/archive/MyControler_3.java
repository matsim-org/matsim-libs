/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler.java
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.archive;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.pt.config.TransitConfigGroup;

/**
 * @author Ihab
 *
 */

public class MyControler_3 {



	public static void main(final String[] args) {
			
		Config config = new Config();
		
		GlobalConfigGroup global = new GlobalConfigGroup();
		global.setCoordinateSystem("Atlantis");
		global.setRandomSeed(4711);
		
		NetworkConfigGroup network = new NetworkConfigGroup();
		network.setInputFile("../../shared-svn/studies/ihab/busCorridor/input/network_busline.xml");
		
		PlansConfigGroup plans = new PlansConfigGroup();
		plans.setInputFile("../../shared-svn/studies/ihab/busCorridor/input/population.xml");
		
		ScenarioConfigGroup scenarioConfGroup = new ScenarioConfigGroup();
		scenarioConfGroup.setUseTransit(true);
		scenarioConfGroup.setUseVehicles(true);
		
		ControlerConfigGroup controlerConfGroup = new ControlerConfigGroup();
		controlerConfGroup.setFirstIteration(0);
		controlerConfGroup.setLastIteration(10);
		controlerConfGroup.setOutputDirectory("../../shared-svn/studies/ihab/busCorridor/output/busline_10buses_test");
		controlerConfGroup.addParam("eventsFileFormat", "xml");
		
		QSimConfigGroup qSim = new QSimConfigGroup();
		qSim.addParam("snapshotperiod", "00:00:01");
		qSim.addParam("snapshotFormat", "otfvis");
		qSim.addParam("snapshotStyle", "queue");
		qSim.addParam("startTime", "00:00:00");
		qSim.addParam("endTime", "30:00:00");

		PlanCalcScoreConfigGroup planCalcScore = new PlanCalcScoreConfigGroup();
		planCalcScore.addParam("activityType_0", "home");
		planCalcScore.addParam("activityPriority_0", "1");
		planCalcScore.addParam("activityTypicalDuration_0", "12:00:00");
		planCalcScore.addParam("activityMinimalDuration_0", "08:00:00");
		
		planCalcScore.addParam("activityType_1", "work");
		planCalcScore.addParam("activityPriority_1", "1");
		planCalcScore.addParam("activityTypicalDuration_1", "08:00:00");
		planCalcScore.addParam("activityMinimalDuration_1", "06:00:00");
		planCalcScore.addParam("activityOpeningTime_1", "07:00:00");
		planCalcScore.addParam("activityLatestStartTime_1", "09:00:00");
		planCalcScore.addParam("activityEarliestEndTime_1", "");
		planCalcScore.addParam("activityClosingTime_1", "18:00:00");
		
		planCalcScore.addParam("activityType_2", "pt interaction");
		planCalcScore.addParam("activityPriority_2", "1");
		planCalcScore.addParam("activityTypicalDuration_2", "00:01:30");
		planCalcScore.addParam("activityMinimalDuration_2", "00:01:00");
			
		planCalcScore.addParam("activityType_3", "shopping");
		planCalcScore.addParam("activityPriority_3", "1");
		planCalcScore.addParam("activityTypicalDuration_3", "01:00:00");
		planCalcScore.addParam("activityMinimalDuration_3", "00:30:00");

		StrategyConfigGroup strategy = new StrategyConfigGroup();
		strategy.setMaxAgentPlanMemorySize(5);
		strategy.addParam("ModuleProbability_1", "0.7");
		strategy.addParam("Module_1", "BestScore");
		strategy.addParam("ModuleProbability_2", "0.1");
		strategy.addParam("Module_2", "TransitTimeAllocationMutator");

		TransitConfigGroup transit = new TransitConfigGroup();
		transit.setTransitScheduleFile("../../shared-svn/studies/ihab/busCorridor/input/transitschedule10buses.xml");
		transit.setVehiclesFile("../../shared-svn/studies/ihab/busCorridor/input/transitVehicles10buses.xml");
		transit.addParam("transitModes", "pt");
		
		config.addModule("global", global);
		config.addModule("network", network);
		config.addModule("plans", plans);
		config.addModule("scenarioConfGroup", scenarioConfGroup);
		config.addModule("controlerConfGroup", controlerConfGroup);
		config.addModule("qsim", qSim);
		config.addModule("planCalcScore", planCalcScore);
		config.addModule("strategy", strategy);
		config.addModule("transit", transit);

		Controler controler = new Controler(config);
		
		controler.setOverwriteFiles(true);			
		controler.run();
		}
}
