/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerQNetsimEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

/*
 * This has to extend the QNetsimEngine since the letVehicleArrive(...)
 * method is not part of an Interface.
 */
public class PassengerQNetsimEngine extends QNetsimEngine {
	
//	private static final Logger log = Logger.getLogger(PassengerQNetsimEngine.class);
	
	
	/**
	 * Mode of passengers who are physically simulated.
	 */
	public static final String PASSENGER_TRANSPORT_MODE = "ride_passenger";
	
	/**
	 * Pickup and dropoff activity types.
	 */
	public static final String PICKUP_ACTIVITY_TYPE = "pickup";
	public static final String DROP_OFF_ACTIVITY_TYPE = "dropoff";
	
	private final DepartureHandler departureHandler;
	
	public PassengerQNetsimEngine(final QSim sim, final Random random,
			JointDepartureOrganizer jointDepartureOrganizer) {
		super(sim);
		this.departureHandler = new PassengerDepartureHandler(this, jointDepartureOrganizer);
	}
	
	@Override
	public DepartureHandler getDepartureHandler() {
		return departureHandler;
	}
	
	public DepartureHandler getVehicularDepartureHandler() {
		return super.getDepartureHandler();
	}
	
	@Override
	void letVehicleArrive(QVehicle veh) {
		
		/*
		 * The driver's arrival logic is located in the super-class.
		 */
		super.letVehicleArrive(veh);
		
		/*
		 * Check whether also some passengers have reached their destination link.
		 */
		double now = this.getMobsim().getSimTimer().getTimeOfDay();
		EventsManager eventsManager = this.getMobsim().getEventsManager();
		List<PassengerAgent> passengers = new ArrayList<PassengerAgent>(veh.getPassengers());
		for (PassengerAgent passenger : passengers) {
			
			MobsimAgent mobsimAgent = (MobsimAgent) passenger;
			
			if (passenger.getDestinationLinkId().equals(veh.getCurrentLink().getId())) {
				
				// remove passenger from vehicle and teleport it to the vehicle's position
				veh.removePassenger(passenger);
				passenger.setVehicle(null);
				mobsimAgent.notifyArrivalOnLinkByNonNetworkMode(veh.getCurrentLink().getId());
				
				eventsManager.processEvent(new PersonLeavesVehicleEvent(now, mobsimAgent.getId(), veh.getId()));
				mobsimAgent.endLegAndComputeNextState(now);
				this.internalInterface.arrangeNextAgentState(mobsimAgent);				
			}
			/*
			 * Check for all agents who stay in the vehicle whether they have scheduled a joint 
			 * departure for the vehicles next leg. If yes, the PassengerDepartureHandler
			 * marks them as waiting for the other agents of the joint departure.
			 */
			else {
				// Probably this needs to be synchronized since vehicles may arrive concurrently in the ParallelQSim.
				synchronized (this.departureHandler) {
					this.departureHandler.handleDeparture(now, mobsimAgent, veh.getCurrentLink().getId());					
				}
				// This is not necessarily an error...
//				log.info("Passenger " + passenger.getId().toString() + " waits in vehicle " + veh.getId() + 
//						" on link " + veh.getCurrentLink().getId() + ".");
			}		
		}
	}
}