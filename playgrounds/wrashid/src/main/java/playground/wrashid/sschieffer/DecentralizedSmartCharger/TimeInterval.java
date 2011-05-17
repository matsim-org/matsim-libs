/* *********************************************************************** *
 * project: org.matsim.*
 * timeInterval.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

/**
 * abstract class TimeInterval stores start and end second of time interval
 * 
 * @author Stella
 *
 */
public abstract class TimeInterval implements Comparable{
	double start;
	double end;
	
	
	
	
	TimeInterval(double start, double end){
		this.start=start;
		this.end=end;
	}
	
	
	public double getStartTime(){
		return start;
	}
	
	public double getEndTime(){
		return end;
	}
	
	
	/**
	 * checks if this time interval is included within he specified time interval
	 * @param other
	 * @return
	 */
	public boolean isIncluded (TimeInterval other){
		boolean check=false;
		if( other.timeIsEqualOrGreaterThanStartAndEqualOrSmallerThanEnd(getStartTime())
				&&
				other.timeIsEqualOrGreaterThanStartAndEqualOrSmallerThanEnd(getEndTime()) 
				&&
				getStartTime()<getEndTime()){
			check=true;
		}
		return check;
	}
	
	
	
	/**
	 * checks whether this time interval includes the specified time interval
	 * @param other
	 * @return
	 */
	public boolean includes(TimeInterval other){
		boolean check=false;
		if( timeIsEqualOrGreaterThanStartAndEqualOrSmallerThanEnd(other.getStartTime()) 
				&&
				timeIsEqualOrGreaterThanStartAndEqualOrSmallerThanEnd(other.getEndTime())
				&&
				other.getStartTime()<other.getEndTime()) {
			check=true;
		}
		return check;
	}
	
	
	public boolean equalInterval(TimeInterval other){
		boolean check=false;
		if( getStartTime()==other.getStartTime() 
				&&
				getEndTime() == other.getEndTime()) {
			check=true;
		}
		return check;
	}
	
	/**
	 * checks if the specified interval is partly in this interval, i.e. only the start or end time is in the interval
	 * @param other
	 * @return
	 */
	public boolean partlyIncludes(TimeInterval other){
		boolean check=false;
		//only beginning in
		if( timeIsGreaterThanStartAndSmallerThanEnd(other.getStartTime()) 
				&& 
				!timeIsEqualOrGreaterThanStartAndEqualOrSmallerThanEnd(other.getEndTime())) {
			check=true;
		}
		
		//only end in
		if( !timeIsEqualOrGreaterThanStartAndEqualOrSmallerThanEnd(other.getStartTime()) 
				&& 
				timeIsGreaterThanStartAndSmallerThanEnd(other.getEndTime())) {
			check=true;
		}
		return check;
	}
	
	
	/**
	 * returns true, if the interval includes, is equal, is included or partly includes the specified interval
	 * @param other
	 * @return
	 */
	public boolean overlap(TimeInterval other){
		boolean check=false;
		if( this.includes(other) || equalInterval(other)
				||this.partlyIncludes(other) ||
				this.isIncluded(other) ) {
			check=true;
		}
		return check;
	}
	
	/**
	 * returns 1 if this object starts later
	 * returns -1 if this starts before
	 * returns 0 if same starting time
	 * 	 * @param o
	 * @return
	 */
	public int compareTo(Object o){
		TimeInterval t = (TimeInterval) o;
		
		if (start>t.getStartTime()){
			return 1;
		}
		else{ if(start<t.getStartTime()){
			return -1;
			
			}else{// equal start
				return 0;
				}
			}
	}
	
	
	
	public LoadDistributionInterval ifOverlapWithLoadDistributionIntervalReturnOverlap(LoadDistributionInterval l){
		
		if(includes(l)){
			//if start and end in
			return l;
			
		}else{
			if( partlyIncludes(l)){
				//if start in and end not in
				if(timeIsEqualOrGreaterThanStartAndEqualOrSmallerThanEnd(l.getStartTime())){
					return new LoadDistributionInterval(l.getStartTime(), 
							getEndTime(), 
							l.getPolynomialFunction(), 
							l.isOptimal());
				}else{//if start not in and end in
					return new LoadDistributionInterval(getStartTime(), 
							l.getEndTime(), 
							l.getPolynomialFunction(), 
							l.isOptimal());
				}
				
				
			}else{
					if (isIncluded(l)){
						return new LoadDistributionInterval(getStartTime(), 
								getEndTime(), 
								l.getPolynomialFunction(), 
								l.isOptimal());
						
					}else{
						//nothing in
						return null;
					}
					
				}
				
			}
		
	}
	
	
	public boolean timeIsEqualOrGreaterThanStartAndEqualOrSmallerThanEnd(double time){
		if(time>=getStartTime() && time<=getEndTime()){
			return true;
		}else{return false;}
	}
	
	
	public boolean timeIsGreaterThanStartAndSmallerThanEnd(double time){
		if(time>getStartTime() && time<getEndTime()){
			return true;
		}else{return false;}
	}
	
	
	public double getIntervalLength(){
		return end-start;
	}
	
	public boolean isParking(){
		if(this.getClass().equals(ParkingInterval.class)){
			return true;
		}else{return false;}
	}
	
	public boolean isDriving(){
		if(this.getClass().equals(DrivingInterval.class)){
			return true;
		}else{return false;}
	}
	
	public boolean isCharging(){
		if(this.getClass().equals(ChargingInterval.class)){
			return true;
		}else{return false;}
	}
	
	public boolean isLoadDistributionInterval(){
		if(this.getClass().equals(LoadDistributionInterval.class)){
			return true;
		}else{return false;}
	}
	
	
	public void printInterval(){
		System.out.println("start: "+ this.getStartTime()+ "\t  end: "+ this.getEndTime());
	}
	
}
