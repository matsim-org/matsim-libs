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

package playground.mrieser.pt.demo;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;

import playground.mrieser.pt.analysis.RouteTimeDiagram;
import playground.mrieser.pt.analysis.TransitRouteAccessEgressAnalysis;
import playground.mrieser.pt.analysis.VehicleTracker;

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

	private final ScenarioImpl scenario = new ScenarioImpl();
	public final Id[] ids = new Id[Math.max(nOfLinks + 1, nOfBuses)];

	private void createIds() {
		for (int i = 0; i < this.ids.length; i++) {
			this.ids[i] = this.scenario.createId(Integer.toString(i));
		}
	}

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.setQSimConfigGroup(new QSimConfigGroup());
		config.scenario().setUseVehicles(true);
		config.scenario().setUseTransit(true);
		config.getQSimConfigGroup().setSnapshotStyle("queue");
		config.getQSimConfigGroup().setEndTime(24.0*3600);
	}

	private void createNetwork() {
		NetworkImpl network = this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node[] nodes = new Node[nOfLinks + 1];
		for (int i = 0; i <= nOfLinks; i++) {
			nodes[i] = network.getFactory().createNode(this.ids[i], this.scenario.createCoord(i * 500, 0));
			network.addNode(nodes[i]);
		}
		for (int i = 0; i < nOfLinks; i++) {
			Link l = network.getFactory().createLink(this.ids[i], nodes[i].getId(), nodes[i+1].getId());
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
		ArrayList<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>(nOfLinks);
		for (int i = 0; i < nOfLinks; i++) {
			stops[i] = builder.createTransitStopFacility(this.ids[i], this.scenario.createCoord((i+1)*500, 0), stopsBlockLane);
			stops[i].setLinkId(this.ids[i]);
			schedule.addStopFacility(stops[i]);
			TransitRouteStop stop = builder.createTransitRouteStop(stops[i], i * 50, i * 50 + 10);
			stopList.add(stop);
		}
		Link startLink = this.scenario.getNetwork().getLinks().get(this.ids[0]);
		Link endLink = this.scenario.getNetwork().getLinks().get(this.ids[nOfLinks - 1]);
		NetworkRoute networkRoute = (NetworkRoute) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, startLink.getId(), endLink.getId());
		ArrayList<Id> linkList = new ArrayList<Id>(nOfLinks - 2);
		for (int i = 1; i < nOfLinks -1; i++) {
			linkList.add(this.ids[i]);
		}
		networkRoute.setLinkIds(startLink.getId(), linkList, endLink.getId());
		TransitRoute tRoute = builder.createTransitRoute(this.ids[1], networkRoute, stopList, "bus");

		TransitLine tLine = builder.createTransitLine(this.ids[1]);
		tLine.addRoute(tRoute);
		schedule.addTransitLine(tLine);

		for (int i = 0; i < nOfBuses; i++	) {
			Departure dep = builder.createDeparture(this.ids[i], departureTime + i*heading + (i == delayedBus ? delay : 0));
			dep.setVehicleId(this.ids[i]);
			tRoute.addDeparture(dep);
		}
	}

	private void createVehicles() {
		Vehicles vehicles = this.scenario.getVehicles();
		VehiclesFactory vb = vehicles.getFactory();
		VehicleType vehicleType = vb.createVehicleType(new IdImpl("transitVehicleType"));
		VehicleCapacity capacity = vb.createVehicleCapacity();
		capacity.setSeats(Integer.valueOf(101));
		capacity.setStandingRoom(Integer.valueOf(0));
		vehicleType.setCapacity(capacity);
		for (int i = 0; i < nOfBuses; i++) {
			vehicles.getVehicles().put(this.ids[i], vb.createVehicle(this.ids[i], vehicleType));
		}
	}

	private void createPopulation() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		Population population = this.scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
		TransitStopFacility[] stops = schedule.getFacilities().values().toArray(new TransitStopFacility[schedule.getFacilities().size()]);
		TransitLine tLine = schedule.getTransitLines().get(this.ids[1]);
		TransitRoute tRoute = tLine.getRoutes().get(this.ids[1]);

		TransitStopFacility lastStop = schedule.getFacilities().get(this.ids[stops.length - 1]);
		for (int i = 0; i < stops.length; i++) {
			TransitStopFacility stop = stops[i];
			if (stop == lastStop) {
				continue;
			}
			for (int j = 0; j < nOfAgentsPerStop; j++) {
				PersonImpl person = (PersonImpl) pb.createPerson(this.scenario.createId(Integer.toString(i * nOfAgentsPerStop + j)));
				PlanImpl plan = (PlanImpl) pb.createPlan();
				ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", stop.getLinkId());
				act1.setEndTime(departureTime + j * agentInterval);
				LegImpl leg = (LegImpl) pb.createLeg(TransportMode.pt);
				leg.setRoute(new ExperimentalTransitRoute(stop, tLine, tRoute, lastStop));
				ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("work", this.ids[nOfLinks - 1]);

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
		EventsManagerImpl events = new EventsManagerImpl();

		TransitRoute route = this.scenario.getTransitSchedule().getTransitLines().get(this.ids[1]).getRoutes().get(this.ids[1]);
		VehicleTracker vehTracker = new VehicleTracker();
		events.addHandler(vehTracker);
		TransitRouteAccessEgressAnalysis analysis = new TransitRouteAccessEgressAnalysis(route, vehTracker);
		events.addHandler(analysis);
		RouteTimeDiagram diagram = new RouteTimeDiagram();
		events.addHandler(diagram);

		final QSim sim = new QSim(this.scenario, events);
		// Transit vehicle drivers are created inside the TransitQueueSimulation, by the createAgents() method. That is, they exist
		// as derivatives from the schedule, not as behavioral entities by themselves.  kai, oct'09

		sim.addFeature(new OTFVisMobsimFeature(sim));
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
		new AccessEgressDemo().run();
	}

}
