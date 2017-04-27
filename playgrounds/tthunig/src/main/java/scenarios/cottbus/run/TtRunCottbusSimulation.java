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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
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
import org.matsim.lanes.data.LanesWriter;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingModule;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import analysis.TtAnalyzedGeneralResultsWriter;
import analysis.TtGeneralAnalysis;
import analysis.TtListenerToBindGeneralAnalysis;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV10;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.CongestionHandlerImplV7;
import playground.vsp.congestion.handlers.CongestionHandlerImplV8;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;
import signals.CombinedSignalsModule;
import signals.downstreamSensor.DownstreamPlanbasedSignalController;
import signals.sylvia.controler.DgSylviaConfig;
import signals.sylvia.model.SylviaSignalController;
import utils.SignalizeScenario;

/**
 * Class to run a cottbus simulation.
 * 
 * @author tthunig
 *
 */
public class TtRunCottbusSimulation {

	private static final Logger LOG = Logger.getLogger(TtRunCottbusSimulation.class);
	
	private final static String RUN_ID = "3103c";
	
	private final static NetworkType NETWORK_TYPE = NetworkType.V1;
	public enum NetworkType {
		BTU_NET, // "network small simplified" in BTU_BASE_DIR
		V1, // network of the public-svn scenario from 2016-03-18 (same as from DG)
		V2, // add missing highway part, add missing links, correct directions, add missing signal
		V21, // add missing lanes
		V3 // double flow capacities of all signalized links and lanes
	}
	private final static PopulationType POP_TYPE = PopulationType.WoMines;
	public enum PopulationType {
		GRID_LOCK_BTU, // artificial demand: from every ingoing link to every outgoing link of the inner city ring
		BTU_POP_MATSIM_ROUTES,
		BTU_POP_BTU_ROUTES,
		WMines, // with mines as working places. causes an oversized number of working places in the south west of Cottbus.
		WoMines // without mines as working places
	}
	
	private final static SignalType SIGNAL_TYPE = SignalType.ALL_MS_AS_SYLVIA_INSIDE_ENVELOPE_REST_GREEN;
	public enum SignalType {
		NONE, MS, MS_RANDOM_OFFSETS, MS_SYLVIA, BTU_OPT, DOWNSTREAM_MS, DOWNSTREAM_BTUOPT, DOWNSTREAM_ALLGREEN, 
		ALL_NODES_ALL_GREEN, ALL_NODES_DOWNSTREAM, ALL_GREEN_INSIDE_ENVELOPE, 
		ALL_MS_INSIDE_ENVELOPE_REST_GREEN, // all MS systems fixed-time, rest all green
		ALL_MS_AS_SYLVIA_INSIDE_ENVELOPE_REST_GREEN, // all MS systems as sylvia with MS basis, rest all green. note: green basis for sylvia does not work
		ALL_MS_AS_DOWNSTREAM_INSIDE_ENVELOPE_REST_GREEN, // all MS systems as downstream with MS basis, rest all green
		ALL_DOWNSTREAM_INSIDE_ENVELOPE_BASIS_MS, // all MS systems as downstream with MS basis, rest downstream with green basis
		ALL_DOWNSTREAM_INSIDE_ENVELOPE_BASIS_GREEN, // all systems inside envelope downstream with green basis
		ALL_MS_AS_DOWNSTREAM_BASIS_GREEN_INSIDE_ENVELOPE_REST_GREEN // all MS systems as downstream with green basis, rest all green
	}
	
	// defines which kind of pricing should be used
	private static final PricingType PRICING_TYPE = PricingType.NONE;
	private enum PricingType {
		NONE, CP_V3, CP_V4, CP_V7, CP_V8, CP_V9, CP_V10, FLOWBASED, CORDON_INNERCITY, CORDON_RING
	}
	
	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private static final double SIGMA = 0.0;
	
	private static final String OUTPUT_BASE_DIR = "../../../runs-svn/cottbus/createGridLock/";
	private static final String INPUT_BASE_DIR = "../../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/";
	private static final String BTU_BASE_DIR = "../../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2015-02-25_minflow_50.0_morning_peak_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";
	
