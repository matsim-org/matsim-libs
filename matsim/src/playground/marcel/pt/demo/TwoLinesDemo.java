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

package playground.marcel.pt.demo;

import java.util.ArrayList;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.Scenario;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.experimental.population.PopulationBuilder;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitStopFacility;

import playground.marcel.OTFDemo;
import playground.marcel.pt.integration.ExperimentalTransitRoute;
import playground.marcel.pt.integration.TransitQueueSimulation;
import playground.marcel.pt.transitSchedule.DepartureImpl;
import playground.marcel.pt.transitSchedule.TransitLineImpl;
import playground.marcel.pt.transitSchedule.TransitRouteImpl;
import playground.marcel.pt.transitSchedule.TransitRouteStopImpl;
import playground.marcel.pt.transitSchedule.TransitScheduleImpl;

public class TwoLinesDemo {
	
	private final Scenario scenario = new ScenarioImpl();
	private final TransitScheduleImpl schedule = new TransitScheduleImpl();
	private final Id[] ids = new Id[15];

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
		NetworkLayer network = (NetworkLayer) this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = network.createNode(this.ids[1], this.scenario.createCoord(-2000, 0));
		Node node2 = network.createNode(this.ids[2], this.scenario.createCoord(-2000, 1000));
		Node node3 = network.createNode(this.ids[3], this.scenario.createCoord(-1000, 0));
		Node node4 = network.createNode(this.ids[4], this.scenario.createCoord(-1000, 1000));
		Node node5 = network.createNode(this.ids[5], this.scenario.createCoord(0, 0));
		Node node6 = network.createNode(this.ids[6], this.scenario.createCoord(0, 1000));
		Node node7 = network.createNode(this.ids[7], this.scenario.createCoord(500, 500));
		Node node8 = network.createNode(this.ids[8], this.scenario.createCoord(1500, 500));
		Node node9 = network.createNode(this.ids[9], this.scenario.createCoord(2500, 500));
		Node node10 = network.createNode(this.ids[10], this.scenario.createCoord(3500, 500));
		Node node11 = network.createNode(this.ids[11], this.scenario.createCoord(4000, 1000));
		Node node12 = network.createNode(this.ids[12], this.scenario.createCoord(4000, 0));
		Node node13 = network.createNode(this.ids[13], this.scenario.createCoord(5000, 1000));
		Node node14 = network.createNode(this.ids[14], this.scenario.createCoord(5000, 0));
		
