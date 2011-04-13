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

public abstract class TimeInterval implements Comparable{
	double start;
	double end;
	
	TimeInterval(double start, double end){
		this.start=start;
		this.end=end;
	}
	
	
	double getStartTime(){
		return start;
	}
	
	double getEndTime(){
		return end;
	}
	
	
	public boolean overlap(TimeInterval other){
		boolean check=false;
		if( timeIsEqualOrGreaterThanStartOrEqualOrSmallerThanEnd(other.getStartTime()) 
				||
				timeIsEqualOrGreaterThanStartOrEqualOrSmallerThanEnd(other.getEndTime()) ) {
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
		
		if(timeIsEqualOrGreaterThanStartOrEqualOrSmallerThanEnd(l.getStartTime())
				&&
				timeIsEqualOrGreaterThanStartOrEqualOrSmallerThanEnd(l.getEndTime()	)){
			//if start and end in
			return l;
			
		}else{
			if(timeIsEqualOrGreaterThanStartOrEqualOrSmallerThanEnd(l.getStartTime())
					&&
					!timeIsEqualOrGreaterThanStartOrEqualOrSmallerThanEnd(l.getEndTime()	)){
				//if start in and end not in
				return new LoadDistributionInterval(l.getStartTime(), 
						getEndTime(), 
						l.getPolynomialFunction(), 
						l.isOptimal());
				
			}else{
				if(!timeIsEqualOrGreaterThanStartOrEqualOrSmallerThanEnd(l.getStartTime())
						&&
						timeIsEqualOrGreaterThanStartOrEqualOrSmallerThanEnd(l.getEndTime()	)){
					//if start not in and end in
					return new LoadDistributionInterval(getStartTime(), 
							l.getEndTime(), 
							l.getPolynomialFunction(), 
							l.isOptimal());
					
				}else{
					//nothing in
					return null;
				}
				
			}
			
		}
	}
	
	
	public boolean timeIsEqualOrGreaterThanStartOrEqualOrSmallerThanEnd(double time){
		if(time>=getStartTime() || time<=getEndTime()){
			return true;
		}else{return false;}
	}
	
	
	public double getIntervalLength(){
		return end-start;
	}
	
	public boolean isParking(){
		if(this.getClass().equals(new ParkingInterval(0,0,null).getClass())){
			return true;
		}else{return false;}
	}
	
	public boolean isDriving(){
		if(this.getClass().equals(new DrivingInterval(0,0,0).getClass())){
			return true;
		}else{return false;}
	}
	
	public boolean isCharging(){
		if(this.getClass().equals(new ChargingInterval(0,0).getClass())){
			return true;
		}else{return false;}
	}
	
	public boolean isLoadDistributionInterval(){
		if(this.getClass().equals(new LoadDistributionInterval(0,0, new PolynomialFunction(new double[]{0}), false).getClass())){
			return true;
		}else{return false;}
	}
	
	
	public void printInterval(){
		System.out.println("start: "+ this.getStartTime()+ "\t  end: "+ this.getEndTime());
	}
	
}
