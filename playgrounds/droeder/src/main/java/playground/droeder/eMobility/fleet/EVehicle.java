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
package playground.droeder.eMobility.fleet;

import java.util.ArrayList;
import java.util.ListIterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;

import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.energy.DisChargingProfiles;
import playground.droeder.eMobility.events.SoCChangeEvent;
import playground.droeder.eMobility.population.EActivity;

/**
 * @author droeder
 *
 */
public class EVehicle {

	private boolean hasChanged = false;
	private double soc;
	private Double linkEnterTime = null;
	private double linkLength;
	private Id linkId;
	private Id id;
	private ListIterator<EActivity> chargingActs;
	
	public EVehicle (Id id, double soc, Link l, ArrayList<EActivity> chargingActivities){
		this.id = id;
		this.soc = soc;
		this.linkId = l.getId();
		this.linkLength = l.getLength();
		this.chargingActs = chargingActivities.listIterator();
	}
	
	public double getCurrentSoC(){
		return this.soc;
	}
	
	public Id getId(){
		return this.id;
	}

	/**
	 * @param manager
	 */
	public void update(EventsManager manager, double time) {
		if(this.hasChanged ){
			this.hasChanged = false;
			manager.processEvent(createSoCEvent(time));
		}
	}
	
	public void setLinkEnterTime(double time){
		this.linkEnterTime = time;
	}
	
	public void setLinkEnterInformation(double time, double linkLength, Id linkId){
		this.linkEnterTime = time;
		this.linkLength = linkLength;
		this.linkId = linkId;
	}
	
	
	public void setSoC(double soc){
		this.soc = soc;
		this.hasChanged = true;
	}

	/**
	 * @param time
	 * @return
	 */
	private Event createSoCEvent(double time) {
		return new SoCChangeEvent(this.id, time, this.soc, this.linkId);
	}



	/**
	 * @param time
	 * @param discharging
	 */
	public void disCharge(double time, DisChargingProfiles discharging) {
		if(currentActivity == null) return;
		double disChargePerKmInJoule = discharging.getJoulePerKm(currentActivity.getDischargingId(), this.linkLength / (time - linkEnterTime), 0.0);
		double discharge = disChargePerKmInJoule * this.linkLength / 1000. * 2.778 * Math.pow(10, -7);
		this.setSoC(this.soc - discharge);
	}


	private EActivity currentActivity= null;
	/**
	 * @param time
	 * @param discharging
	 */
	public void finishDriving(double time, DisChargingProfiles discharging, boolean charge) {
		if(this.currentActivity == null){
			this.charge = false;
		}else{
			double disChargePerKmInJoule = discharging.getJoulePerKm(currentActivity.getDischargingId(), this.linkLength / (time - linkEnterTime), 0.0);
			double discharge = disChargePerKmInJoule * this.linkLength / 1000. * 2.778 * Math.pow(10, -7);
			this.setSoC(this.soc - discharge);
			this.setLinkEnterTime(time);
			this.charge = charge;
		}
	}
	
	public Id getPoiId(){
		if(this.currentActivity == null){
			return null;
		}
		return this.currentActivity.getPoiId();
	}
	
	private boolean charge = false;

	/**
	 * @param time
	 * @param charging
	 */
	public boolean finishCharging(double time, ChargingProfiles charging) {
		if(this.charge){
			double duration, start;
			if(currentActivity.plannedStart() < this.linkEnterTime){
				start = this.linkEnterTime;
			}else{
				start = currentActivity.plannedStart();
			}
			if((time - start) < currentActivity.plannedDuration()){
				duration = time - start;
			}else{
				duration = currentActivity.plannedDuration();
			}
			this.setSoC(charging.getNewState(currentActivity.getChargingId(), duration, this.soc));
		}
		this.setLinkEnterTime(time);
		if(this.chargingActs.hasNext()){
			currentActivity = this.chargingActs.next();
		}else{
			currentActivity = null;
		}
		return this.charge;
	}
	

}
