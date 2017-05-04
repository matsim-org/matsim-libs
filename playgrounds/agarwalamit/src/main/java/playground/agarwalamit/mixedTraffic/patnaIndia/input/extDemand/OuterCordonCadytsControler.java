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

import java.io.File;
import java.util.HashSet;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
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
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
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
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Counts;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import playground.agarwalamit.analysis.modalShare.ModalShareControlerListener;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.tripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.counts.MultiModeCountsControlerListener;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.joint.JointCalibrationControler;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.others.PatnaVehiclesGenerator;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForTruck;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.multiModeCadyts.CountsInserter;
import playground.agarwalamit.multiModeCadyts.ModalCadytsContext;
import playground.agarwalamit.multiModeCadyts.ModalLink;
import playground.agarwalamit.utils.plans.SelectedPlansFilter;

/**
 * @author amit
 */

public class OuterCordonCadytsControler {

	private static final String inputLocation = PatnaUtils.INPUT_FILES_DIR;
	private static String plansFile = inputLocation+"/simulationInputs/external/"+PatnaUtils.PATNA_NETWORK_TYPE+"/outerCordonDemand_10pct.xml.gz";
	private static String outputDir = "../../../../repos/runs-svn/patnaIndia/run108/external/"+PatnaUtils.PATNA_NETWORK_TYPE+"/multiModalCadyts/outerCordonOutput_10pct_OC1Excluded/";

	private static final boolean STABILITY_CHECK_AFTER_CADYTS = false;

