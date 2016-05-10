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
package congestionPricing2CapacityAdoption;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.scenario.ScenarioUtils;

import analysis.TtStaticLinkFlowValues;
import analysis.interruptedRuns.TtBindAnalysis;
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

/**
 * @author tthunig
 *
 */
public class TtRunCapAdopForBraess {
	
	private static final Logger LOG = Logger.getLogger(TtRunCapAdopForBraess.class);
	
	private static final int flowValueStepSize = 5;
	private static final int iterationNumberPerStep = 100;
	private static final boolean REUSE_PLANS = false;
	
	// defines which kind of pricing should be used
	private static final PricingType PRICING_TYPE = PricingType.V9;
	private enum PricingType {
		V3, V4, V7, V8, V9, V10
	}

	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private static final double SIGMA = 3.0;

	private static final String OUTPUT_BASE_DIR = "../../../runs-svn/braess/capacityAdoption/";

	private static final String INPUT_BASE_DIR = "../../../shared-svn/projects/cottbus/data/scenarios/braess_scenario/cap4000-1800_noSignals/";
	private static final String NETWORK_FILE = INPUT_BASE_DIR + "network.xml";
	private static final String PLANS_FILE = INPUT_BASE_DIR + "plans.xml";
	
	public static void main(String[] args) {
	
		// load scenario (twice)
		Controler controlerPricing = createControler(true);
		Controler controlerBasic = createControler(false);
		
		// create handlers that determine flow values
		TtStaticLinkFlowValues flowValuesPricing = new TtStaticLinkFlowValues();
		TtStaticLinkFlowValues flowValuesBasic = new TtStaticLinkFlowValues();
		// create tools for overall analysis
		TtAbstractAnalysisTool analyzerBasic = new TtAnalyzeBraess();
		TtBindAnalysis analyzerBindingBasic = new TtBindAnalysis(controlerBasic.getScenario(), analyzerBasic);
		// add all handlers and listeners
		controlerPricing.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(flowValuesPricing);
				// analysis tool for travel time etc of uninterrupted runs
				this.bind(TtAbstractAnalysisTool.class).to(TtAnalyzeBraess.class).asEagerSingleton();
				this.addEventHandlerBinding().to(TtAbstractAnalysisTool.class);
				this.bind(TtAnalyzedResultsWriter.class);
				this.addControlerListenerBinding().to(TtListenerToBindAndWriteAnalysis.class);
			}
		});
		controlerBasic.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(flowValuesBasic);
				// add analysis tools for travel time etc of interrupted runs
				this.addEventHandlerBinding().toInstance(analyzerBasic);
				this.addControlerListenerBinding().toInstance(analyzerBindingBasic);
			}
		});
		
		// run pricing scenario as first best solution to compare with
		controlerPricing.run();
		
		// initialize stop criterion
		boolean stableState = false;
		int i = 0;
		
		// start loop. adopt scenario while stable state is not reached
		while (!stableState) {
			// run basic scenario with changed capacities
			controlerBasic.run();
			
			Network networkBasic = controlerBasic.getScenario().getNetwork();
			int currentFirstIteration = controlerBasic.getScenario().getConfig().controler().getFirstIteration();
			int currentLastIteration = controlerBasic.getScenario().getConfig().controler().getLastIteration();
			
			// write current network of the basic scenario to file
			new NetworkWriter(networkBasic).write(createOutputDirName("comparison") + "network" + currentFirstIteration + ".xml");
			// prepare file for comparison of link flows
			CompFileWriter compFileWriter = new CompFileWriter(currentFirstIteration);
						
			// adopt iteration numbers for the next loop
			controlerBasic.getScenario().getConfig().controler().setFirstIteration(currentLastIteration + 1);
			controlerBasic.getScenario().getConfig().controler().setLastIteration(currentLastIteration + iterationNumberPerStep);
			// TODO does this change anything? the scenario is not loaded again afterwards...
			if (REUSE_PLANS) {
				// use last plans for the next iteration
				controlerBasic.getScenario().getConfig().plans().setInputFile(controlerBasic.getScenario().getConfig().controler().getOutputDirectory() + "output_plans.xml.gz");
			} else {
				// use plans without routes from the beginning
				controlerBasic.getScenario().getConfig().plans().setInputFile(PLANS_FILE);
			}
			
			// prepare determination of the link with the maximum flow value difference
			int maxNegativeFlowValueDiff = 0;
			Id<Link> maxNegativeFlowValueDiffLink = null;
			
			// compare link flows of both runs
			for (Id<Link> linkId : networkBasic.getLinks().keySet()){
				
				// compare static flow values
				int flowValueBasic = flowValuesBasic.getStaticLinkFlow(linkId);
				int flowValuePricing = flowValuesPricing.getStaticLinkFlow(linkId);
				int flowValueDiff = flowValuePricing - flowValueBasic;
				
				if (maxNegativeFlowValueDiff > flowValueDiff){
					maxNegativeFlowValueDiff = flowValueDiff;
					maxNegativeFlowValueDiffLink = linkId;
				}
				
				compFileWriter.addLine(linkId, flowValueBasic, flowValuePricing, flowValueDiff);
			}
			
			if (maxNegativeFlowValueDiffLink == null){
				// stop the process if flow values do not differ
				continue;
			}
			
			// adopt capacity of link with maximal flow value difference
			double capacitySummand = maxNegativeFlowValueDiff / flowValueStepSize;
			double previousCapacity = networkBasic.getLinks().get(maxNegativeFlowValueDiffLink).getCapacity();
			double newCapacity = previousCapacity + capacitySummand;
			if (newCapacity < 1){
				newCapacity = 1;
			}
			networkBasic.getLinks().get(maxNegativeFlowValueDiffLink).setCapacity(newCapacity);
			LOG.warn("link: " + maxNegativeFlowValueDiffLink + "\t flow diff: " + maxNegativeFlowValueDiff + "\t prev cap: " + previousCapacity + "\t new cap: " + newCapacity);
			
			// check stop criterion
			if (capacitySummand > -1) {
				stableState = true;
			}
			
			i++;
			// stop the process if it does not stop itself after a specific number of loops
			if (i > 10) {
				stableState = true;
			}			
		}
		
		// close writing streams and plot summarized analysis
		analyzerBindingBasic.runFinished();
		// TODO plot travel times together (new gnuplot script)
	}

	private static class CompFileWriter {
		
		private PrintStream CompFileWritingStream;
		
		CompFileWriter(int iteration) {
			// create writing stream
			try {
				this.CompFileWritingStream = new PrintStream(new File(createOutputDirName("comparison") + "LinkFlows" + iteration + ".txt"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			// write header
			String header = "link id\tflow basic\tflow pricing\tflow diff";
			this.CompFileWritingStream.println(header);
		}
		
		void addLine(Id<Link> linkId, int flowValueBasic, int flowValuePricing, int flowValueDiff){
			StringBuffer line = new StringBuffer();
			line.append(linkId + "\t" + flowValueBasic + "\t" + flowValuePricing + "\t" + flowValueDiff);
			this.CompFileWritingStream.println(line.toString());
		}		
	}
	
	
	private static Controler createControler(boolean useCongestionPricing) {
		
		Config config = ConfigUtils.createConfig();
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setOutputDirectory(createOutputDirName(useCongestionPricing? "pricing" : "basic"));
		config.network().setInputFile(NETWORK_FILE);
		config.plans().setInputFile(PLANS_FILE);
		
		if (useCongestionPricing){
			config.controler().setLastIteration(200);
		} else {
			config.controler().setLastIteration(iterationNumberPerStep - 1);
		}
		
		config.planCalcScore().setBrainExpBeta( 20 );
		
		// set travelTimeBinSize (only has effect if reRoute is used)
		config.travelTimeCalculator().setTraveltimeBinSize( 10 );
		config.travelTimeCalculator().setTravelTimeCalculatorType(TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());
		
		// define strategies:
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultStrategy.ReRoute.toString());
			strat.setWeight(0.05);
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
		
		// adapt monetary distance cost rate
		// (should be negative. the smaller it is, the more counts the distance.
		// use -12.0 to balance time [h] and distance [m].
		// use -0.0033 to balance [s] and [m], -0.012 to balance [h] and [km], -0.0004 to balance [h] and 30[km]...
		// use -0.0 to use only time.)
		config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate( -0.0002 ); // Ihab: 20Cent=0.2Eur guter Wert pro km -> 0.0002 pro m

		config.planCalcScore().setMarginalUtilityOfMoney(1.0); // default is 1.0

		
		// choose maximal number of plans per agent. 0 means unlimited
		config.strategy().setMaxAgentPlanMemorySize( 5 );
		
		config.qsim().setStuckTime( 3600 );
		config.qsim().setRemoveStuckVehicles(false);
		
//		config.qsim().setStorageCapFactor( 0.7 );
//		config.qsim().setFlowCapFactor( 0.7 );
		
//		config.qsim().setStartTime(3600 * 0);
		config.qsim().setEndTime(3600 * 12);
		
		config.controler().setWriteEventsInterval(config.controler().getLastIteration());
		config.controler().setWritePlansInterval(config.controler().getLastIteration());
		
		config.vspExperimental().setWritingOutputEvents(true);
		config.planCalcScore().setWriteExperiencedPlans(false);
		config.controler().setCreateGraphs(true);
		
		// define activity types
		{
			ActivityParams dummyAct = new ActivityParams("dummy");
			dummyAct.setTypicalDuration(12 * 3600);
			dummyAct.setOpeningTime(5 * 3600);
			dummyAct.setLatestStartTime(10 * 3600);
			config.planCalcScore().addActivityParams(dummyAct);
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

		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);

		if (useCongestionPricing) {
			// add tolling
			TollHandler tollHandler = new TollHandler(scenario);

			// add correct TravelDisutilityFactory for tolls if ReRoute is used
			StrategySettings[] strategies = config.strategy().getStrategySettings().toArray(new StrategySettings[0]);
			for (int i = 0; i < strategies.length; i++) {
				if (strategies[i].getStrategyName().equals(DefaultStrategy.ReRoute.toString())) {
					if (strategies[i].getWeight() > 0.0) { // ReRoute is used
						final CongestionTollTimeDistanceTravelDisutilityFactory factory = new CongestionTollTimeDistanceTravelDisutilityFactory(new Builder(TransportMode.car, config.planCalcScore()),
								tollHandler, config.planCalcScore());
						factory.setSigma(SIGMA);
						controler.addOverridingModule(new AbstractModule() {
							@Override
							public void install() {
								this.bindCarTravelDisutilityFactory().toInstance(factory);
							}
						});
					}
				}
			}

			// choose the correct congestion handler and add it
			EventHandler congestionHandler = null;
			switch (PRICING_TYPE) {
			case V3:
				congestionHandler = new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario());
				break;
			case V4:
				congestionHandler = new CongestionHandlerImplV4(controler.getEvents(), controler.getScenario());
				break;
			case V7:
				congestionHandler = new CongestionHandlerImplV7(controler.getEvents(), controler.getScenario());
				break;
			case V8:
				congestionHandler = new CongestionHandlerImplV8(controler.getEvents(), controler.getScenario());
				break;
			case V9:
				congestionHandler = new CongestionHandlerImplV9(controler.getEvents(), controler.getScenario());
				break;
			case V10:
				congestionHandler = new CongestionHandlerImplV10(controler.getEvents(), controler.getScenario());
				break;
			default:
				break;
			}
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, congestionHandler));

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
		
		return controler;
	}

	private static String createOutputDirName(String suffix) {
		// get the current date in format "yyyy-mm-dd"
		Calendar cal = Calendar.getInstance();
		// this class counts months from 0, but days from 1
		int month = cal.get(Calendar.MONTH) + 1;
		String monthStr = month + "";
		if (month < 10)
			monthStr = "0" + month;
		String date = cal.get(Calendar.YEAR) + "-" + monthStr + "-" + cal.get(Calendar.DAY_OF_MONTH);

		String runName = date;
		
		runName += "_it" + iterationNumberPerStep;
		runName += "_flowValueSteps" + flowValueStepSize;
		runName += "_" + suffix;
		
		String outputDir = OUTPUT_BASE_DIR + runName + "/"; 
		// create directory
		new File(outputDir).mkdirs();
		
		return outputDir;
	}

}
