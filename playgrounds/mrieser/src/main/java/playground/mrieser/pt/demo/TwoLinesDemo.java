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

import java.util.ArrayList;
import java.util.Collections;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;

import playground.mrieser.pt.analysis.RouteOccupancy;
import playground.mrieser.pt.analysis.VehicleTracker;

public class TwoLinesDemo {

	private final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private final Id[] ids = new Id[15];

	private void createIds() {
		for (int i = 0; i < this.ids.length; i++) {
			this.ids[i] = this.scenario.createId(Integer.toString(i));
		}
	}

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.addQSimConfigGroup(new QSimConfigGroup());
		config.getQSimConfigGroup().setSnapshotStyle("queue");
		config.getQSimConfigGroup().setEndTime(24.0*3600);
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
		NetworkImpl network = this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = network.createAndAddNode(this.ids[1], this.scenario.createCoord(-2000, 0));
		Node node2 = network.createAndAddNode(this.ids[2], this.scenario.createCoord(-2000, 1000));
		Node node3 = network.createAndAddNode(this.ids[3], this.scenario.createCoord(-1000, 0));
		Node node4 = network.createAndAddNode(this.ids[4], this.scenario.createCoord(-1000, 1000));
		Node node5 = network.createAndAddNode(this.ids[5], this.scenario.createCoord(0, 0));
		Node node6 = network.createAndAddNode(this.ids[6], this.scenario.createCoord(0, 1000));
		Node node7 = network.createAndAddNode(this.ids[7], this.scenario.createCoord(500, 500));
		Node node8 = network.createAndAddNode(this.ids[8], this.scenario.createCoord(1500, 500));
		Node node9 = network.createAndAddNode(this.ids[9], this.scenario.createCoord(2500, 500));
		Node node10 = network.createAndAddNode(this.ids[10], this.scenario.createCoord(3500, 500));
		Node node11 = network.createAndAddNode(this.ids[11], this.scenario.createCoord(4000, 0));
		Node node12 = network.createAndAddNode(this.ids[12], this.scenario.createCoord(4000, 1000));
		Node node13 = network.createAndAddNode(this.ids[13], this.scenario.createCoord(5000, 0));
		Node node14 = network.createAndAddNode(this.ids[14], this.scenario.createCoord(5000, 1000));

