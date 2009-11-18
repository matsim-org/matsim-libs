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

package playground.rost.eaflow.ea_flow;

//java imports
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

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
	 * the actual path in order from the sink
	 */
	private LinkedList<PathEdge> _edges;
	
	/**
	 * arrivaltime of a path
	 */
	private int _arrival;
	
	/**
	 * time, that the path wait in the source
	 */
	private int _wait;
	
	
	/**
	 * currently aequivalent to _wait, will change in future!
	 */
	private int startTime;
	

	/**
	 * class variable to turn on debug mode, default is off
	 */
	
	@SuppressWarnings("unused")
	private static boolean _debug = false;
	
	/**
	 * Class representing an edge in a path with flow over time on an network
	 * @author Manuel Schneider
	 *
	 */
	public class PathEdge {
		
		/**
		 * Edge in a path
		 */
		private final Link edge;
		
		/**
		 * time upon which the flow enters the edge
		 */
		private final int startTime;
		
		/**
		 * time upon the flow arrivs at the toNode
		 */
		private final int arrivalTime;
		
		
		/**
		 * reminder if this is a forward edge or not
		 */
		private final boolean forward;
		
		/**
		 * default Constructor setting the Arguments
		 * @param edge Link used
		 * @param time starting time
		 * @param forward flag if edge is forward or backward
		 */
		PathEdge(Link edge, int startTime, int arrivalTime, boolean forward){
			this.startTime = startTime;
			this.arrivalTime = arrivalTime;
			this.edge = edge;
			this.forward = forward;
		}
		
		/**
		 * Method returning a String representation of the PathEdge
		 */
		@Override
		public String toString(){
			String s = this.startTime + " " + edge.getFromNode().getId().toString()+"-->" + edge.getToNode().getId().toString() + " " +this.arrivalTime;
			if(!this.forward){
				s += " backwards";
			}
			return s;
		}

		/**
		 * Getter for the Link used
		 * @return the edge
		 */
		public Link getEdge() {
			return edge;
		}

		/**
		 * checks weather the link is used in forward direction
		 * @return the forward
		 */
		public boolean isForward() {
			return forward;
		}

		/**
		 * getter for the time at which an edge is entered
		 * @return the time
		 */
		public int getStartTime() {
			return startTime;
		}
		
		public int getArrivalTime()
		{
			return arrivalTime;
		}
		
		public boolean equals(PathEdge other)
		{
			if(this.getStartTime() == other.startTime
					&& this.getEdge().equals(other.getEdge())
					&& this.getArrivalTime() == other.getArrivalTime())
			{
				return true;
			}
			return false;
			
		}
	}
	
	/**
	 * Default Constructor creating a Path with flow value 0 and no edges
	 */
	public TimeExpandedPath(){
		this._flow = 0;
		this._edges = new LinkedList<PathEdge>();
	}
	
	/**
	 * Method to append a new Edge to the end of the path with the specified input
	 * @param edge Link used
	 * @param time starting time
	 * @param forward flag if edge is forward or backward
	 * @exception throws an IllegalArgumentException if the new edge is not adjacent to te last edge in the path
	 */
	public void append(Link edge, int startTime, int arrivalTime, boolean forward){
		//adding first PathEdge
		if(this._edges.isEmpty()){
			PathEdge temp =new PathEdge(edge, startTime, arrivalTime, forward);
			this._edges.addLast(temp);
		}else{
			PathEdge old = this._edges.getLast();
			PathEdge temp =new PathEdge(edge, startTime, arrivalTime, forward);
			if(checkPair(old,temp)){
				this._edges.addLast(temp);
			}else{
				throw new IllegalArgumentException("non adjacent last PathEdge: ... " + old.toString() +" "+ temp.toString() ); 
			}
		}
	}
	
	/**
	 * Method to push a new Edge to the beginning of the path with the specified input
	 * @param edge Link used
	 * @param time starting time
	 * @param forward flag if edge is forward or backward
	 * @exception throws an IllegalArgumentException if the new edge is not adjacent to te first edge in the path
	 */
	public void push(Link edge, int startTime, int arrivalTime, boolean forward){
		if(this._edges.isEmpty()){
			PathEdge temp =new PathEdge(edge, startTime, arrivalTime, forward);
			this._edges.addFirst(temp);
		}else{
			PathEdge old = this._edges.getFirst();
			PathEdge temp =new PathEdge(edge, startTime, arrivalTime, forward);
			if(checkPair(temp,old)){
				this._edges.addFirst(temp);
			}else{
				throw new IllegalArgumentException("non adjacent first PathEdge:" + temp.toString() + old.toString()+"..." ); 
			}
		}
	}
	
	/**
	 * checks weather two edges are adjacent with respect to their direction used
	 * does not account for valid times
	 * @param first first edge in order traversion of the path
	 * @param second second edge in order traversion of the path
	 * @return true iff a path could go over first and over second immediatly after
	 */
	private static boolean checkPair(PathEdge first, PathEdge second){
		Node node;
		if(first.forward){
			node = first.edge.getToNode();
		}else{
			node = first.edge.getFromNode();
		}
		if(second.forward){
			return(node==second.edge.getFromNode());
		}else{
			return(node==second.edge.getToNode());
		}
		
	}
	
	/**
	 * checks weather a path is consistent with respect to adjacency of its edges in the specified order, 
	 * does not acount for valid times 
	 * @return true iff refrenced Object describes a path
	 */
	public boolean check(){
		ListIterator<PathEdge> iter = this._edges.listIterator();
		PathEdge last = iter.next();
		while(iter.hasNext()){
			PathEdge next = iter.next();
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
		for(PathEdge edge : this._edges){
			if (!edge.isForward()){
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
		PathEdge firstedge = this._edges.getFirst();
		Node result;
		if(firstedge.isForward()){
			result = firstedge.getEdge().getFromNode();
		}else{
			result = firstedge.getEdge().getToNode();
		}
		return result;
	}
	
	/**
	 * returns a subpath of the path from "from" to "to"
	 * @param fromNode from
	 * @param toNode to
	 * @return subpath
	 */
	public TimeExpandedPath getSubPath(int from, int to){
		TimeExpandedPath result = null;
		if(from <= to){
			if((from < this._edges.size()) && (from >= 0)){
				if((to < this._edges.size()) && (to >= 0)){
					result = new TimeExpandedPath();
					for(int i = from; i <= to; i++){
						result.append(this._edges.get(i).edge, this._edges.get(i).startTime, this._edges.get(i).arrivalTime, this._edges.get(i).isForward());
					}
				}
			}
		}
		if(result == null){
			System.out.println("Indices doesn't match");
		}
		return result;
	}
	
	/**
	 * Method to indicate, if link is in a path
	 * @param PathEdge edge
	 * @return boolean 
	 */
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
	}
	
	/**
	 * Method to find the forward link of an backward link in a path
	 * @param PathEdge edge
	 * @return PathEdge 
	 */
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
	}
	
	/**
	 * Method to find the index of the forward link of an backward link in a path
	 * @param PathEdge edge
	 * @return index of forward link 
	 */
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
	}
	
	/**
	 * returns a String representation of the Path
	 */
	@Override
	public String toString(){
		StringBuilder strb = new StringBuilder();
		strb.append("f: "+this._flow+" on: ");
		for (PathEdge edge : this._edges){
			strb.append(" |" + edge.toString() + "| ");
		}	
		strb.append("arrivaltime: " + _arrival);
		return strb.toString();
	}
	
	/**
	 * Getter for the List of PathEdges of which the Path consitst 
	 * @return List of PathEdges in order of thier traversal
	 */
	public LinkedList<PathEdge> getPathEdges(){
		return this._edges;
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
	 * setting the time the path wait in the source
	 * @param time
	 */
	public void setWait(int time){
		this._wait = time;
	}
	
	/**
	 * getter for time, the path wait in the source, if it is set
	 * @return wait time
	 */
	public int getWait(){
		return this._wait;
	}
	
	/**
	 * getter for length of the path
	 * @return length
	 */
	public int length(){
		return this._edges.size();
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
	 */
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
	}
	
	public List<TimeExpandedPath> splitPathAtEdge(PathEdge pathEdgeToSplitAt, boolean testForward)
	{
		List<TimeExpandedPath> result = new LinkedList<TimeExpandedPath>();
		TimeExpandedPath head = new TimeExpandedPath();
		TimeExpandedPath tail = new TimeExpandedPath();
		head.setArrival(this._arrival);
		tail.setArrival(this._arrival);
		head.setFlow(this._flow);
		tail.setFlow(this._flow);
		boolean preSplit = true;
		for(PathEdge pE : this._edges)
		{
			if(pE.equals(pathEdgeToSplitAt) && (!testForward || pE.isForward() == pathEdgeToSplitAt.forward))
			{
				preSplit = false;
				continue;
			}
			if(preSplit)
			{
				if(pE.isForward())
				{
					head.append(pE.edge, pE.getStartTime(), pE.getArrivalTime(), pE.isForward());
				}
				else
				{
					head.append(pE.edge, pE.getStartTime(), pE.getArrivalTime(), pE.isForward());
				}
			}
			else
			{
				if(pE.isForward())
				{
					tail.append(pE.edge, pE.getStartTime(), pE.getArrivalTime(), pE.isForward());
				}
				else
				{
					tail.append(pE.edge, pE.getStartTime() , pE.getArrivalTime(), pE.isForward());
				}
			}
		}
		result.add(head);
		result.add(tail);
		return result;
	}
	
	public void addTailToPath(TimeExpandedPath other)
	{
		for(PathEdge pE : other.getPathEdges())
		{
			this.append(pE);
		}
		this.setArrival(other.getArrival());
	}
	
	public void append(PathEdge pE)
	{
		this.append(pE.getEdge(), pE.getStartTime(), pE.getArrivalTime(), pE.isForward());
	}
	
	public static TimeExpandedPath clone(TimeExpandedPath original)
	{
		TimeExpandedPath copy = new TimeExpandedPath();
		copy.setArrival(original.getArrival());
		copy.setFlow(original.getArrival());
		copy.setWait(original.getWait());
		for(PathEdge pE : original.getPathEdges())
		{
			copy.append(pE);
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
	
}
