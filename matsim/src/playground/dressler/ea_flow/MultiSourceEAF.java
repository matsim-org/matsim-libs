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


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;

/**
 * @author Manuel Schneider
 *
 */
public class MultiSourceEAF {
	
	/**
	 * debug flag
	 */
	private static boolean _debug = false;
	
	
	/**
	 * A method to read a file containing the information on demands in an evacuation scenario for a given network
	 * the syntax of the file is as follows:
	 * every line contains the ID of a node which must be contained in the network and its demand seperated by ";"
	 * @param network the network for which the demands should be read	
	 * @param filename the path of the demands file
	 * @return A HashMap<Node,Integer> containing the demands for every node in the file
	 * @throws IOException if file reading fails
	 */
	private static HashMap<Node,Integer> readDemands(NetworkLayer network, String filename) throws IOException{
			BufferedReader in = new BufferedReader(new FileReader(filename));
			HashMap<Node,Integer> demands = new HashMap<Node,Integer>();
			String inline = null;
			while ((inline = in.readLine()) != null) {
				String[] line = inline.split(";");
				Node node = network.getNode(line[0].trim()); 
				int d = Integer.valueOf(line[1].trim());
				demands.put(node, d);
			}
		return demands;
	}
	
	

	/**
	 * generates demand from an population by placing demand 1 for every person on the node in the Persons first plan first activity edges ToNode
	 * @param network network for the demands node
	 * @param filename path of the Population file
	 * @return 
	 */
	private static HashMap<Node,Integer> readPopulation(NetworkLayer network, String filename){
		Population population = new Population(Population.NO_STREAMING);
		new MatsimPopulationReader(population,network).readFile(filename);
		network.connect();
		HashMap<Node,Integer> allnodes = new HashMap<Node,Integer>();
		
		for(Person person : population.getPersons().values() ){
			
			Plan plan = person.getPlans().get(0);
			if(plan.getFirstActivity().getLinkId()==null){
				continue;
			}
			
			Node node = network.getLink(plan.getFirstActivity().getLinkId()).getToNode();
			if(allnodes.containsKey(node)){
				int temp = allnodes.get(node);
				allnodes.put(node, temp + 1);
			}else{
				allnodes.put(node, 1);
			}
		}
		
		return allnodes;
	}
	
	
	/**
	 * main method to run an EAF algorithm on the specified cenario
	 * @param args b
	 * 
	 */
	public static void main(String[] args) {
		 System.out.println("Ich lebe");
		 
		 
		 //read network
		 NetworkLayer network = new NetworkLayer();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
		 //networkReader.readFile("/Users/manuel/Documents/meine_EA/manu/manu2.xml");
		//networkReader.readFile("/homes/combi/Projects/ADVEST/code/matsim/examples/meine_EA/inken_xmas_network.xml");
		networkReader.readFile("/homes/combi/Projects/ADVEST/padang/network/padang_net_evac.xml");
		
		 //read demands 
		 HashMap<Node, Integer> demands;
		 try {
				//demands = readDemands(network, "/Users/manuel/Documents/meine_EA/manu/manu2.dem");
				demands = readPopulation(network, "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_10p.xml.gz");
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			
		//set parameters
		int timeHorizon = 2000000;
		Node sink = network.getNode("en2");
		TimeExpandedPath result = null;
		int rounds = 1000;
		
		//check if demands and sink are set 
		if (demands.isEmpty() || sink == null) {
			System.out.println("nicht da");
		} else {
			FakeTravelTimeCost travelcost = new FakeTravelTimeCost();
			

			//Flow fluss = new Flow(network, flow, sources, demands, sink, timeHorizon);
			Flow fluss = new Flow( network, travelcost, demands, sink, timeHorizon );
			BellmanFordVertexIntervalls routingAlgo = new BellmanFordVertexIntervalls(fluss);
			BellmanFordVertexIntervalls.debug(false);
			for (int i=0; i<rounds; i++){
				result = routingAlgo.doCalculations();
				if (result==null){
					break;
				}
				System.out.println("TimeExpandedPath: " +  result);
				fluss.augment(result);
				//System.out.println(fluss);
			}
			System.out.println(fluss.arrivalsToString());
			System.out.println(fluss.arrivalPatternToString());
		}
		/*System.out.println("demands:");
		for (Node node : demands.keySet()){
			System.out.println("node:" + node.getId().toString()+ " demand:" + demands.get(node));
		}*/
   	    System.out.println("... immer noch!\n");
	}

}
