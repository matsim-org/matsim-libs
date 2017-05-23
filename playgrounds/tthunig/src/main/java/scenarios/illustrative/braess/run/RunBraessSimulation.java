/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.braess.run;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.data.LanesWriter;

import analysis.signals.TtSignalAnalysisListener;
import analysis.signals.TtSignalAnalysisTool;
import analysis.signals.TtSignalAnalysisWriter;
import playground.ikaddoura.analysis.pngSequence2Video.MATSimVideoUtils;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.decongestion.DecongestionControlerListener;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.handler.DelayAnalysis;
import playground.ikaddoura.decongestion.handler.IntervalBasedTolling;
import playground.ikaddoura.decongestion.handler.IntervalBasedTollingAll;
import playground.ikaddoura.decongestion.handler.PersonVehicleTracker;
import playground.ikaddoura.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollSetting;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollingPID;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV10;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.CongestionHandlerImplV7;
import playground.vsp.congestion.handlers.CongestionHandlerImplV8;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;
import scenarios.illustrative.analysis.TtAbstractAnalysisTool;
import scenarios.illustrative.analysis.TtAnalyzedResultsWriter;
import scenarios.illustrative.analysis.TtListenerToBindAndWriteAnalysis;
import scenarios.illustrative.braess.analysis.TtAnalyzeBraess;
import scenarios.illustrative.braess.createInput.TtCreateBraessNetworkAndLanes;
import scenarios.illustrative.braess.createInput.TtCreateBraessNetworkAndLanes.LaneType;
import scenarios.illustrative.braess.createInput.TtCreateBraessPopulation;
import scenarios.illustrative.braess.createInput.TtCreateBraessPopulation.InitRoutes;
import scenarios.illustrative.braess.createInput.TtCreateBraessSignals;
import scenarios.illustrative.braess.createInput.TtCreateBraessSignals.SignalBasePlan;
import scenarios.illustrative.braess.createInput.TtCreateBraessSignals.SignalControlLogic;
import scenarios.illustrative.braess.signals.ResponsiveLocalDelayMinimizingSignal;
import signals.CombinedSignalsModule;

/**
 * Class to run a simulation of the braess scenario with or without signals. 
 * It analyzes the simulation with help of TtAnalyzeBraess.java.
 * 
 * @author tthunig
 * 
 */
public final class RunBraessSimulation {

	private static final Logger log = Logger
			.getLogger(RunBraessSimulation.class);

	/* population parameter */
	
	private static final int NUMBER_OF_PERSONS = 3600; // per hour
	private static final int SIMULATION_PERIOD = 1; // in hours
	private static final double SIMULATION_START_TIME = 0.0; // seconds from midnight
	
	private static final InitRoutes INIT_ROUTES_TYPE = InitRoutes.NONE;
	// initial score for all initial plans (if to low, to many agents switch to outer routes simultaneously)
	private static final Double INIT_PLAN_SCORE = null;
	
	// defines which kind of signals should be used. use 'SIGNAL_LOGIC = SignalControlLogic.NONE' if signals should not be used
	private static final SignalBasePlan SIGNAL_BASE_PLAN = SignalBasePlan.NONE;
	// if SignalBasePlan SIGNAL4_X_Seconds_Z.. is used, SECONDS_Z_GREEN gives the green time for Z
	private static final int SECONDS_Z_GREEN = 59;
	private static final SignalControlLogic SIGNAL_LOGIC = SignalControlLogic.NONE;
	
	// defines which kind of lanes should be used
	private static final LaneType LANE_TYPE = LaneType.NONE;
	
	// defines which kind of pricing should be used
	private static final PricingType PRICING_TYPE = PricingType.NONE;
	public enum PricingType{
		NONE, V3, V4, V7, V8, V9, V10, FLOWBASED, GREGOR, INTERVALBASED
	}

	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private static final double SIGMA = 0.0;	
		
	private static final boolean WRITE_INITIAL_FILES = true;
	
	private static final String OUTPUT_BASE_DIR = "../../../runs-svn/braess/hEART_congestionPricing/";
	
