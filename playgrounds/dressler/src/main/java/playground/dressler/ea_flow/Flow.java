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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.dressler.Intervall.src.Intervalls.EdgeIntervalls;
import playground.dressler.ea_flow.GlobalFlowCalculationSettings.EdgeTypeEnum;
import playground.dressler.ea_flow.TimeExpandedPath.PathEdge;
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
	 * The network on which we find routes. We expect the network to change
	 * between runs!
	 */
	private final NetworkLayer _network;
	
	/**
	 * used to calculate the length of every edge in the network
	 */
	private Map<Link, FlowEdgeTraversalCalculator> _lengths = new HashMap<Link, FlowEdgeTraversalCalculator>(); 
	
	/**
	 * Edge representation of flow on the network  
	 */
	private HashMap<Link, EdgeIntervalls> _flow;
	
	/**
	 * TimeExpandedTimeExpandedPath representation of flow on the network
	 */
	private LinkedList<TimeExpandedPath> _TimeExpandedPaths;
	
	/**
	 * list of all sources
	 */
	private final LinkedList<Node> _sources;
	
	/**
	 * stores unsatisfied demands for each source
	 */
	private Map<Node,Integer> _demands;
	
	/**
	 *stores for all nodes whether they are an non active source 
	 */
	private HashMap<Node,Boolean> _nonactives;

	/**
	 * the sink, to which all flow is directed
	 */
	private final  Node _sink;
	
	/**
	 * maximal time Horizon for the flow
	 */
	private final  int _timeHorizon;
	
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
	
	private final EdgeTypeEnum edgeType;
	