		network.createLink(this.ids[1], node1, node3, 1000.0, 10.0, 3600.0, 1);
		network.createLink(this.ids[2], node2, node4, 1000.0, 10.0, 3600.0, 1);
		network.createLink(this.ids[3], node3, node5, 1000.0, 10.0, 3600.0, 1);
		network.createLink(this.ids[4], node4, node6, 1000.0, 10.0, 3600.0, 1);
		network.createLink(this.ids[5], node5, node7, 1000.0, 10.0, 3600.0, 1);
		network.createLink(this.ids[6], node6, node7, 1000.0, 10.0, 3600.0, 1);
		network.createLink(this.ids[7], node7, node8, 1000.0, 10.0, 3600.0, 1);
		network.createLink(this.ids[8], node8, node9, 1000.0, 10.0, 3600.0, 1);
		network.createLink(this.ids[9], node9, node10, 1000.0, 10.0, 3600.0, 1);
		network.createLink(this.ids[10], node10, node12, 1000.0, 10.0, 3600.0, 1);
		network.createLink(this.ids[11], node10, node11, 1000.0, 10.0, 3600.0, 1);
		network.createLink(this.ids[12], node12, node14, 1000.0, 10.0, 3600.0, 1);
		network.createLink(this.ids[13], node11, node13, 1000.0, 10.0, 3600.0, 1);
	}
	
	private void createTransitSchedule() {
		TransitStopFacility stop1 = new TransitStopFacility(this.ids[1], this.scenario.createCoord(-100, -50));
		TransitStopFacility stop2 = new TransitStopFacility(this.ids[2], this.scenario.createCoord(-100, 850));
		TransitStopFacility stop3 = new TransitStopFacility(this.ids[3], this.scenario.createCoord(1400, 450));
		TransitStopFacility stop4 = new TransitStopFacility(this.ids[4], this.scenario.createCoord(3400, 450));
		TransitStopFacility stop5 = new TransitStopFacility(this.ids[5], this.scenario.createCoord(3900, 50));
		TransitStopFacility stop6 = new TransitStopFacility(this.ids[6], this.scenario.createCoord(3900, 850));
		
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

		stop1.setLink(link3);
		stop2.setLink(link4);
		stop3.setLink(link7);
		stop4.setLink(link9);
		stop5.setLink(link11);
		stop6.setLink(link10);

		this.schedule.addStopFacility(stop1);
		this.schedule.addStopFacility(stop2);
		this.schedule.addStopFacility(stop3);
		this.schedule.addStopFacility(stop4);
		this.schedule.addStopFacility(stop5);
		this.schedule.addStopFacility(stop6);


		TransitLineImpl tLine1 = new TransitLineImpl(this.ids[1]);
		NetworkRoute networkRoute = (NetworkRoute) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, link1, link13);
		ArrayList<Link> linkList = new ArrayList<Link>(6);
		linkList.add(link3);
		linkList.add(link5);
		linkList.add(link7);
		linkList.add(link8);
		linkList.add(link9);
		linkList.add(link11);
		networkRoute.setLinks(link1, linkList, link13);
		ArrayList<TransitRouteStopImpl> stopList = new ArrayList<TransitRouteStopImpl>(4);
		stopList.add(new TransitRouteStopImpl(stop1, 0, 0));
		stopList.add(new TransitRouteStopImpl(stop3, 90, 100));
		stopList.add(new TransitRouteStopImpl(stop4, 290, 300));
		stopList.add(new TransitRouteStopImpl(stop5, 390, Time.UNDEFINED_TIME));
		TransitRouteImpl tRoute1 = new TransitRouteImpl(this.ids[1], networkRoute, stopList, TransportMode.bus);
		tLine1.addRoute(tRoute1);
		
		tRoute1.addDeparture(new DepartureImpl(this.ids[1], Time.parseTime("07:00:00")));
		tRoute1.addDeparture(new DepartureImpl(this.ids[2], Time.parseTime("07:05:00")));
		tRoute1.addDeparture(new DepartureImpl(this.ids[3], Time.parseTime("07:10:00")));
		tRoute1.addDeparture(new DepartureImpl(this.ids[4], Time.parseTime("07:15:00")));
		tRoute1.addDeparture(new DepartureImpl(this.ids[5], Time.parseTime("07:20:00")));
		tRoute1.addDeparture(new DepartureImpl(this.ids[6], Time.parseTime("07:25:00")));
		
		this.schedule.addTransitLine(tLine1);

		TransitLineImpl tLine2 = new TransitLineImpl(this.ids[2]);
		networkRoute = (NetworkRoute) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, link2, link12);
		linkList = new ArrayList<Link>(6);
		linkList.add(link4);
		linkList.add(link6);
		linkList.add(link7);
		linkList.add(link8);
		linkList.add(link9);
		linkList.add(link10);
		networkRoute.setLinks(link2, linkList, link12);
		stopList = new ArrayList<TransitRouteStopImpl>(4);
		stopList.add(new TransitRouteStopImpl(stop2, 0, 0));
		stopList.add(new TransitRouteStopImpl(stop3, 90, 100));
		stopList.add(new TransitRouteStopImpl(stop4, 290, 300));
		stopList.add(new TransitRouteStopImpl(stop6, 390, Time.UNDEFINED_TIME));
		TransitRouteImpl tRoute2 = new TransitRouteImpl(this.ids[1], networkRoute, stopList, TransportMode.bus);
		tLine2.addRoute(tRoute2);

		tRoute2.addDeparture(new DepartureImpl(this.ids[1], Time.parseTime("07:02:00")));
		tRoute2.addDeparture(new DepartureImpl(this.ids[2], Time.parseTime("07:12:00")));
		tRoute2.addDeparture(new DepartureImpl(this.ids[3], Time.parseTime("07:22:00")));
		
		this.schedule.addTransitLine(tLine2);
	}

	private void createPopulation() {
		Population population = this.scenario.getPopulation();
		PopulationBuilder pb = population.getPopulationBuilder();
		
		TransitLineImpl tLine1 = this.schedule.getTransitLines().get(this.ids[1]);
		TransitLineImpl tLine2 = this.schedule.getTransitLines().get(this.ids[2]);
		
		TransitStopFacility stop1 = this.schedule.getFacilities().get(this.ids[1]);
		TransitStopFacility stop2 = this.schedule.getFacilities().get(this.ids[2]);
		TransitStopFacility stop3 = this.schedule.getFacilities().get(this.ids[3]);
		TransitStopFacility stop4 = this.schedule.getFacilities().get(this.ids[4]);
		TransitStopFacility stop5 = this.schedule.getFacilities().get(this.ids[5]);
		TransitStopFacility stop6 = this.schedule.getFacilities().get(this.ids[6]);
		
		{ // person 1
			PersonImpl person = pb.createPerson(this.ids[1]);
			PlanImpl plan = pb.createPlan(person);
			ActivityImpl act1 = pb.createActivityFromLinkId("home", this.ids[1]);
			act1.setEndTime(Time.parseTime("07:01:00"));
			LegImpl leg1 = pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, stop3));
			ActivityImpl act2 = pb.createActivityFromLinkId("pt interaction", this.ids[3]);
			act2.setEndTime(Time.parseTime("07:01:00"));
			LegImpl leg2 = pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop3, tLine2, stop5));
			ActivityImpl act3 = pb.createActivityFromLinkId("pt interaction", this.ids[6]);
			
			plan.addActivity(act1);
			plan.addLeg(leg1);
			plan.addActivity(act2);
			plan.addLeg(leg2);
			plan.addActivity(act3);
			person.getPlans().add(plan);
			person.setSelectedPlan(plan);
			population.getPersons().put(person.getId(), person);
		}

		{ // person 2
			PersonImpl person = pb.createPerson(this.ids[2]);
			PlanImpl plan = pb.createPlan(person);
			ActivityImpl act1 = pb.createActivityFromLinkId("home", this.ids[1]);
			act1.setEndTime(Time.parseTime("07:06:00"));
			LegImpl leg1 = pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, stop3));
			ActivityImpl act2 = pb.createActivityFromLinkId("pt interaction", this.ids[3]);
			act2.setEndTime(Time.parseTime("07:06:00"));
			LegImpl leg2 = pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop3, tLine2, stop5));
			ActivityImpl act3 = pb.createActivityFromLinkId("pt interaction", this.ids[6]);
			
			plan.addActivity(act1);
			plan.addLeg(leg1);
			plan.addActivity(act2);
			plan.addLeg(leg2);
			plan.addActivity(act3);
			person.getPlans().add(plan);
			person.setSelectedPlan(plan);
			population.getPersons().put(person.getId(), person);
		}

		{ // person 3
			PersonImpl person = pb.createPerson(this.ids[3]);
			PlanImpl plan = pb.createPlan(person);
			ActivityImpl act1 = pb.createActivityFromLinkId("home", this.ids[1]);
			act1.setEndTime(Time.parseTime("07:11:00"));
			LegImpl leg1 = pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, stop3));
			ActivityImpl act2 = pb.createActivityFromLinkId("pt interaction", this.ids[3]);
			act2.setEndTime(Time.parseTime("07:11:00"));
			LegImpl leg2 = pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop3, tLine2, stop5));
			ActivityImpl act3 = pb.createActivityFromLinkId("pt interaction", this.ids[6]);
			
			plan.addActivity(act1);
			plan.addLeg(leg1);
			plan.addActivity(act2);
			plan.addLeg(leg2);
			plan.addActivity(act3);
			person.getPlans().add(plan);
			person.setSelectedPlan(plan);
			population.getPersons().put(person.getId(), person);
		}
	}

	private void runSim() {
		Events events = new Events();
		
		TransitQueueSimulation sim = new TransitQueueSimulation(this.scenario.getNetwork(), this.scenario.getPopulation(), events);
		sim.startOTFServer("two_lines_demo");
		sim.setTransitSchedule(this.schedule);
		
		OTFDemo.ptConnect("two_lines_demo");
		
		sim.run();
	}

	public void run() {
		createIds();
		prepareConfig();
		createNetwork();
		createTransitSchedule();
		createPopulation();
		runSim();
	}
	
	public static void main(String[] args) {
		new TwoLinesDemo().run();
	}

}