	public static void main(String[] args) {
		Config config = defineConfig();
		Scenario scenario = prepareScenario(config);
		Controler controler = prepareController(scenario);
	
		controler.run();
		
		try {
			MATSimVideoUtils.createLegHistogramVideo(config.controler().getOutputDirectory());
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (PRICING_TYPE.equals(PricingType.INTERVALBASED)) {
			try {
				MATSimVideoUtils.createVideo(config.controler().getOutputDirectory(), 1, "toll_perLinkAndTimeBin");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static Config defineConfig() {
		Config config = ConfigUtils.createConfig();

		// set number of iterations
		config.controler().setLastIteration(100);

		// able or enable signals and lanes
		config.qsim().setUseLanes(LANE_TYPE.equals(LaneType.NONE) ? false : true);
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems(SIGNAL_LOGIC.equals(SignalControlLogic.NONE) ? false : true);
		config.qsim().setUsingFastCapacityUpdate(false);

		// set brain exp beta
		config.planCalcScore().setBrainExpBeta(2);

		// choose between link to link and node to node routing
		// (only has effect if lanes are used)
		boolean link2linkRouting = false;
		config.controler().setLinkToLinkRoutingEnabled(link2linkRouting);

		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(link2linkRouting);
		config.travelTimeCalculator().setCalculateLinkTravelTimes(true);

		// set travelTimeBinSize (only has effect if reRoute is used)
		config.travelTimeCalculator().setTraveltimeBinSize(10);
//		config.travelTimeCalculator().setMaxTime((int) (3600 * (SIMULATION_START_TIME + SIMULATION_PERIOD + 2)));
		config.travelTimeCalculator().setMaxTime(3600 * 24);

		config.travelTimeCalculator().setTravelTimeCalculatorType(TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());
		// hash map and array produce same results. only difference: memory and time.
		// for small time bins and sparse values hash map is better. theresa, may'15
		
		config.timeAllocationMutator().setMutationRange(60);

		// define strategies:
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultStrategy.ReRoute.toString());
			strat.setWeight(0.1);
			strat.setDisableAfter(config.controler().getLastIteration() - 50);
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultStrategy.TimeAllocationMutator.toString());
			strat.setWeight(0.0);
			strat.setDisableAfter(config.controler().getLastIteration() - 25);
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.SelectRandom.toString());
			strat.setWeight(0.0);
			strat.setDisableAfter(config.controler().getLastIteration() - 50);
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
			strat.setWeight(0.9);
			strat.setDisableAfter(config.controler().getLastIteration());
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.BestScore.toString());
			strat.setWeight(0.0);
			strat.setDisableAfter(config.controler().getLastIteration() - 50);
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.KeepLastSelected.toString());
			strat.setWeight(0.0);
			strat.setDisableAfter(config.controler().getLastIteration());
			config.strategy().addStrategySettings(strat);
		}

		config.strategy().setMaxAgentPlanMemorySize(5);

		config.qsim().setStuckTime(3600 * 10.);
		config.qsim().setRemoveStuckVehicles(false);

		config.qsim().setStartTime(3600 * SIMULATION_START_TIME);
		// set end time to shorten simulation run time: 2 hours after the last agent departs
