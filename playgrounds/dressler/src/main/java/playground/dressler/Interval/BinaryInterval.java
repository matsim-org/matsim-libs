/* *********************************************************************** *
 * project: org.matsim.*												   *
 * BinaryInterval.java													   *	
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
 * @author Daniel Dressler
 * Class representing a binary value on an interval.
 */
public class BinaryInterval extends Interval
{

//**********************************FIELDS*****************************************//
	/**
	 * field for flow information
	 */
	boolean val = false;

//********************************METHODS******************************************//	

	
//----------------------------CONSTRUCTORS-----------------------------------------//
	
	/**
	 * Default constructor creating an (0,1) BinaryInterval with default value false
	 */
	public BinaryInterval() {
		super();
	}
	/**
	 * copy constructor
	 * @param old BinaryInterval to make a copy of
	 */
	public BinaryInterval(final BinaryInterval old){
		super(old);
		this.val = old.val;
	}

	/**
	 * creating a BinaryInterval (l,r)
	 * with value false
	 * @param l lowerbound
	 * @param r highbound
	 */
	public BinaryInterval(final int l,final int r) {
		super(l, r);
	}
	
	/**
	 * Construct a BinaryInterval out of an Interval
	 * value is set to false
	 * @param i Interval to be copied to an BinaryInterval
	 * 
	 */
	public BinaryInterval(final Interval i){
		super(i.getLowBound(),i.getHighBound());
	}
	
	/**
	 * Construct a BinaryInterval out of an Interval
	 * value is set to f
	 * @param i Interval to be copied to a BinaryInterval
	 * @param f binary value
	 * 
	 */
	public BinaryInterval(final Interval i, final boolean f){
		this(i);
		this.val = f;
	}

	/**
	 * Create a BinaryInterval (l,r) with value f 
	 * @param l lowbound 
	 * @param r highbound
	 * @param f boolean value
	 */
	public BinaryInterval(final int l,final int r,final boolean f) {
		super(l, r);
		this.val = f;
	}
	
//-------------------------------------SPLITTING-----------------------------------//
	
	/**
	 * Splits the referenced BinaryInterval at t contained in (lowbound, higbound)
	 * by changing !! the referenced Interval to (lowbound,t) 
	 * and creating a new BinaryInterval (t,highbound) with same
	 * value as the referenced
	 *@param t point to split at
	 *@return new Interval 
	 */
	@Override
	public BinaryInterval splitAt(final int t){
		Interval j = super.splitAt(t);
		BinaryInterval k = new BinaryInterval(j);
		k.val = this.val;
		return k;
	}
	
//-----------------------------------COMPARING-------------------------------------//	
	
	/**
	 * checks for equality of referenced and other
	 * @param other Interval
	 * @return true iff Interval and flow value are equal
	 */
	public boolean equals(final BinaryInterval  other){
		boolean ret = false;
		ret = super.equals(other);
		if (!ret) return false;
		
		return (this.val == other.val);
	}
	
//----------------------- SHIFTING ------------------------------------------------//
	
	/**
	 * Method to shift a BinaryInterval within the positive numbers
	 *  and same value.
	 * The BinaryInterval might get cut off.
	 * @param tau value to be shifted by
	 * @return BinaryInterval in the positive integers null if the shift
	 *  does not intersect the positive integers
	 */
	@Override
	public BinaryInterval shiftPositive(final int tau){
		Interval tmp = super.shiftPositive(tau);
		if(tmp == null)
			return null;
		return new BinaryInterval(tmp, this.val);
	}

//--------------------STRING METHODS-----------------------------------------------//
	
	/**
	 * Gives a String representation of an BinaryInterval.
	 * Example: "[1,2) val: 2"
	 * @return String representation
	 */
	@Override
	public String toString()
	{
			return super.toString()+" val: " + this.val;
	}

}
