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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.joint;

import java.io.File;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter.PatnaUserGroup;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class JointConfigWriter {
	
	private static final String inputLocation = PatnaUtils.INPUT_FILES_DIR;

	private static final String NET_FILE = inputLocation+"/simulationInputs/network/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/network.xml.gz"; //
	private static final String JOINT_PLANS_10PCT = inputLocation+"/simulationInputs/joint/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/joint_plans_10pct.xml.gz"; //
	private static final String JOINT_PERSONS_ATTRIBUTE_10PCT = inputLocation+"/simulationInputs/joint/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/joint_personAttributes_10pct.xml.gz"; //
	private static final String JOINT_COUNTS_10PCT = inputLocation+"/simulationInputs/joint/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/joint_counts.xml.gz"; //
	private static final String JOINT_VEHICLES_10PCT = inputLocation+"/simulationInputs/joint/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/joint_vehicles_10pct.xml.gz";

	private final Config config = ConfigUtils.createConfig();

	public static void main(String[] args) {
		String configFileName = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/"+PatnaUtils.PCU_2W.toString()+"pcu/input/config.xml.gz";
		JointConfigWriter jcw = new JointConfigWriter();
		jcw.run();
		jcw.writeConfig(configFileName);
	}

	private void writeConfig(final String filename){
		if (new File(filename).exists()) throw new RuntimeException("A config with same file name exists. Remove it and run again.");
		new ConfigWriter(config).write(filename);	
	}

	/**
	 * This config do not have locations of inputs files (network, plans, counts etc).
	 */
	private void run() {
		config.network().setInputFile(NET_FILE);

		config.plans().setInputFile(JOINT_PLANS_10PCT);
		config.plans().setSubpopulationAttributeName(PatnaUtils.SUBPOP_ATTRIBUTE);
		config.plans().setInputPersonAttributeFile(JOINT_PERSONS_ATTRIBUTE_10PCT);

		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
		config.vehicles().setVehiclesFile(JOINT_VEHICLES_10PCT);

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(200);
		config.controler().setWriteEventsInterval(100);
		config.controler().setWritePlansInterval(100);

		config.counts().setInputFile(JOINT_COUNTS_10PCT);
		config.counts().setWriteCountsInterval(100);
		config.counts().setCountsScaleFactor(1/OuterCordonUtils.SAMPLE_SIZE);
		config.counts().setOutputFormat("all");

		config.qsim().setFlowCapFactor(OuterCordonUtils.SAMPLE_SIZE); //1.06% sample
		config.qsim().setStorageCapFactor(3*OuterCordonUtils.SAMPLE_SIZE);
		config.qsim().setEndTime(30*3600);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ);
		config.qsim().setMainModes(PatnaUtils.ALL_MAIN_MODES);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);

		{//urban
			StrategySettings expChangeBeta = new StrategySettings();
			expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
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

			config.timeAllocationMutator().setAffectingDuration(false);
			config.timeAllocationMutator().setMutationRange(7200.);

			StrategySettings modeChoice = new StrategySettings();
			modeChoice.setStrategyName(DefaultStrategy.ChangeTripMode.name());
			modeChoice.setSubpopulation(PatnaUserGroup.urban.name());
			modeChoice.setWeight(0.1);
			config.strategy().addStrategySettings(modeChoice);

			config.changeMode().setModes(PatnaUtils.URBAN_ALL_MODES.toArray(new String [PatnaUtils.URBAN_ALL_MODES.size()]));
		}

		{//commuters
			StrategySettings expChangeBeta = new StrategySettings();
			expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
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
			expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
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
		config.planCalcScore().setPerforming_utils_hr(0.30);

		for(String mode : PatnaUtils.ALL_MODES){
			ModeParams modeParam = new ModeParams(mode);
			modeParam.setConstant(0.);
			switch(mode){
			case "car":
				modeParam.setMarginalUtilityOfTraveling(-0.64);
				modeParam.setMonetaryDistanceRate(-3.7*Math.pow(10, -5)); break;
			case "motorbike" :
				modeParam.setMarginalUtilityOfTraveling(-0.18);
				modeParam.setMonetaryDistanceRate(-1.6*Math.pow(10, -5)); break;
			case "pt" :
				modeParam.setMarginalUtilityOfTraveling(-0.29);
				/* modeParam.setMonetaryDistanceRate(-0.3*Math.pow(10, -5)); */ break;
			case "walk" :
				modeParam.setMarginalUtilityOfTraveling(-0.0);
				modeParam.setMonetaryDistanceRate(0.0); 
				modeParam.setMarginalUtilityOfDistance(-0.0002); break;
			case "bike" :
				modeParam.setMarginalUtilityOfTraveling(-0.0);
				modeParam.setMonetaryDistanceRate(0.0); 
				modeParam.setMarginalUtilityOfDistance(-0.0002); 
				break;
			default :
				modeParam.setMarginalUtilityOfTraveling(0.0);
				modeParam.setMonetaryDistanceRate(0.0); break;
			}
			config.planCalcScore().addModeParams(modeParam);
		}

		config.plansCalcRoute().setNetworkModes(PatnaUtils.ALL_MAIN_MODES);

		{
			ModeRoutingParams mrp = new ModeRoutingParams("walk");
			mrp.setTeleportedModeSpeed(5./3.6);
			mrp.setBeelineDistanceFactor(1.5);
			config.plansCalcRoute().addModeRoutingParams(mrp);
		}
		{
			ModeRoutingParams mrp = new ModeRoutingParams("pt");
			mrp.setTeleportedModeSpeed(20./3.6);
			mrp.setBeelineDistanceFactor(1.5);
			config.plansCalcRoute().addModeRoutingParams(mrp);
		}
	}
}
