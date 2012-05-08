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
package playground.droeder.eMobility.poi;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class POI {
	
	private double maxSpace;
	private List<Double> currentLoad;
	private double timeBinSize;
	private Id id;
	private int currentSlot = 0;
	private List<Double> maxLoad;
	
	public POI(Id id, double maxSpace, double timeBinSize){
		this(id,maxSpace, timeBinSize, 0.);
	}
	
	public POI(Id id, double maxSpace, double timeBinSize, double currentLoad){
		this.maxSpace = maxSpace;
		this.timeBinSize = timeBinSize;
		this.id = id;
		this.currentLoad = new ArrayList<Double>();
		this.currentLoad.add(this.currentSlot, currentLoad);
		this.maxLoad = new ArrayList<Double>();
		this.maxLoad.add(this.currentSlot, currentLoad);
	}
	
	public double getTimeBinSize(){
		return this.timeBinSize;
	}
	
	public Double[] getMaxLoad(){
		return this.maxLoad.toArray(new Double[this.currentLoad.size()]);
	}
	
	public double getMaxSpace(){
		return this.maxSpace;
	}

	/**
	 * 
	 */
	private void init(int slot) {
		if(slot > this.currentSlot){
			for(int i = (this.currentSlot + 1) ; i < (slot +1); i++){
				this.currentLoad.add(i, this.currentLoad.get(this.currentSlot));
				this.maxLoad.add(i, this.currentLoad.get(this.currentSlot));
			}
			this.currentSlot = slot;
		}
	}

	/**
	 * @param time 
	 * @return
	 */
	private boolean hasFreeChargingSpace(int slot) {
		this.init(slot);
		if(this.currentLoad.get(slot) < this.maxSpace){
			return true;
		}else{
			return false;
		}
	}
	
	public boolean plugVehicle(double time){
		int slot = this.getSlot(time);
		if(hasFreeChargingSpace(slot)){
			this.currentLoad.set(slot, this.currentLoad.get(slot) + 1);
			this.maxLoad.set(slot, Math.max(this.maxLoad.get(slot), this.currentLoad.get(slot)));
			return true;
		}else{
			return false;
		}
	}
	
	public void unplugVehicle (double time){
		int slot = this.getSlot(time);
		this.init(slot);
		this.currentLoad.set(slot, this.currentLoad.get(slot) - 1);
	}
	
	
	private int getSlot(double time){
		return (int) (time / this.timeBinSize);
	} 

	/**
	 * @return
	 */
	public Id getId() {
		return this.id;
	}

}
