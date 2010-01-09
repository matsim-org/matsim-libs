/* *********************************************************************** *
 * project: org.matsim.*
 * MultiSourceEAF.java
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
package playground.rost.eaflow.ea_flow;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.rost.controller.gui.helpers.progressinformation.ProgressInformationProvider;
import playground.rost.eaflow.Intervall.src.Intervalls.VertexIntervalls;
import playground.rost.eaflow.ea_flow.GlobalFlowCalculationSettings.EdgeTypeEnum;
import playground.rost.eaflow.ea_flow.TimeExpandedPath.PathEdge;
import playground.rost.graph.evacarea.EvacArea;
import playground.rost.graph.nodepopulation.PopulationNodeMap;
import playground.rost.graph.shortestdistances.LengthCostFunction;
import playground.rost.graph.shortestdistances.ShortestDistanceFromSupersource;

/**
 * @author Manuel Schneider
 *
 */
public class MultiSourceEAF implements ProgressInformationProvider{

	/**
	 * debug flag
	 */
	private static boolean _debug = true;
	private int lastArrival;
	
	private EdgeTypeEnum edgeTypeToUse = EdgeTypeEnum.SIMPLE;


	public EdgeTypeEnum getEdgeTypeToUse() {
		return edgeTypeToUse;
	}

	public void setEdgeTypeToUse(EdgeTypeEnum edgeTypeToUse) {
		this.edgeTypeToUse = edgeTypeToUse;
	}

	public static void debug(final boolean debug){
		_debug=debug;
	}
	
