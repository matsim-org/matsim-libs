/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.dgrether.daganzosignal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v11.LaneData11;
import org.matsim.lanes.data.v11.LaneDefinitions11;
import org.matsim.lanes.data.v11.LaneDefinitions11Impl;
import org.matsim.lanes.data.v11.LaneDefinitionsFactory11;
import org.matsim.lanes.data.v11.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.data.v11.LanesToLinkAssignment11;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;

import playground.dgrether.DgPaths;
import playground.dgrether.DgPlaygroundJobfileCreator;

/**
 * @author dgrether
 *
 */
public class DaganzoScenarioGenerator {

	private static final Logger log = Logger
			.getLogger(DaganzoScenarioGenerator.class);

	public static String DAGANZO_SVN_DIR = "shared-svn/studies/dgrether/daganzo2012/";

	public static String DAGANZOBASEDIR = DgPaths.REPOS + DAGANZO_SVN_DIR;

	public static final String DAGANZO_NETWORK = "daganzoNetwork2.xml";

	public static final String DAGANZONETWORKFILE = DAGANZOBASEDIR
			+DAGANZO_NETWORK;

	public static final String CONFIG_MODULE = "daganzoSignal";

	public static final String PSIGNAL_CONFIG_PARAMETER = "psignal";

	public static final String SPLITSG1LINK4_CONFIG_PARAMETER = "splitSg1Link4";

	public static final String INTIAL_RED_NORMAL_ROUTE_CONFIG_PARAMETER = "initialRedNormalRoute";

	//	public static final String NETWORKFILE =  DAGANZOBASEDIR + "daganzoNetworkNoLanes.xml";//DAGANZONETWORKFILE;
	public static final String NETWORKFILE =  DAGANZONETWORKFILE;

	private static final String PLANS1OUT = DAGANZOBASEDIR
			+ "daganzo2PlansNormalRoute.xml";

	private static final String PLANS2OUT = DAGANZOBASEDIR
			+ "daganzo2PlansAlternativeRoute.xml";

	private static final String CONFIG1OUT = DAGANZOBASEDIR
			+ "daganzoConfigNormalRoutePlansOnly.xml";

	private static final String CONFIG2OUT = DAGANZOBASEDIR
			+ "daganzoConfigAlternativeRoutePlansOnly.xml";

	public static final String DAGANZO_LANES = "daganzoLaneDefinitions.xml";

	public static final String LANESOUTPUTFILE = DAGANZOBASEDIR
		+ DAGANZO_LANES;

	public static final String DAGANZO_SIGNALS = "daganzoSignalSystems.xml";

	public static final String SIGNALSYSTEMSOUTPUTFILE = DAGANZOBASEDIR
		+ DAGANZO_SIGNALS;

	public static final String DAGANZO_SIGNALS_CONFIG = "daganzoSignalSystemsConfigs6.xml";

	public static final String SIGNALSYSTEMCONFIGURATIONSOUTPUTFILE = DAGANZOBASEDIR
		+ DAGANZO_SIGNALS_CONFIG;


	public String configOut, plansOut, outputDirectory;


	private static final boolean isUsePlansOnly = true;

	private static final boolean isUseLanes = true;

	private static final boolean isUseSignalSystems = true;

	public static int iterations = 4000;

	private static final int agents = 5000;

	private static final int ttBinSize = 1;

	private final static Double pSignal = null;//0.5;

	private final static Double splitSgLink4 = null;

	public static boolean isAlternativeRouteEnabled = false;

	private Double initialRedNormalRoute = 1500.0;

	private double brainExpBeta = 2.0;

//	private static final String controllerClass = AdaptiveController6.class.getCanonicalName();

	public static final String runId = "1190";//"";

	private static final boolean writerClusterFiles = false;

	private boolean doOtfOutput = true;

	private int numberOfLanes = 3;

	private String networkInputFile;

	private String lanesInputFile;

	private String signalInputFile;

