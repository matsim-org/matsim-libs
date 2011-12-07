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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;


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
	private Double traveledDistance;
	private ArrayList<Tuple<Double, Double>> time2charge;
	private ArrayList<Tuple<Double, Double>> distance2charge;


	public ElectroVehicle(Id id, Double maxCharge, Double currentCharge){
		this.id = id;
		this.maxCharge = maxCharge;
		this.currentCharge = currentCharge;
		this.chargingType = new IdImpl("default");
		this.dischargingType = new IdImpl("default");
		this.time2charge = new ArrayList<Tuple<Double, Double>>();
		this.distance2charge = new ArrayList<Tuple<Double,Double>>();
		this.traveledDistance = 0.;
	}
	
	public Id getId(){
		return this.id;
	}
	
	public double getChargeState(){
		return this.currentCharge;
	}
	
	public void disCharge(Double kWh, Double time, Double distance){
		double newCharge = this.currentCharge - kWh;
		this.saveCharge(time, newCharge, distance);
	}
	
	public void setNewCharge(Double newChargekWh, Double time){
		this.saveCharge(time, newChargekWh, 0.);
	}
	
	/**
	 * @param time
	 * @param distance 
	 * @param currentCharge
	 */
	private void saveCharge(Double time, Double newCharge, Double distance) {
		this.currentCharge = newCharge;
		this.time2charge.add(new Tuple<Double, Double>(time, this.currentCharge));
		this.traveledDistance+= distance;
		this.distance2charge.add(new Tuple<Double, Double>(this.traveledDistance, this.currentCharge));
	}

	public Id getDisChargingType(){
		return this.dischargingType;
	}
	
	public Id getChargingType(){
		return this.chargingType;
	}
}
