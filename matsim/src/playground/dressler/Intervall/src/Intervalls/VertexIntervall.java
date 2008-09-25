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
import org.matsim.network.Link;
import java.util.Collection;


/**
 * @author Manuel Schneider
 * class representing a node in an time expanded network
 * between two integer Points of time
 * carries a prdecessor node and a min distance for shortest path computation
 */
public class VertexIntervall extends Intervall {

//---------------------------FIELDS----------------------------//	
	
	/**
	 * shows weateher the vertex is reacheable during the time intervall
	 */
	private boolean _dist = false;

	/**
	 * predecessor in a shortest path
	 */
	//TODO predecessor
	private Link _predecessor=null;
	

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
		this.setDist(d);
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
		this.setDist(d);
		this.setPredecessor(pred);
		
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
	public void setDist(boolean d){
		this._dist=d;
	}
	
	/**
	 * Getter for the min distance to the sink at time lowbound
	 * @return min distance to sink
	 */
	public boolean getDist(){
		return this._dist;
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
	public VertexIntervall splitAt(int t){
		boolean newdist = this.getDist();
		Intervall j =super.splitAt(t);
		VertexIntervall k = new VertexIntervall(j);
		k._dist =newdist;
		k._predecessor= this._predecessor;
		return k;
	}

	
//----------------------------MAIN METHOD--------------------------//
	
	/**
	 * main method for testing
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
