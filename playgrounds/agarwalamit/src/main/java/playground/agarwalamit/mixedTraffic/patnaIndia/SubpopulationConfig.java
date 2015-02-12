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
package playground.agarwalamit.mixedTraffic.patnaIndia;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author amit
 */

public class SubpopulationConfig {

	public SubpopulationConfig() {
		config = ConfigUtils.createConfig(); 
	}

	private Config config;
	private String [] subPopulations = {"slum","nonSlum"}; 
	private Collection <String> mainModes = Arrays.asList("slum_car","slum_motorbike","slum_bike","nonSlum_car","nonSlum_motorbike","nonSlum_bike");
	private  String [] allModes = {"slum_car","slum_motorbike","slum_bike","slum_pt","slum_walk","nonSlum_car","nonSlum_motorbike","nonSlum_bike","nonSlum_pt","nonSlum_walk"};
	private String outputDir = "../../../repos/runs-svn/patnaIndia/run104/c7/";

	public static void main(String[] args) {

		new SubpopulationConfig().run();
	}

	public void run(){
		//inputs
		config.network().setInputFile("../../../repos/runs-svn/patnaIndia/inputs/network.xml");

		config.plans().setInputFile("../../../repos/runs-svn/patnaIndia/inputs/plansSubPop.xml.gz");
		config.plans().setInputPersonAttributeFile("../../../repos/runs-svn/patnaIndia/inputs/personsAttributesSubPop.xml.gz");
		config.counts().setCountsFileName("../../../repos/runs-svn/patnaIndia/inputs/counts/countsCarMotorbikeBike.xml");
		config.counts().setOutputFormat("all");
		config.counts().setWriteCountsInterval(100);
		config.counts().setCountsScaleFactor(94.52); 
		config.controler().setOutputDirectory(outputDir);

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(200);
		config.controler().setMobsim("qsim");
		config.controler().setWriteEventsInterval(100);
		config.controler().setWritePlansInterval(100);
		config.controler().setWriteSnapshotsInterval(100);	
		config.controler().setSnapshotFormat(Arrays.asList("otfvis"));

		//qsim
		config.qsim().setFlowCapFactor(0.011);		//1.06% sample
		config.qsim().setStorageCapFactor(0.033);
		config.qsim().setSnapshotPeriod(5*60);
		config.qsim().setEndTime(36*3600);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ.toString());
		config.qsim().setMainModes(mainModes);

		config.setParam("TimeAllocationMutator", "mutationAffectsDuration", "false");
		config.setParam("TimeAllocationMutator", "mutationRange", "7200.0");

		config.plans().setSubpopulationAttributeName("incomeGroup");

		for(String subPop : subPopulations){
			StrategySettings timeAllocationMutator	= new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			timeAllocationMutator.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.name());
			timeAllocationMutator.setWeight(0.05);

			StrategySettings expChangeBeta = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			expChangeBeta.setStrategyName("ChangeExpBeta");
			expChangeBeta.setWeight(0.9);

			StrategySettings reRoute = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.name());
			reRoute.setWeight(0.1);

			StrategySettings modeChoice = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			modeChoice.setStrategyName("ChangeLegMode_".concat(subPop));
			modeChoice.setWeight(0.05);

			timeAllocationMutator.setSubpopulation(subPop);
			reRoute.setSubpopulation(subPop);
			expChangeBeta.setSubpopulation(subPop);
			modeChoice.setSubpopulation(subPop);

			config.strategy().addStrategySettings(expChangeBeta);
			config.strategy().addStrategySettings(reRoute);
			config.strategy().addStrategySettings(timeAllocationMutator);
			config.strategy().addStrategySettings(modeChoice);

		}

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

		for(String str : allModes){
			config.planCalcScore().getOrCreateModeParams(str).setConstant(0.);
			config.planCalcScore().getOrCreateModeParams(str).setMarginalUtilityOfTraveling(0.);
		}
		
		config.plansCalcRoute().setNetworkModes(mainModes);
		config.plansCalcRoute().setBeelineDistanceFactor(1.0);

		config.plansCalcRoute().getTeleportedModeSpeeds().put("slum_walk", 4/3.6);
		config.plansCalcRoute().getTeleportedModeSpeeds().put("nonSlum_walk", 4/3.6);
		config.plansCalcRoute().getTeleportedModeSpeeds().put("slum_pt", 20/3.6);
		config.plansCalcRoute().getTeleportedModeSpeeds().put("nonSlum_pt", 20/3.6);

		ActivityParams workAct = new ActivityParams("work");
		workAct.setTypicalDuration(8*3600);
		config.planCalcScore().addActivityParams(workAct);

		ActivityParams homeAct = new ActivityParams("home");
		homeAct.setTypicalDuration(12*3600);
		config.planCalcScore().addActivityParams(homeAct);

		new ConfigWriter(config).write(outputDir+"/configSubPop.xml");
	}

	public Config getPatnaConfig(){
		return this.config;
	}

}
