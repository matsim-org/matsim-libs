/* *********************************************************************** *
 * project: org.matsim.*
 * Flow.java
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.RuntimeErrorException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.dressler.Intervall.src.Intervalls.EdgeIntervalls;
import playground.dressler.Intervall.src.Intervalls.SourceIntervalls;
import playground.dressler.ea_flow.TimeExpandedPath;
import playground.dressler.ea_flow.PathStep;
import playground.dressler.ea_flow.StepEdge;
import playground.dressler.ea_flow.StepSourceFlow;
/**
 * Class representing a dynamic flow on an network with multiple sources and a single sink 
 * @author Manuel Schneider
 *
 */

public class Flow {
////////////////////////////////////////////////////////////////////////////////////////	
//--------------------------FIELDS----------------------------------------------------//
////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * The global settings.
	 */
	private final FlowCalculationSettings _settings;
	
	/**
	 * The network on which we find routes. 
	 * We expect the network not to change between runs!
	 */
	private final NetworkLayer _network;
		
	/**
	 * Edge representation of flow on the network  
	 */
	private HashMap<Link, EdgeIntervalls> _flow;
	
	
	/**
	 * Source outflow, somewhat like holdover for sources  
	 */
	private HashMap<Node, SourceIntervalls> _sourceoutflow;
	
	/**
	 * TimeExpandedTimeExpandedPath representation of flow on the network
	 */
	private final LinkedList<TimeExpandedPath> _TimeExpandedPaths;
	
	/**
	 * list of all sources
	 */
	private final LinkedList<Node> _sources;
		
	/**
	 * stores unsatisfied demands for each source
	 */
	private Map<Node,Integer> _demands;
	
	/**
	 * the sink, to which all flow is directed
	 */
	private final  Node _sink;
	
	/**
	 * the Time Horizon (for easy access)
	 */
	private int _timeHorizon;
	
		
	/**
	 * total flow augmented so far
	 */
	private int totalflow;
	
	
	
	/**
	 * TODO use debug mode
	 * flag for debug mode
	 */
	@SuppressWarnings("unused")
	private static int _debug = 0;
		
	
///////////////////////////////////////////////////////////////////////////////////	
//-----------------------------Constructors--------------------------------------//	
///////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Constructor that initializes a zero flow over time for the specified settings
	 * @param settings
	 */
	public Flow(FlowCalculationSettings settings) {
		
		this._settings = settings;
		this._network = settings.getNetwork();
		this._flow = new HashMap<Link,EdgeIntervalls>();
		this._sourceoutflow = new HashMap<Node, SourceIntervalls>();
		
		this._TimeExpandedPaths = new LinkedList<TimeExpandedPath>();		
		this._demands = new HashMap<Node, Integer>();
		this._sources = new LinkedList<Node>();
		
				
		for(Node node : this._network.getNodes().values()){
			if (this._settings.isSource(node)) {
				int i = this._settings.getDemand(node);
				this._sources.add(node);
				this._sourceoutflow.put(node, new SourceIntervalls());
				this._demands.put(node, i);
			}		
		}
		// initialize EdgeIntervalls
		for (Link edge : this._network.getLinks().values()) {
			this._flow.put(edge, new EdgeIntervalls(this._settings.getLength(edge)));
		}
		
		this._sink = settings.getSink();
		this._timeHorizon = settings.TimeHorizon;
		this.totalflow = 0;
		
	}	
	
//////////////////////////////////////////////////////////////////////////////////
//--------------------Flow handling Methods-------------------------------------//	
//////////////////////////////////////////////////////////////////////////////////	
	/**
	 * Method to determine whether a Node is a Source with positive demand
	 * @param node Node that is checked
	 * @return true iff Node is a Source and has positive demand
	 */
	public boolean isActiveSource(final Node node) {
		Integer i = _demands.get(node);
		if (i== null){
			return false;
		}else{
			return (i > 0);			
		}
	}
	
