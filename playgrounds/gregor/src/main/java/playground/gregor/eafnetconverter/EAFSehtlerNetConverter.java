/* *********************************************************************** *
 * project: org.matsim.*
 * EAFSehtlerNetConverter.java
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
package playground.gregor.eafnetconverter;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.utils.collections.Tuple;

public class EAFSehtlerNetConverter {
	
	
	public static void main (String [] args) {
		
		String net = "/home/laemmel/devel/EAF/data/padang_v2010_mit_shelter.xml.gz";
		String netOut ="/home/laemmel/devel/EAF/data/shelter_net.xml.gz";
		String plans ="/home/laemmel/devel/EAF/data/padang_plans_v2010_10s_shelters_EAF.xml.gz";
		String plansOut ="/home/laemmel/devel/EAF/data/shelter_plans.xml.gz";
		ScenarioImpl sc = new ScenarioImpl();
		new MatsimNetworkReader(sc).readFile(net);
		new PopulationReaderMatsimV4(sc).readFile(plans);
		
		extendNetwork(sc);
		
		modPlans(sc);
		
		new NetworkWriter(sc.getNetwork()).write(netOut);
		new PopulationWriter(sc.getPopulation(), sc.getNetwork()).write(plansOut);
		
	}

	private static void modPlans(ScenarioImpl sc) {

		for (Person pers : sc.getPopulation().getPersons().values()) {
			
			Plan plan = pers.getPlans().get(0);
			Leg leg = (Leg) plan.getPlanElements().get(1);
			ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(2);
			Link old = sc.getNetwork().getLinks().get(act.getLinkId());
			Node toNode = old.getToNode();
			if (!toNode.getId().toString().contains("shelter")) {
				continue;
			}
			Link newLink =sc.getNetwork().getLinks().get(toNode.getId());
			act.setLinkId(newLink.getId());
			LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();
			List<Id> links = route.getLinkIds();
			links.add(route.getEndLinkId());
			route.setLinkIds(route.getStartLinkId(), links, newLink.getId());
//			route.notify();
			
			
		}
		
	}

	private static void extendNetwork(ScenarioImpl sc) {
		NetworkImpl net = sc.getNetwork();
		List<Tuple<Node, Id>> nodes = new ArrayList<Tuple<Node,Id>>();
			
		
		
		for (Node node : net.getNodes().values()) {
			if (node.getId().toString().contains("shelter")) {
				IdImpl id = new IdImpl(node.getId().toString() +" b");
				nodes.add(new Tuple<Node,Id>(node,id));
			}
		}
		for (Tuple<Node,Id> tuple : nodes) {
			Node node = sc.getNetwork().createAndAddNode(tuple.getSecond(), tuple.getFirst().getCoord());
			sc.getNetwork().createAndAddLink(tuple.getFirst().getId(), tuple.getFirst(), node, 1, 1.66, 1, 1);
		}
		
	}

}
