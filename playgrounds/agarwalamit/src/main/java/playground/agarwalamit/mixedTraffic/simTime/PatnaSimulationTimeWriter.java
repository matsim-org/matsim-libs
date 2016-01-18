/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.simTime;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import playground.agarwalamit.mixedTraffic.patnaIndia.evac.EvacPatnaControler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.plans.BackwardCompatibilityForOldPlansType;

/**
 * @author amit
 */

public class PatnaSimulationTimeWriter {

	private final int [] randomNumbers = {4711, 6835, 1847, 4144, 4628, 2632, 5982, 3218, 5736, 7573,4389, 1344} ;
	private static String outputFolder =  "../../../../repos/runs-svn/patnaIndia/run107/";
	private static String inputFilesDir = "../../../../repos/runs-svn/patnaIndia/inputs/";
	private static PrintStream writer;

	public static void main(String[] args) {

		boolean isUsingCluster = false;

		if (args.length != 0) isUsingCluster = true;

		if ( isUsingCluster ) {
			outputFolder = args[0];
			inputFilesDir = args[1];
		}

		PatnaSimulationTimeWriter pstw = new PatnaSimulationTimeWriter();

		try {
			writer = new PrintStream(outputFolder+"/simTime.txt");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason : "+e);
		}

		writer.print("scenario \t simTimeInSec \n");

		for (LinkDynamics ld : LinkDynamics.values() ) {
			for ( TrafficDynamics td : TrafficDynamics.values()){
				writer.print(ld+"_"+td+"\t");
				pstw.processAndWriteSimulationTime(ld, td);
				writer.println();	
			}
		}

		try {
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason : "+e);
		}
	}

	private void processAndWriteSimulationTime (QSimConfigGroup.LinkDynamics ld, QSimConfigGroup.TrafficDynamics td) {
		for (int i = 0; i<randomNumbers.length;i++) {
			int randomSeed = randomNumbers[i];
			MatsimRandom.reset(randomSeed);
			double startTime = System.currentTimeMillis();

			String runSpecificOutputDir = outputFolder + "/output_"+ld+"_"+td+"_"+i+"/";
			runControler(ld, td, runSpecificOutputDir);
			double endTime = System.currentTimeMillis();

			if(i>1 ) { // avoid two initial runs
				writer.print(String.valueOf( (endTime - startTime)/1000 ) + "\t");
			}
		}
	}

	private void runControler (QSimConfigGroup.LinkDynamics ld, QSimConfigGroup.TrafficDynamics td, String runSpecificOutputDir) {
		Collection <String> mainModes = Arrays.asList("car","motorbike","bike");
		Config config = createBasicConfigSettings();
		String outPlans = inputFilesDir + "/SelectedPlans_new.xml.gz";

		BackwardCompatibilityForOldPlansType bcrt = new BackwardCompatibilityForOldPlansType(inputFilesDir+"/SelectedPlansOnly.xml", mainModes);
		bcrt.extractPlansExcludingLinkInfo();
		bcrt.writePopOut(outPlans);

		config.plans().setInputFile(outPlans);

		config.network().setInputFile(inputFilesDir+"/network.xml");
		config.counts().setCountsFileName(inputFilesDir+"counts/countsCarMotorbikeBike.xml");

		config.qsim().setLinkDynamics(ld.toString());
		config.qsim().setTrafficDynamics(td);

		if(ld.equals(QSimConfigGroup.LinkDynamics.SeepageQ)) {
			config.qsim().setSeepMode("bike");
			config.qsim().setSeepModeStorageFree(false);
			config.qsim().setRestrictingSeepage(true);
		}

		config.controler().setCreateGraphs(false);
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
		Scenario sc = ScenarioUtils.loadScenario(config);

		Logger.getLogger(PatnaSimulationTimeWriter.class).error("Check the modes in the following call first. jan 16");
//		PatnaUtils.createAndAddVehiclesToScenario(sc);

		final Controler controler = new Controler(sc);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controler().setDumpDataAtEnd(false);

		final RandomizingTimeDistanceTravelDisutility.Builder builder = new RandomizingTimeDistanceTravelDisutility.Builder("bike");
		
		controler.addOverridingModule(new AbstractModule() {
			// following must be added in order to get travel time and travel disutility in the router for modes other than car
			@Override
			public void install() {
				addTravelTimeBinding("bike").to(FreeSpeedTravelTime.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder);
				// alternatively test -->  addTravelDisutilityFactoryBinding("bike").to(carTravelDisutilityFactoryKey()); after removing builder injection line.
//				addTravelTimeBinding("bike").to(networkTravelTime());
//				addTravelDisutilityFactoryBinding("bike").to(carTravelDisutilityFactoryKey());
				addTravelTimeBinding("motorbike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());
			}
		});
		controler.getScenario().getConfig().controler().setOutputDirectory(runSpecificOutputDir);
		controler.run();
	}

	private Config createBasicConfigSettings(){
		Collection <String> mainModes = Arrays.asList("car","motorbike","bike");

		Config config = ConfigUtils.createConfig();

		config.counts().setWriteCountsInterval(0);
		config.counts().setCountsScaleFactor(94.52); 

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(200);
		//disable writing of the following data
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);

		config.qsim().setFlowCapFactor(0.011);		//1.06% sample
		config.qsim().setStorageCapFactor(0.033);
		config.qsim().setEndTime(36*3600);
		config.qsim().setMainModes(mainModes);


		config.setParam("TimeAllocationMutator", "mutationAffectsDuration", "false");
		config.setParam("TimeAllocationMutator", "mutationRange", "7200.0");

		StrategySettings expChangeBeta = new StrategySettings();
		expChangeBeta.setStrategyName("ChangeExpBeta");
		expChangeBeta.setWeight(0.85);

		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.1);

		StrategySettings timeAllocationMutator	= new StrategySettings();
		timeAllocationMutator.setStrategyName("TimeAllocationMutator");
		timeAllocationMutator.setWeight(0.05);

		config.strategy().addStrategySettings(expChangeBeta);
		config.strategy().addStrategySettings(reRoute);
		config.strategy().addStrategySettings(timeAllocationMutator);

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");

		ActivityParams workAct = new ActivityParams("work");
		workAct.setTypicalDuration(8*3600);
		config.planCalcScore().addActivityParams(workAct);

		ActivityParams homeAct = new ActivityParams("home");
		homeAct.setTypicalDuration(12*3600);
		config.planCalcScore().addActivityParams(homeAct);

		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(0);
		config.planCalcScore().setPerforming_utils_hr(6.0);

		ModeParams car = new ModeParams("car");
		car.setConstant(-3.30);
		car.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(car);

		ModeParams bike = new ModeParams("bike");
		bike.setConstant(0.0);
		bike.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(bike);

		ModeParams motorbike = new ModeParams("motorbike");
		motorbike.setConstant(-2.20);
		motorbike.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(motorbike);

		ModeParams pt = new ModeParams("pt");
		pt.setConstant(-3.40);
		pt.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(pt);

		ModeParams walk = new ModeParams("walk");
		walk.setConstant(0.0);
		walk.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(walk);

		config.plansCalcRoute().setNetworkModes(mainModes);

		{
			ModeRoutingParams mrp = new ModeRoutingParams("walk");
			mrp.setTeleportedModeSpeed(4./3.6);
			config.plansCalcRoute().addModeRoutingParams(mrp);
		}

		{
			ModeRoutingParams mrp = new ModeRoutingParams("pt");
			mrp.setTeleportedModeSpeed(20./3.6);
			config.plansCalcRoute().addModeRoutingParams(mrp);
		}
		return config;
	}
}