		network.createAndAddLink(this.ids[1], node1, node3, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[2], node2, node4, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[3], node3, node5, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[4], node4, node6, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[5], node5, node7, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[6], node6, node7, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[7], node7, node8, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[8], node8, node9, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[9], node9, node10, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[10], node10, node12, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[11], node10, node11, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[12], node12, node14, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[13], node11, node13, 1000.0, 10.0, 3600.0, 1);
	}

	private void createTransitSchedule() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();
		TransitStopFacility stop1 = builder.createTransitStopFacility(this.ids[1], this.scenario.createCoord(-100, -50), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(this.ids[2], this.scenario.createCoord(-100, 850), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(this.ids[3], this.scenario.createCoord(1400, 450), false);
		TransitStopFacility stop4 = builder.createTransitStopFacility(this.ids[4], this.scenario.createCoord(3400, 450), false);
		TransitStopFacility stop5 = builder.createTransitStopFacility(this.ids[5], this.scenario.createCoord(3900, 50), false);
		TransitStopFacility stop6 = builder.createTransitStopFacility(this.ids[6], this.scenario.createCoord(3900, 850), false);

		Link link1 = this.scenario.getNetwork().getLinks().get(this.ids[1]);
		Link link2 = this.scenario.getNetwork().getLinks().get(this.ids[2]);
		Link link3 = this.scenario.getNetwork().getLinks().get(this.ids[3]);
		Link link4 = this.scenario.getNetwork().getLinks().get(this.ids[4]);
		Link link5 = this.scenario.getNetwork().getLinks().get(this.ids[5]);
		Link link6 = this.scenario.getNetwork().getLinks().get(this.ids[6]);
		Link link7 = this.scenario.getNetwork().getLinks().get(this.ids[7]);
		Link link8 = this.scenario.getNetwork().getLinks().get(this.ids[8]);
		Link link9 = this.scenario.getNetwork().getLinks().get(this.ids[9]);
		Link link10 = this.scenario.getNetwork().getLinks().get(this.ids[10]);
		Link link11 = this.scenario.getNetwork().getLinks().get(this.ids[11]);
		Link link12 = this.scenario.getNetwork().getLinks().get(this.ids[12]);
		Link link13 = this.scenario.getNetwork().getLinks().get(this.ids[13]);

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

		TransitLine tLine1 = builder.createTransitLine(this.ids[1]);
		NetworkRoute networkRoute = (NetworkRoute) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, link1.getId(), link13.getId());
		ArrayList<Id> linkIdList = new ArrayList<Id>(6);
		Collections.addAll(linkIdList, link3.getId(), link5.getId(), link7.getId(), link8.getId(), link9.getId(), link11.getId());
		networkRoute.setLinkIds(link1.getId(), linkIdList, link13.getId());
		ArrayList<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>(4);
		stopList.add(builder.createTransitRouteStop(stop1, 0, 0));
		stopList.add(builder.createTransitRouteStop(stop3, 90, 100));
		stopList.add(builder.createTransitRouteStop(stop4, 290, 300));
		stopList.add(builder.createTransitRouteStop(stop5, 390, Time.UNDEFINED_TIME));
		TransitRoute tRoute1 = builder.createTransitRoute(this.ids[1], networkRoute, stopList, "bus");
		tLine1.addRoute(tRoute1);

		tRoute1.addDeparture(builder.createDeparture(this.ids[1], Time.parseTime("07:00:00")));
		tRoute1.addDeparture(builder.createDeparture(this.ids[2], Time.parseTime("07:05:00")));
		tRoute1.addDeparture(builder.createDeparture(this.ids[3], Time.parseTime("07:10:00")));
		tRoute1.addDeparture(builder.createDeparture(this.ids[4], Time.parseTime("07:15:00")));
		tRoute1.addDeparture(builder.createDeparture(this.ids[5], Time.parseTime("07:20:00")));
		tRoute1.addDeparture(builder.createDeparture(this.ids[6], Time.parseTime("07:25:00")));

		schedule.addTransitLine(tLine1);

		TransitLine tLine2 = builder.createTransitLine(this.ids[2]);
		networkRoute = (NetworkRoute) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, link2.getId(), link12.getId());
		linkIdList = new ArrayList<Id>(6);
		Collections.addAll(linkIdList, link4.getId(), link6.getId(), link7.getId(), link8.getId(), link9.getId(), link10.getId());
		networkRoute.setLinkIds(link2.getId(), linkIdList, link12.getId());
		stopList = new ArrayList<TransitRouteStop>(4);
		stopList.add(builder.createTransitRouteStop(stop2, 0, 0));
		stopList.add(builder.createTransitRouteStop(stop3, 90, 100));
		stopList.add(builder.createTransitRouteStop(stop4, 290, 300));
		stopList.add(builder.createTransitRouteStop(stop6, 390, Time.UNDEFINED_TIME));
		TransitRoute tRoute2 = builder.createTransitRoute(this.ids[1], networkRoute, stopList, "bus");
		tLine2.addRoute(tRoute2);

		tRoute2.addDeparture(builder.createDeparture(this.ids[1], Time.parseTime("07:02:00")));
		tRoute2.addDeparture(builder.createDeparture(this.ids[2], Time.parseTime("07:12:00")));
		tRoute2.addDeparture(builder.createDeparture(this.ids[3], Time.parseTime("07:22:00")));

		schedule.addTransitLine(tLine2);
	}

	private void createPopulation() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		Population population = this.scenario.getPopulation();
		PopulationFactory pb = population.getFactory();

		TransitLine tLine1 = schedule.getTransitLines().get(this.ids[1]);
		TransitRoute tRoute1 = tLine1.getRoutes().get(this.ids[1]);
		TransitLine tLine2 = schedule.getTransitLines().get(this.ids[2]);
		TransitRoute tRoute2 = tLine1.getRoutes().get(this.ids[1]);

		TransitStopFacility stop1 = schedule.getFacilities().get(this.ids[1]);
		/*TransitStopFacility stop2 =*/ schedule.getFacilities().get(this.ids[2]);
		TransitStopFacility stop3 = schedule.getFacilities().get(this.ids[3]);
		TransitStopFacility stop4 = schedule.getFacilities().get(this.ids[4]);
		/*TransitStopFacility stop5 =*/ schedule.getFacilities().get(this.ids[5]);
		TransitStopFacility stop6 = schedule.getFacilities().get(this.ids[6]);

		{ // person 1
			PersonImpl person = (PersonImpl) pb.createPerson(this.ids[1]);
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", this.ids[3]);
			act1.setEndTime(Time.parseTime("07:01:00"));
			LegImpl leg1 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, tRoute1, stop3));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", this.ids[3]);
			act2.setEndTime(Time.parseTime("07:01:00"));
			LegImpl leg2 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop3, tLine2, tRoute2, stop6));
			ActivityImpl act3 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", this.ids[6]);

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
			PersonImpl person = (PersonImpl) pb.createPerson(this.ids[2]);
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", this.ids[3]);
			act1.setEndTime(Time.parseTime("07:06:00"));
			LegImpl leg1 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, tRoute1, stop3));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", this.ids[3]);
			act2.setEndTime(Time.parseTime("07:06:00"));
			LegImpl leg2 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop3, tLine2, tRoute2, stop6));
			ActivityImpl act3 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", this.ids[6]);

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
			PersonImpl person = (PersonImpl) pb.createPerson(this.ids[3]);
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", this.ids[3]);
			act1.setEndTime(Time.parseTime("07:11:00"));
			LegImpl leg1 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, tRoute1, stop4));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", this.ids[3]);
			act2.setEndTime(Time.parseTime("07:11:00"));
			LegImpl leg2 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop4, tLine2, tRoute2, stop6));
			ActivityImpl act3 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", this.ids[6]);

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
		new CreateVehiclesForSchedule(this.scenario.getTransitSchedule(), this.scenario.getVehicles()).run();
	}

	private void runSim() {
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		VehicleTracker vehTracker = new VehicleTracker();
		events.addHandler(vehTracker);
		TransitRoute route1 = this.scenario.getTransitSchedule().getTransitLines().get(this.ids[1]).getRoutes().get(this.ids[1]);
		TransitRoute route2 = this.scenario.getTransitSchedule().getTransitLines().get(this.ids[2]).getRoutes().get(this.ids[1]);
		RouteOccupancy analysis1 = new RouteOccupancy(route1, vehTracker);
		RouteOccupancy analysis2 = new RouteOccupancy(route2, vehTracker);
		events.addHandler(analysis1);
		events.addHandler(analysis2);

		QSim sim = new QSim(this.scenario, events);
		sim.addFeature(new OTFVisMobsimFeature(sim));
		sim.run();

		System.out.println("stop\t#exitleaving\t#enter\t#inVehicle");
		int inVehicle = 0;
		for (TransitRouteStop stop : route1.getStops()) {
			Id stopId = stop.getStopFacility().getId();
			int enter = analysis1.getNumberOfEnteringPassengers(stopId);
			int leave = analysis1.getNumberOfLeavingPassengers(stopId);
			inVehicle = inVehicle + enter - leave;
			System.out.println(stopId + "\t" + leave + "\t" + enter + "\t" + inVehicle);
		}

		System.out.println("stop\t#exitleaving\t#enter\t#inVehicle");
		inVehicle = 0;
		for (TransitRouteStop stop : route2.getStops()) {
			Id stopId = stop.getStopFacility().getId();
			int enter = analysis2.getNumberOfEnteringPassengers(stopId);
			int leave = analysis2.getNumberOfLeavingPassengers(stopId);
			inVehicle = inVehicle + enter - leave;
			System.out.println(stopId + "\t" + leave + "\t" + enter + "\t" + inVehicle);
		}
	}

	public void run() {
		createIds();
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