	private String signalConfigInputFile;

	private String plansInputFile;

	private MutableScenario scenario = null;


	public DaganzoScenarioGenerator() {
		this.scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		init();
	}

	private void init() {
		String baseString = "daganzo4_";
		if (isAlternativeRouteEnabled) {
			baseString += "alternativeRoute_";
//			plansOut = PLANS2OUT;
//			configOut = CONFIG2OUT;
//			outputDirectory = OUTPUTDIRECTORYALTERNATIVEROUTE;
		}
		else {
			baseString += "normalRoute_";
//			plansOut = PLANS1OUT;
//			configOut = CONFIG1OUT;
//			outputDirectory = OUTPUTDIRECTORYNORMALROUTE;
		}
//		baseString += "usePlansOnly-" + Boolean.toString(isUsePlansOnly) + "_";
//		baseString += "noAgents-" + Integer.toString(agents) + "_";
		baseString += "iterations-" + Integer.toString(iterations) + "_";
//		if (pSignal != null) {
//			baseString += "pSignal-" + pSignal + "_";
//		}
//		if (splitSgLink4 != null) {
//			baseString += "splitSgLink4-" + splitSgLink4 + "_";
//		}
//		baseString += "ttbinsize-" + ttBinSize;

		if (writerClusterFiles){
			configOut = DAGANZOBASEDIR + runId + "_config.xml";
			plansOut = DAGANZOBASEDIR  + runId + "_plans.xml.gz";
			outputDirectory = DgPaths.CLUSTER_MATSIM_OUTPUT + "run" +  runId;
			networkInputFile = DgPaths.CLUSTERSVN + DAGANZO_SVN_DIR + DAGANZO_NETWORK;
			lanesInputFile = DgPaths.CLUSTERSVN + DAGANZO_SVN_DIR + DAGANZO_LANES;
			signalInputFile = DgPaths.CLUSTERSVN + DAGANZO_SVN_DIR + DAGANZO_SIGNALS;
			signalConfigInputFile = DgPaths.CLUSTERSVN + DAGANZO_SVN_DIR + DAGANZO_SIGNALS_CONFIG;
			plansInputFile = DgPaths.CLUSTERSVN + DAGANZO_SVN_DIR + runId + "_plans.xml.gz";
		}
		else {
			plansOut = DAGANZOBASEDIR + "plans_" + baseString + ".xml";
			configOut = DAGANZOBASEDIR + "config_" + baseString + ".xml";
			outputDirectory = DAGANZOBASEDIR + baseString + "/";
			networkInputFile = DAGANZOBASEDIR + DAGANZO_NETWORK;
			lanesInputFile = DAGANZOBASEDIR + DAGANZO_LANES;
			signalInputFile = DAGANZOBASEDIR + DAGANZO_SIGNALS;
			signalConfigInputFile = DAGANZOBASEDIR + DAGANZO_SIGNALS_CONFIG;
			plansInputFile = DAGANZOBASEDIR  + "plans_" + baseString + ".xml";
		}


	}

	public void createScenario() {
		//get the config
		Config config = scenario.getConfig();
		//set the network input file to the config and load it
		config.network().setInputFile(NETWORKFILE);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);
		
		
		//create the plans and write them
//		createPlans(scenario);
//		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(plansOut);
		if (isUseLanes) {
			config.qsim().setUseLanes(true);
			config.network().setLaneDefinitionsFile(LANESOUTPUTFILE);
			//create the lanes and write them
			Lanes lanes = createLanes(scenario);
			LaneDefinitionsWriter20 laneWriter = new LaneDefinitionsWriter20(lanes);
			laneWriter.write(LANESOUTPUTFILE);
		}
		if (isUseSignalSystems) {
			//enable lanes and signal system feature in config
			ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(true);
			ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalSystemFile(SIGNALSYSTEMSOUTPUTFILE);
			ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalControlFile(SIGNALSYSTEMCONFIGURATIONSOUTPUTFILE);
			//create the signal systems and write them
//			SignalSystems signalSystems = createSignalSystems(scenario);
//			MatsimSignalSystemsWriter ssWriter = new MatsimSignalSystemsWriter(signalSystems);
//			ssWriter.writeFile(SIGNALSYSTEMSOUTPUTFILE);
			//create the signal system's configurations and write them
//			SignalSystemConfigurations ssConfigs = createSignalSystemsConfig(scenario);
//			MatsimSignalSystemConfigurationsWriter ssConfigsWriter = new MatsimSignalSystemConfigurationsWriter(ssConfigs);
//			ssConfigsWriter.writeFile(SIGNALSYSTEMCONFIGURATIONSOUTPUTFILE);
		}

