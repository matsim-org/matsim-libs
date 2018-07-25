/* *********************************************************************** *
 * project: org.matsim.*
 * AccessEgressDemo.java
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

import java.util.ArrayList;

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
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.pt.SimpleTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.analysis.RouteTimeDiagram;
import org.matsim.pt.analysis.TransitRouteAccessEgressAnalysis;
import org.matsim.pt.analysis.VehicleTracker;
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

public class AccessEgressDemo {

	private static final int nOfLinks = 15;
	private static final int nOfBuses = 20;
	private static final int nOfAgentsPerStop = 100;
	private static final int agentInterval = 60;
	private static final int delayedBus = 9;
	private static final int heading = 5*60;
	private static final int delay = 60;
	private static final double departureTime = 7.0*3600;
	private static final boolean stopsBlockLane = true;

	private final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.transit().setUseTransit(true);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		config.qsim().setEndTime(24.0*3600);
	}

	private void createNetwork() {
		Network network = this.scenario.getNetwork();
		Node[] nodes = new Node[nOfLinks + 1];
		for (int i = 0; i <= nOfLinks; i++) {
			nodes[i] = network.getFactory().createNode(Id.create(i, Node.class), new Coord(i * 500, 0));
			network.addNode(nodes[i]);
		}
		for (int i = 0; i < nOfLinks; i++) {
			Link l = network.getFactory().createLink(Id.create(i, Link.class), nodes[i], nodes[i+1]);
			l.setLength(500.0);
			l.setFreespeed(10.0);
			l.setCapacity(1000.0);
			l.setNumberOfLanes(1);
			network.addLink(l);
		}
	}

	private void createTransitSchedule() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();
		TransitStopFacility[] stops = new TransitStopFacility[nOfLinks];
		ArrayList<TransitRouteStop> stopList = new ArrayList<>(nOfLinks);
		for (int i = 0; i < nOfLinks; i++) {
			stops[i] = builder.createTransitStopFacility(Id.create(i, TransitStopFacility.class), new Coord((i + 1) * 500, 0), stopsBlockLane);
			stops[i].setLinkId(Id.create(i, Link.class));
			schedule.addStopFacility(stops[i]);
			TransitRouteStop stop = builder.createTransitRouteStop(stops[i], i * 50, i * 50 + 10);
			stopList.add(stop);
		}
		Link startLink = this.scenario.getNetwork().getLinks().get(Id.create(0, Link.class));
		Link endLink = this.scenario.getNetwork().getLinks().get(Id.create(nOfLinks - 1, Link.class));
		NetworkRoute networkRoute = this.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, startLink.getId(), endLink.getId());
		ArrayList<Id<Link>> linkList = new ArrayList<>(nOfLinks - 2);
		for (int i = 1; i < nOfLinks -1; i++) {
			linkList.add(Id.create(i, Link.class));
		}
		networkRoute.setLinkIds(startLink.getId(), linkList, endLink.getId());
		TransitRoute tRoute = builder.createTransitRoute(Id.create(1, TransitRoute.class), networkRoute, stopList, "bus");

		TransitLine tLine = builder.createTransitLine(Id.create(1, TransitLine.class));
		tLine.addRoute(tRoute);
		schedule.addTransitLine(tLine);

		for (int i = 0; i < nOfBuses; i++	) {
			Departure dep = builder.createDeparture(Id.create(i, Departure.class), departureTime + i*heading + (i == delayedBus ? delay : 0));
			dep.setVehicleId(Id.create(i, Vehicle.class));
			tRoute.addDeparture(dep);
		}
	}

	private void createVehicles() {
		Vehicles vehicles = this.scenario.getTransitVehicles();
		VehiclesFactory vb = vehicles.getFactory();
		VehicleType vehicleType = vb.createVehicleType(Id.create("transitVehicleType", VehicleType.class));
		vehicles.addVehicleType(vehicleType);
		
		VehicleCapacity capacity = vb.createVehicleCapacity();
		capacity.setSeats(101);
		capacity.setStandingRoom(0);
		vehicleType.setCapacity(capacity);
		for (int i = 0; i < nOfBuses; i++) {
			vehicles.addVehicle( vb.createVehicle(Id.create(i, Vehicle.class), vehicleType));
		}
	}

	private void createPopulation() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		Population population = this.scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
		TransitStopFacility[] stops = schedule.getFacilities().values().toArray(new TransitStopFacility[schedule.getFacilities().size()]);
		TransitLine tLine = schedule.getTransitLines().get(Id.create(1, TransitLine.class));
		TransitRoute tRoute = tLine.getRoutes().get(Id.create(1, TransitRoute.class));

		TransitStopFacility lastStop = schedule.getFacilities().get(Id.create(stops.length - 1, TransitStopFacility.class));
		for (int i = 0; i < stops.length; i++) {
			TransitStopFacility stop = stops[i];
			if (stop == lastStop) {
				continue;
			}
			for (int j = 0; j < nOfAgentsPerStop; j++) {
				Person person = pb.createPerson(Id.create(Integer.toString(i * nOfAgentsPerStop + j), Person.class));
				Plan plan = pb.createPlan();
				Activity act1 = pb.createActivityFromLinkId("home", stop.getLinkId());
				act1.setEndTime(departureTime + j * agentInterval);
				Leg leg = pb.createLeg(TransportMode.pt);
				leg.setRoute(new ExperimentalTransitRoute(stop, tLine, tRoute, lastStop));
				Activity act2 = pb.createActivityFromLinkId("work", Id.create(nOfLinks - 1, Link.class));

				population.addPerson(person);
				person.addPlan(plan);
				person.setSelectedPlan(plan);
				plan.addActivity(act1);
				plan.addLeg(leg);
				plan.addActivity(act2);
			}
		}
	}

	private void runSim() {
		EventsManager events = EventsUtils.createEventsManager();

		TransitRoute route = this.scenario.getTransitSchedule().getTransitLines().get(Id.create(1, TransitLine.class)).getRoutes().get(Id.create(1, TransitRoute.class));
		VehicleTracker vehTracker = new VehicleTracker();
		events.addHandler(vehTracker);
		TransitRouteAccessEgressAnalysis analysis = new TransitRouteAccessEgressAnalysis(route, vehTracker);
		events.addHandler(analysis);
		RouteTimeDiagram diagram = new RouteTimeDiagram();
		events.addHandler(diagram);

		final QSim sim = new QSimBuilder(scenario.getConfig()) //
				.addDefaultPlugins() //
				.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bind(TransitStopHandlerFactory.class).to(SimpleTransitStopHandlerFactory.class)
								.asEagerSingleton();
					}
				}) //
				.build(scenario, events);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(this.scenario.getConfig(), this.scenario, events, sim);
		OTFClientLive.run(this.scenario.getConfig(), server);
		sim.run();

		System.out.println("TransitRouteAccessEgressAnalysis:");
		analysis.printStats();
		System.out.println("Route-Time-Diagram:");
		diagram.writeData();
		String filename = "output/routeTimeDiagram.png";
		System.out.println("writing route-time diagram to: " + filename);
		diagram.createGraph(filename, route);
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
		new AccessEgressDemo().run();
	}

}
