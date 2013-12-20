/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.wagonSim.mobsim.qsim.pt;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener.VehicleLoad;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.PassengerAccessEgress;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandler;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;

/**
 * @author droeder
 *
 */
class WagonSimTransitStopHandler implements TransitStopHandler{
	private static final Logger log = Logger
			.getLogger(WagonSimTransitStopHandler.class);


	private VehicleLoad vehicleLoad;
	private ObjectAttributes wagonAttribs;
	private ObjectAttributes locomotiveAttribs;
	
	/**
	 * All passengers are allowed to leave the vehicle. Entering is only allowed when 
	 * free capacities are available.
	 *  
	 * @param vehicle
	 * @param vehicleLoad 
	 * @param locomitiveAttribs 
	 */
	WagonSimTransitStopHandler(Vehicle vehicle, VehicleLoad vehicleLoad, ObjectAttributes wagonAttribs, ObjectAttributes locomitiveAttribs) {
		this.vehicleLoad = vehicleLoad;
		this.wagonAttribs = wagonAttribs;
		this.locomotiveAttribs = locomitiveAttribs;
	}

	@Override
	public double handleTransitStop(TransitStopFacility stop, double now,
			List<PTPassengerAgent> leavingPassengers,
			List<PTPassengerAgent> enteringPassengers,
			PassengerAccessEgress handler, MobsimVehicle vehicle) {
		// leaving a vehicle is always allowed
		while(leavingPassengers.size() > 0){
			handler.handlePassengerLeaving(leavingPassengers.remove(0), vehicle, stop.getLinkId(), now);
		}
		// entering is allowed only when there is free capacity
		while((enteringPassengers.size()>0)){
			if(freeCapacity(stop, now, enteringPassengers, vehicle)){
				if(handler.handlePassengerEntering(enteringPassengers.get(0), vehicle, stop.getId(), now)){
					enteringPassengers.remove(0);
				}
			}else{
				// no more capacity left
				// TODO[dr] generate boarding denied events
				break;
			}
		}
//		if(enteringPassengers.size() > 0){
//			log.warn("vehicle " + vehicle.getId() + ", boarding denied for " + enteringPassengers.size());
//		}
		// duration for entering/leaving is modeled in the schedule. Thus, the duration here is always zero!
		// only when the vehicle is full pax are not allowed to enter.
		return 0;
	}

	/**
	 * @return
	 */
	private boolean freeCapacity(TransitStopFacility stop, double now,
			List<PTPassengerAgent> enteringPassengers, MobsimVehicle vehicle) {
		int i = -1;
		for(TransitRouteStop s: ((AbstractTransitDriverAgent) vehicle.getDriver()).getTransitRoute().getStops()){
			if(s.getStopFacility().getId().equals(stop.getId())){
				// this assumes each stop is served only once. I think it will not work when one stop is served twice 
				// (except for the last where only people leave).
				i = ((AbstractTransitDriverAgent) vehicle.getDriver()).getTransitRoute().getStops().indexOf(s);
				break;
			}
		}
		if(i == -1) {
			throw new IllegalArgumentException("did not find " + stop.getId() + " for vehicle " + vehicle.getId());
		}
		boolean free = WagonSimVehicleLoadListener.freeCapacityInVehicle(
				vehicle.getId().toString(), 
				stop.getId().toString(), 
				i, 
				enteringPassengers.get(0).getId().toString(), 
				vehicleLoad, 
				locomotiveAttribs, 
				wagonAttribs); 
		return free;
	}
}