	public static void main(String[] args) {
		String patnaVehicles = inputLocation+"/simulationInputs/external/"+PatnaUtils.PATNA_NETWORK_TYPE+"/outerCordonVehicles_10pct.xml.gz";

		if( STABILITY_CHECK_AFTER_CADYTS) {
			String inPlans = outputDir+"/output_plans.xml.gz";	
			plansFile = "../../../../repos/runs-svn/patnaIndia/run108/input/"+PatnaUtils.PATNA_NETWORK_TYPE+"/cordonOutput_plans_10pct_selected.xml.gz";

			SelectedPlansFilter spf = new SelectedPlansFilter();
			spf.run(inPlans);
			spf.writePlans(plansFile);

			outputDir = "../../../../repos/runs-svn/patnaIndia/run108/external/"+PatnaUtils.PATNA_NETWORK_TYPE+"/multiModalCadyts/outerCordonOutput_10pct_OC1Excluded_ctd/";
			patnaVehicles = "../../../../repos/runs-svn/patnaIndia/run108/input/"+PatnaUtils.PATNA_NETWORK_TYPE+"/outerCordonVehicles_10pct_ctd.xml.gz";
		}

		OuterCordonCadytsControler pcc = new OuterCordonCadytsControler();
		final Config config = pcc.getConfig();

		PatnaVehiclesGenerator pvg = new PatnaVehiclesGenerator(plansFile);
		Vehicles vehs = pvg.createAndReturnVehicles(PatnaUtils.EXT_MAIN_MODES);

		new VehicleWriterV1(vehs).writeFile(patnaVehicles);
		config.vehicles().setVehiclesFile(patnaVehicles);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(scenario);
		controler.getConfig().controler().setDumpDataAtEnd(true);

		final RandomizingTimeDistanceTravelDisutilityFactory builder_bike =  new RandomizingTimeDistanceTravelDisutilityFactory("bike", config.planCalcScore());
		final RandomizingTimeDistanceTravelDisutilityFactory builder_truck =  new RandomizingTimeDistanceTravelDisutilityFactory("truck", config.planCalcScore());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				addTravelTimeBinding("motorbike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());

				// due to speed difference of bike and truck, using configured free speed travel time, builder should also use this.
				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder_bike);

				addTravelTimeBinding("truck").to(FreeSpeedTravelTimeForTruck.class);
				addTravelDisutilityFactoryBinding("truck").toInstance(builder_truck);
			}
		});

		if(!STABILITY_CHECK_AFTER_CADYTS) pcc.addCadytsSetting(controler, config);

		controler.addOverridingModule(new AbstractModule() { // ploting modal share over iterations
			@Override
			public void install() {
				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

				this.addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
			}
		});

		controler.run();

		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		for (int index =firstIt+1; index <lastIt; index ++){
			String dirToDel = outputDir+"/ITERS/it."+index;
			Logger.getLogger(JointCalibrationControler.class).info("Deleting the directory "+dirToDel);
			IOUtils.deleteDirectoryRecursively(new File(dirToDel).toPath());
		}
		
		new File(outputDir+"/analysis/").mkdir();
		String outputEventsFile = outputDir+"/output_events.xml.gz";
		
		ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile);
		msc.run();
		msc.writeResults(outputDir+"/analysis/modalShareFromEvents.txt");
	}

	private void addCadytsSetting(final Controler controler, final Config config){
		
		CountsInserter jcg = new CountsInserter();
		jcg.processInputFile( inputLocation+"/raw/counts/externalDemandCountsFile/outerCordonData_allCounts_shpNetwork.txt" );
		jcg.run();
		
		Counts<ModalLink> modalLinkCounts = jcg.getModalLinkCounts();
		modalLinkCounts.setYear(2008);
		modalLinkCounts.setName("Patna_counts");
		
		Map<String, ModalLink> modalLinkContainer = jcg.getModalLinkContainer();
		
		String modes = CollectionUtils.setToString(new HashSet<>(PatnaUtils.EXT_MAIN_MODES));
		config.counts().setAnalyzedModes(modes);
		config.counts().setFilterModes(true);
		config.strategy().setMaxAgentPlanMemorySize(10);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Key.get(new TypeLiteral<Counts<ModalLink>>(){}, Names.named("calibration"))).toInstance(modalLinkCounts);
				bind(Key.get(new TypeLiteral<Map<String,ModalLink>>(){})).toInstance(modalLinkContainer);
				
				bind(ModalCadytsContext.class).asEagerSingleton();
				addControlerListenerBinding().to(ModalCadytsContext.class);
			}
		});
		
		CadytsConfigGroup cadytsConfigGroup = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		cadytsConfigGroup.setStartTime(0);
		cadytsConfigGroup.setEndTime(24*3600-1);
		
		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			final ScoringParametersForPerson parameters = new SubpopulationScoringParameters( controler.getScenario() );
			@Inject
            Network network;
			@Inject
            ModalCadytsContext cContext;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, network));
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<ModalLink> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cContext);
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
		config.network().setInputFile(inputLocation+"/simulationInputs/network/"+PatnaUtils.PATNA_NETWORK_TYPE+"/network.xml.gz");

		config.qsim().setFlowCapFactor(OuterCordonUtils.SAMPLE_SIZE);
		config.qsim().setStorageCapFactor(3*OuterCordonUtils.SAMPLE_SIZE);
		config.qsim().setMainModes(PatnaUtils.EXT_MAIN_MODES);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ);
		config.qsim().setEndTime(36*3600);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);

		config.counts().setInputFile(inputLocation+"/simulationInputs/external/"+PatnaUtils.PATNA_NETWORK_TYPE+"/outerCordonCounts_10pct_OC1Excluded.xml.gz");
		config.counts().setWriteCountsInterval(5);
		config.counts().setCountsScaleFactor(1/OuterCordonUtils.SAMPLE_SIZE);
		config.counts().setOutputFormat("all");

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		config.controler().setOutputDirectory(outputDir);
		config.controler().setWritePlansInterval(100);
		config.controler().setWriteEventsInterval(50);

		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.name());
		reRoute.setWeight(0.3);
		config.strategy().addStrategySettings(reRoute);

		StrategySettings expChangeBeta = new StrategySettings();
		expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
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
		car.setMarginalUtilityOfTraveling(-0.64);
		car.setMonetaryDistanceRate(-3.7*Math.pow(10, -5));
		config.planCalcScore().addModeParams(car);

		ModeParams bike = new ModeParams("bike");
		bike.setConstant(0.0);
		bike.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(bike);

		ModeParams motorbike = new ModeParams("motorbike");
		motorbike.setConstant(0.0);
		motorbike.setMarginalUtilityOfTraveling(-0.18);
		motorbike.setMonetaryDistanceRate(-1.6*Math.pow(10, -5));
		config.planCalcScore().addModeParams(motorbike);

		ModeParams truck = new ModeParams("truck"); // using default for them.
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
