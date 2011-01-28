/* *********************************************************************** *
 * project: org.matsim.*												   *	
 * Interval.java														   *
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

import playground.dressler.control.Debug;



/**
 * class representing intervals in the reals with integral boundaries
 * @author Manuel Schneider
 *
 */
public class Interval
implements Comparable<Interval>
{
	
//********************FIELDS*******************************************************//	
	
	/**
	 * lowbound 
	 */
	int _l;
	
	/**
	 * highbound 
	 */
	int _r;

	/*
	 *mode of opennes with the folowing meaning:
	 *1 closed
	 *2 leftopen
	 *3 rightopen
	 *4 open
	 *else case 3
	 */
	// private static int mode = 3;
	// mode can no longer be changed!
	
	

//*******************METHODS*******************************************************//
	
//	-----------------Constructors--------------------------------------------------//
	
	/**
	 * constructing a default interval [0,1] 
	 */
	public Interval(){
		this._l = 0;
		this._r =1;	
	}
	
	/**
	 * copy constructor
	 * @param old Interval to make a copy of
	 */
	public Interval(final Interval old){
		this(old._l,old._r);
	}
	
	/**
	 * Constructs an Interval  given containing all piont between l and r.
	 * Only accepts l <= r.
	 * @param l lowbound of the Interval
	 * @param r highbound of the Interval
	 * 
	 */
	public Interval(final int l,final int r){
		if (Debug.GLOBAL && Debug.INTERVAL_CHECKS) {
			if(!legalBounds(l,r)) throw new IllegalArgumentException("Empty Interval");
		}
		
		this._l = l;
		this._r = r ;
		
	}
	
//--------------------MODE STUFF---------------------------------------------------//
	
	/**
	 * this method checks whether the bounds would be feasible for an interval according to the current mode
	 * @param l represents the potential lowboud
	 * @param r	represents the potential highbound
	 * @return true iff l and r represent an Interval in current mode 
	 */
	public static boolean legalBounds(final int l,final int r){
		return (l<r);
	}
	
	/**
	 * checks whether the bounds of the Interval are valid in the current mode
	 * (x,x) Intervals are considered valid in mode 1
	 * (x,y) Intervals with y>x are considered valid in mode 4
	 * @return true iff the bounds are valid
	 */
	public boolean isValid(){
		return _l < _r;
	}
	
//--------------------CONTAINING SPLITTING LENGTH----------------------------------// 
	
	/**
	 * Method to get the position of the Interval with respect to a point t.
	 * @param t integral point
	 * @return 0 iff t is contained, 1 iff t is lowbound but not contained, 2 iff t<lowbound , -1 iff t is highbound but not contained -2 iff t>highbound
	 */
	public int posWithRespTo(final int t){
		if (this.contains(t)){
			return 0;
		}else{
			if (this._l==t) return 1;
			if (this._l>t)return 2;
			if (this._r==t) return-1;
			if (this._r<t)return -2;
		}
		return -1;
	}
	
	/**
	 * Method to decide whether a point t is contained in the Interval. It uses the current mode of Intervals
	 * @param t integral point
	 * @return true iff t is contained in the Interval 
	 */
	public boolean contains(final int t){
		return(this._l<=t && this._r > t);		
	}

	
	/**
	 * Method to decide whether a given Interval is contained in the referenced Interval 
	 * @param other Interval
	 * @return true iff other is a subset of refereced Interval 
	 */
	public boolean contains(final Interval other){
		return (_l <= other._l && _r >= other._r);
	}
	
	/**
	 * Calculates the length of a given Interval basically doing  highbound-lowbound
	 * @return length of the referenced Interval
	 */
	public int length(){
		return (this._r-this._l);
	}
	
	/**
	 * Splits the referenced Interval up into two Intervals if t is an Interior point or t is a bound in a closed Interval(mode 1).
	 * Does not modify the referenced Interval!!!!
	 * @param t point to split the Interval at
	 * @return an Array of Intervals with the "smaller" one at first
 	 */
	public Interval[] getSplitedAt(final int t){
		if (Debug.GLOBAL && Debug.INTERVAL_CHECKS) { 
			if(!(this.isSplittableAt(t))) {
				throw new IllegalArgumentException(
						"cant split interval at " + t +
						" since it ist not an interior piont in the Interval" );
			}
		}
		Interval[] arr = new Interval[2];
		arr[0]=new Interval(this._l,t);
		arr[1]=new Interval(t,this._r);
		return arr;

	}
	
	/**
	 * Splits the Interval up into two Intervals if t is an Interior point or t is a bound in a closed Interval.
	 * The referenced interval is changed to the "smaller" one.
	 * Does modify the referenced Interval!!!!
	 * @param t point to split the interval at
	 * @return Interval which is the "higher" one
 	 */
	public Interval splitAt(final int t){
		if (Debug.GLOBAL && Debug.INTERVAL_CHECKS) { 
			if(!(this.isSplittableAt(t))) {
				throw new IllegalArgumentException(
						"cant split interval at " + t +
						" since it ist not an interior piont in the Interval" );
			}
		}
		
		Interval i= new Interval(t,this._r);
		this.setHighBound(t);
		return i;		
	}
	
	/**
	 * decides whether a given Interval can be split at point t into two nonempty Intervals
	 * @param t point to split at
	 * @return true iff t is interior point or the Interval is closed and contains t 
	 */
	public boolean isSplittableAt(final int t){
		return ( (this._l < t) && (this._r > t) );
	}
	
	
//--------------------INTERSECTION UNION-------------------------------------------//	
	
	/**
	 * checks whether two Intervals are equal with respect to their bounds
	 * @param i  other Interval
	 * @return true iff both are equal
	 */
	public boolean equals(final Interval i){
		return(this._l==i._l && this._r == i._r);
	}
	
	/**
	 * compares two Intervals by the lowbounds. does not consent with the equals method!!!!
	 * @param o other Interval
	 * @return 0 iff lowbounds are equal <0 iff referenced lowbound is smaller than otherts lowbound >0 else
	 */
	public int compareTo(final Interval o){
		return (this._l-o._l);
	}
	
	/**
	 * Decides whether two Intervals intersect. This method does not modify 
	 * the given Intervals.
	 * @param other 
 	 * @return true iff refferenced and other interval share a point
	 */
	public boolean intersects(final Interval other){
		if (_r <= other._l) return false;
		if (_l >= other._r) return false;

		return true;
	}

	
	/**
	 * Calculates the intersection of two Intervals. This method does not modify 
	 * the given Intervals. 
	 * @param other 
	 * @return returns the Interval of all points shared by the referenced and and other Interval 
	 * and null if the do not intersect
	 */
	public Interval Intersection(final Interval other){
		int l = Math.max(this._l, other._l);
		int r = Math.min(this._r, other._r);

		if (l < r)
			return new Interval(l,r);

		return null;
	}

//-------------------------GETTER AND SETTER---------------------------------------//	
	
	/** 
	 * getter for leftbound
	 * @return the _l
	 */
	public int getLowBound() {
		return _l;
	}

	/** 
	 * setter for leftbound bounds have to be legal
	 * @param l the _l to set
	 */
	public void setLowBound(final int l) {
		if (Debug.GLOBAL && Debug.INTERVAL_CHECKS) {
			if(!legalBounds(l,this._r)){
				throw new IllegalArgumentException("illegal lowbound");
			}
		}
		this._l = l;
	}

	/**
	 * getter for reightbound 
	 * @return the _r
	 */
	public int getHighBound() {
		return _r;
	}

	/**
	 * setter for rightbound bounds have to be legal
	 * @param r the _r to set
	 */
	public void setHighBound(final int r) {
		if (Debug.GLOBAL && Debug.INTERVAL_CHECKS) {
			if(!legalBounds(this._l,r)){
				throw new IllegalArgumentException("illegal highbound");
			}
		}
		this._r = r;
	}

//--------------------STRING METHODS-----------------------------------------------//	
	
	/**
	 * Gives a String representation of an Interval with correct brackets
	 * @return String representation
	 */
	@Override
	public String toString(){		
		return "[" +this._l+","+this._r+")";	
	}
	
//----------------------- SHIFTING ------------------------------------------------//	

	/**
	 * Method to shift a Interval within the positive numbers. The Interval might get cut off
	 * @param tau value to be shifted by
	 * @return Interval in the positive integers null if the shift does not intersect the positive integers
	 */
	public Interval shiftPositive(final int tau){
		int l,r;
		if (Integer.MAX_VALUE - tau >= this._r ){
			r = this._r + tau;
		} else {
			r = Integer.MAX_VALUE;
		}
		l = Math.max(0, this._l + tau);
		
		if (legalBounds(l,r)) {
			return new Interval(l,r);
		} else { 
			return null;
		}
	}
}
