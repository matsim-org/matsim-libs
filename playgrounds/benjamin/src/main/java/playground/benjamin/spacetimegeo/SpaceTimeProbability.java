/* *********************************************************************** *
 * project: org.matsim.*
 * SpaceTimeProbability.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.spacetimegeo;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoadPricingConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricing;

/**
 * @author benjamin
 *
 */
public class SpaceTimeProbability {
	private static Logger logger = Logger.getLogger(SpaceTimeProbability.class);
	
	Config config;
	Scenario scenario;
	Controler controler;
	
	Integer lastIteration = 10;
	String workingDirectory = "../../runs-svn/spacetimeGeo/";
	String tollLinksFile = workingDirectory + "input/tollLinksFile.xml";

	private void run() {
		this.config = ConfigUtils.createConfig();
		this.config.addCoreModules();
		this.scenario = ScenarioUtils.createScenario(this.config);
		
		createNetwork();
		createAgent();
		
		this.controler = new Controler(this.scenario);
		specifyControler();
		
		final LinkLeaveCountHandler handler = new LinkLeaveCountHandler(this.controler);
		StartupListener startupListener = new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getControler().getEvents().addHandler(handler);
			}
		};
		this.controler.addControlerListener(startupListener);
		this.controler.addControlerListener(new RoadPricing()); 
		this.controler.run();
		
		System.out.println("");
		logger.info("The agent in average payed " + handler.getTollPaid() / handler.getLink3Counter() + " EUR when using link 3");
		System.out.println("");
		logger.info("Link 3 was used in " + handler.getLink3Counter() + " cases of " + (this.controler.getLastIteration() + 1) + " iterations");
		logger.info("Link 9 was used in " + handler.getLink9Counter() + " cases of " + (this.controler.getLastIteration() + 1) + " iterations");
		logger.info("Link 11 was used in " + handler.getLink11Counter() + " cases of " + (this.controler.getLastIteration() + 1) + " iterations");
		System.out.println("\n");
	}
	
	private void specifyControler() {
		// controler settings	
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);

		// controlerConfigGroup
		ControlerConfigGroup ccg = controler.getConfig().controler();
		ccg.setFirstIteration(0);
		ccg.setLastIteration(this.lastIteration);
		ccg.setOutputDirectory(workingDirectory + "output/");
		ccg.setMobsim(MobsimType.qsim.toString());
		Set set = new HashSet();
		set.add(EventsFileFormat.xml);
		ccg.setEventsFileFormats(set);
		ccg.setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
		ccg.setWriteEventsInterval(this.lastIteration);
		ccg.setWritePlansInterval(this.lastIteration);
		ccg.setWriteSnapshotsInterval(0);

		// qsimConfigGroup
		QSimConfigGroup qcg = new QSimConfigGroup();
		qcg.setStartTime(0 * 3600.);
		qcg.setEndTime(24 * 3600.);
		qcg.setFlowCapFactor(1);
		qcg.setStorageCapFactor(1);
		qcg.setNumberOfThreads(1);
		qcg.setRemoveStuckVehicles(false);
		controler.getConfig().addQSimConfigGroup(qcg);

		// planCalcScoreConfigGroup
		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();

		ActivityParams act1Params = new ActivityParams("home");
		act1Params.setTypicalDuration(16 * 3600);
		pcs.addActivityParams(act1Params);

		ActivityParams act2Params = new ActivityParams("work");
		act2Params.setLatestStartTime(7 * 3600);
		act2Params.setTypicalDuration(8 * 3600);
		pcs.addActivityParams(act2Params);

		pcs.setBrainExpBeta(1.0);
//		pcs.setPerforming_utils_hr(0.0);
		pcs.setPerforming_utils_hr(6.0);
//		pcs.setTraveling_utils_hr(-0.0);
		pcs.setTraveling_utils_hr(-6.0);
		pcs.setMarginalUtilityOfMoney(1.2);
		pcs.setMonetaryDistanceCostRateCar(-0.0);
//		pcs.setMonetaryDistanceCostRateCar(-0.0001);
		pcs.setLateArrival_utils_hr(-0.0);
//		pcs.setLateArrival_utils_hr(-60.0);

		// strategyConfigGroup
		StrategyConfigGroup scg = controler.getConfig().strategy();

		StrategySettings changePlan = new StrategySettings(new IdImpl("1"));
//		changePlan.setModuleName("SelectRandom");
		changePlan.setModuleName("ChangeExpBeta");
		changePlan.setProbability(1.0);

//		StrategySettings reRoute = new StrategySettings(new IdImpl("2"));
//		reRoute.setModuleName("ReRoute");
//		reRoute.setProbability(0.99);
//		reRoute.setDisableAfter(2);
//		
//		scg.setMaxAgentPlanMemorySize(2);

		scg.addStrategySettings(changePlan);