		//create and write the config with the correct paths to the files created above
		createConfig(config);
		new ConfigWriter(config).write(configOut);

		DgPlaygroundJobfileCreator.createJobfile(DAGANZOBASEDIR + runId + "jobfile",
				DgPaths.CLUSTERBASE + DAGANZO_SVN_DIR + runId + "_config.xml", runId);

		log.info("scenario written!");
	}



	private void createPlans(MutableScenario scenario) {
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();
		double firstHomeEndTime =  600.0;
		double homeEndTime = firstHomeEndTime;
//		Link l1 = network.getLinks().get(scenario.createId("1"));
//		Link l7 = network.getLinks().get(scenario.createId("7"));
		PopulationFactory factory = population.getFactory();

		for (int i = 1; i <= this.agents; i++) {
			Person p = factory.createPerson(Id.create(i, Person.class));
			// home
			// homeEndTime = homeEndTime + ((i - 1) % 3);
			homeEndTime+= 1;
//			if ((i - 1) % 3 == 0) {
//				homeEndTime++;
//			}
			Plan plan = null;
			Plan plan2 = null;
			if (isUsePlansOnly){
			  plan = this.createPlan(false, factory, homeEndTime, network);
        plan2 = this.createPlan(true, factory, homeEndTime, network);
//        plan.setScore(-0.2733333333333334);
//        plan2.setScore(-0.2733333333333334);
        if (isAlternativeRouteEnabled){
        	p.addPlan(plan2);
        	p.addPlan(plan);
          p.setSelectedPlan(plan2);
        }
        else {
        	p.addPlan(plan);
        	p.addPlan(plan2);
          p.setSelectedPlan(plan);
        }

			}
			else {
			  plan = this.createPlan(isAlternativeRouteEnabled, factory, homeEndTime, network);
			  p.addPlan(plan);
			}

			population.addPerson(p);
		}
	}

	private Plan createPlan(boolean useAlternativeRoute, PopulationFactory factory,
	    double homeEndTime, Network network){
    Plan plan = factory.createPlan();
    // home
    // homeEndTime = homeEndTime + ((i - 1) % 3);
    homeEndTime+= 1;
//    if ((i - 1) % 3 == 0) {
//      homeEndTime++;
//    }
    Activity act1 = (Activity) factory.createActivityFromLinkId("h", Id.create(1, Link.class));
    act1.setEndTime(homeEndTime);
    plan.addActivity(act1);
    // leg to home
    Leg leg = (Leg) factory.createLeg(TransportMode.car);
    LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(Id.create(1, Link.class), Id.create(7, Link.class));
    if (useAlternativeRoute) {
      route.setLinkIds(Id.create(1, Link.class), NetworkUtils.getLinkIds("2 3 5 6"), Id.create(7, Link.class));
    }
    else {
      route.setLinkIds(Id.create(1, Link.class), NetworkUtils.getLinkIds("2 4 6"), Id.create(7, Link.class));
    }
    leg.setRoute(route);

    plan.addLeg(leg);

    Activity act2 = (Activity) factory.createActivityFromLinkId("h", Id.create(7, Link.class));
    act2.setLinkId(Id.create(7, Link.class));
    plan.addActivity(act2);
    return plan;
	}

	private void createConfig(Config config) {
	// set scenario
		config.network().setInputFile(networkInputFile);
		if (isAlternativeRouteEnabled){
			if (writerClusterFiles){
				config.plans().setInputFile(DgPaths.CLUSTERBASE  + DAGANZO_SVN_DIR + "alternate_plans.xml");
			}
			else {
				config.plans().setInputFile( DAGANZOBASEDIR + "alternate_plans.xml");
			}
		}
		else {
			if (writerClusterFiles){
				config.plans().setInputFile(DgPaths.CLUSTERBASE + DAGANZO_SVN_DIR + "normal_plans.xml");
			}
			else{
				config.plans().setInputFile(DAGANZOBASEDIR + "normal_plans.xml");
			}
		}
		if (isUseLanes){
			config.network().setLaneDefinitionsFile(lanesInputFile);
		}
		if (isUseSignalSystems){
			ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalSystemFile(signalInputFile);
			ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalControlFile(this.signalConfigInputFile);
		}

		if (runId != null) {
			config.controler().setRunId(runId);
		}

		// configure scoring for plans
		config.planCalcScore().setLateArrival_utils_hr(0.0);
		config.planCalcScore().setPerforming_utils_hr(0.0);
		// this is unfortunately not working at all....
		ActivityParams homeParams = new ActivityParams("h");
		// homeParams.setOpeningTime(0);
		config.planCalcScore().addActivityParams(homeParams);
		// set it with f. strings
		config.planCalcScore().addParam("activityType_0", "h");
		config.planCalcScore().addParam("activityTypicalDuration_0",
				"24:00:00");
		config.planCalcScore().setBrainExpBeta(this.brainExpBeta);

		// configure controler
		config.travelTimeCalculator().setTraveltimeBinSize(ttBinSize);
		config.controler().setLastIteration(iterations);
		config.controler().setOutputDirectory(outputDirectory);
		Set set = new HashSet();
		set.add(EventsFileFormat.xml);
		config.controler().setEventsFileFormats(set);

		// configure simulation and snapshot writing
		if (this.doOtfOutput){
			config.controler().setSnapshotFormat(Arrays.asList("otfvis"));
			config.qsim().setSnapshotPeriod(10.0);
//			config.getQSimConfigGroup().setSnapshotStyle( SnapshotStyle.queue ) ;;
		}

		//    config.getQSimConfigGroup().setSnapshotFormat(null);
		// configure strategies for replanning
		config.strategy().setMaxAgentPlanMemorySize(4);

		StrategyConfigGroup.StrategySettings selectExp = new StrategyConfigGroup.StrategySettings(
				Id.create(1, StrategySettings.class));
		selectExp.setStrategyName("ChangeExpBeta");
//		selectExp.setModuleName("BestScore");
		config.strategy().addStrategySettings(selectExp);
		if (isUsePlansOnly) {
		  selectExp.setWeight(1.0);
		}
		else {
		  selectExp.setWeight(0.9);
		  StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings(
		      Id.create(2, StrategySettings.class));
		  reRoute.setWeight(0.1);
		  reRoute.setStrategyName("ReRoute");
		  reRoute.setDisableAfter(iterations);
		  config.strategy().addStrategySettings(reRoute);
		}

		ConfigGroup module = new ConfigGroup(CONFIG_MODULE);
		if (pSignal != null){
			module.addParam(PSIGNAL_CONFIG_PARAMETER, Double.toString(pSignal));
		}
		if (splitSgLink4 != null){
			module.addParam(SPLITSG1LINK4_CONFIG_PARAMETER, Double.toString(splitSgLink4));
		}
		if (this.initialRedNormalRoute != null) {
			module.addParam(INTIAL_RED_NORMAL_ROUTE_CONFIG_PARAMETER, Double.toString(this.initialRedNormalRoute));
		}
		config.addModule(module);
	}


	private Lanes createLanes(MutableScenario scenario) {
		LaneDefinitions11 lanes = new LaneDefinitions11Impl();
		LaneDefinitionsFactory11 factory = lanes.getFactory();
		//lanes for link 4
		LanesToLinkAssignment11 lanesForLink4 = factory.createLanesToLinkAssignment(Id.create(4, Link.class));
		LaneData11 link4lane1 = factory.createLane(Id.create(1, Lane.class));
		link4lane1.addToLinkId(Id.create(6, Link.class));
		link4lane1.setNumberOfRepresentedLanes(numberOfLanes);
		link4lane1.setStartsAtMeterFromLinkEnd(100.0);
		lanesForLink4.addLane(link4lane1);
		lanes.addLanesToLinkAssignment(lanesForLink4);
		//lanes for link 5
		LanesToLinkAssignment11 lanesForLink5 = factory.createLanesToLinkAssignment(Id.create(5, Link.class));
		LaneData11 link5lane1 = factory.createLane(Id.create(1, Lane.class));
		link5lane1.setNumberOfRepresentedLanes(numberOfLanes);
		link5lane1.addToLinkId(Id.create(6, Link.class));
		link5lane1.setStartsAtMeterFromLinkEnd(7.5);
		lanesForLink5.addLane(link5lane1);
		lanes.addLanesToLinkAssignment(lanesForLink5);
		Lanes lanesv2 = LaneDefinitionsV11ToV20Conversion.convertTo20(lanes, scenario.getNetwork());
		return lanesv2;
	}


