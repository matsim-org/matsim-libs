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
package playground.agarwalamit.mixedTraffic.patnaIndia.simTime;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.others.PatnaVehiclesGenerator;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.UrbanDemandGenerator;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class PatnaSimulationTimeWriter {

	private static final Logger LOG = Logger.getLogger(PatnaSimulationTimeWriter.class);
	private final int [] randomSeeds = {4711, 6835, 1847, 4144, 4628, 2632, 5982, 3218, 5736, 7573,4389, 1344} ;
	private static String runDir =  "../../../../repos/runs-svn/patnaIndia/run110/";
	private static String inputFilesDir = runDir+"/inputs/";
	
	private static String linkDynamics_CSV = "FIFO,PassingQ,SeepageQ";
	private static String trafficDynamics_CSV = "queue,withHoles";
	
	private static int cloningFactor = 1;
	private static PrintStream writer;

	public static void main(String[] args) {

		boolean isUsingCluster = false;

		if (args.length != 0) isUsingCluster = true;

		if ( isUsingCluster ) {
			runDir = args[0];
			inputFilesDir = args[1];
			cloningFactor = Integer.valueOf( args[2] );
			linkDynamics_CSV = args[3]; // for 100% scenario, all cases (72) cant be simulated only in one job
			trafficDynamics_CSV = args[4];
		}

		PatnaSimulationTimeWriter pstw = new PatnaSimulationTimeWriter();

		try {
			String outputDir = runDir+"/"+cloningFactor+"pct";
			new File(outputDir).mkdirs();
			writer = new PrintStream(outputDir+"/simTime.txt");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason : "+e);
		}

		writer.print("scenario \t simTimeInSec \n");

		List<String> lds  = Arrays.asList( linkDynamics_CSV.split(",") );
		List<String> tds = Arrays.asList(trafficDynamics_CSV.split(","));
		
		for (String ldString : lds ) {
			LinkDynamics ld = LinkDynamics.valueOf(ldString);
			for ( String tdString : tds){
				TrafficDynamics td = TrafficDynamics.valueOf(tdString);
				writer.print(ld+"_"+tdString+"\t");
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

	private void processAndWriteSimulationTime (LinkDynamics ld, TrafficDynamics td) {
		for (int i = 0; i<randomSeeds.length;i++) {
			int randomSeed = randomSeeds[i];
			MatsimRandom.reset(randomSeed);
			double startTime = System.currentTimeMillis();

			String runSpecificOutputDir = runDir+"/"+cloningFactor+"pct" + "/output_"+ld+"_"+td+"_"+i+"/";
			runControler(ld, td, runSpecificOutputDir);
			double endTime = System.currentTimeMillis();

			if(i>1 ) { // avoid two initial runs
				writer.print(String.valueOf( (endTime - startTime)/1000 ) + "\t");
			}
		}
	}

	private void runControler (LinkDynamics ld, TrafficDynamics td, String runSpecificOutputDir) {
		Config config = createBasicConfigSettings();
		String outPlans = inputFilesDir + "/SelectedPlans_clonedTo"+cloningFactor+".xml.gz";
		
		if (! new File(outPlans).exists() ) { // run only once and not for every case.
			UrbanDemandGenerator udg = new UrbanDemandGenerator(cloningFactor);
			udg.startProcessing(inputFilesDir);
			new PopulationWriter(udg.getPopulation()).write(outPlans);	
		}
		
//		BackwardCompatibilityForOldPlansType bcrt = new BackwardCompatibilityForOldPlansType(inputFilesDir+"/SelectedPlansOnly.xml", PatnaUtils.URBAN_MAIN_MODES);
//		bcrt.extractPlansExcludingLinkInfo();
//		bcrt.writePopOut(outPlans);

		config.plans().setInputFile(outPlans);

		config.network().setInputFile(inputFilesDir+"/network.xml.gz");
		config.counts().setInputFile(inputFilesDir+"counts/countsCarMotorbikeBike.xml");

		config.qsim().setLinkDynamics(ld.toString());
		config.qsim().setTrafficDynamics(td);

		if(ld.equals(LinkDynamics.SeepageQ)) {
			config.qsim().setSeepModes(Arrays.asList("bike"));
			config.qsim().setSeepModeStorageFree(false);
			config.qsim().setRestrictingSeepage(true);
		}

		config.controler().setCreateGraphs(false);
		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
		Scenario sc = ScenarioUtils.loadScenario(config);

//		//clone persons here.
//		PersonsCloner pc = new PersonsCloner(sc);
//		pc.clonePersons(cloningFactor);
		
		PatnaVehiclesGenerator.createAndAddVehiclesToScenario(sc, PatnaUtils.URBAN_MAIN_MODES);

		final Controler controler = new Controler(sc);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controler().setDumpDataAtEnd(true);

		final RandomizingTimeDistanceTravelDisutilityFactory builder = new RandomizingTimeDistanceTravelDisutilityFactory("bike", config.planCalcScore());
		
		controler.addOverridingModule(new AbstractModule() {
			// following must be added in order to get travel time and travel disutility in the router for modes other than car
			@Override
			public void install() {
				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder);
				addTravelTimeBinding("motorbike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());
			}
		});
		controler.getScenario().getConfig().controler().setOutputDirectory(runSpecificOutputDir);
		controler.run();

		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		for (int index =firstIt+1; index <lastIt; index ++){
			String dirToDel = runSpecificOutputDir+"/ITERS/it."+index;
			LOG.info("Deleting the directory "+dirToDel);
			IOUtils.deleteDirectory(new File(dirToDel),false);
		}
	}

	private Config createBasicConfigSettings(){

		Config config = ConfigUtils.createConfig();

		config.counts().setWriteCountsInterval(200);
		config.counts().setCountsScaleFactor(100/cloningFactor); 

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(200);
		//disable writing of the following data
		config.controler().setWriteEventsInterval(200);
		config.controler().setWritePlansInterval(200);

		config.qsim().setFlowCapFactor(0.01*cloningFactor);		
		config.qsim().setStorageCapFactor(0.03*cloningFactor);
		config.qsim().setEndTime(36*3600);
		config.qsim().setMainModes(PatnaUtils.URBAN_MAIN_MODES);

		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);

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

		{//activities --> urban
			ActivityParams workAct = new ActivityParams("work");
			workAct.setTypicalDuration(8*3600);
			config.planCalcScore().addActivityParams(workAct);

			ActivityParams homeAct = new ActivityParams("home");
			homeAct.setTypicalDuration(12*3600);
			config.planCalcScore().addActivityParams(homeAct);

			ActivityParams edu = new ActivityParams("educational");
			edu.setTypicalDuration(7*3600);
			config.planCalcScore().addActivityParams(edu);

			ActivityParams soc = new ActivityParams("social");
			soc.setTypicalDuration(5*3600);
			config.planCalcScore().addActivityParams(soc);

			ActivityParams oth = new ActivityParams("other");
			oth.setTypicalDuration(5*3600);
			config.planCalcScore().addActivityParams(oth);

			ActivityParams unk = new ActivityParams("unknown");
			unk.setTypicalDuration(7*3600);
			config.planCalcScore().addActivityParams(unk);
		}

		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(0);
		config.planCalcScore().setPerforming_utils_hr(6.0);

		// since demand is not calibrated for 10% or 100%, using all zeros, instead.
		ModeParams car = new ModeParams("car");
		car.setConstant(-0.0);
		car.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(car);

		ModeParams bike = new ModeParams("bike");
		bike.setConstant(0.0);
		bike.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(bike);

		ModeParams motorbike = new ModeParams("motorbike");
		motorbike.setConstant(-0.0);
		motorbike.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(motorbike);

		ModeParams pt = new ModeParams("pt");
		pt.setConstant(-0.0);
		pt.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(pt);

		ModeParams walk = new ModeParams("walk");
		walk.setConstant(0.0);
		walk.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(walk);

		config.plansCalcRoute().setNetworkModes(PatnaUtils.URBAN_MAIN_MODES);

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
