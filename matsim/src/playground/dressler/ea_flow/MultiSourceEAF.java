/* *********************************************************************** *
 * project: org.matsim.*
 * FakeTravelTimeCost.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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


/**
 * 
 */
package playground.dressler.ea_flow;


import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkReaderMatsimV1;
import org.matsim.network.Node;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

import playground.dressler.Intervall.src.Intervalls.EdgeIntervalls;

/**
 * @author Manuel Schneider
 *
 */
public class MultiSourceEAF {
	
	/**
	 * @param args blub
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		 System.out.println("Ich lebe");
		 NetworkLayer network = new NetworkLayer();
		 NetworkReaderMatsimV1 networkReader = new NetworkReaderMatsimV1(network);
		 
		 HashMap<Link, EdgeIntervalls> flow;
		 int timeHorizon = 6;
		 
		 //TODO choose the one you need
		 //networkReader.readFile("/homes/combi/olthoff/.eclipse/Matsim/examples/equil/network.xml");
		 //networkReader.readFile("/homes/combi/olthoff/.eclipse/Matsim/examples/two-routes/network.xml");
		 //networkReader.readFile("/homes/combi/olthoff/.eclipse/Matsim/examples/roundabout/network.xml");
		 //networkReader.readFile("C:/Documents and Settings/Administrator/workspace/matsim/examples/equil/network.xml");
		 //networkReader.readFile("C:/Documents and Settings/Administrator/workspace/matsim/examples/two-routes/network.xml");
		 //networkReader.readFile("C:/Documents and Settings/Administrator/workspace/matsim/examples/roundabout/network.xml");
		 networkReader.readFile("/homes/combi/Projects/ADVEST/code/matsim/examples/meine_EA/inken_xmas_network.xml");		 
		 
		// CODE
		flow = new HashMap<Link, EdgeIntervalls>();
		for(Link link : network.getLinks().values()){
			//System.out.println(link.getId().toString());
			flow.put(link, new EdgeIntervalls((int)link.getLength()));
			//TODO achtung cast von double auf int
		}
		
		// find source and sink
		Node source = network.getNode("0_erste_source");
		LinkedList<Node> sources = new LinkedList<Node>();
		sources.add(source);
		/*Id maxId = source.getId();
		for(Node node : network.getNodes().values()){
			if(Integer.parseInt(node.getId().toString()) > Integer.parseInt(maxId.toString())){
				maxId = node.getId();
			}
		}*/
		Node sink = network.getNode("5_zweite_sink");
		
		if (source == null || sink == null) {
			System.out.println("nicht da");
		} else {
			TravelCost travelcost = new FakeTravelTimeCost();
			TravelTime traveltime = (TravelTime) travelcost;

			BellmanFordVertexIntervalls routingAlgo = new BellmanFordVertexIntervalls(network, travelcost, traveltime, flow, timeHorizon,sink, sources);
			routingAlgo.setDebugMode(true);
			routingAlgo.doCalculations();
		}
   	    System.out.println("... immer noch!\n");
	}

}