	private static final boolean WRITE_INITIAL_FILES = true;
	private static final boolean USE_COUNTS = false;
	private static final double SCALING_FACTOR = 1.;
	
	public static void main(String[] args) {		
		Config config = defineConfig();
		
//		OTFVisConfigGroup otfvisConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class ) ;
//		otfvisConfig.setDrawTime(true);
//		otfvisConfig.setAgentSize(80f);
		
		Scenario scenario = prepareScenario( config );
		Controler controler = prepareController( scenario );
		
//		controler.addOverridingModule( new OTFVisWithSignalsLiveModule() ) ;
		
		controler.run();
	}

	private static Config defineConfig() {
		Config config = ConfigUtils.createConfig();

		switch (NETWORK_TYPE) {
		case BTU_NET:
			config.network().setInputFile(BTU_BASE_DIR + "network_small_simplified.xml.gz");
			config.network().setLaneDefinitionsFile(BTU_BASE_DIR + "lanes_network_small.xml.gz");
			break;
		case V1:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n.xml.gz");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes.xml");
			break;
		case V2:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_v2.xml");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes.xml");
			break;
		case V21:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_v2.xml");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes_v2.1.xml");
			break;
		case V3:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_v3.xml");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes_v3.xml");
			break;
		}
		if (SIGNAL_TYPE.toString().startsWith("ALL") && !SIGNAL_TYPE.toString().contains("MS")){
			// if signal type 'All...' without 'MS' is used, lanes are defined later in 'prepareScenario'
			config.network().setLaneDefinitionsFile(null);
		}
		
		switch (POP_TYPE) {
		case BTU_POP_MATSIM_ROUTES:
			config.plans().setInputFile(BTU_BASE_DIR + "trip_plans_from_morning_peak_ks_commodities_minFlow50.0.xml");
			break;
		case BTU_POP_BTU_ROUTES:
			config.plans().setInputFile(BTU_BASE_DIR + "routeComparison/2015-03-10_sameEndTimes_ksOptRouteChoice_paths.xml");
			break;
		case WMines:
			config.plans().setInputFile(INPUT_BASE_DIR + "cb_spn_gemeinde_nachfrage_landuse/commuter_population_wgs84_utm33n_car_only.xml.gz");
			break;
		case WoMines:
			// TODO choose one
			// BaseCase output plans. the number specifies the flow and storage capacity that was used	
			config.plans().setInputFile(OUTPUT_BASE_DIR + "run3100/3100.output_plans.xml.gz");
			// BaseCase plans, no routes
//			config.plans().setInputFile(INPUT_BASE_DIR + "cb_spn_gemeinde_nachfrage_landuse_woMines/commuter_population_wgs84_utm33n_car_only.xml.gz");
			// BaseCase plans, no links for acitivties, no routes
//			config.plans().setInputFile(INPUT_BASE_DIR + "cb_spn_gemeinde_nachfrage_landuse_woMines/commuter_population_wgs84_utm33n_car_only_woLinks.xml.gz");
			break;
		case GRID_LOCK_BTU:
			// take these as initial plans
			if (SIGNAL_TYPE.equals(SignalType.MS) || SIGNAL_TYPE.equals(SignalType.DOWNSTREAM_MS)){
				config.plans().setInputFile(OUTPUT_BASE_DIR + "2017-02-2_100it_ReRoute0.1_tbs10_ChExp0.9_beta2_lanes_2link_MS_5plans_GRID_LOCK_BTU_BTU_NET_3600'12'6/output_plans.xml.gz");
			} else if (SIGNAL_TYPE.toString().startsWith("ALL")){
				config.plans().setInputFile(OUTPUT_BASE_DIR + "2017-02-3_100it_ReRoute0.1_tbs10_ChExp0.9_beta2_lanes_2link_ALL_NODES_ALL_GREEN_5plans_GRID_LOCK_BTU_BTU_NET_3600'12'3/output_plans.xml.gz");
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2017-02-3_100it_ReRoute0.1_tbs10_ChExp0.9_beta2_lanes_2link_ALL_NODES_DOWNSTREAM_5plans_GRID_LOCK_BTU_BTU_NET_3600'12'3/output_plans.xml.gz");						
			}
			break;
		default:
			break;
		}
		// // pt scenario
		// config.network().setInputFile(INPUT_BASE_DIR + "Cottbus-pt/INPUT_mod/public/input/network_improved.xml");
		// config.plans().setInputFile(INPUT_BASE_DIR + "Cottbus-pt/INPUT_mod/public/input/plans_scale1.4false.xml");

		// set number of iterations
		// TODO
		config.controler().setFirstIteration(100);
		config.controler().setLastIteration(200);

		// able or enable signals and lanes
		// if signal type 'All...' is used without 'MS', lanes and signals are defined later in 'prepareScenario'
		if (!SIGNAL_TYPE.equals(SignalType.NONE) && (SIGNAL_TYPE.toString().contains("MS") || !SIGNAL_TYPE.toString().startsWith("ALL"))) {
			SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
			signalConfigGroup.setUseSignalSystems(true);
			config.qsim().setUseLanes(SIGNAL_TYPE.toString().startsWith("ALL") && !SIGNAL_TYPE.toString().contains("MS") ? false : true);
			// set signal systems
			switch (NETWORK_TYPE) {
			case V1:
				signalConfigGroup.setSignalSystemFile(INPUT_BASE_DIR + "signal_systems_no_13.xml");
				break;
			case BTU_NET:
//				signalConfigGroup.setSignalSystemFile(BTU_BASE_DIR + "output_signal_systems_v2.0.xml.gz"); // gives SAXParseException: Content is not allowed in prolog
				signalConfigGroup.setSignalSystemFile(BTU_BASE_DIR + "signal_systems_no_13_btuNet.xml");
				break;
			case V2:
				signalConfigGroup.setSignalSystemFile(INPUT_BASE_DIR + "signal_systems_no_13_v2.xml");
				break;
			default:
				signalConfigGroup.setSignalSystemFile(INPUT_BASE_DIR + "signal_systems_no_13_v2.1.xml");
				break;
			}			
			// set signal group
			if (NETWORK_TYPE.equals(NetworkType.V1) || NETWORK_TYPE.equals(NetworkType.BTU_NET)) {
				signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_no_13.xml");
			} else {
				signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_no_13_v2.xml");
			}
			// set signal control
			switch (SIGNAL_TYPE) {
			case MS:
			case DOWNSTREAM_MS: // will be changed to downstream later
			case DOWNSTREAM_ALLGREEN: // will be changed to all day green and downstream later
			case ALL_MS_AS_DOWNSTREAM_BASIS_GREEN_INSIDE_ENVELOPE_REST_GREEN: // will be changed to all day green and downstream later; additional signal systems will be added later
			case ALL_MS_INSIDE_ENVELOPE_REST_GREEN: // additional signal systems will be added later
			case ALL_MS_AS_DOWNSTREAM_INSIDE_ENVELOPE_REST_GREEN: // will be changed to downstream later; additional signal systems will be added later
			case ALL_DOWNSTREAM_INSIDE_ENVELOPE_BASIS_MS: // additional signal systems will be added later
				if (NETWORK_TYPE.equals(NetworkType.V1) || NETWORK_TYPE.equals(NetworkType.BTU_NET)) {
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_no_13.xml");
				} else {
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_no_13_v2.xml");
				}
				break;
			case MS_RANDOM_OFFSETS:
				if (NETWORK_TYPE.equals(NetworkType.V1) || NETWORK_TYPE.equals(NetworkType.BTU_NET)) {
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_no_13_random_offsets.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case MS_SYLVIA:
			case ALL_MS_AS_SYLVIA_INSIDE_ENVELOPE_REST_GREEN:
				if (NETWORK_TYPE.equals(NetworkType.V1)){
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_sylvia_no_13.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case BTU_OPT:
			case DOWNSTREAM_BTUOPT:
				if (NETWORK_TYPE.equals(NetworkType.V1) || NETWORK_TYPE.equals(NetworkType.BTU_NET)) {
					signalConfigGroup.setSignalControlFile(BTU_BASE_DIR + "btu/signal_control_opt.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			}
		}
		if (PRICING_TYPE.toString().startsWith("CORDON_")){
			RoadPricingConfigGroup roadPricingCG = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
			// TODO adapt toll value here
			if (PRICING_TYPE.equals(PricingType.CORDON_INNERCITY)){
				roadPricingCG.setTollLinksFile(INPUT_BASE_DIR + "cordonToll/tollLinksFile_innerCityWoRing_100.xml");
			} else { // PricingType.CORDON_RING
				roadPricingCG.setTollLinksFile(INPUT_BASE_DIR + "cordonToll/tollLinksFile_innerCityWRing_100.xml");				
			}
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
		config.travelTimeCalculator().setTraveltimeBinSize( 900 );

		config.travelTimeCalculator().setTravelTimeCalculatorType(TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());
		// hash map and array produce same results. only difference: memory and time.
		// for small time bins and sparse values hash map is better. theresa, may'15

		// define strategies:
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultStrategy.ReRoute.toString());
			if (POP_TYPE.equals(PopulationType.BTU_POP_BTU_ROUTES))
				strat.setWeight(0.0); // no ReRoute, fix route choice set
			else
				strat.setWeight(0.1);
			strat.setDisableAfter(config.controler().getLastIteration() - config.controler().getFirstIteration() > 200 ? 
					config.controler().getLastIteration() - 100 : config.controler().getLastIteration() - 50);
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
		if (POP_TYPE.equals(PopulationType.BTU_POP_BTU_ROUTES))
			config.strategy().setMaxAgentPlanMemorySize(0); //unlimited because ReRoute is switched off anyway
		else 
			config.strategy().setMaxAgentPlanMemorySize( 5 );

		config.qsim().setStuckTime( 3600 );
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setStartTime(3600 * 5); 
		// TODO change to a higher value for congested scenarios
		config.qsim().setEndTime(24.*3600.);
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(false); // false is default
//		config.qsim().setTrafficDynamics(TrafficDynamics.withHoles); // queue is default
		
		if (NETWORK_TYPE.equals(NetworkType.BTU_NET)){
			LOG.warn("Keep in mind that the btu network capacity has already been scaled");
		}
//		config.qsim().setStorageCapFactor( SCALING_FACTOR );
		/* storage cap should be scaled less than flow capacity factor. 
		 * read in NicolaiNagelHiResAccessibilityMethod (2014), p.75f. (or p.9 in preprint), they mention RieserNagel2008NetworkBreakdown as reference */
		config.qsim().setStorageCapFactor( SCALING_FACTOR / Math.pow(SCALING_FACTOR,1/4.) );
		config.qsim().setFlowCapFactor( SCALING_FACTOR );
		
		// adapt monetary distance cost rate
		// (should be negative. the smaller it is, the more counts the distance.
		// use -12.0 to balance time [h] and distance [m].
		// use -0.0033 to balance [s] and [m], -0.012 to balance [h] and [km], -0.0004 to balance [h] and 30[km]...
		// use -0.0 to use only time.)
		config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate( -0.0 ); // Ihab: 20Cent=0.2Eur guter Wert pro km -> 0.0002 pro m

		config.planCalcScore().setMarginalUtilityOfMoney(1.0); // default is 1.0

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		// note: overwriteExistingFiles necessary when 'writeInputFiles' is true
		// note: the output directory is defined in createRunNameAndOutputDir(...) after all adaptations are done

		config.vspExperimental().setWritingOutputEvents(true);
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.controler().setCreateGraphs(true);

		config.controler().setWriteEventsInterval(config.controler().getLastIteration());
		config.controler().setWritePlansInterval(config.controler().getLastIteration());

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
		
		config.global().setCoordinateSystem("EPSG:25833"); //UTM33
		
		// add counts module
		if (USE_COUNTS) {
			if (!NETWORK_TYPE.equals(NetworkType.V1)){
				throw new UnsupportedOperationException("In this scenario, counts can only be used together with NetworkType.V1"
						+ " because they are not available for other simplified networks.");
			}
//			config.counts().setCountsFileName(INPUT_BASE_DIR + "CottbusCounts/counts_matsim/counts_final_shifted.xml");
			config.counts().setInputFile(INPUT_BASE_DIR + "CottbusCounts/counts_matsim/counts_final_shifted_v2.xml");
			config.counts().setCountsScaleFactor(1.0 / SCALING_FACTOR); // sample size
			config.counts().setWriteCountsInterval(config.controler().getLastIteration());
//			config.counts().setWriteCountsInterval(1);
			config.counts().setOutputFormat("all");
//			config.counts().setInputCRS(inputCRS);
			config.counts().setAverageCountsOverIterations(10);
		}
		
		return config;
	}

	private static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);	
	
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		}

		// adoptions for some signal types necessary:
		switch (SIGNAL_TYPE) {
		case DOWNSTREAM_BTUOPT:
		case DOWNSTREAM_MS:
		case DOWNSTREAM_ALLGREEN:
		case ALL_MS_AS_DOWNSTREAM_BASIS_GREEN_INSIDE_ENVELOPE_REST_GREEN:
			// adapt signal controller for downstream signal control
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			for (SignalSystemControllerData controllerData : signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().values()) {
				controllerData.setControllerIdentifier(DownstreamPlanbasedSignalController.IDENTIFIER);
				if (SIGNAL_TYPE.toString().contains("GREEN")){
					// change to all day green
					for (SignalPlanData planData : controllerData.getSignalPlanData().values()) {
						for (SignalGroupSettingsData groupSetting : planData.getSignalGroupSettingsDataByGroupId().values()) {
							groupSetting.setOnset(0);
							groupSetting.setDropping(planData.getCycleTime()-1);
						}
					}
				}
			}
			if (SIGNAL_TYPE.equals(SignalType.ALL_MS_AS_DOWNSTREAM_BASIS_GREEN_INSIDE_ENVELOPE_REST_GREEN)){
				// add additional all day green signals inside envelope
				SignalizeScenario signalizer = new SignalizeScenario(scenario);
				signalizer.setOverwriteSignals(false);
				signalizer.setBoundingBox(INPUT_BASE_DIR + "shape_files/signal_systems/bounding_box.shp");
				// note: no specific signal controller identifier - additional signals should show green all day
				signalizer.createSignalsAndLanesForAllTurnings();
			}
			break;
		case ALL_DOWNSTREAM_INSIDE_ENVELOPE_BASIS_MS:
			// change all signal controller to downstream
			signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			for (SignalSystemControllerData controllerData : signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().values()) {
				controllerData.setControllerIdentifier(DownstreamPlanbasedSignalController.IDENTIFIER);
			}
			// note: no break - should continue! (signals of following types are replaced anyway)
		case ALL_NODES_ALL_GREEN:
		case ALL_NODES_DOWNSTREAM:
		case ALL_GREEN_INSIDE_ENVELOPE:
		case ALL_DOWNSTREAM_INSIDE_ENVELOPE_BASIS_GREEN:
			// signalize all intersections (or all inside envelope or all that are not signalized yet), create corresponding lanes
			SignalizeScenario signalizer = new SignalizeScenario(scenario);
			if (SIGNAL_TYPE.toString().contains("BASIS_MS"))
				signalizer.setOverwriteSignals(false);
			if (SIGNAL_TYPE.toString().contains("DOWNSTREAM"))
				signalizer.setSignalControlIdentifier(DownstreamPlanbasedSignalController.IDENTIFIER);
			if (SIGNAL_TYPE.toString().contains("ENVELOPE"))
				signalizer.setBoundingBox(INPUT_BASE_DIR + "shape_files/signal_systems/bounding_box.shp");
			signalizer.createSignalsAndLanesForAllTurnings();
			break;		
		case ALL_MS_AS_DOWNSTREAM_INSIDE_ENVELOPE_REST_GREEN:
			// change all signal controller to downstream
			signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			for (SignalSystemControllerData controllerData : signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().values()) {
				controllerData.setControllerIdentifier(DownstreamPlanbasedSignalController.IDENTIFIER);
			} 
			// note: no break - should continue! (following sylvia and ms already have correct signal controller)
		case ALL_MS_AS_SYLVIA_INSIDE_ENVELOPE_REST_GREEN:
		case ALL_MS_INSIDE_ENVELOPE_REST_GREEN:
			// add additional all day green signals inside envelope
			signalizer = new SignalizeScenario(scenario);
			signalizer.setOverwriteSignals(false);
			signalizer.setBoundingBox(INPUT_BASE_DIR + "shape_files/signal_systems/bounding_box.shp");
			// note: no specific signal controller identifier - additional signals should show green all day
			signalizer.createSignalsAndLanesForAllTurnings();
			break;
		default:
			break;
		}
		
//		// adoptions for artificial population (GRID_LOCK_BTU)
//		if (POP_TYPE.equals(PopulationType.GRID_LOCK_BTU)){
//			createArificialGridLockPopulation(scenario);
//		}
		
		createRunNameAndOutputDir(scenario);
		if (WRITE_INITIAL_FILES){ 
			writeInitFiles(scenario);
		}
		
		return scenario;
	}

	private static void createArificialGridLockPopulation(Scenario scenario) {
		String[] inLinks = {"7919", "4909", "1281", "506", "3411", "2100-2098-5377", "6230",
				"40", "3503", "6663", "2759", "2475"};
		String[] outLinks = {"7918", "4908", "1282", "4511", "3418", "5376-2097-2099", "6213",
				"8632", "3490", "6662", "2758", "6747"};
		List<Id<Link>> inLinkIds = new LinkedList<>();
		List<Id<Link>> outLinkIds = new LinkedList<>();
		for (int i=0; i<inLinks.length; i++){
			inLinkIds.add(Id.createLinkId(inLinks[i]));
			outLinkIds.add(Id.createLinkId(outLinks[i]));
		}
		
		Population pop = scenario.getPopulation();
		
		int numberOfPersonsPerODPerH = 3600/12/3;
		double simulationPeriod_h = 3;
		double simulationStartTime_s = 6*3600;
		for (Id<Link> inLinkId : inLinkIds){
			for (Id<Link> outLinkId : outLinkIds){
				for (int i = 0; i < numberOfPersonsPerODPerH * simulationPeriod_h; i++) {
					// create a person
					Person person = pop.getFactory().createPerson(Id.createPersonId(inLinkId + "_" + outLinkId + "_" + i));

					// create a start activity at the inLink
					Activity startAct = pop.getFactory().createActivityFromLinkId("dummy", inLinkId);
					// distribute agents uniformly between simulationStartTime and (simulationStartTime + simulationPeriod) am.
					startAct.setEndTime(simulationStartTime_s + (double)(i)/(numberOfPersonsPerODPerH * simulationPeriod_h) * simulationPeriod_h * 3600);
				
					// create a drain activity at outLink
					Activity drainAct = pop.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(outLinkId));
					
					// create a dummy leg
					Leg leg = pop.getFactory().createLeg(TransportMode.car);
					
					// create a plan for the person that contains all this information
					Plan plan = pop.getFactory().createPlan();
					plan.addActivity(startAct);
					plan.addLeg(leg);
					plan.addActivity(drainAct);
					
					// store information in population
					person.addPlan(plan);
					
					pop.addPerson(person);
				}
			}
		}		
	}

	private static Controler prepareController(Scenario scenario) {
		Config config = scenario.getConfig();
		Controler controler = new Controler(scenario);

		// add the signals module if signal systems are used
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		if (signalsConfigGroup.isUseSignalSystems()) {
			CombinedSignalsModule signalsModule = new CombinedSignalsModule();
			DgSylviaConfig sylviaConfig = new DgSylviaConfig();
			// TODO change for sylvia
			sylviaConfig.setUseFixedTimeCycleAsMaximalExtension(false);
			sylviaConfig.setSignalGroupMaxGreenScale(1.5);
//			sylviaConfig.setCheckDownstream(true);
			signalsModule.setSylviaConfig(sylviaConfig);
			controler.addOverridingModule(signalsModule);
		}
		
		if (PRICING_TYPE.toString().startsWith("CP_")){
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
			case CP_V3:
				congestionHandler = new CongestionHandlerImplV3(controler.getEvents(), 
						controler.getScenario());
				break;
			case CP_V4:
				congestionHandler = new CongestionHandlerImplV4(controler.getEvents(), 
						controler.getScenario());
				break;
			case CP_V7:
				congestionHandler = new CongestionHandlerImplV7(controler.getEvents(), 
						controler.getScenario());
				break;
			case CP_V8:
				congestionHandler = new CongestionHandlerImplV8(controler.getEvents(), 
						controler.getScenario());
				break;
			case CP_V9:
				congestionHandler = new CongestionHandlerImplV9(controler.getEvents(), 
						controler.getScenario());
				break;
			case CP_V10:
				congestionHandler = new CongestionHandlerImplV10(controler.getEvents(), 
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
		} else if (PRICING_TYPE.toString().startsWith("CORDON_")){
			// (loads the road pricing scheme, uses custom travel disutility including tolls, etc.)
			controler.addOverridingModule(new RoadPricingModule());
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
		} else { 
			if (storeCap != 1.0)
				runName += "_storeCap" + storeCap;
			if (flowCap != 1.0)
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
				runName += "_2link";
			else
				runName += "_2node";
		}			

		if (ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME,
				SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			switch (SIGNAL_TYPE){
			case BTU_OPT:
				runName += "_BtuOpt";
				break;
			case MS_RANDOM_OFFSETS:
				runName += "_rdmOff";
				break;
			case DOWNSTREAM_MS:
				runName += "_dwnBC";
				break;
			case DOWNSTREAM_ALLGREEN:
				runName += "_dwnGreen";
				break;
			case DOWNSTREAM_BTUOPT:
				runName += "_dwnOPT";
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
			runName += "_" + config.strategy().getMaxAgentPlanMemorySize() + "plans";

		if (USE_COUNTS){
			runName += "_counts";
		}
		
		runName += "_" + POP_TYPE;
		runName += "_" + NETWORK_TYPE;
		
		String outputDir = OUTPUT_BASE_DIR + "run" + RUN_ID + "/"; 
//		String outputDir = OUTPUT_BASE_DIR + runName + "/"; 
		// create directory
		new File(outputDir).mkdirs();

		config.controler().setOutputDirectory(outputDir);
		LOG.info("The output will be written to " + outputDir);
		
		config.controler().setRunId(RUN_ID);
		
		// write run description
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
		// correct paths before writing config
		scenario.getConfig().network().setInputFile("network.xml");
		if (scenario.getConfig().qsim().isUseLanes()) {
			new LanesWriter(scenario.getLanes()).write(outputDir + "lanes.xml");
			scenario.getConfig().network().setLaneDefinitionsFile("lanes.xml");
		}
		String outputDirTmp = scenario.getConfig().controler().getOutputDirectory();
		// adapt output dir to be able to run it on the cluster
		scenario.getConfig().controler().setOutputDirectory("/net/ils3/thunig/runs-svn/cottbus/createGridLock/run" + RUN_ID + "/");
		
		// write population
		new PopulationWriter(scenario.getPopulation()).write(outputDir + "plans.xml");
		scenario.getConfig().plans().setInputFile("plans.xml");
		
		// write signal files
		if (!SIGNAL_TYPE.equals(SignalType.NONE)) {
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			new SignalSystemsWriter20(signalsData.getSignalSystemsData()).write(outputDir + "signalSystems.xml");
			new SignalControlWriter20(signalsData.getSignalControlData()).write(outputDir + "signalControl.xml");
			new SignalGroupsWriter20(signalsData.getSignalGroupsData()).write(outputDir + "signalGroups.xml");
			
			SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
					SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
			signalsConfigGroup.setSignalSystemFile("signalSystems.xml");
			signalsConfigGroup.setSignalGroupsFile("signalGroups.xml");
			signalsConfigGroup.setSignalControlFile("signalControl.xml");
		}
		
		// write config
		new ConfigWriter(scenario.getConfig()).write(outputDir + "config.xml");
		
		// restore output dir
		scenario.getConfig().controler().setOutputDirectory(outputDirTmp);
	}

}
