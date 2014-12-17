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

import java.util.Arrays;
import java.util.Collection;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.PlanStrategyRegistrar;

/**
 * @author amit
 */

public class SubpopulationConfig {

	public SubpopulationConfig() {
		config = ConfigUtils.createConfig(); 
	}

	private Config config;
	private String [] subPopulations = {"slum","nonSlum"}; 
	private Collection <String> mainModes = Arrays.asList("car","motorbike","bike");
	private String outputDir = "../../../repos/runs-svn/patnaIndia/run104/output/";

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
		
		config.plans().setSubpopulationAttributeName("incomeGroup");
		
		for(String subPop : subPopulations){
			StrategySettings timeAllocationMutator	= new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			timeAllocationMutator.setStrategyName(PlanStrategyRegistrar.Names.TimeAllocationMutator.name());
			timeAllocationMutator.setWeight(0.05);
			
			StrategySettings expChangeBeta = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			expChangeBeta.setStrategyName("ChangeExpBeta");
			expChangeBeta.setWeight(0.9);

			StrategySettings reRoute = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			reRoute.setStrategyName(PlanStrategyRegistrar.Names.ReRoute.name());
			reRoute.setWeight(0.1);

			timeAllocationMutator.setSubpopulation(subPop);
			reRoute.setSubpopulation(subPop);
			expChangeBeta.setSubpopulation(subPop);
			
			config.strategy().addStrategySettings(expChangeBeta);
			config.strategy().addStrategySettings(reRoute);
			config.strategy().addStrategySettings(timeAllocationMutator);

		}
		
		StrategySettings modeChoice = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		modeChoice.setStrategyName(PlanStrategyRegistrar.Names.ChangeLegMode.name());
		modeChoice.setWeight(0.05);

		config.setParam("changeLegMode", "modes", "bike,motorbike,pt,walk");
		modeChoice.setSubpopulation(subPopulations[0]);
		config.strategy().addStrategySettings(modeChoice);
		
		
		modeChoice = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		modeChoice.setStrategyName(PlanStrategyRegistrar.Names.ChangeLegMode.name());
		modeChoice.setWeight(0.05);
		
		config.setParam("changeLegMode", "modes", "car,bike,motorbike,pt,walk");
		modeChoice.setSubpopulation(subPopulations[1]);
		config.strategy().addStrategySettings(modeChoice);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

		config.planCalcScore().setTraveling_utils_hr(0);
		config.planCalcScore().setTravelingBike_utils_hr(0);
		config.planCalcScore().setTravelingOther_utils_hr(0);
		config.planCalcScore().setTravelingPt_utils_hr(0);
		config.planCalcScore().setTravelingWalk_utils_hr(0);
		
		config.plansCalcRoute().setNetworkModes(mainModes);
		config.plansCalcRoute().setBeelineDistanceFactor(1.0);
		
		config.plansCalcRoute().setTeleportedModeSpeed("walk", 4/3.6); 
		config.plansCalcRoute().setTeleportedModeSpeed("pt", 20/3.6);
		
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
