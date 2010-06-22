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

package playground.mrieser.core.sim.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;

/*package*/ class Fixture {

	private final Id[] ids = new Id[10];
	public final Scenario scenario;

	/*package*/ Fixture() {
		this.scenario = new ScenarioImpl();

		for (int i = 0; i < ids.length; i++) {
			this.ids[i] = this.scenario.createId(Integer.toString(i));
		}

		Network network = this.scenario.getNetwork();
		NetworkFactory netFactory = network.getFactory();
		network.addNode(netFactory.createNode(this.ids[0], this.scenario.createCoord(0, 0)));
		network.addNode(netFactory.createNode(this.ids[1], this.scenario.createCoord(500, 0)));
		network.addNode(netFactory.createNode(this.ids[2], this.scenario.createCoord(1000, 0)));
		network.addNode(netFactory.createNode(this.ids[3], this.scenario.createCoord(1500, 0)));
		network.addNode(netFactory.createNode(this.ids[4], this.scenario.createCoord(2000, 0)));
		network.addNode(netFactory.createNode(this.ids[5], this.scenario.createCoord(2500, 0)));
		network.addLink(setLinkAttributes(netFactory.createLink(this.ids[0], this.ids[0], this.ids[1]), 500.0, 3600.0, 10.0, 1));
		network.addLink(setLinkAttributes(netFactory.createLink(this.ids[1], this.ids[1], this.ids[2]), 500.0, 3600.0, 10.0, 1));
		network.addLink(setLinkAttributes(netFactory.createLink(this.ids[2], this.ids[2], this.ids[3]), 500.0, 3600.0, 10.0, 1));
		network.addLink(setLinkAttributes(netFactory.createLink(this.ids[3], this.ids[3], this.ids[4]), 500.0, 3600.0, 10.0, 1));
		network.addLink(setLinkAttributes(netFactory.createLink(this.ids[4], this.ids[4], this.ids[5]), 500.0, 3600.0, 10.0, 1));
	}

	public Person addPersonWithOneLeg() {
		Population population = this.scenario.getPopulation();
		PopulationFactory popFactory = population.getFactory();
		Person person1 = popFactory.createPerson(this.ids[0]);
		Plan plan = popFactory.createPlan();
		Activity homeAct1 = popFactory.createActivityFromLinkId("home", this.ids[0]);
		homeAct1.setEndTime(7.0*3600);
		plan.addActivity(homeAct1);
		Leg leg = popFactory.createLeg(TransportMode.car);
		NetworkRoute route = new LinkNetworkRouteImpl(this.ids[0], this.ids[2]);
		List<Id> linkIds = new ArrayList<Id>(1);
		Collections.addAll(linkIds, this.ids[1]);
		route.setLinkIds(this.ids[0], linkIds, this.ids[2]);
		leg.setRoute(route);
		plan.addLeg(leg);
		Activity workAct = popFactory.createActivityFromLinkId("work", this.ids[2]);
		workAct.setEndTime(16.0*3600);
		plan.addActivity(workAct);
		person1.addPlan(plan);
		population.addPerson(person1);
		return person1;
	}

	public Person addPersonWithTwoLegs() {
		Population population = this.scenario.getPopulation();
		PopulationFactory popFactory = population.getFactory();
		Person person2 = popFactory.createPerson(this.ids[1]);
		Plan plan = popFactory.createPlan();

		Activity homeAct1 = popFactory.createActivityFromLinkId("home", this.ids[0]);
		homeAct1.setEndTime(7.0*3600);
		plan.addActivity(homeAct1);

		Leg leg = popFactory.createLeg(TransportMode.car);
		NetworkRoute route = new LinkNetworkRouteImpl(this.ids[0], this.ids[2]);
		List<Id> linkIds = new ArrayList<Id>(1);
		Collections.addAll(linkIds, this.ids[1]);
		route.setLinkIds(this.ids[0], linkIds, this.ids[2]);
		leg.setRoute(route);
		plan.addLeg(leg);

		Activity workAct = popFactory.createActivityFromLinkId("work", this.ids[2]);
		workAct.setEndTime(16.0*3600);
		plan.addActivity(workAct);

		leg = popFactory.createLeg(TransportMode.car);
		route = new LinkNetworkRouteImpl(this.ids[2], this.ids[4]);
		linkIds = new ArrayList<Id>(1);
		Collections.addAll(linkIds, this.ids[3]);
		route.setLinkIds(this.ids[2], linkIds, this.ids[4]);
		leg.setRoute(route);
		plan.addLeg(leg);

		Activity homeAct2 = popFactory.createActivityFromLinkId("home", this.ids[4]);
		plan.addActivity(homeAct2);

		person2.addPlan(plan);
		population.addPerson(person2);
		return person2;
	}

	public Person addPersonWithTwoActsOnSameLink() {
		Population population = this.scenario.getPopulation();
		PopulationFactory popFactory = population.getFactory();
		Person person3 = popFactory.createPerson(this.ids[2]);
		Plan plan = popFactory.createPlan();

		Activity homeAct1 = popFactory.createActivityFromLinkId("home", this.ids[0]);
		homeAct1.setEndTime(7.0*3600);
		plan.addActivity(homeAct1);

		Leg leg = popFactory.createLeg(TransportMode.car);
		NetworkRoute route = new LinkNetworkRouteImpl(this.ids[0], this.ids[2]);
		List<Id> linkIds = new ArrayList<Id>(1);
		Collections.addAll(linkIds, this.ids[1]);
		route.setLinkIds(this.ids[0], linkIds, this.ids[2]);
		leg.setRoute(route);
		plan.addLeg(leg);

		Activity work1Act = popFactory.createActivityFromLinkId("work", this.ids[2]);
		work1Act.setEndTime(12.0*3600);
		plan.addActivity(work1Act);

		leg = popFactory.createLeg(TransportMode.car);
		route = new LinkNetworkRouteImpl(this.ids[2], this.ids[2]);
		linkIds = new ArrayList<Id>(1);
		route.setLinkIds(this.ids[2], linkIds, this.ids[2]);
		leg.setRoute(route);
		plan.addLeg(leg);

		Activity work2Act = popFactory.createActivityFromLinkId("leisure", this.ids[2]);
		work2Act.setEndTime(16.0*3600);
		plan.addActivity(work2Act);

		leg = popFactory.createLeg(TransportMode.car);
		route = new LinkNetworkRouteImpl(this.ids[2], this.ids[4]);
		linkIds = new ArrayList<Id>(1);
		Collections.addAll(linkIds, this.ids[3]);
		route.setLinkIds(this.ids[2], linkIds, this.ids[4]);
		leg.setRoute(route);
		plan.addLeg(leg);

		Activity homeAct2 = popFactory.createActivityFromLinkId("home", this.ids[4]);
		plan.addActivity(homeAct2);

		person3.addPlan(plan);
		population.addPerson(person3);
		return person3;
	}

	private static Link setLinkAttributes(final Link link, final double length, final double capacity, final double freespeed, final int nOfLanes) {
		link.setLength(length);
		link.setCapacity(capacity);
		link.setFreespeed(freespeed);
		link.setNumberOfLanes(nOfLanes);
		return link;
	}

}
