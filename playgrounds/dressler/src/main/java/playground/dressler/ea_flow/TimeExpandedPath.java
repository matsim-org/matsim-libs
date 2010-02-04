/* *********************************************************************** *
 * project: org.matsim.*
 * TimeExpandedPath.java
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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

//matsim imports
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

/**
 * Class representing a path with flow over time on an network
 * @author Manuel Schneider
 *
 */
public class TimeExpandedPath {
	
	/**
	 * amount of flow on the path
	 */
	private int _flow;
	
	/**
	 * the actual path in order from the source
	 */
	private LinkedList<PathStep> _steps;
	
	/**
	 * arrivaltime of a path
	 */
	private int _arrival;
	

	/**
	 * time, that the path wait in the source
	 */
	private int startTime;
	

	/**
	 * class variable to turn on debug mode, default is off
	 */
	
	@SuppressWarnings("unused")
	private static boolean _debug = false;
	
	/**
	 * Class representing an edge in a path with flow over time on an network
	 * it can also represent sourceoutflow or holdover if edge == null
	 * @author Manuel Schneider, Daniel Dressler
	 *
	 */	
	
	/**
	 * Default Constructor creating a Path with flow value 0 and no edges
	 */
	public TimeExpandedPath(){
		this._flow = 0;
		this._steps = new LinkedList<PathStep>();
	}
	
	/**
	 * Method to append a new Edge to the end of the path with the specified input
	 * @param edge Link used
	 * @param time starting time
	 * @param forward flag if edge is forward or backward
	 * @exception throws an IllegalArgumentException if the new edge is not adjacent to te last edge in the path
	 */
	public void append(Link edge, int startTime, int arrivalTime, boolean forward){
		//adding first PathStep
		StepEdge temp = new StepEdge(edge, startTime, arrivalTime, forward);
		if(this._steps.isEmpty()){			
			this._steps.addLast(temp);
		}else{
			PathStep old = this._steps.getLast();			
			if(checkPair(old,temp)){
				this._steps.addLast(temp);
			}else{
				throw new IllegalArgumentException("non adjacent PathSteps: ... " + old.toString() +" "+ temp.toString() ); 
			}
		}
	}
	

	/**
	 * Method to prepend a new Edge to the beginning of the path with the specified input
	 * @param edge Link used
	 * @param time starting time
	 * @param forward flag if edge is forward or backward
	 * @exception throws an IllegalArgumentException if the new edge is not adjacent to te first edge in the path
	 */
	public void prepend(Link edge, int startTime, int arrivalTime, boolean forward){
		//adding first PathStep
		StepEdge temp = new StepEdge(edge, startTime, arrivalTime, forward);
		if(this._steps.isEmpty()){			
			this._steps.addLast(temp);
		}else{
			PathStep old = this._steps.getFirst();			
			if(checkPair(temp, old)){
				this._steps.addLast(temp);
			}else{
				throw new IllegalArgumentException("non adjacent PathSteps: ... " + temp.toString() +" "+ old.toString() ); 
			}
		}		
	}
	
	/**
	 * checks whether two steps are adjacent with respect to their direction and time 
	 * @param first first edge in order traversion of the path
	 * @param second second edge in order traversion of the path
	 * @return true iff a path could go over first and over second immediatly after
	 */
	private static boolean checkPair(PathStep first, PathStep second){
		if (first.getArrivalTime() == second.getStartTime()) {
			return first.getArrivalNode().equals(second.getStartNode()); 
		} else {
			return false;
		}				 
	}
	
	/**
	 * checks whether a path is consistent with respect to adjacency of its edges in the specified order, 
	 * also checks arrival and departure times  
	 * @return true iff refrenced Object describes a path
	 */
	public boolean check(){
		ListIterator<PathStep> iter = this._steps.listIterator();
		PathStep last = iter.next();
		while(iter.hasNext()){
			PathStep next = iter.next();
			if(!checkPair(last,next)){
				return false;
			}
			last=next;
		}
		return true;
	}
	
