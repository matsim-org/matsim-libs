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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.agarwalamit.congestionPricing.MarginalCongestionHandlerImplV5;
import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.CongestionHandlerImplV6;

/**
 * @author amit
 */
public class TestNetworkExperiment {
	final Logger log = Logger.getLogger(TestNetworkExperiment.class);


	public static void main(String[] args) {
		TestNetworkExperiment testEx = new TestNetworkExperiment();
				testEx.test4MarginalCongestionCosts();
//		testEx.printData();
	}

	public void test4MarginalCongestionCosts(){
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


	public void printData(){
		Map<Id<Person>, Double> personId2AffectedDelays_v3 = getPersonId2Delays("v3", "affected");
		Map<Id<Person>, Double> personId2AffectedDelays_v4 = getPersonId2Delays("v4", "affected");
		Map<Id<Person>, Double> personId2AffectedDelays_v5 = getPersonId2Delays("v5", "affected");
		Map<Id<Person>, Double> personId2AffectedDelays_v6 = getPersonId2Delays("v6", "affected");

		Map<Id<Person>, Double> personId2CausingDelays_v3 = getPersonId2Delays("v3", "causing");
		Map<Id<Person>, Double> personId2CausingDelays_v4 = getPersonId2Delays("v4", "causing");
		Map<Id<Person>, Double> personId2CausingDelays_v5 = getPersonId2Delays("v5", "causing");
		Map<Id<Person>, Double> personId2CausingDelays_v6 = getPersonId2Delays("v6", "causing");

		BufferedWriter  writer = IOUtils.getBufferedWriter("./output/comparisonOfPricingImpls.txt");
		try {
			writer.write("PersonID \t Delay affected(V3) \t Delay affected(V4) \t Delay affected (V5) \t Delay affected (V6) \t Delay caused (V3) \t Delay caused (V4) \t Delay caused (V5) \t Delay caused (V6) \n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
		//		System.out.println("PersonID \t Delay affected(V4) \t Delay affected (V5) \t Delay affected (V6) \t Delay caused (V4) \t Delay caused (V5) \t Delay caused (V6) ");

		Set<Id<Person>> personIds = new HashSet<Id<Person>>();
		personIds.addAll(personId2AffectedDelays_v4.keySet());
		personIds.addAll(personId2CausingDelays_v4.keySet());
		personIds.addAll(personId2AffectedDelays_v5.keySet());
		personIds.addAll(personId2CausingDelays_v5.keySet());
		personIds.addAll(personId2AffectedDelays_v3.keySet());
		personIds.addAll(personId2CausingDelays_v3.keySet());


		try {
			for(Id<Person> personId : personIds){
				writer.write(personId + "\t" + personId2AffectedDelays_v3.get(personId) + "\t"+ personId2AffectedDelays_v4.get(personId) + 
						"\t" + personId2AffectedDelays_v5.get(personId) + "\t"+ personId2AffectedDelays_v6.get(personId) + "\t"  + personId2CausingDelays_v3.get(personId) + "\t" + 
						personId2CausingDelays_v4.get(personId) + "\t" +personId2CausingDelays_v5.get(personId) + "\t" + personId2CausingDelays_v6.get(personId)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}
	}

	public Map<Id<Person>, Double> getPersonId2Delays(String congestionPricingImpl, String affectedOrCausing){
		int numberOfPersonInPlan = 10;
		createPseudoInputs pseudoInputs = new createPseudoInputs();
		pseudoInputs.createNetwork();
		pseudoInputs.createPopulation(numberOfPersonInPlan);
		Scenario sc = pseudoInputs.scenario;

		EventsManager events = EventsUtils.createEventsManager();

		Map<Id<Person>, Double> personId2Delay = new HashMap<Id<Person>, Double>();

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});
		if(congestionPricingImpl.equalsIgnoreCase("v3")) events.addHandler(new CongestionHandlerImplV3(events, (ScenarioImpl)sc));
		else if(congestionPricingImpl.equalsIgnoreCase("v4")) events.addHandler(new CongestionHandlerImplV4(events, sc));
		else if(congestionPricingImpl.equalsIgnoreCase("v5")) events.addHandler(new MarginalCongestionHandlerImplV5(events, sc));
		else if(congestionPricingImpl.equalsIgnoreCase("v6")) events.addHandler(new CongestionHandlerImplV6(events, sc));

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

	private QSim createQSim (Scenario sc, EventsManager manager,boolean useOTFVis){
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

		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();

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
		Scenario scenario;
		Config config;
		NetworkImpl network;
		Population population;
		Link link1;
		Link link2;
		Link link3;
		Link link4;
		Link link5;
		Link link6;
		public createPseudoInputs(){
			config=ConfigUtils.createConfig();
			this.scenario = ScenarioUtils.loadScenario(config);
			network =  (NetworkImpl) this.scenario.getNetwork();
			population = this.scenario.getPopulation();
		}

		private void createNetwork(){
			Node node1 = network.createAndAddNode(Id.createNodeId("1"), this.scenario.createCoord(0, 0));
			Node node2 = network.createAndAddNode(Id.createNodeId("2"), this.scenario.createCoord(100, 100));
			Node node3 = network.createAndAddNode(Id.createNodeId("3"), this.scenario.createCoord(300, 90));
			Node node4 = network.createAndAddNode(Id.createNodeId("4"), this.scenario.createCoord(500, 200));
			Node node5 = network.createAndAddNode(Id.createNodeId("5"), this.scenario.createCoord(700, 150));
			Node node6 = network.createAndAddNode(Id.createNodeId("6"), this.scenario.createCoord(500, 20));
			Node node7 = network.createAndAddNode(Id.createNodeId("7"), this.scenario.createCoord(700, 100));

			link1 = network.createAndAddLink(Id.createLinkId(String.valueOf("1")), node1, node2,1000.0,20.0,3600,1,null,"7");
			link2 = network.createAndAddLink(Id.createLinkId(String.valueOf("2")), node2, node3,1000.0,20.0,3600,1,null,"7");
			link3 = network.createAndAddLink(Id.createLinkId(String.valueOf("3")), node3, node4,10.0,20.0,360,1,null,"7");
			link4 = network.createAndAddLink(Id.createLinkId(String.valueOf("4")), node4, node5,1000.0,20.0,3600,1,null,"7");
			link5 = network.createAndAddLink(Id.createLinkId(String.valueOf("5")), node3, node6,1000.0,20.0,360,1,null,"7");
			link6 = network.createAndAddLink(Id.createLinkId(String.valueOf("6")), node6, node7,1000.0,20.0,3600,1,null,"7");
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
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
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
