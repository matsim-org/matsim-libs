/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.agarwalamit.exposure;

import java.util.HashSet;
import java.util.Set;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author julia, benjamin, amit
 */

public class EquilTestSetUp {

	public Scenario createConfigAndReturnScenario(){
		Config config = ConfigUtils.createConfig();

		config.strategy().setMaxAgentPlanMemorySize(2);

		ControlerConfigGroup ccg = config.controler();
		ccg.setWriteEventsInterval(2);
		ccg.setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		ccg.setCreateGraphs(false);		
		ccg.setFirstIteration(0);
		ccg.setLastIteration(0);
		ccg.setWriteEventsInterval(1);
		ccg.setMobsim("qsim");

		Set<EventsFileFormat> set = new HashSet<EventsFileFormat>();
		set.add(EventsFileFormat.xml);
		ccg.setEventsFileFormats(set);

		QSimConfigGroup qcg = config.qsim();		
		qcg.setStartTime(0 * 3600.);
		qcg.setEndTime(24 * 3600.);
		qcg.setFlowCapFactor(0.1);
		qcg.setStorageCapFactor(0.3);

		PlanCalcScoreConfigGroup pcs = config.planCalcScore();

		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration( 6*3600 );
		pcs.addActivityParams(home);

		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration( 9*3600 );
		pcs.addActivityParams(work);

		pcs.setMarginalUtilityOfMoney(0.0789942);
		pcs.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(0.0);
		pcs.getModes().get(TransportMode.car).setMonetaryDistanceRate(new Double("-3.0E-4"));
		pcs.setLateArrival_utils_hr(0.0);
		pcs.setPerforming_utils_hr(0.96);
		
		StrategyConfigGroup scg  = config.strategy();

		StrategySettings strategySettings = new StrategySettings();
		strategySettings.setStrategyName("ChangeExpBeta");
		strategySettings.setWeight(1);
		scg.addStrategySettings(strategySettings);

		// TODO: the following does not work yet. Need to force controler to always write events in the last iteration.
		VspExperimentalConfigGroup vcg = config.vspExperimental() ;
		vcg.setWritingOutputEvents(false) ;

		return ScenarioUtils.loadScenario(config);
	}

	/**
	 * Exposure to these agents will result in toll for active agent.
	 */
	public void createPassiveAgents(Scenario scenario) {
		PopulationFactory pFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		// passive agents' home coordinates are around node 9 (12500, 7500)
		for(Integer i=0; i<5; i++){ // x
			for(Integer j=0; j<4; j++){

				String idpart = i.toString()+j.toString();

				Person person = pFactory.createPerson(Id.create("passive_"+idpart, Person.class)); 

				double xCoord = 6563. + (i+1)*625;
				double yCoord = 7188. + (j-1)*625;
				Plan plan = pFactory.createPlan(); 

				Coord coord = new Coord(xCoord, yCoord);
				Activity home = pFactory.createActivityFromCoord("home", coord );
				home.setEndTime(1.0);
				Leg leg = pFactory.createLeg(TransportMode.walk);
				Coord coord2 = new Coord(xCoord, yCoord + 1.);
				Activity home2 = pFactory.createActivityFromCoord("home", coord2);

				plan.addActivity(home);
				plan.addLeg(leg);
				plan.addActivity(home2);
				person.addPlan(plan);
				scenario.getPopulation().addPerson(person);
			}
		}
	}

	/**
	 * This agent is traveling and thus will be charged for exposure/emission toll.
	 */
	public void createActiveAgents(Scenario scenario) {
		PopulationFactory pFactory = scenario.getPopulation().getFactory();
		Person person = pFactory.createPerson(Id.create("567417.1#12424", Person.class));
		Plan plan = pFactory.createPlan();

		Coord homeCoords = new Coord(1.0, 10000.0);
		Activity home = pFactory.createActivityFromCoord("home", homeCoords);
		//Activity home = pFactory.createActivityFromLinkId("home", scenario.createId("12"));
		home.setEndTime(6 * 3600);
		plan.addActivity(home);

		Leg leg1 = pFactory.createLeg(TransportMode.car);
		plan.addLeg(leg1);

		Coord workCoords = new Coord(19999.0, 10000.0);
		Activity work = pFactory.createActivityFromCoord("work" , workCoords);
		//		Activity work = pFactory.createActivityFromLinkId("work", scenario.createId("45"));
		work.setEndTime(home.getEndTime() + 600 + 8 * 3600);
		plan.addActivity(work);

		Leg leg2 = pFactory.createLeg(TransportMode.car);
		plan.addLeg(leg2);

		home = pFactory.createActivityFromCoord("home", homeCoords);
		//		home = pFactory.createActivityFromLinkId("home", scenario.createId("12"));
		plan.addActivity(home);

		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}

