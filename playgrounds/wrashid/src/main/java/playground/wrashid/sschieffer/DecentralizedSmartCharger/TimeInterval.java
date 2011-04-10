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
		if( (other.getStartTime()>=getStartTime() && other.getStartTime()<getEndTime()) 
				||
				(other.getEndTime()>getStartTime() && other.getEndTime()<=getEndTime())){
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
	
	
	public void printInterval(){
		System.out.println("start: "+ this.getStartTime()+ "\t  end: "+ this.getEndTime());
	}
	
}
