/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.tnicolai.matsim4opus.alpha;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.LeastCostPathTree;
import org.matsim.utils.LeastCostPathTree.NodeData;

public class TestSpanningTree {
	
	public static void main(String[] args) {
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile("/Users/thomas/Development/opus_home/data/psrc/network/psrc.xml.gz");
		Network network = scenario.getNetwork();
		
		// init spanning tree here
		TravelTime ttc = new TravelTimeCalculator(network,60,30*3600, scenario.getConfig().travelTimeCalculator()).getLinkTravelTimes();
		LeastCostPathTree lcpt = new LeastCostPathTree(ttc,new TravelTimeAndDistanceBasedTravelDisutility(ttc, scenario.getConfig().planCalcScore()));
		
		// than set the start node
		Node origin = network.getNodes().get(new IdImpl(4224));
		lcpt.calculate(network, origin, 8*3600);

		Map<Id, NodeData> tree = lcpt.getTree();
		
		// now set the destination node
		Node destination = network.getNodes().get(new IdImpl(2176));
		
		List<Id> nodeList = new ArrayList<Id>();
								// set destination node ...
								// ... from there we get the route to the origin node by the following iteration
		Id tmpNode = destination.getId();
		while(true){
			
			nodeList.add(tmpNode);
			
			NodeData nodeData = tree.get(tmpNode);
			assert(nodeData != null);
			tmpNode = nodeData.getPrevNodeId();
			
			if(tmpNode == null)
				break;
		}
		System.out.println();
		for(Id node: nodeList)
			System.out.println(node);
		
		// create a link list out of that ...
		List<Link> linkList = RouteUtils.getLinksFromNodeIds(network, nodeList);
		List<Id<Link>> linkIdList = new ArrayList<Id<Link>>();
		
		System.out.println();
		for(Link link: linkList){
			System.out.println(link.getId());
			linkIdList.add(link.getId());
		}
		
		// ... to calculate the distance	
		
		NetworkRoute nwr= RouteUtils.createNetworkRoute(linkIdList, network);
		double distance1 = RouteUtils.calcDistance(nwr, network);
		
		// or alternately
		double distance2 = 0.;
		for(Link link: linkList)
			distance2 += link.getLength();
		double distanceLink1 = linkList.get(0).getLength();
		double distanceLink2 = linkList.get(linkList.size()-1).getLength();
		
		
		System.out.println("Distance1 is : " + distance1);
		System.out.println("Distance2 is : " + distance2 + " without origin and end link distance this is " + (distance2 - (distanceLink1+distanceLink2)));
		
		
//		System.out.println("Node ID\t TravelTime \t TravelCost\t PreviousNode ID");
//		for (Id id : tree.keySet()) {
//			NodeData d = tree.get(id);
//			
//			
//			
//			if (d.getPrevNode() != null) {
//				System.out.println(id+"\t"+d.getTime()+"\t"+d.getCost()+"\t"+d.getPrevNode().getId());
//			}
//			else {
//				System.out.println(id+"\t"+d.getTime()+"\t"+d.getCost()+"\t"+"0");
//			}
//		}
	}
	

}
