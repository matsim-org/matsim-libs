/* *********************************************************************** *
 * project: org.matsim.*
 * TwoLinesDemo.java
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

package org.matsim.pt.demo;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.ActiveQSimBridge;
import org.matsim.core.mobsim.qsim.AgentCounterImpl;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.analysis.RouteOccupancy;
import org.matsim.pt.analysis.VehicleTracker;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import java.util.ArrayList;
import java.util.Collections;

public class TwoLinesDemo {

	private final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.transit().setUseTransit(true);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		config.qsim().setEndTime(24.0*3600);
	}

	private void createNetwork() {
		/*
		 * (2)---2---(4)---4---(6)                                       (12)---12---(14)
		 *                    o  \                                       /o
		 *                    2   \                                     /  6
		 *                         6                                   10
		 *                          \                                 /
		 *                           \                               /
		 *                           (7)---7---(8)---8---(9)---9---(10)
		 *                           /        o                   o  \
		 *                          /         3                   4   \
		 *                         5                                   11
		 *                        /                                     \
		 *                       /                                      o\
		 * (1)---1---(3)---3---(5)                                     5 (11)---13---(13)
		 *                    o
		 *                    1
		 *
		 */
		Network network = (Network) this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		double x3 = -2000;
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord(x3, (double) 0));
		double x2 = -2000;
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord(x2, (double) 1000));
		double x1 = -1000;
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord(x1, (double) 0));
		double x = -1000;
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord(x, (double) 1000));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 0, (double) 0));
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create(6, Node.class), new Coord((double) 0, (double) 1000));
		Node node7 = NetworkUtils.createAndAddNode(network, Id.create(7, Node.class), new Coord((double) 500, (double) 500));
		Node node8 = NetworkUtils.createAndAddNode(network, Id.create(8, Node.class), new Coord((double) 1500, (double) 500));
		Node node9 = NetworkUtils.createAndAddNode(network, Id.create(9, Node.class), new Coord((double) 2500, (double) 500));
		Node node10 = NetworkUtils.createAndAddNode(network, Id.create(10, Node.class), new Coord((double) 3500, (double) 500));
		Node node11 = NetworkUtils.createAndAddNode(network, Id.create(11, Node.class), new Coord((double) 4000, (double) 0));
		Node node12 = NetworkUtils.createAndAddNode(network, Id.create(12, Node.class), new Coord((double) 4000, (double) 1000));
		Node node13 = NetworkUtils.createAndAddNode(network, Id.create(13, Node.class), new Coord((double) 5000, (double) 0));
		Node node14 = NetworkUtils.createAndAddNode(network, Id.create(14, Node.class), new Coord((double) 5000, (double) 1000));
		final Node fromNode = node1;
		final Node toNode = node3;

		NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node4;
		NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), fromNode1, toNode1, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node5;
		NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), fromNode2, toNode2, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode3 = node4;
		final Node toNode3 = node6;
		NetworkUtils.createAndAddLink(network,Id.create(4, Link.class), fromNode3, toNode3, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode4 = node5;
		final Node toNode4 = node7;
		NetworkUtils.createAndAddLink(network,Id.create(5, Link.class), fromNode4, toNode4, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode5 = node6;
		final Node toNode5 = node7;
		NetworkUtils.createAndAddLink(network,Id.create(6, Link.class), fromNode5, toNode5, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode6 = node7;
		final Node toNode6 = node8;
		NetworkUtils.createAndAddLink(network,Id.create(7, Link.class), fromNode6, toNode6, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode7 = node8;
		final Node toNode7 = node9;
		NetworkUtils.createAndAddLink(network,Id.create(8, Link.class), fromNode7, toNode7, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode8 = node9;
		final Node toNode8 = node10;
		NetworkUtils.createAndAddLink(network,Id.create(9, Link.class), fromNode8, toNode8, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode9 = node10;
		final Node toNode9 = node12;
		NetworkUtils.createAndAddLink(network,Id.create(10, Link.class), fromNode9, toNode9, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode10 = node10;
		final Node toNode10 = node11;
		NetworkUtils.createAndAddLink(network,Id.create(11, Link.class), fromNode10, toNode10, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode11 = node12;
		final Node toNode11 = node14;
		NetworkUtils.createAndAddLink(network,Id.create(12, Link.class), fromNode11, toNode11, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode12 = node11;
		final Node toNode12 = node13;
		NetworkUtils.createAndAddLink(network,Id.create(13, Link.class), fromNode12, toNode12, 1000.0, 10.0, 3600.0, (double) 1 );
	}

	private void createTransitSchedule() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();
		double x1 = -100;
		double y = -50;
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(x1, y), false);
		double x = -100;
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(x, (double) 850), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord((double) 1400, (double) 450), false);
		TransitStopFacility stop4 = builder.createTransitStopFacility(Id.create(4, TransitStopFacility.class), new Coord((double) 3400, (double) 450), false);
		TransitStopFacility stop5 = builder.createTransitStopFacility(Id.create(5, TransitStopFacility.class), new Coord((double) 3900, (double) 50), false);
		TransitStopFacility stop6 = builder.createTransitStopFacility(Id.create(6, TransitStopFacility.class), new Coord((double) 3900, (double) 850), false);

		Link link1  = this.scenario.getNetwork().getLinks().get(Id.create( 1, Link.class));
		Link link2  = this.scenario.getNetwork().getLinks().get(Id.create( 2, Link.class));
		Link link3  = this.scenario.getNetwork().getLinks().get(Id.create( 3, Link.class));
		Link link4  = this.scenario.getNetwork().getLinks().get(Id.create( 4, Link.class));
		Link link5  = this.scenario.getNetwork().getLinks().get(Id.create( 5, Link.class));
		Link link6  = this.scenario.getNetwork().getLinks().get(Id.create( 6, Link.class));
		Link link7  = this.scenario.getNetwork().getLinks().get(Id.create( 7, Link.class));
		Link link8  = this.scenario.getNetwork().getLinks().get(Id.create( 8, Link.class));
		Link link9  = this.scenario.getNetwork().getLinks().get(Id.create( 9, Link.class));
		Link link10 = this.scenario.getNetwork().getLinks().get(Id.create(10, Link.class));
		Link link11 = this.scenario.getNetwork().getLinks().get(Id.create(11, Link.class));
		Link link12 = this.scenario.getNetwork().getLinks().get(Id.create(12, Link.class));
		Link link13 = this.scenario.getNetwork().getLinks().get(Id.create(13, Link.class));

		stop1.setLinkId(link3.getId());
		stop2.setLinkId(link4.getId());
		stop3.setLinkId(link7.getId());
		stop4.setLinkId(link9.getId());
		stop5.setLinkId(link11.getId());
		stop6.setLinkId(link10.getId());

		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);
		schedule.addStopFacility(stop4);
		schedule.addStopFacility(stop5);
		schedule.addStopFacility(stop6);

		TransitLine tLine1 = builder.createTransitLine(Id.create(1, TransitLine.class));
		NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link13.getId());
		ArrayList<Id<Link>> linkIdList = new ArrayList<>(6);
		Collections.addAll(linkIdList, link3.getId(), link5.getId(), link7.getId(), link8.getId(), link9.getId(), link11.getId());
		networkRoute.setLinkIds(link1.getId(), linkIdList, link13.getId());
		ArrayList<TransitRouteStop> stopList = new ArrayList<>(4);
		stopList.add(builder.createTransitRouteStop(stop1, 0, 0));
		stopList.add(builder.createTransitRouteStop(stop3, 90, 100));
		stopList.add(builder.createTransitRouteStop(stop4, 290, 300));
		stopList.add(builder.createTransitRouteStop(stop5, 390, Time.UNDEFINED_TIME));
		TransitRoute tRoute1 = builder.createTransitRoute(Id.create(1, TransitRoute.class), networkRoute, stopList, "bus");
		tLine1.addRoute(tRoute1);

		tRoute1.addDeparture(builder.createDeparture(Id.create(1, Departure.class), Time.parseTime("07:00:00")));
		tRoute1.addDeparture(builder.createDeparture(Id.create(2, Departure.class), Time.parseTime("07:05:00")));
		tRoute1.addDeparture(builder.createDeparture(Id.create(3, Departure.class), Time.parseTime("07:10:00")));
		tRoute1.addDeparture(builder.createDeparture(Id.create(4, Departure.class), Time.parseTime("07:15:00")));
		tRoute1.addDeparture(builder.createDeparture(Id.create(5, Departure.class), Time.parseTime("07:20:00")));
		tRoute1.addDeparture(builder.createDeparture(Id.create(6, Departure.class), Time.parseTime("07:25:00")));

		schedule.addTransitLine(tLine1);

		TransitLine tLine2 = builder.createTransitLine(Id.create(2, TransitLine.class));
		networkRoute = RouteUtils.createLinkNetworkRouteImpl(link2.getId(), link12.getId());
		linkIdList = new ArrayList<>(6);
		Collections.addAll(linkIdList, link4.getId(), link6.getId(), link7.getId(), link8.getId(), link9.getId(), link10.getId());
		networkRoute.setLinkIds(link2.getId(), linkIdList, link12.getId());
		stopList = new ArrayList<>(4);
		stopList.add(builder.createTransitRouteStop(stop2, 0, 0));
		stopList.add(builder.createTransitRouteStop(stop3, 90, 100));
		stopList.add(builder.createTransitRouteStop(stop4, 290, 300));
		stopList.add(builder.createTransitRouteStop(stop6, 390, Time.UNDEFINED_TIME));
		TransitRoute tRoute2 = builder.createTransitRoute(Id.create(1, TransitRoute.class), networkRoute, stopList, "bus");
		tLine2.addRoute(tRoute2);

		tRoute2.addDeparture(builder.createDeparture(Id.create(1, Departure.class), Time.parseTime("07:02:00")));
		tRoute2.addDeparture(builder.createDeparture(Id.create(2, Departure.class), Time.parseTime("07:12:00")));
		tRoute2.addDeparture(builder.createDeparture(Id.create(3, Departure.class), Time.parseTime("07:22:00")));

		schedule.addTransitLine(tLine2);
	}

	private void createPopulation() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		Population population = this.scenario.getPopulation();
		PopulationFactory pb = population.getFactory();

		TransitLine tLine1 = schedule.getTransitLines().get(Id.create(1, TransitLine.class));
		TransitRoute tRoute1 = tLine1.getRoutes().get(Id.create(1, TransitRoute.class));
		TransitLine tLine2 = schedule.getTransitLines().get(Id.create(2, TransitLine.class));
		TransitRoute tRoute2 = tLine1.getRoutes().get(Id.create(1, TransitRoute.class));

		TransitStopFacility stop1 = schedule.getFacilities().get(Id.create(1, TransitStopFacility.class));
		/*TransitStopFacility stop2 =*/ schedule.getFacilities().get(Id.create(2, TransitStopFacility.class));
		TransitStopFacility stop3 = schedule.getFacilities().get(Id.create(3, TransitStopFacility.class));
		TransitStopFacility stop4 = schedule.getFacilities().get(Id.create(4, TransitStopFacility.class));
		/*TransitStopFacility stop5 =*/ schedule.getFacilities().get(Id.create(5, TransitStopFacility.class));
		TransitStopFacility stop6 = schedule.getFacilities().get(Id.create(6, TransitStopFacility.class));

		{ // person 1
			Person person = pb.createPerson(Id.create(1, Person.class));
			Plan plan = pb.createPlan();
			Activity act1 = pb.createActivityFromLinkId("home", Id.create(3, Link.class));
			act1.setEndTime(Time.parseTime("07:01:00"));
			Leg leg1 = pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, tRoute1, stop3));
			Activity act2 = pb.createActivityFromLinkId("pt interaction", Id.create(3, Link.class));
			act2.setEndTime(Time.parseTime("07:01:00"));
			Leg leg2 = pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop3, tLine2, tRoute2, stop6));
			Activity act3 = pb.createActivityFromLinkId("pt interaction", Id.create(6, Link.class));

			plan.addActivity(act1);
			plan.addLeg(leg1);
			plan.addActivity(act2);
			plan.addLeg(leg2);
			plan.addActivity(act3);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			population.addPerson(person);
		}

		{ // person 2
			Person person = pb.createPerson(Id.create(2, Person.class));
			Plan plan = pb.createPlan();
			Activity act1 = pb.createActivityFromLinkId("home", Id.create(3, Link.class));
			act1.setEndTime(Time.parseTime("07:06:00"));
			Leg leg1 = pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, tRoute1, stop3));
			Activity act2 = pb.createActivityFromLinkId("pt interaction", Id.create(3, Link.class));
			act2.setEndTime(Time.parseTime("07:06:00"));
			Leg leg2 = pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop3, tLine2, tRoute2, stop6));
			Activity act3 = pb.createActivityFromLinkId("pt interaction", Id.create(6, Link.class));

			plan.addActivity(act1);
			plan.addLeg(leg1);
			plan.addActivity(act2);
			plan.addLeg(leg2);
			plan.addActivity(act3);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			population.addPerson(person);
		}

		{ // person 3
			Person person = pb.createPerson(Id.create(3, Person.class));
			Plan plan = pb.createPlan();
			Activity act1 = pb.createActivityFromLinkId("home", Id.create(3, Link.class));
			act1.setEndTime(Time.parseTime("07:11:00"));
			Leg leg1 = pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, tRoute1, stop4));
			Activity act2 = pb.createActivityFromLinkId("pt interaction", Id.create(3, Link.class));
			act2.setEndTime(Time.parseTime("07:11:00"));
			Leg leg2 = pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop4, tLine2, tRoute2, stop6));
			Activity act3 = pb.createActivityFromLinkId("pt interaction", Id.create(6, Link.class));

			plan.addActivity(act1);
			plan.addLeg(leg1);
			plan.addActivity(act2);
			plan.addLeg(leg2);
			plan.addActivity(act3);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			population.addPerson(person);
		}
	}

	private void createTransitVehicles() {
		new CreateVehiclesForSchedule(this.scenario.getTransitSchedule(), this.scenario.getTransitVehicles()).run();
	}

	private void runSim() {
		EventsManager events = EventsUtils.createEventsManager();
		VehicleTracker vehTracker = new VehicleTracker();
		events.addHandler(vehTracker);
		TransitRoute route1 = this.scenario.getTransitSchedule().getTransitLines().get(Id.create(1, TransitLine.class)).getRoutes().get(Id.create(1, TransitRoute.class));
		TransitRoute route2 = this.scenario.getTransitSchedule().getTransitLines().get(Id.create(2, TransitLine.class)).getRoutes().get(Id.create(1, TransitRoute.class));
		RouteOccupancy analysis1 = new RouteOccupancy(route1, vehTracker);
		RouteOccupancy analysis2 = new RouteOccupancy(route2, vehTracker);
		events.addHandler(analysis1);
		events.addHandler(analysis2);
		
		MobsimTimer mobsimTimer = new MobsimTimer(scenario.getConfig());
		AgentCounter agentCounter = new AgentCounterImpl();
		ActiveQSimBridge activeQSimBridge = new ActiveQSimBridge();

		QSim sim = QSimUtils.createDefaultQSim(this.scenario, events, mobsimTimer, agentCounter, activeQSimBridge);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, sim, mobsimTimer);
		OTFClientLive.run(scenario.getConfig(), server);
		sim.run();

		System.out.println("stop\t#exitleaving\t#enter\t#inVehicle");
		int inVehicle = 0;
		for (TransitRouteStop stop : route1.getStops()) {
			Id<TransitStopFacility> stopId = stop.getStopFacility().getId();
			int enter = analysis1.getNumberOfEnteringPassengers(stopId);
			int leave = analysis1.getNumberOfLeavingPassengers(stopId);
			inVehicle = inVehicle + enter - leave;
			System.out.println(stopId + "\t" + leave + "\t" + enter + "\t" + inVehicle);
		}

		System.out.println("stop\t#exitleaving\t#enter\t#inVehicle");
		inVehicle = 0;
		for (TransitRouteStop stop : route2.getStops()) {
			Id<TransitStopFacility> stopId = stop.getStopFacility().getId();
			int enter = analysis2.getNumberOfEnteringPassengers(stopId);
			int leave = analysis2.getNumberOfLeavingPassengers(stopId);
			inVehicle = inVehicle + enter - leave;
			System.out.println(stopId + "\t" + leave + "\t" + enter + "\t" + inVehicle);
		}
	}

	public void run() {
		prepareConfig();
		createNetwork();
		createTransitSchedule();
		createPopulation();
		createTransitVehicles();
		runSim();
	}

	public static void main(final String[] args) {
		new TwoLinesDemo().run();
	}

}
