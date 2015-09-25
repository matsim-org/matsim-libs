/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.config.groups.TimeAllocationMutatorConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class CreateBasecase {

	public static void main(String[] args) {
		String basedir = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/";
		
		Config config = ConfigUtils.createConfig();
		ControlerConfigGroup ccg = config.controler();
		ccg.setRunId("vw006.100pct");
		ccg.setOutputDirectory(basedir+"output/"+ccg.getRunId()+"/");
		ccg.setFirstIteration(0);
		int lastIteration = 150;
		ccg.setLastIteration(lastIteration);
		int disableAfter = (int) (lastIteration * 0.9);
		ccg.setMobsim("qsim");
		ccg.setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		ccg.setWriteEventsInterval(50);
		ccg.setWritePlansInterval(50);
		config.global().setNumberOfThreads(16);
		
		QSimConfigGroup qsc = config.qsim();
		qsc.setUsingFastCapacityUpdate(true);
		qsc.setTrafficDynamics(TrafficDynamics.withHoles);
		qsc.setNumberOfThreads(16);
//		qsc.setStorageCapFactor(0.03);
//		qsc.setFlowCapFactor(0.02);
//		qsc.setEndTime(28*3600);
		config.network().setInputFile(basedir + "network.xml");
		
//		config.plans().setInputFile(basedir+"initial_plans1.0.xml.gz");
		config.plans().setInputFile(basedir+"vw005.100pct.0.plans.xml.gz");
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		
		StrategyConfigGroup scg = config.strategy();
		scg.setMaxAgentPlanMemorySize(5);
		
		StrategySettings set = new StrategySettings();
		set.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
		set.setWeight(0.7);
		scg.addParameterSet(set);
		
		StrategySettings time = new StrategySettings();
		time.setStrategyName(DefaultStrategy.TimeAllocationMutator.toString());
		time.setWeight(0.1);
		time.setDisableAfter(disableAfter);
		scg.addParameterSet(time);
		
		StrategySettings route = new StrategySettings();
		route.setStrategyName(DefaultStrategy.ReRoute.toString());
		route.setWeight(0.1);
		route.setDisableAfter(disableAfter);
		scg.addParameterSet(route);
		
//		StrategySettings tour = new StrategySettings();
//		tour.setStrategyName(DefaultStrategy.SubtourModeChoice.toString());
//		tour.setWeight(0.1);
//		tour.setDisableAfter(disableAfter);
//		scg.addParameterSet(tour);
		
		
		TimeAllocationMutatorConfigGroup tamcg = config.timeAllocationMutator();
		tamcg.setMutationRange(7200);
		tamcg.setAffectingDuration(false);
		
		SubtourModeChoiceConfigGroup smc = config.subtourModeChoice();
		smc.setConsiderCarAvailability(false);
		smc.setModes(new String[]{"pt","bike","walk","car"});
						
		VspExperimentalConfigGroup vsp = config.vspExperimental();
		vsp.setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.abort);
		
		PlansCalcRouteConfigGroup pcg = config.plansCalcRoute();
		ModeRoutingParams bike = new ModeRoutingParams();
		bike.setMode("bike");
		bike.setBeelineDistanceFactor(1.3);
		bike.setTeleportedModeSpeed(4.1667);
		pcg.addModeRoutingParams(bike);;

		ModeRoutingParams walk = new ModeRoutingParams();
		walk.setMode("walk");
		walk.setBeelineDistanceFactor(1.3);
		walk.setTeleportedModeSpeed(0.8333333);
		pcg.addModeRoutingParams(walk);
		
		ModeRoutingParams pt = new ModeRoutingParams();
		pt.setMode("pt");
		pt.setTeleportedModeFreespeedFactor(2.0);
		pcg.addModeRoutingParams(pt);
		
		PlanCalcScoreConfigGroup pcs = config.planCalcScore();
		
		
		ActivityParams home = new ActivityParams();
		home.setActivityType("home");
		home.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		home.setTypicalDuration(3600*14);
		pcs.addActivityParams(home);
		
		//shift workers home
		ActivityParams home2 = new ActivityParams();
		home2.setActivityType("homeD");
		home2.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		home2.setTypicalDuration(3600*14);
		home2.setOpeningTime(6*3600);
		home2.setClosingTime(3600*21.75);
		pcs.addActivityParams(home2);
		
		ActivityParams school = new ActivityParams();
		school.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		school.setActivityType("school");
		school.setTypicalDuration(3600*6);
		school.setOpeningTime(8*3600);
		school.setMinimalDuration(1);
		school.setClosingTime(16*3600);
		pcs.addActivityParams(school);
		
		ActivityParams university = new ActivityParams();
		university.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		university.setActivityType("university");
		university.setTypicalDuration(3600*6);
		university.setOpeningTime(8*3600);
		university.setClosingTime(16*3600);
		pcs.addActivityParams(university);
		
		ActivityParams work = new ActivityParams();
		work.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		work.setActivityType("work");
		work.setTypicalDuration(3600*8);
		work.setOpeningTime(7*3600);
		work.setClosingTime(18*3600);
		pcs.addActivityParams(work);
		
		ActivityParams vwf = new ActivityParams();
		vwf.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		vwf.setActivityType("work_vw_flexitime");
		vwf.setTypicalDuration(3600*7.75);
		
		vwf.setOpeningTime(7.5*3600);
		vwf.setLatestStartTime(9.5*3600);
		vwf.setEarliestEndTime(14.5*3600);
		vwf.setClosingTime(17.75*3600);
		pcs.addActivityParams(vwf);
		
		ActivityParams vw1 = new ActivityParams();
		vw1.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		vw1.setActivityType("work_vw_shift1");
		vw1.setTypicalDuration(3600*7.5);
		vw1.setOpeningTime(6*3600);
		vw1.setLatestStartTime(6.25*3600);
		vw1.setClosingTime(13.75*3600);
		pcs.addActivityParams(vw1);
		
		
		ActivityParams shop = new ActivityParams();
		shop.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		shop.setActivityType("shopping");
		shop.setTypicalDuration(3600);
		shop.setOpeningTime(6*3600);
		shop.setClosingTime(21*3600);
		pcs.addActivityParams(shop);

		ActivityParams priv = new ActivityParams();
		priv.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		priv.setActivityType("private");
		priv.setTypicalDuration(3600);
		priv.setOpeningTime(6*3600);
		priv.setClosingTime(23*3600);
		pcs.addActivityParams(priv);
		
		ActivityParams free = new ActivityParams();
		free.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		free.setActivityType("leisure");
		free.setTypicalDuration(3600);
		free.setOpeningTime(6*3600);
		free.setClosingTime(23*3600);
		pcs.addActivityParams(free);
		
		
		ActivityParams vw2 = new ActivityParams();
		vw2.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		vw2.setActivityType("work_vw_shift2");
		vw2.setTypicalDuration(3600*7.75);
		vw2.setOpeningTime(14*3600);
		vw2.setClosingTime(21.75*3600);
		pcs.addActivityParams(vw2);
		
		ActivityParams vw3 = new ActivityParams();
		vw3.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		vw3.setActivityType("work_vw_shift3");
		vw3.setTypicalDuration(3600*7.75);
		pcs.addActivityParams(vw3);
		
		ActivityParams cargo = new ActivityParams();
		cargo.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		cargo.setActivityType("cargo");
//		cargo.setScoringThisActivityAtAll(false);
		cargo.setTypicalDuration(3600*18);
		pcs.addActivityParams(cargo);


		ActivityParams cargoD = new ActivityParams();
		cargoD.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		cargoD.setActivityType("cargoD");
//		cargoD.setScoringThisActivityAtAll(false);

		cargoD.setOpeningTime(0.5*3600);
		cargoD.setClosingTime(19*3600);
		cargoD.setTypicalDuration(3600*2);
		
		pcs.addActivityParams(cargoD);
		
		ActivityParams deliv = new ActivityParams();
		deliv.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		deliv.setOpeningTime(0.5*3600);
		deliv.setClosingTime(20*3600);
		deliv.setActivityType("delivery");
//		deliv.setScoringThisActivityAtAll(false);
	
		deliv.setTypicalDuration(3600*2);
		pcs.addActivityParams(deliv);
		
		ActivityParams source = new ActivityParams();
		source.setTypicalDuration(3600*18);
//		source.setScoringThisActivityAtAll(false);
		source.setActivityType("source");
		pcs.addActivityParams(source);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.run();
		
	}

}
