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
package scenarios.cottbus.run;

import java.io.File;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
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

import analysis.TtAnalyzedGeneralResultsWriter;
import analysis.TtGeneralAnalysis;
import analysis.TtListenerToBindGeneralAnalysis;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.CongestionHandlerImplV8;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;
import scenarios.illustrative.braess.createInput.TtCreateBraessSignals.SignalControlType;

/**
 * Class to run a cottbus simulation.
 * 
 * @author tthunig
 *
 */
public class TtRunCottbusSimulation {

	private static final Logger LOG = Logger.getLogger(TtRunCottbusSimulation.class);
	
	private final static SignalType SIGNAL_TYPE = SignalType.MS;
	private enum SignalType {
		NONE, MS, MS_RANDOM_OFFSETS
	}
	
	// defines which kind of pricing should be used
	private static final PricingType PRICING_TYPE = PricingType.NONE;
	private enum PricingType {
		NONE, V3, V4, V8, V9, FLOWBASED
	}
	
	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private static final double SIGMA = 0.0;
	
	private static final String OUTPUT_BASE_DIR = "../../../runs-svn/cottbus/createNewBC/";
	private static final String INPUT_BASE_DIR = "../../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/";
	
	private static final boolean WRITE_INITIAL_FILES = true;
	
	public static void main(String[] args) {
		Config config = defineConfig();
		Scenario scenario = prepareScenario( config );
		Controler controler = prepareController( scenario );
		
		controler.run();
	}

	private static Config defineConfig() {
		Config config = ConfigUtils.createConfig();

		config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_woTagebau.xml");
		config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes.xml");
		config.plans().setInputFile(INPUT_BASE_DIR + "cb_spn_gemeinde_nachfrage_landuse/"
				+ "commuter_population_wgs84_utm33n_car_only_woLinks.xml.gz");
		
		// set number of iterations
		config.controler().setLastIteration(100);

		// able or enable signals and lanes
		config.qsim().setUseLanes( true );
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems(SIGNAL_TYPE.equals(SignalControlType.NONE) ? false : true);
		String signalControlFile = "";
		if (SIGNAL_TYPE.equals(SignalType.MS))
			signalControlFile = "signal_control_no_13.xml";
		else if (SIGNAL_TYPE.equals(SignalType.MS_RANDOM_OFFSETS))
			signalControlFile = "signal_control_no_13_random_offsets.xml";
		if (!SIGNAL_TYPE.equals(SignalType.NONE)) {
			signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + signalControlFile);
			signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_no_13.xml");
			signalConfigGroup.setSignalSystemFile(INPUT_BASE_DIR + "signal_systems_no_13.xml");
		}
		
		// set brain exp beta
		config.planCalcScore().setBrainExpBeta( 2 );

		// choose between link to link and node to node routing
		// (only has effect if lanes are used)
		boolean link2linkRouting = true;
		config.controler().setLinkToLinkRoutingEnabled(link2linkRouting);
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(link2linkRouting);
		config.travelTimeCalculator().setCalculateLinkTravelTimes(true);

		// set travelTimeBinSize (only has effect if reRoute is used)
		config.travelTimeCalculator().setTraveltimeBinSize(10);

		config.travelTimeCalculator().setTravelTimeCalculatorType(TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());
		// hash map and array produce same results. only difference: memory and time.
		// for small time bins and sparse values hash map is better. theresa, may'15

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
			strat.setDisableAfter(config.controler().getLastIteration() - 100);
			config.strategy().addStrategySettings(strat);
			config.timeAllocationMutator().setMutationRange(1800); // 1800 is default
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

		// choose maximal number of plans per agent. 0 means unlimited
		config.strategy().setMaxAgentPlanMemorySize(5);

//		config.qsim().setStuckTime(3600 * 10.);
		config.qsim().setRemoveStuckVehicles(false);
		
		config.qsim().setStorageCapFactor( 0.5 );
		config.qsim().setFlowCapFactor( 0.5 );
		
		config.qsim().setStartTime(3600 * 6); 

		// adapt monetary distance cost rate
		// (should be negative. use -12.0 to balance time [h] and distance [m].
		// use -0.000015 to approximately balance the utility of travel time and
		// distance in a scenario with 3 vs 11min travel time and 40 vs 50 km.
		// use -0.0 to use only time.)
		config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate(-0.0);

		config.planCalcScore().setMarginalUtilityOfMoney(1.0); // default is 1.0

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		// note: the output directory is defined in
		// createRunNameAndOutputDir(...) after all adaptations are done

		config.vspExperimental().setWritingOutputEvents(true);
		config.planCalcScore().setWriteExperiencedPlans(false);
		config.controler().setCreateGraphs(true);

		config.controler().setWriteEventsInterval(config.controler().getLastIteration());
		config.controler().setWritePlansInterval(config.controler().getLastIteration());

