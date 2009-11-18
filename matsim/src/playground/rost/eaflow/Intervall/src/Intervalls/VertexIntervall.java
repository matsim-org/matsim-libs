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


package playground.rost.eaflow.Intervall.src.Intervalls;
import org.matsim.api.core.v01.network.Link;


/**
 * @author Manuel Schneider
 * class representing a node in an time expanded network
 * between two integer Points of time
 * carries a prdecessor node and a min distance for shortest path computation
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
	//TODO predecessor
	private Link _predecessor=null;
	
	private int travelTimeToPredecessor;
	
	//VERY IMPORTANT DEFAULT SETTING.
	//the variable may not be used..
	private int lastDepartureAtFromNode = Integer.MAX_VALUE;
	
	//VERY IMPORTANT DEFAULT SETTING.
	//the variable may not be used..
	private boolean overridable = false;
	

//---------------------------METHODS----------------------------//
//**************************************************************//
	

	public int getTravelTimeToPredecessor() {
		return travelTimeToPredecessor;
	}

	public void setTravelTimeToPredecessor(int travelTimeToPredecessor) {
		this.travelTimeToPredecessor = travelTimeToPredecessor;
	}

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
	 * Creates in interval from l to r
	 * @param l lowbound
	 * @param r highbound
	 */
	public VertexIntervall(int l, int r) {
		super(l, r);
		
	}
	
	/**
	 * * Default costructor creates an (0,1) Intervall 
	 * with d as initial distance to the sink
	 * no predesessor is specified
	 * @param l lowbound
	 * @param r highbound
	 * @param d distance
	 */
	public VertexIntervall(int l, int r, boolean d) {
		super(l, r);
		this.setReachable(d);
	}
	
	/**
	 * * Default costructor creates an (0,1) Intervall 
	 * with d as initial distance to the sink
	 * predesessor will be pred
	 * @param l lowbound
	 * @param r highbound
	 * @param d distance
	 * @param pred Predecessor in a shortest path
	 */
	public VertexIntervall(int l, int r, boolean d, Link pred) {
		super(l, r);
		this.setReachable(d);
		this.setPredecessor(pred);
	}
	
	public VertexIntervall(int l, int r, VertexIntervall other)
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
	public VertexIntervall(Intervall j) {
		super(j.getLowBound(),j.getHighBound());
		
	}

//------------------------DISTANCE----------------------//
	/**
	 * Setter for the min distance to the sink at time lowbound
	 * @param d min distance to sink
	 */
	public void setReachable(boolean d){
		this.reachable=d;
	}
	
	/**
	 * Getter for the min distance to the sink at time lowbound
	 * @return min distance to sink
	 */
	public boolean getReachable(){
		return this.reachable;
	}
	
	
	
//----------------------------PREDECESSOR------------------------//
	
	/**
	 * Setter for the predecessor in a shortest path
	 * @param pred predesessor vertex
	 */
	public void setPredecessor(Link pred){
		this._predecessor=pred;
	}
	
	/**
	 * Getter for the predecessor in a shortest path
	 * @return predecessor vertex 
	 */
	public Link getPredecessor(){
		return this._predecessor;
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
	@Override
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
	
	@Override
	public String toString()
	{
		if(this._predecessor != null)
			return super.toString() + "; reachable: " + this.reachable + "; scanned: " + this.scanned + "; pred: " + this._predecessor.getId().toString() + "; travelTime: " + this.travelTimeToPredecessor;
		else
			return super.toString() + "; reachable: " + this.reachable + "; scanned: " + this.scanned;
	}
	
	public boolean isScanned() {
		return scanned;
	}

	public void setScanned(boolean scanned) {
		this.scanned = scanned;
	}

	public int getLastDepartureAtFromNode() {
		return lastDepartureAtFromNode;
	}

	public void setLastDepartureAtFromNode(int lastArrivalAtThisNode) {
		this.lastDepartureAtFromNode = lastArrivalAtThisNode;
	}

	public boolean isOverridable() {
		return overridable;
	}

	public void setOverridable(boolean overridable) {
		this.overridable = overridable;
	}
	
//----------------------------MAIN METHOD--------------------------//
	
	

}
