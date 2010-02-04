/* *********************************************************************** *
 * project: org.matsim.*												   *
 * EdgeIntervall.java													   *	
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


/**
 * @author Manuel Schneider
 * class representing an edge in a given timeIntervall in an time expanded network
 * carries information on the constant integer flow during that time frame 
 */
public class EdgeIntervall extends Intervall 

{

//**********************************FIELDS*****************************************//
	/**
	 * field for flow information
	 * flow is considered to be constant during the time span of the Intervall
	 * default flow value is 0
	 */
	private int _flow =0;

//********************************METHODS******************************************//	

	
//----------------------------CONSTRUCTORS-----------------------------------------//
	
	/**
	 * Default constructor creating an (0,1) EdgeIntervall with flow value 0
	 */
	public EdgeIntervall() {
		super();
	}
	/**
	 * copy constructor
	 * @param old EdgeIntervall to make a copy of
	 */
	public EdgeIntervall(final EdgeIntervall old){
		super(old);
		this._flow =old._flow;
	}

	/**
	 * creating an EdgeIntervall (l,r)
	 * with flow value 0
	 * @param l lowerbound
	 * @param r highbound
	 */
	public EdgeIntervall(final int l,final int r) {
		super(l, r);
	}
	
	/**
	 * Construct an EdgeIntervall out of an Intervall
	 * flow value is set to 0
	 * @param i Intervall to be copied to an EdgeIntervall
	 * 
	 */
	public EdgeIntervall(final Intervall i){
		super(i.getLowBound(),i.getHighBound());
	}
	
	/**
	 * Construct An EdgeIntervall out of an Intervall
	 * flow value is set to f
	 * @param i Intervall to be copied to an EdgeIntervall
	 * @param f positive flow value
	 * 
	 */
	public EdgeIntervall(final Intervall i,final int f){
		this(i);
		this.setFlow(f);
	}

	/**
	 * creating an EdgeIntervall (l,r)
	 * with flow value f
	 * @param l lowbound 
	 * @param r highbound
	 * @param f positive flow value
	 */
	public EdgeIntervall(final int l,final int r,final int f) {
		super(l, r);
		this.setFlow(f);
	}
	
//----------------------------------GETTER AND SETTER------------------------------//
	
	/**
	 * Setter for flow value on an edge during the time frame of the Intervall
	 * @param f positive flow value
	 */
	public void setFlow(final int f){
		if(f>=0){
			this._flow=f;
		}
		else{
			throw new IllegalArgumentException("flow is negative");
		}
	}
	
	/**
	 * Getter for the flow value on an edge during the time frame of the Intervall
	 * @return amount of flow on edge 
	 */
	public int getFlow(){
		return this._flow;
	}
	
	/**
	 * sets the flow to f and throws an Exception if the new flow would succeed 
	 * u or is negative
	 * @param f new flow value
	 * @param u  nonnegative capacity
	 */
	public void setFlow(final int f,final int u){
		if(u<0){
			throw new IllegalArgumentException("negative capacity");
		}
		if(f<=u){
			this.setFlow(f);
		}else
			throw new IllegalArgumentException("capacity violated");
	}
	
	/**
	 * changes the flow by f and throws an Exception if the new flow would succeed 
	 * u or get negative
	 * @param f amount of flow to augment
	 * @param u  nonnegative capacity
	 */
	public void changeFlow(final int f,final int u){
		this.setFlow(f+this._flow, u);
	}
	
//-------------------------------------SPLITTING-----------------------------------//
	
	/**
	 * splits the referenced EdgeIntervall at t contained in (lowbound, higbound)
	 * by changing !! the referenced Intervall to (lowbound,t) 
	 * and creating a new EdgeInterval (t,highbound) with same
	 * flow value as the referenced
	 *@param t point to split at
	 *@return new Interval 
	 */
	@Override
	public EdgeIntervall splitAt(final int t){
		Intervall j =super.splitAt(t);
		EdgeIntervall k = new EdgeIntervall(j);
		k._flow=this._flow;
		return k;
	}
	
//-----------------------------------COMPARING-------------------------------------//	
	
	/**
	 * compares the  lowbounds
	 * Is not compatible to equals !!!!!
	 * @param other Intervall to compare
	 * @return this.lowbound-other.lowbound
	 */
	public int compareTo(final EdgeIntervall other) {
		return (this.getLowBound()-other.getLowBound());
	
	}
	
	/**
	 * checks for equality of referenced and other
	 * @param other Intervall
	 * @return true iff Intervall and flow value are equal
	 */
	public boolean equals(final EdgeIntervall  other){
		boolean ret = false;
		ret= super.equals(other);
		if(ret){
			ret= (this._flow == other._flow);
		}
		return ret;
	}
	
	/**
	 * checks for equality of the underlying Intervalls, flow value may differ
	 * equivalent to Intervall.equlals(Intervall)
	 * @param other EdgeIntervall
	 * @return
	 */
	public boolean onSameIntervall(final EdgeIntervall other){
		return super.equals(other);
	}

//----------------------- SHIFTING ------------------------------------------------//
	
	/**
	 * Method to shift a EdgeIntervall within the positive numbers
	 *  and same flow value.
	 * The EdgeIntervall might get cut off.
	 * @param tau value to be shifted by
	 * @return EdgeIntervall in the positive integers null if the shift
	 *  does not intersect the positive integers
	 */
	@Override
	public EdgeIntervall shiftPositive(final int tau){
		Intervall tmp = super.shiftPositive(tau);
		if(tmp == null)
			return null;
		return new EdgeIntervall(tmp, this.getFlow());
	}

//--------------------STRING METHODS-----------------------------------------------//
	
	/**
	 * Gives a String representation of an EdgeIntervall.
	 * Example: "[1,2) f: 2"
	 * @return String representation
	 */
	@Override
	public String toString()
	{
		return "[" + this.getLowBound() + "," + 
			this.getHighBound() + ") f: " + this.getFlow(); 
	}

}