//	private SignalSystems createSignalSystems(ScenarioImpl scenario) {
//		SignalSystems systems = scenario.getSignalSystems();
//		SignalSystemsFactory factory = systems.getFactory();
//		//create the signal system no 1
//		SignalSystemDefinition definition = factory.createSignalSystemDefinition(id1);
//		systems.addSignalSystemDefinition(definition);
//
//		//create signal group for traffic on link 4 on lane 1 with toLink 6
//		SignalGroupDefinition groupLink4 = factory.createSignalGroupDefinition(id4, id1);
//		groupLink4.addLaneId(id1);
//		groupLink4.addToLinkId(id6);
//		//assing the group to the system
//		groupLink4.setSignalSystemDefinitionId(id1);
//		//add the signalGroupDefinition to the container
//		systems.addSignalGroupDefinition(groupLink4);
//
//		//create signal group  with id no 2 for traffic on link 5 on lane 1 with toLink 6
//		SignalGroupDefinition groupLink5 = factory.createSignalGroupDefinition(id5, id2);
//		groupLink5.addLaneId(id1);
//		groupLink5.addToLinkId(id6);
//		//assing the group to the system
//		groupLink5.setSignalSystemDefinitionId(id1);
//
//		//add the signalGroupDefinition to the container
//		systems.addSignalGroupDefinition(groupLink5);
//
//		return systems;
//	}
//
//	public SignalSystemConfigurations createSignalSystemsConfig(
//			ScenarioImpl scenario) {
//		SignalSystemConfigurations configs = scenario.getSignalSystemConfigurations();
//		SignalSystemConfigurationsFactory factory = configs.getFactory();
//
//		SignalSystemConfiguration systemConfig = factory.createSignalSystemConfiguration(id1);
//		AdaptiveSignalSystemControlInfo controlInfo = factory.createAdaptiveSignalSystemControlInfo();
//		controlInfo.addSignalGroupId(id1);
//		controlInfo.addSignalGroupId(id2);
//		controlInfo.setAdaptiveControlerClass(controllerClass);
//		systemConfig.setSignalSystemControlInfo(controlInfo);
//
//		configs.addSignalSystemConfiguration(systemConfig);
//
//		return configs;
//	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			new DaganzoScenarioGenerator().createScenario();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


  public String getConfigOut() {
    return configOut;
  }

}
