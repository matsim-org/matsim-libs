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

package playground.dressler.ea_flow;

//java imports
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;

import playground.dressler.Interval.EdgeIntervals;
import playground.dressler.Interval.VertexIntervals;
import playground.dressler.util.ImportSimpleNetwork;

/**
 * @author Manuel Schneider
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
	public static HashMap<Node,Integer> readDemands(final NetworkLayer network, final String filename) throws IOException{
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
		new MatsimPopulationReader(scenario).readFile(filename);
		HashMap<Node,Integer> allnodes = new HashMap<Node,Integer>();
		
		int missing = 0;

		for(Person person : scenario.getPopulation().getPersons().values() ){

			Plan plan = person.getPlans().get(0);
			if(((PlanImpl) plan).getFirstActivity().getLinkId()==null){
				continue;
			}
			
			Link link = scenario.getNetwork().getLinks().get(((PlanImpl) plan).getFirstActivity().getLinkId());
			if (link == null) {
				missing += 1;				
				continue;				
			}
			Node node = link.getToNode();
			if(allnodes.containsKey(node)){
				int temp = allnodes.get(node);
				allnodes.put(node, temp + 1);
			}else{
				allnodes.put(node, 1);
			}
		}
		
		if (missing > 0) {
			System.out.println("Missed some start links! Ignored " + missing + " people.");
		}

		return allnodes;
	}

	/**
	 * THE ONLY FUNCTION WHICH REALLY CALCULATES A FLOW
	 * and provides status info
	 *
	 * @param settings 
	 * @return a Flow object
	 */
	public static Flow calcEAFlow(FlowCalculationSettings settings) {	
		Flow fluss;
		

		List<TimeExpandedPath> result = null;
		fluss = new Flow(settings);
		
		String tempstr = "";

		if(_debug){
		  System.out.println("starting calculations");
		}
		
		//int lastArrival = 0;

		long timeMBF = 0;
		long timeAugment = 0;
		long timer1, timer2, timer3;
		long timeStart = System.currentTimeMillis();

		BellmanFordIntervalBased routingAlgo = new BellmanFordIntervalBased(settings, fluss);
			
		int i;
		long EdgeGain = 0;
		long VertexGain = 0;
		int lasttime = 0;
		
		boolean tryReverse;
		int lastForward = 0; // for trackUnreachable, to do a forward step once in a while
		
		LinkedList<TimeExpandedPath> successfulPaths = new LinkedList<TimeExpandedPath>();
		
		tryReverse = (settings.searchAlgo == FlowCalculationSettings.SEARCHALGO_REVERSE);
		
		for (i=1; i<=settings.MaxRounds; i++){
			timer1 = System.currentTimeMillis();
			
			//System.out.println("Iteration " + i);
			
			// THE IMPORTANT FUNCTION CALL HAPPENS HERE //
			routingAlgo.startNewIter(lasttime);
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
	    			} else {
	    			    result = routingAlgo.doCalculationsForward();
	    			    lastForward = lasttime; // not perfectly precise ... oh well
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
			 
			VertexGain += routingAlgo.vertexGain;
			
			timer2 = System.currentTimeMillis();
			timeMBF += timer2 - timer1;
			
			boolean trySuccessfulPaths = false;
			tempstr = "";
			int zeroaugment = 0;
			int goodaugment = 0;
			
			
			if (result == null || result.isEmpty()){
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
					if (path.getArrival() > lasttime) {
						// time has increased!
						
						lasttime = path.getArrival();

						// might be worth trying the reverse search again
						// if it is enabled at all
						tryReverse = (settings.searchAlgo == FlowCalculationSettings.SEARCHALGO_REVERSE);

						// recall the list of successful paths and add them first!
						// it cannot hurt much ...
						trySuccessfulPaths = true;

					}				
					// we just needed one element
					break;
				}
			}
			
			int totalsizeaugmented = 0;
			
			if (trySuccessfulPaths && settings.useRepeatedPaths) {
				LinkedList<TimeExpandedPath> newSP = new LinkedList<TimeExpandedPath>();
				
				for(TimeExpandedPath path : successfulPaths){
					String tempstr2 = "";
					
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
			
			
			
			/* sort the paths first by the number of steps used!			
			  helps a little for mixed search ...
			 and a lot for reverse search.
			 none for forward? maybe because all paths are augmented anyway */
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
			
			//System.out.println(result);
						
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
			}
			
			tempstr += "Good augment on " + goodaugment + " paths.\n";
			tempstr += "Zero augment on " + zeroaugment + " paths.\n";
			if (goodaugment > 0) {
				tempstr += "Avg size of augmented path " + totalsizeaugmented / (float) goodaugment + ".\n";
			}
			
			
			if (_debug) {				
				System.out.println(tempstr);				
			}
			
			timer3 = System.currentTimeMillis();
			EdgeGain += fluss.cleanUp();
			
			timeAugment += timer3 - timer2;
			
			if (i % 100 == 0) {		
				long timecurrent = System.currentTimeMillis();
				System.out.println();
				System.out.println("Iterations: " + i + ". flow: " + fluss.getTotalFlow() + " of " + settings.getTotalDemand() + ". Time: Total: " + (timecurrent - timeStart) / 1000 + ", Time: MBF " + timeMBF / 1000 + ", augment " + timeAugment / 1000 + ".");
				System.out.println("CleanUp got rid of " + EdgeGain + " edge intervalls so far.");
				System.out.println("CleanUp got rid of  " + VertexGain + " vertex intervals so far.");
				//System.out.println("removed on the fly:" + VertexIntervalls.rem);
				System.out.println("last path: " + tempstr);
				System.out.println(routingAlgo.measure());	
				System.out.println(fluss.measure());
				System.out.println();
			}
			
			if (settings.checkConsistency > 0) {
				if (i % settings.checkConsistency == 0) {
					System.out.println("Checking consistency once in a while ...");
					if (!fluss.checkTEPsAgainstFlow()) {
						throw new RuntimeException("Flow and stored TEPs disagree!"); 
					}
					System.out.println("Everything seems to be okay.");
				}
			}

		}
		
		
		long timeStop = System.currentTimeMillis();
		System.out.println("");
		System.out.println("");
		System.out.println("Iterations: " + i + ". flow: " + fluss.getTotalFlow() + " of " + settings.getTotalDemand() + ". Time: Total: " + (timeStop - timeStart) / 1000 + ", MBF " + timeMBF / 1000 + ", augment " + timeAugment / 1000 + ".");
		System.out.println("CleanUp got rid of " + EdgeGain + " edge intervalls so far.");
		System.out.println("CleanUp got rid of  " + VertexGain + " vertex intervals so far.");
		//System.out.println("removed on the fly:" + VertexIntervalls.rem);
		System.out.println("last path: " + tempstr);
		System.out.println(routingAlgo.measure());	
		System.out.println(fluss.measure());
		System.out.println();
		
		
		return fluss;
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
		String sinkid;
		String simplenetworkfile = null;
		int uniformDemands = 0;
		
		// Rounding is now done according to timestep and flowFactor!
		int timeStep; 
		double flowFactor;

		int instance = 2; 
		// 0 = custom
		// 1 = siouxfalls, demand 500
		// 2 = swissold, demand 100
		// 3 = padang, demand 5
		// 4 = padang, with 10% plans		
		// else = something else
		
		if (instance == 1) {
			networkfile  = "/homes/combi/dressler/V/code/meine_EA/siouxfalls_network.xml";
			uniformDemands = 500;
			timeStep = 10;
			flowFactor = 1.0;
			sinkid = "supersink";
		} else if (instance == 2) {
			networkfile = "/homes/combi/Projects/ADVEST/testcases/meine_EA/swissold_network_5s.xml";
			uniformDemands = 100;
			timeStep = 10;
			flowFactor = 1.0;
			sinkid = "en1";
		} else if (instance == 3) {
			networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20080618.xml";
			uniformDemands = 5;
			timeStep = 10;
			flowFactor = 1.0;
			sinkid = "en1";
		} else if (instance == 4) {
			networkfile  = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_v20080618.xml";
			plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_v20080618_reduced_10p.xml.gz";
			timeStep = 10;
			flowFactor = 0.1;
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

			simplenetworkfile = "/homes/combi/dressler/V/code/meine_EA/problem.dat";
			
			uniformDemands = 100;


			timeStep = 1; 
			flowFactor = 1.0;

			//String sinkid = "supersink"; //siouxfalls, problem
			sinkid = "en1";  //padang, line, swissold .. en1 fuer forward

		}
		
		
		if(_debug){
			System.out.println("starting to read input");
		}
		
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		HashMap<Node, Integer> demands = null;
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
			for (NodeImpl node : network.getNodes().values()) {
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
		
		//check if demands and sink are set
		if (demands.isEmpty() ) {
			System.out.println("demands not found");
		}
		
		if(_debug){
			System.out.println("Total source demand is " + totaldemands);
			System.out.println("reading input done");
		}

		
		if (sink == null) {			
			settings = new FlowCalculationSettings(network, demands, timeStep, flowFactor);
		} else {
			settings = new FlowCalculationSettings(network, sinkid, demands, timeStep, flowFactor);
		}

		// set additional parameters, mostly TimeHorizon for the LP
		//settings.TimeHorizon = 1240;
		//settings.MaxRounds = 101;
		//settings.checkConsistency = 100;		
		settings.useVertexCleanup = false;
		settings.useImplicitVertexCleanup = true;
		//settings.searchAlgo = FlowCalculationSettings.SEARCHALGO_FORWARD;
		//settings.searchAlgo = FlowCalculationSettings.SEARCHALGO_MIXED;
		settings.searchAlgo = FlowCalculationSettings.SEARCHALGO_REVERSE;
		settings.useRepeatedPaths = true;
		// track unreachable vertices only works in REVERSE (with forward in between), and wastes time otherwise
		settings.trackUnreachableVertices = false && (settings.searchAlgo == FlowCalculationSettings.SEARCHALGO_REVERSE); 
		settings.sortPathsBeforeAugmenting = true;
		settings.checkTouchedNodes = true;
		settings.keepPaths = true; // do not store paths at all!
		settings.unfoldPaths = true; // unfold stored paths into forward paths
		
		
		settings.printStatus();
		
		//settings.writeLP();
		//settings.writeNET(false);
		
		
		
		Flow fluss;
		fluss = calcEAFlow(settings);
		
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
				System.out.println("node:" + node.getId().toString()+ " demand:" + demand);
			}
		}

		if(outputplansfile!=null){
			PopulationImpl output = fluss.createPopulation(plansfile);				
			new PopulationWriter(output, network).writeFile(outputplansfile);
		}
		
		if(_debug){
			System.out.println("done");
		}
	}

}
