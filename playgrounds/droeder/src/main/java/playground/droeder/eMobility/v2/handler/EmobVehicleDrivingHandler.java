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
package playground.droeder.eMobility.v2.handler;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.energy.DisChargingProfiles;
import playground.droeder.eMobility.v2.energy.EmobCharging;
import playground.droeder.eMobility.v2.energy.EmobDischarging;
import playground.droeder.eMobility.v2.fleet.EmobVehicle;

/**
 * @author droeder
 *
 */
public class EmobVehicleDrivingHandler implements LinkEnterEventHandler, LinkLeaveEventHandler{
	
	private static final Logger log = Logger
			.getLogger(EmobVehicleDrivingHandler.class);
	
	private Map<Id, LinkEnterEvent> veh2linkEnter;
	private Network net;
	private DisChargingProfiles disCharge;

	private Map<Id, EmobVehicle> vehicles;
	private Map<Id, EmobDischarging> usageProfiles;

	
	public EmobVehicleDrivingHandler(Network net, DisChargingProfiles disCharge){
		this.net = net;
		this.veh2linkEnter = new HashMap<Id, LinkEnterEvent>();
		this.disCharge = disCharge;
		this.vehicles = new HashMap<Id, EmobVehicle>();
		this.usageProfiles = new HashMap<Id, EmobDischarging>();
	}

	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.calcAndSetNewSocForDischarge(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.veh2linkEnter.put(event.getVehicleId(), event);
	}

	private void calcAndSetNewSocForDischarge(Id vehId, double time) {
		LinkEnterEvent e = this.veh2linkEnter.remove(vehId);
		EmobVehicle veh = this.vehicles.get(vehId);
		
		//TODO calc length from coordinates or use the given one?
		double length = this.net.getLinks().get(e.getLinkId()).getLength();
		double speed = length / (time - e.getTime());
		double slope = 0.0;
		double joulePerKm = this.disCharge.getJoulePerKm(this.usageProfiles.get(vehId).getType(), speed, slope);
		veh.setSoC(veh.getCurrentSoC() - (joulePerKm  * (length/1000)));
		veh.setPosistion(e.getLinkId());
	}

	/**
	 * @param vehicle
	 * @param currentDischargingProfile
	 */
	public void registerVeh(Id id, EmobVehicle vehicle, EmobDischarging currentDischargingProfile) {
		this.vehicles.put(id, vehicle);
		this.usageProfiles.put(id, currentDischargingProfile);
		
	}

	/**
	 * @param vehicleId
	 */
	public void removeVeh(Id vehicleId, Double time) {
		this.calcAndSetNewSocForDischarge(vehicleId, time);
	}

}
