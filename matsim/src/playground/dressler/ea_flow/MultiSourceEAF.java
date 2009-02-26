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

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicPopulation;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriterV5;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.world.World;

import playground.dressler.Intervall.src.Intervalls.EdgeIntervalls;
import playground.dressler.Intervall.src.Intervalls.VertexIntervalls;

/**
 * @author Manuel Schneider
 *
 */
public class MultiSourceEAF {
	
	/**
	 * debug flag
	 */
	private static boolean _debug = false;
	private static boolean vertexAlgo = true;
	
	
	public static void debug(boolean debug){
		_debug=debug;
	}
	
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
			
			//set debuging modes
			MultiSourceEAF.debug(true);
			BellmanFordVertexIntervalls.debug(0);
			VertexIntervalls.debug(false);
			//VertexIntervall.debug(false);
			EdgeIntervalls.debug(false);
			//EdgeIntervall.debug(false);
			Flow.debug(0);
			
		
		if(_debug){
			System.out.println("starting to read input");
		}
		
		String networkfile = null;
		networkfile = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_100p_flow_10s_cap.xml";
		//networkfile = "/Users/manuel/Documents/meine_EA/manu/manu2.xml";
		//networkfile = "./examples/meine_EA/siouxfalls_network_5s.xml";
		
		
		String plansfile = null;
		plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_10p.xml.gz";
		//plansfile ="/homes/combi/Projects/ADVEST/code/matsim/examples/meine_EA/siouxfalls_plans_simple.xml";
		
		
		
		String demandsfile = null;
		//demandsfile = "/Users/manuel/Documents/meine_EA/manu/manu2.dem";
		
		String outputplansfile = null;
		//outputplansfile = "/homes/combi/dressler/V/code/workspace/matsim/examples/meine_EA/padangplans_10p_5s.xml";
		//outputplansfile = "./examples/meine_EA/siouxfalls_plans_5s_demand_100_emptylegs.xml";
		outputplansfile = "./examples/meine_EA/padang_plans_100p_flow_10s_test.xml";
		
		int uniformDemands = 100;
		
		//set parameters
		int timeHorizon = 200000;
		int rounds = 100000;
		String sinkid = "en2";
		boolean emptylegs = false;
		
		
		//read network
		NetworkLayer network = new NetworkLayer();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
		networkReader.readFile(networkfile);
		Node sink = network.getNode(sinkid);
		
		//read demands 
		HashMap<Node, Integer> demands;
		if(plansfile!=null){
			demands = readPopulation(network, plansfile);
		}else if (demandsfile != null){
			try {
				demands = readDemands(network,demandsfile);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		} else {
			// uniform demands
			demands = new HashMap<Node, Integer>();
			for (Node node : network.getNodes().values()) {
				if (!node.getId().equals(sink.getId())) {
				  demands.put(node, Math.max(uniformDemands,0));
				}
			}
		}
		
		//check if demands and sink are set 
		if (demands.isEmpty() ) {
			System.out.println("demands not found");
		}
		if (sink == null){
			System.out.println("sink not found");
		}
		if(_debug){
			System.out.println("reading input done");
		}
		
		if(!demands.isEmpty() && (sink != null)) {
			TimeExpandedPath result = null;
			FakeTravelTimeCost travelcost = new FakeTravelTimeCost();
			Flow fluss = new Flow( network, travelcost, demands, sink, timeHorizon );
			
			if(_debug){
				System.out.println("starting calculations");
			}
			
			
			long timeMBF = 0;
			long timeAugment = 0;
			long timer1, timer2, timer3;
			long timeStart = System.currentTimeMillis();
			
			//main loop for calculations
			if(vertexAlgo){
			BellmanFordVertexIntervalls routingAlgo = new BellmanFordVertexIntervalls(fluss);
				
			int i;
			for (i=0; i<rounds; i++){
				timer1 = System.currentTimeMillis();
				result = routingAlgo.doCalculations();
				timer2 = System.currentTimeMillis();
				timeMBF += timer2 - timer1;
				if (result==null){
					break;
				}
				if(_debug){
					System.out.println("found path: " +  result);
				}
				fluss.augment(result);
				timer3 = System.currentTimeMillis();
				fluss.cleanUp();
				timeAugment += timer3 - timer2;
				if (_debug) {
					if (i % 100 == 0) {
					  System.out.println("Iteration " + i + ". Total flow: " + fluss.getTotalFlow() + ". Time: MBF " + timeMBF / 1000 + ", augment " + timeAugment / 1000 + ".");
					}
				}
			}
			if (_debug) {
				  long timeStop = System.currentTimeMillis();
				  System.out.println("Iterations: " + i + ". Total flow: " + fluss.getTotalFlow() + ". Time: Total: " + (timeStop - timeStart) / 1000 + ", MBF " + timeMBF / 1000 + ", augment " + timeAugment / 1000 + ".");
			}
			}
			else{
				FakeTravelTimeCost length = new FakeTravelTimeCost();
				fluss = new Flow(network, length, demands, sink, timeHorizon);
				TravelCost travelCost = length;
				TravelTime travelTime = length;
				MBFdynamic_withFlowClass routingAlgo = new MBFdynamic_withFlowClass(travelCost, travelTime, fluss);
				fluss = routingAlgo.calcLeastCostFlow(0.0, fluss);
			}
			if(_debug){
				System.out.println(fluss.arrivalsToString());
				System.out.println(fluss.arrivalPatternToString());
				System.out.println("unsatisfied demands:");
				for (Node node : demands.keySet()){
					if (demands.get(node) > 0) {
					  System.out.println("node:" + node.getId().toString()+ " demand:" + demands.get(node));
					}
				}
			}
			if(outputplansfile!=null){
				BasicPopulation output = fluss.createPoulation(emptylegs);
				if (emptylegs) {
					Config config = Gbl.createConfig(new String[] {});
					
					World world = Gbl.getWorld();
					world.setNetworkLayer(network);
					world.complete();

					CharyparNagelScoringFunctionFactory factory = new CharyparNagelScoringFunctionFactory();
					PlansCalcRoute router = new PlansCalcRoute(network, new FakeTravelTimeCost(), new FakeTravelTimeCost());
					//PlansCalcRoute router = new PlansCalcRouteDijkstra(network, new FakeTravelTimeCost(), new FakeTravelTimeCost(), new FakeTravelTimeCost());
					for (Object O_person : output.getPersons().values()) {
						Person person = (Person) O_person;
						Plan plan = person.getPlans().get(0);
						router.run(plan);
					}
				}
				PopulationWriterV5 popwriter = new PopulationWriterV5(output);

				try {
				  popwriter.writeFile(outputplansfile);
				} catch (Exception e) {
					e.printStackTrace();
				}								
			}
		}
		if(_debug){
			System.out.println("done");
		}
	}

}
