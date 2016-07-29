/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.juliakern.responsibilityOffline;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;


public class TestXmls {

	public static void main (String[] args){
		
		 String stream = "input/sample/events.xml";
		EventWriterXML mxw = new EventWriterXML(stream);
		
		for(int i=1; i<11; i++){
			for(int j = 1; j<11; j++){
			Id<Person> agentId = Id.create("person_"+i+"_"+j, Person.class);
			Id<Link> linkId = Id.create("link_"+i+"_"+j, Link.class);
			Event event = new ActivityStartEvent(0, agentId, linkId, null, "home");
			mxw.handleEvent(event);
			}
		}
		
		mxw.closeFile();
		System.out.println("done");
		
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Network network = (Network) sc.getNetwork();
		for(int i=0; i<10; i++){
			for(int j=0; j<10; j++ ){
				Coord coordA = new Coord((double) (i * 10 + 5), (double) (j * 10 + 5));
				String nodeAs = "Node "+i+"_"+j+"A";
				final Coord coord = coordA;
				//Node node1 = network.createAndAddNode(sc.createId("node 1"), sc.createCoord(-20000.0,     0.0));
				Node nodeA = NetworkUtils.createAndAddNode(network, Id.create(nodeAs, Node.class), coord);
				Coord coordB = new Coord((double) (i * 10 + 6), (double) (j * 10 + 6));
				String nodeBs = "Node "+i+"_"+j+"B";
				final Coord coord1 = coordB;
				Node nodeB = NetworkUtils.createAndAddNode(network, Id.create(nodeBs, Node.class), coord1);
				Id<Link> linkId = Id.create("link_"+i+"_"+j, Link.class);
				final Id<Link> id = linkId;
				final Node fromNode = nodeA;
				final Node toNode = nodeB;
				NetworkUtils.createAndAddLink(network,id, fromNode, toNode, 20., 30., (double) 3600, (double) 1, null, null);
			}
		}
		
		NetworkWriter nw = new NetworkWriter(network);
		nw.write("input/sample/test_network.xml");
		
		
		Population pop = sc.getPopulation();
		for(int i=1; i<11; i++){
			for(int j = 1; j<11; j++){
				Person person = pop.getFactory().createPerson(Id.create("person" +i+"_"+j, Person.class));
				Coord coord = new Coord((double) (i * 10 - 5), (double) (j * 10 - 5));
				Activity homeAct = pop.getFactory().createActivityFromCoord("home", coord );
				Plan plan = pop.getFactory().createPlan();
				plan.addActivity(homeAct);
				person.addPlan(plan);
				pop.addPerson(person);
			}
		}
		
		PopulationWriter pw = new PopulationWriter(pop, network);
		pw.write("input/sample/test_plans.xml");
		
	}
	
	
}
