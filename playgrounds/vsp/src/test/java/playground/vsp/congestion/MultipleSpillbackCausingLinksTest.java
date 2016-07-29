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
import org.matsim.api.core.v01.population.*;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author amit
 */

public class MultipleSpillbackCausingLinksTest {

	private final boolean isUsingOTFVis = false;

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * This test shows that (1) an agent can have spill back delays due to more than one link and 
	 * therefore, need to iterate through all spill back causing links.
	 * (2) an agent may have two different links as next link in the route and therefore correct one should be adopted. 
	 */
	@Test
	public final void multipleSpillBackTest(){

		/*
		 * 10 agents
		 * trip1 - link 1 (home),2,3,4 (work)
		 * trip2 - link 4(work), 5, 1 (home)
		 * trip3 - link 1(home), 6,7, 4 (work)
		 * Agents in their second (home-work) trip are delays due to spill back on link 2 and 6 both.
		 */

		List<CongestionEvent> congestionEvents = getAffectedPersonId2Delays();

		int index=1;
		List<Double> eventTimes = new ArrayList<Double>();

		for(CongestionEvent e : congestionEvents){
			if(e.getAffectedAgentId().equals(Id.createPersonId("2"))&&e.getLinkId().equals(Id.createLinkId("2")) && index ==1){
				// first next link in the route is link 2
				Assert.assertEquals("Delay on first next link in the route is not correct.", 10.0, e.getDelay(), MatsimTestUtils.EPSILON);
				eventTimes.add(e.getTime());
				index ++;
			}

			if(e.getAffectedAgentId().equals(Id.createPersonId("2"))&&e.getLinkId().equals(Id.createLinkId("6")) && (index == 2 || index == 3) ){
				// second next link in the route is link 6
				// agent 3 and 4 leave prior to agent 2 during home work (trip3)
				if(e.getCausingAgentId().equals(Id.createPersonId("3"))){ 
					Assert.assertEquals("Delay on second next link in the route is not correct.", 10.0, e.getDelay(), MatsimTestUtils.EPSILON);
				} else if (e.getCausingAgentId().equals(Id.createPersonId("4"))){
					Assert.assertEquals("Delay on second next link in the route is not correct.", 10.0, e.getDelay(), MatsimTestUtils.EPSILON);
				}
				eventTimes.add(e.getTime());
				index ++;
			}

			if(e.getAffectedAgentId().equals(Id.createPersonId("2"))&&e.getLinkId().equals(Id.createLinkId("2")) && index ==4){
				// first next link in the route is link 2
				// multiple spill back causing link
				Assert.assertEquals("Delay on first next link in the route due to multiple spill back is not correct.", 6.0, e.getDelay(), MatsimTestUtils.EPSILON);
				eventTimes.add(e.getTime());
				index ++;
			}
		}

		// now check, for agent 2, first link in the route (trip1) is occur first and rest later time i.e. during index ==1. 
		Assert.assertFalse("Congestion event for first next link in the route should occur first.", eventTimes.get(0) > eventTimes.get(1));

		// all other congestion event for agent 2 while leaving link 1 should occur at the same time.
		Assert.assertFalse("Congestion event for multiple spill back causing links should occur at the same time.", eventTimes.get(1) == eventTimes.get(2) && eventTimes.get(2) == eventTimes.get(3));


	}

	private List<CongestionEvent> getAffectedPersonId2Delays(){

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

		events.addHandler(new CongestionHandlerImplV4(events, sc));

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

		if(isUsingOTFVis){
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
		Link link6;
		Link link7;

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
			Node node6 = NetworkUtils.createAndAddNode(network, Id.createNodeId("6"), new Coord((double) 500, (double) 50));
			final Node fromNode = node1;
			final Node toNode = node2;

			link1 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("1")), fromNode, toNode, 100.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode1 = node2;
			final Node toNode1 = node3;
			link2 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("2")), fromNode1, toNode1, 7.0, 20.0, (double) 360, (double) 1, null, (String) "7");
			final Node fromNode2 = node3;
			final Node toNode2 = node4;
			link3 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("3")), fromNode2, toNode2, 100.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode3 = node4;
			final Node toNode3 = node5;
			link4 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("4")), fromNode3, toNode3, 100.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode4 = node5;
			final Node toNode4 = node1;
			link5 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("5")), fromNode4, toNode4, 100.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode5 = node2;
			final Node toNode5 = node6;
			link6 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("6")), fromNode5, toNode5, 7.0, 20.0, (double) 360, (double) 1, null, (String) "7");
			final Node fromNode6 = node6;
			final Node toNode6 = node4;
			link7 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("7")), fromNode6, toNode6, 100.0, 20.0, (double) 3600, (double) 1, null, (String) "7");

		}

		private void createPopulation(){

			for(int i=1;i<10;i++){

				Id<Person> id = Id.createPersonId(i);
				Person p = population.getFactory().createPerson(id);
				Plan plan = population.getFactory().createPlan();
				p.addPlan(plan);

				{//home-work route 1
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
					a2.setEndTime(0+50-i);
					plan.addActivity(a2);
				}

				{//work-home
					Leg leg = population.getFactory().createLeg(TransportMode.car);
					plan.addLeg(leg);
					LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
					NetworkRoute route = (NetworkRoute) factory.createRoute(link4.getId(), link1.getId());
					List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
					linkIds.add(link5.getId());
					route.setLinkIds(link4.getId(), linkIds, link1.getId());
					leg.setRoute(route);
					Activity a3 = population.getFactory().createActivityFromLinkId("h", link1.getId());
					plan.addActivity(a3);
					a3.setEndTime(0+60-5*i);
				}

				{//home-work route 2
					Leg leg = population.getFactory().createLeg(TransportMode.car);
					plan.addLeg(leg);
					LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
					NetworkRoute route = (NetworkRoute) factory.createRoute(link1.getId(), link4.getId());
					List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
					linkIds.add(link6.getId());
					linkIds.add(link7.getId());
					route.setLinkIds(link1.getId(), linkIds, link4.getId());
					leg.setRoute(route);
					Activity a4 = population.getFactory().createActivityFromLinkId("w", link4.getId());
					plan.addActivity(a4);
				}
				population.addPerson(p);	
			}
		}
	}
}

