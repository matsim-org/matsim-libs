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

/**
 * generates network with 6 links. Out of 6, first 4 person go up and next 2 go down.
 *<p>				  o----3----o
 *<p> 				  |
 *<p>				  2 
 *<p>				  |
 *<p>				  |
 *<p>  o--0---o---1---o
 *<p>				  |
 *<p>				  |
 *<p>				  4
 *<p>				  |
 *<p>				  o----5----o
 */
 class DivergingNetworkAndPlans {
	private final Scenario scenario;
	private final Config config;
	private final Network network;
	private final Population population;
	private Link link0;
	private Link link1;
	private Link link2;
	private Link link3;
	private Link link4;
	private Link link5;

	 DivergingNetworkAndPlans(){
		config=ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.loadScenario(config);
		network = this.scenario.getNetwork();
		population = this.scenario.getPopulation();
	}

	 void createNetwork(){
		 Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord((double) 0, (double) 0)) ;
		 Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord((double) 100, (double) 100));
		 Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord((double) 300, (double) 90));
		 Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("4"), new Coord((double) 500, (double) 200));
		 Node node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId("5"), new Coord((double) 700, (double) 150));
		 Node node6 = NetworkUtils.createAndAddNode(network, Id.createNodeId("6"), new Coord((double) 500, (double) 20));
		 Node node7 = NetworkUtils.createAndAddNode(network, Id.createNodeId("7"), new Coord((double) 700, (double) 100));
		final Node fromNode = node1;
		final Node toNode = node2;

		link0 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("0")), fromNode, toNode, 1000.0, 20.0, (double) 3600, (double) 1, null,
                "7");
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		link1 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("1")), fromNode1, toNode1, 100.0, 40.0, (double) 3600, (double) 1, null,
                "7");
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		link2 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("2")), fromNode2, toNode2, 10.0, 10.0, (double) 720, (double) 1, null,
                "7");
		final Node fromNode3 = node4;
		final Node toNode3 = node5;
		link3 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("3")), fromNode3, toNode3, 100.0, 40.0, (double) 3600, (double) 1, null,
                "7");
		final Node fromNode4 = node3;
		final Node toNode4 = node6;
		link4 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("4")), fromNode4, toNode4, 100.0, 40.0, (double) 3600, (double) 1, null,
                "7");
		final Node fromNode5 = node6;
		final Node toNode5 = node7;
		link5 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("5")), fromNode5, toNode5, 100.0, 40.0, (double) 3600, (double) 1, null,
                "7");
	}

	 void createPopulation(int numberOfPersons){

		for(int i=1;i<=numberOfPersons;i++){
			Id<Person> id = Id.createPersonId(i);
			Person p = population.getFactory().createPerson(id);
			Plan plan = population.getFactory().createPlan();
			p.addPlan(plan);
			Activity a1 = population.getFactory().createActivityFromLinkId("h", link0.getId());
			a1.setEndTime(0*3600+i-1);
			Leg leg = population.getFactory().createLeg(TransportMode.car);
			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route;
			List<Id<Link>> linkIds = new ArrayList<>();
			if(i <= 4 ) { // first 4 agents towards bottleneck and then 2 agents to other destination
				route= (NetworkRoute) factory.createRoute(link0.getId(), link3.getId());
				linkIds.add(link1.getId());
				linkIds.add(link2.getId());
				route.setLinkIds(link0.getId(), linkIds, link3.getId());
				leg.setRoute(route);
				Activity a2 = population.getFactory().createActivityFromLinkId("w", link3.getId());
				plan.addActivity(a2);
			} else {
				route = (NetworkRoute) factory.createRoute(link0.getId(), link5.getId());
				linkIds.add(link1.getId());
				linkIds.add(link4.getId());
				route.setLinkIds(link0.getId(), linkIds, link5.getId());
				leg.setRoute(route);
				Activity a2 = population.getFactory().createActivityFromLinkId("w", link5.getId());
				plan.addActivity(a2);
			}
			population.addPerson(p);
		}
	}

	 Scenario getDesiredScenario() {
		return this.scenario;
	}
}

