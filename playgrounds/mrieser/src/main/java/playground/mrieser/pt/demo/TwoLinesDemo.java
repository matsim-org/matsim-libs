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

package playground.mrieser.pt.demo;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.mrieser.pt.analysis.RouteOccupancy;
import playground.mrieser.pt.analysis.VehicleTracker;

import java.util.ArrayList;
import java.util.Collections;

public class TwoLinesDemo {

	private final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.transit().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
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
		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), this.scenario.createCoord(-2000, 0));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), this.scenario.createCoord(-2000, 1000));
		Node node3 = network.createAndAddNode(Id.create(3, Node.class), this.scenario.createCoord(-1000, 0));
		Node node4 = network.createAndAddNode(Id.create(4, Node.class), this.scenario.createCoord(-1000, 1000));
		Node node5 = network.createAndAddNode(Id.create(5, Node.class), this.scenario.createCoord(0, 0));
		Node node6 = network.createAndAddNode(Id.create(6, Node.class), this.scenario.createCoord(0, 1000));
		Node node7 = network.createAndAddNode(Id.create(7, Node.class), this.scenario.createCoord(500, 500));
		Node node8 = network.createAndAddNode(Id.create(8, Node.class), this.scenario.createCoord(1500, 500));
		Node node9 = network.createAndAddNode(Id.create(9, Node.class), this.scenario.createCoord(2500, 500));
		Node node10 = network.createAndAddNode(Id.create(10, Node.class), this.scenario.createCoord(3500, 500));
		Node node11 = network.createAndAddNode(Id.create(11, Node.class), this.scenario.createCoord(4000, 0));
		Node node12 = network.createAndAddNode(Id.create(12, Node.class), this.scenario.createCoord(4000, 1000));
		Node node13 = network.createAndAddNode(Id.create(13, Node.class), this.scenario.createCoord(5000, 0));
		Node node14 = network.createAndAddNode(Id.create(14, Node.class), this.scenario.createCoord(5000, 1000));

		network.createAndAddLink(Id.create(1, Link.class), node1, node3, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(2, Link.class), node2, node4, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(3, Link.class), node3, node5, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(4, Link.class), node4, node6, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(5, Link.class), node5, node7, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(6, Link.class), node6, node7, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(7, Link.class), node7, node8, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(8, Link.class), node8, node9, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(9, Link.class), node9, node10, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(10, Link.class), node10, node12, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(11, Link.class), node10, node11, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(12, Link.class), node12, node14, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(13, Link.class), node11, node13, 1000.0, 10.0, 3600.0, 1);
	}

	private void createTransitSchedule() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create(1, TransitStopFacility.class), this.scenario.createCoord(-100, -50), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create(2, TransitStopFacility.class), this.scenario.createCoord(-100, 850), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create(3, TransitStopFacility.class), this.scenario.createCoord(1400, 450), false);
		TransitStopFacility stop4 = builder.createTransitStopFacility(Id.create(4, TransitStopFacility.class), this.scenario.createCoord(3400, 450), false);
		TransitStopFacility stop5 = builder.createTransitStopFacility(Id.create(5, TransitStopFacility.class), this.scenario.createCoord(3900, 50), false);
		TransitStopFacility stop6 = builder.createTransitStopFacility(Id.create(6, TransitStopFacility.class), this.scenario.createCoord(3900, 850), false);

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
		NetworkRoute networkRoute = new LinkNetworkRouteImpl(link1.getId(), link13.getId());
		ArrayList<Id<Link>> linkIdList = new ArrayList<Id<Link>>(6);
		Collections.addAll(linkIdList, link3.getId(), link5.getId(), link7.getId(), link8.getId(), link9.getId(), link11.getId());
		networkRoute.setLinkIds(link1.getId(), linkIdList, link13.getId());
		ArrayList<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>(4);
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
		networkRoute = new LinkNetworkRouteImpl(link2.getId(), link12.getId());
		linkIdList = new ArrayList<Id<Link>>(6);
		Collections.addAll(linkIdList, link4.getId(), link6.getId(), link7.getId(), link8.getId(), link9.getId(), link10.getId());
		networkRoute.setLinkIds(link2.getId(), linkIdList, link12.getId());
		stopList = new ArrayList<TransitRouteStop>(4);
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
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", Id.create(3, Link.class));
			act1.setEndTime(Time.parseTime("07:01:00"));
			LegImpl leg1 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, tRoute1, stop3));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", Id.create(3, Link.class));
			act2.setEndTime(Time.parseTime("07:01:00"));
			LegImpl leg2 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop3, tLine2, tRoute2, stop6));
			ActivityImpl act3 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", Id.create(6, Link.class));

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
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", Id.create(3, Link.class));
			act1.setEndTime(Time.parseTime("07:06:00"));
			LegImpl leg1 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, tRoute1, stop3));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", Id.create(3, Link.class));
			act2.setEndTime(Time.parseTime("07:06:00"));
			LegImpl leg2 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop3, tLine2, tRoute2, stop6));
			ActivityImpl act3 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", Id.create(6, Link.class));

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
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", Id.create(3, Link.class));
			act1.setEndTime(Time.parseTime("07:11:00"));
			LegImpl leg1 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, tRoute1, stop4));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", Id.create(3, Link.class));
			act2.setEndTime(Time.parseTime("07:11:00"));
			LegImpl leg2 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop4, tLine2, tRoute2, stop6));
			ActivityImpl act3 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", Id.create(6, Link.class));

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

		QSim sim = (QSim) QSimUtils.createDefaultQSim(this.scenario, events);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, sim);
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
