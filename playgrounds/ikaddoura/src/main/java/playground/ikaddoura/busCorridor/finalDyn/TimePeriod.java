/* *********************************************************************** *
 * project: org.matsim.*
 * TimePeriod.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.finalDyn;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Time;

/**
 * @author Ihab
 *
 */
public class TimePeriod {

	private final static Logger log = Logger.getLogger(TimePeriod.class);

	private int orderId;
	private int numberOfBuses;
	private double fromTime;
	private double toTime;
	
	public TimePeriod(int orderId, int numberOfBuses, double fromTime, double toTime){
		this.orderId = orderId;
		this.numberOfBuses = numberOfBuses;
		this.fromTime = fromTime;
		this.toTime = toTime;
	}
	public TimePeriod(){

	}
	
	public void increaseNumberOfBuses(int increase){
		this.setNumberOfBuses(this.getNumberOfBuses() + increase);
		log.info("Number of buses in time period "+this.getOrderId()+" for next external iteration: "+this.getNumberOfBuses()+".");
	}
	
	public String toString(){
		String tag = Time.writeTime(this.getFromTime(), Time.TIMEFORMAT_HHMMSS)+"-"+Time.writeTime(this.getToTime(), Time.TIMEFORMAT_HHMMSS)+"-"+this.getNumberOfBuses();
		return tag;
	}
	
	/**
	 * @return the numberOfBuses
	 */
	public int getNumberOfBuses() {
		return numberOfBuses;
	}
	/**
	 * @param numberOfBuses the numberOfBuses to set
	 */
	public void setNumberOfBuses(int numberOfBuses) {
		this.numberOfBuses = numberOfBuses;
	}
	/**
	 * @return the fromTime
	 */
	public double getFromTime() {
		return fromTime;
	}
	/**
	 * @param fromTime the fromTime to set
	 */
	public void setFromTime(double fromTime) {
		this.fromTime = fromTime;
	}
	/**
	 * @return the toTime
	 */
	public double getToTime() {
		return toTime;
	}
	/**
	 * @param toTime the toTime to set
	 */
	public void setToTime(double toTime) {
		this.toTime = toTime;
	}


	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}


	public int getOrderId() {
		return orderId;
	}
	
	
	
}