//		scg.addStrategySettings(reRoute);
		
		// scenarioConfigGroup
		ScenarioConfigGroup sccg = controler.getConfig().scenario();
		sccg.setUseRoadpricing(true);

		// roadPricingConfigGroup
		RoadPricingConfigGroup rpc = controler.getConfig().roadpricing();
		rpc.setTollLinksFile(this.tollLinksFile);
		
	}

	private void createAgent() {
		PopulationFactoryImpl pFactory = (PopulationFactoryImpl) this.scenario.getPopulation().getFactory();
		Person person = pFactory.createPerson(this.scenario.createId("worker"));
		
		Id homeLinkId = this.scenario.createId("1");
		Id workLinkId = this.scenario.createId("4");
		
		for(int i=0; i<3; i++){
			Plan plan = pFactory.createPlan();
			
			Activity home = pFactory.createActivityFromLinkId("home", homeLinkId);
			home.setEndTime(6 * 3600);
			plan.addActivity(home);
			
			Leg leg1 = pFactory.createLeg(TransportMode.car);
			
			List<Id> routeLinkIds = new LinkedList<Id>();
			if(i == 0){
				Id id2 = this.scenario.createId("2");
				Id id3 = this.scenario.createId("3");
				routeLinkIds.add(id2);
				routeLinkIds.add(id3);
			} else if(i == 1){
				Id id2 = this.scenario.createId("2");
				Id id8 = this.scenario.createId("8");
				Id id9 = this.scenario.createId("9");
				routeLinkIds.add(id2);
				routeLinkIds.add(id8);
				routeLinkIds.add(id9);
			} else if(i == 2){
				Id id2 = this.scenario.createId("2");
				Id id10 = this.scenario.createId("10");
				Id id11 = this.scenario.createId("11");
				routeLinkIds.add(id2);
				routeLinkIds.add(id10);
				routeLinkIds.add(id11);
			} else {
				throw new RuntimeException();
			}
			
			Route route1 = pFactory.createRoute(leg1.getMode(), null, null);
			((NetworkRoute) route1).setLinkIds(homeLinkId, routeLinkIds, workLinkId);
			leg1.setRoute(route1);
			logger.info(((NetworkRoute) leg1.getRoute()).getLinkIds());
			plan.addLeg(leg1);
			
			Activity work = pFactory.createActivityFromLinkId("work", workLinkId);
			work.setEndTime(home.getEndTime() + 1800 + 8 * 3600);
			plan.addActivity(work);
			
			Leg leg2 = pFactory.createLeg(TransportMode.car);
			
			List<Id> routeLinkIds2 = new LinkedList<Id>();
			Id id5 = this.scenario.createId("5");
			Id id6 = this.scenario.createId("6");
			Id id7 = this.scenario.createId("7");
			routeLinkIds2.add(id5);
			routeLinkIds2.add(id6);
			routeLinkIds2.add(id7);

			Route route2 = pFactory.createRoute(leg2.getMode(), null, null);
			((NetworkRoute) route2).setLinkIds(workLinkId, routeLinkIds2, homeLinkId);
			leg2.setRoute(route2);
			logger.info(((NetworkRoute) leg2.getRoute()).getLinkIds());
			plan.addLeg(leg2);
			
			plan.addActivity(home);
			plan.setScore(1000.0);
			person.addPlan(plan);
		}
		this.scenario.getPopulation().addPerson(person);
	}

	private void createNetwork() {
		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();

		Node node1 = network.createAndAddNode(this.scenario.createId("1"), this.scenario.createCoord(-26000.0,     0.0));
		Node node2 = network.createAndAddNode(this.scenario.createId("2"), this.scenario.createCoord(-25000.0,     0.0));
		Node node3 = network.createAndAddNode(this.scenario.createId("3"), this.scenario.createCoord(-21000.0,     0.0));
		Node node4 = network.createAndAddNode(this.scenario.createId("4"), this.scenario.createCoord( -1000.0,     0.0));
		Node node5 = network.createAndAddNode(this.scenario.createId("5"), this.scenario.createCoord(     0.0,     0.0));
		Node node6 = network.createAndAddNode(this.scenario.createId("6"), this.scenario.createCoord( -6500.0, -5000.0));
		Node node7 = network.createAndAddNode(this.scenario.createId("7"), this.scenario.createCoord(-19500.0, -5000.0));
		Node node8 = network.createAndAddNode(this.scenario.createId("8"), this.scenario.createCoord(-11000.0,  2500.0));
		Node node9 = network.createAndAddNode(this.scenario.createId("9"), this.scenario.createCoord(-11000.0, -2500.0));
		
		//base distance to work 5km; travel time 6min
		network.createAndAddLink(this.scenario.createId("2"), node2, node3, 4000, 13.89, 3600, 1, null, null);
		network.createAndAddLink(this.scenario.createId("4"), node4, node5, 1000, 13.89, 3600, 1, null, null);

		//additional distance to work 20km; travel time 24min
		network.createAndAddLink(this.scenario.createId("3"), node3, node4, 20000, 13.89, 3600, 1, null, null);
		
		//additional distance to work 45km; travel time 54min
		network.createAndAddLink(this.scenario.createId("8"), node3, node8, 22500, 13.89, 3600, 1, null, null);
		network.createAndAddLink(this.scenario.createId("9"), node8, node4, 22500, 13.89, 3600, 1, null, null);
		
		//additional distance to work 70km; travel time 84min
		network.createAndAddLink(this.scenario.createId("10"), node3, node9, 35000, 13.89, 3600, 1, null, null);
		network.createAndAddLink(this.scenario.createId("11"), node9, node4, 35000, 13.89, 3600, 1, null, null);
		
		//distance from work 5km; travel time 6min
		network.createAndAddLink(this.scenario.createId("5"), node5, node6, 1000, 13.89, 3600, 1, null, null);
		network.createAndAddLink(this.scenario.createId("6"), node6, node7, 2000, 13.89, 3600, 1, null, null);
		network.createAndAddLink(this.scenario.createId("7"), node7, node1, 1000, 13.89, 3600, 1, null, null);
		network.createAndAddLink(this.scenario.createId("1"), node1, node2, 1, 13.89, 3600, 1, null, null);
	}

	public static void main(String[] args) {
		SpaceTimeProbability stp = new SpaceTimeProbability();
		stp.run();
	}
}