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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsFactory;
import org.matsim.lanes.LaneDefinitionsWriter11;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsWriter;
import org.matsim.signalsystems.MatsimSignalSystemsWriter;
import org.matsim.signalsystems.config.AdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.config.SignalSystemConfiguration;
import org.matsim.signalsystems.config.SignalSystemConfigurations;
import org.matsim.signalsystems.config.SignalSystemConfigurationsFactory;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.signalsystems.systems.SignalSystemDefinition;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.signalsystems.systems.SignalSystemsFactory;

import playground.dgrether.DgPaths;
import playground.dgrether.DgPlaygroundJobfileCreator;
import playground.dgrether.utils.IdFactory;

/**
 * @author dgrether
 *
 */
public class DaganzoScenarioGenerator {

	private static final Logger log = Logger
			.getLogger(DaganzoScenarioGenerator.class);

	public static String DAGANZO_SVN_DIR = "shared-svn/studies/dgrether/daganzo2010/";

	public static String DAGANZOBASEDIR = DgPaths.SCMWORKSPACE + DAGANZO_SVN_DIR;

	public static final String DAGANZO_NETWORK = "daganzoNetwork2.xml";
	
	public static final String DAGANZONETWORKFILE = DAGANZOBASEDIR
			+DAGANZO_NETWORK;

	public static final String CONFIG_MODULE = "daganzoSignal";
	
	public static final String PSIGNAL_CONFIG_PARAMETER = "psignal";
	
	public static final String SPLITSG1LINK4_CONFIG_PARAMETER = "splitSg1Link4";
	
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
	
	public static final String DAGANZO_SIGNALS_CONFIG = "daganzoSignalSystemsConfigs.xml";

	public static final String SIGNALSYSTEMCONFIGURATIONSOUTPUTFILE = DAGANZOBASEDIR
		+ DAGANZO_SIGNALS_CONFIG;

	private static final String CLUSTERJOBFILES = DgPaths.CLUSTERBASE + "daganzoJobfiles/";
	
	
	public String configOut, plansOut, outputDirectory;

	public static boolean isAlternativeRouteEnabled = true;

	private static final boolean isUsePlansOnly = true;

	private static final boolean isUseLanes = true;

	private static final boolean isUseSignalSystems = true;

	public static int iterations = 30;
	
	private static final int agents = 5000;

	private static final int ttBinSize = 1;

	private final static Double pSignal = 0.0;//0.5;
	
	private final static Double splitSgLink4 = 0.80;
	
	private static final String controllerClass = AdaptiveController3.class.getCanonicalName();

	public static final String runId = "1168";//"";
	
	private static final boolean writerClusterFiles = false;
	
	private boolean doOtfOutput = true;
	
	private int numberOfLanes = 3;
	
	private Id id1, id2, id4, id5, id6, id7;

	private String networkInputFile;

	private String lanesInputFile;

	private String signalInputFile;

	private String signalConfigInputFile;

	private String plansInputFile;

	public DaganzoScenarioGenerator() {
		init();
	}

