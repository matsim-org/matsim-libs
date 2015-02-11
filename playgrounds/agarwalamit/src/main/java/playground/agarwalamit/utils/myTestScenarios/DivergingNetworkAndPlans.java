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
package playground.agarwalamit.utils.myTestScenarios;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * generates network with 6 links. Even persons will go on one branch (down) and odd persons will go on other (up).
 *<p>				  o----4----o
 *<p> 				  |
 *<p>				  3 
 *<p>				  |
 *<p>				  |
 *<p>  o--1---o---2---o
 *<p>				  |
 *<p>				  |
 *<p>				  5
 *<p>				  |
 *<p>				  o----5----o
 */
public class DivergingNetworkAndPlans {
	Scenario scenario;
	Config config;
	NetworkImpl network;
	Population population;
	Link link1;
	Link link2;
	Link link3;
	Link link4;
	Link link5;
	Link link6;

	public DivergingNetworkAndPlans(){
		config=ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.loadScenario(config);
		network =  (NetworkImpl) this.scenario.getNetwork();
		population = this.scenario.getPopulation();
	}

	void createNetwork(){
		final Set<String> allModesAllowed = new HashSet<String>();
		allModesAllowed.addAll(Arrays.asList("car","motorbike","pt", "bike", "walk"));

		Node node1 = network.createAndAddNode(Id.createNodeId("1"), this.scenario.createCoord(0, 0)) ;
		Node node2 = network.createAndAddNode(Id.createNodeId("2"), this.scenario.createCoord(100, 100));
		Node node3 = network.createAndAddNode(Id.createNodeId("3"), this.scenario.createCoord(300, 90));
		Node node4 = network.createAndAddNode(Id.createNodeId("4"), this.scenario.createCoord(500, 200));
		Node node5 = network.createAndAddNode(Id.createNodeId("5"), this.scenario.createCoord(700, 150));
		Node node6 = network.createAndAddNode(Id.createNodeId("6"), this.scenario.createCoord(500, 20));
		Node node7 = network.createAndAddNode(Id.createNodeId("7"), this.scenario.createCoord(700, 100));

		link1 = network.createAndAddLink(Id.createLinkId(String.valueOf("1")), node1, node2,1000.0,20.0,3600,1,null,"7");
		link1.setAllowedModes(allModesAllowed);
		link2 = network.createAndAddLink(Id.createLinkId(String.valueOf("2")), node2, node3,1000.0,20.0,3600,1,null,"7");
		link2.setAllowedModes(allModesAllowed);
		link3 = network.createAndAddLink(Id.createLinkId(String.valueOf("3")), node3, node4,10.0,20.0,360,1,null,"7");
		link3.setAllowedModes(allModesAllowed);
		link4 = network.createAndAddLink(Id.createLinkId(String.valueOf("4")), node4, node5,1000.0,20.0,3600,1,null,"7");
		link4.setAllowedModes(allModesAllowed);
		link5 = network.createAndAddLink(Id.createLinkId(String.valueOf("5")), node3, node6,1000.0,20.0,3600,1,null,"7");
		link5.setAllowedModes(allModesAllowed);
		link6 = network.createAndAddLink(Id.createLinkId(String.valueOf("6")), node6, node7,1000.0,20.0,3600,1,null,"7");
		link6.setAllowedModes(allModesAllowed);
	}

	void createPopulation(int numberOfPersons){

		for(int i=0;i<numberOfPersons;i++){
			Id<Person> id = Id.createPersonId(i);
			Person p = population.getFactory().createPerson(id);
			Plan plan = population.getFactory().createPlan();
			p.addPlan(plan);
			Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());
			a1.setEndTime(8*3600+i);
			Leg leg = population.getFactory().createLeg(TransportMode.car);
			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route;
			List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
			if(i%2==0) {
				route= (NetworkRoute) factory.createRoute(link1.getId(), link4.getId());
				linkIds.add(link2.getId());
				linkIds.add(link3.getId());
				route.setLinkIds(link1.getId(), linkIds, link4.getId());
				leg.setRoute(route);
				Activity a2 = population.getFactory().createActivityFromLinkId("w", link4.getId());
				plan.addActivity(a2);
			} else {
				route = (NetworkRoute) factory.createRoute(link1.getId(), link6.getId());
				linkIds.add(link2.getId());
				linkIds.add(link5.getId());
				route.setLinkIds(link1.getId(), linkIds, link6.getId());
				leg.setRoute(route);
				Activity a2 = population.getFactory().createActivityFromLinkId("w", link6.getId());
				plan.addActivity(a2);
			}
			population.addPerson(p);
		}
	}
}