	/**
	 * Method for residual bottleneck capacity of the TimeExpandedPath
	 * (also limited by the source)
	 * @param TimeExpandedPath
	 * @return minimum over all unused capacities and the demand in the first node
	 */
	private int bottleNeckCapacity(final TimeExpandedPath TimeExpandedPath){
		//check if first node is a source
		Node source = TimeExpandedPath.getSource();
		if(!this._demands.containsKey(source)){
			throw new IllegalArgumentException("Startnode is no source " + TimeExpandedPath);
		}
		int result = this._demands.get(source);
		if(result == 0) {			
			System.out.println("Weird. Source of TimeExpandedPath had no supply left.");
			return 0;
		}
		//go through the path edges
		//System.out.println("augmenting path: ");
		
		int cap;
		for(PathStep step : TimeExpandedPath.getPathSteps()){			
			
			// FIXME really bad style ...			
			if (step instanceof StepEdge) {
				StepEdge se = (StepEdge) step;
				Link edge = se.getEdge();				
						
				if(se.getForward()){
					cap = this._settings.getCapacity(edge) - this._flow.get(edge).getFlowAt(se.getStartTime());										
				} else {
					cap = this._flow.get(edge).getFlowAt(se.getArrivalTime());
				}			
				if(cap<result ){
					result= cap;
				}			
			} else if (step instanceof StepSourceFlow) {
				StepSourceFlow ssf = (StepSourceFlow) step;
				Node node  = ssf.getStartNode();
				
				if (!ssf.getForward()) {
					SourceIntervalls si = this._sourceoutflow.get(node);
					if (si == null) {
						System.out.println("Weird. Source of StepSourceFlow has no sourceoutflow!");
						return 0;			    	 
					} else {
						cap = si.getFlowAt(ssf.getStartTime());
						if (cap < result) {
							result = cap;
						}
					}				    		  
				} 
				/* no else, because outflow out of a source has no cap.
				   (the demand of the original source is accounted for,
				   demand of sources we pass through does not matter) */    
			} else {
				throw new RuntimeException("Unsupported kind of PathStep!");
			}  
						
		}
		//System.out.println(""+ result);
		return result;
	}
	
	/**
	 * Method to add another TimeExpandedPath to the flow. The TimeExpandedPath will be added with flow equal to its bottleneck capacity
	 * @param TimeExpandedPath the TimeExpandedPath on which the maximal possible flow is augmented
	 * @return Amount of flow augmented 
	 */
	public int augment(TimeExpandedPath TEP){	  
	  int bottleneck = bottleNeckCapacity(TEP);
	  this.augment(TEP, bottleneck);
	  return bottleneck;
	}
	
	/**
	 * Method to add another TimeExpandedPath to the flow with a given flow value on it 
	 * @param TEP The TimeExpandedPath on which the maximal flow possible is augmented
	 * @param gamma Amount of flow to augment 
	 * @return Amount of flow that was really augmented. Should be gamma under most circumstances.
	 */
	public void augment(TimeExpandedPath TEP, int gamma){
	  if (TEP.hadToFixSourceLinks()) {
	    System.out.println("TimeExpandedPath should start with PathEdge of type SourceOutflow! Fixed.");		  
	  }
	  TEP.setFlow(gamma);
	  dumbaugment(TEP, gamma); // throws exceptions if something is wrong	  
	  this.totalflow += gamma;
	  	  
	  this.unfold(TEP);	  // FIXME still a stub
	}
	/**
	 * Method to change just the flow values according to TimeExpandedPath and gamma 
	 * @param TEP The TimeExpandedPath on which the flow is augmented
	 * @param gamma Amount of flow to augment 
	 * @return Amount of flow that was really augmented. Should be gamma under most circumstances.
	 */
	
