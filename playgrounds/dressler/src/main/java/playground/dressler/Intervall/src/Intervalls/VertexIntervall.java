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

import playground.dressler.ea_flow.PathStep;


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
	 * the times stored in the step are valid for the low bound!
	 */
	private PathStep _predecessor=null;
	

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
		// dummy values
		this.reachable = false;
		this.scanned = false;
		this._predecessor = null;
	}
	
	/**
	 * construct an VertexIntervall from l to r with the settings of other
	 * the Predecessor will be shifted to the new value of l
	 * @param l lowbound
	 * @param r highbound
	 * @param other Intervall to copy settings from
	 */
	public VertexIntervall(final int l,final int r,final VertexIntervall other)
	{
		super(l,r);
		//this.lastDepartureAtFromNode = other.lastDepartureAtFromNode;		
		this.reachable = other.reachable;
		this.scanned = other.scanned;
		this._predecessor = other._predecessor.copyShiftedTo(l);
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
	 * @return if reachable from source
	 */
	public boolean getReachable(){
		return this.reachable;
	}
	
	
	/**
	 * Setter for the predecessor in a shortest path
	 * @param pred predesessor vertex
	 */
	public void setPredecessor(final PathStep pred){
		this._predecessor=pred;
	}
	
	/**
	 * Getter for the predecessor in a shortest path
	 * @return predecessor vertex 
	 */
	public PathStep getPredecessor(){
		return this._predecessor;
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
	 * Set the fields of the VertexIntervall reachable true, scanned false.
	 * This performs no checks at all!
	 * @param pred which is set as predecessor, always shifted to low bound!
	 */
	public void setArrivalAttributes (final PathStep pred)
	{
		//we might have already scanned this intervall
		this.scanned = false;
		this.reachable = true;				
		this._predecessor = pred.copyShiftedTo(this.getLowBound());
		//ourIntervall.setLastDepartureAtFromNode(arrive.getLastDepartureAtFromNode());		
	}
	
//----------------------------SPLITTING----------------------------//
	
	/**
	 * splits the referenced VertexIntervall at t contained in (lowbound, higbound)
	 * by changing!! referenced Intervall to (lowbound,t) 
	 * and creating a new EdgeInterval (t,highbound) with same Predecesor as the referenced
	 *@param t time to split at
	 *@return new Interval 
	 */
	public VertexIntervall splitAt(int t){		
		Intervall j = super.splitAt(t);
		VertexIntervall k = new VertexIntervall(j);
		k.reachable = this.reachable;
		k.scanned = this.scanned;
		k._predecessor= this._predecessor.copyShiftedTo(k.getLowBound());
		//k.lastDepartureAtFromNode = this.lastDepartureAtFromNode;
		return k;
	}
	
	/**
	 * gives a string representation of tje VertexIntervall of the form with optional predecessor
	 * [x,y): reachable: true scanned: false pred: 
	 * @return string representation 
	 */
	 
	public String toString()
	{
		return super.toString() + "; reachable: " + this.reachable + "; scanned: " + this.scanned + "; pred: " + this._predecessor;
	}
}
