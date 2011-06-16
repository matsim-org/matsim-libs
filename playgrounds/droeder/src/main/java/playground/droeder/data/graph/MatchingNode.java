/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.data.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 * @param <T>
 *
 */
public class MatchingNode implements Comparable<MatchingNode>, GraphElement{

	private Id id;
	private Coord coord;
	private Map<Id, MatchingEdge> incoming;
	private Map<Id, MatchingEdge> outgoing;
	private double dist;
	
	public MatchingNode(Id id, Coord coord){
		this.id = id;
		this.coord = coord;
		this.incoming = new HashMap<Id, MatchingEdge>();
		this.outgoing = new HashMap<Id, MatchingEdge>();
		this.dist = Double.MAX_VALUE;
	}

	/**
	 * @return
	 */
	public Id getId() {
		return this.id;
	}
	
	/**
	 * @return
	 */
	public Coord getCoord(){
		return this.coord;
	}
	
	/**
	 * @return
	 */
	public boolean addIncidentEdge(MatchingEdge e){
		if(e.getToNode().getId().equals(this.id)){
			this.incoming.put(e.getId(), e);
			return true;
		}else if(e.getFromNode().getId().equals(this.id)){
			this.outgoing.put(e.getId(), e);
			return true;
		}
		Log.error("given edge is neither incomingEdge nor outgoingEdge!");
		return false;
	}
	
	public Collection<MatchingEdge> getOutEdges(){
		return this.outgoing.values();
	}
	
	
	/**
	 * calculates the distance to the given coordinate, so Collection.sort() can work
	 * @param c
	 */
	public void setDist(Coord c){
		Double a = c.getX() - this.coord.getX();
		Double b = c.getY() - this.coord.getY();
		this.dist = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
	}
	
	/**
	 * call setDist(Coord c) before, otherwise a Double.MAX_VALUE is returned
	 * @return
	 */
	public Double getDist(){
		return this.dist;
	}

	@Override
	public int compareTo(MatchingNode o) {
		if(this.dist < o.getDist()){
			return -1;
		}else if(this.dist == o.getDist()){
			return 0;
		}else{
			return 1;
		}
	}

}
