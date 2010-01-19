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
package playground.droeder.gershensonSignals;


import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.MatsimLaneDefinitionsWriter;
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

import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */

public class GershensonScenarioGenerator {
	public static final String INPUT = DaPaths.INPUT;
	public static final String OUTPUT =DaPaths.OUTPUT;
	public static final String NETWORKFILE = INPUT + "gershensonTestNetwork.xml";
	public static final String CONFIGOUTPUTFILE = OUTPUT + "gershensonConfigFile.xml";
	public static final String LANESOUTPUTFILE = OUTPUT + "gershensonLanes.xml";
	public static final String SIGNALSYSTEMSOUTPUTFILE = OUTPUT + "gershensonSignalSystems.xml";
	public static final String SIGNALSYSTEMCONFIGURATIONSOUTPUTFILE = OUTPUT + "gershensonSignalSystemsConfig.xml";
	public static final String POPULATIONOUTPUTFILE = OUTPUT + "gershensonPopulation.xml";
	private static final String PLANSOUTPUTFILE = OUTPUT + "gershensonPlans.xml";
	private static final String OUTPUTDIRECTORY = OUTPUT + "route//";;
	
	private static final boolean isUseLanes = true;
	private static final boolean isUseSignalSystems = true;
	
	private static final String controllerClass = GershensonAdaptiveTrafficLightController.class.getCanonicalName();
	private static final int iterations = 1000;


	
	private Id id1, id2, id3, id11, id12, id13, id14;
	
