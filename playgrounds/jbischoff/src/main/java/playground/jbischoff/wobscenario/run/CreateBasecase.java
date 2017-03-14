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

package playground.jbischoff.wobscenario.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
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
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.utils.collections.CollectionUtils;

import playground.jbischoff.analysis.TripHistogramModule;

import javax.inject.Inject;

/**
 * @author  jbischoff
 *
 */
public class CreateBasecase {

	public static void main(String[] args) {
		boolean useCadyts = true;
		final Config config;
		final Scenario scenario;
		if (args.length>0){
			config = ConfigUtils.loadConfig(args[0], new CadytsConfigGroup() );
			boolean useCadytsConf= Boolean.parseBoolean(args[1]);
			useCadyts=useCadytsConf;
			System.out.println("using cadyts: "+useCadyts);
			scenario = ScenarioUtils.loadScenario(config);
		}
		else
		{
			config = ConfigUtils.createConfig();
			System.out.println("using cadyts: "+useCadyts);
			scenario = ScenarioUtils.loadScenario(config);
		}
		
		
		System.out.println("using cadyts: "+useCadyts);

		
		final	Controler controler = new Controler(scenario);
		controler.addOverridingModule(new TripHistogramModule());

		if (useCadyts){
		// create the cadyts context and add it to the control(l)er:

				controler.addOverridingModule(new CadytsCarModule());

				// include cadyts into the plan scoring (this will add the cadyts corrections to the scores):
				controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
					private final ScoringParametersForPerson parameters = new SubpopulationScoringParameters( scenario );
					@Inject CadytsContext cContext;
					@Override
					public ScoringFunction createNewScoringFunction(Person person) {

						final ScoringParameters params = parameters.getScoringParameters( person );
						
						SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
						scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
						scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
						scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

						final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cContext);
						final double cadytsScoringWeight = 20. * config.planCalcScore().getBrainExpBeta() ;
						scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
						scoringFunctionAccumulator.addScoringFunction(scoringFunction );

						return scoringFunctionAccumulator;
					}
				}) ;
				
		}
		controler.run();
		
	}