	private void init() {
		String baseString = "daganzo3_";
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
		baseString += "usePlansOnly-" + Boolean.toString(isUsePlansOnly) + "_";
		baseString += "noAgents-" + Integer.toString(agents) + "_";
		baseString += "iterations-" + Integer.toString(iterations) + "_";
		if (pSignal != null) {
			baseString += "pSignal-" + pSignal + "_";
		}
		if (splitSgLink4 != null) {
			baseString += "splitSgLink4-" + splitSgLink4 + "_";
		}
		baseString += "ttbinsize-" + ttBinSize;
		
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

	private void createIds(ScenarioImpl sc){
		id1 = sc.createId("1");
		id2 = sc.createId("2");
		id4 = sc.createId("4");
		id5 = sc.createId("5");
		id6 = sc.createId("6");
		id7 = sc.createId("7");
	}

	public void createScenario() {
		//create a scenario
		ScenarioImpl scenario = new ScenarioImpl();
		//get the config
		Config config = scenario.getConfig();
		//set the network input file to the config and load it
		config.network().setInputFile(NETWORKFILE);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadNetwork();
		//create some ids as members of the class for convenience reasons
		createIds(scenario);
		//create the plans and write them
		createPlans(scenario);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(plansOut);
		if (isUseLanes) {
			config.scenario().setUseLanes(true);
			config.network().setLaneDefinitionsFile(LANESOUTPUTFILE);
			//create the lanes and write them
			LaneDefinitions lanes = createLanes(scenario);
			LaneDefinitionsWriter11 laneWriter = new LaneDefinitionsWriter11(lanes);
			laneWriter.write(LANESOUTPUTFILE);
		}
		if (isUseSignalSystems) {
			//enable lanes and signal system feature in config
			config.scenario().setUseSignalSystems(true);
			config.signalSystems().setSignalSystemFile(SIGNALSYSTEMSOUTPUTFILE);
			config.signalSystems().setSignalSystemConfigFile(SIGNALSYSTEMCONFIGURATIONSOUTPUTFILE);
			//create the signal systems and write them
			SignalSystems signalSystems = createSignalSystems(scenario);
			MatsimSignalSystemsWriter ssWriter = new MatsimSignalSystemsWriter(signalSystems);
			ssWriter.writeFile(SIGNALSYSTEMSOUTPUTFILE);
			//create the signal system's configurations and write them
			SignalSystemConfigurations ssConfigs = createSignalSystemsConfig(scenario);
			MatsimSignalSystemConfigurationsWriter ssConfigsWriter = new MatsimSignalSystemConfigurationsWriter(ssConfigs);
			ssConfigsWriter.writeFile(SIGNALSYSTEMCONFIGURATIONSOUTPUTFILE);
		}

		//create and write the config with the correct paths to the files created above
		createConfig(config);
		new ConfigWriter(config).write(configOut);
		
		DgPlaygroundJobfileCreator.createJobfile(DAGANZOBASEDIR + runId + "jobfile", 
				DgPaths.CLUSTERSVN + DAGANZO_SVN_DIR + runId + "_config.xml", runId);

		log.info("scenario written!");
	}


	private void createPlans(ScenarioImpl scenario) {
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();
		double firstHomeEndTime =  600.0;
		double homeEndTime = firstHomeEndTime;
//		Link l1 = network.getLinks().get(scenario.createId("1"));
//		Link l7 = network.getLinks().get(scenario.createId("7"));
		PopulationFactory factory = population.getFactory();

		for (int i = 1; i <= this.agents; i++) {
			PersonImpl p = (PersonImpl) factory.createPerson(scenario.createId(Integer
					.toString(i)));
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
			  p.addPlan(plan);
        plan2 = this.createPlan(true, factory, homeEndTime, network);
        p.addPlan(plan2);
//        plan.setScore(-0.2733333333333334);
//        plan2.setScore(-0.2733333333333334);
        if (isAlternativeRouteEnabled){
          plan.setSelected(false);
          plan2.setSelected(true);
          p.setSelectedPlan(plan2);
        }
        else {
          plan.setSelected(true);
          p.setSelectedPlan(plan);
          plan2.setSelected(false);
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
    ActivityImpl act1 = (ActivityImpl) factory.createActivityFromLinkId("h", id1);
    act1.setEndTime(homeEndTime);
    plan.addActivity(act1);
    // leg to home
    LegImpl leg = (LegImpl) factory.createLeg(TransportMode.car);
    LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(id1, id7);
    if (useAlternativeRoute) {
      route.setLinkIds(id1, NetworkUtils.getLinkIds("2 3 5 6"), id7);
    }
    else {
      route.setLinkIds(id1, NetworkUtils.getLinkIds("2 4 6"), id7);
    }
    leg.setRoute(route);

    plan.addLeg(leg);

    ActivityImpl act2 = (ActivityImpl) factory.createActivityFromLinkId("h", id7);
    act2.setLinkId(id7);
    plan.addActivity(act2);
    return plan;
	}

	private void createConfig(Config config) {
	// set scenario
		config.network().setInputFile(networkInputFile);
		config.plans().setInputFile(plansInputFile);
		if (isUseLanes){
			config.network().setLaneDefinitionsFile(lanesInputFile);
		}
		if (isUseSignalSystems){
			config.signalSystems().setSignalSystemFile(signalInputFile);
			config.signalSystems().setSignalSystemConfigFile(this.signalConfigInputFile);
		}
		
		if (runId != null) {
			config.controler().setRunId(runId);
		}

		// configure scoring for plans
		config.charyparNagelScoring().setLateArrival(0.0);
		config.charyparNagelScoring().setPerforming(0.0);
		// this is unfortunately not working at all....
		ActivityParams homeParams = new ActivityParams("h");
		// homeParams.setOpeningTime(0);
		config.charyparNagelScoring().addActivityParams(homeParams);
		// set it with f. strings
		config.charyparNagelScoring().addParam("activityType_0", "h");
		config.charyparNagelScoring().addParam("activityTypicalDuration_0",
				"24:00:00");

		// configure controler
		config.travelTimeCalculator().setTraveltimeBinSize(ttBinSize);
		config.controler().setLastIteration(iterations);
		config.controler().setOutputDirectory(outputDirectory);


		// configure simulation and snapshot writing
		config.setQSimConfigGroup(new QSimConfigGroup());
		if (this.doOtfOutput){
			config.getQSimConfigGroup().setSnapshotFormat("otfvis");
			config.getQSimConfigGroup().setSnapshotPeriod(10.0);
			config.getQSimConfigGroup().setSnapshotStyle("queue");
		}

		//    config.getQSimConfigGroup().setSnapshotFormat(null);
		// configure strategies for replanning
		config.strategy().setMaxAgentPlanMemorySize(4);

		StrategyConfigGroup.StrategySettings selectExp = new StrategyConfigGroup.StrategySettings(
				IdFactory.get(1));
		selectExp.setModuleName("ChangeExpBeta");
//		selectExp.setModuleName("BestScore");
		config.strategy().addStrategySettings(selectExp);
		if (isUsePlansOnly) {
		  selectExp.setProbability(1.0);
		}
		else {
		  selectExp.setProbability(0.9);
		  StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings(
		      IdFactory.get(2));
		  reRoute.setProbability(0.1);
		  reRoute.setModuleName("ReRoute");
		  reRoute.setDisableAfter(iterations);
		  config.strategy().addStrategySettings(reRoute);
		}
		
		Module module = new Module(CONFIG_MODULE);
		if (pSignal != null){
			module.addParam(PSIGNAL_CONFIG_PARAMETER, Double.toString(pSignal));
		}
		if (splitSgLink4 != null){
			module.addParam(SPLITSG1LINK4_CONFIG_PARAMETER, Double.toString(splitSgLink4));
		}
		config.addModule(CONFIG_MODULE, module);
	}


	private LaneDefinitions createLanes(ScenarioImpl scenario) {
		LaneDefinitions lanes = scenario.getLaneDefinitions();
		LaneDefinitionsFactory factory = lanes.getFactory();
		//lanes for link 4
		LanesToLinkAssignment lanesForLink4 = factory.createLanesToLinkAssignment(id4);
		Lane link4lane1 = factory.createLane(id1);
		link4lane1.addToLinkId(id6);
		link4lane1.setNumberOfRepresentedLanes(numberOfLanes);
		link4lane1.setStartsAtMeterFromLinkEnd(100.0);
		lanesForLink4.addLane(link4lane1);
		lanes.addLanesToLinkAssignment(lanesForLink4);
		//lanes for link 5
		LanesToLinkAssignment lanesForLink5 = factory.createLanesToLinkAssignment(id5);
		Lane link5lane1 = factory.createLane(id1);
		link5lane1.setNumberOfRepresentedLanes(numberOfLanes);
		link5lane1.addToLinkId(id6);
		link5lane1.setStartsAtMeterFromLinkEnd(15.0);
		lanesForLink5.addLane(link5lane1);
		lanes.addLanesToLinkAssignment(lanesForLink5);
		return lanes;
	}


	private SignalSystems createSignalSystems(ScenarioImpl scenario) {
		SignalSystems systems = scenario.getSignalSystems();
		SignalSystemsFactory factory = systems.getFactory();
		//create the signal system no 1
		SignalSystemDefinition definition = factory.createSignalSystemDefinition(id1);
		systems.addSignalSystemDefinition(definition);

		//create signal group for traffic on link 4 on lane 1 with toLink 6
		SignalGroupDefinition groupLink4 = factory.createSignalGroupDefinition(id4, id1);
		groupLink4.addLaneId(id1);
		groupLink4.addToLinkId(id6);
		//assing the group to the system
		groupLink4.setSignalSystemDefinitionId(id1);
		//add the signalGroupDefinition to the container
		systems.addSignalGroupDefinition(groupLink4);

		//create signal group  with id no 2 for traffic on link 5 on lane 1 with toLink 6
		SignalGroupDefinition groupLink5 = factory.createSignalGroupDefinition(id5, id2);
		groupLink5.addLaneId(id1);
		groupLink5.addToLinkId(id6);
		//assing the group to the system
		groupLink5.setSignalSystemDefinitionId(id1);

		//add the signalGroupDefinition to the container
		systems.addSignalGroupDefinition(groupLink5);

		return systems;
	}

	private SignalSystemConfigurations createSignalSystemsConfig(
			ScenarioImpl scenario) {
		SignalSystemConfigurations configs = scenario.getSignalSystemConfigurations();
		SignalSystemConfigurationsFactory factory = configs.getFactory();

		SignalSystemConfiguration systemConfig = factory.createSignalSystemConfiguration(id1);
		AdaptiveSignalSystemControlInfo controlInfo = factory.createAdaptiveSignalSystemControlInfo();
		controlInfo.addSignalGroupId(id1);
		controlInfo.addSignalGroupId(id2);
		controlInfo.setAdaptiveControlerClass(controllerClass);
		systemConfig.setSignalSystemControlInfo(controlInfo);

		configs.getSignalSystemConfigurations().put(systemConfig.getSignalSystemId(), systemConfig);

		return configs;
	}

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
