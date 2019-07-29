package vwExamples.Zim;

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

import java.util.Random;

//import org.matsim.core.population.io.PopulationReader;

/**
 * @author jbischoff
 * This is an example how to set different flow capacity consumptions for different vehicles.
 * Two groups of agents, one equipped with AVs (having an improved flow of factor 2), the other one using ordinary cars are traveling on two different routes in a grid network
 * , highlighting the difference between vehicles.
 * Network flow capacities are the same on all links.
 * All agents try to depart at the same time. The queue is emptied twice as fast for the agents using an AV.
 */

/**
 *
 */
public class Sim03_HomeOffice {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		new Sim03_HomeOffice().run(false);

	}

	public void run(boolean otfvis) {

		String runId = "VW243_HomeOfficeInOut_10pct";
		String base = "D:\\Matsim\\Axer\\Hannover\\Zim\\";
		String input = base + "input\\";
		String ouput = base + "output\\"+runId;
		Config config = ConfigUtils.loadConfig(input + "Sim03_HomeOffice.xml", new OTFVisConfigGroup());
		config.plans().setInputFile(input + "plans\\vw243_cadON_ptSpeedAdj.0.1_homeOffice_InOut.output_plans.xml.gz");
		

		// StrategySettings strategySettings = new StrategySettings();
		// strategySettings.addParam("KeepLastSelected", "KeepLastSelected");
		// strategySettings.setStrategyName("KeepLastSelected");
		// config.strategy().addStrategySettings(strategySettings);
		
		
		PlanCalcScoreConfigGroup.ModeParams scoreParams =  new PlanCalcScoreConfigGroup.ModeParams("stayHome");
		config.planCalcScore().addModeParams(scoreParams);
		
		PlansCalcRouteConfigGroup.ModeRoutingParams params = new PlansCalcRouteConfigGroup.ModeRoutingParams();
		params.setMode("stayHome");
		params.setTeleportedModeFreespeedLimit(100000d);
		params.setTeleportedModeSpeed(100000d);
		params.setBeelineDistanceFactor(1.3);
		config.plansCalcRoute().addModeRoutingParams(params);

		config.planCalcScore().addModeParams(scoreParams);


		config.controler().setOutputDirectory(ouput);
		config.network().setInputFile(input + "network\\network.xml.gz");
		config.transit().setTransitScheduleFile(input + "transit\\transitschedule.xml");
		config.transit().setVehiclesFile(input + "transit\\transitvehicles.xml");
		config.controler().setLastIteration(2); // Number of simulation iterations
		config.controler().setWriteEventsInterval(2); // Write Events file every x-Iterations
		config.controler().setWritePlansInterval(2); // Write Plan file every x-Iterations
		config.qsim().setStartTime(0);
		config.qsim().setNumberOfThreads(16);
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		config.qsim().setFlowCapFactor(0.1);
		config.qsim().setStorageCapFactor(0.11);
		config.controler().setRunId(runId);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Run Simulation
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new SwissRailRaptorModule());
		controler.run();
	}

}
