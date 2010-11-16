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

package playground.dressler.control;

//java imports
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEventsParser;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.population.PopulationWriter;
import org.xml.sax.SAXException;

import playground.dressler.Interval.EdgeIntervals;
import playground.dressler.Interval.Interval;
import playground.dressler.Interval.SourceIntervals;
import playground.dressler.Interval.VertexIntervals;
import playground.dressler.ea_flow.BellmanFordIntervalBased;
import playground.dressler.ea_flow.BellmanFordIntervalBasedWithCost;
import playground.dressler.ea_flow.Flow;
import playground.dressler.ea_flow.StepEdge;
import playground.dressler.ea_flow.StepSinkFlow;
import playground.dressler.ea_flow.StepSourceFlow;
import playground.dressler.ea_flow.TimeExpandedPath;
import playground.dressler.util.CPUTimer;
import playground.dressler.util.ImportSimpleNetwork;

/**
 * @author Daniel Dressler, Manuel Schneider
 *
 */
public class MultiSourceEAF {

	/**
	 * debug flag and the algorithm to use
	 */
	private static boolean _debug = false;

	public static void debug(final boolean debug){
		_debug=debug;
	}
	
	public static boolean getDebug(){
		return _debug;
	}

	public static void enableDebuggingForAllFlowRelatedClasses()
	{
		MultiSourceEAF.debug(true);
		//BellmanFordVertexIntervalls.debug(3);
		BellmanFordIntervalBased.debug(3);
		VertexIntervals.debug(3);
		EdgeIntervals.debug(3);
		SourceIntervals.debug(3);
		Flow.debug(3);
	}

