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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import playground.dressler.network.IndexedNodeI;

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
	 * time, that the path wait in the source
	 */
	private int startTime;
	
	/*
	 * which real nodes are part of this path?
	 * only build when needed  
	 */
	private HashSet<IndexedNodeI> nodesTouched = null;
	

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
		this.startTime = 0;
	}
	
	/**
	 * Copy Constructor, but note that the PathSteps are still the same objects
	 */
	public TimeExpandedPath(TimeExpandedPath other) {
	  this._flow = other._flow;
	  this._steps = new LinkedList<PathStep>(other._steps);
	  this.startTime = other.startTime;
	}
	
	
	/**
	 * checks whether two steps are adjacent with respect to their direction and time 
	 * @param first first edge in order traversion of the path
	 * @param second second edge in order traversion of the path
	 * @return true iff a path could go over first and over second immediatly after
	 */
	private static boolean checkPair(PathStep first, PathStep second){
		return (first.getArrivalNode().equals(second.getStartNode()));
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
	 * This function ignores virtual nodes! 
	 * @return first Node 
	 */
	public IndexedNodeI getSource(){
		PathStep step = this._steps.getFirst();

		return step.getStartNode().getRealNode();
	}
	
	/**
	 * Method to find the last node in a path
	 * This function ignores virtual nodes! 
	 * @return first Node 
	 */
	public IndexedNodeI getSink(){
		PathStep step = this._steps.getLast();

		return step.getArrivalNode().getRealNode();
	}
	
	
	/**
	 * returns a copy of the subpath determined by the indices
	 * @param start index of the first step to be included
	 * @param end index of the first step to not be included
	 * @return subpath
	 */
	public TimeExpandedPath getSubPath(int start, int end){
		
		if (start >= end) return null;
		
		TimeExpandedPath result = new TimeExpandedPath();
		int i = 0;
		for (PathStep step : this._steps) {
		   if (i >= start) {
			   if (i < end) {
				   result.append(step.copyShifted(0));
			   } else {
				   break;
			   }
		   }
		   i++;
		}
		
		return result;
	}
	
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
	 * getter for arrival time if it is set
	 * @return arrival time
	 */
	public int getArrival(){
		// weird, but for a StepSinkFlow, the departure time is when the sink is entered.
		return this._steps.getLast().getStartTime(); 
	}
	
	/**
	 * sums up the cost of the pathsteps!
	 * @return the total cost
	 */
	public int getCost() {
		int cost = 0;
		for (PathStep step : this._steps) {
			cost += step.getCost();
		}
		return cost;
	}
	
	/**
	 * getter for length of the path in steps (not cost or time or anything clever)
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

	/* changes all steps so that the last step arrives at newArrival
	 * @param newArrival time to arrive
	 * @return the latest time used in the new path, usually this is newArrival  
	 */
	public int shiftToArrival(int newArrival) {
		int latestUsed = 0;

		if (this._steps == null || this._steps.isEmpty()) {
			return -1;
		}

		int shift = newArrival - this.getArrival();

		LinkedList<PathStep> newSteps = new LinkedList<PathStep>();

		for (PathStep step : this._steps) {
			PathStep newStep = step.copyShifted(shift);
			newSteps.addLast(newStep);  
			latestUsed = Math.max(latestUsed, newStep.getStartTime());
			latestUsed = Math.max(latestUsed, newStep.getArrivalTime());
		}

		this._steps = newSteps;

		return latestUsed;
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
	 * @param testForward check the direction as well
	 * @return length
	 */
	public LinkedList<TimeExpandedPath> splitPathAtStep(PathStep stepToSplitAt, boolean testForward)
	{
		LinkedList<TimeExpandedPath> result = new LinkedList<TimeExpandedPath>();
		TimeExpandedPath head = new TimeExpandedPath();
		TimeExpandedPath tail = new TimeExpandedPath();
		
		head.setFlow(this._flow);
		tail.setFlow(this._flow);
		
		boolean preSplit = true;
		if (testForward) {
			for(PathStep step : this._steps)
			{			
				// should there be a second copy of stepToSplitAt in TEP,
				// it will be output ... otherwise it would have been dropped silently.
				if(preSplit && step.equals(stepToSplitAt)) 	{
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
		} else {
			for(PathStep step : this._steps)
			{
				// should there be a second copy of stepToSplitAt in TEP,
				// it will be output ... otherwise it would have been dropped silently.				
				if(preSplit && step.equalsNoCheckForward(stepToSplitAt)) 	{
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
	}
	
	public void append(PathStep step)
	{
		this._steps.addLast(step);
	}
	
	public void append(List<PathStep> steps)
	{
		for (PathStep step : steps) 
			this._steps.addLast(step);
	}
	
	public void prepend(PathStep step)
	{
		this._steps.addFirst(step);
	}
	
	public void prepend(LinkedList<PathStep> steps)
	{
		Iterator<PathStep >iter = steps.descendingIterator();
		
		while (iter.hasNext()) {
			this._steps.addFirst(iter.next());	
		}
	}
	
	public void removeLast() {
		this._steps.removeLast();
	}
	
	public void removeFirst() {
		this._steps.removeFirst();
	}
	
	/* Creates a new, deep copy of a TimeExpandedPath 
	 * 
	 */
	public static TimeExpandedPath clone(TimeExpandedPath original)
	{
		TimeExpandedPath copy = new TimeExpandedPath();		
		copy.setFlow(original.getFlow());		
		for(PathStep step : original.getPathSteps())
		{	
			// there is no real clone function for the PathSteps, but this works.
			copy.append(step.copyShifted(0)); 
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
	

	/**
	 * Make sure that TimeExpandedPath starts with a PathStep that is a StepSourceFlow!
	 * @return true iff something was fixed
	 */
	public boolean hadToFixSourceLinks() {
		PathStep step = this._steps.getFirst();
		if (!(step instanceof StepSourceFlow)) {		
			StepSourceFlow newstep = new StepSourceFlow(step.getStartNode().getRealNode(), step.getStartTime(), true);
			this._steps.addFirst(newstep);
			return true;
		}
		return false;
	}
	
	
	/*
	 *  method to rebuild the set of touched nodes
	 */
	public void rebuildTouchedNodes() {
	  this.nodesTouched = new HashSet<IndexedNodeI>(3*this._steps.size()/2); // effectively a bit too big!
	  for (PathStep step : this._steps) {
		  this.nodesTouched.add(step.getStartNode().getRealNode());
		  this.nodesTouched.add(step.getArrivalNode().getRealNode());
	  }
	  //System.out.println("Rebuilding ...");
	}
	
	/*
	 * method to check whether a (real) node is used at all in the path
	 * this only works reliable if touchednodes is updated after any change
	 * which is not done automatically!
	 */	
	public boolean doesTouch(IndexedNodeI node) {
		// careful ... this might not be up to date if the TEP was changed!
		
		if (this.nodesTouched == null) {
			rebuildTouchedNodes();
		}
		return (this.nodesTouched.contains(node)); 
	}
	
	
	/*
	 * method to check whether the real node associated with the virtual node is used at all in the path
	 * this only works reliable if touchednodes is updated after any change
	 * which is not done automatically!
	 */	
	public boolean doesTouch(VirtualNode node) {
		// careful ... this might not be up to date if the TEP was changed!
		
		if (this.nodesTouched == null) {
			rebuildTouchedNodes();
		}
		return (this.nodesTouched.contains(node.getRealNode()));
	}
	
	public String print(){
		StringBuilder temp = new StringBuilder();
		temp.append("Path:"+_flow);
		for(PathStep step : _steps){
			temp.append(";");
			temp.append(step.print());
		}
		return temp.toString();
	}
	
	/**
	 * Returns a simplified (no loops) copy of this path.
     * Works regardless of whether the path is forward,
     * but makes less sense to call for non-forward paths.
     * 
     * NB: This does not consider Holdover-Steps properly! Those may still overlap.
     * @return a fresh path (mostly) without loops, with the same start, end, and flow value 
	 */
    public TimeExpandedPath simplify() {
    	// TODO handle StepHoldover properly!
    	
       TimeExpandedPath newTEP = new TimeExpandedPath();
       newTEP.setFlow(this._flow);              
              
       // a hashmap from a VirtualNode to the next step.
       // using the String representation makes this a lot safer
       HashMap<String, PathStep> shortcuts = new HashMap<String, PathStep>();

       // go through the list, denoting the next step for each VirtualNode
       for (PathStep step : this._steps) {
    	  shortcuts.put(step.getStartNode().toString(), step);   
       }
       
       // go through the hashmap, reconstructing a path
       
       VirtualNode node = this._steps.getFirst().getStartNode();
       while (node != null) {
    	   PathStep step = shortcuts.get(node.toString());
    	   if (step != null) {
    		   // copy the step!
    		   newTEP.append(step.copyShifted(0));    		  
    		   node = step.getArrivalNode();
    	   } else {
    		  node = null;
    	   }
       }
       
       // DEBUG
       /*String before = this.toString();
       String after = newTEP.toString();
       
       if (!after.equals(before)) {
    	  System.out.println("Simplified path. Before: ");
    	  System.out.println(before);
    	  System.out.println("After: ");
    	  System.out.println(after);
       }*/
       
       return newTEP;
    }
}
