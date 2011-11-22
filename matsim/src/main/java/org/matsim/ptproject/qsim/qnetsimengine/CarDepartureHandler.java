/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.qnetsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.ptproject.qsim.agents.PersonDriverAgentImpl;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;

class CarDepartureHandler implements DepartureHandler {

	/*
	 * What to do when some agent wants to drive their vehicle but it isn't there.
	 * Either teleport it to the agent's location (no matter what!), or have the agent wait
	 * for the car to arrive, or throw an exception.
	 */
	public static enum VehicleBehavior { TELEPORT, WAIT_UNTIL_IT_COMES_ALONG, EXCEPTION };
	
	private static Logger log = Logger.getLogger(CarDepartureHandler.class);

	private int cntTeleportVehicle = 0;

	private VehicleBehavior vehicleBehavior;

	private QNetsimEngine qNetsimEngine;
	
	CarDepartureHandler(QNetsimEngine qNetsimEngine, VehicleBehavior vehicleBehavior) {
		this.qNetsimEngine = qNetsimEngine;
		this.vehicleBehavior = vehicleBehavior;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		if (agent.getMode().equals(TransportMode.car)) {
			if ( agent instanceof MobsimDriverAgent ) {
				handleCarDeparture(now, (MobsimDriverAgent)agent, linkId);
				return true ;
			} else {
				throw new UnsupportedOperationException("wrong agent type to use a car") ;
			}
		}
		return false ;
	}

	private void handleCarDeparture(double now, MobsimDriverAgent agent, Id linkId) {
//		NetworkRoute route = (NetworkRoute) leg.getRoute();
//		Id vehicleId = route.getVehicleId();
		Id vehicleId = agent.getPlannedVehicleId() ;
		if (vehicleId == null) {
			vehicleId = agent.getId(); // backwards-compatibility
		}
		AbstractQLink qlink = (AbstractQLink) qNetsimEngine.getNetsimNetwork().getNetsimLink(linkId);
		QVehicle vehicle = qlink.removeParkedVehicle(vehicleId);
		if (vehicle == null) {
			if (vehicleBehavior == VehicleBehavior.TELEPORT && agent instanceof PersonDriverAgentImpl) {
				vehicle = qNetsimEngine.getVehicles().get(vehicleId);
				teleportVehicleTo(vehicle, linkId);
				qlink.letAgentDepartWithVehicle(agent, vehicle, now);
				// (since the "teleportVehicle" does not physically move the vehicle, this is finally achieved in the departure
				// logic.  kai, nov'11)
			} else if (vehicleBehavior == VehicleBehavior.WAIT_UNTIL_IT_COMES_ALONG) {
				// While we are waiting for our car
				qlink.registerAdditionalAgentOnLink(agent);
			} else {
				throw new RuntimeException("vehicle not available for agent " + agent.getId() + " on link " + linkId);
			}
		} else {
			qlink.letAgentDepartWithVehicle(agent, vehicle, now);
		}
	}

	/**
	 * Design thoughs:<ul>
	 * <li> yyyyyy It is not completely clear what happens when the vehicle is used by someone else. kai, nov'11
	 * <li> Seems to me that a parked vehicle is teleported. kai, nov'11
	 * <li> yyyyyy Seems to me that a non-parked vehicle will end up with two references to it, with race conditions???? kai, nov11
	 * <li> yyyyyy Note that the "linkId" parameter is not used for any physical action!!
	 * </ul> 
	 */
	private void teleportVehicleTo(QVehicle vehicle, Id linkId) {
		if (vehicle.getCurrentLink() != null) {
			if (cntTeleportVehicle < 9) {
				cntTeleportVehicle++;
				log.info("teleport vehicle " + vehicle.getId() + " from link " + vehicle.getCurrentLink().getId() + " to link " + linkId);
				if (cntTeleportVehicle == 9) {
					log.info("No more occurrences of teleported vehicles will be reported.");
				}
			}
			AbstractQLink qlinkOld = (AbstractQLink) qNetsimEngine.getNetsimNetwork().getNetsimLink(vehicle.getCurrentLink().getId());
			qlinkOld.removeParkedVehicle(vehicle.getId());
		}
	}

}
