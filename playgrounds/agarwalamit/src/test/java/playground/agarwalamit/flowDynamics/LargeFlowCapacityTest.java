/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,        *
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
package playground.agarwalamit.flowDynamics;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import java.util.*;

/**
 * Tests that two persons can leave a link at the same time if flow capacity permits
 * In other words, test if qsim can handle capacity more than 3600 PCU/Hr.
 * If the flow capacity is 3601 PCU/Hr it will allow the two vehicles.
 * 
 */
public class LargeFlowCapacityTest {

	@Test 
	public void test4PassingInFreeFlowState(){

		SimpleNetwork net = new SimpleNetwork();

		//=== build plans; two persons with cars enter and leaves one link at the same time and should have the same travel time.

		for(int i=0;i<2;i++){
			Id<Person> id = Id.create(i, Person.class);
			Person p = net.population.getFactory().createPerson(id);
			PlanImpl plan = ((PersonImpl)p).createAndAddPlan(true);
			ActivityImpl a1 = plan.createAndAddActivity("h",net.link1.getId());
			a1.setEndTime(8*3600);
			LegImpl leg=plan.createAndAddLeg(TransportMode.car);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(net.link1.getId(), net.link3.getId());
			route.setLinkIds(net.link1.getId(), Arrays.asList(net.link2.getId()), net.link3.getId());
			leg.setRoute(route);
			plan.createAndAddActivity("w", net.link3.getId());
			net.population.addPerson(p);
		}

		Map<Id<Person>, Map<Id<Link>, double[]>> personLinkTravelTimes = new HashMap<Id<Person>, Map<Id<Link>, double[]>>();

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new PersonLinkTravelTimeEventHandler(personLinkTravelTimes));

		QSim qSim = createQSim(net,manager);
		qSim.run();

		Map<Id<Link>, double[]> times1 = personLinkTravelTimes.get(Id.create("0", Person.class));
		Map<Id<Link>, double[]> times2 = personLinkTravelTimes.get(Id.create("1", Person.class));

		int linkEnterTime1 = (int)times1.get(Id.create("2", Link.class))[0]; 
		int linkEnterTime2 = (int)times2.get(Id.create("2", Link.class))[0];

		int linkLeaveTime1 = (int)times1.get(Id.create("2", Link.class))[1]; 
		int linkLeaveTime2 = (int)times2.get(Id.create("2", Link.class))[1];
		
		Assert.assertEquals("Vehicles Entered at different time", 0, linkEnterTime1-linkEnterTime2);
		Assert.assertEquals("Vehicles Entered at same time but not leaving the link at the same time.", 0, linkLeaveTime1-linkLeaveTime2);
	}

	private QSim createQSim (SimpleNetwork net, EventsManager manager){
		Scenario sc = net.scenario;
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
		qSim.addAgentSource(agentSource);
		return qSim;
	}


	private static final class SimpleNetwork{

		final Config config;
		final Scenario scenario ;
		final NetworkImpl network;
		final Population population;
		final Link link1;
		final Link link2;
		final Link link3;

		public SimpleNetwork(){

			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			config = scenario.getConfig();
			config.qsim().setFlowCapFactor(1.0);
			config.qsim().setStorageCapFactor(1.0);
			config.qsim().setMainModes(Arrays.asList("car","bike"));
			config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ.name());

			network = (NetworkImpl) scenario.getNetwork();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			Node node1 = network.createAndAddNode(Id.create("1", Node.class), scenario.createCoord(-100.0,0.0));
			Node node2 = network.createAndAddNode(Id.create("2", Node.class), scenario.createCoord( 0.0,  0.0));
			Node node3 = network.createAndAddNode(Id.create("3", Node.class), scenario.createCoord( 0.0,1000.0));
			Node node4 = network.createAndAddNode(Id.create("4", Node.class), scenario.createCoord( 0.0,1100.0));

			Set<String> allowedModes = new HashSet<String>(); allowedModes.addAll(Arrays.asList("car","bike"));

			link1 = network.createAndAddLink(Id.create("1", Link.class), node1, node2, 100, 25, 3601, 1, null, "22"); //capacity is 1 PCU per min.
			link2 = network.createAndAddLink(Id.create("2", Link.class), node2, node3, 1000, 25, 3601, 1, null, "22");	
			link3 = network.createAndAddLink(Id.create("3", Link.class), node3, node4, 100, 25, 3600, 1, null, "22");

			population = scenario.getPopulation();
		}
	}

	private static class PersonLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Person>, Map<Id<Link>, double[]>> personLinkEnterLeaveTimes;

		public PersonLinkTravelTimeEventHandler(Map<Id<Person>, Map<Id<Link>, double[]>> agentLinkEnterLeaveTimes) {
			this.personLinkEnterLeaveTimes = agentLinkEnterLeaveTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Logger.getLogger(PersonLinkTravelTimeEventHandler.class).info(event.toString());
			Map<Id<Link>, double[]> times = this.personLinkEnterLeaveTimes.get(event.getPersonId());
			if (times == null) {
				times = new HashMap<Id<Link>, double[]>();
				double [] linkEnterLeaveTime = {Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
				times.put(event.getLinkId(), linkEnterLeaveTime);
				this.personLinkEnterLeaveTimes.put(event.getPersonId(), times);
			}
			double linkLeaveTime;
			if(times.get(event.getLinkId())!=null){
				linkLeaveTime = times.get(event.getLinkId())[1];
			} else linkLeaveTime = Double.POSITIVE_INFINITY;
			
			double [] linkEnterTime = {event.getTime(),linkLeaveTime};
			times.put(event.getLinkId(), linkEnterTime);
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Logger.getLogger(PersonLinkTravelTimeEventHandler.class).info(event.toString());
			Map<Id<Link>, double[]> times = this.personLinkEnterLeaveTimes.get(event.getPersonId());
			if (times != null) {
				double linkEnterTime = times.get(event.getLinkId())[0];
				double [] linkEnterLeaveTime = {linkEnterTime,event.getTime()};
				times.put(event.getLinkId(), linkEnterLeaveTime);
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}
}
