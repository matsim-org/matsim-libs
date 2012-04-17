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
package playground.droeder.eMobility.v3.poi;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class POI {
	
	private double maxSpace;
	private Double[] maxLoad  = new Double[48];
	private double timeBinSize;
	private Id id;
	private int currentSlot = 0;
	
	public POI(Id id, double maxSpace, double timeBinSize){
		this.maxSpace = maxSpace;
		this.timeBinSize = timeBinSize;
		this.id = id;
		this.maxLoad[currentSlot] = 0.;
	}

	/**
	 * 
	 */
	private void init(int slot) {
		if(slot > this.currentSlot){
			for(int i = (this.currentSlot + 1) ; i < (slot +1); i++){
				this.maxLoad[i] = this.maxLoad[this.currentSlot];
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
		if(this.maxLoad[slot] < this.maxSpace){
			return true;
		}else{
			return false;
		}
	}
	
	public boolean plugVehicle(double time){
		int slot = this.getSlot(time);
		if(hasFreeChargingSpace(slot)){
			this.maxLoad[slot]++;
			return true;
		}else{
			return false;
		}
	}
	
	public void unplugVehicle (double time){
		int slot = this.getSlot(time);
		this.init(slot);
		this.maxLoad[slot]--;
	}
	
	
	private int getSlot(double time){
		return (int) (time / timeBinSize);
	}

	/**
	 * @return
	 */
	public Id getId() {
		return this.id;
	}

}
