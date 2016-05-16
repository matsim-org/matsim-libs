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
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.contrib.signals.router.InvertedNetworkRoutingModuleModule;
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
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;

import matsimConnector.congestionpricing.MSACongestionHandler;
import matsimConnector.congestionpricing.MSAMarginalCongestionPricingContolerListener;
import matsimConnector.congestionpricing.MSATollDisutilityCalculatorFactory;
import matsimConnector.congestionpricing.MSATollHandler;
import playground.dgrether.signalsystems.sylvia.controler.SylviaSignalsModule;
import playground.ikaddoura.intervalBasedCongestionPricing.IntervalBasedCongestionPricing;
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
import scenarios.illustrative.braess.createInput.TtCreateBraessSignals.SignalControlType;

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
	
	private static final int NUMBER_OF_PERSONS = 2000; // per hour
	private static final int SIMULATION_PERIOD = 1; // in hours
	private static final double SIMULATION_START_TIME = 0.0; // seconds from midnight
	
	private static final InitRoutes INIT_ROUTES_TYPE = InitRoutes.ALL;
	// initial score for all initial plans
	private static final Double INIT_PLAN_SCORE = null;

	/// defines which kind of signals should be used
	private static final SignalControlType SIGNAL_TYPE = SignalControlType.NONE;
	// defines which kind of lanes should be used
	private static final LaneType LANE_TYPE = LaneType.NONE;
	
	// defines which kind of pricing should be used
	private static final PricingType PRICING_TYPE = PricingType.INTERVALBASED;
	public enum PricingType{
		NONE, V3, V4, V7, V8, V9, V10, FLOWBASED, GREGOR, INTERVALBASED
	}

	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private static final double SIGMA = 0.0;	
		
	private static final boolean WRITE_INITIAL_FILES = true;
	
	private static final String OUTPUT_BASE_DIR = "../../../runs-svn/braess/intervalBased/";
	
	public static void main(String[] args) {
		Config config = defineConfig();
		Scenario scenario = prepareScenario(config);
		Controler controler = prepareController(scenario);
	
		controler.run();
	}

	private static Config defineConfig() {
			Config config = ConfigUtils.createConfig();
	
			// set number of iterations
			config.controler().setLastIteration( 100 );
	
			// able or enable signals and lanes
			config.qsim().setUseLanes( LANE_TYPE.equals(LaneType.NONE)? false : true );
			SignalSystemsConfigGroup signalConfigGroup = ConfigUtils
					.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME,
							SignalSystemsConfigGroup.class);
			signalConfigGroup.setUseSignalSystems( SIGNAL_TYPE.equals(SignalControlType.NONE)? false : true );
	
			// set brain exp beta
			config.planCalcScore().setBrainExpBeta( 2 );
	
			// choose between link to link and node to node routing
			// (only has effect if lanes are used)
			boolean link2linkRouting = false;
			config.controler().setLinkToLinkRoutingEnabled(link2linkRouting);
			
			config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(link2linkRouting);
			config.travelTimeCalculator().setCalculateLinkTravelTimes(true);
			
			// set travelTimeBinSize (only has effect if reRoute is used)
			config.travelTimeCalculator().setTraveltimeBinSize( 10 );
			
			config.travelTimeCalculator().setTravelTimeCalculatorType(
					TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());
			// hash map and array produce same results. only difference: memory and time.
			// for small time bins and sparse values hash map is better. theresa, may'15
			
			// define strategies:
			{
				StrategySettings strat = new StrategySettings() ;
				strat.setStrategyName( DefaultStrategy.ReRoute.toString() );
				strat.setWeight( 0.0 ) ;
				strat.setDisableAfter( config.controler().getLastIteration() - 50 );
				config.strategy().addStrategySettings(strat);
			}
			{
				StrategySettings strat = new StrategySettings() ;
				strat.setStrategyName( DefaultSelector.SelectRandom.toString() );
				strat.setWeight( 0.0 ) ;
				strat.setDisableAfter( config.controler().getLastIteration() - 50 );
				config.strategy().addStrategySettings(strat);
			}
			{
				StrategySettings strat = new StrategySettings() ;
				strat.setStrategyName( DefaultSelector.ChangeExpBeta.toString() );
				strat.setWeight( 0.9 ) ;
				strat.setDisableAfter( config.controler().getLastIteration() );
				config.strategy().addStrategySettings(strat);
			}
			{
				StrategySettings strat = new StrategySettings() ;
				strat.setStrategyName( DefaultSelector.BestScore.toString() );
				strat.setWeight( 0.0 ) ;
				strat.setDisableAfter( config.controler().getLastIteration() - 50 );
				config.strategy().addStrategySettings(strat);
			}
			{
				StrategySettings strat = new StrategySettings() ;
				strat.setStrategyName( DefaultSelector.KeepLastSelected.toString() );
				strat.setWeight( 0.0 ) ;
				strat.setDisableAfter( config.controler().getLastIteration() );
				config.strategy().addStrategySettings(strat);
			}
	
			config.strategy().setMaxAgentPlanMemorySize( 3 );			
			
			config.qsim().setStuckTime(3600 * 10.);
			
			config.qsim().setStartTime(3600 * SIMULATION_START_TIME);
			// set end time to shorten simulation run time. (set it to 2 hours after the last agent departs)
			config.qsim().setEndTime(3600 * (SIMULATION_START_TIME + SIMULATION_PERIOD + 2));
			
			// adapt monetary distance cost rate (should be negative)
			config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate( -0.0 );
			
			config.planCalcScore().setMarginalUtilityOfMoney( 1.0 ); // default is 1.0
	
			// "overwriteExistingFiles" necessary if initial files should be written out
			config.controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles );		
			// note: the output directory is defined in createRunNameAndOutputDir(...) after all adaptations are done
			
			config.vspExperimental().setWritingOutputEvents(true);
			config.planCalcScore().setWriteExperiencedPlans(true);
	
			config.controler().setWriteEventsInterval( config.controler().getLastIteration() );
			config.controler().setWritePlansInterval( config.controler().getLastIteration() );
			
			ActivityParams dummyAct = new ActivityParams("dummy");
			dummyAct.setTypicalDuration(12 * 3600);
			config.planCalcScore().addActivityParams(dummyAct);
			
			config.controler().setCreateGraphs( true );
			
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
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME,
					new SignalsScenarioLoader(signalsConfigGroup).loadSignalsData());
			createSignals(scenario);
		}
		
		if (WRITE_INITIAL_FILES) 
			writeInitFiles(scenario);
		
		return scenario;
	}

	private static Controler prepareController(Scenario scenario) {
		Config config = scenario.getConfig();
		Controler controler = new Controler(scenario);

		// add the signals module
		boolean alwaysSameMobsimSeed = false;
		SylviaSignalsModule sylviaSignalsModule = new SylviaSignalsModule();
		sylviaSignalsModule.setAlwaysSameMobsimSeed(alwaysSameMobsimSeed);
		controler.addOverridingModule(sylviaSignalsModule);
		
		// add the module for link to link routing if enabled
		if (config.controler().isLinkToLinkRoutingEnabled()){
			controler.addOverridingModule(new InvertedNetworkRoutingModuleModule());
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
										new Builder( TransportMode.car, config.planCalcScore() ),
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
			final MSATollHandler tollHandler = new MSATollHandler(scenario);
			final MSATollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new MSATollDisutilityCalculatorFactory(tollHandler, config.planCalcScore());

			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( tollDisutilityCalculatorFactory );
				}
			}); 
				
			controler.addControlerListener(new MSAMarginalCongestionPricingContolerListener(scenario, tollHandler, new MSACongestionHandler(controler.getEvents(), scenario)));
		} else if (PRICING_TYPE.equals(PricingType.FLOWBASED)) {
			
			throw new UnsupportedOperationException("Not yet implemented!");
//			Initializer initializer = new Initializer();
//			controler.addControlerListener(initializer);
			
		} else if (PRICING_TYPE.equals(PricingType.INTERVALBASED)) {
			
			controler.addControlerListener(new IntervalBasedCongestionPricing(scenario));
			
			final RandomizingTimeDistanceTravelDisutility.Builder builder = 
					new RandomizingTimeDistanceTravelDisutility.Builder( TransportMode.car, config.planCalcScore() );
			builder.setSigma(SIGMA);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(builder);
				}
			});
			
		} else { // no pricing
			
			// adapt sigma for randomized routing
			final RandomizingTimeDistanceTravelDisutility.Builder builder = 
					new RandomizingTimeDistanceTravelDisutility.Builder( TransportMode.car, config.planCalcScore() );
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
			}
		});
		
		return controler;
	}

	private static void createNetwork(Scenario scenario) {	
		
		TtCreateBraessNetworkAndLanes netCreator = new TtCreateBraessNetworkAndLanes(scenario);
		netCreator.setUseBTUProperties( false );
		netCreator.setSimulateInflowCap( false );
		netCreator.setMiddleLinkExists( true );
//		netCreator.setCapZ(1);
		netCreator.setLaneType(LANE_TYPE);
		netCreator.setNumberOfPersonsPerHour(NUMBER_OF_PERSONS);
		netCreator.setCapTolerance( 0. );
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
		signalsCreator.setSignalType(SIGNAL_TYPE);
		signalsCreator.createSignals();
	}

	private static void createRunNameAndOutputDir(Scenario scenario) {

		Config config = scenario.getConfig();
		
		// get the current date in format "yyyy-mm-dd"
		Calendar cal = Calendar.getInstance ();
		// this class counts months from 0, but days from 1
		int month = cal.get(Calendar.MONTH) + 1;
		String monthStr = month + "";
		if (month < 10)
			monthStr = "0" + month;
		String date = cal.get(Calendar.YEAR) + "-" 
				+ monthStr + "-" + cal.get(Calendar.DAY_OF_MONTH);
		
		String runName = date;

		runName += "_" + NUMBER_OF_PERSONS + "p";
		if (SIMULATION_PERIOD != 1){
			runName += "_" + SIMULATION_PERIOD + "h";
		}
		runName += "_start" + (int)SIMULATION_START_TIME; 
		
		switch(INIT_ROUTES_TYPE){
		case ALL:
			runName += "_ALL-sel1+3";
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

		// create info about the different possible travel times
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
			int fastTT = (int)Math.ceil(middleLink.getLength()
					/ middleLink.getFreespeed());
			int slowTT = (int)Math.ceil(slowLink.getLength()
					/ slowLink.getFreespeed());
			int capZ = (int)middleLink.getCapacity();
			runName += "_" + fastTT + "-vs-" + slowTT + "_capZ" + capZ;
		}
		
		// create info about capacity and link length
		runName += "_cap" + (int)slowLink.getCapacity();
		if (slowLink.getLength() != 200)
			runName += "_l" + (int)slowLink.getLength() + "m";
		if (slowLink.getLength() != fastLink.getLength()){
			runName += "_l" + (int)fastLink.getLength() + "m";
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
			switch (SIGNAL_TYPE){
			case ONE_SECOND_Z:
				runName += "_1sZ";
				break;
			case ONE_SECOND_SO:
				runName += "_1sSO";
				break;
			case SIGNAL4_ONE_SECOND_SO:
				runName += "_S4_1sSO";
				break;
			case SIGNAL4_ONE_SECOND_Z:
				runName += "_S4_1sZ";
				break;
			case SIGNAL4_SYLVIA_V2Z:
				runName += "_S4_Sylvia_V2Z";
				break;
			case SIGNAL4_SYLVIA_Z2V:
				runName += "_S4_Sylvia_Z2V";
				break;
			default:
				runName += "_" + SIGNAL_TYPE;
				break;
			}			
		}
		
		if (!PRICING_TYPE.equals(PricingType.NONE)){
			runName += "_" + PRICING_TYPE.toString();
		}
		
		if (config.strategy().getMaxAgentPlanMemorySize() != 0)
			runName += "_max" + config.strategy().getMaxAgentPlanMemorySize() + "plans";

		String outputDir = OUTPUT_BASE_DIR + runName + "/"; 
		// create directory
		new File(outputDir).mkdirs();

		config.controler().setOutputDirectory(outputDir);
		log.info("The output will be written to " + outputDir);
	}

	private static void writeInitFiles(Scenario scenario) {
		String outputDir = scenario.getConfig().controler().getOutputDirectory() + "initialFiles/";
		// create directory
		new File(outputDir).mkdirs();
		
		// write network and lanes
		new NetworkWriter(scenario.getNetwork()).write(outputDir + "network.xml");
		if (!LANE_TYPE.equals(LaneType.NONE)) 
			new LaneDefinitionsWriter20(scenario.getLanes()).write(outputDir + "lanes.xml");
		
		// write population
		new PopulationWriter(scenario.getPopulation()).write(outputDir + "plans.xml");
		
		// write signal files
		if (!SIGNAL_TYPE.equals(SignalControlType.NONE)) {
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			new SignalSystemsWriter20(signalsData.getSignalSystemsData()).write(outputDir + "signalSystems.xml");
			new SignalControlWriter20(signalsData.getSignalControlData()).write(outputDir + "signalControl.xml");
			new SignalGroupsWriter20(signalsData.getSignalGroupsData()).write(outputDir + "signalGroups.xml");
		}
		
		// write config
		new ConfigWriter(scenario.getConfig()).write(outputDir + "config.xml");
	}
}
