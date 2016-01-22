/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.combined;

import java.util.Arrays;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;

import playground.agarwalamit.mixedTraffic.patnaIndia.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter.PatnaUserGroup;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class PatnaJointCalibrationControler {

	private final static double SAMPLE_SIZE = 0.10;
	private final static String subPopAttributeName = "userGroup";
	
	private static final String NET_FILE = "../../../../repos/shared-svn/projects/patnaIndia/inputs/simulationInputs/network_diff_linkSpeed.xml.gz"; //
	private static final String JOINT_PLANS_10PCT = "../../../../repos/shared-svn/projects/patnaIndia/inputs/simulationInputs/joint_plans_10pct.xml.gz"; //
	private static final String JOINT_PERSONS_ATTRIBUTE_10PCT = "../../../../repos/shared-svn/projects/patnaIndia/inputs/simulationInputs/joint_personAttributes_10pct.xml.gz"; //
	private static final String JOINT_COUNTS_10PCT = "../../../../repos/shared-svn/projects/patnaIndia/inputs/simulationInputs/joint_counts.xml.gz"; //
	private static final String JOINT_VEHICLES_10PCT = "../../../../repos/shared-svn/projects/patnaIndia/inputs/simulationInputs/joint_vehicles_10pct.xml.gz";
	
	private static final String OUTPUT_DIR = "../../../../repos/runs-svn/patnaIndia/run108/calibration/c1/";
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		PatnaJointCalibrationControler pjc = new PatnaJointCalibrationControler();
		
		if(args.length>0){
			ConfigUtils.loadConfig(config, args[0]);
		} else {
			config = pjc.createBasicConfigSettings();
		}

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		final Controler controler = new Controler(config);
		controler.getConfig().controler().setDumpDataAtEnd(true);

		final RandomizingTimeDistanceTravelDisutility.Builder builder_bike =  new RandomizingTimeDistanceTravelDisutility.Builder("bike");
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder_bike);
				
				addTravelTimeBinding("bike_ext").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike_ext").toInstance(builder_bike);

				for(String mode : Arrays.asList("car_ext","motorbike_ext","truck_ext","truck","motorbike")){
					addTravelTimeBinding(mode).to(networkTravelTime());
					addTravelDisutilityFactoryBinding(mode).to(carTravelDisutilityFactoryKey());					
				}
			
			}
		});
		controler.run();
	}

	/**
	 * This config do not have locations of inputs files (network, plans, counts etc).
	 */
	public Config createBasicConfigSettings () {
		
		Config config = ConfigUtils.createConfig();
		
		config.network().setInputFile(NET_FILE);
		
		config.plans().setInputFile(JOINT_PLANS_10PCT);
		config.plans().setSubpopulationAttributeName(subPopAttributeName);
		config.plans().setInputPersonAttributeFile(JOINT_PERSONS_ATTRIBUTE_10PCT);
		
		config.qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);
		config.vehicles().setVehiclesFile(JOINT_VEHICLES_10PCT);
		
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		config.controler().setWriteEventsInterval(50);
		config.controler().setWritePlansInterval(50);
		config.controler().setOutputDirectory(OUTPUT_DIR);

		config.counts().setCountsFileName(JOINT_COUNTS_10PCT);
		config.counts().setWriteCountsInterval(50);
		config.counts().setCountsScaleFactor(1/SAMPLE_SIZE);
		//ZZ_TODO : there is something about multipleModes in counts. I could not see any effect of it.

		config.qsim().setFlowCapFactor(SAMPLE_SIZE); //1.06% sample
		config.qsim().setStorageCapFactor(3*SAMPLE_SIZE);
		config.qsim().setEndTime(36*3600);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ.toString());
		config.qsim().setMainModes(PatnaUtils.ALL_MAIN_MODES);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);

		config.setParam(DefaultStrategy.TimeAllocationMutator.name(), "mutationAffectsDuration", "false");
		config.setParam(DefaultStrategy.TimeAllocationMutator.name(), "mutationRange", "7200.0");

		{//urban
			StrategySettings expChangeBeta = new StrategySettings();
			expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.name());
			expChangeBeta.setSubpopulation(PatnaUserGroup.urban.name());
			expChangeBeta.setWeight(0.7);
			config.strategy().addStrategySettings(expChangeBeta);

			StrategySettings reRoute = new StrategySettings();
			reRoute.setStrategyName(DefaultStrategy.ReRoute.name());
			reRoute.setSubpopulation(PatnaUserGroup.urban.name());
			reRoute.setWeight(0.15);
			config.strategy().addStrategySettings(reRoute);

			StrategySettings timeAllocationMutator	= new StrategySettings();
			timeAllocationMutator.setStrategyName(DefaultStrategy.TimeAllocationMutator.name());
			timeAllocationMutator.setSubpopulation(PatnaUserGroup.urban.name());
			timeAllocationMutator.setWeight(0.05);
			config.strategy().addStrategySettings(timeAllocationMutator);

			StrategySettings modeChoice = new StrategySettings();
			modeChoice.setStrategyName(DefaultStrategy.ChangeLegMode.name());
			modeChoice.setSubpopulation(PatnaUserGroup.urban.name());
			modeChoice.setWeight(0.1);
			config.strategy().addStrategySettings(modeChoice);

			config.changeLegMode().setModes(PatnaUtils.URBAN_ALL_MODES.toArray(new String [PatnaUtils.URBAN_ALL_MODES.size()]));
		}

		{//commuters
			StrategySettings expChangeBeta = new StrategySettings();
			expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.name());
			expChangeBeta.setSubpopulation(PatnaUserGroup.commuter.name());
			expChangeBeta.setWeight(0.85);
			config.strategy().addStrategySettings(expChangeBeta);

			StrategySettings reRoute = new StrategySettings();
			reRoute.setStrategyName(DefaultStrategy.ReRoute.name());
			reRoute.setSubpopulation(PatnaUserGroup.commuter.name());
			reRoute.setWeight(0.15);
			config.strategy().addStrategySettings(reRoute);
		}

		{//through
			StrategySettings expChangeBeta = new StrategySettings();
			expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.name());
			expChangeBeta.setSubpopulation(PatnaUserGroup.through.name());
			expChangeBeta.setWeight(0.85);
			config.strategy().addStrategySettings(expChangeBeta);

			StrategySettings reRoute = new StrategySettings();
			reRoute.setStrategyName(DefaultStrategy.ReRoute.name());
			reRoute.setSubpopulation(PatnaUserGroup.through.name());
			reRoute.setWeight(0.15);
			config.strategy().addStrategySettings(reRoute);
		}

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");
		config.vspExperimental().setWritingOutputEvents(true);

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
		{//activities --> commuters/through
			ActivityParams ac1 = new ActivityParams("E2E_Start");
			ac1.setTypicalDuration(10*60*60);
			config.planCalcScore().addActivityParams(ac1);

			ActivityParams act2 = new ActivityParams("E2E_End");
			act2.setTypicalDuration(10*60*60);
			config.planCalcScore().addActivityParams(act2);

			ActivityParams act3 = new ActivityParams("E2I_Start");
			act3.setTypicalDuration(12*60*60);
			config.planCalcScore().addActivityParams(act3);

			for(String area : OuterCordonUtils.getAreaType2ZoneIds().keySet()){
				ActivityParams act4 = new ActivityParams("E2I_mid_"+area.substring(0,3));
				act4.setTypicalDuration(8*60*60);
				config.planCalcScore().addActivityParams(act4);			
			}
		}

		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(0);
		config.planCalcScore().setPerforming_utils_hr(6.0);

		for(String mode : PatnaUtils.ALL_MODES){
			ModeParams modeParam = new ModeParams(mode);
			modeParam.setConstant(0.);
			modeParam.setMarginalUtilityOfTraveling(0.0);
			config.planCalcScore().addModeParams(modeParam);
		}

		config.plansCalcRoute().setNetworkModes(PatnaUtils.ALL_MAIN_MODES);

		{
			ModeRoutingParams mrp = new ModeRoutingParams("walk");
			mrp.setTeleportedModeSpeed(5./3.6);
			mrp.setBeelineDistanceFactor(1.1);
			config.plansCalcRoute().addModeRoutingParams(mrp);
		}
		{
			ModeRoutingParams mrp = new ModeRoutingParams("pt");
			mrp.setTeleportedModeSpeed(20./3.6);
			mrp.setBeelineDistanceFactor(1.5);
			config.plansCalcRoute().addModeRoutingParams(mrp);
		}
		return config;
	}
}