	public MultiSourceEAF()
	{
		this.setupStatusInfo();
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
	private static HashMap<Node,Integer> readDemands(final NetworkLayer network, final String filename) throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(filename));
		HashMap<Node,Integer> demands = new HashMap<Node,Integer>();
		String inline = null;
		while ((inline = in.readLine()) != null) {
			String[] line = inline.split(";");
			Node node = network.getNodes().get(new IdImpl(line[0].trim()));
			Integer d = Integer.valueOf(line[1].trim());
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
	private static HashMap<Node,Integer> readPopulation(final Scenario scenario, final String filename){
		new MatsimPopulationReader(scenario).readFile(filename);
		HashMap<Node,Integer> allnodes = new HashMap<Node,Integer>();

		for(Person person : scenario.getPopulation().getPersons().values() ){

			Plan plan = person.getPlans().get(0);
			if(((PlanImpl) plan).getFirstActivity().getLinkId()==null){
				continue;
			}

			Node node = scenario.getNetwork().getLinks().get(((PlanImpl) plan).getFirstActivity().getLinkId()).getToNode();
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
	public static void main(final String[] args) {
		//TODO somehow access calcEAFlow (copy old code?)
		
	}
	
	public Flow calcEAFlow(EvacArea evacArea, NetworkLayer network, PopulationNodeMap populationNodeMap)
	{
		Map<Node, Integer> demands = new HashMap<Node, Integer>();
		for(String id : populationNodeMap.populationForNode.keySet())
		{
			Node node = network.getNodes().get(new IdImpl(id));
			if(node != null)
			{
				int demand = populationNodeMap.populationForNode.get(id);
				demands.put(node, demand);
			}
		}
		
		//create sink
		network.createAndAddNode(new IdImpl(GlobalFlowCalculationSettings.superSinkId), new CoordImpl(0,0));
		Node sink = network.getNodes().get(new IdImpl(GlobalFlowCalculationSettings.superSinkId));
		int counter = 0;
		//create links from real sinks to supersink!
		for(String id : evacArea.evacBorderNodeIds)
		{
			Id linkId = new IdImpl("borderNode->sink" + (++counter));
			network.createAndAddLink( linkId, 
								network.getNodes().get(new IdImpl(id)), 
								network.getNodes().get(new IdImpl(GlobalFlowCalculationSettings.superSinkId)),
								0,
								Integer.MAX_VALUE, //freespeed 
								Integer.MAX_VALUE, //capacity
								1);
		}
		
		ShortestDistanceFromSupersource sd = new ShortestDistanceFromSupersource(new LengthCostFunction(), network, evacArea);
		sd.calcShortestDistances();
		sd.getNodes(Double.MAX_VALUE);
		
		Flow fluss = calcEAFlow(network, demands);
		return fluss;
	}
	
	
	/**
	 * THE ONLY FUNCTION WHICH REALLY CALCULATES A FLOW
	 * and provides status info
	 * 
	 * @param network
	 * @param demands
	 * @return
	 */
	public Flow calcEAFlow(NetworkLayer network, Map<Node, Integer> demands)
	{
		Set<Link> toRemove = new HashSet<Link>();
		for(Link link : network.getLinks().values())
		{
			if(link.getFromNode().equals(link.getToNode()))
				toRemove.add(link);
		}
		for(Link link : toRemove)
		{
			network.removeLink(link);
		}
		if(_debug){
			System.out.println("starting to read input");
		}
		
		int totaldemands = 0;
		for(Integer count : demands.values())
		{
			totaldemands += count;
		}
		
		//create sink
		Node sink = network.getNodes().get(new IdImpl(GlobalFlowCalculationSettings.superSinkId));
		
		int timeHorizon = 10000;
		int rounds = 10000;
		String tempstr = "";
		Flow fluss = null;
		lastArrival = 0;
		if(!demands.isEmpty() && (sink != null)) {
			Collection<TimeExpandedPath> result = null;
			fluss = new Flow( network, demands, sink, timeHorizon, this.edgeTypeToUse);

			if(_debug){
				System.out.println("starting calculations");
			}


			long timeMBF = 0;
			long timeAugment = 0;
			long timer1, timer2, timer3;
			long timeStart = System.currentTimeMillis();

			BellmanFordVertexIntervalls routingAlgo = new BellmanFordVertexIntervalls(fluss);
			//BellmanFordIntervallBased routingAlgo = new BellmanFordIntervallBased(fluss);
			int i;
			int gain = 0;
			for (i=1; i<=rounds && fluss.getTotalFlow() < totaldemands; i++){
				timer1 = System.currentTimeMillis();
				if(i == 738)
				{
					int foobar = 0;
				}
				result = routingAlgo.doCalculations();
				timer2 = System.currentTimeMillis();
				timeMBF += timer2 - timer1;
				if (result==null){
					break;
				}
				if(_debug){
					tempstr = "found path " + result;
					//System.out.println("found path: " +  result);
				}
				if(result.size() == 0)
					throw new RuntimeException("NO PATH FOUND!");
				int gammaSum = 0;
				int currentArrival = -1;
				System.out.println("new paths!");
				
				for(TimeExpandedPath tEPath : result)
				{
					int arrive = tEPath.getArrival();
					if(arrive != currentArrival && currentArrival != -1)
					{
						throw new RuntimeException("the found augmenting paths arrive at different times");
					}
					currentArrival = arrive;
					int foobar = 0;
					int gamma = fluss.augment(tEPath);
					gammaSum += gamma;
					//tEPath.print();
					if(_debug)
						System.out.println("path found with " + gamma + " of flow");
				}
				System.out.println(""+currentArrival);
				if(currentArrival < lastArrival)
					throw new RuntimeException("the found paths are shorter than the last found shortest path!");
				lastArrival = currentArrival;
				if(gammaSum == 0)
					throw new RuntimeException("no flow could be transported over any augmenting path!");
				
				timer3 = System.currentTimeMillis();
				gain += fluss.cleanUp();

				timeAugment += timer3 - timer2;
				if (true) {
					if (i % 1 == 0) {
						System.out.println("Iteration " + i + ". flow: " + fluss.getTotalFlow() + " of " + totaldemands + ". Time: MBF " + timeMBF / 1000 + ", augment " + timeAugment / 1000 + ".");
						//System.out.println("CleanUp got rid of " + gain + " intervalls so far.");
						//System.out.println("last " + tempstr);
						System.out.println(routingAlgo.measure());
						System.out.println("");
					}
				}
				this.setProgressInfo("Iteration", "" + i);
				this.setProgressInfo("Time", ""+ (timeMBF / 1000) );
				this.setProgressInfo("Last Arrival", ""+this.lastArrival );
				this.setProgressInfo("Total Flow", fluss.getTotalFlow() + " / " + totaldemands);
				this.setProgressInfo("Found Paths", "" + fluss.getPaths().size());
				this.setProgressInfo("Paths / Iteration", ""+ (fluss.getPaths().size() / (double)i));
				
			}
			if (true) {
				long timeStop = System.currentTimeMillis();
				System.out.println("Iterations: " + i + ". flow: " + fluss.getTotalFlow() + " of " + totaldemands + ". Time: Total: " + (timeStop - timeStart) / 1000 + ", MBF " + timeMBF / 1000 + ", augment " + timeAugment / 1000 + ".");				  
				System.out.println("CleanUp got rid of " + gain + " intervalls so far.");
				System.out.println("last " + tempstr);
			}
			System.out.println("Removed " + routingAlgo.gain + " intervals.");
			System.out.println("removed on the fly:" + VertexIntervalls.rem);
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
		this.isFinished = true;
		if(_debug){
			System.out.println("done");
		}
		calculateTotalHoldover(fluss);
		return fluss;
	}
	
	protected void calculateTotalHoldover(Flow fluss)
	{
		int time = 0;
		int flow = 0;
		int multiSum = 0;
		for(TimeExpandedPath tEPath : fluss.getPaths())
		{
			int last = -1;
			for(PathEdge pE : tEPath.getPathEdges())
			{
				if(last == -1)
				{
					last = pE.getArrivalTime();
				}
				else
				{
					int diff = pE.getStartTime() - last;
					if(diff > 0)
					{
						time += diff;
						flow += tEPath.getFlow();
						multiSum += diff * tEPath.getFlow();
					}
					last = pE.getArrivalTime();
				}
			}
		}
		System.out.println("time: " + time);
		System.out.println("flow:" + flow);
		System.out.println("multi: " + multiSum);
	}
	

	protected List<String> progressTitles;
	
	/**
	 * 	must not (!) get accessed unsychronized
	 */
	protected Map<String, String> progressInfos;
	
	protected boolean isFinished;
	
	protected void setupStatusInfo()
	{
		isFinished = false;
		progressTitles = new LinkedList<String>();
		progressTitles.add("Iteration");
		progressTitles.add("Time");
		progressTitles.add("Last Arrival");
		progressTitles.add("Total Flow");
		progressTitles.add("Found Paths");
		progressTitles.add("Paths / Iteration");
		progressInfos = new HashMap<String, String>();
		for(String key : progressTitles)
		{
			progressInfos.put(key, "");
		}
	}
	
	protected void setProgressInfo(String key, String value)
	{
		synchronized(progressInfos)
		{
			progressInfos.put(key, value);
		}
	}

	public List<String> getListOfKeys() {
		return progressTitles;
	}

	public String getProgressInformation(String key) {
		synchronized(progressInfos)
		{
			return progressInfos.get(key);
		}
	}

	public String getTitle() {
		return "Flow Calculation";
	}

	public boolean isFinished() {
		return isFinished;
	}

}