	private void dumbaugment(TimeExpandedPath TEP, int gamma) {
	  
		for (PathStep step : TEP.getPathSteps()) {

			// FIXME really bad style ...			
			if (step instanceof StepEdge) {
				StepEdge se = (StepEdge) step;
				Link edge = se.getEdge();				

				if(se.getForward()){
					this._flow.get(edge).augment(se.getStartTime(), gamma, this._settings.getCapacity(edge));
				}	else {
					this._flow.get(edge).augmentreverse(se.getArrivalTime(), gamma);									  
				}			
			} else if (step instanceof StepSourceFlow) {
				StepSourceFlow ssf = (StepSourceFlow) step;
				Node source = ssf.getStartNode();
				Integer demand = this._demands.get(source);

				if (demand == null) {
					throw new IllegalArgumentException("Startnode is no source on TimeExpandedPath " + TEP);
				}

				if (ssf.getForward()) {	
					demand -= gamma;
					if (demand < 0) {
						throw new IllegalArgumentException("too much flow on TimeExpandedPath " + TEP);
					}
					this._sourceoutflow.get(source).augment(ssf.getArrivalTime(), gamma, Integer.MAX_VALUE);
					this._demands.put(source, demand);
				} else {
					this._sourceoutflow.get(source).augmentreverse(ssf.getStartTime(), gamma);
					this._demands.put(source, demand + gamma);
				}

			} else {
				throw new RuntimeException("Unsupported kind of PathStep!");
			}  
		}
	}
	
	/**
	 * Recursive method to resolve residual edges in TEP 
	 * @param TEP A TimeExpandedPath 
	 */
	private void unfold(TimeExpandedPath TEP){
		System.out.println("Cannot unfold paths yet! TEPs are not stored at all.");
		// TODO stub
	}
	
