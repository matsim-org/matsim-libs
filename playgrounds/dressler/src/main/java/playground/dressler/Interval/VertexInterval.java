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
	private boolean reachable = false;
	
	private boolean scanned = false;


	/**
	 * predecessor in a shortest path
	 * the times stored in the step are valid for arrival at the low bound!
	 */
	private PathStep _breadcrumb=null;
	

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
		this._breadcrumb = null;
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
		this._breadcrumb = other._breadcrumb.copyShiftedToArrival(l);
	}

	/**
	 * creates a VertexInterval instance as a copy of an Interval
	 * not reachable, not scanned, no predecessor
	 * @param j Interval to copy
	 */
	public VertexInterval(final Interval j) {
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
		this._breadcrumb=pred;
	}
	
	/**
	 * Getter for the predecessor in a shortest path
	 * @return predecessor vertex 
	 */
	public PathStep getPredecessor(){
		return this._breadcrumb;
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
	 * Set the fields of the VertexInterval reachable true, scanned false, and the _breadrumb to breadcrumb
	 * This performs no checks at all!
	 * @param bread which is set as breadcrumb. It is never shifted anymore.
	 */
	public void setArrivalAttributes (final PathStep bread)
	{
		// we might have already scanned this interval
		// but someone insists on relabelling it.
		this.scanned = false;
		this.reachable = true;
		this._breadcrumb = bread;
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
		if (this._breadcrumb == null) {
			k._breadcrumb = null;
		} else {
			// should hardly ever occur, if at all.
		    k._breadcrumb = this._breadcrumb.copyShifted(k.getLowBound() - this.getLowBound());
		}
		
		return k;
	}
	
	/**
	 * gives a string representation of the VertexInterval of the form 
	 * [x,y): reachable: true scanned: false  breadcrumb: ...  
	 * @return string representation 
	 */
	 
	public String toString()
	{
		return super.toString() + "; reachable: " + this.reachable + "; scanned: " + this.scanned + "; breadcrumb: " + this._breadcrumb;
	}
}
