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

package playground.sergioo.ptsim2013.qnetsimengine;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.QSimConfigGroup.VehicleBehavior;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.Vehicle;

class PTVehicularDepartureHandler implements DepartureHandler {

	private static Logger log = Logger.getLogger(PTVehicularDepartureHandler.class);

	private int cntTeleportVehicle = 0;

	private VehicleBehavior vehicleBehavior;

	private PTQNetsimEngine qNetsimEngine;

	private Collection<String> transportModes;

	PTVehicularDepartureHandler(PTQNetsimEngine qNetsimEngine, VehicleBehavior vehicleBehavior) {
		this.qNetsimEngine = qNetsimEngine;
		this.vehicleBehavior = vehicleBehavior;
		this.transportModes = qNetsimEngine.getMobsim().getScenario().getConfig().qsim().getMainModes() ;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		if (this.transportModes.contains(agent.getMode())) {
			if ( agent instanceof MobsimDriverAgent ) {
				handleCarDeparture(now, (MobsimDriverAgent)agent, linkId);
				return true ;
			} else {
				throw new UnsupportedOperationException("wrong agent type to depart on a network mode") ;
			}
		}
		return false ;
	}

	private void handleCarDeparture(double now, MobsimDriverAgent agent, Id<Link> linkId) {
		if ( ! (agent instanceof TransitDriverAgentImpl) ) {
			// (UmlaufDriver somehow is different. kai, dec'11)
			if (linkId.equals(agent.getDestinationLinkId())) {
				if ( agent.chooseNextLinkId() == null ) {

					// no physical travel is necessary.  We still treat this as a departure and an arrival, since there is a 
					// "leg".  Some of the design allows to have successive activities without invervening legs, but this is not 
					// consistently implemented.  One could also decide to not have these departure/arrival events here
					// (we would still have actEnd/actStart events).  kai, nov'11

					agent.endLegAndComputeNextState(now) ;
					this.qNetsimEngine.internalInterface.arrangeNextAgentState(agent) ;
					return;
				}
			}
		}		
		Id<Vehicle> vehicleId = agent.getPlannedVehicleId() ;
		PTQLink qlink = (PTQLink) qNetsimEngine.getNetsimNetwork().getNetsimLink(linkId);
		QVehicle vehicle = qlink.removeParkedVehicle(vehicleId);
		if (vehicle == null) {
			if (vehicleBehavior == VehicleBehavior.teleport) {
				vehicle = qNetsimEngine.getVehicles().get(vehicleId);
				teleportVehicleTo(vehicle, linkId);
				vehicle.setDriver(agent);
				qlink.letVehicleDepart(vehicle, now);
				// (since the "teleportVehicle" does not physically move the vehicle, this is finally achieved in the departure
				// logic.  kai, nov'11)
			} else if (vehicleBehavior == VehicleBehavior.wait ) {
				// While we are waiting for our car
				qlink.registerDriverAgentWaitingForCar(agent);
			} else {
				throw new RuntimeException("vehicle not available for agent " + agent.getId() + " on link " + linkId);
			}
		} else {
			vehicle.setDriver(agent);
			qlink.letVehicleDepart(vehicle, now);
		}
	}

	/**
	 * Design thoughts:<ul>
	 * <li>  It is not completely clear what happens when the vehicle is used by someone else. kai, nov'11
	 * <li> Seems to me that a parked vehicle is teleported. kai, nov'11
	 * <li>  Seems to me that a non-parked vehicle will end up with two references to it, with race conditions???? kai, nov11
	 * <li>  Note that the "linkId" parameter is not used for any physical action!!
	 * </ul> 
	 */
	private void teleportVehicleTo(QVehicle vehicle, Id<Link> linkId) {
		if (vehicle.getCurrentLink() != null) {
			if (cntTeleportVehicle < 9) {
				cntTeleportVehicle++;
				log.info("teleport vehicle " + vehicle.getId() + " from link " + vehicle.getCurrentLink().getId() + " to link " + linkId);
				if (cntTeleportVehicle == 9) {
					log.info("No more occurrences of teleported vehicles will be reported.");
				}
			}
			PTQLink qlinkOld = (PTQLink) qNetsimEngine.getNetsimNetwork().getNetsimLink(vehicle.getCurrentLink().getId());
			qlinkOld.removeParkedVehicle(vehicle.getId());
		}
	}

}
