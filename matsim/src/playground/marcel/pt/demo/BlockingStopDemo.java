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

package playground.marcel.pt.demo;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.Scenario;
import org.matsim.core.api.ScenarioImpl;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
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
import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.transitSchedule.TransitSchedule;

public class BlockingStopDemo {

	private static final int nOfLinks = 13;
	private static final int nOfStops = nOfLinks-3;
	private static final int nOfCars = 20;
	private static final int carsHeading = 30;
	private static final double startTime = 7.0 * 3600;
	private static final double busDeparture = 7.0 * 3600 + 3 * 60 + 15;

	private final Scenario scenario = new ScenarioImpl();
	private final TransitSchedule schedule = new TransitSchedule();
	private final Id[] ids = new Id[nOfLinks * 2 + 2];

	private TransitQueueSimulation sim = null;

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
		Node[] nodes = new Node[nOfLinks * 2 + 2];
		for (int i = 0; i <= nOfLinks; i++) {
			nodes[i           ] = network.createNode(this.ids[i           ], this.scenario.createCoord(i * 500, 0));
			nodes[i+nOfLinks+1] = network.createNode(this.ids[i+nOfLinks+1], this.scenario.createCoord(i * 500, 500));
		}
		for (int i = 0; i < nOfLinks; i++) {
			network.createLink(this.ids[i         ], nodes[i           ], nodes[i+1], 500.0, 10.0, 1000.0, 1);
			network.createLink(this.ids[i+nOfLinks], nodes[i+nOfLinks+1], nodes[i+nOfLinks+2], 500.0, 10.0, 1000.0, 1);
		}
	}

	private void createTransitSchedule() {
		TransitStopFacility[] stops = new TransitStopFacility[nOfStops * 2];
		ArrayList<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>(nOfStops);

		// line 1
		for (int i = 0; i < nOfStops; i++) {
			stops[i] = new TransitStopFacility(this.ids[i], this.scenario.createCoord(1000 + i*500, 0), false);
			stops[i].setLink(this.scenario.getNetwork().getLinks().get(this.ids[i+1]));
			this.schedule.addStopFacility(stops[i]);
			stopList.add(new TransitRouteStop(stops[i], 100 + i*70, 120 + i*70));
		}

		Link startLink = this.scenario.getNetwork().getLinks().get(this.ids[0]);
		Link endLink = this.scenario.getNetwork().getLinks().get(this.ids[nOfLinks-1]);
		NetworkRoute networkRoute = (NetworkRoute) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, startLink, endLink);
		ArrayList<Link> linkList = new ArrayList<Link>(nOfLinks);
		for (int i = 1; i < nOfLinks-1; i++) {
			linkList.add(this.scenario.getNetwork().getLinks().get(this.ids[i]));
		}
		networkRoute.setLinks(startLink, linkList, endLink);
		TransitRoute tRoute1 = new TransitRoute(this.ids[1], networkRoute, stopList, TransportMode.bus);

		TransitLine tLine1 = new TransitLine(this.ids[1]);
		tLine1.addRoute(tRoute1);
		this.schedule.addTransitLine(tLine1);

		tRoute1.addDeparture(new Departure(this.ids[1], busDeparture));

		// line 2
		stopList = new ArrayList<TransitRouteStop>(nOfStops);
		for (int i = 0; i < nOfStops; i++) {
			stops[i+nOfStops] = new TransitStopFacility(this.ids[i+nOfStops], this.scenario.createCoord(1000 + i*500, 500), true);
			stops[i+nOfStops].setLink(this.scenario.getNetwork().getLinks().get(this.ids[i+1+nOfLinks]));
			this.schedule.addStopFacility(stops[i+nOfStops]);
			stopList.add(new TransitRouteStop(stops[i+nOfStops], 100 + i*70, 120 + i*70));
		}

		startLink = this.scenario.getNetwork().getLinks().get(this.ids[nOfLinks]);
		endLink = this.scenario.getNetwork().getLinks().get(this.ids[2*nOfLinks-1]);
		networkRoute = (NetworkRoute) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, startLink, endLink);
		linkList = new ArrayList<Link>(nOfLinks);
		for (int i = nOfLinks+1; i < (2*nOfLinks - 1); i++) {
			linkList.add(this.scenario.getNetwork().getLinks().get(this.ids[i]));
		}
		networkRoute.setLinks(startLink, linkList, endLink);
		TransitRoute tRoute2 = new TransitRoute(this.ids[2], networkRoute, stopList, TransportMode.bus);

		TransitLine tLine2 = new TransitLine(this.ids[2]);
		tLine2.addRoute(tRoute2);
		this.schedule.addTransitLine(tLine2);

		tRoute2.addDeparture(new Departure(this.ids[2], busDeparture));

//		try {
//			new TransitScheduleWriterV1(this.schedule).write("blockingStopSchedule.xml");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	private void createPopulation() {
		Population population = this.scenario.getPopulation();
		PopulationBuilder pb = population.getPopulationBuilder();