	/*public Set<TimeExpandedPath> augmentPathWithBackwardEdges(TimeExpandedPath pathWithBackwardEdges, int flowToAugment)
	{
		List<PathEdge> backwardEdges = new LinkedList<PathEdge>();
		for(PathEdge pE : pathWithBackwardEdges.getPathEdges())
		{
			if(!pE.isForward())
				backwardEdges.add(pE);
		}
		if(backwardEdges.size() > 1)
		{
			int foobar = 0;
			foobar++;
		}
		Set<TimeExpandedPath> timeExpandedPathsWithBackwardEdges = new HashSet<TimeExpandedPath>();
		timeExpandedPathsWithBackwardEdges.add(pathWithBackwardEdges);
		int counter = 0;
		while(backwardEdges.size() > 0)
		{
			PathEdge pE = backwardEdges.remove(0);
//			System.out.println(++counter + "th backwaerd edge");
//			System.out.println("pE: " + pE.toString());
			//call it with all paths, that contains backward edges.
			//the method returns an updated set with every path which contains a time expanded path
			//after the operation.
			timeExpandedPathsWithBackwardEdges = augmentSingleBackwardEdge(timeExpandedPathsWithBackwardEdges, pE, pathWithBackwardEdges.getFlow());
		}
		return timeExpandedPathsWithBackwardEdges;
	}
	
	protected Set<TimeExpandedPath> augmentSingleBackwardEdge(Set<TimeExpandedPath> tEPathsWithBackwardEdges, PathEdge backwardEdgeToReplace, int flowToAugment)
	{
		Set<TimeExpandedPath> result = new HashSet<TimeExpandedPath>();
		//get one backward edge and convert it!
		Map<TimeExpandedPath, PathEdge> forwardEdges = getForwardPathEdges(backwardEdgeToReplace, flowToAugment);
		for(TimeExpandedPath pathWithBackwardEdges : tEPathsWithBackwardEdges)
		{
			int tmpFlowToAugment = flowToAugment;
			Set<TimeExpandedPath> forwardPathsToRemove = new HashSet<TimeExpandedPath>();
			for(TimeExpandedPath tEPath : forwardEdges.keySet())
			{
				int foobar = 0;
//				System.out.println("forward path: ");
//				tEPath.print();
//				System.out.println("path with backward edge: ");
//				pathWithBackwardEdges.print();
				int flowOnPath = tEPath.getFlow();
				PathEdge forwardPathEdge = forwardEdges.get(tEPath);
				//create new paths
				List<TimeExpandedPath> splittedForwardPath = tEPath.splitPathAtEdge(forwardPathEdge, false);
				List<TimeExpandedPath> splittedBackwardPath = pathWithBackwardEdges.splitPathAtEdge(backwardEdgeToReplace, true);
				
				TimeExpandedPath forwardHead = splittedForwardPath.get(0);
//				System.out.println("forwardHead: ");
//				forwardHead.print();
				TimeExpandedPath forwardTail = splittedForwardPath.get(1);
//				System.out.println("forwardTail: ");
//				forwardTail.print();
				
				TimeExpandedPath backwardHead = splittedBackwardPath.get(0);
//				System.out.println("backwardHead: ");
//				backwardHead.print();
				TimeExpandedPath backwardTail = splittedBackwardPath.get(1);
//				System.out.println("backwardTail: ");
//				backwardTail.print();
				
				forwardHead.addTailToPath(backwardTail);
//				System.out.println("new backward path: ");
//				forwardHead.print();
				
				backwardHead.addTailToPath(forwardTail);
//				System.out.println("new forward path: ");
//				backwardHead.print();
				//adjust values
				if(flowOnPath <= tmpFlowToAugment)
				{
					forwardHead.setFlow(flowOnPath);
					backwardHead.setFlow(flowOnPath);
					tmpFlowToAugment -= flowOnPath;
					//remove orinigal forward path, because it is no flow left on this path.
					_TimeExpandedPaths.remove(tEPath);
					forwardPathsToRemove.add(tEPath);
				}
				else
				{
					//clone original forward Path
					TimeExpandedPath copyOfForward = TimeExpandedPath.clone(tEPath);
					copyOfForward.setFlow(flowOnPath-tmpFlowToAugment);
					_TimeExpandedPaths.add(copyOfForward);
					forwardHead.setFlow(tmpFlowToAugment);
					backwardHead.setFlow(tmpFlowToAugment);
					tmpFlowToAugment = 0;
				}
				_TimeExpandedPaths.add(backwardHead);
				result.add(forwardHead);
				if(tmpFlowToAugment == 0)
					break;
			}
			for(TimeExpandedPath remove : forwardPathsToRemove)
			{
				forwardEdges.remove(remove);
			}
		}
		return result;
	}
	
	protected Map<TimeExpandedPath, PathEdge> getForwardPathEdges(PathEdge backwardPathEdge, int neededFlow)
	{
		Map<TimeExpandedPath, PathEdge> result = new HashMap<TimeExpandedPath, PathEdge>();
		for(TimeExpandedPath tEPath : _TimeExpandedPaths)
		{
			LinkedList<PathEdge> list = tEPath.getPathEdges();
			for(PathEdge pE : tEPath.getPathEdges())
			{
				if(backwardPathEdge.isResidualVersionOf(pE))
				{
					result.put(tEPath, pE);
					neededFlow -= Math.min(tEPath.getFlow(), neededFlow);						
					break;
				}
			}
			if(neededFlow == 0)
				break;
		}
		return result;
	}
	*//**
	 * Method to add another TimeExpandedPath to the flow. The TimeExpandedPath will be added with flow equal to its bottleneck capacity
	 * @param TimeExpandedPath the TimeExpandedPath on which the maximal flow possible is augmented 
	 *//*
	public int augment(TimeExpandedPath timeExpandedPath){
		boolean backward = false;
		for(PathEdge pE : timeExpandedPath.getPathEdges())
		{
			if(!pE.isForward())
			{
				backward = true;
				break;
			}
		}
		int dummy = 0;
		int gamma = bottleNeckCapacity(timeExpandedPath);
		if(gamma == 0)
			return 0;
//		this.totalflow += gamma;
		timeExpandedPath.setFlow(gamma);
		// no backward links
		if(!backward){
			for(PathEdge edge : timeExpandedPath.getPathEdges()){
				Link link = edge.getEdge();
				int startTime = edge.getStartTime();
				EdgeIntervalls flow = _flow.get(link);
				if(edge.isForward()){
					flow.augment(startTime, gamma, (int)link.getCapacity(1.));
				}else{
					System.out.println("Unexpected error!");
				}
			}
			reduceDemand(timeExpandedPath);
			this._TimeExpandedPaths.add(timeExpandedPath);
		}
		else{
			//for every backward link 2 different paths will be created and will be added to _timeexpandedpaths
			Set<TimeExpandedPath> newPaths = augmentPathWithBackwardEdges(timeExpandedPath, gamma);
			//augment the single path:
			int flow = timeExpandedPath.getFlow();
			for(PathEdge pE : timeExpandedPath.getPathEdges())
			{
				if(pE.isForward())
					_flow.get(pE.getEdge()).augment(pE.getStartTime(), flow, (int)pE.getEdge().getCapacity(1.));
				else 
					_flow.get(pE.getEdge()).augmentreverse(pE.getStartTime(), flow);
			}
			for(TimeExpandedPath tEPath : newPaths)
			{
				this._TimeExpandedPaths.add(tEPath);
			}
						
			// add rest of the paths and reduce demands
			reduceDemand(timeExpandedPath);
		}
		return gamma;
	}*/
	
