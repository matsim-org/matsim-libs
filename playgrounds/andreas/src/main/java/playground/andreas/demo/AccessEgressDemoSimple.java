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
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.andreas.bvgAna.mrieser.analysis.RouteTimeDiagram;
import playground.andreas.bvgAna.mrieser.analysis.TransitRouteAccessEgressAnalysis;
import playground.andreas.bvgAna.mrieser.analysis.VehicleTracker;

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

	private final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.transit().setUseTransit(true);
		config.qsim().setSnapshotStyle( SnapshotStyle.queue );
		config.qsim().setEndTime(24.0*3600);
	}

	private void createNetwork() {
		Network network = this.scenario.getNetwork();
		((NetworkImpl) network).setCapacityPeriod(3600.0);
		
		Node n01, n10, n11, n12, n13, n14;
		double x1 = -500;
		network.addNode(n01 = network.getFactory().createNode(Id.create("01", Node.class), new Coord(x1, 0)));
		network.addNode(n10 = network.getFactory().createNode(Id.create("10", Node.class), new Coord(0, 0)));
		network.addNode(n11 = network.getFactory().createNode(Id.create("11", Node.class), new Coord(500, 0)));
		network.addNode(n12 = network.getFactory().createNode(Id.create("12", Node.class), new Coord(1000, 0)));
		network.addNode(n13 = network.getFactory().createNode(Id.create("13", Node.class), new Coord(1500, 0)));
		network.addNode(n14 = network.getFactory().createNode(Id.create("14", Node.class), new Coord(2000, 0)));
		
		Node n02, n20, n21, n22, n23, n24;
		double x = -500;
		double y5 = -500;
		network.addNode(n02 = network.getFactory().createNode(Id.create("02", Node.class), new Coord(x, y5)));
		double y4 = -500;
		network.addNode(n20 = network.getFactory().createNode(Id.create("20", Node.class), new Coord(0, y4)));
		double y3 = -500;
		network.addNode(n21 = network.getFactory().createNode(Id.create("21", Node.class), new Coord(500, y3)));
		double y2 = -500;
		network.addNode(n22 = network.getFactory().createNode(Id.create("22", Node.class), new Coord(1000, y2)));
		double y1 = -500;
		network.addNode(n23 = network.getFactory().createNode(Id.create("23", Node.class), new Coord(1500, y1)));
		double y = -500;
		network.addNode(n24 = network.getFactory().createNode(Id.create("24", Node.class), new Coord(2000, y)));
		
		Link l;
		l = network.getFactory().createLink(Id.create("0110", Link.class), n01, n10); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("1011", Link.class), n10, n11); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("1112", Link.class), n11, n12); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("1213", Link.class), n12, n13); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("1314", Link.class), n13, n14); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		
		l = network.getFactory().createLink(Id.create("0220", Link.class), n02, n20); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("2021", Link.class), n20, n21); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("2122", Link.class), n21, n22); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("2223", Link.class), n22, n23); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("2324", Link.class), n23, n24); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(1000.0); l.setNumberOfLanes(1); network.addLink(l);
		
	}

	private void createTransitSchedule() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();
		
		ArrayList<TransitRouteStop> stopListA = new ArrayList<TransitRouteStop>();
		ArrayList<TransitRouteStop> stopListB = new ArrayList<TransitRouteStop>();

		// create stops
		TransitStopFacility stopFac;
		TransitRouteStop stop;

		stopFac = builder.createTransitStopFacility(Id.create("11", TransitStopFacility.class), new Coord(500, 0), stopsBlockLane); stopFac.setLinkId(Id.create("1011", Link.class)); schedule.addStopFacility(stopFac);
		stop = builder.createTransitRouteStop(stopFac, 0, 10); stopListA.add(stop);
		stopFac = builder.createTransitStopFacility(Id.create("13", TransitStopFacility.class), new Coord(1500, 0), stopsBlockLane); stopFac.setLinkId(Id.create("1213", Link.class)); schedule.addStopFacility(stopFac);
		stop = builder.createTransitRouteStop(stopFac, 50, 60); stopListA.add(stop);

		double y1 = -500;
		stopFac = builder.createTransitStopFacility(Id.create("21", TransitStopFacility.class), new Coord(500, y1), stopsBlockLane); stopFac.setLinkId(Id.create("2021", Link.class)); schedule.addStopFacility(stopFac);
		stop = builder.createTransitRouteStop(stopFac, 0, 10); stopListB.add(stop);
		double y = -500;
		stopFac = builder.createTransitStopFacility(Id.create("23", TransitStopFacility.class), new Coord(1500, y), stopsBlockLane); stopFac.setLinkId(Id.create("2223", Link.class)); schedule.addStopFacility(stopFac);
		stop = builder.createTransitRouteStop(stopFac, 50, 60); stopListB.add(stop);

		// transit line A		
		Link startLinkA = this.scenario.getNetwork().getLinks().get(Id.create("0110", Link.class));
		Link endLinkA = this.scenario.getNetwork().getLinks().get(Id.create("1314", Link.class));
		NetworkRoute networkRouteA = ((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, startLinkA.getId(), endLinkA.getId());
		
		ArrayList<Id<Link>> linkListA = new ArrayList<Id<Link>>(); 
		linkListA.add(Id.create("1011", Link.class)); 
		linkListA.add(Id.create("1112", Link.class)); 
		linkListA.add(Id.create("1213", Link.class));
		
		networkRouteA.setLinkIds(startLinkA.getId(), linkListA, endLinkA.getId());
		TransitRoute tRouteA = builder.createTransitRoute(Id.create("A", TransitRoute.class), networkRouteA, stopListA, "bus");
		TransitLine tLineA = builder.createTransitLine(Id.create("line A", TransitLine.class)); tLineA.addRoute(tRouteA); schedule.addTransitLine(tLineA);

		for (int i = 0; i < nOfBuses; i++	) {
			Departure dep = builder.createDeparture(Id.create(i, Departure.class), departureTime + i*heading + (i == delayedBus ? delay : 0));
			dep.setVehicleId(Id.create(i, Vehicle.class));
			tRouteA.addDeparture(dep);
		}
		
		// transit line B		
		Link startLinkB = this.scenario.getNetwork().getLinks().get(Id.create("0220", Link.class));
		Link endLinkB = this.scenario.getNetwork().getLinks().get(Id.create("2324", Link.class));
		NetworkRoute networkRouteB = ((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, startLinkB.getId(), endLinkB.getId());
		
		ArrayList<Id<Link>> linkListB = new ArrayList<Id<Link>>();
		linkListB.add(Id.create("2021", Link.class));
		linkListB.add(Id.create("2122", Link.class));
		linkListB.add(Id.create("2223", Link.class));
		
		networkRouteB.setLinkIds(startLinkB.getId(), linkListB, endLinkB.getId());
		TransitRoute tRouteB = builder.createTransitRoute(Id.create("B", TransitRoute.class), networkRouteB, stopListB, "bus");
		TransitLine tLineB = builder.createTransitLine(Id.create("line B", TransitLine.class)); tLineB.addRoute(tRouteB); schedule.addTransitLine(tLineB);

		for (int i = 0; i < nOfBuses; i++) {
			Departure dep = builder.createDeparture(Id.create(i + nOfBuses, Departure.class), departureTime + i*heading + (i == delayedBus ? delay : 0));
			dep.setVehicleId(Id.create(i + nOfBuses, Vehicle.class));
			tRouteB.addDeparture(dep);
		}
	}
	
	private void createVehicles() {
		Vehicles vehicles = this.scenario.getTransitVehicles();
		VehiclesFactory vb = vehicles.getFactory();
		
		// bus like
		VehicleType busType = vb.createVehicleType(Id.create("bus", VehicleType.class));
		VehicleCapacity capacity = vb.createVehicleCapacity();
		capacity.setSeats(Integer.valueOf(9999));
		capacity.setStandingRoom(Integer.valueOf(0));
		busType.setCapacity(capacity);
		busType.setAccessTime(2.0);
		busType.setEgressTime(1.0);
		vehicles.addVehicleType(busType);
		
		// train like
		VehicleType trainType = vb.createVehicleType(Id.create("train", VehicleType.class));
		capacity = vb.createVehicleCapacity();
		capacity.setSeats(Integer.valueOf(9999));
		capacity.setStandingRoom(Integer.valueOf(0));
		trainType.setCapacity(capacity);
		trainType.setAccessTime(0.2);
		trainType.setEgressTime(0.1);
		vehicles.addVehicleType(trainType);
		
		for (int i = 0; i < nOfBuses/2; i++) {
			vehicles.addVehicle( vb.createVehicle(Id.create(i, Vehicle.class), busType));
		}
		
		for (int i = nOfBuses/2; i < nOfBuses; i++) {
			vehicles.addVehicle( vb.createVehicle(Id.create(i, Vehicle.class), trainType));
		}		
		
		for (int i = 0; i < nOfBuses; i++) {
			vehicles.addVehicle( vb.createVehicle(Id.create(i + nOfBuses, Vehicle.class), busType));
		}
	}

	private void createPopulation() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		Population population = this.scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
		
		// line A
		TransitLine tLine = schedule.getTransitLines().get(Id.create("line A", TransitLine.class));
		TransitRoute tRoute = tLine.getRoutes().get(Id.create("A", TransitRoute.class));
		for (int j = 0; j < nOfAgentsPerStopTrain; j++) {
			Person person = pb.createPerson(Id.create("A - " + j, Person.class));
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", schedule.getFacilities().get(Id.create("11", TransitStopFacility.class)).getLinkId());
			act1.setEndTime(departureTime + j * agentIntervalTrain);
			LegImpl leg = (LegImpl) pb.createLeg(TransportMode.pt);
			leg.setRoute(new ExperimentalTransitRoute(schedule.getFacilities().get(Id.create("11", TransitStopFacility.class)), tLine, tRoute, schedule.getFacilities().get(Id.create("13", TransitStopFacility.class))));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("work", schedule.getFacilities().get(Id.create("13", TransitStopFacility.class)).getLinkId());

			population.addPerson(person);
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			plan.addActivity(act1);
			plan.addLeg(leg);
			plan.addActivity(act2);
		}	
		
		// line B
		tLine = schedule.getTransitLines().get(Id.create("line B", TransitLine.class));
		tRoute = tLine.getRoutes().get(Id.create("B", TransitRoute.class));
		for (int j = 0; j < nOfAgentsPerStopBus; j++) {
			Person person = pb.createPerson(Id.create("B - " + j, Person.class));
			PlanImpl plan = (PlanImpl) pb.createPlan();
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", schedule.getFacilities().get(Id.create("21", TransitStopFacility.class)).getLinkId());
			act1.setEndTime(departureTime + j * agentIntervalBus);
			LegImpl leg = (LegImpl) pb.createLeg(TransportMode.pt);
			leg.setRoute(new ExperimentalTransitRoute(schedule.getFacilities().get(Id.create("21", TransitStopFacility.class)), tLine, tRoute, schedule.getFacilities().get(Id.create("23", TransitStopFacility.class))));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("work", schedule.getFacilities().get(Id.create("23", TransitStopFacility.class)).getLinkId());

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

		TransitRoute route = this.scenario.getTransitSchedule().getTransitLines().get(Id.create("line A", TransitLine.class)).getRoutes().get(Id.create("A", TransitRoute.class));
		VehicleTracker vehTracker = new VehicleTracker();
		events.addHandler(vehTracker);
		TransitRouteAccessEgressAnalysis analysis = new TransitRouteAccessEgressAnalysis(route, vehTracker);
		events.addHandler(analysis);
		RouteTimeDiagram diagram = new RouteTimeDiagram();
		events.addHandler(diagram);
		QSim qSim1 = new QSim(this.scenario, events);
		ActivityEngine activityEngine = new ActivityEngine(events, qSim1.getAgentCounter());
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim1);
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, events);
		qSim1.addMobsimEngine(teleportationEngine);

        QSim qSim = qSim1;
        AgentFactory agentFactory;
            agentFactory = new TransitAgentFactory(qSim);
            TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
            qSim.addDepartureHandler(transitEngine);
            qSim.addAgentSource(transitEngine);
            qSim.addMobsimEngine(transitEngine);
        PopulationAgentSource agentSource = new PopulationAgentSource(this.scenario.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);
        final QSim sim = qSim;
		// VisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(sim);
		// Transit vehicle drivers are created inside the TransitQueueSimulation, by the createAgents() method. That is, they exist
		// as derivatives from the schedule, not as behavioral entities by themselves.  kai, oct'09

//		sim.addQueueSimulationListeners(queueSimulationFeature);
//		sim.getEventsManager().addHandler(queueSimulationFeature) ;
		
		
		transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
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