	public static void disableDebuggingForAllFlowRelatedClasses()
	{
		MultiSourceEAF.debug(false);
		//BellmanFordVertexIntervalls.debug(0);
		BellmanFordIntervalBased.debug(0);
		VertexIntervals.debug(0);
		EdgeIntervals.debug(0);
		SourceIntervals.debug(0);
		Flow.debug(0);
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
	public static HashMap<Node,Integer> readDemands(final NetworkImpl network, final String filename) throws IOException{
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
	public static HashMap<Node,Integer> readPopulation(final Scenario scenario, final String filename){
		new PopulationReaderMatsimV4(scenario).readFile(filename);
		return parsePopulation(scenario);
	}

	public static HashMap<Node,Integer> parsePopulation(final Scenario scenario) {
		HashMap<Node,Integer> allnodes = new HashMap<Node,Integer>();
		
		//System.out.println("Parsing population of size: " + scenario.getPopulation().getPersons().size());
		int missing = 0;

		for(Person person : scenario.getPopulation().getPersons().values() ){

			Plan plan = person.getPlans().get(0);
			/*if(((PlanImpl) plan).getFirstActivity().getLinkId()==null){
				continue;
			}*/
			Id id =
				((org.matsim.core.population.ActivityImpl)person.getPlans().get(0).getPlanElements().get(0)).getLinkId();
			//Link link = scenario.getNetwork().getLinks().get(((PlanImpl) plan).getFirstActivity().getLinkId());
			Link link = scenario.getNetwork().getLinks().get(id);
			if (link == null) {
				missing += 1;
				continue;
			}
			Node node = link.getToNode();
			/*if (node.getId().toString().equals("en1")) {
				System.out.println("Person starting on en1: " + person );
			}*/
			if(allnodes.containsKey(node)){
				int temp = allnodes.get(node);
				allnodes.put(node, temp + 1);
			}else{
				allnodes.put(node, 1);
			}
		}
		
		int sum = 0;
		for (Node node : allnodes.keySet()) {
			sum += allnodes.get(node);
		}
		//System.out.println("Found " +  sum + " total demand.");

		if (missing > 0) {
			System.out.println("Missed some start links! Ignored " + missing + " people.");
		}

		return allnodes;
	}
	
	
	private static List<NetworkChangeEvent> readChangeEvents(final NetworkImpl network, final String filename) throws IOException{
		NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network);
		try {
			parser.parse(filename);
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return parser.getEvents();
	}

	
	public static void readShelterFile(NetworkImpl network,final String filename,
			final int totaldemands,HashMap<Node, Integer> demands,final boolean addshelterlinks) throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String inline = null;
		while ((inline = in.readLine()) != null) {
			String[] line = inline.split(",");
			Node node = network.getNodes().get(new IdImpl(line[0].trim()));
			if(node==null){
				continue;
			}else{
				String nodeid = line[0].trim();
				double flowcapacity = Double.valueOf(line[1].trim());
				int sheltercapacity = Integer.valueOf(line[2]);
				
				if(addshelterlinks){
					//create and add new shelternode
					Id shelterid = new IdImpl("shelter"+nodeid);
					if(network.getNodes().get(shelterid)==null){
						NodeImpl shelter = new NodeImpl(shelterid);
						shelter.setCoord(node.getCoord());
						network.addNode(shelter);
						demands.put(shelter, 0);
					}
					Node shelter =network.getNodes().get(shelterid);
					//create and add link from node to shelter
					Id linkid = new IdImpl("shelterlink" + nodeid);
					Link link = network.getLinks().get(linkid);
					if (link == null) {
						//	link = new LinkImpl(linkid, node, shelter, network, 10.66, 1.66, flowcapacity, 1);
						link = network.getFactory().createLink(linkid, node, shelter, network, 10.66, 1.66, flowcapacity, 1.);
						network.addLink(link);
					}
					//set new demands
					int olddemand = demands.get(shelter);
					int newdemand = olddemand - sheltercapacity;
					
					demands.put(shelter, newdemand);
				} else {
					int olddemand = 0;
					if (demands.get(node) == null) {
						olddemand=demands.get(node);
					}
					int newdemand = olddemand - sheltercapacity;
					demands.put(node, newdemand);
				}
			}
		}
		in.close();
		
	}
	public static List<TimeExpandedPath> readPathFlow(NetworkImpl network, String filename ) throws IOException{
		List<TimeExpandedPath> result = new LinkedList<TimeExpandedPath>();
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String inline = null;
		while ((inline = in.readLine()) != null){
			//read a line and split it into steps
			TimeExpandedPath path= new TimeExpandedPath();
			String[] line = inline.split(";");
			for( int i=0;i<line.length;i++){
				String[] step= line[i].split(":");
				//if first word is not Path red next line
				if(i==0 && !step[0].trim().equals("Path")){
					break;
				}
				//read flow on Path
				if(step[0].trim().equals("Path")){
					int flow = Integer.valueOf(step[1].trim());
					path.setFlow(flow);
				}
				//read a sourcestep
				if(step[0].trim().equals("source")){
					Id nodeid = new IdImpl(step[1].trim());
					Node node = network.getNodes().get(nodeid);
					int time = Integer.valueOf(step[2].trim());
					boolean forward = Boolean.valueOf(step[3].trim());
					StepSourceFlow sourcestep =new StepSourceFlow(node,time,forward);
					path.append(sourcestep);
				}
				//read a sinkstep
				if(step[0].trim().equals("sink")){
					Id nodeid = new IdImpl(step[1].trim());
					Node node = network.getNodes().get(nodeid);
					int time = Integer.valueOf(step[2].trim());
					boolean forward = Boolean.valueOf(step[3].trim());
					StepSinkFlow sinkstep =new StepSinkFlow(node,time,forward);
					path.append(sinkstep);
				}
				//read a edgestep
				if(step[0].trim().equals("edge")){
					Id edgeid = new IdImpl(step[1].trim());
					Link link = network.getLinks().get(edgeid);
					int starttime = Integer.valueOf(step[2].trim());
					int endtime = Integer.valueOf(step[3].trim());
					boolean forward = Boolean.valueOf(step[4].trim());
					StepEdge edgestep =new StepEdge(link,starttime,endtime,forward);
					path.append(edgestep);
				}
			}
			if(path.getFlow()!=0){
				result.add(path);
			}
		}
		in.close();
		return result;
	}
	
	/**
	 * THE ONLY FUNCTION WHICH REALLY CALCULATES A FLOW
	 *
	 * @param settings
	 * @return a Flow object
	 */
	public static Flow calcEAFlow(FlowCalculationSettings settings,List<TimeExpandedPath> paths) {
		CPUTimer Tsearch = new CPUTimer("Search ");
		CPUTimer Taugment = new CPUTimer("Augment ");;
		CPUTimer Tgarbage = new CPUTimer("Garbage Collection ");;
		CPUTimer Tall = new CPUTimer("Total ");
		CPUTimer Tcheck = new CPUTimer("Checks ");
		CPUTimer Trepeatedpaths = new CPUTimer("Repeated paths ");
		Tall.onoff();
		
		Flow fluss;
		
		
		List<TimeExpandedPath> result = null;
		fluss = new Flow(settings);
		
		if(paths!=null) {
			System.out.println("restoring flow");			
			for (TimeExpandedPath path : paths) {
				fluss.augment(path,path.getFlow());
			}
			fluss.cleanUp();
		}
		String tempstr = "";

		if (_debug) {
		  System.out.println("starting calculations");
		}

		//int lastArrival = 0;

		
		BellmanFordIntervalBased routingAlgo;
		if (settings.useSinkCapacities) {
			routingAlgo = new BellmanFordIntervalBasedWithCost(settings, fluss);
		} else {
			routingAlgo = new BellmanFordIntervalBased(settings, fluss);
		}

		int VERBOSITY = 100;

		int i;
		long EdgeGain = 0;
		int lasttime = 0;
		int lastcost = Integer.MAX_VALUE;
		boolean haslastcost = false;

		boolean tryReverse;
		boolean usedForwardSearch = false;
		int lastForward = 0; // for trackUnreachable, to do a forward step once in a while

		LinkedList<TimeExpandedPath> successfulPaths = new LinkedList<TimeExpandedPath>();

		tryReverse = (settings.searchAlgo == FlowCalculationSettings.SEARCHALGO_REVERSE);
		
		Runtime runtime = Runtime.getRuntime();		

		for (i=1; i<=settings.MaxRounds; i++){

			//System.out.println("Iteration " + i);

			Tall.newiter();
			Tsearch.newiter();
			Tcheck.newiter();
			Tgarbage.newiter();
			Trepeatedpaths.newiter();
						
			Tsearch.onoff();
			// ugly ...
			if (haslastcost && settings.useSinkCapacities) {
			  ((BellmanFordIntervalBasedWithCost) routingAlgo).startNewIter(lasttime, lastcost);
			} else {
			  routingAlgo.startNewIter(lasttime);
			}
			Tsearch.onoff();
			
			// call the garbage collection now that we have deleted most data structures
			if (settings.doGarbageCollection > 0) {								
				if (i % settings.doGarbageCollection == 0) {
					Tgarbage.onoff();
					runtime.gc();
					Tgarbage.onoff();
				}
			}
			    

			// THE IMPORTANT FUNCTION CALL HAPPENS HERE //
			Tsearch.onoff();
			switch (settings.searchAlgo) {
	            case FlowCalculationSettings.SEARCHALGO_FORWARD: {
	            	result = routingAlgo.doCalculationsForward();
	            	lastForward = lasttime;
	            	break;
	            }
	            case FlowCalculationSettings.SEARCHALGO_REVERSE: {
	            	// FIXME an arbitrary constant ...
	            	// run reverse, unless we want to update the unreachable vertices
	            	boolean needForward = settings.trackUnreachableVertices && (lastForward <= lasttime - 3);
	            	if (tryReverse && !needForward) {
	    				result = routingAlgo.doCalculationsReverse(lasttime);
	    				usedForwardSearch = false;
	    			} else {
	    			    result = routingAlgo.doCalculationsForward();
	    			    usedForwardSearch = true;
	    			}
	            	break;
	            }
	            case FlowCalculationSettings.SEARCHALGO_MIXED: {
	            	result = routingAlgo.doCalculationsMixed(lasttime);
	            	break;
	            }
	            default: {
	            	throw new RuntimeException("Unkown search algorithm!");
	            }
	        }
			Tsearch.onoff();
		
			// DEBUG
			//System.out.println("Returned paths");
			//System.out.println(result);
			
			Taugment.newiter();
			Taugment.onoff();
			
			boolean trySuccessfulPaths = false;
			tempstr = "";
			int zeroaugment = 0;
			int goodaugment = 0;


			if (result == null || result.isEmpty()){
				haslastcost = false;
				if (tryReverse) {
					// backward search didn't find anything.
					// we should try forward next time to determine new arrvivaltime
					lasttime += 1; // guess new time
					tryReverse = false;
					trySuccessfulPaths = true; // before we do the forward search, we can simply repeat paths!
				} else {
					// forward or mixed search didn't find anything
					// that's it, we are done.					
					break;
				}
			} else {
				// lazy way to get some element ...
				for(TimeExpandedPath path : result) {
					lastcost = path.getCost();
					haslastcost = true;
					
					if (path.getArrival() > lasttime) {
						// time has increased!

						lasttime = path.getArrival();

						// recall the list of successful paths and add them first!
						// it cannot hurt much ...
						trySuccessfulPaths = true;

					}

					// even if this did not increase lasttime
					// (because it was increased preemptively last iteration)
					// we now can try the reverse search again
					// if it is enabled at all
					tryReverse = (settings.searchAlgo == FlowCalculationSettings.SEARCHALGO_REVERSE);
					if (usedForwardSearch) {
						lastForward = lasttime;
					}

					// we just needed one element
					break;
				}
			}

			int totalsizeaugmented = 0;

			Trepeatedpaths.onoff();
			if (trySuccessfulPaths && settings.useRepeatedPaths) {
				LinkedList<TimeExpandedPath> newSP = new LinkedList<TimeExpandedPath>();

				for(TimeExpandedPath path : successfulPaths){
					String tempstr2 = "";
					
					// Careful! This is not a new path!
					int latestused = path.shiftToArrival(lasttime);

					// some integrity checks
					if (latestused >= settings.TimeHorizon || path.getPathSteps().getFirst().getStartTime() < 0) {
						zeroaugment += 1;
						continue;
					}

					tempstr2 = path.toString() + "\n";

					int augment = fluss.augment(path);
					
					if (augment > 0) {
						tempstr += tempstr2;
						tempstr += "augmented " + augment + "\n";
						//System.out.println("augmented " + augment + "\n");
						// remember this path for the next timelayer
						newSP.addLast(path); // keep the order!
						goodaugment += 1;
						totalsizeaugmented += path.getPathSteps().size();
					} else {
						zeroaugment += 1;
					}
				}

				if (_debug) {
					System.out.println("Had Repeated paths: " + successfulPaths.size());
					System.out.println("Sucess: " + newSP.size() + ", zeroaugment: " + zeroaugment);
				}

				successfulPaths = newSP;

				if (!newSP.isEmpty()) {
					// we augmented succesfully!
					// in case the reverse search didn't find anything,
					// we now know that we can arrive at the old lasttime + 1 (now just lasttime)
					// so try again!
					tryReverse = (settings.searchAlgo == FlowCalculationSettings.SEARCHALGO_REVERSE);
				}
			}
			Trepeatedpaths.onoff();


			/* sort the paths first by the number of steps used!
			  helps a little for mixed search ...
			 and a lot for reverse search.
			 none for forward? maybe because all paths are augmented anyway */
			if (result != null && !result.isEmpty()) {
				if (settings.sortPathsBeforeAugmenting) {
					Collections.sort(result, new Comparator<TimeExpandedPath>() {
						public int compare(TimeExpandedPath first, TimeExpandedPath second) {
							int v1 = first.getPathSteps().size();
							int v2 = second.getPathSteps().size();
							if (v1 > v2) {
								return 1;
							} else if (v1 == v2) {
								return 0;
							} else {
								return -1;
							}

						}
					});
				}
			}

			// BIG DEBUG
			//System.out.println(result);
			
			if (result != null && !result.isEmpty()) {
				for(TimeExpandedPath path : result){
					String tempstr2 = "";

					tempstr2 = path.toString() + "\n";
					
					int augment = fluss.augment(path);
										
					if (augment > 0) {
						tempstr += tempstr2;
						tempstr += "augmented " + augment + "\n";

						// remember this path for the next timelayer
						successfulPaths.addLast(path); // keep the order

						goodaugment += 1;
						totalsizeaugmented += path.getPathSteps().size();
					} else {
						zeroaugment += 1;
					}
					// BIG DEBUG
					// augment only the first path
					//break;
				}
			}

			tempstr += "Good augment on " + goodaugment + " paths.\n";
			tempstr += "Zero augment on " + zeroaugment + " paths.\n";
			if (goodaugment > 0) {
				tempstr += "Avg size of augmented path " + totalsizeaugmented / (float) goodaugment + ".\n";
			}


			if (_debug) {
				System.out.println(tempstr);
			}
			
			EdgeGain += fluss.cleanUp();
			
			Taugment.onoff();
			// store iteration timing
			Tall.onoff(); 
			Tall.onoff();

			if (settings.checkConsistency > 0) {
				Tcheck.onoff();
				if (i % settings.checkConsistency == 0) {
					System.out.println("Checking consistency once in a while ...");
					if (!fluss.checkTEPsAgainstFlow()) {
						throw new RuntimeException("Flow and stored TEPs disagree!");
					}
					System.out.println("Everything seems to be okay.");
				}
				Tcheck.onoff();
			}
			
			

			// DEBUG
			if(_debug) {
				Tcheck.onoff();
				int[] arrivals = fluss.arrivals();
				long totalcost = 0;
				long totalflow = 0;
				for (int ii = 0; ii < arrivals.length; ii++) {
					totalflow += arrivals[ii];
					totalcost += ii*arrivals[ii];
				}
				System.out.println("Iter " + i + " , total flow: " + totalflow + " , total cost: " + totalcost);
				Tcheck.onoff();
			}
			
			if (i % VERBOSITY == 0) {				
				System.out.println();
				System.out.println("Iterations: " + i + " flow: " + fluss.getTotalFlow() + " of " + settings.getTotalDemand() + " Time: " + Tall + ", " + Tsearch + ", " + Taugment + ".");
				System.out.println(Tgarbage + ", " + Tcheck + ", " + Trepeatedpaths);				
				System.out.println("CleanUp got rid of " + EdgeGain + " edge intervalls so far.");

				//System.out.println("removed on the fly:" + VertexIntervalls.rem);
				System.out.println("last path: " + tempstr);
				System.out.println(routingAlgo.measure());
				System.out.println(fluss.measure());
				System.out.println();				
			}
		}
		
		Tall.onoff();
		
		System.out.println("");
		System.out.println("");
		System.out.println("Iterations: " + i + " flow: " + fluss.getTotalFlow() + " of " + settings.getTotalDemand() + " Time: " + Tall + ", " + Tsearch + ", " + Taugment + ".");
		System.out.println(Tgarbage + ", " + Tcheck + ", " + Trepeatedpaths);
		System.out.println("CleanUp got rid of " + EdgeGain + " edge intervalls so far.");
		//System.out.println("removed on the fly:" + VertexIntervalls.rem);
		System.out.println("last path: " + tempstr);
		System.out.println(routingAlgo.measure());
		System.out.println(fluss.measure());
		System.out.println();

		return fluss;
	}


	/**
	 * main method to run an EAF algorithm on the specified scenario
	 * @param args ignored
	 *
	 */
	public static void main(final String[] args) {

		FlowCalculationSettings settings;

		//set debuging modes
		MultiSourceEAF.debug(false);
		BellmanFordIntervalBased.debug(0);
		VertexIntervals.debug(0);
		//VertexIntervall.debug(false);
		EdgeIntervals.debug(0);
		//EdgeIntervall.debug(false);
		Flow.debug(0);



		String networkfile = null;
		String plansfile = null;
		String demandsfile = null;
		String outputplansfile = null;
		String sinkid = null;
		String simplenetworkfile = null;
		String shelterfile = null;
		String flowfile = null;
		String changeeventsfile = null;
		double changeeventsoffset = 0; 
		int uniformDemands = 0;

		// Rounding is now done according to timestep and flowFactor!
		int timeStep;
		double flowFactor;

		int instance = -1;
		// 1 = siouxfalls, demand 500
		// 11 same as above only Manuel and 5s euclid
		// 2 = swissold, demand 100
		// 3 = padang, demand 5
		// 4 = padang, with 10% plans, 10s steps
		// 41 = padang, with 100% plans, 1s steps ...
		// 42 = padang, v2010, with 100% plans (no shelters yet)
		// 43 = padang, v2010, with 100% plans, 10s steps, shelters
		// 44 = padang, v2010, with 100% plans, 5s steps (no shelters yet)
		// 45 = padang, v2010, with 100% plans, 1s steps (no shelters yet)
		// 48 = padang, v2010, with 100% plans, 1s, change events, no shelters
		// 49 = padang, v2010, with 100% plans, 10s, change events, no shelters 
		// 421-441 same as above only Manuel
		// 5 = probeevakuierung telefunken
		// 6 = various debug stuff
		// 1024 = xmas network
		// else = custom ...

		if (instance == 1) {
			networkfile  = "/homes/combi/dressler/V/code/meine_EA/siouxfalls_network.xml";
			uniformDemands = 500;
			timeStep = 10;
			flowFactor = 1.0;
			sinkid = "supersink";
		}else if (instance == 11) {
			networkfile  = "/Users/manuel/testdata/siouxfalls_network_5s_euclid.xml";
			uniformDemands = 10;
			timeStep = 5;
			flowFactor = 1.0;
			sinkid = "supersink";
		}else if (instance == 111) {
			networkfile  = "/home/manuel/Dokumente/Advest/testdata/siouxfalls_network_5s_euclid.xml";
			uniformDemands = 10;
			timeStep = 5;
			flowFactor = 1.0;
			sinkid = "supersink";
		}else if (instance == 12) {
			networkfile  = "/Users/manuel/testdata/simple/elfen_net.xml";
			//uniformDemands = 30;
			plansfile ="/Users/manuel/testdata/simple/elfen_1_plan.xml";
			timeStep = 1;
			flowFactor = 1.0;
			sinkid = "en1";}
		else if (instance == 2) {
			networkfile = "/homes/combi/Projects/ADVEST/testcases/meine_EA/swissold_network_5s.xml";
			uniformDemands = 10; // war 100
			timeStep = 10; 
			flowFactor = 1.0; 
			sinkid = "en1";
		} else if (instance == 3) {
			networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20080618.xml";
			uniformDemands = 5; // war 5
			timeStep = 10; // war 10
			flowFactor = 1.0;
			sinkid = "en1";
		} else if (instance == 4) {
			networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20080618.xml";
			plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_v20080618_reduced_10p.xml.gz";
			timeStep = 10;
			flowFactor = 0.1;
			sinkid = "en1";
		} else if (instance == 41) {
			networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20080618.xml";
			plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_v20080618_reduced.xml.gz";
			timeStep = 1;
			flowFactor = 1.0;
			sinkid = "en1";
		} else if (instance == 42) {
			networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20100317.xml.gz";
			plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_v20100317.xml.gz";
			timeStep = 10; // war 10
			flowFactor = 1.0;
			sinkid = "en1";
			//shelterfile = "/homes/combi/Projects/ADVEST/padang/network/shelter_info_v20100317";
		} else if (instance == 4200) {
			// wie 42, aber als .dat-Datei
			simplenetworkfile = "/homes/combi/dressler/V/code/meine_EA/padang_v2010_10s_no_shelters.dat";
			timeStep = 1;
			flowFactor = 1.0;
		} else if (instance == 43) {
			networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20100317.xml.gz";
			plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_v20100317.xml.gz";
			timeStep = 10;
			flowFactor = 1.0;
			sinkid = "en1";
			shelterfile = "/homes/combi/Projects/ADVEST/padang/network/shelter_info_v20100317";
		} else if (instance == 44) {
			networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20100317.xml.gz";
			plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_v20100317.xml.gz";
			timeStep = 5; 
			flowFactor = 1.0;
			sinkid = "en1";
		} else if (instance == 45) {
			networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20100317.xml.gz";
			plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_v20100317.xml.gz";
			timeStep = 1;
			flowFactor = 1.0;
			sinkid = "en1";			
		} else if (instance == 4500) {
			// wie 45, aber als .dat-Datei
			simplenetworkfile = "/homes/combi/dressler/V/code/meine_EA/padang_v2010_1s_no_shelters.dat";
			timeStep = 1;
			flowFactor = 1.0;
		} else if (instance == 421) {
			networkfile  = "/Users/manuel/testdata/padang/network/padang_net_evac_v20100317.xml.gz";
			plansfile = "/Users/manuel/testdata/padang/plans/padang_plans_v20100317.xml.gz";
			timeStep = 2;
			flowFactor = 1.0;
			sinkid = "en1";
			shelterfile = "/Users/manuel/testdata/padang/network/shelter_info_v20100317";
		} else if (instance == 431) {
			networkfile  = "/Users/manuel/testdata/padang/network/padang_net_evac_v20100317.xml.gz";
			plansfile = "/Users/manuel/testdata/padang/plans/padang_plans_v20100317.xml.gz";
			timeStep = 10;
			flowFactor = 1.0;
			sinkid = "en1";
		} else if (instance == 441) {
			networkfile  = "/Users/manuel/testdata/padang/network/padang_net_evac_v20100317.xml.gz";
			plansfile = "/Users/manuel/testdata/padang/plans/padang_plans_v20100317.xml.gz";
			timeStep = 5;
			flowFactor = 1.0;
			sinkid = "en1";
		} else if (instance == 48) {
			networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20100317.xml.gz";
			plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_v20100317.xml.gz";
			changeeventsfile = "/homes/combi/Projects/ADVEST/padang/network/change_events_v20100317.xml.gz";
			changeeventsoffset = 3 * 3600; // starts at 3:00:00
			// oder mit 5 Minuten Wellenangst:
			// changeeventsoffset = 3 * 3600 + 300;
			timeStep = 1;
			flowFactor = 1.0;
			sinkid = "en1";
		} else if (instance == 49) {
			networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20100317.xml.gz";
			plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_v20100317.xml.gz";
			changeeventsfile = "/homes/combi/Projects/ADVEST/padang/network/change_events_v20100317.xml.gz";
			changeeventsoffset = 3 * 3600; // starts at 3:00:00
			timeStep = 10;
			flowFactor = 1.0;
			sinkid = "en1";
		} else if (instance == 5) {
			simplenetworkfile = "/homes/combi/dressler/V/code/meine_EA/probeevakuierung.zet.dat";
			timeStep = 1;
			flowFactor = 1.0;
		} else if (instance == 1024) {
			networkfile  = "/homes/combi/dressler/V/code/meine_EA/xmas_network.xml";
			plansfile =  "/homes/combi/dressler/V/code/meine_EA/xmas_plans.xml";
			uniformDemands = 1;
			timeStep = 60;
			flowFactor = 1.0;
			sinkid = "5_Nordpol";
		} else if (instance == 6) {
			networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20100317.xml.gz";
			plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_v20100317.xml.gz";
			timeStep = 50; 
			flowFactor = 1.0;
			sinkid = "en1";
		} else {
			// custom instance

			//networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20080618.xml";
			//networkfile  = "/homes/combi/dressler/V/code/meine_EA/problem.xml";
			//networkfile = "/Users/manuel/Documents/meine_EA/manu/manu2.xml";
			//networkfile = "/homes/combi/Projects/ADVEST/testcases/meine_EA/swissold_network_5s.xml";
			//networkfile  = "/homes/combi/dressler/V/code/meine_EA/siouxfalls_network.xml";

			//***---------MANU------**//
			//networkfile = "/Users/manuel/testdata/siouxfalls_network_5s_euclid.xml";
			//networkfile = "/Users/manuel/testdata/simple/line_net.xml";
			//networkfile = "/Users/manuel/testdata/simple/elfen_net.xml";
			//networkfile = "/Users/manuel/testdata/padangcomplete/network/padang_net_evac_v20080618_100p_1s_EAF.xml";

			//plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_v20080618_reduced_10p.xml.gz";
			//plansfile ="/homes/combi/Projects/ADVEST/code/matsim/examples/meine_EA/siouxfalls_plans.xml";
			//plansfile = "/homes/combi/dressler/V/Project/testcases/swiss_old/matsimevac/swiss_old_plans_evac.xml";
			//plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_v20080618_reduced_10p.xml.gz";
			//plansfile = "/Users/manuel/testdata/simple/elfen_1_plan.xml";
			//plansfile = "/Users/manuel/testdata/padangcomplete/plans/padang_plans_10p.xml";



			//demandsfile = "/Users/manuel/Documents/meine_EA/manu/manu2.dem";
			//demandsfile = "/homes/combi/dressler/V/code/meine_EA/problem_demands.dem";


			//outputplansfile = "/homes/combi/dressler/V/code/workspace/matsim/examples/meine_EA/padangplans_10p_5s.xml";
			//outputplansfile = "./examples/meine_EA/swissold_plans_5s_demands_100.xml";
			//outputplansfile = "./examples/meine_EA/padang_plans_100p_flow_2s.xml";
			//outputplansfile = "./examples/meine_EA/siouxfalls_plans_5s_euclid_demands_100_empty.xml";

			//outputplansfile = "./examples/meine_EA/siouxfalls_plans_5s_demand_100_emptylegs.xml";
			//outputplansfile = "/homes/combi/dressler/stuff/testplans.xml";
			//outputplansfile = "/homes/combi/schneide/fricke/testplans.xml";
			//outputplansfile = "/Users/manuel/tester/ws3_testoutput.xml";

			//simplenetworkfile = "/homes/combi/dressler/V/code/meine_EA/problem.dat";
			//simplenetworkfile = "/homes/combi/dressler/V/code/meine_EA/demo.zet.dat";
			//simplenetworkfile = "/homes/combi/dressler/V/code/meine_EA/audimax.zet.dat";
			//simplenetworkfile = "/homes/combi/dressler/V/code/meine_EA/otto hahn straße 14.zet.dat";
			//simplenetworkfile = "/homes/combi/dressler/V/code/meine_EA/probeevakuierung.zet.dat";
			//simplenetworkfile = "/homes/combi/dressler/V/code/meine_EA/capsinks.dat";
			//simplenetworkfile = "/homes/combi/dressler/V/code/meine_EA/padang_v2010_3_sources.dat";
			
			simplenetworkfile  = "/homes/combi/dressler/V/code/meine_EA/padang_5_10s.dat";
			//simplenetworkfile  = "/homes/combi/dressler/V/code/grids/test_small.dat";
			//simplenetworkfile  = "/homes/combi/dressler/V/code/grids/grid_500x50_10000_at_all_to_10_R3.dat";
			//simplenetworkfile  = "/homes/combi/dressler/V/code/grids/grid_50x50_100000_at_all_to_10_R2.dat";
			
			uniformDemands = 100;


			timeStep = 1;
			flowFactor = 1.0;

			//String sinkid = "supersink"; //siouxfalls, problem
			sinkid = "en1";  //padang, line, swissold .. en1 fuer forward

		}
		
		//outputplansfile = "/homes/combi/dressler/V/code/meine_EA/tempplans.xml";
		//flowfile = "/homes/combi/dressler/V/vnotes/statistik_2010_04_april/bug_shelters_implicit.pathflow";
		//flowfile = "/homes/combi/dressler/V/code/meine_EA/padang_v2010_250k_10s_shelters.pathflow";
		

		if(_debug){
			System.out.println("starting to read input");
		}

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		HashMap<Node, Integer> demands = null;
		HashMap<Id, Interval> whenAvailable = null;
		Node sink = null;

		//read network
		if (networkfile != null) {
			scenario = new ScenarioImpl();
			network = scenario.getNetwork();
			MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
			networkReader.readFile(networkfile);
			sink = network.getNodes().get(new IdImpl(sinkid));
			if (sink == null){
				System.out.println("sink not found");
			}
		} else if (simplenetworkfile != null) {
			ImportSimpleNetwork importer = new ImportSimpleNetwork(simplenetworkfile);
			try {
				network = importer.getNetwork();
				demands = importer.getDemands();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		//read demands

		if(plansfile!=null){
			demands = readPopulation(scenario, plansfile);
		}else if (demandsfile != null){
			try {
				demands = readDemands(network,demandsfile);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		} else if (simplenetworkfile != null) {
			// we should already have read those ...
		} else {
			// uniform demands
			demands = new HashMap<Node, Integer>();
			for (Node node : network.getNodes().values()) {
				if (!node.getId().equals(sink.getId())) {
					demands.put(node, Math.max(uniformDemands,0));
				}
			}

			// Hack for those networks with en1->en2
			// set demand of en2 to 0, because it cannot be satisfied if en1 is the sink
			if (sinkid.equals("en1")) {
				Node sink2 = network.getNodes().get(new IdImpl("en2"));
				if (sink2 != null) {
					Integer d = demands.get(sink2);
					if (d != null && d > 0) {
						demands.put(sink2, 0);
					}
				}
			}
		}
		
		int totaldemands = 0;
		for (int i : demands.values()) {
			if (i > 0)
			  totaldemands += i;
		}
		
		if (shelterfile != null) {
			try {
				readShelterFile(network,shelterfile,totaldemands,demands,true);
			} catch(IOException e) {
				e.printStackTrace();
				return;
			}
			totaldemands = 0;
			for (int i : demands.values()) {
				if (i > 0)
				  totaldemands += i;
			}
			for(Node node : demands.keySet()){
				if (demands.get(node) < 0) {
						System.out.println("NEGATIVE DEMAND SHELTER :"+demands.get(node)+" at "+ node);
					}
			}
			
		}
		
		// read change events
		
		if (changeeventsfile != null) {
			List<NetworkChangeEvent> changeEvents = null; 
			whenAvailable = new HashMap<Id, Interval>();
			try {
			  changeEvents = readChangeEvents(network, changeeventsfile);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			for (NetworkChangeEvent event : changeEvents) {
				int roundedtime = (int) Math.round(event.getStartTime() - changeeventsoffset);
				roundedtime = roundedtime / timeStep;
				if (roundedtime <= 0) {
					System.out.println("Warning! ChangeEvents delete a link entirely! The link will be available at the first time step, though.");
					roundedtime = 1; // FIXME is this necessary?
				}
				for (Link link : event.getLinks()) {				
				  Interval i = new Interval(0, roundedtime);
				  whenAvailable.put(link.getId(), i);
				  //System.out.println(roundedtime + " " + link.getId());
				}
			}			
		}
		
		
		//NetworkWriter writer = new NetworkWriter(network);
		//writer.write("/homes/combi/dressler/V/code/meine_EA/padang_v2010_mit_shelter.xml");
		//if (true) return;
		
		
		//check if demands and sink are set
		if (demands.isEmpty()) {
			System.out.println("demands not found");
		}

		if(_debug){
			System.out.println("Total source demand is " + totaldemands);
			System.out.println("reading input done");
		}


		settings = new FlowCalculationSettings();
		settings.setNetwork(network);
		settings.setDemands(demands);
		settings.whenAvailable = whenAvailable;
		
		settings.supersink = sink;
		settings.timeStep = timeStep; // default 1
		settings.flowFactor = flowFactor; // default 1.0

		// set additional parameters
		//settings.TimeHorizon = 550;
		//settings.MaxRounds = 20;
		//settings.checkConsistency = 1;
		//settings.doGarbageCollection = 10; // > 0 generally not such a good idea.
		//settings.minTravelTime = 1;
		settings.useSinkCapacities = false;
		//settings.useVertexCleanup = false;
		settings.useImplicitVertexCleanup = true;
		//settings.useShadowFlow = false;		
		
		//settings.searchAlgo = FlowCalculationSettings.SEARCHALGO_FORWARD;
		//settings.searchAlgo = FlowCalculationSettings.SEARCHALGO_MIXED;
		settings.searchAlgo = FlowCalculationSettings.SEARCHALGO_REVERSE;
		settings.usePriorityQueue = true; // use a PriorityQueue instead of a Queue
		
		settings.useRepeatedPaths = true; // not compatible with costs!
		// track unreachable vertices only works in REVERSE (with forward in between), and wastes time otherwise
		settings.trackUnreachableVertices = true  && (settings.searchAlgo == FlowCalculationSettings.SEARCHALGO_REVERSE);
		//settings.sortPathsBeforeAugmenting = true;
		settings.checkTouchedNodes = false;
		settings.keepPaths = true; // store paths at all
		settings.unfoldPaths = false; // unfold stored paths into forward paths
		settings.delaySinkPropagation = true; // propagate sinks (and resulting intervals) only if the search has nothing else to do 
		settings.quickCutOff = 0.1; // <0, continue fully,  =0 stop as soon as the first good path is found, > 0 continue a bit (e.g. 0.1 continue for another 10% of the polls so far) 
		settings.mapLinksToTEP = true; // remember which path uses an edge at a given time
		settings.useHoldover = true; //only forward/reverse, no cost, no unwind
		//settings.whenAvailable = new HashMap<Link, Interval>();
		//settings.whenAvailable.put(network.getLinks().get(new IdImpl("1")), new Interval(2,3));

		Flow fluss;


		/* --------- the actual work starts --------- */

		boolean settingsOkay = settings.prepare();
		settings.printStatus();

		if(!settingsOkay) {
			System.out.println("Something was bad, aborting.");
			return;
		}
		//read flow if specified
		List<TimeExpandedPath> flowpaths = null;
		if (flowfile != null) {
			try {
			flowpaths = readPathFlow(network, flowfile);
			} catch(Exception e) {
				e.printStackTrace();
				return;
			}
			
		}

		//settings.writeLP();
		//settings.writeSimpleNetwork(true);
		//settings.writeSimpleNetwork(false);
		//settings.writeNET(true);
		//settings.writeLodyfa();
		//if(true)return;
		
		fluss = MultiSourceEAF.calcEAFlow(settings, flowpaths);
		
		//System.out.println(fluss.writePathflow(false));

		/* --------- the actual work is done --------- */
		

		int[] arrivals = fluss.arrivals();
		long totalcost = 0;
		for (int i = 0; i < arrivals.length; i++) {
			totalcost += i*arrivals[i];
		}

		System.out.println("Total cost: " + totalcost);
		System.out.println("Collected " + fluss.getPaths().size() + " paths.");

		System.out.println(fluss.arrivalsToString());
		System.out.println(fluss.arrivalPatternToString());
		System.out.println("unsatisfied demands:");
		for (Node node : fluss.getDemands().keySet()){
			int demand = fluss.getDemands().get(node);
			if (demand > 0) {
				// this can be a lot of text				
				//System.out.println("node:" + node.getId().toString()+ " demand:" + demand);
			}
		}

		// decompose the flow
		Flow reconstructedFlow = new Flow(settings);
		CPUTimer Tdecompose = new CPUTimer("Path Decomposition");
		CPUTimer Treconstruct = new CPUTimer("Flow Reconstruction");
		
		Tdecompose.onoff();
		LinkedList<TimeExpandedPath> decomp = fluss.doPathDecomposition(); 
		Tdecompose.onoff();
			
		
		Treconstruct.onoff();
		if( decomp !=null) {
			System.out.println("reconstructing flow");			
			for (TimeExpandedPath path : decomp) {
				reconstructedFlow.augment(path,path.getFlow());
			}
			reconstructedFlow.cleanUp();
		}		
		Treconstruct.onoff();
		
		fluss = reconstructedFlow;
		
		System.out.println("== After decomposition & reconstruction ==");
		System.out.println(Tdecompose);
		System.out.println(Treconstruct);
		
		int[] arrivals2 = fluss.arrivals();
		long totalcost2 = 0;
		for (int i = 0; i < arrivals2.length; i++) {
			totalcost2 += i*arrivals2[i];
		}

		System.out.println("Total cost: " + totalcost2);
		System.out.println("Collected " + fluss.getPaths().size() + " paths.");

		System.out.println(fluss.arrivalsToString());
		System.out.println(fluss.arrivalPatternToString());
		System.out.println("unsatisfied demands:");
		for (Node node : fluss.getDemands().keySet()){
			int demand = fluss.getDemands().get(node);
			if (demand > 0) {
				// this can be a lot of text				
				//System.out.println("node:" + node.getId().toString()+ " demand:" + demand);
			}
		}
		
		
		if (outputplansfile != null) {
			PopulationCreator popcreator = new PopulationCreator(settings);
			
			// fix the final link
			//popcreator.pathSuffix.put(new IdImpl("en1"), new IdImpl("el1"));
			popcreator.autoFixSink(new IdImpl("en2"));			
			
			Population output = popcreator.createPopulation(fluss.getPaths(), scenario);
			new PopulationWriter(output, network).write(outputplansfile);
		}


		// statistics
		Partitioner partitioner = new Partitioner(settings);
		partitioner.goBackHowMany = 0;
		partitioner.determineStatistics(fluss.getPaths());
		//partitioner.printStatistics();
		
		String imageBaseName = "/homes/combi/dressler/V/code/meine_EA/tmpimage/";
		partitioner.drawStatistics(imageBaseName, 500, 500);

		if(_debug){
			System.out.println("done");
		}
	}


}
