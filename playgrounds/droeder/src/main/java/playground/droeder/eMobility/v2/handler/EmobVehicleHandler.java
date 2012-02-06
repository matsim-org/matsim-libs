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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

import playground.droeder.eMobility.energy.DisChargingProfiles;
import playground.droeder.eMobility.v2.fleet.EmobFleet;
import playground.droeder.eMobility.v2.fleet.EmobVehicle;

/**
 * @author droeder
 *
 */
public class EmobVehicleHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonLeavesVehicleEventHandler{
	
	private EmobFleet fleet;
	private Map<Id, LinkEnterEvent> person2linkEnter;
	private Network net;
	private DisChargingProfiles disCharge;
	
	public EmobVehicleHandler(EmobFleet fleet, Network net, DisChargingProfiles disCharge){
		this.fleet = fleet;
		this.net = net;
		this.person2linkEnter = new HashMap<Id, LinkEnterEvent>();
		this.disCharge = disCharge;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.calcAndSetNewSoc(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.person2linkEnter.put(event.getVehicleId(), event);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		this.calcAndSetNewSoc(event.getVehicleId(), event.getTime());
	}
	
	private void calcAndSetNewSoc(Id vehId, double time) {
		LinkEnterEvent e = this.person2linkEnter.remove(vehId);
		EmobVehicle veh = this.fleet.getVehicle(vehId);
		
		//TODO calc links from coordinates or use the given one?
		double l = this.net.getLinks().get(e.getLinkId()).getLength();
		double speed = l / (time - e.getTime());
		double slope = 0.0;
		double joulePerKm = this.disCharge.getJoulePerKm(veh.getCurrentDischargingMode(), speed, slope);
		veh.setSoC(veh.getCurrentSoC() - (joulePerKm  * (l/1000)));
	}

}
