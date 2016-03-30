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
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.TimeVariantLink;
import org.matsim.vehicles.Vehicle;

/**
 * Essentially an interface, but since I do not want the methods public for the time being, it is incarnated as an abstract class.
 * <p/>
 * Contains all the logic for the QLinks which make up the QNetwork.
 * <p/>
 * One can argue that the QNetsimEngine could treat more general links than just "queues".  However, in the end they live with 
 * "(qlane.)addFromUpstream" and "(qlane.)addFromUpstream", which is quite restricted, since it does not pass the velocity.
 * Not passing the velocity means that it cannot be used for more general mobsims.
 * 
 * @author nagel
 *
 */
abstract class QLinkI implements NetsimLink, TimeVariantLink {
	// yyyy might make make sense to also pass the "isAccepting/addFromUpstream" through something like 
	// "getFromNodeQueueLanes". kai, feb'16
	
	// yyyy my intuition is to unify all the "registerDriverAgentForCar" etc. methods into something like
	// registerAdditionalAgentOnLink( MobsimAgent agent, String reason ) ;
	

	abstract QNode getToNode() ;

	/**
	 * add vehicle at "activity" location
	 * <br>
	 * Seems ok as public interface function. 
	 * Rename to "addToParking(...)".  kai, aug'15
	 */
	abstract void addParkedVehicle(MobsimVehicle vehicle) ;
	
	/**
	 * remove vehicle from "activity" location
	 */
	abstract QVehicle removeParkedVehicle(Id<Vehicle> vehicleId) ;

	/**
	 * returns the vehicle if it is parked at the link
	 */
	abstract QVehicle getParkedVehicle(Id<Vehicle> vehicleId) ; 
	
	/**
	 * if you want an agent visualized while he/she is computationally not on the link, register him/her here
	 * (has --hopefully-- no effect on dynamics)
	 */
	abstract void registerAdditionalAgentOnLink(MobsimAgent planAgent) ;

	/**
	 * inverse of "registerAdditionalAgentOnlyLink"
	 */
	abstract MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> mobsimAgentId) ;

	/**
	 * return all agents/vehicles/... that are NOT in traffic.
	 * Probably only used for visualization, but no guarantee.
	 * <br>
	 * Seems ok as public interface function. kai, aug'15
	 */
	abstract Collection<MobsimAgent> getAdditionalAgentsOnLink() ;
	
	/**
	 * Agent that ends a leg or an activity is computationally passed to the QSim.  If the next PlanElement is a leg,
	 * and the leg is treated by _this_ NetsimEngine, then the QSim passes it to the NetsimEngine, which inserts it here.
	 */
	abstract void letVehicleDepart(QVehicle vehicle, double now) ;

	abstract boolean insertPassengerIntoVehicle(MobsimAgent passenger, Id<Vehicle> vehicleId, double now);
	
	abstract QVehicle getVehicle(Id<Vehicle> vehicleId) ;
	
	/**
	 * this is for driver agents who want to depart but their car is not (yet) there.  Subject to design change.
	 */
	abstract void registerDriverAgentWaitingForCar(final MobsimDriverAgent agent) ;
	
	/**
	 * this is for driver agents who want to depart but not all passengers are (yet) there.  Subject to design change.
	 */
	abstract void registerDriverAgentWaitingForPassengers(MobsimDriverAgent agent) ;
	abstract MobsimAgent unregisterDriverAgentWaitingForPassengers(Id<Person> agentId) ;
	
	/**
	 * this is for passenger agents who want to depart but their car is not (yet) there.  Subject to design change.
	 * TODO: create something like a PassengerAgent which knows the vehicle it is waiting for. 
	 */
	abstract void registerPassengerAgentWaitingForCar(MobsimAgent agent, Id<Vehicle> vehicleId) ;
	abstract MobsimAgent unregisterPassengerAgentWaitingForCar(MobsimAgent agent, Id<Vehicle> vehicleId) ;

	
	/**
	 * yy Can't we get this functionality from {@link #getAdditionalAgentsOnLink()}?
	 */
	abstract Set<MobsimAgent> getAgentsWaitingForCar(Id<Vehicle> vehicleId) ;

	abstract List<QLaneI> getOfferingQLanes() ;

	/**
	 * Seems ok as public interface function. kai, aug'15
	 */
	abstract boolean doSimStep();

	/**
	 * Seems ok as public interface function. kai, aug'15 
	 */
	abstract void clearVehicles();

	abstract boolean isNotOfferingVehicle();

	abstract QLaneI getAcceptingQLane() ;

}
