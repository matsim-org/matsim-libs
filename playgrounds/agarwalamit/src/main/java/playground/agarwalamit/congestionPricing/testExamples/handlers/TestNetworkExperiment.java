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
package playground.agarwalamit.congestionPricing.testExamples.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;

/**
 * @author amit
 */
class TestNetworkExperiment {
	final Logger log = Logger.getLogger(TestNetworkExperiment.class);


	public static void main(String[] args) {
		TestNetworkExperiment testEx = new TestNetworkExperiment();
				testEx.test4MarginalCongestionCosts();
//		testEx.printData();
	}

	void test4MarginalCongestionCosts(){
		String outputDir = "./output/pop10/";
		new File(outputDir).mkdirs();

		int numberOfPersonInPlan = 10;
		createPseudoInputs pseudoInputs = new createPseudoInputs();
		pseudoInputs.createNetwork();
		pseudoInputs.createPopulation(numberOfPersonInPlan);
		Scenario sc = pseudoInputs.scenario;
		sc.getConfig().controler().setWriteEventsInterval(1);

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new CongestionHandlerImplV4(events, sc));
		EventWriterXML writer = new EventWriterXML(outputDir+"/events.xml.gz");
		events.addHandler(writer);

		final boolean useOTFVis = true ;
		QSim qSim = createQSim(sc, events, useOTFVis);
		qSim.run();
		writer.closeFile();
	}


	void printData(){
		Map<Id<Person>, Double> personId2AffectedDelaysV3 = generatePersonId2Delays("v3", "affected");
		Map<Id<Person>, Double> personId2AffectedDelaysV4 = generatePersonId2Delays("v4", "affected");
		Map<Id<Person>, Double> personId2AffectedDelaysV5 = generatePersonId2Delays("v5", "affected");
		Map<Id<Person>, Double> personId2AffectedDelaysV6 = generatePersonId2Delays("v6", "affected");

		Map<Id<Person>, Double> personId2CausingDelaysV3 = generatePersonId2Delays("v3", "causing");
		Map<Id<Person>, Double> personId2CausingDelaysV4 = generatePersonId2Delays("v4", "causing");
		Map<Id<Person>, Double> personId2CausingDelaysV5 = generatePersonId2Delays("v5", "causing");
		Map<Id<Person>, Double> personId2CausingDelaysV6 = generatePersonId2Delays("v6", "causing");

		BufferedWriter  writer = IOUtils.getBufferedWriter("./output/comparisonOfPricingImpls.txt");
		try {
			writer.write("PersonID \t Delay affected(V3) \t Delay affected(V4) \t Delay affected (V5) \t Delay affected (V6) \t Delay caused (V3) \t Delay caused (V4) \t Delay caused (V5) \t Delay caused (V6) \n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
		//		System.out.println("PersonID \t Delay affected(V4) \t Delay affected (V5) \t Delay affected (V6) \t Delay caused (V4) \t Delay caused (V5) \t Delay caused (V6) ");

		Set<Id<Person>> personIds = new HashSet<>();
		personIds.addAll(personId2AffectedDelaysV4.keySet());
		personIds.addAll(personId2CausingDelaysV4.keySet());
		personIds.addAll(personId2AffectedDelaysV5.keySet());
		personIds.addAll(personId2CausingDelaysV5.keySet());
		personIds.addAll(personId2AffectedDelaysV3.keySet());
		personIds.addAll(personId2CausingDelaysV3.keySet());
		// yy why is v6 not added? kai, aug'15


		try {
			for(Id<Person> personId : personIds){
				writer.write(personId + "\t" + personId2AffectedDelaysV3.get(personId) + "\t"+ personId2AffectedDelaysV4.get(personId) + 
						"\t" + personId2AffectedDelaysV5.get(personId) + "\t"+ personId2AffectedDelaysV6.get(personId) + "\t"  + personId2CausingDelaysV3.get(personId) + "\t" + 
						personId2CausingDelaysV4.get(personId) + "\t" +personId2CausingDelaysV5.get(personId) + "\t" + personId2CausingDelaysV6.get(personId)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}
	}

	Map<Id<Person>, Double> generatePersonId2Delays(String congestionPricingImpl, String affectedOrCausing){
		int numberOfPersonInPlan = 10;
		createPseudoInputs pseudoInputs = new createPseudoInputs();
		pseudoInputs.createNetwork();
		pseudoInputs.createPopulation(numberOfPersonInPlan);
		Scenario sc = pseudoInputs.scenario;

		EventsManager events = EventsUtils.createEventsManager();

		Map<Id<Person>, Double> personId2Delay = new HashMap<>();

		final List<CongestionEvent> congestionEvents = new ArrayList<>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});
		if(congestionPricingImpl.equalsIgnoreCase("v3")) events.addHandler(new CongestionHandlerImplV3(events, (MutableScenario)sc));
		else if(congestionPricingImpl.equalsIgnoreCase("v4")) events.addHandler(new CongestionHandlerImplV4(events, sc));
//		else if(congestionPricingImpl.equalsIgnoreCase("v6")) events.addHandler(new CongestionHandlerImplV6(events, sc));

		QSim sim = createQSim(sc, events, false);
		sim.run();

		for (CongestionEvent event : congestionEvents) {
			Id<Person> desiredPerson = null;
			if(affectedOrCausing.equalsIgnoreCase("affected")) desiredPerson = event.getAffectedAgentId();
			else if(affectedOrCausing.equalsIgnoreCase("causing")) desiredPerson = event.getCausingAgentId();

			if(personId2Delay.containsKey(desiredPerson)){
				double delaySoFar = personId2Delay.get(desiredPerson);
				personId2Delay.put(desiredPerson, event.getDelay()+delaySoFar);
			} else personId2Delay.put(desiredPerson, event.getDelay());
		}
		return personId2Delay;
	}

	private static QSim createQSim (Scenario sc, EventsManager manager,boolean useOTFVis){
		QSim qSim1 = new QSim(sc, manager);
		ActivityEngine activityEngine = new ActivityEngine(manager, qSim1.getAgentCounter());
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim1);
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, manager);
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);

		Map<String, VehicleType> modeVehicleTypes = new HashMap<>();

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);
		modeVehicleTypes.put("car", car);
		agentSource.setModeVehicleTypes(modeVehicleTypes);
		qSim.addAgentSource(agentSource);

		if ( useOTFVis ) {
			// otfvis configuration.  There is more you can do here than via file!
			final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
			otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
			//				otfVisConfig.setShowParking(true) ; // this does not really work

			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, manager, qSim);
			OTFClientLive.run(sc.getConfig(), server);
		}
		return qSim;
	}

	/**
	 * generates network with 6 links. Even persons will go on one branch (down) and odd persons will go on other (up).
	 *<p>				  o----4----o
	 *<p> 				  |
	 *<p>				  3 
	 *<p>				  |
	 *<p>				  |
	 *<p>  o--1---o---2---o
	 *<p>				  |
	 *<p>				  |
	 *<p>				  5
	 *<p>				  |
	 *<p>				  o----5----o
	 */
	private class createPseudoInputs {
		final Scenario scenario;
		final Config config;
		final Network network;
		final Population population;
		Link link1;
		Link link2;
		Link link3;
		Link link4;
		Link link5;
		Link link6;
		public createPseudoInputs(){
			config=ConfigUtils.createConfig();
			this.scenario = ScenarioUtils.loadScenario(config);
			network =  (Network) this.scenario.getNetwork();
			population = this.scenario.getPopulation();
		}

		private void createNetwork(){
			Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord((double) 0, (double) 0));
			Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord((double) 100, (double) 100));
			Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord((double) 300, (double) 90));
			Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("4"), new Coord((double) 500, (double) 200));
			Node node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId("5"), new Coord((double) 700, (double) 150));
			Node node6 = NetworkUtils.createAndAddNode(network, Id.createNodeId("6"), new Coord((double) 500, (double) 20));
			Node node7 = NetworkUtils.createAndAddNode(network, Id.createNodeId("7"), new Coord((double) 700, (double) 100));
			final Node fromNode = node1;
			final Node toNode = node2;

			link1 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("1")), fromNode, toNode, 1000.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode1 = node2;
			final Node toNode1 = node3;
			link2 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("2")), fromNode1, toNode1, 1000.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode2 = node3;
			final Node toNode2 = node4;
			link3 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("3")), fromNode2, toNode2, 10.0, 20.0, (double) 360, (double) 1, null, (String) "7");
			final Node fromNode3 = node4;
			final Node toNode3 = node5;
			link4 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("4")), fromNode3, toNode3, 1000.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode4 = node3;
			final Node toNode4 = node6;
			link5 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("5")), fromNode4, toNode4, 1000.0, 20.0, (double) 360, (double) 1, null, (String) "7");
			final Node fromNode5 = node6;
			final Node toNode5 = node7;
			link6 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("6")), fromNode5, toNode5, 1000.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
		}

		private void createPopulation(int numberOfPersons){

			for(int i=0;i<numberOfPersons;i++){
				Id<Person> id = Id.createPersonId(i);
				Person p = population.getFactory().createPerson(id);
				Plan plan = population.getFactory().createPlan();
				p.addPlan(plan);
				Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());
				a1.setEndTime(8*3600+i);
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				plan.addActivity(a1);
				plan.addLeg(leg);
				LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
				NetworkRoute route;
				List<Id<Link>> linkIds = new ArrayList<>();
				if(i%2==0) {
					route= (NetworkRoute) factory.createRoute(link1.getId(), link4.getId());
					linkIds.add(link2.getId());
					linkIds.add(link3.getId());
					route.setLinkIds(link1.getId(), linkIds, link4.getId());
					leg.setRoute(route);
					Activity a2 = population.getFactory().createActivityFromLinkId("w", link4.getId());
					plan.addActivity(a2);
				} else {
					route = (NetworkRoute) factory.createRoute(link1.getId(), link6.getId());
					linkIds.add(link2.getId());
					linkIds.add(link5.getId());
					route.setLinkIds(link1.getId(), linkIds, link6.getId());
					leg.setRoute(route);
					Activity a2 = population.getFactory().createActivityFromLinkId("w", link6.getId());
					plan.addActivity(a2);
				}
				population.addPerson(p);
			}
		}
	}
}
