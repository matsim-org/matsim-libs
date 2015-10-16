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
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
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
	Scenario scenario;
	Config config;
	NetworkImpl network;
	Population population;
	Link link0;
	Link link1;
	Link link2;
	Link link3;
	Link link4;
	Link link5;

	 DivergingNetworkAndPlans(){
		config=ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.loadScenario(config);
		network =  (NetworkImpl) this.scenario.getNetwork();
		population = this.scenario.getPopulation();
	}

	 void createNetwork(){
		 Node node1 = network.createAndAddNode(Id.createNodeId("1"), new Coord((double) 0, (double) 0)) ;
		 Node node2 = network.createAndAddNode(Id.createNodeId("2"), new Coord((double) 100, (double) 100));
		 Node node3 = network.createAndAddNode(Id.createNodeId("3"), new Coord((double) 300, (double) 90));
		 Node node4 = network.createAndAddNode(Id.createNodeId("4"), new Coord((double) 500, (double) 200));
		 Node node5 = network.createAndAddNode(Id.createNodeId("5"), new Coord((double) 700, (double) 150));
		 Node node6 = network.createAndAddNode(Id.createNodeId("6"), new Coord((double) 500, (double) 20));
		 Node node7 = network.createAndAddNode(Id.createNodeId("7"), new Coord((double) 700, (double) 100));

		link0 = network.createAndAddLink(Id.createLinkId(String.valueOf("0")), node1, node2,1000.0,20.0,3600,1,null,"7");
		link1 = network.createAndAddLink(Id.createLinkId(String.valueOf("1")), node2, node3,100.0,40.0,3600,1,null,"7");
		link2 = network.createAndAddLink(Id.createLinkId(String.valueOf("2")), node3, node4,10.0,10.0,720,1,null,"7");
		link3 = network.createAndAddLink(Id.createLinkId(String.valueOf("3")), node4, node5,100.0,40.0,3600,1,null,"7");
		link4 = network.createAndAddLink(Id.createLinkId(String.valueOf("4")), node3, node6,100.0,40.0,3600,1,null,"7");
		link5 = network.createAndAddLink(Id.createLinkId(String.valueOf("5")), node6, node7,100.0,40.0,3600,1,null,"7");
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
			List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
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

