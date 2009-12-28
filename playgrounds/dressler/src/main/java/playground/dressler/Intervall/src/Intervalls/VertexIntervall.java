/* *********************************************************************** *
 * project: org.matsim.*
 * VertexIntervall.java
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


package playground.dressler.Intervall.src.Intervalls;

//matsim imports
import org.matsim.api.core.v01.network.Link;


/**
 * @author Manuel Schneider
 * class representing a node in an time expanded network
 * between two integer Points of time
 * carries a predecessor node and a minimal distance for shortest path computation
 */
public class VertexIntervall extends Intervall {

//---------------------------FIELDS----------------------------//	
	
	/**
	 * shows whether the vertex is reacheable during the time intervall
	 */
	private boolean reachable = false;
	
	private boolean scanned = false;


	/**
	 * predecessor in a shortest path
	 */
	private Link _predecessor=null;
	
	/**
	 * travel time to predecessor
	 */
	private int travelTimeToPredecessor;
	
	//VERY IMPORTANT DEFAULT SETTING.
	//the variable may not be used..
	private int lastDepartureAtFromNode = Integer.MAX_VALUE;
	
	//VERY IMPORTANT DEFAULT SETTING.
	//the variable may not be used..
	private boolean overridable = false;
	

//---------------------------METHODS----------------------------//
//**************************************************************//
	


//--------------------------CONSTUCTORS-------------------------//
	/**
	 * Default costructor creates an (0,1) Intervall 
	 * with Integer.MAX_VALUE as initial distance to the sink
	 * no predesessor is specified
	 */
	public VertexIntervall() {
		super();
	}

	/**
	 * Creates in VertexIntervall from l to r
	 * @param l lowbound
	 * @param r highbound
	 */
	public VertexIntervall(final int l,final int r) {
		super(l, r);
	}
	
	/**
	 * construct an VertexIntervall from l to r wit he settings of other
	 * @param l lowbound
	 * @param r highbound
	 * @param other Intervall to copy settings from
	 */
	public VertexIntervall(final int l,final int r,final VertexIntervall other)
	{
		super(l,r);
		this.setLastDepartureAtFromNode(other.lastDepartureAtFromNode);
		this.setPredecessor(other._predecessor);
		this.setReachable(other.reachable);
		this.setScanned(other.isScanned());
		this.setOverridable(other.overridable);
		this.setTravelTimeToPredecessor(other.travelTimeToPredecessor);
	}

	/**
	 * creates an VertexIntervall instance as a copy of an Intervall
	 * Predecessor is set to null and dist to Integer.MAX_VALUE
	 * @param j Intervall to copy
	 */
	public VertexIntervall(final Intervall j) {
		super(j.getLowBound(),j.getHighBound());
		
	}

//------------------------Getter Setter----------------------//
	/**
	 * Setter for the min distance to the sink at time lowbound
	 * @param d min distance to sink
	 */
	public void setReachable(final boolean d){
		this.reachable=d;
	}
	
	/**
	 * Getter for the min distance to the sink at time lowbound
	 * @return min distance to sink
	 */
	public boolean getReachable(){
		return this.reachable;
	}
	
	
	/**
	 * Setter for the predecessor in a shortest path
	 * @param pred predesessor vertex
	 */
	public void setPredecessor(final Link pred){
		this._predecessor=pred;
	}
	
	/**
	 * Getter for the predecessor in a shortest path
	 * @return predecessor vertex 
	 */
	public Link getPredecessor(){
		return this._predecessor;
	}
	
	/**
	 * getter for travelTimeToPredecessor
	 * @return travelTimeToPredecessor
	 */
	public int getTravelTimeToPredecessor() {
		return travelTimeToPredecessor;
	}

	/**
	 * setter for travelTimeToPredecessor
	 * @param travelTimeToPredecessor
	 */
	public void setTravelTimeToPredecessor(final int travelTimeToPredecessor) {
		this.travelTimeToPredecessor = travelTimeToPredecessor;
	}
	
	/**
	 * getter for scanned
	 * @return scanned
	 */
	public boolean isScanned() {
		return scanned;
	}

	/**
	 * setter for scanned
	 * @param scanned
	 */
	public void setScanned(final boolean scanned) {
		this.scanned = scanned;
	}
	/**
	 * getter for lastDepartureAtFromNode
	 * @return lastDepartureAtFromNode
	 */
	public int getLastDepartureAtFromNode() {
		return lastDepartureAtFromNode;
	}

	/**
	 * setter for lastDepartureAtFromNode
	 * @param lastArrivalAtThisNode
	 */
	public void setLastDepartureAtFromNode(final int lastArrivalAtThisNode) {
		this.lastDepartureAtFromNode = lastArrivalAtThisNode;
	}

	/**
	 * getter for overridable
	 * @return overridable
	 */
	public boolean isOverridable() {
		return overridable;
	}

	/**
	 * setter for overridable
	 * @param overridable
	 */
	public void setOverridable(final boolean overridable) {
		this.overridable = overridable;
	}
	
//----------------------------SPLITTING----------------------------//
	
	/**
	 * splits the referenced VertexIntervall at t contained in (lowbound, higbound)
	 * by changeing!! referenced Intervall to (lowbound,t) 
	 * and creating a new EdgeInterval (t,highbound) with same Predecesor as the referenced
	 * Min Dist is set to dist at time  t
	 *@param t point to split at
	 *@return new Interval 
	 */
	public VertexIntervall splitAt(int t){
		boolean newdist = this.getReachable();
		Intervall j =super.splitAt(t);
		VertexIntervall k = new VertexIntervall(j);
		k.reachable = newdist;
		k.scanned = this.isScanned();
		k._predecessor= this._predecessor;
		k.travelTimeToPredecessor = this.travelTimeToPredecessor;
		k.lastDepartureAtFromNode = this.lastDepartureAtFromNode;
		k.overridable = this.overridable;
		return k;
	}
	
	/**
	 * gives a string representation of tje VertexIntervall of the form with optional predecessor
	 * [x,y): reachable: true scanned: false pred: 
	 * @return string representation 
	 */
	 
	public String toString()
	{
		if(this._predecessor != null)
			return super.toString() + "; reachable: " + this.reachable + "; scanned: " + this.scanned + "; pred: " + this._predecessor.getId().toString() + "; travelTime: " + this.travelTimeToPredecessor;
		else
			return super.toString() + "; reachable: " + this.reachable + "; scanned: " + this.scanned;
	}
}
