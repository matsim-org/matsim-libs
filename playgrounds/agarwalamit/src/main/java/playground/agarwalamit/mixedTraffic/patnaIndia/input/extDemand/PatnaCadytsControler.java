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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.extDemand;

import javax.inject.Inject;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
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
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.vehicles.VehicleWriterV1;

import playground.agarwalamit.mixedTraffic.patnaIndia.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.PatnaVehiclesGenerator;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.plans.SelectedPlansFilter;

/**
 * @author amit
 */

public class PatnaCadytsControler {

	private static String plansFile = "../../../../repos/runs-svn/patnaIndia/run108/input/outerCordonDemand_10pct.xml.gz";
	private static String outputDir = "../../../../repos/runs-svn/patnaIndia/run108/outerCordonOutput_10pct_OC1Excluded/";

	private static final boolean STABILITY_CHECK_AFTER_CADYTS = false;
	
	public static void main(String[] args) {
		String patnaVehicles = "../../../../repos/runs-svn/patnaIndia/run108/input/patnaVehicles_outerCordon.xml.gz";
		
		if( STABILITY_CHECK_AFTER_CADYTS) {
			String inPlans = outputDir+"/output_plans.xml.gz";	
			plansFile = "../../../../repos/runs-svn/patnaIndia/run108/input/cordonOutput_plans_10pct_selected.xml.gz";
			
			SelectedPlansFilter spf = new SelectedPlansFilter();
			spf.run(inPlans);
			spf.writePlans(plansFile);
			
			outputDir = "../../../../repos/runs-svn/patnaIndia/run108/outerCordonOutput_10pct_ctd/";
			patnaVehicles = "../../../../repos/runs-svn/patnaIndia/run108/input/patnaVehicles_outerCordon_ctd.xml.gz";
		}
		
		PatnaCadytsControler pcc = new PatnaCadytsControler();
		final Config config = pcc.getConfig();

		PatnaVehiclesGenerator pvg = new PatnaVehiclesGenerator(plansFile);
		pvg.createVehicles(PatnaUtils.EXT_MAIN_MODES);
		
		new VehicleWriterV1(pvg.getPatnaVehicles()).writeFile(patnaVehicles);
		config.vehicles().setVehiclesFile(patnaVehicles);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		final Controler controler = new Controler(config);
		controler.getConfig().controler().setDumpDataAtEnd(true);

		final RandomizingTimeDistanceTravelDisutility.Builder builder_bike =  new RandomizingTimeDistanceTravelDisutility.Builder("bike");
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder_bike);
				addTravelTimeBinding("motorbike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());
				addTravelTimeBinding("truck").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("truck").to(carTravelDisutilityFactoryKey());
			}
		});

		if(!STABILITY_CHECK_AFTER_CADYTS) pcc.addCadytsSetting(controler, config);

		controler.run();
	}

	private void addCadytsSetting(final Controler controler, final Config config){
		controler.addOverridingModule(new CadytsCarModule());

		CadytsConfigGroup cadytsConfigGroup = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		cadytsConfigGroup.setStartTime(0);
		cadytsConfigGroup.setEndTime(24*3600-1);
		
		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			final CharyparNagelScoringParametersForPerson parameters = new SubpopulationCharyparNagelScoringParameters( controler.getScenario() );
			@Inject Network network;
			@Inject CadytsContext cContext;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, network));
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, cContext);
				final double cadytsScoringWeight = 15.0;
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				sumScoringFunction.addScoringFunction(scoringFunction );

				return sumScoringFunction;
			}
		}) ;
	}

	private Config getConfig(){
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(PatnaUtils.EPSG);

		config.plans().setInputFile(plansFile);
		config.network().setInputFile("../../../../repos/runs-svn/patnaIndia/run108/input/network_diff_linkSpeed.xml.gz");

		config.qsim().setFlowCapFactor(OuterCordonUtils.SAMPLE_SIZE);
		config.qsim().setStorageCapFactor(3*OuterCordonUtils.SAMPLE_SIZE);
		config.qsim().setMainModes(PatnaUtils.EXT_MAIN_MODES);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ.name());
		config.qsim().setEndTime(36*3600);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		config.qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);

		config.counts().setCountsFileName("../../../../repos/runs-svn/patnaIndia/run108/input/outerCordonCounts_10pct_OC1Excluded.xml.gz");
		config.counts().setWriteCountsInterval(5);
		config.counts().setCountsScaleFactor(1/OuterCordonUtils.SAMPLE_SIZE);
		config.counts().setOutputFormat("all");

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		config.controler().setOutputDirectory(outputDir);
		config.controler().setWritePlansInterval(100);
		config.controler().setWriteEventsInterval(2);

		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.name());
		reRoute.setWeight(0.3);
		config.strategy().addStrategySettings(reRoute);

		StrategySettings expChangeBeta = new StrategySettings();
		expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.name());
		expChangeBeta.setWeight(0.7);
		config.strategy().addStrategySettings(expChangeBeta);

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.strategy().setMaxAgentPlanMemorySize(6);

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

		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");
		config.vspExperimental().setWritingOutputEvents(true);

		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(0);
		config.planCalcScore().setPerforming_utils_hr(6.0);

		ModeParams car = new ModeParams("car");
		car.setConstant(0.0);
		car.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(car);

		ModeParams bike = new ModeParams("bike");
		bike.setConstant(0.0);
		bike.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(bike);

		ModeParams motorbike = new ModeParams("motorbike");
		motorbike.setConstant(0.0);
		motorbike.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(motorbike);

		ModeParams truck = new ModeParams("truck");//ZZ_TODO : should I calibrate asc for truck??
		truck.setConstant(0.0);
		truck.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(truck);

		config.plansCalcRoute().setNetworkModes(PatnaUtils.EXT_MAIN_MODES);

		//following is necessary to override all defaults for teleportation.
		ModeRoutingParams mrp = new ModeRoutingParams("pt");
		mrp.setTeleportedModeSpeed(20./3.6);
		mrp.setBeelineDistanceFactor(1.5);
		config.plansCalcRoute().addModeRoutingParams(mrp);
		return config;
	}
}