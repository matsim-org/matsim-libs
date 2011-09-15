/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.demo;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.run.OTFVis;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.mrieser.pt.analysis.RouteTimeDiagram;
import playground.mrieser.pt.analysis.TransitRouteAccessEgressAnalysis;
import playground.mrieser.pt.analysis.VehicleTracker;

public class AccessEgressDemoSimple {

	private static final int nOfBuses = 10;
	private static final int nOfAgentsPerStopTrain = 1000;
	private static final int nOfAgentsPerStopBus = 100;
	private static final int agentIntervalTrain = 6;
	private static final int agentIntervalBus = 60;
	private static final int delayedBus = 9;
	private static final int heading = 5*60;
	private static final int delay = 60;
	private static final double departureTime = 7.0*3600;
	private static final boolean stopsBlockLane = true;

	private final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	public final Id[] ids = new Id[10];

	private void createIds() {
		for (int i = 0; i < this.ids.length; i++) {
			this.ids[i] = this.scenario.createId(Integer.toString(i));
		}
	}

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.addQSimConfigGroup(new QSimConfigGroup());
		config.scenario().setUseVehicles(true);
		config.scenario().setUseTransit(true);
		config.getQSimConfigGroup().setSnapshotStyle("queue");
		config.getQSimConfigGroup().setEndTime(24.0*3600);
	}

	private void createNetwork() {
		NetworkImpl network = this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		
		network.addNode(network.getFactory().createNode(new IdImpl("01"), this.scenario.createCoord(-500, 0)));
		network.addNode(network.getFactory().createNode(new IdImpl("10"), this.scenario.createCoord(0, 0)));
		network.addNode(network.getFactory().createNode(new IdImpl("11"), this.scenario.createCoord(500, 0)));
		network.addNode(network.getFactory().createNode(new IdImpl("12"), this.scenario.createCoord(1000, 0)));
		network.addNode(network.getFactory().createNode(new IdImpl("13"), this.scenario.createCoord(1500, 0)));
		network.addNode(network.getFactory().createNode(new IdImpl("14"), this.scenario.createCoord(2000, 0)));
		
		network.addNode(network.getFactory().createNode(new IdImpl("02"), this.scenario.createCoord(-500, -500)));
		network.addNode(network.getFactory().createNode(new IdImpl("20"), this.scenario.createCoord(0, -500)));
		network.addNode(network.getFactory().createNode(new IdImpl("21"), this.scenario.createCoord(500, -500)));
		network.addNode(network.getFactory().createNode(new IdImpl("22"), this.scenario.createCoord(1000, -500)));
		network.addNode(network.getFactory().createNode(new IdImpl("23"), this.scenario.createCoord(1500, -500)));
		network.addNode(network.getFactory().createNode(new IdImpl("24"), this.scenario.createCoord(2000, -500)));
		
		Link l;
		l = network.getFactory().createLink(new IdImpl("0110"), new IdImpl("01"), new IdImpl("10")); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(new IdImpl("1011"), new IdImpl("10"), new IdImpl("11")); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(new IdImpl("1112"), new IdImpl("11"), new IdImpl("12")); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(new IdImpl("1213"), new IdImpl("12"), new IdImpl("13")); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(new IdImpl("1314"), new IdImpl("13"), new IdImpl("14")); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		
		l = network.getFactory().createLink(new IdImpl("0220"), new IdImpl("02"), new IdImpl("20")); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(new IdImpl("2021"), new IdImpl("20"), new IdImpl("21")); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(new IdImpl("2122"), new IdImpl("21"), new IdImpl("22")); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(new IdImpl("2223"), new IdImpl("22"), new IdImpl("23")); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(new IdImpl("2324"), new IdImpl("23"), new IdImpl("24")); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		
	}

	private void createTransitSchedule() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();
		
		ArrayList<TransitRouteStop> stopListA = new ArrayList<TransitRouteStop>();
		ArrayList<TransitRouteStop> stopListB = new ArrayList<TransitRouteStop>();

		// create stops
		TransitStopFacility stopFac;
		TransitRouteStop stop;
		
		stopFac = builder.createTransitStopFacility(new IdImpl("11"), this.scenario.createCoord(500, 0), stopsBlockLane); stopFac.setLinkId(new IdImpl("1011")); schedule.addStopFacility(stopFac);
		stop = builder.createTransitRouteStop(stopFac, 0, 10); stopListA.add(stop);		
		stopFac = builder.createTransitStopFacility(new IdImpl("13"), this.scenario.createCoord(1500, 0), stopsBlockLane); stopFac.setLinkId(new IdImpl("1213")); schedule.addStopFacility(stopFac);
		stop = builder.createTransitRouteStop(stopFac, 50, 60); stopListA.add(stop);
		
		stopFac = builder.createTransitStopFacility(new IdImpl("21"), this.scenario.createCoord(500, -500), stopsBlockLane); stopFac.setLinkId(new IdImpl("2021")); schedule.addStopFacility(stopFac);
		stop = builder.createTransitRouteStop(stopFac, 0, 10); stopListB.add(stop);
		stopFac = builder.createTransitStopFacility(new IdImpl("23"), this.scenario.createCoord(1500, -500), stopsBlockLane); stopFac.setLinkId(new IdImpl("2223")); schedule.addStopFacility(stopFac);
		stop = builder.createTransitRouteStop(stopFac, 50, 60); stopListB.add(stop);

		// transit line A		
		Link startLinkA = this.scenario.getNetwork().getLinks().get(new IdImpl("0110"));
		Link endLinkA = this.scenario.getNetwork().getLinks().get(new IdImpl("1314"));
		NetworkRoute networkRouteA = (NetworkRoute) ((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).createRoute(TransportMode.car, startLinkA.getId(), endLinkA.getId());
		
		ArrayList<Id> linkListA = new ArrayList<Id>(); linkListA.add(new IdImpl("1011")); linkListA.add(new IdImpl("1112")); linkListA.add(new IdImpl("1213"));
		
		networkRouteA.setLinkIds(startLinkA.getId(), linkListA, endLinkA.getId());
		TransitRoute tRouteA = builder.createTransitRoute(new IdImpl("A"), networkRouteA, stopListA, "bus");
		TransitLine tLineA = builder.createTransitLine(new IdImpl("line A")); tLineA.addRoute(tRouteA); schedule.addTransitLine(tLineA);

		for (int i = 0; i < nOfBuses; i++	) {
			Departure dep = builder.createDeparture(new IdImpl(i), departureTime + i*heading + (i == delayedBus ? delay : 0));
			dep.setVehicleId(new IdImpl(i));
			tRouteA.addDeparture(dep);
		}
		
		// transit line B		
		Link startLinkB = this.scenario.getNetwork().getLinks().get(new IdImpl("0220"));
		Link endLinkB = this.scenario.getNetwork().getLinks().get(new IdImpl("2324"));
		NetworkRoute networkRouteB = (NetworkRoute) ((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).createRoute(TransportMode.car, startLinkB.getId(), endLinkB.getId());
		
		ArrayList<Id> linkListB = new ArrayList<Id>(); linkListB.add(new IdImpl("2021")); linkListB.add(new IdImpl("2122")); linkListB.add(new IdImpl("2223"));
		
		networkRouteB.setLinkIds(startLinkB.getId(), linkListB, endLinkB.getId());
		TransitRoute tRouteB = builder.createTransitRoute(new IdImpl("B"), networkRouteB, stopListB, "bus");
		TransitLine tLineB = builder.createTransitLine(new IdImpl("line B")); tLineB.addRoute(tRouteB); schedule.addTransitLine(tLineB);

		for (int i = 0; i < nOfBuses; i++) {
			Departure dep = builder.createDeparture(new IdImpl(i + nOfBuses), departureTime + i*heading + (i == delayedBus ? delay : 0));
			dep.setVehicleId(new IdImpl(i + nOfBuses));
			tRouteB.addDeparture(dep);
		}
	}
	
	private void createVehicles() {
		Vehicles vehicles = this.scenario.getVehicles();
		VehiclesFactory vb = vehicles.getFactory();
		
		// bus like
		VehicleType busType = vb.createVehicleType(new IdImpl("bus"));
		VehicleCapacity capacity = vb.createVehicleCapacity();
		capacity.setSeats(Integer.valueOf(9999));
		capacity.setStandingRoom(Integer.valueOf(0));
		busType.setCapacity(capacity);
		busType.setAccessTime(2.0);
		busType.setEgressTime(1.0);
		
		// train like
		VehicleType trainType = vb.createVehicleType(new IdImpl("bus"));
		capacity = vb.createVehicleCapacity();
		capacity.setSeats(Integer.valueOf(9999));
		capacity.setStandingRoom(Integer.valueOf(0));
		trainType.setCapacity(capacity);
		trainType.setAccessTime(0.2);
		trainType.setEgressTime(0.1);
		
		for (int i = 0; i < nOfBuses/2; i++) {
			vehicles.getVehicles().put(new IdImpl(i), vb.createVehicle(new IdImpl(i), busType));
		}
		
		for (int i = nOfBuses/2; i < nOfBuses; i++) {
			vehicles.getVehicles().put(new IdImpl(i), vb.createVehicle(new IdImpl(i), trainType));
		}		
		
		for (int i = 0; i < nOfBuses; i++) {
			vehicles.getVehicles().put(new IdImpl(i + nOfBuses), vb.createVehicle(new IdImpl(i + nOfBuses), busType));
		}
	}

	private void createPopulation() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		Population population = this.scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
		
		// line A
		TransitLine tLine = schedule.getTransitLines().get(new IdImpl("line A"));
		TransitRoute tRoute = tLine.getRoutes().get(new IdImpl("A"));
		for (int j = 0; j < nOfAgentsPerStopTrain; j++) {
			PersonImpl person = (PersonImpl) pb.createPerson(this.scenario.createId("A - " + j));
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", schedule.getFacilities().get(new IdImpl("11")).getLinkId());
			act1.setEndTime(departureTime + j * agentIntervalTrain);
			LegImpl leg = (LegImpl) pb.createLeg(TransportMode.pt);
			leg.setRoute(new ExperimentalTransitRoute(schedule.getFacilities().get(new IdImpl("11")), tLine, tRoute, schedule.getFacilities().get(new IdImpl("13"))));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("work", schedule.getFacilities().get(new IdImpl("13")).getLinkId());

			population.addPerson(person);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			plan.addActivity(act1);
			plan.addLeg(leg);
			plan.addActivity(act2);
		}	
		
		// line B
		tLine = schedule.getTransitLines().get(new IdImpl("line B"));
		tRoute = tLine.getRoutes().get(new IdImpl("B"));
		for (int j = 0; j < nOfAgentsPerStopBus; j++) {
			PersonImpl person = (PersonImpl) pb.createPerson(this.scenario.createId("B - " + j));
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", schedule.getFacilities().get(new IdImpl("21")).getLinkId());
			act1.setEndTime(departureTime + j * agentIntervalBus);
			LegImpl leg = (LegImpl) pb.createLeg(TransportMode.pt);
			leg.setRoute(new ExperimentalTransitRoute(schedule.getFacilities().get(new IdImpl("21")), tLine, tRoute, schedule.getFacilities().get(new IdImpl("23"))));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("work", schedule.getFacilities().get(new IdImpl("23")).getLinkId());

			population.addPerson(person);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			plan.addActivity(act1);
			plan.addLeg(leg);
			plan.addActivity(act2);
		}
		

	}

	private void runSim() {
		EventsManager events = EventsUtils.createEventsManager();

		TransitRoute route = this.scenario.getTransitSchedule().getTransitLines().get(new IdImpl("line A")).getRoutes().get(new IdImpl("A"));
		VehicleTracker vehTracker = new VehicleTracker();
		events.addHandler(vehTracker);
		TransitRouteAccessEgressAnalysis analysis = new TransitRouteAccessEgressAnalysis(route, vehTracker);
		events.addHandler(analysis);
		RouteTimeDiagram diagram = new RouteTimeDiagram();
		events.addHandler(diagram);

		final QSim sim = new QSim(this.scenario, events);
		// VisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(sim);
		// Transit vehicle drivers are created inside the TransitQueueSimulation, by the createAgents() method. That is, they exist
		// as derivatives from the schedule, not as behavioral entities by themselves.  kai, oct'09

//		sim.addQueueSimulationListeners(queueSimulationFeature);
//		sim.getEventsManager().addHandler(queueSimulationFeature) ;
		
		
		sim.getTransitEngine().setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, sim);
		OTFClientLive.run(scenario.getConfig(), server);
		sim.run();

		System.out.println("TransitRouteAccessEgressAnalysis:");
		analysis.printStats();
		System.out.println("Route-Time-Diagram:");
		diagram.writeData();
		String filename = "output/routeTimeDiagram.png";
		System.out.println("writing route-time diagram to: " + filename);
		diagram.createGraph(filename, route);
//		new agentGraph(this,analysis);
	}

	public void run() {
		createIds();
		prepareConfig();
		createNetwork();
		createTransitSchedule();
		createVehicles();
		createPopulation();
		runSim();
	}

	public static void main(final String[] args) {
		new AccessEgressDemoSimple().run();
	}

}