	/**
	 * checks whether all edges in the path are forward edges
	 * @return true iff all edges are forward
	 */
	public boolean isforward() {
		for(PathStep step: this._steps){
			if (!step.getForward()){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Method to find the First node in a path
	 * @return first Node 
	 */
	public Node getSource(){
		PathStep step = this._steps.getFirst();

		return step.getStartNode();
	}
	
	/**
	 * returns a subpath of the path from "from" to "to"
	 * @param fromNode from
	 * @param toNode to
	 * @return subpath
	 *//*
	public TimeExpandedPath getSubPath(int from, int to){
		TimeExpandedPath result = null;
		if(from <= to){
			if((from < this._edges.size()) && (from >= 0)){
				if((to < this._edges.size()) && (to >= 0)){
					result = new TimeExpandedPath();
					for(int i = from; i <= to; i++){
						result.append(this._edges.get(i));
					}
				}
			}
		}
		if(result == null){
			System.out.println("Indices don't match");
		}
		return result;
	}*/
	
	/**
	 * Method to indicate, if link is in a path
	 * @param PathEdge edge
	 * @return boolean 
	 *//*
	public boolean containsForwardLink(PathEdge edge){
		if(edge.isForward()){
			System.out.println("Error: Try to find forward link of an forward link.");
			return false;
		}
		boolean result = false;
		for(PathEdge pathEdge : this._edges){
			if(pathEdge.getEdge().equals(edge.getEdge())){
				if(pathEdge.getStartTime() == edge.getStartTime()
						&& pathEdge.getArrivalTime() == edge.getArrivalTime()){
					result = true;
					break;
				}
			}
		}
		return result;
	}*/
	
	/**
	 * Method to find the forward link of an backward link in a path
	 * @param PathEdge edge
	 * @return PathEdge 
	 *//*
	public PathEdge getForwardLink(PathEdge edge){
		if(edge.isForward()){
			System.out.println("Error: Try to find forward link of an forward link.");
			return null;
		}
		if(!containsForwardLink(edge)){
			System.out.println("Error: Forward link is not contained in this path.");
			return null;
		}
		PathEdge result = null;
		for(PathEdge pathEdge : this._edges){
			if(pathEdge.getEdge().equals(edge.getEdge())){
				if(pathEdge.getStartTime() == edge.getStartTime()
						&& pathEdge.getArrivalTime() == edge.getArrivalTime()){
					result = pathEdge;
					break;
				}
			}
		}
		return result;
	}*/
	
	/**
	 * Method to find the index of the forward link of an backward link in a path
	 * @param PathEdge edge
	 * @return index of forward link 
	 *//*
	public Integer getIndexOfForwardLink(PathEdge edge){
		if(edge.isForward()){
			System.out.println("Error: Try to find forward link of an forward link.");
			return null;
		}
		if(!containsForwardLink(edge)){
			System.out.println("Error: Forward link is not contained in this path.");
			return null;
		}
		Integer result = 0;
		for(PathEdge pathEdge : this._edges){
			if(pathEdge.getEdge().equals(edge.getEdge())){
				if(pathEdge.getStartTime() == edge.getStartTime()
						&& pathEdge.getArrivalTime() == edge.getArrivalTime()){
					break;
				}
			}
			result++;
		}
		if(result >= this._edges.size()){
			System.out.println("Error: No index found!");
			return null;
		}
		return result;
	}*/
	
	/**
	 * returns a String representation of the Path
	 */
	@Override
	public String toString(){
		StringBuilder strb = new StringBuilder();
		strb.append("f: "+this._flow+" on: ");
		for (PathStep step : this._steps){
			strb.append(" |" + step.toString() + "| ");
		}	
		strb.append("arrivaltime: " + _arrival);
		return strb.toString();
	}
	
	/**
	 * Getter for the List of PathEdges of which the Path consitst 
	 * @return List of PathEdges in order of thier traversal
	 */
	public LinkedList<PathStep> getPathSteps(){
		return this._steps;
	}
	
	/**
	 * Setter for the amount of flow on the path
	 * @param flow nonnegative flow on the path
	 * @exception throws an IllegalArgumentException iff flow is negative
	 */
	public void setFlow(int flow){
		if(flow<0){
			throw new IllegalArgumentException("negative flow value!");
		}
		this._flow = flow;
	}
	
	/**
	 * getter for the amount of flow on a Path
	 * @return flow on the Path
	 */
	public int getFlow(){
		 return this._flow;
	 }
	
	/**
	 * setting the arrival time at the final node
	 * @param time
	 */
	public void setArrival(int time){
		this._arrival = time;
	}
	
	/**
	 * getter for arrival time if it is set
	 * @return arrival time
	 */
	public int getArrival(){
		return this._arrival;
	}
	
	
	/**
	 * getter for length of the path
	 * @return length
	 */
	public int length(){
		return this._steps.size();
	}
	
	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(boolean debug){
		TimeExpandedPath._debug=debug;
	}


	/**
	 * print the path
	 *//*
	public void print(){
		Link edge;
		System.out.println("Path waits at source " + this._wait);
		for(PathEdge pE : this.getPathEdges())
		{
			System.out.println("startTime: " + pE.getStartTime() + "; " + pE.getEdge().getFromNode().getId().toString()+  "-->" + pE.getEdge().getToNode().getId().toString() + " " + pE.getArrivalTime());
			if(!pE.isForward())
				System.out.println("(backward above)");
		}
		System.out.println();
		System.out.println("Path arrives at sink at " + this._arrival);
		System.out.println("Path has flow " + this._flow);
		System.out.println();
	}*/
	
	/**
	 * Split a path at a given pathEdge
	 * @param pathEdgeToSplitAt this edge will be in neither of the parts
	 * @param testForward check the direction 
	 * @return length
	 */
	public List<TimeExpandedPath> splitPathAtStep(PathStep stepToSplitAt, boolean testForward)
	{
		List<TimeExpandedPath> result = new LinkedList<TimeExpandedPath>();
		TimeExpandedPath head = new TimeExpandedPath();
		TimeExpandedPath tail = new TimeExpandedPath();
		head.setArrival(this._arrival);
		tail.setArrival(this._arrival);
		head.setFlow(this._flow);
		tail.setFlow(this._flow);
		boolean preSplit = true;
		for(PathStep step : this._steps)
		{
			// that testFowrard check is always redundant with PathEdge.equals !
			//if(pE.equals(pathEdgeToSplitAt) && (!testForward || pE.isForward() == pathEdgeToSplitAt.forward))
			// FIXME gehoert das so mit equals? oder doch forward egal?
			if(step.equals(stepToSplitAt))
			{
				preSplit = false;
				continue;
			}
			if(preSplit)
			{
				head.append(step);				
			}
			else
			{
				tail.append(step);				
			}
		}
		result.add(head);
		result.add(tail);
		return result;
	}
	
	public void addTailToPath(TimeExpandedPath other)
	{
		for(PathStep step : other.getPathSteps())
		{
			this.append(step);
		}
		this.setArrival(other.getArrival());
	}
	
	public void append(PathStep step)
	{
		this._steps.addLast(step);
	}
	
	public void prepend(PathStep step)
	{
		this._steps.addFirst(step);
	}
	
	public static TimeExpandedPath clone(TimeExpandedPath original)
	{
		TimeExpandedPath copy = new TimeExpandedPath();
		copy.setArrival(original.getArrival());
		copy.setFlow(original.getArrival());		
		for(PathStep step : original.getPathSteps())
		{
			copy.append(step);
		}
		return copy;
	}
	
	public void addFlow(int flow)
	{
		this._flow += flow;
	}
	
	public void subtractFlow(int flow)
	{
		this._flow -= flow;
	}
	
	
	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	/**
	 * Make sure that TimeExpandedPath starts with a PathStep that is a StepSourceFlow!
	 * @return true iff something was fixed
	 */
	public boolean hadToFixSourceLinks() {
		PathStep step = this._steps.getFirst();
		if (!(step instanceof StepSourceFlow)) {		
			StepSourceFlow newstep = new StepSourceFlow(step.getStartNode(), step.getStartTime(), true);
			this._steps.addFirst(newstep);
			return true;
		}
		return false;
	}
	
}
