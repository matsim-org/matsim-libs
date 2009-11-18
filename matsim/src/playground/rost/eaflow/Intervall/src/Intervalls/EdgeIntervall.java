/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeIntervall.java
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
//import java.util.Collection;




/**
 * @author Manuel Schneider
 * class representing an edge in a given timeIntervall in an timeexpanded network
 * carries information on the constant integer flow during that timeframe 
 */
public class EdgeIntervall extends Intervall 

{

//-----------------------------FIELDS--------------------------//
	/**
	 * field for flow information
	 * flow is considered to be constant during the timespan of the Intervall
	 * default flowvalue is 0
	 */
	private int _flow =0;

//-----------------------------METHODS-------------------------//	
//*************************************************************//
	
//----------------------------CONSTRUCTORS---------------------//
	
	/**
	 * Default constructor creating an (0,1) intervall with flowvalue 0
	 */
	public EdgeIntervall() {
		super();
	}

	/**
	 * creating an EdgeIntervall (l,r)
	 * with flowvalue 0
	 * @param l lowbound
	 * @param r highbound
	 */
	public EdgeIntervall(int l, int r) {
		super(l, r);
	}
	
	/**
	 * Construct An EdgeIntervall out of an Intervall
	 * flowvalue is set to 0
	 * @param i Intervall to be copied to an EdgeIntervall
	 * 
	 */
	public EdgeIntervall(Intervall i){
		super(i.getLowBound(),i.getHighBound());
	}
	
	/**
	 * Construct An EdgeIntervall out of an Intervall
	 * flowvalue is set to f
	 * @param i Intervall to be copied to an EdgeIntervall
	 * @param flowvalue
	 * 
	 */
	public EdgeIntervall(Intervall i,int f){
		this(i);
		this.setFlow(f);
	}

	/**
	 * creating an EdgeIntervall (l,r)
	 * with flowvalue f
	 * @param l lowbound 
	 * @param r highbound
	 * @param f flowvalue
	 */
	public EdgeIntervall(int l, int r, int f) {
		super(l, r);
		this.setFlow(f);
	}
	
//-------------------------GETTER AND SETTER-----------------------//
	
	/**
	 * Setter for flowvalue on an edge during tthe toimeframe of the Intervall
	 * @param f flowvalue
	 */
	public void setFlow(int f){
		if(f>=0){
			this._flow=f;
		}
		else{
			throw new IllegalArgumentException("flow is negative");
		}
	}
	
	/**
	 * Getter for the flowvalue on an edge during tthe toimeframe of the Intervall
	 * @return amount of flow on edge 
	 */
	public int getFlow(){
		return this._flow;
	}
	
	/**
	 * changes the flow by f and throws an Exception if the new flow would succeed u or get negative
	 * @param f amount of flow to augment
	 * @param u  nonegetive capacty
	 */
	public void changeFlow(int f, int u){
		this.setFlow(f+this._flow, u);
	}
	
	/**
	 * sets the flow to f and throws an Exception if the new flow would succeed u or is negative
	 * @param f new flow value
	 * @param u  nonegetive capacty
	 */
	public void setFlow(int f, int u){
		if(u<0){
			throw new IllegalArgumentException("negative capacity");
		}
		if(f<=u){
			this.setFlow(f);
		}else
			throw new IllegalArgumentException("capacity violated");
	}

	
//-----------------------------SPLITTING---------------------------//
	
	/**
	 * splits the referenced EdgeIntervall at t contained in (lowbound, higbound)
	 * by changeing!! referenced Intervall to (lowbound,t) 
	 * and creating a new EdgeInterval (t,highbound) with same Flowvalue as the referenced
	 *@param t point to split at
	 *@return new Interval 
	 */
	@Override
	public EdgeIntervall splitAt(int t){
		Intervall j =super.splitAt(t);
		EdgeIntervall k = new EdgeIntervall(j);
		k._flow=this._flow;
		return k;
	}
	/*
	public static EdgeIntervall maxRight( Collection<EdgeIntervall> C){
		if(C==null)throw new NullPointerException("Collection was null");
		if(!C.isEmpty()){
			int max = Integer.MIN_VALUE;
			EdgeIntervall maxintervall = null;
			for(EdgeIntervall i : C){
				if (max<=i.getHighBound()){
					max= i.getHighBound();
					maxintervall=i;
				}
			}
		 return maxintervall;	
		}
		throw new IllegalArgumentException("Empty Collection");
	}
	*/
//-----------------------------comparators-------------------------//	
	
	/**
	 * compares the  lowbounds
	 * Is not compatible to equals !!!!!
	 * @param o
	 * @return this.low-o.low
	 */
	public int compareTo(EdgeIntervall o) {
		return (this.getLowBound()-o.getLowBound());
	
	}
	
	/**
	 * checks for equality of referenced and other
	 * @param other Intervall
	 * @return true iff Intervall and Flow are equal
	 */
	public boolean equals(EdgeIntervall  other){
		boolean ret = false;
		ret= super.equals(other);
		if(ret){
			
			ret= (this.getFlow()==other.getFlow());
		}
		return ret;
	}
	
	/**
	 * checks for equality of the intervalls, flow may differ
	 * equivalten to Intervall.equlals(Intervall)
	 * @param other
	 * @return
	 */
	public boolean sameIntervall(EdgeIntervall other){
		return super.equals(other);
	}
	
	@Override
	public EdgeIntervall shiftPositive(int tau){
		int l,r;
		Intervall tmp = super.shiftPositive(tau);
		if(tmp == null)
			return null;
		return new EdgeIntervall(tmp, this.getFlow());
	}
	
	@Override
	public String toString()
	{
		return "[" + this.getLowBound() + "," + this.getHighBound() + "): f: " + this.getFlow(); 
	}
	
//	-----------------------------MAIN METHOD-------------------------//	
	
}