//		config.qsim().setEndTime(3600 * (SIMULATION_START_TIME + SIMULATION_PERIOD + 2));
		config.qsim().setEndTime(3600 * 24);
		
		// adapt monetary distance cost rate (should be negative)
		config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate(-0.0);

		config.planCalcScore().setMarginalUtilityOfMoney(1.0); // default is 1.0

		// "overwriteExistingFiles" necessary if initial files should be written out
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		// note: the output directory is defined in createRunNameAndOutputDir(...) after all adaptations are done

		config.vspExperimental().setWritingOutputEvents(true);
		config.planCalcScore().setWriteExperiencedPlans(true);

		config.controler().setWriteEventsInterval(config.controler().getLastIteration());
		config.controler().setWritePlansInterval(config.controler().getLastIteration());

		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.planCalcScore().addActivityParams(dummyAct);

		config.controler().setCreateGraphs(true);

		// decongestion relevant parameters
		DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWRITE_OUTPUT_ITERATION(1);
		decongestionSettings.setTOLL_ADJUSTMENT(0.1);
		decongestionSettings.setUPDATE_PRICE_INTERVAL(1);
		decongestionSettings.setTOLL_BLEND_FACTOR(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
		config.addModule(decongestionSettings);
		
		return config;
	}

	private static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		createNetwork(scenario);
		createPopulation(scenario);
		createRunNameAndOutputDir(scenario);
	
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
			createSignals(scenario);
		}
		
		if (WRITE_INITIAL_FILES) 
			writeInitFiles(scenario);
		
		return scenario;
	}

	private static Controler prepareController(Scenario scenario) {
		Config config = scenario.getConfig();
		Controler controler = new Controler(scenario);
	
		switch (SIGNAL_LOGIC){
		case NONE:
			break;
		case SIMPLE_RESPONSIVE:
			// add responsive signal controler if enabled
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(ResponsiveLocalDelayMinimizingSignal.class).asEagerSingleton();
					addControlerListenerBinding().to(ResponsiveLocalDelayMinimizingSignal.class);
				}
			});
			break;
		default:
			// add combined signals module (works for different signal types as sylvia, downstream or planbased)
			boolean alwaysSameMobsimSeed = false;
			CombinedSignalsModule signalsModule = new CombinedSignalsModule();
			signalsModule.setAlwaysSameMobsimSeed(alwaysSameMobsimSeed);
			controler.addOverridingModule(signalsModule);
			break;
		}
		
		if (!PRICING_TYPE.equals(PricingType.NONE) && !PRICING_TYPE.equals(PricingType.FLOWBASED) && !PRICING_TYPE.equals(PricingType.GREGOR) && !PRICING_TYPE.equals(PricingType.INTERVALBASED)){
			// add tolling
			TollHandler tollHandler = new TollHandler(scenario);
			
			// add correct TravelDisutilityFactory for tolls if ReRoute is used
			StrategySettings[] strategies = config.strategy().getStrategySettings()
					.toArray(new StrategySettings[0]);
			for (int i = 0; i < strategies.length; i++) {
				if (strategies[i].getStrategyName().equals(DefaultStrategy.ReRoute.toString())){
					if (strategies[i].getWeight() > 0.0){ // ReRoute is used
						final CongestionTollTimeDistanceTravelDisutilityFactory factory =
								new CongestionTollTimeDistanceTravelDisutilityFactory(
										new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, config.planCalcScore() ),
								tollHandler, config.planCalcScore()
							) ;
						factory.setSigma(SIGMA);
						controler.addOverridingModule(new AbstractModule(){
							@Override
							public void install() {
								this.bindCarTravelDisutilityFactory().toInstance( factory );
							}
						});
					}
				}
			}		
			
			// choose the correct congestion handler and add it
			EventHandler congestionHandler = null;
			switch (PRICING_TYPE){
			case V3:
				congestionHandler = new CongestionHandlerImplV3(controler.getEvents(), 
						controler.getScenario());
				break;
			case V4:
				congestionHandler = new CongestionHandlerImplV4(controler.getEvents(), 
						controler.getScenario());
				break;
			case V7:
				congestionHandler = new CongestionHandlerImplV7(controler.getEvents(), 
						controler.getScenario());
				break;
			case V8:
				congestionHandler = new CongestionHandlerImplV8(controler.getEvents(), 
						controler.getScenario());
				break;
			case V9:
				congestionHandler = new CongestionHandlerImplV9(controler.getEvents(), 
						controler.getScenario());
				break;
			case V10:
				congestionHandler = new CongestionHandlerImplV10(controler.getEvents(), 
						controler.getScenario());
				break;
			default:
				break;
			}
			controler.addControlerListener(
					new MarginalCongestionPricingContolerListener(scenario, tollHandler, congestionHandler));
		
		} else if (PRICING_TYPE.equals(PricingType.GREGOR)){
			
			throw new RuntimeException("The following lines of code lead to non-compiling code... IK"); // TODO
			
//			final MSATollHandler tollHandler = new MSATollHandler(scenario);
//			final MSATollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new MSATollDisutilityCalculatorFactory(tollHandler, config.planCalcScore());
//
//			controler.addOverridingModule(new AbstractModule(){
//				@Override
//				public void install() {
//					this.bindCarTravelDisutilityFactory().toInstance( tollDisutilityCalculatorFactory );
//				}
//			}); 
//				
//			controler.addControlerListener(new MSAMarginalCongestionPricingContolerListener(scenario, tollHandler, new MSACongestionHandler(controler.getEvents(), scenario)));
	
		} else if (PRICING_TYPE.equals(PricingType.FLOWBASED)) {
			
			throw new UnsupportedOperationException("Not yet implemented!");
//			Initializer initializer = new Initializer();
//			controler.addControlerListener(initializer);
			
		} else if (PRICING_TYPE.equals(PricingType.INTERVALBASED)) {
			
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					
					this.bind(DecongestionInfo.class).asEagerSingleton();
					
					this.bind(DecongestionTollSetting.class).to(DecongestionTollingPID.class);
					this.bind(IntervalBasedTolling.class).to(IntervalBasedTollingAll.class);
					
					this.bind(IntervalBasedTollingAll.class).asEagerSingleton();
					this.bind(DelayAnalysis.class).asEagerSingleton();
					this.bind(PersonVehicleTracker.class).asEagerSingleton();
									
					this.addEventHandlerBinding().to(IntervalBasedTollingAll.class);
					this.addEventHandlerBinding().to(DelayAnalysis.class);
					this.addEventHandlerBinding().to(PersonVehicleTracker.class);
					
					this.addControlerListenerBinding().to(DecongestionControlerListener.class);

				}
			});
			
			// toll-adjusted routing
			
			final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
			travelDisutilityFactory.setSigma(0.);
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
				}
			});
						
		} else { // no pricing
			
			// adapt sigma for randomized routing
			final RandomizingTimeDistanceTravelDisutilityFactory builder =
					new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, config.planCalcScore() );
			builder.setSigma(SIGMA);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(builder);
				}
			});
		}
		
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
//				this.bind(TtAnalyzeBraess.class).asEagerSingleton();
//				this.addEventHandlerBinding().to(TtAnalyzeBraess.class);
				this.bind(TtAbstractAnalysisTool.class).to(TtAnalyzeBraess.class).asEagerSingleton();
				this.addEventHandlerBinding().to(TtAbstractAnalysisTool.class);
				this.bind(TtAnalyzedResultsWriter.class);
				this.addControlerListenerBinding().to(TtListenerToBindAndWriteAnalysis.class);
				
				SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
						SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
				if (signalsConfigGroup.isUseSignalSystems()) {
					// bind tool to analyze signals
					this.bind(TtSignalAnalysisTool.class).asEagerSingleton();
					this.addEventHandlerBinding().to(TtSignalAnalysisTool.class);
					this.addControlerListenerBinding().to(TtSignalAnalysisTool.class);
					this.bind(TtSignalAnalysisWriter.class);
					this.addControlerListenerBinding().to(TtSignalAnalysisListener.class);
				}
			}
		});
		
		return controler;
	}

	private static void createNetwork(Scenario scenario) {	
		
		TtCreateBraessNetworkAndLanes netCreator = new TtCreateBraessNetworkAndLanes(scenario);
//		netCreator.setUseBTUProperties( false );
		netCreator.setSimulateInflowCap( false );
		netCreator.setMiddleLinkExists( true );
		
		netCreator.setCapFirstLast(4000);
		netCreator.setCapZ(1800);
		netCreator.setCapFast(1800);
		netCreator.setCapSlow(1800);
		
		netCreator.setLaneType(LANE_TYPE);
		netCreator.setNumberOfPersonsPerHour(NUMBER_OF_PERSONS);
		
		netCreator.createNetworkAndLanes();
	}

	private static void createPopulation(Scenario scenario) {
		
		TtCreateBraessPopulation popCreator = 
				new TtCreateBraessPopulation(scenario.getPopulation(), scenario.getNetwork());
		popCreator.setNumberOfPersons(NUMBER_OF_PERSONS);
		popCreator.setSimulationPeriod(SIMULATION_PERIOD);
		popCreator.setSimulationStartTime(SIMULATION_START_TIME);
		
		popCreator.createPersons(INIT_ROUTES_TYPE, INIT_PLAN_SCORE);
	}

	private static void createSignals(Scenario scenario) {

		TtCreateBraessSignals signalsCreator = new TtCreateBraessSignals(scenario);
		signalsCreator.setLaneType(LANE_TYPE);
		signalsCreator.setSignalControlLogic(SIGNAL_LOGIC);
		signalsCreator.setBasePlanType(SIGNAL_BASE_PLAN);
		signalsCreator.setSecondsZGreen(SECONDS_Z_GREEN);
		signalsCreator.createSignals();
	}

	private static void createRunNameAndOutputDir(Scenario scenario) {

		Config config = scenario.getConfig();
		
		// get the current date in format "yyyy-mm-dd-hh-mm-ss"
		Calendar cal = Calendar.getInstance ();
		// this class counts months from 0, but days from 1
		int month = cal.get(Calendar.MONTH) + 1;
		String monthStr = month + "";
		if (month < 10)
			monthStr = "0" + month;
		String date = cal.get(Calendar.YEAR) + "-" 
				+ monthStr + "-" + cal.get(Calendar.DAY_OF_MONTH) 
				+ "-" + cal.get(Calendar.HOUR_OF_DAY) + "-" + cal.get(Calendar.MINUTE) + "-" + cal.get(Calendar.SECOND);
		
		String outputDir = OUTPUT_BASE_DIR + date + "/"; 
		// create directory
		new File(outputDir).mkdirs();

		config.controler().setOutputDirectory(outputDir);
		log.info("The output will be written to " + outputDir);
		
		writeRunDescription(outputDir, createRunName(scenario));
	}
	
	private static String createRunName(Scenario scenario) {
		Config config = scenario.getConfig();
		
		String runName = NUMBER_OF_PERSONS + "p";
		if (SIMULATION_PERIOD != 1){
			runName += "_" + SIMULATION_PERIOD + "h";
		}
		runName += "_start" + (int)SIMULATION_START_TIME; 
		
		switch(INIT_ROUTES_TYPE){
		case ALL:
			runName += "_ALL"; //"_ALL-sel1+3";
			break;
		case ONLY_OUTER:
			runName += "_OUTER";
			break;
		case ONLY_MIDDLE:
			runName += "_MIDDLE";
			break;
		default: // e.g. NONE
			break;
		}
		if (INIT_PLAN_SCORE != null)
			runName += "-score" + INIT_PLAN_SCORE;

		runName += "_" + config.controler().getLastIteration() + "it";

		// create info about the different possible travel times, capacities and link length
		Link middleLink = scenario.getNetwork().getLinks()
				.get(Id.createLinkId("3_4"));
		Link slowLink = scenario.getNetwork().getLinks()
				.get(Id.createLinkId("3_5"));
		Link fastLink = scenario.getNetwork().getLinks().containsKey(Id.createLinkId("2_3"))? 
				scenario.getNetwork().getLinks().get(Id.createLinkId("2_3")) : 
					scenario.getNetwork().getLinks().get(Id.createLinkId("23_3"));
		// note: link 3_5 always exists. link 2_3 only exists if inflow links are not used. it is 23_3 otherwise.
		if (middleLink == null){
			runName += "_woZ";
		} else {
			int middleTT = (int)Math.ceil(middleLink.getLength()
					/ middleLink.getFreespeed());
			int fastTT = (int)Math.ceil(fastLink.getLength()
					/ fastLink.getFreespeed());
			int slowTT = (int)Math.ceil(slowLink.getLength()
					/ slowLink.getFreespeed());
			int capZ = (int)middleLink.getCapacity();
			int capFast = (int)fastLink.getCapacity();
			int capSlow = (int)slowLink.getCapacity();
			if (fastTT != 60 || middleTT != 60 || slowTT != 600){
				runName += "_tt-" + fastTT + "-" + middleTT + "-" + slowTT;
			}
			runName += "_cap-" + capFast + "-" + capZ + "-" + capSlow;
			if (fastLink.getLength() != 1000){
				runName += "_l-" + (int)fastLink.getLength() + "-" + (int)middleLink.getLength() + "-" + (int)slowLink.getLength();
			}
		}
		
		if (scenario.getNetwork().getNodes().containsKey(Id.createNodeId(23))){
			runName += "_inflow";
			
			Link inflowLink = scenario.getNetwork().getLinks()
					.get(Id.createLinkId("2_23"));
			if (inflowLink.getLength() != 7.5)
				runName += inflowLink.getLength();
		}
		
		StrategySettings[] strategies = config.strategy().getStrategySettings()
				.toArray(new StrategySettings[0]);
		for (int i = 0; i < strategies.length; i++) {
			double weight = strategies[i].getWeight();
			if (weight != 0.0){
				String name = strategies[i].getStrategyName();
				if (name.equals(DefaultSelector.ChangeExpBeta.toString())){
					runName += "_ChExp" + weight;
					runName += "_beta" + (int)config.planCalcScore().getBrainExpBeta();
				} else if (name.equals(DefaultSelector.KeepLastSelected.toString())){
					runName += "_KeepLast" + weight;
				} else if (name.equals(DefaultStrategy.ReRoute.toString())){
					runName += "_ReRoute" + weight;
					runName += "_tbs" + config.travelTimeCalculator().getTraveltimeBinSize();
				} else if (name.equals(DefaultStrategy.TimeAllocationMutator.toString())){
					runName += "_TimeAll" + weight;
					runName += "_range" + config.timeAllocationMutator().getMutationRange();
				} else {
					runName += "_" + name + weight;
				}
			}
		}
		
		if (SIGMA != 0.0)
			runName += "_sigma" + SIGMA;
		if (config.planCalcScore().getModes().get(TransportMode.car).getMonetaryDistanceRate() != 0.0)
			runName += "_distCost"
					+ config.planCalcScore().getModes().get(TransportMode.car).getMonetaryDistanceRate();

		if (LANE_TYPE.equals(LaneType.TRIVIAL)) {
			runName += "_trivialLanes";
		}
		else if (LANE_TYPE.equals(LaneType.REALISTIC)){
			runName += "_lanes";
		}

		// link 2 link vs node 2 node routing. this only has an effect if lanes are used
		if (LANE_TYPE.equals(LaneType.REALISTIC)){
			if (config.controler().isLinkToLinkRoutingEnabled())
				runName += "_link";
			else
				runName += "_node";
		}			

		if (ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME,
				SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			switch (SIGNAL_LOGIC){
			case SIMPLE_RESPONSIVE:
				runName += "_simpleResp";
				break;
			case DOWNSTREAM_RESPONSIVE:
				runName += "_downstream";
				break;
			default:
				runName += "_" + SIGNAL_LOGIC;
				break;
			}
			switch (SIGNAL_BASE_PLAN){
			case ALL_NODES_ALL_GREEN:
				runName += "_allGreen";
				break;
			case ALL_NODES_GREEN_WAVE_SO:
				runName += "_greenWaveSO";
				break;
			case ALL_NODES_GREEN_WAVE_Z:
				runName += "_greenWaveZ";
				break;
			case ALL_NODES_ONE_SECOND_SO:
				runName += "_1sSO";
				break;
			case ALL_NODES_ONE_SECOND_Z:
				runName += "_1sZ";
				break;
			case SIGNAL4_X_SECOND_Z_V2Z:
				runName += "_S4_" + SECONDS_Z_GREEN + "sZ_V2Z";
				break;
			case SIGNAL4_X_SECOND_Z_Z2V:
				runName += "_S4_" + SECONDS_Z_GREEN + "sZ_Z2V";
				break;
			default:
				runName += "_" + SIGNAL_BASE_PLAN;
				break;
			}			
		}
		
		if (!PRICING_TYPE.equals(PricingType.NONE)){
			if (PRICING_TYPE.equals(PricingType.INTERVALBASED)){
				runName += "_INTERVAL_tbs" + config.travelTimeCalculator().getTraveltimeBinSize();
			} else {
				runName += "_" + PRICING_TYPE.toString();
			}
		}
		
		if (config.strategy().getMaxAgentPlanMemorySize() != 0)
			runName += "_" + config.strategy().getMaxAgentPlanMemorySize() + "pl";
		
		runName += "_stuckT" + (int)config.qsim().getStuckTime();
		if (config.qsim().getEndTime() != Time.UNDEFINED_TIME)
			runName += "_simEndT" + (int)(config.qsim().getEndTime()/24) + "h";
		
		return runName;
	}

	private static void writeRunDescription(String outputDir, String runName){
		PrintStream stream;
		String filename = outputDir + "runDescription.txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.println(runName);
		stream.close();
	}

	private static void writeInitFiles(Scenario scenario) {
		String outputDir = scenario.getConfig().controler().getOutputDirectory() + "initialFiles/";
		// create directory
		new File(outputDir).mkdirs();
		
		// write network and lanes
		new NetworkWriter(scenario.getNetwork()).write(outputDir + "network.xml");
		if (!LANE_TYPE.equals(LaneType.NONE)) 
			new LanesWriter(scenario.getLanes()).write(outputDir + "lanes.xml");
		
		// write population
		new PopulationWriter(scenario.getPopulation()).write(outputDir + "plans.xml");
		
		// write signal files
		if (!SIGNAL_LOGIC.equals(SignalControlLogic.NONE)) {
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			new SignalSystemsWriter20(signalsData.getSignalSystemsData()).write(outputDir + "signalSystems.xml");
			new SignalControlWriter20(signalsData.getSignalControlData()).write(outputDir + "signalControl.xml");
			new SignalGroupsWriter20(signalsData.getSignalGroupsData()).write(outputDir + "signalGroups.xml");
		}
		
		// write config
		new ConfigWriter(scenario.getConfig()).write(outputDir + "config.xml");
	}
}
