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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;

/**
 * 
 * Please read the docu AbstractQLane and QLinkImpl jointly. kai, nov'11
 * 
 * 
 * @author nagel
 *
 */
abstract class QLinkInternalI extends AbstractQLane implements NetsimLink {
	
	abstract boolean doSimStep(double now);

	abstract QNode getToNode() ;

	/**
	 * add vehicle at "activity" location
	 */
	abstract void addParkedVehicle(MobsimVehicle vehicle) ;
	
	/**
	 * remove vehicle from "activity" location
	 */
	abstract QVehicle removeParkedVehicle(Id vehicleId) ;

	/**
	 * returns the vehicle if it is parked at the link
	 */
	abstract QVehicle getParkedVehicle(Id vehicleId) ; 
	
	/**
	 * if you want an agent visualized while he/she is computationally not on the link, register him/her here
	 * (has --hopefully-- no effect on dynamics)
	 */
	abstract void registerAdditionalAgentOnLink(MobsimAgent planAgent) ;

	/**
	 * inverse of "registerAdditionalAgentOnlyLink"
	 */
	abstract MobsimAgent unregisterAdditionalAgentOnLink(Id mobsimAgentId) ;

	/**
	 * return all agents/vehicles/... that are NOT in traffic.
	 * Probably only used for visualization, but no guarantee.
	 */
	abstract Collection<MobsimAgent> getAdditionalAgentsOnLink() ;
	
	/**
	 * at simulation end for shutdown.
	 */
	abstract void clearVehicles() ;

	/**
	 * Agent that ends a leg or an activity is computationally passed to the QSim.  If the next PlanElement is a leg,
	 * and the leg is treated by _this_ NetsimEngine, then the QSim passes it to the NetsimEngine, which inserts it here.
	 */
//	abstract void letAgentDepartWithVehicle(MobsimDriverAgent agent, QVehicle vehicle, double now) ;
	abstract void letVehicleDepart(QVehicle vehicle, double now) ;

	abstract boolean insertPassengerIntoVehicle(MobsimAgent passenger, Id vehicleId, double now);
	
	abstract QVehicle getVehicle(Id vehicleId) ;
	
	/**
	 * this is for driver agents who want to depart but their car is not (yet) there.  Subject to design change.
	 */
	abstract void registerDriverAgentWaitingForCar(MobsimDriverAgent agent) ;
	
	/**
	 * this is for driver agents who want to depart but not all passengers are (yet) there.  Subject to design change.
	 */
	abstract void registerDriverAgentWaitingForPassengers(MobsimDriverAgent agent) ;
	abstract MobsimAgent unregisterDriverAgentWaitingForPassengers(Id agentId) ;
	
	/**
	 * this is for passenger agents who want to depart but their car is not (yet) there.  Subject to design change.
	 * TODO: create something like a PassengerAgent which knows the vehicle it is waiting for. 
	 */
	abstract void registerPassengerAgentWaitingForCar(MobsimAgent agent, Id vehicleId) ;
	abstract MobsimAgent unregisterPassengerAgentWaitingForCar(MobsimAgent agent, Id vehicleId) ;
	abstract Set<MobsimAgent> getAgentsWaitingForCar(Id vehicleId) ;
	
}