///////////////////////////////////////////////////////////////////////////////////	
//-----------------------------Constructors--------------------------------------//	
///////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Constructor that initializes a zero flow over time on the specified network
	 * the length of the edges will be as specified by FakeTravelTimeCost 
	 * @param network network on which the flow will "live"
	 * @param sources the potential sources of the flow		
	 * @param demands the demands in the sources as nonnegative integers
	 * @param sink the sink for all the flow
	 * @param horizon the time horizon in which flow is allowed
	 */
	public Flow(final NetworkLayer network, Map<Node, Integer> demands, final Node sink, final int horizon, EdgeTypeEnum edgeTypeToUse) {
		this._network = network;
		this._flow = new HashMap<Link,EdgeIntervalls>();
		this.edgeType = edgeTypeToUse;
		
		// initialize distances
		for(Link link : network.getLinks().values()){
			
			FlowEdgeTraversalCalculator bTT = GlobalFlowCalculationSettings.getFlowEdgeTraversalCalculator(edgeTypeToUse, link);
			_lengths.put(link, bTT);
			this._flow.put(link, new EdgeIntervalls(bTT));
		}
		this._TimeExpandedPaths = new LinkedList<TimeExpandedPath>();
		this._demands = demands;
		this._sources = new LinkedList<Node>();
		this._sources.addAll(demands.keySet());
		this._sink = sink;
		_timeHorizon = horizon;
		this._nonactives = this.nonActives();
		this.totalflow = 0;
		
	}
	
	/**
	 * for all Nodes it is specified if the node is an non active source
	 */
	private HashMap<Node,Boolean> nonActives(){
		HashMap<Node,Boolean> nonactives = new HashMap<Node,Boolean>();
		for(Node node : this._network.getNodes().values()){
			if(!this._sources.contains(node)){
				nonactives.put(node, false);
			}else{
				if(this._demands.get(node)!=0){
					nonactives.put(node, false);
				}else{
					nonactives.put(node, true);
				}
			}
		}
		return nonactives;
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
			if (i>0){
				return true;
			}else{
				return false;
			}
		}
	}
	
	/**
	 * Method for finding the minimum of the demand at the start node
	 * and the minimal capacity along the TimeExpandedPath
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
		if(result == 0)
			System.out.println("OMG NO DEMANDS FOR ME :(");
		//go through the pat edges
		//System.out.println("augmenting path: ");
		for(PathEdge edge : TimeExpandedPath.getPathEdges()){
			Link link = edge.getEdge();
			int startTime = edge.getStartTime();
			int arrivalTime = edge.getArrivalTime();
			int neededTravelTime = arrivalTime-startTime;
			//check forward capacity
			if(edge.isForward()){
				int flow = this._flow.get(link).getFlowAt(startTime);
				if(_lengths.get(link).getTravelTimeForAdditionalFlow(flow) == null)
				{
					return 0;
				}
				int traveltime = _lengths.get(link).getTravelTimeForAdditionalFlow(flow);
				if(traveltime != neededTravelTime)
				{
					System.out.println("path not feasible anymore");
					return 0;
				}
				int cap = _lengths.get(link).getRemainingForwardCapacityWithThisTravelTime(flow);
				if (cap<0){
					throw new IllegalArgumentException("too much flow on " + edge);
				}
				if(cap<result ){
					result= cap;
				}
			}
			// backwards capacity
			else{
				if(link.getFromNode().getId().toString().equals("29218296") && startTime == 142)
				{
					int debug = 0;
				}
				int flow = this._flow.get(link).getFlowAt(startTime);
				int traveltime = _lengths.get(link).getTravelTimeForFlow(flow);
				if(traveltime != neededTravelTime)
				{
					System.out.println("path not feasible anymore");
					return 0;
				}
				flow = _lengths.get(link).getRemainingBackwardCapacityWithThisTravelTime(flow);
				if(flow<result){
					result= flow;
				}
			}
			
			
		}
		//System.out.println(""+ result);
		return result;
	}
	
	public Set<TimeExpandedPath> augmentPathWithBackwardEdges(TimeExpandedPath pathWithBackwardEdges, int flowToAugment)
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
				if(pE.equals(backwardPathEdge))
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
	/**
	 * Method to add another TimeExpandedPath to the flow. The TimeExpandedPath will be added with flow equal to its bottleneck capacity
	 * @param TimeExpandedPath the TimeExpandedPath on which the maximal flow possible is augmented 
	 */
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
		this.totalflow += gamma;
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
	}
	
	/**
	 * construct a path from a List of subpaths
	 * @param PathList with subgraphs
	 * @return new path
	 */
	public TimeExpandedPath constructPath(LinkedList<TimeExpandedPath> PathList){
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
	
	/**
	 * Reduces the demand of the first node in the TimeExpandedPath by the flow value of the TimeExpandedPath
	 * @param TimeExpandedPath TimeExpandedPath used to determine flow and Source Node
	 */
	private void reduceDemand(final TimeExpandedPath TimeExpandedPath) {
		Node source = TimeExpandedPath.getSource();
		if(!this._demands.containsKey(source)){
			throw new IllegalArgumentException("Startnode is no source" + TimeExpandedPath);
		}
		int flow = TimeExpandedPath.getFlow();
		int demand = this._demands.get(source)-flow;
		if(demand<0){
			throw new IllegalArgumentException("too much flow on TimeExpandedPath" + TimeExpandedPath);
		}
		this._demands.put(source, demand);
		if (demand==0){
			this._nonactives.put(source, true);
		}
	}
	
	/**
	 * decides whether a Node is an non active Source
	 * @param node Node to check for	
	 * @return true iff node is a Source with demand 0
	 */
	public boolean isNonActiveSource(final Node node){
		return this._nonactives.get(node);
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
		for (int i=1; i<a.length;i++){
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
		for (int i=1; i<a.length;i++){
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
	public PopulationImpl createPoulation(String oldfile){
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
				for (PathEdge edge : path.getPathEdges()){
					ids.add(edge.getEdge().getId());
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
						ActivityImpl home = new ActivityImpl("h", (LinkImpl)fromlink);
//						home.setLinkId(fromlink.getId());
						Link tolink =_network.getLinks().get(ids.getLast());
						ActivityImpl work = new ActivityImpl("w", (LinkImpl)tolink);
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
					
				/*}  else { // LEAVE THE ROUTES EMPTY! (sadly, this needs different types ...)
					BasicRouteImpl route;				
					route = new BasicRouteImpl(ids.get(0),ids.get(ids.size()-1));
					
					//BasicLegImpl leg = new BasicLegImpl(BasicLeg.Mode.car);
					Leg leg = new org.matsim.population.LegImpl(BasicLeg.Mode.car);
					leg.setRoute(route);
					//BasicActImpl home = new BasicActImpl("h");
					ActImpl home = new org.matsim.population.ActImpl("h", path.getPathEdges().getFirst().getEdge());
					home.setEndTime(0);
					home.setCoord(path.getPathEdges().getFirst().getEdge().getFromNode().getCoord());
					home.setEndTime(path.getPathEdges().getFirst().getTime());
					//BasicActImpl work = new BasicActImpl("w");
					ActImpl work = new org.matsim.population.ActImpl("w", path.getPathEdges().getLast().getEdge());
					work.setEndTime(0);
					work.setCoord(path.getPathEdges().getLast().getEdge().getToNode().getCoord());
					Link fromlink =path.getPathEdges().getFirst().getEdge();
					Link tolink =path.getPathEdges().getLast().getEdge();
					
					home.setLink(fromlink);
					work.setLink(tolink);
					for (int i =1 ; i<= nofpersons;i++){
						Id matsimid  = new IdImpl(id);
						Person p = new PersonImpl(matsimid);
						Plan plan = new org.matsim.population.PlanImpl(p);
						plan.addAct(home);
						plan.addLeg(leg);					
						plan.addAct(work);
						p.addPlan(plan);
						result.addPerson(p);
						id++;
					}
										
				}*/
				
			
				
			}else{
				// TODO this should not happen! just output an error?
				// residual edges
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
		return gain;
	}	
	
//////////////////////////////////////////////////////////////////////////////////////
//-------------------Getters Setters toString---------------------------------------//	
//////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * returns a String representation of a TimeExpandedPath
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
	 * @param demands the _demands to set
	 */
	public void setDemands(HashMap<Node, Integer> demands) {
		this._demands = demands;
	}

	/**
	 * @return the _flow
	 */
	public HashMap<Link, EdgeIntervalls> getFlow() {
		return this._flow;
	}

	/**
	 * @param flow the _flow to set
	 */
	public void setFlow(HashMap<Link, EdgeIntervalls> flow) {
		this._flow = flow;
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
	 * @return the _timeHorizon
	 */
	public int getTimeHorizon() {
		return this._timeHorizon;
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
	
	public int getMaxTravelTimeForLink(Link l)
	{
		return _lengths.get(l).getMaximalTravelTime();
	}

	public EdgeTypeEnum getEdgeType()
	{
		return this.edgeType;
	}
}
