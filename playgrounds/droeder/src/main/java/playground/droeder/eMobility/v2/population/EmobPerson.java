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
package playground.droeder.eMobility.v2.population;

import java.util.List;
import java.util.ListIterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;

import playground.droeder.eMobility.v2.energy.EmobCharging;
import playground.droeder.eMobility.v2.energy.EmobDischarging;


/**
 * @author droeder
 *
 */
public class EmobPerson {
	
	
	private Id id;
	private Plan plan;
	private List<EmobCharging> charging;
	private List<EmobDischarging> discharging;
	private int actCnt;
	private int appCnt;
	private int legCnt;
	private boolean atChargingLocation;
	

	public EmobPerson(Id id, Plan plan, List<EmobCharging> charging, List<EmobDischarging> disCharging){
		this.id = id;
		this.plan = plan;
		this.charging =  charging;
		this.discharging = disCharging;
		this.actCnt = 0;
		this.appCnt = 0;
		this.legCnt = -1;
		this.atChargingLocation = false;
	}
	
	public Id getId(){
		return this.id;
	}
	
	public boolean atChargingStation(){
		return this.atChargingLocation;
	}

	/**
	 * @param e
	 */
	public void processEvent(AgentArrivalEvent e) {
		this.actCnt++;
		if(this.actCnt == 2){
			this.atChargingLocation = true;
		}else if(this.actCnt == 3){
			this.actCnt = 0;
			this.legCnt = 0;
			this.appCnt++;
		}
	}

	/**
	 * @param e
	 */
	public void processEvent(AgentDepartureEvent e) {
		this.legCnt++;
		if(this.legCnt == 1){
			this.atChargingLocation = false;
		}
	}
	
	public EmobCharging getCurrentChargingParameters(){
		return this.charging.get(this.appCnt);
	}
	
	public EmobDischarging getCurrentDischargingProfile(){
		return this.discharging.get(this.appCnt);
	}

}
