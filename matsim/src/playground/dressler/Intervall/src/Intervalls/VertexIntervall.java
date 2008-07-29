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

import java.util.Collection;

/**
 * 
 */

/**
 * @author manuelschneider
 * class representing a node in an time expanded network
 * between two integer Points of time
 * carries a prdecessor node and a min distance for shortest path computation
 */
public class VertexIntervall extends Intervall {

//---------------------------FIELDS----------------------------//	
	
	/**
	 * minimal distance to the sink at time lowbound
	 */
	private int _dist = Integer.MAX_VALUE;

	/**
	 * predecessor in a shortest path
	 */
	//TODO predecessor
	private String _predecessor=null;

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
	public VertexIntervall(int l, int r, int d) {
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
	public VertexIntervall(int l, int r, int d, String pred) {
		super(l, r);
		this.setDist(d);
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
	public void setDist(int d){
		this._dist=d;
	}
	
	/**
	 * Getter for the min distance to the sink at time lowbound
	 * @return min distance to sink
	 */
	public int getDist(){
		return this._dist;
	}
	
	/**
	 * Calculates the actual min distance to the sink at time t assuming that 
	 * it incrases proportional with time
	 * @param t time
	 * @return minimal distance to the sink at time t
	 */
	public int getDistAt(int t){
		if(this.contains(t)){
			int l= this.getLowBound();
			return (this._dist+ (t-l));
		}
		else throw new IllegalArgumentException("Inntervall does not contain the time " +t);
	}
	
//----------------------------PREDECESSOR------------------------//
	
	/**
	 * Setter for the predecessor in a shortest path
	 * @param pred predesessor vertex
	 */
	public void setPredecessor(String pred){
		this._predecessor=pred;
	}
	
	/**
	 * Getter for the predecessor in a shortest path
	 * @return predecessor vertex 
	 */
	public String getPredecessor(){
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
		int newdist = this.getDistAt(t);
		Intervall j =super.splitAt(t);
		VertexIntervall k = new VertexIntervall(j);
		k._dist =newdist;
		k._predecessor= this._predecessor;
		return k;
	}

	public static VertexIntervall maxRight( Collection<VertexIntervall> C){
		if(C==null)throw new NullPointerException("Collection was null");
		if(!C.isEmpty()){
			int max = Integer.MIN_VALUE;
			VertexIntervall maxintervall = null;
			for(VertexIntervall i : C){
				if (max<=i.getHighBound()){
					max= i.getHighBound();
					maxintervall=i;
				}
			}
		 return maxintervall;	
		}
		throw new IllegalArgumentException("Empty Collection");
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