		// define activity types
		{
//			ActivityParams dummyAct = new ActivityParams("dummy");
//			dummyAct.setTypicalDuration(12 * 3600);
//			config.planCalcScore().addActivityParams(dummyAct);
		}
		{
			ActivityParams homeAct = new ActivityParams("home");
			homeAct.setTypicalDuration(15.5 * 3600);
			config.planCalcScore().addActivityParams(homeAct);
		}
		{
			ActivityParams workAct = new ActivityParams("work");
			workAct.setTypicalDuration(8.5 * 3600);
			workAct.setOpeningTime(7 * 3600);
			workAct.setClosingTime(17.5 * 3600);
			config.planCalcScore().addActivityParams(workAct);
		}
		
		return config;
	}

	private static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);		
		createRunNameAndOutputDir(scenario);
	
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME,
					new SignalsScenarioLoader(signalsConfigGroup).loadSignalsData());
		}
		
		if (WRITE_INITIAL_FILES) 
			writeInitFiles(scenario);
		
		return scenario;
	}

	private static Controler prepareController(Scenario scenario) {
		Config config = scenario.getConfig();
		Controler controler = new Controler(scenario);

		// add the signals module if signal systems are used
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		if (signalsConfigGroup.isUseSignalSystems()) {
			controler.addOverridingModule(new SignalsModule());
		}
		
		// add the module for link to link routing if enabled
		if (config.controler().isLinkToLinkRoutingEnabled()){
			controler.addOverridingModule(new InvertedNetworkRoutingModuleModule());
		}

		if (!PRICING_TYPE.equals(PricingType.NONE) && !PRICING_TYPE.equals(PricingType.FLOWBASED)){
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
			case V8:
				congestionHandler = new CongestionHandlerImplV8(controler.getEvents(), 
						controler.getScenario());
				break;
			case V9:
				congestionHandler = new CongestionHandlerImplV9(controler.getEvents(), 
						controler.getScenario());
				break;
			default:
				break;
			}
			controler.addControlerListener(
					new MarginalCongestionPricingContolerListener(controler.getScenario(), 
							tollHandler, congestionHandler));
		
		} else if (PRICING_TYPE.equals(PricingType.FLOWBASED)) {
			
			throw new UnsupportedOperationException("Not yet implemented!");
//			Initializer initializer = new Initializer();
//			controler.addControlerListener(initializer);		
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
				this.bind(TtGeneralAnalysis.class).asEagerSingleton();
				this.addEventHandlerBinding().to(TtGeneralAnalysis.class);
				this.bind(TtAnalyzedGeneralResultsWriter.class);
				this.addControlerListenerBinding().to(TtListenerToBindGeneralAnalysis.class);
			}
		});
		
		return controler;
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

		runName += "_" + config.controler().getLastIteration() + "it";
		
		// create info about capacities
		double storeCap = config.qsim().getStorageCapFactor();
		double flowCap = config.qsim().getFlowCapFactor();
		if (storeCap == flowCap && storeCap != 1.0){
			runName += "_cap" + storeCap;
		} else if (storeCap != 1.0) {
			runName += "_storeCap" + storeCap;
		} else if (flowCap != 1.0) {
			runName += "_flowCap" + flowCap;
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
					runName += "_TimeMut" + weight;
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

		if (config.qsim().isUseLanes()){
			runName += "_lanes";
			// link 2 link vs node 2 node routing. this only has an effect if lanes are used
			if (config.controler().isLinkToLinkRoutingEnabled())
				runName += "_link";
			else
				runName += "_node";
		}			

		if (ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME,
				SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			switch (SIGNAL_TYPE){
			case MS_RANDOM_OFFSETS:
				runName += "_rdmOff";
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
		LOG.info("The output will be written to " + outputDir);
	}

	private static void writeInitFiles(Scenario scenario) {
		String outputDir = scenario.getConfig().controler().getOutputDirectory() + "initialFiles/";
		// create directory
		new File(outputDir).mkdirs();
		
		// write network and lanes
		new NetworkWriter(scenario.getNetwork()).write(outputDir + "network.xml");
		if (scenario.getConfig().qsim().isUseLanes()) 
			new LaneDefinitionsWriter20(scenario.getLanes()).write(outputDir + "lanes.xml");
		
		// write population
		new PopulationWriter(scenario.getPopulation()).write(outputDir + "plans.xml");
		
		// write signal files
		if (!SIGNAL_TYPE.equals(SignalType.NONE)) {
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			new SignalSystemsWriter20(signalsData.getSignalSystemsData()).write(outputDir + "signalSystems.xml");
			new SignalControlWriter20(signalsData.getSignalControlData()).write(outputDir + "signalControl.xml");
			new SignalGroupsWriter20(signalsData.getSignalGroupsData()).write(outputDir + "signalGroups.xml");
		}
		
		// write config
		new ConfigWriter(scenario.getConfig()).write(outputDir + "config.xml");
	}

}
