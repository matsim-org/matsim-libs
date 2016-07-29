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
package playground.vsp.congestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;

/**
 * Accounting for flow delays even if leaving agents list is empty.
 * 
 * @author amit
 */

public class CombinedFlowAndStorageDelayTest {
	
	private final boolean usingOTFVis = false;
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * Basically agent first delay due to flow cap and then when agent can leave the link, it is delayed due to storage cap
	 * on the next link in the route. Since, it is waiting for storage delay, causing agent(s) for flow delay is not captured by leaving agents list.
	 * Thus, such agents are caught by comparing the free speed link time of each agent. If time headway is less than minimum time headway of the link,
	 * agents are stored to charge later if required.
	 */
	@Test
	public final void implV4Test(){
		/*
		 * In the test, two routes (1-2-3-4 and 5-3-4) are assigned to agents. First two agents (1,2) start on first route and next two (3,4) on 
		 * other route. After agent 1 leave the link 2 (marginal flow delay =100), agent 2 is delayed. Mean while, before agent 2 can move to next link,
		 * link 3 is blocked by agent 3 (departed on link 5). Thus, agent 2 on link 2 is delayed. Causing agents should be 1 (flow cap), 4 (storage cap).
		 */
		List<CongestionEvent> congestionEvents = getAffectedPersonId2Delays("v4");
		
		for(CongestionEvent e : congestionEvents){
			if(e.getAffectedAgentId().equals(Id.createPersonId("2")) && e.getCausingAgentId().equals(Id.createPersonId("1"))){
				Assert.assertEquals("Delay caused by agent 2 is not correct.", 100, e.getDelay(), MatsimTestUtils.EPSILON);
				// this is not captured by only leaving agents list.
			} 
		}
		
		Assert.assertEquals("Number of congestion events are not correct.", 4, congestionEvents.size(), MatsimTestUtils.EPSILON);
	}

//	@Test
	public final void implV6Test(){
		/*
		 * In the test, two routes (1-2-3-4 and 5-3-4) are assigned to agents. First two agents (1,2) start on first route and next two (3,4) on 
		 * other route. After agent 1 leave the link 2 (marginal flow delay =100), agent 2 is delayed. Mean while, before agent 2 can move to next link,
		 * link 3 is blocked by agent 3 (departed on link 5). Thus, agent 2 on link 2 is delayed. Causing agents should be 1 (flow cap), 4 (storage cap).
		 */
		List<CongestionEvent> congestionEvents = getAffectedPersonId2Delays("v6");
		
		for(CongestionEvent e : congestionEvents){
			if(e.getAffectedAgentId().equals(Id.createPersonId("2")) && e.getLinkId().equals(Id.createLinkId("2"))){
				Assert.assertEquals("Wrong causing agent", Id.createPersonId("1"), e.getCausingAgentId());
				// this is not captured by only leaving agents list.
			} 
		}
		Assert.assertEquals("Number of congestion events are not correct.", 3, congestionEvents.size(), MatsimTestUtils.EPSILON);
	}
	
	private List<CongestionEvent> getAffectedPersonId2Delays(String congestionPricingImpl){

		createPseudoInputs pseudoInputs = new createPseudoInputs();
		pseudoInputs.createNetwork();
		pseudoInputs.createPopulation();
		Scenario sc = pseudoInputs.scenario;

		EventsManager events = EventsUtils.createEventsManager();

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
		if(congestionPricingImpl.equalsIgnoreCase("v4")) events.addHandler(new CongestionHandlerImplV4(events, sc));
//		else if(congestionPricingImpl.equalsIgnoreCase("v6")) events.addHandler(new CongestionHandlerImplV6(events, sc));

		QSim sim = createQSim(sc, events);
		sim.run();

		return congestionEvents;
	}

	private QSim createQSim (Scenario sc, EventsManager manager){
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

		if(usingOTFVis){
			// otfvis configuration.  There is more you can do here than via file!
			final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
			otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
			//				otfVisConfig.setShowParking(true) ; // this does not really work

			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, manager, qSim);
			OTFClientLive.run(sc.getConfig(), server);
		}
		return qSim;
	}

	private class createPseudoInputs {
		Scenario scenario;
		Config config;
		Network network;
		Population population;
		Link link1;
		Link link2;
		Link link3;
		Link link4;
		Link link5;

		public createPseudoInputs(){
			config=ConfigUtils.createConfig();
			this.scenario = ScenarioUtils.loadScenario(config);
			network =  (Network) this.scenario.getNetwork();
			population = this.scenario.getPopulation();
		}

		private void createNetwork(){

			Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord((double) 0, (double) 0)) ;
			Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord((double) 0, (double) 100));
			Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord((double) 500, (double) 150));
			Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("4"), new Coord((double) 1000, (double) 100));
			Node node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId("5"), new Coord((double) 1000, (double) 0));
			final Node fromNode = node1;
			final Node toNode = node2;

			link1 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("1")), fromNode, toNode, 100.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode1 = node2;
			final Node toNode1 = node3;
			link2 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("2")), fromNode1, toNode1, 100.0, 20.0, (double) 36, (double) 1, null, (String) "7");
			final Node fromNode2 = node3;
			final Node toNode2 = node4;
			link3 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("3")), fromNode2, toNode2, 1.0, 20.0, (double) 360, (double) 1, null, (String) "7");
			final Node fromNode3 = node4;
			final Node toNode3 = node5;
			link4 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("4")), fromNode3, toNode3, 100.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode4 = node1;
			final Node toNode4 = node3;
			
			link5 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("5")), fromNode4, toNode4, 100.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
		}

		private void createPopulation(){

			for(int i=1;i<3;i++){

				Id<Person> id = Id.createPersonId(i);
				Person p = population.getFactory().createPerson(id);
				Plan plan = population.getFactory().createPlan();
				p.addPlan(plan);

				Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());
				a1.setEndTime(0+i);
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				plan.addActivity(a1);
				plan.addLeg(leg);
				LinkNetworkRouteFactory factory1 = new LinkNetworkRouteFactory();
				NetworkRoute route1;
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				route1= (NetworkRoute) factory1.createRoute(link1.getId(), link4.getId());
				linkIds.add(link2.getId());
				linkIds.add(link3.getId());
				route1.setLinkIds(link1.getId(), linkIds, link4.getId());
				leg.setRoute(route1);
				Activity a2 = population.getFactory().createActivityFromLinkId("w", link4.getId());
				plan.addActivity(a2);
				population.addPerson(p);
			}

			for(int i=3;i<5;i++) {
				Id<Person> id = Id.createPersonId(i);
				Person p = population.getFactory().createPerson(id);
				Plan plan = population.getFactory().createPlan();
				p.addPlan(plan);

				Activity a1 = population.getFactory().createActivityFromLinkId("h", link5.getId());
				a1.setEndTime(100+i);
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				plan.addActivity(a1);
				plan.addLeg(leg);
				LinkNetworkRouteFactory factory1 = new LinkNetworkRouteFactory();
				NetworkRoute route1;
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				route1= (NetworkRoute) factory1.createRoute(link5.getId(), link4.getId());
				linkIds.add(link3.getId());
				route1.setLinkIds(link5.getId(), linkIds, link4.getId());
				leg.setRoute(route1);
				Activity a2 = population.getFactory().createActivityFromLinkId("w", link4.getId());
				plan.addActivity(a2);
				population.addPerson(p);
			}
		}

	}
}

