/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.eMobility.v2.energy;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class EmobCharging {
	private static final Logger log = Logger.getLogger(EmobCharging.class);
	
	private Double start;
	private Double end;
	private Id type;

	public EmobCharging(Id type, Double plannedStart, Double end){
		this.type = type;
		this.start = plannedStart;
		this.end = end;
	}
	
	public Double getStart(){
		return this.start;
	}
	
	public void setRealStart(double time){
		if(time > this.start){
			this.start = time;
		}
	}
	
	public Double getEnd(){
		return this.end;
	}
	
	public Double getDuration(){
		return (this.end - this.start);
	}
	
	public Id getProfile(){
		return this.type;
	}

	/**
	 * @param time
	 */
	public void checkRealEnd(double time) {
		if(this.end > time){
			log.error("the realEndTime should not be smaller, than the given EndTime. Check plansFile...");
		}
	}

}
