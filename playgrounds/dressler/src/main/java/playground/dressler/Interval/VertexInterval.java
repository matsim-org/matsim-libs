/* *********************************************************************** *
 * project: org.matsim.*
 * VertexInterval.java
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


package playground.dressler.Interval;

//matsim imports
import org.matsim.api.core.v01.network.Link;

import playground.dressler.ea_flow.PathStep;


/**
 * @author Manuel Schneider
 * class representing a node in an time expanded network
 * between two integer Points of time
 * carries a predecessor node and a minimal distance for shortest path computation
 */
public class VertexInterval extends Interval {

//---------------------------FIELDS----------------------------//	
	
	/**
	 * shows whether the vertex is reacheable during the time interval
	 */
	boolean reachable = false;
	
	boolean scanned = false;


	/**
	 * predecessor or successor in a shortest path
	 * the times stored in the step are valid for arrival at the low bound if predecessor
	 * or for departure at the low bound if successor
	 */
	//PathStep _breadcrumb=null;
	
	PathStep _succ=null;
	PathStep _pred=null;
	

//---------------------------METHODS----------------------------//
//**************************************************************//
	


//--------------------------CONSTUCTORS-------------------------//
	/**
	 * Default costructor creates an [0,1) VertexInterval
	 * not reachable, not scanned, no predecessor 
	 */
	public VertexInterval() {
		super();
	}

	/**
	 * Creates in VertexInterval [l,r)
	 * not reachable, not scanned, no predecessor 
	 * @param l lowbound
	 * @param r highbound
	 */
	public VertexInterval(final int l,final int r) {
		super(l, r);
		// dummy values
		this.reachable = false;
		this.scanned = false;
		//this._breadcrumb = null;
		this._pred = null;
		this._succ = null;
	}
	
	/**
	 * construct an VertexInterval from l to r with the settings of other
	 * the Predecessor will be shifted to the new value of l
	 * @param l lowbound
	 * @param r highbound
	 * @param other Interval to copy settings from
	 */
	public VertexInterval(final int l,final int r,final VertexInterval other)
	{
		super(l,r);	
		this.reachable = other.reachable;
		this.scanned = other.scanned;
		this._pred = other._pred;
		this._succ = other._succ;

		//this._breadcrumb = other._breadcrumb.copyShiftedToArrival(l);
	}

	/**
	 * creates a VertexInterval instance as a copy of an Interval
	 * not reachable, not scanned, no predecessor
	 * @param j Interval to copy
	 */
	public VertexInterval(final Interval j) {
		super(j.getLowBound(),j.getHighBound());
		
	}
	
	public VertexInterval(final VertexInterval j) {
		super(j.getLowBound(),j.getHighBound());		
		this.reachable = j.reachable;
		this.scanned = j.scanned;
		this._pred = j._pred;
		this._succ = j._succ;		
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
		this._pred=pred;
	}
	
	/**
	 * Getter for the predecessor in a shortest path
	 * @return predecessor vertex 
	 */
	public PathStep getPredecessor(){
		return this._pred;
	}
	
	/**
	 * Setter for the successor in a shortest path
	 * @param succ succesor vertex
	 */
	public void setSuccessor(final PathStep succ){
		this._succ=succ;
	}
	
	/**
	 * Getter for the successor in a shortest path
	 * @return successor PathStep 
	 */
	public PathStep getSuccessor(){
		return this._succ;
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
	 * Should the other VertexInterval be replaced with this?
	 * Note that in intricate situations of being better "here and there",
	 * the method has to be called on subintervals after the first returned interval again.    
	 * @param other a VertexInterval
	 * @return null if not, and the first subinterval of other that should be replaced otherwise 
	 */
	public Interval isBetterThan(final VertexInterval other) {
		if (!other.reachable) {
			int l = Math.max(this._l, other._l);
			int r = Math.min(this._r, other._r);
			if (l < r) {
			  return new Interval(l, r);
			}
		} 
		
		return null;		
	}
	
	/**
	 * Can this VertexInterval be combined with other?
	 * Times or interval bounds are not checked!
	 * @param other VertexInterval to compare to
	 * @return true iff the intervalls agree on their arrival properties
	 */
	public boolean continuedBy(final VertexInterval other) {
		if (this.scanned != other.scanned) return false;
		if (this.reachable != other.reachable) return false;
		
		if (this._pred == null) {
			if (other._pred != null) return false;			
		} else {
			if (!this._pred.continuedBy(other._pred)) return false;
		}
		
		if (this._succ == null) {
			if (other._succ != null) return false;
		} else {
		   if (!this._succ.continuedBy(other._succ)) return false;
		}
		
		return true;
	}
	
	/**
	 * Set the fields of the VertexInterval reachable true, scanned false, and the _pred to pred
	 * This performs no checks at all!
	 * Note that this is not suitable for the Reverse search anymore! 
	 * @param pred which is set as predecessor. It is never shifted anymore.
	 */	
	public void setArrivalAttributesForward (final PathStep pred)
	{
		// we might have already scanned this interval
		// but someone insists on relabelling it.
		this.scanned = false;
		this.reachable = true;
		this._pred = pred;
	}
	
	/**
	 * Set the fields of the VertexInterval reachable true, scanned false, and the _pred to pred
	 * This performs no checks at all!
	 * @param succ which is set as successor. It is never shifted anymore.
	 */		
	public void setArrivalAttributesReverse (final PathStep succ)
	{
		// we might have already scanned this interval
		// but someone insists on relabelling it.
		this.scanned = false;
		this.reachable = true;
		this._succ = succ;
	}
	
	/**
	 * Set the fields of the VertexInterval to the one given.
	 * Predecessor or Successor are only updated if they are not null. 
	 * @param other The VertexInterval from which the settings are copied 
	 */
	public void setArrivalAttributes (final VertexInterval other)
	{
		this.scanned = other.scanned;
		this.reachable = other.reachable;
		if (other._pred != null) 
		  this._pred = other._pred;
		
		if (other._succ != null)
		  this._succ = other._succ;
	}
	
//----------------------------SPLITTING----------------------------//
	
	/**
	 * splits the referenced VertexInterval at t contained in (lowbound, higbound)
	 * by changing!! referenced Interval to (lowbound,t) 
	 * and creating a new EdgeInterval (t,highbound) with same Predecesor as the referenced
	 *@param t time to split at
	 *@return new Interval 
	 */
	public VertexInterval splitAt(int t){		
		Interval j = super.splitAt(t);
		VertexInterval k = new VertexInterval(j);
		k.reachable = this.reachable;
		k.scanned = this.scanned;
		k._pred = this._pred;
		k._succ = this._succ;

		/*if (this._breadcrumb == null) {
			k._breadcrumb = null;
		} else {
			// should hardly ever occur, if at all.
		    k._breadcrumb = this._breadcrumb.copyShifted(k.getLowBound() - this.getLowBound());
		}*/
		
		return k;
	}
	
	/**
	 * gives a string representation of the VertexInterval of the form 
	 * [x,y): reachable: true scanned: false  breadcrumb: ...  
	 * @return string representation 
	 */
	 
	public String toString()
	{
		return super.toString() + "; reachable: " + this.reachable + "; scanned: " + this.scanned + "; pred: " + this._pred + " succ: " + this._succ;
	}
}
