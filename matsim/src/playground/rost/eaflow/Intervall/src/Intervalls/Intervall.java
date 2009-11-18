/* *********************************************************************** *
 * project: org.matsim.*
 * Intervall.java
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
 * class representing intervalls in the reals boundary pionts are integral 
 * @author Manuel Schneider
 *
 */
public class Intervall
implements Comparable<Intervall>
{
	
// -------------------FIELDS---------------------------------//	
	
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
	
	

//-------------------METHODS---------------------------------//
//***********************************************************//
	
	
	
//	-----------------Constructors----------------------------//
	
	/**
	 * constructing a default interval [0,1] 
	 */
	public Intervall(){
		this._l = 0;
		this._r =1;
		
	}
	
	/**
	 * Constructs an Intervall given containing all piont between l and r.
	 * Only accepts l <= r.
	 * @param l lowbound of the Intervall
	 * @param r highbound of the Interval
	 * 
	 */
	public Intervall(int l, int r){
		if(legalBounds(l,r)){
			this._l = l;
			this._r = r ;
		}else{
			throw new IllegalArgumentException("Empty Interval");
		}
	}
//--------------------MODE STUFF-------------------------------//
	
	/**
	 * this method checks weather the bounds would be feasible for an intervall according to the current mode
	 * @param l represents the potential lowboud
	 * @param r	represents the potential highbound
	 * @return true iff l and r represent an Interval in current mode 
	 */
	public static boolean legalBounds(int l, int r){
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
	 * checks weather the bounds of the Intervall are valid in the current mode
	 * (x,x) Intervalls are cosidered valid in mode 1
	 * (x,x+1) Inter are considered valoid in mode 4
	 * @return true iff bound are valid
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
	public static void setMode(int m){
		if(1<=m && m<=4){
			mode=m;
		}
		else {
			mode=3;
		}
	}
	
	/**
	 * returns the current mode 
	 * @return mode
	 */
	public static int getMode(){
		return mode;
	}
	
	
//--------------------CONTAINING DEVIDING LENGTH---------------// 
	
	/**
	 * Methot to get the position of th Intervall with respect to a point t.
	 * @param t
	 * @return 0 iff t is contained, 1 iff t is lowbound but not contained, 2 iff t<lowbound , -1 iff t is highbound but not contained -2 iff t>highbound
	 */
	public int posWithRespTo(int t){
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
	 * Method to decide weather a point t is contained in the Interval. It uses the current mode of intervalls
	 * @param t point
	 * @return true iff t is contained in the intervall 
	 */
	public boolean contains(int t){
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
	 * chcks weather a given point t is in  the interionr of an Intervall
	 * @param t point to check
	 * @return true iff lowbound<t<highbound
	 */
	public boolean hasInteriorPoint(int t){
		return ( (this._l<t)&&(this._r>t) );
	}
	
	/**
	 * Method to decide weather a given Intervall is contained in the referenced Intervall 
	 * @param other
	 * @return true iff other is a subset of refereced Intervall
	 */
	public boolean contains(Intervall other){
		int m = getMode();
		setMode(1);
		boolean ret;
		ret = ( this.contains(other._r) && this.contains(other._l) );
		setMode(m);
		return ret;
	}
	
	/**
	 * Calculates the length of a given intervall basicly highbound-lowbound
	 * @return length
	 */
	public int length(){
		return (this._r-this._l);
		}
	
	/**
	 * Splits the intervall up into two intervalls if t is an Interior piont or t is a bound in a closed Intervall.
	 * Does not modify the referenced Intervall!!!!
	 * @param t point to split the intervall at
	 * @return an Array of Intervalls with the "smaller" one at first
 	 */
	public Intervall[] getSplitedAt(int t){
		if(this.hasInteriorPoint(t)){
			Intervall[] arr = new Intervall[2];
			arr[0]=new Intervall(this._l,t);
			arr[1]=new Intervall(t,this._r);
			return arr;
		}
		if(mode == 1 && this.contains(t)){
			Intervall[] arr = new Intervall[2];
			arr[0]=new Intervall(this._l,t);
			arr[1]=new Intervall(t,this._r);
			return arr;
		}
		throw new IllegalArgumentException("cant split interval at " + t +" since it ist not an interior piont in the Intervall" );
	}
	
	/**
	 * Splits the intervall up into two intervalls if t is an Interior piont or t is a bound in a closed Intervall.
	 * The referenced intervall is changed to the "smaller" one.
	 * Does modify the referenced Intervall!!!!
	 * @param t point to split the intervall at
	 * @return Intervall wich is the "higher" one
 	 */
	public Intervall splitAt(int t){
		if(this.hasInteriorPoint(t)){
			Intervall i=new Intervall(t,this._r);
			this.setHighBound(t);
			return i;
		}
		if(mode == 1 && this.contains(t)){
		
			Intervall i=new Intervall(t,this._r);
			this.setHighBound(t);
			return i;
		}
		//TODO maybe return null
		throw new IllegalArgumentException("cant split interval at " + t +" since it ist not an interior piont in the Intervall" );
	}
	
	/**
	 * compares two Intervalls by the lowbounds. does not consens with the equal method!!!!
	 * @param o other Interval
	 * @return 0 iff lowbounds are equal <0 iff referenced lowbound is smaller than otherts lowbound >0 else
	 */
	public int compareTo(Intervall o) {
		
		return (this._l-o._l);
	}
	
	
//--------------------INTERSECTION UNION-----------------------//	
	
	
	/**
	 * checks weather two intervall are equal with respect to thier bounds
	 * @param i  other Intervall
	 * @return true iff both are equal
	 */
	public boolean equals(Intervall i)
	{
		return(this._l==i._l && this._r == i._r);
	}
	
	
	/**
	 * Decides weather two Intervalls intersect. This method does not modify 
	 * the given Intervalls.
	 * @param other 
 	 * @return true iff refferenced and other intervall share a point
	 */
	public boolean intersects(Intervall other){
		if(mode!=4){
			if(this.contains(other._r) && other.contains(other._r) ){
				//System.out.println(this+" 1 " + other);
				return true;
			}
			if(this.contains(other._l) && other.contains(other._l)){
				//System.out.println(this+" 2 " + other);
				return true;
			}
			if(other.contains(this._l) && this.contains(this._l)){
				//System.out.println(this+" 3 " + other);
				return true;
			}
			if(other.contains(this._r) && this.contains(this._r)){
				//System.out.println(this+" 4 " + other);
				return true;
			}
			return false;
		}
		else{
			if(this.contains(other._r)  ){
				//System.out.println(this+" 1 " + other);
				return true;
			}
			if(this.contains(other._l) ){
				//System.out.println(this+" 2 " + other);
				return true;
			}
			if(other.contains(this._l) ){
				//System.out.println(this+" 3 " + other);
				return true;
			}
			if(other.contains(this._r) ){
				//System.out.println(this+" 4 " + other);
				return true;
			}
			if(other.equals(this)){
				return true;
			}
			return false;
		}
	}

	
	/**
	 * Calculates the intersection of two intervalls. This method does not modify 
	 * the given Intervalls.
	 * @param other 
	 * @return returnes the Interval of all points shared by the refferenced and and other Intervall
	 */
	public Intervall Intersection(Intervall other){
		if (other!=null){
			if(other.getHighBound() == 8 && other.getLowBound() == 5)
				System.out.println("jasfcbsofbv");
			if(this.intersects(other)){
				int l = Math.max(this._l, other._l);
				int r = Math.min(this._r, other._r);
				return new Intervall(l,r);
			}
			return null;
		}
		throw new NullPointerException("Intervall is null");
	}
	
/*	
	*//**
	 * Returns the minimal Intervall containing refereced and other Intervall
	 * @param other
	 * @return minimal Intervall
	 *//*
	public Intervall getMinContaining(Intervall other){
		if (other!=null){
			int r = Math.max(this._r, other._r);
			int l = Math.min(this._l, other._l);
			return new Intervall(l,r);
		}
		throw new NullPointerException("Intervall is null");
	}
	
	*//**
	 * Method returning the minimal Intervall containing all Inervalls in C 
	 * @param C Collection of Intervalls to "unify"
	 * @return minimal Intervall
	 *//*
	public static Intervall getMinContaining( Collection<Intervall> C){
		
		int r = maxRightValue(C);
		int l = minLeftValue(C);
		return new Intervall(l,r);
	}
	
//---------------------MIN MAX BOUNDS--------------------------//
	
	*//**
	 * Method returnig the Intervall with maximal rightbound in C
	 * @param C Collection of Intervalls to be maximized over
	 * @return Intervall with maximal Rightbound or null if C is empty
	 *//*
	public static Intervall maxRight( Collection<Intervall> C){
		if(C==null)throw new NullPointerException("Collection was null");
		if(!C.isEmpty()){
			int max = Integer.MIN_VALUE;
			Intervall maxintervall = null;
			for(Intervall i : C){
				if (max<=i._r){
					max= i._r;
					maxintervall=i;
				}
			}
		 return maxintervall;	
		}
		throw new IllegalArgumentException("Empty Collection");
	}
	
	*//**
	 * Metod returning the maximal Value of all rightbounds of Intervalls in C 
	 * @param C Collection of Intervalls to be maximized over
	 * @return maximal rightbound or int.NaN if C is empty
	 *//*
	public static int maxRightValue( Collection<Intervall> C){
		Intervall max= maxRight(C);
		return max._r;
	}

	*//**
	 * Method returnig the Intervall with minimal leftbound in C
	 * @param C Collection of Intervalls to be minimized over
	 * @return Intervall with minimal leftbound or null if C is empty
	 *//*
	public static Intervall minLeft( Collection<Intervall> C){
		if(C==null)throw new NullPointerException("Collection was null");
		if(!C.isEmpty()){
			int min = Integer.MAX_VALUE;
			Intervall minintervall = null;
			for(Intervall i : C){
				if (min >= i._l){
					min= i._l;
					minintervall=i;
				}
			}
			return minintervall;	
		}
		throw new IllegalArgumentException("Empty Collection");
	}
	
	*//**
	 * Metod returning the minimal Value of all leftbounds of Intervalls in C 
	 * @param C Collection of Intervalls to be minimized over
	 * @return minimal leftbound or int.NaN if C is empty
	 *//*
	public static int minLeftValue( Collection<Intervall> C){
		Intervall min= minLeft(C);
		return min._l;
	}
	*/
//-------------------------GETTER AND SETTER---------------------------------//	
	
	/** 
	 * getter for leftbound
	 * @return the _l
	 */
	public int getLowBound() {
		return _l;
	}

	/** 
	 * setter for leftbound
	 * @param l the _l to set
	 */
	public void setLowBound(int l) {
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
	 * setter for rightbound
	 * @param r the _r to set
	 */
	public void setHighBound(int r) {
		if(!legalBounds(this._l,r)){
			throw new IllegalArgumentException("illegal highbound");
		}
		this._r = r;
	}
	
	/**
	 * Gives a String representation of abn Intervall with correct brackets
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
	//TODO comment
	public Intervall shiftPositive(int tau){
		int l,r;
		if(Integer.MAX_VALUE/2>this._r ){
			r=this._r+tau;
		}else{
			r=this._r;
		}
		l=Math.max(0,this._l+tau);
		if(legalBounds(l,r)){
			return new Intervall(l,r);
		}else return null;
	}

//-------------------------MAIN METHOD------------------------------------------//
	



}