	private void createIds(ScenarioImpl sc){
		id1 = sc.createId("1");
		id2 = sc.createId("2");
		id3 = sc.createId("3");
		id11 = sc.createId("11");
		id12 = sc.createId("12");
		id13 = sc.createId("13");
		id14 = sc.createId("14");

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
		createIds(scenario);
		
		//create the plans and write them
		createPlans(scenario);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFile(POPULATIONOUTPUTFILE);
		
		if (isUseLanes) {
			config.scenario().setUseLanes(true);
			config.network().setLaneDefinitionsFile(LANESOUTPUTFILE);
			//create the lanes and write them
			LaneDefinitions lanes = createLanes(scenario);
			MatsimLaneDefinitionsWriter laneWriter = new MatsimLaneDefinitionsWriter(lanes);
			laneWriter.writeFile(LANESOUTPUTFILE);
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
		
		//create and write the config 
		createConfig(config);
		new ConfigWriter(config).writeFile(CONFIGOUTPUTFILE);
		
		Log.info("Scenario Written!");
		

	}


	private LaneDefinitions createLanes(ScenarioImpl scenario) {
		LaneDefinitions lanes = scenario.getLaneDefinitions();
		LaneDefinitionsFactory factory = lanes.getFactory();
		

		//lanes for link 11
		LanesToLinkAssignment lanesForLink11 = factory.createLanesToLinkAssignment(id11);
//		Lane link11lane1 = factory.createLane(id1);
		Lane link11lane2 = factory.createLane(id2);
//		Lane link11lane3 = factory.createLane(id3);
		
//		link11lane1.addToLinkId(id13);
//		link11lane1.setNumberOfRepresentedLanes(1);
		link11lane2.addToLinkId(id12);
		link11lane2.setNumberOfRepresentedLanes(1);
//		link11lane3.addToLinkId(id14);
//		link11lane3.setNumberOfRepresentedLanes(1);
		
//		lanesForLink11.addLane(link11lane1);
		lanesForLink11.addLane(link11lane2);
//		lanesForLink11.addLane(link11lane3);
		lanes.addLanesToLinkAssignment(lanesForLink11);
		
		//lanes for link 13
		LanesToLinkAssignment lanesForLink13 = factory.createLanesToLinkAssignment(id13);
//		Lane link13lane1 = factory.createLane(id1);
		Lane link13lane2 = factory.createLane(id2);
//		Lane link13lane3 = factory.createLane(id3);
		
//		link13lane1.addToLinkId(id12);
//		link13lane1.setNumberOfRepresentedLanes(1);
		link13lane2.addToLinkId(id14);
		link13lane2.setNumberOfRepresentedLanes(1);
//		link13lane3.addToLinkId(id11);
//		link13lane3.setNumberOfRepresentedLanes(1);
		
//		lanesForLink13.addLane(link13lane1);
		lanesForLink13.addLane(link13lane2);
//		lanesForLink13.addLane(link13lane3);
		lanes.addLanesToLinkAssignment(lanesForLink13);
		return lanes;
	}



	private SignalSystemConfigurations createSignalSystemsConfig(ScenarioImpl scenario) {
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

	private SignalSystems createSignalSystems(ScenarioImpl scenario) {
		SignalSystems systems = scenario.getSignalSystems();
		SignalSystemsFactory factory = systems.getFactory();
		
		//create the signal system no 1
		SignalSystemDefinition definition = factory.createSignalSystemDefinition(id1);
		systems.addSignalSystemDefinition(definition);
		
		//create signal group for traffic on link 11 
		SignalGroupDefinition groupLink11 = factory.createSignalGroupDefinition(id11, id1);
//		groupLink11.addLaneId(id1);
		groupLink11.addLaneId(id2);
//		groupLink11.addLaneId(id3);
		groupLink11.addToLinkId(id12);
//		groupLink11.addToLinkId(id13);
//		groupLink11.addToLinkId(id14);
		
		//create signal group for traffic on link 13
		SignalGroupDefinition groupLink13 = factory.createSignalGroupDefinition(id13, id2);
//		groupLink13.addLaneId(id1);
		groupLink13.addLaneId(id2);
//		groupLink13.addLaneId(id3);
//		groupLink13.addToLinkId(id11);
//		groupLink13.addToLinkId(id12);
		groupLink13.addToLinkId(id14);
		
	
		//adding groups to the system
		groupLink11.setSignalSystemDefinitionId(id1);
		groupLink13.setSignalSystemDefinitionId(id2);
		
		//adding signalGroupDefinitions to the container
		systems.addSignalGroupDefinition(groupLink11);
		systems.addSignalGroupDefinition(groupLink13);

	
		
		return systems;
	}

	private void createPlans(ScenarioImpl scenario) {
		Network network = scenario.getNetwork();
		PopulationImpl population = scenario.getPopulation();
		int firstHomeEndTime =  1 * 3600;
		int homeEndTime = firstHomeEndTime;
		
		Link l11 = network.getLinks().get(scenario.createId("11"));
		Link l12 = network.getLinks().get(scenario.createId("12"));
		Link l13 = network.getLinks().get(scenario.createId("13"));		
		Link l14 = network.getLinks().get(scenario.createId("14"));
		PopulationFactory factory = population.getFactory();

		for (int i = 1; i <= 500; i++) {
			PersonImpl p1 = (PersonImpl) factory.createPerson(scenario.createId(Integer.toString(i)));
			Plan plan1 = factory.createPlan();
			p1.addPlan(plan1);
			homeEndTime+= 1;

			LinkNetworkRouteImpl route1 = null;
			ActivityImpl act11 = (ActivityImpl) factory.createActivityFromLinkId("h", l11.getId());
			act11.setEndTime(homeEndTime);
			plan1.addActivity(act11);
			// leg to home
			LegImpl leg1 = (LegImpl) factory.createLeg(TransportMode.car);
			route1 = new LinkNetworkRouteImpl(l11, l12);
			
			leg1.setRoute(route1);
			plan1.addLeg(leg1);
				
			ActivityImpl act12 = (ActivityImpl) factory.createActivityFromLinkId("h", l12.getId());
			act12.setLink(l12);
			plan1.addActivity(act12);	
			
			population.addPerson(p1);
			
			PersonImpl p2 = (PersonImpl) factory.createPerson(scenario.createId(Integer.toString(i+500)));
			Plan plan = factory.createPlan();
			p2.addPlan(plan);
			homeEndTime+= 1;

			LinkNetworkRouteImpl route = null;
			ActivityImpl act21 = (ActivityImpl) factory.createActivityFromLinkId("h", l13.getId());
			act21.setEndTime(homeEndTime);
			plan.addActivity(act21);
			// leg to home
			LegImpl leg2 = (LegImpl) factory.createLeg(TransportMode.car);
			route = new LinkNetworkRouteImpl(l13, l14);
			
			leg2.setRoute(route);
			plan.addLeg(leg2);
				
			ActivityImpl act22 = (ActivityImpl) factory.createActivityFromLinkId("h", l14.getId());
			act22.setLink(l14);
			plan.addActivity(act22);	
			
			population.addPerson(p2);
					
			
			
		}
		

	}

	private void createConfig(Config config) {
		
		config.network().setInputFile(NETWORKFILE);
		config.plans().setInputFile(POPULATIONOUTPUTFILE);

		// configure scoring for plans
		config.charyparNagelScoring().setLateArrival(0.0);
		config.charyparNagelScoring().setPerforming(6.0);

		// set it with f. strings
		config.charyparNagelScoring().addParam("activityType_0", "h");
		config.charyparNagelScoring().addParam("activityTypicalDuration_0",
				"24:00:00");

		// configure controler
		config.travelTimeCalculator().setTraveltimeBinSize(1);
		config.controler().setLastIteration(iterations);
		config.controler().setOutputDirectory(OUTPUTDIRECTORY);


		// configure simulation and snapshot writing
		config.setQSimConfigGroup(new QSimConfigGroup());
		config.getQSimConfigGroup().setSnapshotFormat("otfvis");
		config.getQSimConfigGroup().setSnapshotFile("cmcf.mvi");
		config.getQSimConfigGroup().setSnapshotPeriod(60.0);
		config.getQSimConfigGroup().setSnapshotStyle("queue");
		// configure strategies for replanning
		
		config.strategy().setMaxAgentPlanMemorySize(4);
		StrategyConfigGroup.StrategySettings selectExp = new StrategyConfigGroup.StrategySettings(id1);
		selectExp.setProbability(0.9);
		selectExp.setModuleName("ChangeExpBeta");
		config.strategy().addStrategySettings(selectExp);

		StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings(id2);
		reRoute.setProbability(0.1);
		reRoute.setModuleName("ReRoute");
		reRoute.setDisableAfter(iterations);
		config.strategy().addStrategySettings(reRoute);
	}

	
	
	public static void main(final String[] args) {
		try {
			new GershensonScenarioGenerator().createScenario();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