//		TransitStopFacility[] stops = this.schedule.getFacilities().values().toArray(new TransitStopFacility[this.schedule.getFacilities().size()]);
		TransitLine tLine1 = this.schedule.getTransitLines().get(this.ids[1]);
		TransitLine tLine2 = this.schedule.getTransitLines().get(this.ids[2]);

		// bus-passengers line 1
		for (int i = 1; i < nOfStops; i++) {
			Person person = pb.createPerson(this.scenario.createId(Integer.toString(-i)));
			Plan plan = pb.createPlan(person);
			ActivityImpl act1 = pb.createActivityFromLinkId("home", this.ids[i]);
			act1.setEndTime(startTime + i*60);
			LegImpl leg = pb.createLeg(TransportMode.pt);
			leg.setRoute(new ExperimentalTransitRoute(this.schedule.getFacilities().get(this.ids[i-1]), tLine1, this.schedule.getFacilities().get(this.ids[nOfStops-1])));
			ActivityImpl act2 = pb.createActivityFromLinkId("work", this.ids[nOfLinks-1]);

			population.getPersons().put(person.getId(), person);
			person.getPlans().add(plan);
			person.setSelectedPlan(plan);
			plan.addActivity(act1);
			plan.addLeg(leg);
			plan.addActivity(act2);
		}

		// bus-passengers line 2
		for (int i = 1; i < nOfStops; i++) {
			Person person = pb.createPerson(this.scenario.createId(Integer.toString(-i-nOfStops)));
			Plan plan = pb.createPlan(person);
			ActivityImpl act1 = pb.createActivityFromLinkId("home", this.ids[nOfLinks+i]);
			act1.setEndTime(startTime + i*60);
			LegImpl leg = pb.createLeg(TransportMode.pt);
			leg.setRoute(new ExperimentalTransitRoute(this.schedule.getFacilities().get(this.ids[nOfStops+i-1]), tLine2, this.schedule.getFacilities().get(this.ids[2*nOfStops-1])));
			ActivityImpl act2 = pb.createActivityFromLinkId("work", this.ids[2*nOfLinks-1]);

			population.getPersons().put(person.getId(), person);
			person.getPlans().add(plan);
			person.setSelectedPlan(plan);
			plan.addActivity(act1);
			plan.addLeg(leg);
			plan.addActivity(act2);
		}

		// car-drivers
		Network network = this.scenario.getNetwork();
		NetworkRoute carRoute1 = (NetworkRoute) network.getFactory().createRoute(TransportMode.car, network.getLinks().get(this.ids[0]), network.getLinks().get(this.ids[nOfLinks-1]));
		NetworkRoute carRoute2 = (NetworkRoute) network.getFactory().createRoute(TransportMode.car, network.getLinks().get(this.ids[nOfLinks]), network.getLinks().get(this.ids[2*nOfLinks-1]));
		List<Link> links1 = new ArrayList<Link>(nOfLinks-2);
		List<Link> links2 = new ArrayList<Link>(nOfLinks-2);
		for (int i = 1; i<nOfLinks-1; i++) {
			links1.add(network.getLinks().get(this.ids[i]));
			links2.add(network.getLinks().get(this.ids[i+nOfLinks]));
		}
		carRoute1.setLinks(network.getLinks().get(this.ids[0]), links1, network.getLinks().get(this.ids[nOfLinks-1]));
		carRoute2.setLinks(network.getLinks().get(this.ids[nOfLinks]), links2, network.getLinks().get(this.ids[2*nOfLinks-1]));
		for (int i = 0; i < nOfCars; i++) {
			Person person = pb.createPerson(this.scenario.createId(Integer.toString(i)));
			Plan plan = pb.createPlan(person);
			ActivityImpl act1a = pb.createActivityFromLinkId("home", this.ids[0]);
			act1a.setEndTime(startTime + i*carsHeading);
			LegImpl leg1 = pb.createLeg(TransportMode.car);
			leg1.setRoute(carRoute1);
			ActivityImpl act1b = pb.createActivityFromLinkId("work", this.ids[4]);

			population.getPersons().put(person.getId(), person);
			person.getPlans().add(plan);
			person.setSelectedPlan(plan);
			plan.addActivity(act1a);
			plan.addLeg(leg1);
			plan.addActivity(act1b);

			Person person2 = pb.createPerson(this.scenario.createId(Integer.toString(i+nOfCars)));
			Plan plan2 = pb.createPlan(person2);
			ActivityImpl act2a = pb.createActivityFromLinkId("home", this.ids[nOfLinks]);
			act2a.setEndTime(startTime + i*carsHeading);
			LegImpl leg2 = pb.createLeg(TransportMode.car);
			leg2.setRoute(carRoute2);
			ActivityImpl act2b = pb.createActivityFromLinkId("work", this.ids[nOfLinks-1]);

			population.getPersons().put(person2.getId(), person2);
			person2.getPlans().add(plan2);
			person2.setSelectedPlan(plan2);
			plan2.addActivity(act2a);
			plan2.addLeg(leg2);
			plan2.addActivity(act2b);
		}

	}

	private void runSim() {
		Events events = new Events();

		VehicleTracker vehTracker = new VehicleTracker();
		events.addHandler(vehTracker);
		TransitRouteAccessEgressAnalysis analysis = new TransitRouteAccessEgressAnalysis(this.schedule.getTransitLines().get(this.ids[1]).getRoutes().get(this.ids[1]), vehTracker);
		events.addHandler(analysis);

		this.sim = new TransitQueueSimulation(this.scenario.getNetwork(), this.scenario.getPopulation(), events);
		this.sim.startOTFServer("blocking_stop_demo");
		this.sim.setTransitSchedule(this.schedule);

		OTFDemo.ptConnect("blocking_stop_demo");

		this.sim.run();

		analysis.printStats();
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
		new BlockingStopDemo().run();
	}

}
