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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.energy.DisChargingProfiles;
import playground.droeder.eMobility.events.VehiclePlugEvent;
import playground.droeder.eMobility.poi.PoiInfo;

/**
 * @author droeder
 *
 */
public class EFleet implements MobsimAfterSimStepListener{
	private static final Logger log = Logger.getLogger(EFleet.class);
	
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
		if(v.getPoiId() == null) return;
		boolean plug = this.poiInfo.plugVehicle(v.getPoiId(), event.getTime());
		this.manager.processEvent(new VehiclePlugEvent(event.getTime(), plug, v.getPoiId()));
		if(plug){
			v.finishDriving(event.getTime(), this.discharging, plug);
			System.out.println("plug: " + v.getId() + " at " + event.getTime() + ", " + v.getPoiId());
		}
	}


	/**
	 * @param event
	 */
	public void processEvent(PersonEntersVehicleEvent event) {
		EVehicle v = this.fleet.get(event.getVehicleId());
		Id poiId = v.getPoiId();
		if(v.finishCharging(event.getTime(), this.charging)){
			// unplug only if the vehicle was plugged...
			this.poiInfo.unplugVehicle(poiId, event.getTime());
			System.out.println("unplug: " + v.getId() + " at " + event.getTime() + ", " + poiId);
		}
	}

	/**
	 * @param vehicleId
	 * @return
	 */
	public boolean containsVehicle(Id vehicleId) {
		return this.fleet.containsKey(vehicleId);
	}


}
