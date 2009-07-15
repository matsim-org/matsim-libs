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
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.api.experimental.population.PopulationBuilder;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitScheduleBuilderImpl;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleBuilder;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.marcel.OTFDemo;
import playground.marcel.pt.integration.ExperimentalTransitRoute;
import playground.marcel.pt.integration.TransitQueueSimulation;

public class TwoLinesDemo {
	
	private final ScenarioImpl scenario = new ScenarioImpl();
	private final TransitScheduleBuilder builder = new TransitScheduleBuilderImpl();
	private final TransitSchedule schedule = this.builder.createTransitSchedule();
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
		NetworkLayer network = this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		NodeImpl node1 = network.createNode(this.ids[1], this.scenario.createCoord(-2000, 0));
		NodeImpl node2 = network.createNode(this.ids[2], this.scenario.createCoord(-2000, 1000));
		NodeImpl node3 = network.createNode(this.ids[3], this.scenario.createCoord(-1000, 0));
		NodeImpl node4 = network.createNode(this.ids[4], this.scenario.createCoord(-1000, 1000));
		NodeImpl node5 = network.createNode(this.ids[5], this.scenario.createCoord(0, 0));
		NodeImpl node6 = network.createNode(this.ids[6], this.scenario.createCoord(0, 1000));
		NodeImpl node7 = network.createNode(this.ids[7], this.scenario.createCoord(500, 500));
		NodeImpl node8 = network.createNode(this.ids[8], this.scenario.createCoord(1500, 500));
		NodeImpl node9 = network.createNode(this.ids[9], this.scenario.createCoord(2500, 500));
		NodeImpl node10 = network.createNode(this.ids[10], this.scenario.createCoord(3500, 500));
		NodeImpl node11 = network.createNode(this.ids[11], this.scenario.createCoord(4000, 1000));
		NodeImpl node12 = network.createNode(this.ids[12], this.scenario.createCoord(4000, 0));
		NodeImpl node13 = network.createNode(this.ids[13], this.scenario.createCoord(5000, 1000));
		NodeImpl node14 = network.createNode(this.ids[14], this.scenario.createCoord(5000, 0));
		
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
		TransitStopFacility stop1 = this.builder.createTransitStopFacility(this.ids[1], this.scenario.createCoord(-100, -50));
		TransitStopFacility stop2 = this.builder.createTransitStopFacility(this.ids[2], this.scenario.createCoord(-100, 850));
		TransitStopFacility stop3 = this.builder.createTransitStopFacility(this.ids[3], this.scenario.createCoord(1400, 450));
		TransitStopFacility stop4 = this.builder.createTransitStopFacility(this.ids[4], this.scenario.createCoord(3400, 450));
		TransitStopFacility stop5 = this.builder.createTransitStopFacility(this.ids[5], this.scenario.createCoord(3900, 50));
		TransitStopFacility stop6 = this.builder.createTransitStopFacility(this.ids[6], this.scenario.createCoord(3900, 850));
		
		LinkImpl link1 = this.scenario.getNetwork().getLinks().get(this.ids[1]);
		LinkImpl link2 = this.scenario.getNetwork().getLinks().get(this.ids[2]);
		LinkImpl link3 = this.scenario.getNetwork().getLinks().get(this.ids[3]);
		LinkImpl link4 = this.scenario.getNetwork().getLinks().get(this.ids[4]);
		LinkImpl link5 = this.scenario.getNetwork().getLinks().get(this.ids[5]);
		LinkImpl link6 = this.scenario.getNetwork().getLinks().get(this.ids[6]);
		LinkImpl link7 = this.scenario.getNetwork().getLinks().get(this.ids[7]);
		LinkImpl link8 = this.scenario.getNetwork().getLinks().get(this.ids[8]);
		LinkImpl link9 = this.scenario.getNetwork().getLinks().get(this.ids[9]);
		LinkImpl link10 = this.scenario.getNetwork().getLinks().get(this.ids[10]);
		LinkImpl link11 = this.scenario.getNetwork().getLinks().get(this.ids[11]);
		LinkImpl link12 = this.scenario.getNetwork().getLinks().get(this.ids[12]);
		LinkImpl link13 = this.scenario.getNetwork().getLinks().get(this.ids[13]);

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