static void prepareConfig(Config config, boolean useCadyts){
	String basedir = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/";
//	String basedir = "/net/ils4/jbischoff/input/";
	double scale = 0.01;
	
	ControlerConfigGroup ccg = config.controler();
	ccg.setRunId("vw043");
	ccg.setOutputDirectory(basedir+"output/"+ccg.getRunId()+"/");
	ccg.setFirstIteration(0);
	int lastIteration = 50;
	ccg.setLastIteration(lastIteration);
	int disableAfter = (int) (lastIteration * 0.8);
	ccg.setMobsim("qsim");
	ccg.setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
	ccg.setWriteEventsInterval(50);
	ccg.setWritePlansInterval(50);
	config.global().setNumberOfThreads(16);
	
	QSimConfigGroup qsc = config.qsim();
	qsc.setUsingFastCapacityUpdate(true);
	qsc.setTrafficDynamics(TrafficDynamics.withHoles);
	qsc.setNumberOfThreads(6);
	qsc.setStorageCapFactor(1);
	qsc.setFlowCapFactor(2);
	qsc.setEndTime(30*3600);
	
	config.parallelEventHandling().setNumberOfThreads(6);
	
	config.network().setInputFile(basedir + "networkptcgt.xml");
	
	
	config.plans().setInputFile(basedir+"initial_plans0.01.xml.gz");
	config.plans().setInputPersonAttributeFile(basedir+"initial_plans_oA0.01.xml.gz");
	
	config.plans().setRemovingUnneccessaryPlanAttributes(true);
	
	config.transit().setTransitModes(CollectionUtils.stringToSet("pt"));
	config.transit().setTransitScheduleFile(basedir+"transitschedule.xml");
	config.transit().setVehiclesFile(basedir+"transitvehicles.xml");
	config.transit().setUseTransit(true);
	config.transitRouter().setExtensionRadius(500);

	
	CountsConfigGroup counts = config.counts();
	counts.setAnalyzedModes("car");
	counts.setInputFile(basedir+"counts.xml");
	counts.setCountsScaleFactor(1.0/scale);
	
	if (useCadyts){
	CadytsConfigGroup cadyts = (CadytsConfigGroup) config.getModule("cadytsCar");
	cadyts.setStartTime(6*3600);
	cadyts.setEndTime(21*3600+1);
	cadyts.setTimeBinSize(3600);
	cadyts.addParam("calibratedLinks","65601,48358,62489,71335,44441,53098" );
	}
	
	StrategyConfigGroup scg = config.strategy();
	scg.setMaxAgentPlanMemorySize(5);
	
	{
	StrategySettings set = new StrategySettings();
	set.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
	set.setWeight(0.7);
	set.setSubpopulation("teleportPt");
	scg.addParameterSet(set);
	
	StrategySettings time = new StrategySettings();
	time.setStrategyName(DefaultStrategy.TimeAllocationMutator.toString());
	time.setWeight(0.1);
	time.setDisableAfter(disableAfter);
	time.setSubpopulation("teleportPt");
	scg.addParameterSet(time);
	
	StrategySettings route = new StrategySettings();
	route.setStrategyName(DefaultStrategy.ReRoute.toString());
	route.setWeight(0.1);
	route.setDisableAfter(disableAfter);
	route.setSubpopulation("teleportPt");
	scg.addParameterSet(route);

	StrategySettings sl = new StrategySettings();
	sl.setStrategyName(DefaultStrategy.ChangeLegMode.toString());
	sl.setWeight(0.1);
	sl.setSubpopulation("teleportPt");
	sl.setDisableAfter(disableAfter);
	scg.addParameterSet(sl);
	}
	
	{
		StrategySettings set = new StrategySettings();
		set.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
		set.setWeight(0.7);
		set.setSubpopulation("schedulePt");
		scg.addParameterSet(set);
		
		StrategySettings time = new StrategySettings();
		time.setStrategyName(DefaultStrategy.TimeAllocationMutator.toString());
		time.setWeight(0.1);
		time.setDisableAfter(disableAfter);
		time.setSubpopulation("schedulePt");
		scg.addParameterSet(time);
		
		StrategySettings route = new StrategySettings();
		route.setStrategyName(DefaultStrategy.ReRoute.toString());
		route.setWeight(0.1);
		route.setDisableAfter(disableAfter);
		route.setSubpopulation("schedulePt");
		scg.addParameterSet(route);
		
		StrategySettings tour = new StrategySettings();
		tour.setStrategyName(DefaultStrategy.SubtourModeChoice.toString());
		tour.setWeight(0.1);
		tour.setSubpopulation("schedulePt");
		tour.setDisableAfter(disableAfter);
		scg.addParameterSet(tour);
		

	}
	
	{
		StrategySettings set = new StrategySettings();
		set.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
		set.setWeight(0.9);
		set.setSubpopulation("noRep");
		scg.addParameterSet(set);

		StrategySettings route = new StrategySettings();
		route.setStrategyName(DefaultStrategy.ReRoute.toString());
		route.setWeight(0.1);
		route.setDisableAfter(disableAfter);
		route.setSubpopulation("noRep");
		scg.addParameterSet(route);
	}
		
	
	
	TimeAllocationMutatorConfigGroup tamcg = config.timeAllocationMutator();
	tamcg.setMutationRange(5400);

	tamcg.setAffectingDuration(false);
	
	SubtourModeChoiceConfigGroup smc = config.subtourModeChoice();
	smc.setConsiderCarAvailability(false);
	smc.setModes(new String[]{"pt","bike","walk","car"});
	
	config.setParam("changeLegMode", "mode", "tpt,car");
	
					
	VspExperimentalConfigGroup vsp = config.vspExperimental();
	vsp.setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
	
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
	
	ModeRoutingParams tpt = new ModeRoutingParams();
	tpt.setMode("tpt");
	tpt.setTeleportedModeFreespeedFactor(2.0);
	pcg.addModeRoutingParams(tpt);
	

	
	
	PlanCalcScoreConfigGroup pcs = config.planCalcScore();
	
	pcs.setLateArrival_utils_hr(-24.0);
	pcs.setPerforming_utils_hr(6.0);
	pcs.setEarlyDeparture_utils_hr(-6.0);
	
	ModeParams car = new ModeParams("car");
	car.setMarginalUtilityOfTraveling(-4.0);
	car.setConstant(-10.0);
	pcs.addModeParams(car);
	
	ModeParams mtpt = new ModeParams("tpt");
	mtpt.setConstant(-1.0);
	mtpt.setMarginalUtilityOfTraveling(-0.5);
	pcs.addModeParams(mtpt);
	
	ModeParams mspt = new ModeParams("pt");
	mspt.setConstant(-1.0);
	mspt.setMarginalUtilityOfTraveling(-0.5);
	pcs.addModeParams(mspt);
	
	ModeParams mwalk = new ModeParams("walk");
	mwalk.setConstant(-0.0);
	mwalk.setMarginalUtilityOfTraveling(-6.0);
	pcs.addModeParams(mwalk);
	
	ModeParams mbike = new ModeParams("bike");
	mbike.setConstant(-5.0);
	mbike.setMarginalUtilityOfTraveling(-8.0);
	pcs.addModeParams(mbike);
	
	
	ActivityParams home = new ActivityParams();
	home.setActivityType("home");
	home.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	home.setTypicalDuration(3600*7);
	pcs.addActivityParams(home);
	
	//shift workers home
	ActivityParams home2 = new ActivityParams();
	home2.setActivityType("homeD");
	home2.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	home2.setTypicalDuration(3600*8);
	home2.setOpeningTime(6*3600);
	home2.setClosingTime(3600*21.75);
	pcs.addActivityParams(home2);
	
	ActivityParams school = new ActivityParams();
	school.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	school.setActivityType("school");
	school.setTypicalDuration(3600*6);
	school.setOpeningTime(8*3600);
	school.setMinimalDuration(1);
	school.setClosingTime(16*3600);
	pcs.addActivityParams(school);
	
	ActivityParams university = new ActivityParams();
	university.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	university.setActivityType("university");
	university.setTypicalDuration(3600*6);
	university.setOpeningTime(8*3600);
	university.setClosingTime(16*3600);
	pcs.addActivityParams(university);
	
	ActivityParams work = new ActivityParams();
	work.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	work.setActivityType("work");
	work.setTypicalDuration(3600*8);
	work.setOpeningTime(7*3600);
	work.setClosingTime(18*3600);
	pcs.addActivityParams(work);
	
	ActivityParams vwf = new ActivityParams();
	vwf.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	vwf.setActivityType("work_vw_flexitime");
	vwf.setTypicalDuration(3600*7.75);
	
	vwf.setOpeningTime(7.5*3600);
	vwf.setLatestStartTime(9*3600);
	vwf.setEarliestEndTime(14.5*3600);
	vwf.setClosingTime(17.75*3600);
	pcs.addActivityParams(vwf);
	
	ActivityParams vw1 = new ActivityParams();
	vw1.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	vw1.setActivityType("work_vw_shift1");
	vw1.setTypicalDuration(3600*7.5);
	vw1.setOpeningTime(6*3600);
	vw1.setLatestStartTime(6.25*3600);
	vw1.setClosingTime(13.75*3600);
	pcs.addActivityParams(vw1);
	
	
	ActivityParams shop = new ActivityParams();
	shop.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	shop.setActivityType("shopping");
	shop.setTypicalDuration(3600);
	shop.setOpeningTime(6*3600);
	shop.setClosingTime(21*3600);
	pcs.addActivityParams(shop);

	ActivityParams priv = new ActivityParams();
	priv.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	priv.setActivityType("private");
	priv.setTypicalDuration(3600);
	priv.setOpeningTime(6*3600);
	priv.setClosingTime(23*3600);
	pcs.addActivityParams(priv);
	
	ActivityParams free = new ActivityParams();
	free.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	free.setActivityType("leisure");
	free.setTypicalDuration(3600);
	free.setOpeningTime(6*3600);
	free.setClosingTime(23*3600);
	pcs.addActivityParams(free);
	
	
	ActivityParams vw2 = new ActivityParams();
	vw2.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	vw2.setActivityType("work_vw_shift2");
	vw2.setTypicalDuration(3600*7.75);
	vw2.setOpeningTime(14*3600);
	vw2.setClosingTime(21.75*3600);
	pcs.addActivityParams(vw2);
	
	ActivityParams vw3 = new ActivityParams();
	vw3.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	vw3.setActivityType("work_vw_shift3");
	vw3.setTypicalDuration(3600*7.75);
	pcs.addActivityParams(vw3);
	
	ActivityParams cargo = new ActivityParams();
	cargo.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	cargo.setActivityType("cargo");
//	cargo.setScoringThisActivityAtAll(false);
	cargo.setTypicalDuration(3600*18);
	pcs.addActivityParams(cargo);


	ActivityParams cargoD = new ActivityParams();
	cargoD.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	cargoD.setActivityType("cargoD");
//	cargoD.setScoringThisActivityAtAll(false);

	cargoD.setOpeningTime(0.5*3600);
	cargoD.setClosingTime(19*3600);
	cargoD.setTypicalDuration(3600*2);
	
	pcs.addActivityParams(cargoD);
	
	ActivityParams deliv = new ActivityParams();
	deliv.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
	deliv.setOpeningTime(0.5*3600);
	deliv.setClosingTime(20*3600);
	deliv.setActivityType("delivery");
//	deliv.setScoringThisActivityAtAll(false);

	deliv.setTypicalDuration(3600*2);
	pcs.addActivityParams(deliv);
	
	ActivityParams source = new ActivityParams();
	source.setTypicalDuration(3600*18);
//	source.setScoringThisActivityAtAll(false);
	source.setActivityType("source");
	pcs.addActivityParams(source);
	

	
}
}
