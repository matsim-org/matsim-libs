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

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.energy.DisChargingProfiles;
import playground.droeder.eMobility.poi.PoiInfo;

/**
 * @author droeder
 *
 */
public class EFleet implements MobsimAfterSimStepListener{
	
	private EventsManager manager;
	private HashMap<Id, EVehicle> fleet;
	private Network net;
	private DisChargingProfiles discharging;
	private ChargingProfiles charging;
	private PoiInfo poiInfo;

	public EFleet(){
		this.fleet =  new HashMap<Id, EVehicle>();
	}
	
	public void addVehicle(EVehicle v){
		this.fleet.put(v.getId(), v);
	}

	public void init(ChargingProfiles charging, DisChargingProfiles discharging, Network net, PoiInfo poiInfo){
		this.charging = charging;
		this.discharging = discharging;
		this.net = net;
		this.poiInfo = poiInfo;
	}
	
	public void registerEventsManager(EventsManager manager){
		this.manager = manager;
	}
	
	@Override
	public void notifyMobsimAfterSimStep(@SuppressWarnings("rawtypes") MobsimAfterSimStepEvent e) {
		for(EVehicle v: fleet.values()){
//			if(v.getId().equals(new IdImpl("emob_9"))){
//				System.out.println(v.getCurrentSoC());
//			}
			v.update(this.manager, e.getSimulationTime());
		}
	}

	/**
	 * @param event
	 */
	public void processEvent(LinkLeaveEvent event) {
		this.fleet.get(event.getVehicleId()).disCharge(event.getTime(), this.discharging);
	}


	/**
	 * @param event
	 */
	public void processEvent(LinkEnterEvent event) {
		this.fleet.get(event.getVehicleId()).setLinkEnterInformation(event.getTime(), this.net.getLinks().get(event.getLinkId()).getLength(), event.getLinkId());		
	}


	/**
	 * @param event
	 */
	public void processEvent(PersonLeavesVehicleEvent event) {
		EVehicle v = this.fleet.get(event.getVehicleId());
		v.finishDriving(event.getTime(), this.discharging, this.poiInfo.plugVehicle(v.getPoiId(), event.getTime()));
	}


	/**
	 * @param event
	 */
	public void processEvent(PersonEntersVehicleEvent event) {
		this.fleet.get(event.getVehicleId()).finishCharging(event.getTime(), this.charging);
	}
}
