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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.pt.SimpleTransitStopHandlerFactory;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.mrieser.pt.analysis.TransitRouteAccessEgressAnalysis;
import playground.mrieser.pt.analysis.VehicleTracker;

import java.util.ArrayList;
import java.util.List;

public class BlockingStopDemo {

	private static final int nOfLinks = 13;
	private static final int nOfStops = nOfLinks-3;
	private static final int nOfCars = 20;
	private static final int carsHeading = 30;
	private static final double startTime = 7.0 * 3600;
	private static final double busDeparture = 7.0 * 3600 + 3 * 60 + 15;

	private final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	private QSim sim = null;

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.scenario().setUseVehicles(true);
		config.transit().setUseTransit(true);
		config.qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
		config.qsim().setEndTime(24.0*3600);
	}

	private void createNetwork() {
		Network network = this.scenario.getNetwork();
//		network.setCapacityPeriod(3600.0);
		Node[] nodes = new Node[nOfLinks * 2 + 2];
		for (int i = 0; i <= nOfLinks; i++) {
			nodes[i] = network.getFactory().createNode(Id.create(i, Node.class), new Coord((double) (i * 500), (double) 0));
			network.addNode(nodes[i]);
			nodes[i+nOfLinks+1] = network.getFactory().createNode(Id.create(i+nOfLinks+1, Node.class), new Coord((double) (i * 500), (double) 500));
			network.addNode(nodes[i+nOfLinks+1]);
		}
		for (int i = 0; i < nOfLinks; i++) {
			Link link = network.getFactory().createLink(Id.create(i, Link.class), nodes[i], nodes[i+1]);
			link.setLength(500.0);
			link.setFreespeed(10.0);
			link.setCapacity(1000.0);
			link.setNumberOfLanes(1);
			network.addLink(link);
			link = network.getFactory().createLink(Id.create(i+nOfLinks, Link.class), nodes[i+nOfLinks+1], nodes[i+nOfLinks+2]);
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
			stops[i] = builder.createTransitStopFacility(Id.create(i, TransitStopFacility.class), new Coord((double) (1000 + i * 500), (double) 0), false);
			stops[i].setLinkId(Id.create(i+1, Link.class));
			schedule.addStopFacility(stops[i]);
			stopList.add(builder.createTransitRouteStop(stops[i], 100 + i*70, 120 + i*70));
		}

		Link startLink = this.scenario.getNetwork().getLinks().get(Id.create(0, Link.class));
		Link endLink = this.scenario.getNetwork().getLinks().get(Id.create(nOfLinks-1, Link.class));
		NetworkRoute networkRoute = (NetworkRoute) ((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).createRoute(TransportMode.car, startLink.getId(), endLink.getId());
		ArrayList<Id<Link>> linkIdList = new ArrayList<Id<Link>>(nOfLinks);
		for (int i = 1; i < nOfLinks-1; i++) {
			linkIdList.add(Id.create(i, Link.class));
		}
		networkRoute.setLinkIds(startLink.getId(), linkIdList, endLink.getId());
		TransitRoute tRoute1 = builder.createTransitRoute(Id.create(1, TransitRoute.class), networkRoute, stopList, "bus");

		TransitLine tLine1 = builder.createTransitLine(Id.create(1, TransitLine.class));
		tLine1.addRoute(tRoute1);
		schedule.addTransitLine(tLine1);

		Departure dep1 = builder.createDeparture(Id.create(1, Departure.class), busDeparture);
		dep1.setVehicleId(Id.create("tr_1", Vehicle.class));
		tRoute1.addDeparture(dep1);

		// line 2
		stopList = new ArrayList<TransitRouteStop>(nOfStops);
		for (int i = 0; i < nOfStops; i++) {
			stops[i+nOfStops] = builder.createTransitStopFacility(Id.create(i+nOfStops, TransitStopFacility.class), new Coord((double) (1000 + i * 500), (double) 500), true);
			stops[i+nOfStops].setLinkId(Id.create(i+1+nOfLinks, Link.class));
			schedule.addStopFacility(stops[i+nOfStops]);
			stopList.add(builder.createTransitRouteStop(stops[i+nOfStops], 100 + i*70, 120 + i*70));
		}

		startLink = this.scenario.getNetwork().getLinks().get(Id.create(nOfLinks, Link.class));
		endLink = this.scenario.getNetwork().getLinks().get(Id.create(2*nOfLinks-1, Link.class));
		networkRoute = (NetworkRoute) ((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).createRoute(TransportMode.car, startLink.getId(), endLink.getId());
		linkIdList = new ArrayList<Id<Link>>(nOfLinks);
		for (int i = nOfLinks+1; i < (2*nOfLinks - 1); i++) {
			linkIdList.add(Id.create(i, Link.class));
		}
		networkRoute.setLinkIds(startLink.getId(), linkIdList, endLink.getId());
		TransitRoute tRoute2 = builder.createTransitRoute(Id.create(2, TransitRoute.class), networkRoute, stopList, "bus");

		TransitLine tLine2 = builder.createTransitLine(Id.create(2, TransitLine.class));
		tLine2.addRoute(tRoute2);
		schedule.addTransitLine(tLine2);

		Departure dep2 = builder.createDeparture(Id.create(2, Departure.class), busDeparture);
		dep2.setVehicleId(Id.create("tr_2", Vehicle.class));
		tRoute2.addDeparture(dep2);

//		try {
//			new TransitScheduleWriterV1(this.schedule).write("blockingStopSchedule.xml");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	private void createVehicles() {
		Vehicles vehicles = this.scenario.getTransitVehicles();
		VehiclesFactory vb = vehicles.getFactory();
		VehicleType vehicleType = vb.createVehicleType(Id.create("transitVehicleType", VehicleType.class));
		VehicleCapacity capacity = vb.createVehicleCapacity();
		capacity.setSeats(Integer.valueOf(101));
		capacity.setStandingRoom(Integer.valueOf(0));
		vehicleType.setCapacity(capacity);
		vehicles.addVehicleType(vehicleType);
		Id<Vehicle> id = Id.create("tr_1", Vehicle.class);
		vehicles.addVehicle( vb.createVehicle(id, vehicleType));
		id = Id.create("tr_2", Vehicle.class);
		vehicles.addVehicle( vb.createVehicle(id, vehicleType));
	}

	private void createPopulation() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		Population population = this.scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
//		TransitStopFacility[] stops = this.schedule.getFacilities().values().toArray(new TransitStopFacility[this.schedule.getFacilities().size()]);
		TransitLine tLine1 = schedule.getTransitLines().get(Id.create(1, TransitLine.class));
		TransitRoute tRoute1 = tLine1.getRoutes().get(Id.create(1, TransitRoute.class));
		TransitLine tLine2 = schedule.getTransitLines().get(Id.create(2, TransitLine.class));
		TransitRoute tRoute2 = tLine2.getRoutes().get(Id.create(2, TransitRoute.class));

		// bus-passengers line 1
		for (int i = 1; i < nOfStops; i++) {
			PersonImpl person = (PersonImpl) pb.createPerson(Id.create(Integer.toString(-i), Person.class));
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", Id.create(i, Link.class));
			act1.setEndTime(startTime + i*60);
			LegImpl leg = (LegImpl) pb.createLeg(TransportMode.pt);
			leg.setRoute(new ExperimentalTransitRoute(schedule.getFacilities().get(Id.create(i-1, TransitStopFacility.class)), tLine1, tRoute1, schedule.getFacilities().get(Id.create(nOfStops-1, TransitStopFacility.class))));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("work", Id.create(nOfLinks-1, Link.class));

			population.addPerson(person);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			plan.addActivity(act1);
			plan.addLeg(leg);
			plan.addActivity(act2);
		}

		// bus-passengers line 2
		for (int i = 1; i < nOfStops; i++) {
			PersonImpl person = (PersonImpl) pb.createPerson(Id.create(Integer.toString(-i-nOfStops), Person.class));
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", Id.create(nOfLinks+i, Link.class));
			act1.setEndTime(startTime + i*60);
			LegImpl leg = (LegImpl) pb.createLeg(TransportMode.pt);
			leg.setRoute(new ExperimentalTransitRoute(schedule.getFacilities().get(Id.create(nOfStops+i-1, TransitStopFacility.class)), tLine2, tRoute2, schedule.getFacilities().get(Id.create(2*nOfStops-1, TransitStopFacility.class))));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("work", Id.create(2*nOfLinks-1, Link.class));

			population.addPerson(person);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			plan.addActivity(act1);
			plan.addLeg(leg);
			plan.addActivity(act2);
		}

		// car-drivers
		NetworkRoute carRoute1 = (NetworkRoute) ((PopulationFactoryImpl) population.getFactory()).createRoute(TransportMode.car, Id.create(0, Link.class), Id.create(nOfLinks-1, Link.class));
		NetworkRoute carRoute2 = (NetworkRoute) ((PopulationFactoryImpl) population.getFactory()).createRoute(TransportMode.car, Id.create(nOfLinks, Link.class), Id.create(2*nOfLinks-1, Link.class));
		List<Id<Link>> linkIds1 = new ArrayList<Id<Link>>(nOfLinks-2);
		List<Id<Link>> linkIds2 = new ArrayList<Id<Link>>(nOfLinks-2);
		for (int i = 1; i<nOfLinks-1; i++) {
			linkIds1.add(Id.create(i, Link.class));
			linkIds2.add(Id.create(i+nOfLinks, Link.class));
		}
		carRoute1.setLinkIds(Id.create(0, Link.class), linkIds1, Id.create(nOfLinks-1, Link.class));
		carRoute2.setLinkIds(Id.create(nOfLinks, Link.class), linkIds2, Id.create(2*nOfLinks-1, Link.class));
		for (int i = 0; i < nOfCars; i++) {
			PersonImpl person = (PersonImpl) pb.createPerson(Id.create(Integer.toString(i), Person.class));
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1a = (ActivityImpl) pb.createActivityFromLinkId("home", Id.create(0, Link.class));
			act1a.setEndTime(startTime + i*carsHeading);
			LegImpl leg1 = (LegImpl) pb.createLeg(TransportMode.car);
			leg1.setRoute(carRoute1);
			ActivityImpl act1b = (ActivityImpl) pb.createActivityFromLinkId("work", Id.create(4, Link.class));

			population.addPerson(person);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			plan.addActivity(act1a);
			plan.addLeg(leg1);
			plan.addActivity(act1b);

			PersonImpl person2 = (PersonImpl) pb.createPerson(Id.create(Integer.toString(i+nOfCars), Person.class));
			PlanImpl plan2 = (PlanImpl) pb.createPlan();
			ActivityImpl act2a = (ActivityImpl) pb.createActivityFromLinkId("home", Id.create(nOfLinks, Link.class));
			act2a.setEndTime(startTime + i*carsHeading);
			LegImpl leg2 = (LegImpl) pb.createLeg(TransportMode.car);
			leg2.setRoute(carRoute2);
			ActivityImpl act2b = (ActivityImpl) pb.createActivityFromLinkId("work", Id.create(nOfLinks-1, Link.class));

			population.addPerson(person2);
			person2.addPlan(plan2);
			person2.setSelectedPlan(plan2);
			plan2.addActivity(act2a);
			plan2.addLeg(leg2);
			plan2.addActivity(act2b);
		}

	}

	private void runSim() {
		EventsManager events = EventsUtils.createEventsManager();
		Network network = this.scenario.getNetwork();

		VehicleTracker vehTracker = new VehicleTracker();
		events.addHandler(vehTracker);
		TransitRouteAccessEgressAnalysis analysis = new TransitRouteAccessEgressAnalysis(this.scenario.getTransitSchedule().getTransitLines().get(Id.create(1, TransitLine.class)).getRoutes().get(Id.create(1, TransitRoute.class)), vehTracker);
		events.addHandler(analysis);
		TravelTimeCalculator ttc = new TravelTimeCalculator(this.scenario.getNetwork(), 120, 7*3600+1800, new TravelTimeCalculatorConfigGroup());
		events.addHandler(ttc);

		this.sim = (QSim) QSimUtils.createDefaultQSim(this.scenario, events);
		this.sim.getTransitEngine().setTransitStopHandlerFactory(new SimpleTransitStopHandlerFactory());

		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(this.scenario.getConfig(), this.scenario, events, sim);
		OTFClientLive.run(this.scenario.getConfig(), server);
		
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
				System.out.print("\t" + ttc.getLinkTravelTimes().getLinkTravelTime(network.getLinks().get(Id.create(i, Link.class)), time, null, null));
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
				System.out.print("\t" + ttc.getLinkTravelTimes().getLinkTravelTime(network.getLinks().get(Id.create(i, Link.class)), time, null, null));
			}
			System.out.println();
		}
	}

	public void run() {
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
