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

package playground.marcel.pt.demo;

import java.util.ArrayList;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.Scenario;
import org.matsim.core.api.ScenarioImpl;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.population.PopulationBuilder;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.transitSchedule.TransitStopFacility;

import playground.marcel.OTFDemo;
import playground.marcel.pt.analysis.TransitRouteAccessEgressAnalysis;
import playground.marcel.pt.analysis.VehicleTracker;
import playground.marcel.pt.integration.ExperimentalTransitRoute;
import playground.marcel.pt.integration.TransitQueueSimulation;
import playground.marcel.pt.transitSchedule.DepartureImpl;
import playground.marcel.pt.transitSchedule.TransitLineImpl;
import playground.marcel.pt.transitSchedule.TransitRouteImpl;
import playground.marcel.pt.transitSchedule.TransitRouteStopImpl;
import playground.marcel.pt.transitSchedule.TransitScheduleImpl;
import playground.mohit.pt.agentGraph;

public class AccessEgressDemo {

	private static final int nOfLinks = 15;
	private static final int nOfBuses = 20;
	private static final int nOfAgentsPerStop = 100;
	private static final int agentInterval = 60;
	private static final int delayedBus = 6;
	private static final int heading = 5*60;
	private static final int delay = 60;
	private static final double departureTime = 7.0*3600;
	private static final boolean stopsBlockLane = true;

	private static final String SERVERNAME = "access_egress_demo";
	
	private final Scenario scenario = new ScenarioImpl();
	private final TransitScheduleImpl schedule = new TransitScheduleImpl();
	public final Id[] ids = new Id[Math.max(nOfLinks + 1, nOfBuses)];

	private void createIds() {
		for (int i = 0; i < this.ids.length; i++) {
			this.ids[i] = this.scenario.createId(Integer.toString(i));
		}
	}

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.simulation().setSnapshotStyle("queue");
		config.simulation().setEndTime(24.0*3600);
	}

	private void createNetwork() {
		NetworkLayer network = (NetworkLayer) this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node[] nodes = new Node[nOfLinks + 1];
		for (int i = 0; i <= nOfLinks; i++) {
			nodes[i] = network.createNode(this.ids[i], this.scenario.createCoord(i * 500, 0));
		}
		for (int i = 0; i < nOfLinks; i++) {
			network.createLink(this.ids[i], nodes[i], nodes[i+1], 500.0, 10.0, 1000.0, 1);
//			Link link = network.getBuilder().createLink(this.ids[i], nodes[i].getId(), nodes[i+1].getId());
//			network.getLinks().put(link.getId(), link);
//			nodes[i].addOutLink(link);
//			nodes[i+1].addInLink(link);
//			link.setLength(500.0);
//			link.setFreespeed(10.0);
//			link.setCapacity(1000.0);
//			link.setNumberOfLanes(1.0);
		}
	}

	private void createTransitSchedule() {
		TransitStopFacility[] stops = new TransitStopFacility[nOfLinks];
		ArrayList<TransitRouteStopImpl> stopList = new ArrayList<TransitRouteStopImpl>(nOfLinks);
		for (int i = 0; i < nOfLinks; i++) {
			stops[i] = new TransitStopFacility(this.ids[i], this.scenario.createCoord((i+1)*500, 0), stopsBlockLane);
			stops[i].setLink(this.scenario.getNetwork().getLinks().get(this.ids[i]));
			this.schedule.addStopFacility(stops[i]);
			TransitRouteStopImpl stop = new TransitRouteStopImpl(stops[i], i * 50, i * 50 + 10);
			stopList.add(stop);
		}
		Link startLink = this.scenario.getNetwork().getLinks().get(this.ids[0]);
		Link endLink = this.scenario.getNetwork().getLinks().get(this.ids[nOfLinks - 1]);
		NetworkRoute networkRoute = (NetworkRoute) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, startLink, endLink);
		ArrayList<Link> linkList = new ArrayList<Link>(nOfLinks - 2);
		for (int i = 1; i < nOfLinks -1; i++) {
			linkList.add(this.scenario.getNetwork().getLinks().get(this.ids[i]));
		}
		networkRoute.setLinks(startLink, linkList, endLink);
		TransitRouteImpl tRoute = new TransitRouteImpl(this.ids[1], networkRoute, stopList, TransportMode.bus);

		TransitLineImpl tLine = new TransitLineImpl(this.ids[1]);
		tLine.addRoute(tRoute);
		this.schedule.addTransitLine(tLine);

		for (int i = 0; i < nOfBuses; i++	) {
			tRoute.addDeparture(new DepartureImpl(this.ids[i], departureTime + i*heading + (i == delayedBus ? delay : 0)));
		}
//		try {
//			new TransitScheduleWriterV1(this.schedule).write("accessEgressSchedule.xml");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	private void createPopulation() {
		Population population = this.scenario.getPopulation();
		PopulationBuilder pb = population.getPopulationBuilder();
		TransitStopFacility[] stops = this.schedule.getFacilities().values().toArray(new TransitStopFacility[this.schedule.getFacilities().size()]);
		TransitLineImpl tLine = this.schedule.getTransitLines().get(this.ids[1]);

		TransitStopFacility lastStop = this.schedule.getFacilities().get(this.ids[stops.length - 1]);
		for (int i = 0; i < stops.length; i++) {
			TransitStopFacility stop = stops[i];
			if (stop == lastStop) {
				continue;
			}
			for (int j = 0; j < nOfAgentsPerStop; j++) {
				Person person = pb.createPerson(this.scenario.createId(Integer.toString(i * nOfAgentsPerStop + j)));
				Plan plan = pb.createPlan(person);
				ActivityImpl act1 = pb.createActivityFromLinkId("home", this.ids[i]);
				act1.setEndTime(departureTime + j * agentInterval);
				LegImpl leg = pb.createLeg(TransportMode.pt);
				leg.setRoute(new ExperimentalTransitRoute(stop, tLine, lastStop));
				ActivityImpl act2 = pb.createActivityFromLinkId("work", this.ids[nOfLinks - 1]);

				population.getPersons().put(person.getId(), person);
				person.getPlans().add(plan);
				person.setSelectedPlan(plan);
				plan.addActivity(act1);
				plan.addLeg(leg);
				plan.addActivity(act2);
			}
		}
	}

	private void runSim() {
		Events events = new Events();

		VehicleTracker vehTracker = new VehicleTracker();
		events.addHandler(vehTracker);
		TransitRouteAccessEgressAnalysis analysis = new TransitRouteAccessEgressAnalysis(this.schedule.getTransitLines().get(this.ids[1]).getRoutes().get(this.ids[1]), vehTracker);
		events.addHandler(analysis);

		final TransitQueueSimulation sim = new TransitQueueSimulation(this.scenario.getNetwork(), this.scenario.getPopulation(), events);
		sim.startOTFServer(SERVERNAME);
		sim.setTransitSchedule(this.schedule);
		OTFDemo.ptConnect(SERVERNAME);
		sim.run();

		analysis.printStats();
		new agentGraph(this,analysis);
	}

	public void run() {
		createIds();
		prepareConfig();
		createNetwork();
		createTransitSchedule();
		createPopulation();
		runSim();
		
	}

	public static void main(final String[] args) {
		
		AccessEgressDemo a = new AccessEgressDemo();
		a.run();
	}

}
