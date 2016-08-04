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

import java.io.File;
import java.util.HashSet;

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
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Counts;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import playground.agarwalamit.analysis.StatsWriter;
import playground.agarwalamit.analysis.controlerListner.ModalShareControlerListner;
import playground.agarwalamit.analysis.controlerListner.ModalTravelTimeControlerListner;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.analysis.travelTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.counts.MultiModeCountsControlerListener;
import playground.agarwalamit.mixedTraffic.multiModeCadyts.ModalCadytsContext;
import playground.agarwalamit.mixedTraffic.multiModeCadyts.ModalLink;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.combined.router.BikeTimeDistanceTravelDisutilityFactory;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.combined.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.combined.router.FreeSpeedTravelTimeForTruck;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.extDemand.OuterCordonCountsGenerator;
import playground.agarwalamit.mixedTraffic.patnaIndia.ptFare.PtFareEventHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter.PatnaUserGroup;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class JointCalibrationControler {

	private final static double SAMPLE_SIZE = 0.10;

	private static final String NET_FILE = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/network/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/network.xml.gz"; //
	private static final String JOINT_PLANS_10PCT = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/joint/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/joint_plans_10pct.xml.gz"; //
	private static final String JOINT_PERSONS_ATTRIBUTE_10PCT = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/joint/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/joint_personAttributes_10pct.xml.gz"; //
	private static final String JOINT_COUNTS_10PCT = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/joint/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/joint_counts.xml.gz"; //
	private static final String JOINT_VEHICLES_10PCT = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/joint/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/joint_vehicles_10pct.xml.gz";

	private static boolean isUsingCadyts = false;

	private static String OUTPUT_DIR = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/incomeDependent/c000/";

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		JointCalibrationControler pjc = new JointCalibrationControler();

		if(args.length>0){
			ConfigUtils.loadConfig(config, args[0]);
			OUTPUT_DIR = args [1];
			isUsingCadyts = Boolean.valueOf( args[2] );
			config.controler().setOutputDirectory( OUTPUT_DIR );
		} else {
			config = pjc.createBasicConfigSettings();

			config.planCalcScore().getOrCreateModeParams("car").setConstant(0.);
			config.planCalcScore().getOrCreateModeParams("bike").setConstant(0.);
			config.planCalcScore().getOrCreateModeParams("motorbike").setConstant(0.);
			config.planCalcScore().getOrCreateModeParams("pt").setConstant(0.);
			config.planCalcScore().getOrCreateModeParams("walk").setConstant(0.);
		}

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		Scenario sc = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(sc);
		controler.getConfig().controler().setDumpDataAtEnd(true);

		final BikeTimeDistanceTravelDisutilityFactory builder_bike =  new BikeTimeDistanceTravelDisutilityFactory("bike", config.planCalcScore());
		final RandomizingTimeDistanceTravelDisutilityFactory builder_truck =  new RandomizingTimeDistanceTravelDisutilityFactory("truck", config.planCalcScore());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder_bike);

				addTravelTimeBinding("truck").to(FreeSpeedTravelTimeForTruck.class);
				addTravelDisutilityFactoryBinding("truck").toInstance(builder_truck);

				addTravelTimeBinding("motorbike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());					
			}
		});

		controler.addOverridingModule(new AbstractModule() { // ploting modal share over iterations
			@Override
			public void install() {
				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListner.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListner.class);

				this.addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
			}
		});

		// adding pt fare system based on distance 
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().to(PtFareEventHandler.class);
			}
		});
		// for above make sure that util_dist and monetary dist rate for pt are zero.
		ModeParams mp = controler.getConfig().planCalcScore().getModes().get("pt");
		mp.setMarginalUtilityOfDistance(0.0);
		mp.setMonetaryDistanceRate(0.0);


		// add income dependent scoring function factory
		controler.setScoringFunctionFactory(new PatnaScoringFunctionFactory(controler.getScenario())) ;

		// add cadyts
		if (isUsingCadyts) addCadytsSetting(controler,config);

		controler.run();

		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		for (int index =firstIt+1; index <lastIt; index ++){
			String dirToDel = OUTPUT_DIR+"/ITERS/it."+index;
			Logger.getLogger(JointCalibrationControler.class).info("Deleting the directory "+dirToDel);
			IOUtils.deleteDirectory(new File(dirToDel),false);
		}

		new File(OUTPUT_DIR+"/analysis/").mkdir();
		String outputEventsFile = OUTPUT_DIR+"/output_events.xml.gz";
		// write some default analysis
		ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile);
		mtta.run();
		mtta.writeResults(OUTPUT_DIR+"/analysis/modalTravelTime.txt");

		ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile);
		msc.run();
		msc.writeResults(OUTPUT_DIR+"/analysis/modalShareFromEvents.txt");

		StatsWriter.run(OUTPUT_DIR);
	}

	private static void addCadytsSetting(final Controler controler, final Config config){

		OuterCordonCountsGenerator occg = new OuterCordonCountsGenerator();
		occg.run();

		Counts<ModalLink> modalLinkCounts = occg.getModalLinkCounts();

		String modes = CollectionUtils.setToString(new HashSet<>(PatnaUtils.EXT_MAIN_MODES));
		config.counts().setAnalyzedModes(modes);
		config.counts().setFilterModes(true);
		config.strategy().setMaxAgentPlanMemorySize(10);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Key.get(new TypeLiteral<Counts<ModalLink>>(){}, Names.named("calibration"))).toInstance(modalLinkCounts);

				bind(ModalCadytsContext.class).asEagerSingleton();
				addControlerListenerBinding().to(ModalCadytsContext.class);
			}
		});

		CadytsConfigGroup cadytsConfigGroup = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		cadytsConfigGroup.setStartTime(0);
		cadytsConfigGroup.setEndTime(24*3600-1);

		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			final CharyparNagelScoringParametersForPerson parameters = new SubpopulationCharyparNagelScoringParameters( controler.getScenario() );
			@Inject Network network;
			@Inject ModalCadytsContext cContext;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, network));
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<ModalLink> scoringFunction = new CadytsScoring<ModalLink>(person.getSelectedPlan(), config, cContext);
				final double cadytsScoringWeight = 15.0;
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				sumScoringFunction.addScoringFunction(scoringFunction );

				return sumScoringFunction;
			}
		}) ;
	}

	/**
	 * This config do not have locations of inputs files (network, plans, counts etc).
	 */
	public Config createBasicConfigSettings () {

		Config config = ConfigUtils.createConfig();

		config.network().setInputFile(NET_FILE);

		config.plans().setInputFile(JOINT_PLANS_10PCT);
		config.plans().setSubpopulationAttributeName(PatnaUtils.SUBPOP_ATTRIBUTE);
		config.plans().setInputPersonAttributeFile(JOINT_PERSONS_ATTRIBUTE_10PCT);

		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
		config.vehicles().setVehiclesFile(JOINT_VEHICLES_10PCT);

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		config.controler().setWriteEventsInterval(100);
		config.controler().setWritePlansInterval(100);
		config.controler().setOutputDirectory(OUTPUT_DIR);

		config.counts().setInputFile(JOINT_COUNTS_10PCT);
		config.counts().setWriteCountsInterval(100);
		config.counts().setCountsScaleFactor(1/SAMPLE_SIZE);
		config.counts().setOutputFormat("all");
		//ZZ_TODO : there is something about multipleModes in counts. I could not see any effect of it.

		config.qsim().setFlowCapFactor(SAMPLE_SIZE); //1.06% sample
		config.qsim().setStorageCapFactor(3*SAMPLE_SIZE);
		config.qsim().setEndTime(30*3600);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ.toString());
		config.qsim().setMainModes(PatnaUtils.ALL_MAIN_MODES);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);

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
		return config;
	}
}