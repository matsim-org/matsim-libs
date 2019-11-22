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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;


public class Sim03_HomeOffice {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String runId = args[0];
		String base = args[1];
		String configFileName = args[2];
		String networkWithCapacities = args[3];
		String inputPlans = args[4];
		int qsimcores =Integer.parseInt(args[5]);
		int hdlcores =Integer.parseInt(args[6]);
		new Sim03_HomeOffice().run(runId,base,configFileName,networkWithCapacities,inputPlans,qsimcores,hdlcores);

	}

	public void run(String runId,String base, String configFilename, String networkWithCapacities, String inputPlans, int qsimcores, int hdlcores) {

//		String runId = "vw280_HomeOffice_6pct_100pct";
//		String base = "D:\\Matsim\\Axer\\Hannover\\Zim\\";
		String input = base + "input\\";
		String ouput = base + "output\\"+runId;
		Config config = ConfigUtils.loadConfig(input + configFilename,new CadytsConfigGroup());
		config.plans().setInputFile(input + "plans\\vw280_100pct.output_plans_homeOffice.xml.gz");
		config.strategy().setFractionOfIterationsToDisableInnovation(0);
		
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
		config.transit().setTransitScheduleFile(input + "transit//vw280_0.1.output_transitSchedule.xml.gz");
		config.transit().setVehiclesFile(input + "transit//vw280_0.1.output_transitVehicles.xml.gz");
		config.controler().setLastIteration(1); // Number of simulation iterations
//		config.controler().setWriteEventsInterval(2); // Write Events file every x-Iterations
//		config.controler().setWritePlansInterval(2); // Write Plan file every x-Iterations
		config.qsim().setStartTime(0);
		config.global().setNumberOfThreads(Runtime.getRuntime().availableProcessors());
		config.parallelEventHandling().setNumberOfThreads(hdlcores);
		config.qsim().setNumberOfThreads(qsimcores);
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
//		config.qsim().setFlowCapFactor(0.1);
//		config.qsim().setStorageCapFactor(0.11);
		config.controler().setRunId(runId);
		
		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks );
		config.plansCalcRoute().setRoutingRandomness( 3. );

		// vsp defaults
		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
		config.controler().setRunId(runId);

		
		
		// Run Simulation
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new SwissRailRaptorModule());
		adjustPtNetworkCapacity(controler.getScenario().getNetwork(), config.qsim().getFlowCapFactor());
		controler.run();
	}
	
	private static void adjustPtNetworkCapacity(Network network, double flowCapacityFactor){
		if (flowCapacityFactor<1.0){
			for (Link l : network.getLinks().values()){
				if (l.getAllowedModes().contains(TransportMode.pt)){
					l.setCapacity(l.getCapacity()/flowCapacityFactor);
				}
			}
		}
	}

}