	/**
	 * construct a path from a List of subpaths
	 * @param PathList with subgraphs
	 * @return new path
	 */
	/*public TimeExpandedPath constructPath(LinkedList<TimeExpandedPath> PathList){
		TimeExpandedPath result = new TimeExpandedPath();
		if(PathList.size() <= 0){
			return null;
		}
		if(PathList.size() == 1){
			if(PathList.getFirst().check()){
				result = PathList.getFirst();
				return result;
			}
			return null;
		}
		else{
			Node node = null;
			boolean first = true;
			for(int i = 0; i < PathList.size(); i++){
				for(int j = 0; j < PathList.get(i).length(); j++){
					Link edge = PathList.get(i).getPathEdges().get(j).getEdge();
					int startTime = PathList.get(i).getPathEdges().get(j).getStartTime();
					int arrivalTime = PathList.get(i).getPathEdges().get(j).getArrivalTime();
					boolean forward = PathList.get(i).getPathEdges().get(j).isForward();
					if(first){
						node = edge.getFromNode();
						if(this.isActiveSource(node)){
							result.append(edge, startTime, arrivalTime, forward);
							node = edge.getToNode();
							first = false;
						}
						else{
							System.out.println("First node is no source.");
							return null;
						}
					}
					else if(node.equals(edge.getFromNode())){
						node = edge.getToNode();
						result.append(edge, startTime, arrivalTime, forward);
					}
					else if(node.equals(edge.getToNode())){
						node = edge.getFromNode();
						result.append(edge, startTime, arrivalTime, forward);
					}
					else{
						System.out.println("Path isn't connected.");
						return null;
					}
				}
			}
			if(node.equals(this._sink)){
				return result;
			}
			return null;
		}
	}
	*/
	/**
	 * decides whether a Node is a non-active (depleted) Source
	 * @param node Node to check for	
	 * @return true iff node is a Source now with demand 0
	 */
	public boolean isNonActiveSource(final Node node){
		if (this._settings.isSource(node)) { // superfluous ... only sources are in _demands
		  Integer i = this._demands.get(node);
		  return (i != null && i == 0);
		}
		return false;
	}
	
////////////////////////////////////////////////////////////////////////////////////
//-----------evaluation methods---------------------------------------------------//
////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * gives back an array containing the amount of flow into the sink for all time steps from 0 to time horizon
	 */
	public int[] arrivals(){
		int maxtime = 0;
		int[] temp = new int[this._timeHorizon+1];
		for (TimeExpandedPath TimeExpandedPath : _TimeExpandedPaths){
			int flow = TimeExpandedPath.getFlow();
			int time = TimeExpandedPath.getArrival();
			if (maxtime < time){
				maxtime = time; 
			}
			temp[time]+=flow;
		}
		
		int[] result = new int[maxtime+1];
		for(int i=0; i<=maxtime;i++){
			result[i]=temp[i];
		}
		return result;
		
	}
	
	/**
	 * gives back an array containing the total amount of flow into the sink by a given time 
	 * for all time steps from 0 to time horizon
	 */
	public int[] arrivalPattern(){
		int[] result = this.arrivals();
		int sum = 0;
		for (int i=0;i<result.length; i++){
			sum+=result[i];
			result[i]=sum;
		}
		return result;
	}
	/**
	 * String representation of the arrivals specifying the amount of flow into the sink
	 * for all time steps from 0 to time horizon
	 * @return String representation of the arrivals
	 */
	public String arrivalsToString(){
		//StringBuilder strb1 = new StringBuilder();
		StringBuilder strb2 = new StringBuilder("  arrivals:");
		int[] a =this.arrivals();
		for (int i=0; i<a.length;i++){
			String temp = String.valueOf(a[i]);
			strb2.append(" "+i+":"+temp);
		}
		return strb2.toString();
	}
	