		TransitLine tLine1 = this.builder.createTransitLine(this.ids[1]);
		NetworkRoute networkRoute = (NetworkRoute) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, link1, link13);
		ArrayList<LinkImpl> linkList = new ArrayList<LinkImpl>(6);
		linkList.add(link3);
		linkList.add(link5);
		linkList.add(link7);
		linkList.add(link8);
		linkList.add(link9);
		linkList.add(link11);
		networkRoute.setLinks(link1, linkList, link13);
		ArrayList<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>(4);
		stopList.add(this.builder.createTransitRouteStop(stop1, 0, 0));
		stopList.add(this.builder.createTransitRouteStop(stop3, 90, 100));
		stopList.add(this.builder.createTransitRouteStop(stop4, 290, 300));
		stopList.add(this.builder.createTransitRouteStop(stop5, 390, Time.UNDEFINED_TIME));
		TransitRoute tRoute1 = this.builder.createTransitRoute(this.ids[1], networkRoute, stopList, TransportMode.bus);
		tLine1.addRoute(tRoute1);
		
		tRoute1.addDeparture(this.builder.createDeparture(this.ids[1], Time.parseTime("07:00:00")));
		tRoute1.addDeparture(this.builder.createDeparture(this.ids[2], Time.parseTime("07:05:00")));
		tRoute1.addDeparture(this.builder.createDeparture(this.ids[3], Time.parseTime("07:10:00")));
		tRoute1.addDeparture(this.builder.createDeparture(this.ids[4], Time.parseTime("07:15:00")));
		tRoute1.addDeparture(this.builder.createDeparture(this.ids[5], Time.parseTime("07:20:00")));
		tRoute1.addDeparture(this.builder.createDeparture(this.ids[6], Time.parseTime("07:25:00")));
		
		this.schedule.addTransitLine(tLine1);

		TransitLine tLine2 = this.builder.createTransitLine(this.ids[2]);
		networkRoute = (NetworkRoute) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, link2, link12);
		linkList = new ArrayList<LinkImpl>(6);
		linkList.add(link4);
		linkList.add(link6);
		linkList.add(link7);
		linkList.add(link8);
		linkList.add(link9);
		linkList.add(link10);
		networkRoute.setLinks(link2, linkList, link12);
		stopList = new ArrayList<TransitRouteStop>(4);
		stopList.add(this.builder.createTransitRouteStop(stop2, 0, 0));
		stopList.add(this.builder.createTransitRouteStop(stop3, 90, 100));
		stopList.add(this.builder.createTransitRouteStop(stop4, 290, 300));
		stopList.add(this.builder.createTransitRouteStop(stop6, 390, Time.UNDEFINED_TIME));
		TransitRoute tRoute2 = this.builder.createTransitRoute(this.ids[1], networkRoute, stopList, TransportMode.bus);
		tLine2.addRoute(tRoute2);

		tRoute2.addDeparture(this.builder.createDeparture(this.ids[1], Time.parseTime("07:02:00")));
		tRoute2.addDeparture(this.builder.createDeparture(this.ids[2], Time.parseTime("07:12:00")));
		tRoute2.addDeparture(this.builder.createDeparture(this.ids[3], Time.parseTime("07:22:00")));
		
		this.schedule.addTransitLine(tLine2);
	}

	private void createPopulation() {
		PopulationImpl population = this.scenario.getPopulation();
		PopulationBuilder pb = population.getBuilder();
		
		TransitLine tLine1 = this.schedule.getTransitLines().get(this.ids[1]);
		TransitLine tLine2 = this.schedule.getTransitLines().get(this.ids[2]);
		
		TransitStopFacility stop1 = this.schedule.getFacilities().get(this.ids[1]);
		TransitStopFacility stop2 = this.schedule.getFacilities().get(this.ids[2]);
		TransitStopFacility stop3 = this.schedule.getFacilities().get(this.ids[3]);
		TransitStopFacility stop4 = this.schedule.getFacilities().get(this.ids[4]);
		TransitStopFacility stop5 = this.schedule.getFacilities().get(this.ids[5]);
		TransitStopFacility stop6 = this.schedule.getFacilities().get(this.ids[6]);
		
		{ // person 1
			PersonImpl person = (PersonImpl) pb.createPerson(this.ids[1]);
			PlanImpl plan = (PlanImpl) pb.createPlan(person);
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", this.ids[1]);
			act1.setEndTime(Time.parseTime("07:01:00"));
			LegImpl leg1 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, stop3));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", this.ids[3]);
			act2.setEndTime(Time.parseTime("07:01:00"));
			LegImpl leg2 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop3, tLine2, stop5));
			ActivityImpl act3 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", this.ids[6]);
			
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
			PersonImpl person = (PersonImpl) pb.createPerson(this.ids[2]);
			PlanImpl plan = (PlanImpl) pb.createPlan(person);
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", this.ids[1]);
			act1.setEndTime(Time.parseTime("07:06:00"));
			LegImpl leg1 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, stop3));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", this.ids[3]);
			act2.setEndTime(Time.parseTime("07:06:00"));
			LegImpl leg2 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop3, tLine2, stop5));
			ActivityImpl act3 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", this.ids[6]);
			
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
			PersonImpl person = (PersonImpl) pb.createPerson(this.ids[3]);
			PlanImpl plan = (PlanImpl) pb.createPlan(person);
			ActivityImpl act1 = (ActivityImpl) pb.createActivityFromLinkId("home", this.ids[1]);
			act1.setEndTime(Time.parseTime("07:11:00"));
			LegImpl leg1 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg1.setRoute(new ExperimentalTransitRoute(stop1, tLine1, stop3));
			ActivityImpl act2 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", this.ids[3]);
			act2.setEndTime(Time.parseTime("07:11:00"));
			LegImpl leg2 = (LegImpl) pb.createLeg(TransportMode.pt);
			leg2.setRoute(new ExperimentalTransitRoute(stop3, tLine2, stop5));
			ActivityImpl act3 = (ActivityImpl) pb.createActivityFromLinkId("pt interaction", this.ids[6]);
			
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
	
	public static void main(final String[] args) {
		new TwoLinesDemo().run();
	}

}