	/**
	 * Simplified form of Equil network.
	 */
	public void createNetwork(Scenario scenario) {
		Network network = (Network) scenario.getNetwork();

		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(1.0, 10000.0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(2500.0, 10000.0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(4500.0, 10000.0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(17500.0, 10000.0));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord(19999.0, 10000.0));
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create("6", Node.class), new Coord(19999.0, 1500.0));
		Node node7 = NetworkUtils.createAndAddNode(network, Id.create("7", Node.class), new Coord(1.0, 1500.0));
		Node node8 = NetworkUtils.createAndAddNode(network, Id.create("8", Node.class), new Coord(12500.0, 12499.0));
		Node node9 = NetworkUtils.createAndAddNode(network, Id.create("9", Node.class), new Coord(12500.0, 7500.0));
		final Node fromNode = node1;
		final Node toNode = node2;


		//freeSpeed is 100km/h for roadType 22.
		NetworkUtils.createAndAddLink(network,Id.create("12", Link.class), fromNode, toNode, (double) 1000, 100.00/3.6, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("23", Link.class), fromNode1, toNode1, (double) 2000, 100.00/3.6, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode2 = node4;
		final Node toNode2 = node5;
		NetworkUtils.createAndAddLink(network,Id.create("45", Link.class), fromNode2, toNode2, (double) 2000, 100.00/3.6, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode3 = node5;
		final Node toNode3 = node6;
		NetworkUtils.createAndAddLink(network,Id.create("56", Link.class), fromNode3, toNode3, (double) 1000, 100.00/3.6, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode4 = node6;
		final Node toNode4 = node7;
		NetworkUtils.createAndAddLink(network,Id.create("67", Link.class), fromNode4, toNode4, (double) 1000, 100.00/3.6, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode5 = node7;
		final Node toNode5 = node1;
		NetworkUtils.createAndAddLink(network,Id.create("71", Link.class), fromNode5, toNode5, (double) 1000, 100.00/3.6, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode6 = node3;
		final Node toNode6 = node8;

		// two similar path from node 3 to node 4 - north: route via node 8, south: route via node 9
		NetworkUtils.createAndAddLink(network,Id.create("38", Link.class), fromNode6, toNode6, (double) 5000, 100.00/3.6, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode7 = node3;
		final Node toNode7 = node9;
		NetworkUtils.createAndAddLink(network,Id.create("39", Link.class), fromNode7, toNode7, (double) 5000, 100.00/3.6, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode8 = node8;
		final Node toNode8 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("84", Link.class), fromNode8, toNode8, (double) 5000, 100.00/3.6, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode9 = node9;
		final Node toNode9 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("94", Link.class), fromNode9, toNode9, (double) 4999, 100.00/3.6, (double) 3600, (double) 1, null, (String) "22");

		for(Integer i=0; i<5; i++){ // x
			for(Integer j=0; j<4; j++){
				String idpart = i.toString()+j.toString();

				double xCoord = 6563. + (i+1)*625;
				double yCoord = 7188. + (j-1)*625;

				// add a link for each person
				Node nodeA = NetworkUtils.createAndAddNode(network, Id.create("node_"+idpart+"A", Node.class), new Coord(xCoord, yCoord));
				Node nodeB = NetworkUtils.createAndAddNode(network, Id.create("node_"+idpart+"B", Node.class), new Coord(xCoord, yCoord + 1.));
				final Node fromNode10 = nodeA;
				final Node toNode10 = nodeB;
				NetworkUtils.createAndAddLink(network,Id.create("link_p"+idpart, Link.class), fromNode10, toNode10, (double) 10, 30.0, (double) 3600, (double) 1 );
			}
		}
	}
}


