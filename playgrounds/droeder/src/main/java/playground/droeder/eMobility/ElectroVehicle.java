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
package playground.droeder.eMobility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;


/**
 * @author droeder
 *
 */
public class ElectroVehicle {
	private static final Logger log = Logger.getLogger(ElectroVehicle.class);
	
	private Id id;
	private Id dischargingType;
	private Id chargingType;
	private Double maxCharge;
	private Double currentCharge;
	private Double chargeState;
	private boolean warnEmptyAccu = true;
	private List<Double> timeline;
	private List<Double> charge;

	public ElectroVehicle(Id id, Double maxCharge, Double currentCharge){
		this.id = id;
		this.maxCharge = maxCharge;
		this.currentCharge = currentCharge;
		this.chargeState = currentCharge/maxCharge;
		this.chargingType = new IdImpl("default");
		this.dischargingType = new IdImpl("default");
		this.timeline = new ArrayList<Double>();
		this.charge = new ArrayList<Double>();
		if(chargeState > 1.0){
			log.error("vehicle " + this.id + ": current charge exceeds maximum charge...");
		}
	}
	
	public Id getId(){
		return this.id;
	}
	
	public double getChargeState(){
		return this.chargeState;
	}
	
	public void disCharge(Double kWh, Double time){
		double newCharge = this.currentCharge - kWh;
		if(newCharge < 0.0){
			this.currentCharge = 0.0;
//			if(this.warnEmptyAccu){
//				log.error("vehicle " + this.id + ": accu is empty. Veh still in Sim. Message thrown only once per Vehicle...");
//				this.warnEmptyAccu = false;
//			}
		}else{
			this.currentCharge = newCharge;
		}
		this.saveEnergyState(time, this.currentCharge);
		this.chargeState = (this.currentCharge / this.maxCharge);
	}
	
	public void charge(Double kWh, Double time){
		double newCharge = this.currentCharge + kWh;
		if(newCharge > this.maxCharge ) {
			this.currentCharge = maxCharge;
		}else{
			this.currentCharge = newCharge;
		}
		this.saveEnergyState(time, this.currentCharge);
		this.chargeState = (this.currentCharge / this.maxCharge);
	}
	
	/**
	 * @param time
	 * @param currentCharge
	 */
	private void saveEnergyState(Double time, Double currentCharge) {
		this.timeline.add(time);
		this.charge.add(currentCharge);
	}

	public Id getDisChargingType(){
		return this.dischargingType;
	}
	
	public Id getChargingType(){
		return this.chargingType;
	}
}
