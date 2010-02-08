/* *********************************************************************** *
 * project: org.matsim.*												   *
 * EdgeInterval.java													   *	
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


/**
 * @author Manuel Schneider
 * class representing an edge in a given timeInterval in an time expanded network
 * carries information on the constant integer flow during that time frame 
 */
public class EdgeInterval extends Interval 

{

//**********************************FIELDS*****************************************//
	/**
	 * field for flow information
	 * flow is considered to be constant during the time span of the Interval
	 * default flow value is 0
	 */
	private int _flow =0;

//********************************METHODS******************************************//	

	
//----------------------------CONSTRUCTORS-----------------------------------------//
	
	/**
	 * Default constructor creating an (0,1) EdgeInterval with flow value 0
	 */
	public EdgeInterval() {
		super();
	}
	/**
	 * copy constructor
	 * @param old EdgeInterval to make a copy of
	 */
	public EdgeInterval(final EdgeInterval old){
		super(old);
		this._flow =old._flow;
	}

	/**
	 * creating an EdgeInterval (l,r)
	 * with flow value 0
	 * @param l lowerbound
	 * @param r highbound
	 */
	public EdgeInterval(final int l,final int r) {
		super(l, r);
	}
	
	/**
	 * Construct an EdgeInterval out of an Interval
	 * flow value is set to 0
	 * @param i Interval to be copied to an EdgeInterval
	 * 
	 */
	public EdgeInterval(final Interval i){
		super(i.getLowBound(),i.getHighBound());
	}
	
	/**
	 * Construct An EdgeInterval out of an Interval
	 * flow value is set to f
	 * @param i Interval to be copied to an EdgeInterval
	 * @param f positive flow value
	 * 
	 */
	public EdgeInterval(final Interval i,final int f){
		this(i);
		this.setFlow(f);
	}

	/**
	 * creating an EdgeInterval (l,r)
	 * with flow value f
	 * @param l lowbound 
	 * @param r highbound
	 * @param f positive flow value
	 */
	public EdgeInterval(final int l,final int r,final int f) {
		super(l, r);
		this.setFlow(f);
	}
	
//----------------------------------GETTER AND SETTER------------------------------//
	
	/**
	 * Setter for flow value on an edge during the time frame of the Interval
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
	 * Getter for the flow value on an edge during the time frame of the Interval
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
		if (f < 0) {
			throw new IllegalArgumentException("flow is negative");
		}
		if(f > u){
			throw new IllegalArgumentException("capacity violated");
		} else {
			this._flow = f;
		}			
	}
	
	/**
	 * augments the flow by gamma and throws an Exception if the new flow would succeed 
	 * u or get negative
	 * @param gamma amount of flow to augment
	 * @param u  nonnegative capacity
	 */
	public void augment(final int gamma,final int u){
		this.setFlow(gamma+this._flow, u);
	}
	
	/**
	 * augments the flow by gamma, no checking is done! 
	 * @param gamma the amount of flow to augment
	 */
	public void augmentUnsafe(final int gamma){
		this._flow += gamma;		
	}
	
	/**
	 * checks the flow 
	 * @param u the capacity
	 * @return true iff the 0 <= flow <= u 
	 */
	public boolean checkFlow(final int u){
		return (0 <= this._flow && this._flow <= u);
	}
	
//-------------------------------------SPLITTING-----------------------------------//
	
	/**
	 * splits the referenced EdgeInterval at t contained in (lowbound, higbound)
	 * by changing !! the referenced Interval to (lowbound,t) 
	 * and creating a new EdgeInterval (t,highbound) with same
	 * flow value as the referenced
	 *@param t point to split at
	 *@return new Interval 
	 */
	@Override
	public EdgeInterval splitAt(final int t){
		Interval j =super.splitAt(t);
		EdgeInterval k = new EdgeInterval(j);
		k._flow=this._flow;
		return k;
	}
	
//-----------------------------------COMPARING-------------------------------------//	
	
	/**
	 * compares the  lowbounds
	 * Is not compatible to equals !!!!!
	 * @param other Interval to compare
	 * @return this.lowbound-other.lowbound
	 */
	public int compareTo(final EdgeInterval other) {
		return (this.getLowBound()-other.getLowBound());
	
	}
	
	/**
	 * checks for equality of referenced and other
	 * @param other Interval
	 * @return true iff Interval and flow value are equal
	 */
	public boolean equals(final EdgeInterval  other){
		boolean ret = false;
		ret= super.equals(other);
		if(ret){
			ret= (this._flow == other._flow);
		}
		return ret;
	}
	
	/**
	 * checks for equality of the underlying Intervals, flow value may differ
	 * equivalent to Interval.equlals(Interval)
	 * @param other EdgeInterval
	 * @return
	 */
	public boolean onSameInterval(final EdgeInterval other){
		return super.equals(other);
	}

//----------------------- SHIFTING ------------------------------------------------//
	
	/**
	 * Method to shift a EdgeInterval within the positive numbers
	 *  and same flow value.
	 * The EdgeInterval might get cut off.
	 * @param tau value to be shifted by
	 * @return EdgeInterval in the positive integers null if the shift
	 *  does not intersect the positive integers
	 */
	@Override
	public EdgeInterval shiftPositive(final int tau){
		Interval tmp = super.shiftPositive(tau);
		if(tmp == null)
			return null;
		return new EdgeInterval(tmp, this.getFlow());
	}

//--------------------STRING METHODS-----------------------------------------------//
	
	/**
	 * Gives a String representation of an EdgeInterval.
	 * Example: "[1,2) f: 2"
	 * @return String representation
	 */
	@Override
	public String toString()
	{
			return super.toString()+" f: " + this.getFlow(); 
	}

}