	/**
	 * a STring specifying the total amount of flow into the sink by a given time 
	 * for all time steps from 0 to time horizon
	 * @return String representation of the arrival pattern
	 */
	public String arrivalPatternToString(){
		//StringBuilder strb1 = new StringBuilder();
		StringBuilder strb2 = new StringBuilder("arrival pattern:");
		int[] a =this.arrivalPattern();
		for (int i=0; i<a.length;i++){
			String temp = String.valueOf(a[i]);
			strb2.append(" "+i+":"+temp);
		}
		return strb2.toString();
	}

//////////////////////////////////////////////////////////////////////////////////////
//---------------------------Plans Converter----------------------------------------//	
//////////////////////////////////////////////////////////////////////////////////////
	
	
	@SuppressWarnings("unchecked")
	
	/**
	 * 
	 */
	public PopulationImpl createPopulation(String oldfile){
		//check whether oldfile exists
		//boolean org = (oldfile!=null);
		//HashMap<Node,LinkedList<Person>> orgpersons = new  HashMap<Node,LinkedList<Person>>();
		
		//read old network an find out the startnodes of persons if oldfile exists
		/*if(org){
			Population population = new PopulationImpl(PopulationImpl.NO_STREAMING);
			new MatsimPopulationReader(population,_network).readFile(oldfile);
			_network.connect();
			for(Person person : population.getPersons().values() ){
				Link link = person.getPlans().get(0).getFirstActivity().getLink();
				if (link == null) continue; // happens with plans that don't match the network.
				
				Node node = link.getToNode();
				if(orgpersons.get(node)==null){
					LinkedList<Person> list = new LinkedList<Person>();
					list.add(person);
					orgpersons.put(node, list);
				}else{
					LinkedList<Person> list = orgpersons.get(node);
					list.add(person);
				}
			}
		}*/
		
		//construct Population
		PopulationImpl result = new ScenarioImpl().getPopulation();
		int id =1;
		for (TimeExpandedPath path : this._TimeExpandedPaths){
			if(path.isforward()){
				//units of flow on the Path
				int nofpersons = path.getFlow();
				// list of links in order of the path
				LinkedList<Id> ids = new LinkedList<Id>();
				for (PathStep step : path.getPathSteps()){
					if (step instanceof StepEdge) {
					  ids.add(((StepEdge) step).getEdge().getId());
					}
				}
				
				
				//if (!emptylegs) { 
					// normal case, write the routes!
					LinkNetworkRouteImpl route;
					
					Node firstnode  = _network.getLinks().get(ids.get(0)).getFromNode();
					
					// for each unit of flow construct a Person
					for (int i =1 ; i<= nofpersons;i++){
						//add the first edge if olfile exists
						String stringid = null;
						PersonImpl orgperson = null;
						/*if(org && (( orgpersons.get(firstnode))!=null) ){
							LinkedList<Person> list = orgpersons.get(firstnode);
							orgperson = list.getFirst();
							list.remove(0);
							if(list.isEmpty()){
								orgpersons.remove(firstnode);
							}
							Link firstlink = orgperson.getPlans().get(0).getFirstActivity().getLink();
							if(i==1){
							ids.add(0,firstlink.getId());
							}
							stringid = orgperson.getId().toString();
						}else{*/
							stringid = "new"+String.valueOf(id);
							id++;
						//}
						
//						route = new BasicRouteImpl(ids.get(0),ids.get(ids.size()-1));
						Link startLink = _network.getLinks().get(ids.get(0));
						Link endLink = _network.getLinks().get(ids.get(ids.size()-1));
						route = new LinkNetworkRouteImpl(startLink, endLink);
						
						List<Link> routeLinks = null;
						if (ids.size() > 1) {
							routeLinks = new ArrayList<Link>();
//							route.setLinkIds(ids.subList(1, ids.size()-1));
							for (Id iid : ids.subList(1, ids.size()-1)){
								routeLinks.add(_network.getLinks().get(iid));
							}
						} 
						route.setLinks(startLink, routeLinks, endLink);

						
						LegImpl leg = new LegImpl(TransportMode.car);
						//Leg leg = new org.matsim.population.LegImpl(BasicLeg.Mode.car);
						leg.setRoute(route);
						Link fromlink =_network.getLinks().get(ids.getFirst());
						ActivityImpl home = new ActivityImpl("h", fromlink.getId());
//						home.setLinkId(fromlink.getId());
						Link tolink =_network.getLinks().get(ids.getLast());
						ActivityImpl work = new ActivityImpl("w", tolink.getId());
//						work.setLinkId(tolink.getId());
						

						//Act home = new org.matsim.population.ActImpl("h", path.getPathEdges().getFirst().getEdge());
						home.setEndTime(0);
						//home.setCoord(_network.getLink(ids.getFirst()).getFromNode().getCoord());	
						// no end time for now.
						//home.setEndTime(path.getPathEdges().getFirst().getTime());
						
						//Act work = new org.matsim.population.ActImpl("w", path.getPathEdges().getLast().getEdge());
						work.setEndTime(0);
						//work.setCoord(_network.getLink(ids.getLast()).getToNode().getCoord());
						
						
						Id matsimid  = new IdImpl(stringid);
						PersonImpl p = new PersonImpl(matsimid);
						PlanImpl plan = new org.matsim.core.population.PlanImpl(p);
						plan.addActivity(home);
						plan.addLeg(leg);					
						plan.addActivity(work);
						p.addPlan(plan);
						result.addPerson(p);
						id++;
					}
					
			}else{ // residual edges
				// this should not happen!
				System.out.println("createPopulation encountered a residual step in");
				System.out.println(path);
				System.out.println("This should not happen!");
			}
			
			
		}
		
		
		return result;
	}

//////////////////////////////////////////////////////////////////////////////////////
//------------------- Clean Up---------------------------------------//	
//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Call the cleanup-method for each edge 
	 */
	public int cleanUp() {		
		int gain = 0;
		for (EdgeIntervalls EI : _flow.values()) {
		  gain += EI.cleanup();	
		}
		for (Node node : this._sourceoutflow.keySet()) {
			 SourceIntervalls si = this._sourceoutflow.get(node);
			 if (si != null)
			   gain += si.cleanup();				
		}
		return gain;
	}	
	
//////////////////////////////////////////////////////////////////////////////////////
//-------------------Getters Setters toString---------------------------------------//	
//////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * returns a String representation of the entire flows
	 */
	@Override
	public String toString(){
		StringBuilder strb = new StringBuilder();
		for(Link link : _flow.keySet()){
			EdgeIntervalls edge =_flow.get(link);
			strb.append(link.getId().toString()+ ": " + edge.toString()+ "\n");
		}
		return strb.toString();
	}

	/**
	 * @return the _demands
	 */
	public Map<Node, Integer> getDemands() {
		return this._demands;
	}

	/**
	 * @return the _flow
	 */
	public EdgeIntervalls getFlow(Link edge) {
		return this._flow.get(edge);
	}
	
	public SourceIntervalls getSourceOutflow(Node node) {
		return this._sourceoutflow.get(node);
	}

	/**
	 * @return the _sink
	 */
	public Node getSink() {
		return _sink;
	}

	/**
	 * @return the _sources
	 */
	public LinkedList<Node> getSources() {
		return this._sources;
	}

	/**
	 * @return the network
	 */
	public NetworkLayer getNetwork() {
		return this._network;
	}
	
	/**
	 * @return the total flow so far
	 */
	public int getTotalFlow() {
		return this.totalflow;
	}
	
    /** @return the paths
	*/
	public LinkedList<TimeExpandedPath> getPaths() {
		return this._TimeExpandedPaths;
	}
	
	/**
	
	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(int debug){
		Flow._debug=debug;
	}
	
	public int getStartTime()
	{
		return 0;
	}
	
	public int getEndTime()
	{
		return this.arrivals().length-1;
	}
	
}
