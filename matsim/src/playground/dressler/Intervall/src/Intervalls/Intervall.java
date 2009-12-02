/* *********************************************************************** *
 * project: org.matsim.*												   *	
 * Intervall.java														   *
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
 * class representing intervals in the reals with integral boundaries
 * @author Manuel Schneider
 *
 */
public class Intervall
implements Comparable<Intervall>
{
	
//********************FIELDS*******************************************************//	
	
	/**
	 * lowbound 
	 */
	private int _l;
	
	/**
	 * highbound 
	 */
	private int _r;

	/**
	 *mode of oppenes with the folowing meaning:
	 *1 closed
	 *2 leftopen
	 *3 rightopen
	 *4 open
	 *else case 3
	 */
	private static int mode = 3;
	
	

//*******************METHODS*******************************************************//
	
//	-----------------Constructors--------------------------------------------------//
	
	/**
	 * constructing a default interval [0,1] 
	 */
	public Intervall(){
		this._l = 0;
		this._r =1;	
	}
	
	/**
	 * copy constructor
	 * @param old Intervall to make a copy of
	 */
	public Intervall(final Intervall old){
		this(old._l,old._r);
	}
	
	/**
	 * Constructs an Intervall  given containing all piont between l and r.
	 * Only accepts l <= r.
	 * @param l lowbound of the Intervall
	 * @param r highbound of the Interval
	 * 
	 */
	public Intervall(final int l,final int r){
		if(legalBounds(l,r)){
			this._l = l;
			this._r = r ;
		}else{
			throw new IllegalArgumentException("Empty Interval");
		}
	}
	
//--------------------MODE STUFF---------------------------------------------------//
	
	/**
	 * this method checks whether the bounds would be feasible for an intervall according to the current mode
	 * @param l represents the potential lowboud
	 * @param r	represents the potential highbound
	 * @return true iff l and r represent an Interval in current mode 
	 */
	public static boolean legalBounds(final int l,final int r){
		switch (mode){
		case 1: 
			if(l<=r)return true;
			break;
		case 2:
			if(l<r)return true;
			break;
		case 3:
			if(l<r)return true;
			break;
		case 4:
			if(l<r)return true;
			break;
		default: 
			if(l<r)return true;
			break;
		}
		return false;
	}
	
	/**
	 * checks whether the bounds of the Intervall are valid in the current mode
	 * (x,x) Intervalls are considered valid in mode 1
	 * (x,y) Intervalls with y>x are considered valid in mode 4
	 * @return true iff the bounds are valid
	 */
	public boolean isValid(){
		return legalBounds(this._l,this._r);
	}
	
	/**
	 * sets the mode in which intervalls are interpreted with meaning:
	 *1 closed
	 *2 leftopen
	 *3 rightopen
	 *4 open
	 *else case 3
	 *default Value is 3 anyway
	 * @param m mode for all intervalls should be  in {1,2,3,4} is set to 3 otherwise
	 */
	public static void setMode(final int m){
		if(1<=m && m<=4){
			mode=m;
		}
		else {
			mode=3;
		}
	}
	
	/**
	 * returns the current mode 
	 *1 closed
	 *2 leftopen
	 *3 rightopen
	 *4 open
	 * @return mode
	 */
	public static int getMode(){
		return mode;
	}
	
//--------------------CONTAINING SPLITTING LENGTH----------------------------------// 
	
	/**
	 * Method to get the position of the Intervall with respect to a point t.
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
	 * Method to decide whether a point t is contained in the Interval. It uses the current mode of Intervalls
	 * @param t integral point
	 * @return true iff t is contained in the Intervall 
	 */
	public boolean contains(final int t){
		switch (mode){
			case 1: 
				return(this._l<=t && this._r >= t);
			case 2:
				return(this._l<t && this._r >= t);
			case 3:
				return(this._l<=t && this._r > t);
			case 4: 
				return(this._l<t && this._r > t);
		}
		return ( (this._l<=t)&&(this._r>=t) );
	}
	
	/**
	 * checks whether a given point t is in  the interior of an Intervall
	 * @param t integral point to check
	 * @return true iff lowbound<t<highbound
	 */
	public boolean hasInteriorPoint(final int t){
		return ( (this._l<t)&&(this._r>t) );
	}
	
	/**
	 * Method to decide whether a given Intervall is contained in the referenced Intervall 
	 * @param other Intervall
	 * @return true iff other is a subset of refereced Intervall 
	 */
	public boolean contains(final Intervall other){
		int m = getMode();
		setMode(1);
		boolean ret;
		ret = ( this.contains(other._r) && this.contains(other._l) );
		setMode(m);
		return ret;
	}
	
	/**
	 * Calculates the length of a given Intervall basically doing  highbound-lowbound
	 * @return length of the referenced Intervall
	 */
	public int length(){
		return (this._r-this._l);
	}
	
	/**
	 * Splits the referenced Intervall up into two Intervalls if t is an Interior point or t is a bound in a closed Intervall(mode 1).
	 * Does not modify the referenced Intervall!!!!
	 * @param t point to split the Intervall at
	 * @return an Array of Intervalls with the "smaller" one at first
 	 */
	public Intervall[] getSplitedAt(final int t){
		if(this.isSplittableAt(t)){
			Intervall[] arr = new Intervall[2];
			arr[0]=new Intervall(this._l,t);
			arr[1]=new Intervall(t,this._r);
			return arr;
		}
		throw new IllegalArgumentException("cant split interval at " + t +" since it ist not an interior piont in the Intervall" );
	}
	
	/**
	 * Splits the Intervall up into two Intervalls if t is an Interior point or t is a bound in a closed Intervall.
	 * The referenced intervall is changed to the "smaller" one.
	 * Does modify the referenced Intervall!!!!
	 * @param t point to split the intervall at
	 * @return Intervall which is the "higher" one
 	 */
	public Intervall splitAt(final int t){
		if(this.isSplittableAt(t)){
			Intervall i=new Intervall(t,this._r);
			this.setHighBound(t);
			return i;
		}
		throw new IllegalArgumentException("cant split interval at " + t +" since it ist not an interior piont in the Intervall" );
	}
	
	/**
	 * decides whether a given Intervall can be split at point t into two nonempty Intervalls
	 * @param t point to split at
	 * @return true iff t is interior point or the Intervall is closed and contains t 
	 */
	public boolean isSplittableAt(final int t){
		return(this.hasInteriorPoint(t) || (mode == 1 && this.contains(t)));
	}
	
	
//--------------------INTERSECTION UNION-------------------------------------------//	
	
	/**
	 * checks weather two Intervall are equal with respect to their bounds
	 * @param i  other Intervall
	 * @return true iff both are equal
	 */
	public boolean equals(final Intervall i){
		return(this._l==i._l && this._r == i._r);
	}
	
	/**
	 * compares two Intervalls by the lowbounds. does not consent with the equals method!!!!
	 * @param o other Interval
	 * @return 0 iff lowbounds are equal <0 iff referenced lowbound is smaller than otherts lowbound >0 else
	 */
	public int compareTo(final Intervall o){
		return (this._l-o._l);
	}
	
	/**
	 * Decides whether two Intervalls intersect. This method does not modify 
	 * the given Intervalls.
	 * @param other 
 	 * @return true iff refferenced and other intervall share a point
	 */
	public boolean intersects(final Intervall other){
		if(mode!=4){
			if(this.contains(other._r) && other.contains(other._r) ){
				return true;
			}
			if(this.contains(other._l) && other.contains(other._l)){
				return true;
			}
			if(other.contains(this._l) && this.contains(this._l)){
				return true;
			}
			if(other.contains(this._r) && this.contains(this._r)){
				return true;
			}
			return false;
		}
		else{
			if(this.contains(other._r)  ){
				return true;
			}
			if(this.contains(other._l) ){
				return true;
			}
			if(other.contains(this._l) ){
				return true;
			}
			if(other.contains(this._r) ){
				return true;
			}
			if(other.equals(this)){
				return true;
			}
			return false;
		}
	}

	
	/**
	 * Calculates the intersection of two Intervalls. This method does not modify 
	 * the given Intervalls. 
	 * @param other 
	 * @return returns the Intervall of all points shared by the referenced and and other Intervall 
	 * and null if the do not intersect
	 */
	public Intervall Intersection(final Intervall other){
		if(this.intersects(other)){
			int l = Math.max(this._l, other._l);
			int r = Math.min(this._r, other._r);
			return new Intervall(l,r);
		}
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
		if(!legalBounds(l,this._r)){
			throw new IllegalArgumentException("illegal lowbound");
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
		if(!legalBounds(this._l,r)){
			throw new IllegalArgumentException("illegal highbound");
		}
		this._r = r;
	}

//--------------------STRING METHODS-----------------------------------------------//	
	
	/**
	 * Gives a String representation of an Intervall with correct brackets
	 * @return String representation
	 */
	@Override
	public String toString(){
		String l,r;
		switch (mode){
		case 1: 
			l="[";
			r="]";
			break;
		case 2:
			l="(";
			r="]";
			break;
		case 3:
			l="[";
			r=")";
			break;
		case 4:
			l="(";
			r=")";
			break;
		default: 
			l="|";
			r="|";
			break;
		}
		return (l+this._l+","+this._r+r);	
	}
	
//----------------------- SHIFTING ------------------------------------------------//	

	/**
	 * Method to shift a Intervall within the positive numbers. The Intervall might get cut off
	 * @param tau value to be shifted by
	 * @return Intervall in the positive integers null if the shift does not intersect the positive integers
	 */
	public Intervall shiftPositive(final int tau){
		int l,r;
		if(Integer.MAX_VALUE>this._r ){
			r=this._r+tau;
		}else{
			r=this._r;
		}
		l=Math.max(0,this._l+tau);
		if(legalBounds(l,r)){
			return new Intervall(l,r);
		}else return null;
	}
}
