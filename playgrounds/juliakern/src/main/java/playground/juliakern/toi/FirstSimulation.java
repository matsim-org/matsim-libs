/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.juliakern.toi;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class FirstSimulation {
 
	static String networkFile = "input/oslo/trondheim_network_with_lanes.xml";
	//static String cvsSplitBy = ",";
	static String outputDir = "output/oslo/";
	//static String plansFile = "input/oslo/plans_from_csv.xml";
	static String plansFile = "input/oslo/plans_from_eksport_stort_datasett.xml";
	//static String plansFile = "input/oslo/plans_from_start_og_random.xml";
	private static int numberOfIterations = 200;
	//static String plansFile = "input/oslo/smallpop.xml";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		/*
		 * TODO boundingbox aus den plänen auslesen
		 * mit osmosis netzwerk generieren
		 */
		

		Config config = ConfigUtils.createConfig();	
		config.addCoreModules();
		config.controler().setLastIteration(numberOfIterations );
		config.controler().setOutputDirectory(outputDir);
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);
		//config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
		
	
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(13 * 3600);
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(8 * 3600);
		ActivityParams other = new ActivityParams("other");
		other.setTypicalDuration(1*3600);
		ActivityParams comm = new ActivityParams("commute");
		comm.setTypicalDuration(1*3600.);
		config.planCalcScore().addActivityParams(home);
		config.planCalcScore().addActivityParams(work);
		config.planCalcScore().addActivityParams(other);
		config.planCalcScore().addActivityParams(comm);
		
		config.qsim().setEndTime(60*60*24.);
		config.qsim().setRemoveStuckVehicles(true);
		
		// find a shortest route
		StrategySettings reRoute = new StrategySettings(Id.create(1, StrategySettings.class));
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.2);
		reRoute.setDisableAfter(50);
		config.strategy().addStrategySettings(reRoute);
		
		// choose one of existing plans
		StrategySettings change = new StrategySettings(Id.create(2, StrategySettings.class));
		change.setStrategyName("ChangeExpBeta");
		change.setWeight(0.3); //TODO decrease later
		config.strategy().addStrategySettings(change);
		
		// change leg mode of a plan
		StrategySettings modechoice = new StrategySettings(Id.create(3, StrategySettings.class));
		modechoice.setStrategyName("ChangeLegMode");
		modechoice.setWeight(0.05);
		modechoice.setDisableAfter(50);
		config.strategy().addStrategySettings(modechoice);
		
		// shift start times 
		StrategySettings timeAll = new StrategySettings(Id.create(4, StrategySettings.class));
		timeAll.setStrategyName("TimeAllocationMutator");
		timeAll.setWeight(0.1);
		timeAll.setDisableAfter(50);
		String mutationRange = Double.toString(60.*15);		
		config.strategy().addStrategySettings(timeAll);
		config.setParam("TimeAllocationMutator", "mutationRange", mutationRange);
		
		// keep last plan = do nothing
		StrategySettings keep = new StrategySettings(Id.create(5, StrategySettings.class));
		keep.setStrategyName("KeepLastSelected");
		keep.setWeight(0.3);
		config.strategy().addStrategySettings(keep);
		
		
		// timeallocation mutator
		// keep last selected
		// select exp beta (choose randomly but weighted by score)
		/*
		 * <module name="planscalcroute" > 
    <param name="beelineDistanceFactor" value="1.3" /> 
    <param name="bikeSpeed" value="4.166666666666667" /> 
    <param name="ptSpeedFactor" value="2.0" /> 
    <param name="undefinedModeSpeed" value="13.88888888888889" /> 
    <param name="walkSpeed" value="0.8333333333333333" /> 
</module>
		 */
 
		
//		System.out.println(config.plansCalcRoute().getBeelineDistanceFactor());
		// this is now mode-specific, but I don't know to which mode this refers. kai, feb'15
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		new MatsimNetworkReader(scenario).readFile(networkFile);
		
//		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();
//		PopulationFactory populationFactory = population.getFactory();


		new MatsimPopulationReader(scenario).readFile(plansFile);	
		
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setMobsim("qsim");
		NetworkConfigGroup ncg = controler.getConfig().network();
		ncg.setInputFile(networkFile);
		controler.getConfig().plans().setInputFile(plansFile);
				 
		controler.getEvents().addHandler(new TollHandler(population, controler, scenario));
		
		controler.getConfig().setParam("global", "numberOfThreads", "2");
		controler.getConfig().setParam("controler", "routingAlgorithmType" , "AStarLandmarks");
		
		controler.run();
	}

}
