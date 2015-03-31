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
package playground.agarwalamit.congestionPricing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
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
import playground.vsp.congestion.handlers.CongestionHandlerImplV6;

/**
 * @author amit
 */

public class CombinedFlowAndStorageDelayTestV4 {
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * Basically agent first delay due to flow cap and then flow cap is relieved, delayed due to storage cap
	 * Since, it is waiting for storage delay, causing agent for flow delay is not captured. 
	 */
	@Test
	public final void combinationOfFlowAndStorageDelay(){

		List<CongestionEvent> congestionEvents = getAffectedPersonId2Delays("v4");
		//yet to check the values.
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
		else if(congestionPricingImpl.equalsIgnoreCase("v6")) events.addHandler(new CongestionHandlerImplV6(events, sc));

		QSim sim = createQSim(sc, events);
		sim.run();

		return congestionEvents;
	}

	private QSim createQSim (Scenario sc, EventsManager manager){
		QSim qSim1 = new QSim(sc, manager);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim1);
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
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

		// otfvis configuration.  There is more you can do here than via file!
		final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
		otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
		//				otfVisConfig.setShowParking(true) ; // this does not really work

		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, manager, qSim);
		OTFClientLive.run(sc.getConfig(), server);

		return qSim;
	}

	/**
	 * generates network with 5 links.
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
		//		Link link6;
		//		Link link7;

		public createPseudoInputs(){
			config=ConfigUtils.createConfig();
			this.scenario = ScenarioUtils.loadScenario(config);
			network =  (NetworkImpl) this.scenario.getNetwork();
			population = this.scenario.getPopulation();
		}

		private void createNetwork(){

			Node node1 = network.createAndAddNode(Id.createNodeId("1"), this.scenario.createCoord(0, 0)) ;
			Node node2 = network.createAndAddNode(Id.createNodeId("2"), this.scenario.createCoord(0, 100));
			Node node3 = network.createAndAddNode(Id.createNodeId("3"), this.scenario.createCoord(500, 150));
			Node node4 = network.createAndAddNode(Id.createNodeId("4"), this.scenario.createCoord(1000, 100));
			Node node5 = network.createAndAddNode(Id.createNodeId("5"), this.scenario.createCoord(1000, 0));

			link1 = network.createAndAddLink(Id.createLinkId(String.valueOf("1")), node1, node2,100.0,20.0,3600,1,null,"7");
			link2 = network.createAndAddLink(Id.createLinkId(String.valueOf("2")), node2, node3,100.0,20.0,36,1,null,"7");
			link3 = network.createAndAddLink(Id.createLinkId(String.valueOf("3")), node3, node4,1.0,20.0,360,1,null,"7");
			link4 = network.createAndAddLink(Id.createLinkId(String.valueOf("4")), node4, node5,100.0,20.0,3600,1,null,"7");
			link5 = network.createAndAddLink(Id.createLinkId(String.valueOf("5")), node1, node3,100.0,20.0,3600,1,null,"7");
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

