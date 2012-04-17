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
package playground.droeder.eMobility.v3.population;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class EActivity{
	
	private double plannedStart;
	private double plannedDuration;
	private Id dischargingId;
	private Id chargingId;
	private Id poiId;

	public EActivity(Id poiId, double plannedStart, double plannedDuration, Id dischargingId, Id chargingId){
		this.plannedStart = plannedStart;
		this.plannedDuration = plannedDuration;
		this.dischargingId = dischargingId;
		this.poiId = poiId;
		this.chargingId = chargingId;
	}
	
	public Id getChargingId(){
		return this.chargingId;
	}

	/**
	 * @return
	 */
	public double plannedStart() {
		return this.plannedStart;
	}

	/**
	 * @return
	 */
	public double plannedDuration() {
		return this.plannedDuration;
	}

	/**
	 * @return
	 */
	public Id getDischargingId() {
		return this.dischargingId;
	}

	/**
	 * @return
	 */
	public Id getPoiId() {
		return this.poiId;
	}

	@Override
	public String toString(){
		return ("planned Start: " + plannedStart + " planned Duration: " + this.plannedDuration);
	}
}
