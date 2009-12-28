/* *********************************************************************** *
 * project: org.matsim.*
 * BlockingStopDemo.java
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
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.queuesim.TransitQueueSimulation;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.BasicVehicleCapacity;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicles;
import org.matsim.vehicles.VehiclesFactory;

import playground.mrieser.OTFDemo;
import playground.mrieser.pt.analysis.TransitRouteAccessEgressAnalysis;
import playground.mrieser.pt.analysis.VehicleTracker;

public class BlockingStopDemo {

	private static final int nOfLinks = 13;
	private static final int nOfStops = nOfLinks-3;
	private static final int nOfCars = 20;
	private static final int carsHeading = 30;
	private static final double startTime = 7.0 * 3600;
	private static final double busDeparture = 7.0 * 3600 + 3 * 60 + 15;

	private final ScenarioImpl scenario = new ScenarioImpl();
	private final Id[] ids = new Id[nOfLinks * 2 + 2];

	private TransitQueueSimulation sim = null;

	private void createIds() {
		for (int i = 0; i < this.ids.length; i++) {
			this.ids[i] = this.scenario.createId(Integer.toString(i));
		}
	}

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.scenario().setUseVehicles(true);
		config.scenario().setUseTransit(true);
		config.simulation().setSnapshotStyle("queue");
		config.simulation().setEndTime(24.0*3600);
	}

	private void createNetwork() {
		NetworkImpl network = this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node[] nodes = new Node[nOfLinks * 2 + 2];
		for (int i = 0; i <= nOfLinks; i++) {
			nodes[i] = network.getFactory().createNode(this.ids[i], this.scenario.createCoord(i * 500, 0));
			network.addNode(nodes[i]);
			nodes[i+nOfLinks+1] = network.getFactory().createNode(this.ids[i+nOfLinks+1], this.scenario.createCoord(i * 500, 500));
			network.addNode(nodes[i+nOfLinks+1]);
		}
		for (int i = 0; i < nOfLinks; i++) {
			Link link = network.getFactory().createLink(this.ids[i], nodes[i].getId(), nodes[i+1].getId());
			link.setLength(500.0);
			link.setFreespeed(10.0);
			link.setCapacity(1000.0);
			link.setNumberOfLanes(1);
			network.addLink(link);
			link = network.getFactory().createLink(this.ids[i+nOfLinks], nodes[i+nOfLinks+1].getId(), nodes[i+nOfLinks+2].getId());
			link.setLength(500.0);
			link.setFreespeed(10.0);
			link.setCapacity(1000.0);
			link.setNumberOfLanes(1);
			network.addLink(link);
		}
	}

	private void createTransitSchedule() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();

		TransitStopFacility[] stops = new TransitStopFacility[nOfStops * 2];
		ArrayList<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>(nOfStops);

		// line 1
		for (int i = 0; i < nOfStops; i++) {
			stops[i] = builder.createTransitStopFacility(this.ids[i], this.scenario.createCoord(1000 + i*500, 0), false);
			stops[i].setLink(this.scenario.getNetwork().getLinks().get(this.ids[i+1]));
			schedule.addStopFacility(stops[i]);
			stopList.add(builder.createTransitRouteStop(stops[i], 100 + i*70, 120 + i*70));
		}

		LinkImpl startLink = this.scenario.getNetwork().getLinks().get(this.ids[0]);
		LinkImpl endLink = this.scenario.getNetwork().getLinks().get(this.ids[nOfLinks-1]);
		NetworkRouteWRefs networkRoute = (NetworkRouteWRefs) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, startLink, endLink);
		ArrayList<Link> linkList = new ArrayList<Link>(nOfLinks);
		for (int i = 1; i < nOfLinks-1; i++) {
			linkList.add(this.scenario.getNetwork().getLinks().get(this.ids[i]));
		}
		networkRoute.setLinks(startLink, linkList, endLink);
		TransitRoute tRoute1 = builder.createTransitRoute(this.ids[1], networkRoute, stopList, TransportMode.bus);

		TransitLine tLine1 = builder.createTransitLine(this.ids[1]);
		tLine1.addRoute(tRoute1);
		schedule.addTransitLine(tLine1);

		Departure dep1 = builder.createDeparture(this.ids[1], busDeparture);
		dep1.setVehicleId(new IdImpl("tr_1"));
		tRoute1.addDeparture(dep1);

		// line 2
		stopList = new ArrayList<TransitRouteStop>(nOfStops);
		for (int i = 0; i < nOfStops; i++) {
			stops[i+nOfStops] = builder.createTransitStopFacility(this.ids[i+nOfStops], this.scenario.createCoord(1000 + i*500, 500), true);
			stops[i+nOfStops].setLink(this.scenario.getNetwork().getLinks().get(this.ids[i+1+nOfLinks]));
			schedule.addStopFacility(stops[i+nOfStops]);
			stopList.add(builder.createTransitRouteStop(stops[i+nOfStops], 100 + i*70, 120 + i*70));
		}

		startLink = this.scenario.getNetwork().getLinks().get(this.ids[nOfLinks]);
		endLink = this.scenario.getNetwork().getLinks().get(this.ids[2*nOfLinks-1]);
		networkRoute = (NetworkRouteWRefs) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, startLink, endLink);
		linkList = new ArrayList<Link>(nOfLinks);
		for (int i = nOfLinks+1; i < (2*nOfLinks - 1); i++) {
			linkList.add(this.scenario.getNetwork().getLinks().get(this.ids[i]));
		}
		networkRoute.setLinks(startLink, linkList, endLink);
		TransitRoute tRoute2 = builder.createTransitRoute(this.ids[2], networkRoute, stopList, TransportMode.bus);

		TransitLine tLine2 = builder.createTransitLine(this.ids[2]);
		tLine2.addRoute(tRoute2);
		schedule.addTransitLine(tLine2);

		Departure dep2 = builder.createDeparture(this.ids[2], busDeparture);
		dep2.setVehicleId(new IdImpl("tr_2"));
		tRoute2.addDeparture(dep2);

//		try {
//			new TransitScheduleWriterV1(this.schedule).write("blockingStopSchedule.xml");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	private void createVehicles() {
		BasicVehicles vehicles = this.scenario.getVehicles();
		VehiclesFactory vb = vehicles.getFactory();
		BasicVehicleType vehicleType = vb.createVehicleType(new IdImpl("transitVehicleType"));
		BasicVehicleCapacity capacity = vb.createVehicleCapacity();
		capacity.setSeats(Integer.valueOf(101));
		capacity.setStandingRoom(Integer.valueOf(0));
		vehicleType.setCapacity(capacity);
		Id id = new IdImpl("tr_1");
		vehicles.getVehicles().put(id, vb.createVehicle(id, vehicleType));
		id = new IdImpl("tr_2");
		vehicles.getVehicles().put(id, vb.createVehicle(id, vehicleType));
	}

	private void createPopulation() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		PopulationImpl population = this.scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
//		TransitStopFacility[] stops = this.schedule.getFacilities().values().toArray(new TransitStopFacility[this.schedule.getFacilities().size()]);
		TransitLine tLine1 = schedule.getTransitLines().get(this.ids[1]);
		TransitRoute tRoute1 = tLine1.getRoutes().get(this.ids[1]);
		TransitLine tLine2 = schedule.getTransitLines().get(this.ids[2]);
		TransitRoute tRoute2 = tLine2.getRoutes().get(this.ids[2]);

		// bus-passengers line 1
		for (int i = 1; i < nOfStops; i++) {
			PersonImpl person = (PersonImpl) pb.createPerson(this.scenario.createId(Integer.toString(-i)));
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", this.ids[i]);
			act1.setEndTime(startTime + i*60);
			LegImpl leg = (LegImpl) pb.createLeg(TransportMode.pt);
			leg.setRoute(new ExperimentalTransitRoute(schedule.getFacilities().get(this.ids[i-1]), tLine1, tRoute1, schedule.getFacilities().get(this.ids[nOfStops-1])));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("work", this.ids[nOfLinks-1]);

			population.addPerson(person);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			plan.addActivity(act1);
			plan.addLeg(leg);
			plan.addActivity(act2);
		}

		// bus-passengers line 2
		for (int i = 1; i < nOfStops; i++) {
			PersonImpl person = (PersonImpl) pb.createPerson(this.scenario.createId(Integer.toString(-i-nOfStops)));
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", this.ids[nOfLinks+i]);
			act1.setEndTime(startTime + i*60);
			LegImpl leg = (LegImpl) pb.createLeg(TransportMode.pt);
			leg.setRoute(new ExperimentalTransitRoute(schedule.getFacilities().get(this.ids[nOfStops+i-1]), tLine2, tRoute2, schedule.getFacilities().get(this.ids[2*nOfStops-1])));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("work", this.ids[2*nOfLinks-1]);

			population.addPerson(person);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			plan.addActivity(act1);
			plan.addLeg(leg);
			plan.addActivity(act2);
		}

		// car-drivers
		NetworkImpl network = this.scenario.getNetwork();
		NetworkRouteWRefs carRoute1 = (NetworkRouteWRefs) network.getFactory().createRoute(TransportMode.car, network.getLinks().get(this.ids[0]), network.getLinks().get(this.ids[nOfLinks-1]));
		NetworkRouteWRefs carRoute2 = (NetworkRouteWRefs) network.getFactory().createRoute(TransportMode.car, network.getLinks().get(this.ids[nOfLinks]), network.getLinks().get(this.ids[2*nOfLinks-1]));
		List<Link> links1 = new ArrayList<Link>(nOfLinks-2);
		List<Link> links2 = new ArrayList<Link>(nOfLinks-2);
		for (int i = 1; i<nOfLinks-1; i++) {
			links1.add(network.getLinks().get(this.ids[i]));
			links2.add(network.getLinks().get(this.ids[i+nOfLinks]));
		}
		carRoute1.setLinks(network.getLinks().get(this.ids[0]), links1, network.getLinks().get(this.ids[nOfLinks-1]));
		carRoute2.setLinks(network.getLinks().get(this.ids[nOfLinks]), links2, network.getLinks().get(this.ids[2*nOfLinks-1]));
		for (int i = 0; i < nOfCars; i++) {
			PersonImpl person = (PersonImpl) pb.createPerson(this.scenario.createId(Integer.toString(i)));
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1a = (ActivityImpl) pb.createActivityFromLinkId("home", this.ids[0]);
			act1a.setEndTime(startTime + i*carsHeading);
			LegImpl leg1 = (LegImpl) pb.createLeg(TransportMode.car);
			leg1.setRoute(carRoute1);
			ActivityImpl act1b = (ActivityImpl) pb.createActivityFromLinkId("work", this.ids[4]);

			population.addPerson(person);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			plan.addActivity(act1a);
			plan.addLeg(leg1);
			plan.addActivity(act1b);

			PersonImpl person2 = (PersonImpl) pb.createPerson(this.scenario.createId(Integer.toString(i+nOfCars)));
			PlanImpl plan2 = (PlanImpl) pb.createPlan();
			ActivityImpl act2a = (ActivityImpl) pb.createActivityFromLinkId("home", this.ids[nOfLinks]);
			act2a.setEndTime(startTime + i*carsHeading);
			LegImpl leg2 = (LegImpl) pb.createLeg(TransportMode.car);
			leg2.setRoute(carRoute2);
			ActivityImpl act2b = (ActivityImpl) pb.createActivityFromLinkId("work", this.ids[nOfLinks-1]);

			population.addPerson(person2);
			person2.addPlan(plan2);
			person2.setSelectedPlan(plan2);
			plan2.addActivity(act2a);
			plan2.addLeg(leg2);
			plan2.addActivity(act2b);
		}

	}

	private void runSim() {
		EventsManagerImpl events = new EventsManagerImpl();
		Network network = this.scenario.getNetwork();

		VehicleTracker vehTracker = new VehicleTracker();
		events.addHandler(vehTracker);
		TransitRouteAccessEgressAnalysis analysis = new TransitRouteAccessEgressAnalysis(this.scenario.getTransitSchedule().getTransitLines().get(this.ids[1]).getRoutes().get(this.ids[1]), vehTracker);
		events.addHandler(analysis);
		TravelTimeCalculator ttc = new TravelTimeCalculator(this.scenario.getNetwork(), 120, 7*3600+1800, new TravelTimeCalculatorConfigGroup());
		events.addHandler(ttc);

		this.sim = new TransitQueueSimulation(this.scenario, events);
		this.sim.startOTFServer("blocking_stop_demo");

		OTFDemo.ptConnect("blocking_stop_demo");

		this.sim.run();

		System.out.println("TransitRouteAccessEgressAnalysis:");
		analysis.printStats();

		System.out.println("TravelTimes:");
		for (int i = 13; i < 26; i++) {
			System.out.print("\tlink " + i);
		}
		System.out.println();
		for (int time = 7*3600; time < (7*3600 + 15*60); time += 120) {
			System.out.print(Time.writeTime(time));
			for (int i = 13; i < 26; i++) {
				System.out.print("\t" + ttc.getLinkTravelTime(network.getLinks().get(this.ids[i]), time));
			}
			System.out.println();
		}
		System.out.println();
		for (int i = 0; i < 13; i++) {
			System.out.print("\tlink " + i);
		}
		System.out.println();
		for (int time = 7*3600; time < (7*3600 + 15*60); time += 120) {
			System.out.print(Time.writeTime(time));
			for (int i = 0; i < 13; i++) {
				System.out.print("\t" + ttc.getLinkTravelTime(network.getLinks().get(this.ids[i]), time));
			}
			System.out.println();
		}
	}

	public void run() {
		createIds();
		prepareConfig();
		createNetwork();
		createTransitSchedule();
		createPopulation();
		createVehicles();
		runSim();
	}

	public static void main(final String[] args) {
		new BlockingStopDemo().run();
	}

}
