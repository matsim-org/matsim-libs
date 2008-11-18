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
import java.util.LinkedList;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkReaderMatsimV1;
import org.matsim.network.Node;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

import playground.dressler.Intervall.src.Intervalls.EdgeIntervalls;

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
	
	//TODO finish
	/*
	private static HashMap<Node,Integer> readPopulation(NetworkLayer network, String filename){
		Population population = new Population();
		MatsimPopulationReader reader = new MatsimPopulationReader(population);
		reader.readFile(filename);
		HashMap<Node,Integer> allnodes = new HashMap<Node,Integer>();
		for(Person person : population.getPersons().values() ){
			Plan plan = person.getPlans().get(0);
			plan.getFirstActivity();
		}
		
		return null;
	}
	*/
	
	/**
	 * main method to run an EAF algorithm on the specified cenarion
	 * @param args b
	 * 
	 */
	public static void main(String[] args) {
		 System.out.println("Ich lebe");
		 NetworkLayer network = new NetworkLayer();
		 NetworkReaderMatsimV1 networkReader = new NetworkReaderMatsimV1(network);
		 HashMap<Link, EdgeIntervalls> flow;
		 HashMap<Node, Integer> demands;
		 int timeHorizon = 90;
		 
		//Read the data 
		//networkReader.readFile("/homes/combi/Projects/ADVEST/code/matsim/examples/meine_EA/inken_xmas_network.xml");
		networkReader.readFile("/Users/manuel/Documents/meine_EA/manu2.xml");
		try {
			demands = readDemands(network, "/Users/manuel/Documents/meine_EA/manu2.dem");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		flow = new HashMap<Link, EdgeIntervalls>();
		for(Link link : network.getLinks().values()){
			int l = (int)link.getLength()/(int)link.getFreespeed(1.);//TOTO traveltime
			flow.put(link, new EdgeIntervalls(l));
			//TODO achtung cast von double auf int
		}
		
		// find source and sink
		LinkedList<Node> sources = new LinkedList<Node>();
		sources.addAll(demands.keySet());
		
		Node sink = network.getNode("5");
		Path result;
		if (sources.isEmpty() || sink == null) {
			System.out.println("nicht da");
		} else {
			TravelCost travelcost = new FakeTravelTimeCost();
			TravelTime traveltime = (TravelTime) travelcost;

			//Flow fluss = new Flow(network, flow, sources, demands, sink, timeHorizon);
			Flow fluss = new Flow(network,null, flow, sources, demands, sink, timeHorizon);
			BellmanFordVertexIntervalls routingAlgo = new BellmanFordVertexIntervalls(fluss);
			BellmanFordVertexIntervalls.debug(true);
			for (int i=0; i<20000; i++){
				result = routingAlgo.doCalculations();
				if (result==null){
					break;
				}
				System.out.println("path: " +  result);
				fluss.augment(result);
				System.out.println(fluss);
			}
			System.out.println(fluss.arrivalsToString());
			System.out.println(fluss.arrivalPatternToString());
		}
		System.out.println("demands:");
		for (Node node : demands.keySet()){
			System.out.println("node:" + node.getId().toString()+ " demand:" + demands.get(node));
		}
   	    System.out.println("... immer noch!\n");
	}

}
