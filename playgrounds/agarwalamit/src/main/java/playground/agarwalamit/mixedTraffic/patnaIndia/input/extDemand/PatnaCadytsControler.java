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

import java.util.Arrays;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsContext;
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
import playground.agarwalamit.mixedTraffic.patnaIndia.PatnaUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.PatnaVehiclesGenerator;

/**
 * @author amit
 */

public class PatnaCadytsControler {

	public static void main(String[] args) {
		PatnaCadytsControler pcc = new PatnaCadytsControler();
		final Config config = pcc.getConfig();

		PatnaVehiclesGenerator pvg = new PatnaVehiclesGenerator("../../../../repos/runs-svn/patnaIndia/run108/input/outerCordonDemand.xml.gz");
		pvg.createVehicles();
		String patnaVehicles = PatnaUtils.INPUT_FILES_DIR+"/patnaVehicles.xml.gz";
		new VehicleWriterV1(pvg.getPatnaVehicles()).writeFile(patnaVehicles);
		config.vehicles().setVehiclesFile(patnaVehicles);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		final Controler controler = new Controler(config);
		controler.setDumpDataAtEnd(true);

		final RandomizingTimeDistanceTravelDisutility.Builder builder =  new RandomizingTimeDistanceTravelDisutility.Builder("bike");

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder);
				addTravelTimeBinding("motorbike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());
				addTravelTimeBinding("truck").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("truck").to(carTravelDisutilityFactoryKey());
			}
		});

		final CadytsContext cContext = new CadytsContext(controler.getConfig());
		controler.addControlerListener(cContext);

		controler.getConfig().getModule("cadytsCar").addParam("startTime", "00:00:00");
		controler.getConfig().getModule("cadytsCar").addParam("endTime", "24:00:00");

		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			final CharyparNagelScoringParametersForPerson parameters = new SubpopulationCharyparNagelScoringParameters( controler.getScenario() );

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				
				final CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, cContext);
				//final double cadytsScoringWeight = 0.0;
				final double cadytsScoringWeight = 15.0;
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				sumScoringFunction.addScoringFunction(scoringFunction );

				return sumScoringFunction;
			}
		}) ;

		controler.run();
	}

	private Config getConfig(){
		Config config = ConfigUtils.createConfig();

		config.plans().setInputFile("../../../../repos/runs-svn/patnaIndia/run108/input/outerCordonDemand.xml.gz");
		config.network().setInputFile("../../../../repos/runs-svn/patnaIndia/run108/input/network_diff_linkSpeed.xml.gz");

		config.qsim().setFlowCapFactor(0.01);
		config.qsim().setStorageCapFactor(0.01);
		config.qsim().setMainModes(Arrays.asList("car","bike","motorbike","truck"));
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ.name());
		config.qsim().setEndTime(30*3600);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		config.qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);

		config.counts().setCountsFileName("../../../../repos/runs-svn/patnaIndia/run108/input/innerAndOuterCounts.xml.gz");
		config.counts().setWriteCountsInterval(20);
		config.counts().setCountsScaleFactor(100);

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		config.controler().setOutputDirectory("../../../../repos/runs-svn/patnaIndia/run108/outerCordonOutput/");
		config.controler().setWritePlansInterval(50);
		config.controler().setWriteEventsInterval(50);

		StrategySettings strategySettings1 = new StrategySettings();
		strategySettings1.setStrategyName("ReRoute");
		strategySettings1.setWeight(0.3);
		strategySettings1.setDisableAfter(80);
		config.strategy().addStrategySettings(strategySettings1);
		
		StrategySettings expChangeBeta = new StrategySettings();
		expChangeBeta.setStrategyName("ChangeExpBeta");
		expChangeBeta.setWeight(0.85);
		config.strategy().addStrategySettings(expChangeBeta);

		ActivityParams homeActivity = new ActivityParams("E2E_Start");
		homeActivity.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(homeActivity);

		ActivityParams workActivity = new ActivityParams("E2E_End");
		workActivity.setTypicalDuration(8*60*60);

		ActivityParams leisureActivity = new ActivityParams("E2I_Start");
		leisureActivity.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(leisureActivity);

		ActivityParams shopActivity = new ActivityParams("E2I_mid");
		shopActivity.setTypicalDuration(8*60*60);
		config.planCalcScore().addActivityParams(shopActivity);

		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");
		config.vspExperimental().setWritingOutputEvents(true);

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

		ModeParams truck = new ModeParams("truck");//ZZ_TODO : should I calibrate asc for truck??
		truck.setConstant(0.0);
		truck.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(truck);

		config.plansCalcRoute().setNetworkModes(Arrays.asList("car","bike","motorbike","truck"));

		//following is necessary to override all defaults for teleportation.
		ModeRoutingParams mrp = new ModeRoutingParams("pt");
		mrp.setTeleportedModeSpeed(20./3.6);
		mrp.setBeelineDistanceFactor(1.5);
		config.plansCalcRoute().addModeRoutingParams(mrp);

		return config;
	}
}