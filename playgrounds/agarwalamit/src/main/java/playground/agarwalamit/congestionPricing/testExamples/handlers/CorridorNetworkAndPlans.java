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

public class CorridorNetworkAndPlans {


	/**
	 * generates network with 3 links. 
	 *<p>			
	 *<p>  o--0---o---1---o---2---o---3---o
	 *<p>				  
	 */
	Scenario scenario;
	Config config;
	NetworkImpl network;
	Population population;
	
	Link link0;
	Link link1;
	Link link2;
	Link link3;

	public CorridorNetworkAndPlans(){
		config=ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.loadScenario(config);
		network =  (NetworkImpl) this.scenario.getNetwork();
		population = this.scenario.getPopulation();
	}

	public void createNetwork(){

		Node node1 = network.createAndAddNode(Id.createNodeId("1"), this.scenario.createCoord(0, 0)) ;
		Node node2 = network.createAndAddNode(Id.createNodeId("2"), this.scenario.createCoord(100, 10));
		Node node3 = network.createAndAddNode(Id.createNodeId("3"), this.scenario.createCoord(300, -10));
		Node node4 = network.createAndAddNode(Id.createNodeId("4"), this.scenario.createCoord(500, 20));
		Node node5 = network.createAndAddNode(Id.createNodeId("5"), this.scenario.createCoord(700, 0));

		link0 = network.createAndAddLink(Id.createLinkId(String.valueOf("0")), node1, node2, 1000.0, 20.0, 3600.,1,null,"7");
		link1 = network.createAndAddLink(Id.createLinkId(String.valueOf("1")), node2, node3, 100.0, 40.0, 3600.,1,null,"7");
		link2 = network.createAndAddLink(Id.createLinkId(String.valueOf("2")), node3, node4, 10.0, 9.0, 900.,1,null,"7");
		link3 = network.createAndAddLink(Id.createLinkId(String.valueOf("3")), node4, node5, 1000.0, 20.0, 3600.,1,null,"7");
	}

	public void createPopulation(int numberOfPersons){

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
			route= (NetworkRoute) factory.createRoute(link0.getId(), link3.getId());
			linkIds.add(link1.getId());
			linkIds.add(link2.getId());
			linkIds.add(link3.getId());
			route.setLinkIds(link0.getId(), linkIds, link3.getId());
			leg.setRoute(route);
			Activity a2 = population.getFactory().createActivityFromLinkId("w", link3.getId());
			plan.addActivity(a2);
			population.addPerson(p);
		}
	}

	public Scenario getDesiredScenario(){
		return this.scenario;
	}
}
