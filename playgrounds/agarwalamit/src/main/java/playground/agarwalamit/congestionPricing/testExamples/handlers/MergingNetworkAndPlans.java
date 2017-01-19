/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.congestionPricing.testExamples.handlers;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author amit
 */

class MergingNetworkAndPlans {

	/**
	 * generates network with 3 links. 
	 */
    final Scenario scenario;
	final Config config;
	final Network network;
	final Population population;
	Link link1;
	Link link2;
	Link link3;
	Link link4;
	Link link5;

	MergingNetworkAndPlans(){
		config=ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.loadScenario(config);
		network = this.scenario.getNetwork();
		population = this.scenario.getPopulation();
	}

	void createNetwork(){

		Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord((double) 0, (double) 0)) ;
		Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord((double) 0, (double) 100));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord((double) 500, (double) 150));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("4"), new Coord((double) 1000, (double) 100));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId("5"), new Coord((double) 1000, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;

		link1 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("1")), fromNode, toNode, 100.0, 20.0, (double) 3600, (double) 1, null,
                "7");
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		link2 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("2")), fromNode1, toNode1, 100.0, 110.0, (double) 360, (double) 1, null,
                "7");
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		link3 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("3")), fromNode2, toNode2, 5.0, 6.0, (double) 900, (double) 1, null,
                "7");
		final Node fromNode3 = node4;
		final Node toNode3 = node5;
		link4 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("4")), fromNode3, toNode3, 100.0, 20.0, (double) 3600, (double) 1, null,
                "7");
		final Node fromNode4 = node1;
		final Node toNode4 = node3;

		link5 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("5")), fromNode4, toNode4, 100.0, 20.0, (double) 3600, (double) 1, null,
                "7");
	}

	void createPopulation(){

		/*Alternative persons from different links*/

		for(int i=1;i<3;i++){

			Id<Person> id = Id.createPersonId(i);
			Person p = population.getFactory().createPerson(id);
			Plan plan = population.getFactory().createPlan();
			p.addPlan(plan);

			Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());
			a1.setEndTime(0+i-1);
			Leg leg = population.getFactory().createLeg(TransportMode.car);
			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory1 = new LinkNetworkRouteFactory();
			NetworkRoute route1;
			List<Id<Link>> linkIds = new ArrayList<>();
			route1= (NetworkRoute) factory1.createRoute(link1.getId(), link4.getId());
			linkIds.add(link2.getId());
			linkIds.add(link3.getId());
			route1.setLinkIds(link1.getId(), linkIds, link4.getId());
			leg.setRoute(route1);
			Activity a2 = population.getFactory().createActivityFromLinkId("w", link4.getId());
			plan.addActivity(a2);
			population.addPerson(p);
		}

		for(int i=3;i<5;i++) {
			Id<Person> id = Id.createPersonId(i);
			Person p = population.getFactory().createPerson(id);
			Plan plan = population.getFactory().createPlan();
			p.addPlan(plan);

			Activity a1 = population.getFactory().createActivityFromLinkId("h", link5.getId());
			a1.setEndTime(6+i);
			Leg leg = population.getFactory().createLeg(TransportMode.car);
			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory1 = new LinkNetworkRouteFactory();
			NetworkRoute route1;
			List<Id<Link>> linkIds = new ArrayList<>();
			route1= (NetworkRoute) factory1.createRoute(link5.getId(), link4.getId());
			linkIds.add(link3.getId());
			route1.setLinkIds(link5.getId(), linkIds, link4.getId());
			leg.setRoute(route1);
			Activity a2 = population.getFactory().createActivityFromLinkId("w", link4.getId());
			plan.addActivity(a2);
			population.addPerson(p);
		}
	}

	Scenario getDesiredScenario(){
		return this.scenario;
	}
}
