/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerDepartureHandler.java
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

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

public class PassengerDepartureHandler implements DepartureHandler {

	private static Logger log = Logger.getLogger(PassengerDepartureHandler.class);

	public static String driverMode = TransportMode.car;
	public static String passengerMode = TransportMode.ride;
	
	/*
	 * TODO: Think about...
	 * - How and where to schedule a joint departure?
	 * - Where to insert Agents into vehicle?
	 * - Define number of passengers or Id-list of passengers?
	 */
	
	private final QNetsimEngine qNetsimEngine;
	private final JointDepartureOrganizer jointDepartureOrganizer;

	public PassengerDepartureHandler(QNetsimEngine qNetsimEngine,
			JointDepartureOrganizer jointDepartureOrganizer) {
		this.qNetsimEngine = qNetsimEngine;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		
		String mode = agent.getMode();
		if (!mode.equals(driverMode) && !mode.equals(passengerMode)) return false;
		
		JointDeparture jointDeparture = getJointDeparture(agent.getId());
		if (jointDeparture == null) return false;
				
		QLinkInternalI qlink = (QLinkInternalI) qNetsimEngine.getNetsimNetwork().getNetsimLink(linkId);
		QVehicle vehicle = qlink.getParkedVehicle(jointDeparture.getVehicleId());
		
		if (driverMode.equals(agent.getMode())) {
			handleDriverDeparture(jointDeparture, vehicle, qlink, (MobsimDriverAgent) agent, now);
		} else if (passengerMode.equals(agent.getMode())) {
			handlePassengerDeparture(jointDeparture, vehicle, qlink, agent, now);
		} else return false;
		
		if (canDepart(jointDeparture, vehicle, qlink)) {
			checkDeparture(jointDeparture, vehicle);	// throws an exception if something seems to be wrong
			jointDeparture.setDeparted();	// departure has been performed, therefore invalidate it
			handleJointDeparture(now, vehicle, qlink);
		}
		
		return true;
	}
	
	private JointDeparture getJointDeparture(Id agentId) {
		List<JointDeparture> jointDepartures = jointDepartureOrganizer.getJointDepartures(agentId);
		if (jointDepartures == null) return null;
		
		/*
		 * Return the first jointDeparture from the list which has not been processed.
		 */
		JointDeparture jointDeparture;
		while (jointDepartures.size() > 0) {
			jointDeparture = jointDepartures.remove(0);
			if (jointDeparture.isDeparted()) {
				log.warn("Seems that agent " + agentId + 
						" has missed departure " + jointDeparture.getId().toString() + 
						" with vehicle " + jointDeparture.getVehicleId().toString() +
						" on link " + jointDeparture.getLinkId().toString());
			}
			else return jointDeparture;
		}
		// No valid joint departure was found!
		return null;
	}
	
	private void handleDriverDeparture(JointDeparture jointDeparture, QVehicle vehicle,
			QLinkInternalI qlink, MobsimDriverAgent driver, double now) {
		
		Id vehicleId = driver.getPlannedVehicleId();
		
		if (!vehicleId.equals(jointDeparture.getVehicleId())) {
			throw new RuntimeException("The planned vehicle " + vehicleId.toString() +
					" of driver " + driver.getId().toString() + 
					" does not match the vehicle scheduled in the joint departure " +
					jointDeparture.getVehicleId().toString() + "!");
		}
				
		// check whether the driver has to wait for passengers
		if (!allPassengersWaiting(jointDeparture, vehicle, qlink)) {
			qlink.registerDriverAgentWaitingForPassengers(driver);
		}
		
		// TODO: implement vehicle behavior as in VehicularDepartureHandler?
		// if the vehicle is not yet there
		if (vehicle == null) {
			qlink.registerDriverAgentWaitingForCar(driver);
//			qlink.registerPassengerAgentWaitingForCar(driver, vehicleId);
		}
		else {
			vehicle.setDriver(driver);
		}	
	}
	
	private void handlePassengerDeparture(JointDeparture jointDeparture, QVehicle vehicle, 
			QLinkInternalI qlink, MobsimAgent passenger, double now) {

		/*
		 * Check whether the agent is already passenger in the vehicle, e.g.
		 * because the vehicle has stopped to pick up or drop off another agent.
		 * If not, insert the agent into the vehicle. 
		 */
		if (((PassengerAgent) passenger).getVehicle() == null) {
			/*
			 * The qlink checks whether the vehicle is already available for the 
			 * passenger to enter. If not, the agent is added to a waiting list.
			 */
			qlink.insertPassengerIntoVehicle(passenger, jointDeparture.getVehicleId(), now);
		}
		
		/*
		 * If all passengers are now waiting for the driver and / or the vehicle,
		 * the driver has no longer to be marked as "waiting for passengers".
		 * It is unmarked and insert as driver into the vehicle, if the later
		 * is already available.
		 */
		if (allPassengersWaiting(jointDeparture, vehicle, qlink)) {
			MobsimAgent driver = qlink.unregisterDriverAgentWaitingForPassengers(jointDeparture.getDriverId());
			if (driver != null && vehicle != null) vehicle.setDriver((MobsimDriverAgent) driver);
		}
	}
	
	private boolean allPassengersWaiting(JointDeparture jointDeparture, QVehicle vehicle,
			QLinkInternalI qlink) {
		
		int waitingOnLink = 0;
		Set<MobsimAgent> set = qlink.getAgentsWaitingForCar(jointDeparture.getVehicleId());
		if (set != null) waitingOnLink = set.size();
		
		int waitingInVehicle = 0;
		if (vehicle != null) waitingInVehicle = vehicle.getPassengers().size();
		
		return (waitingInVehicle + waitingOnLink) == jointDeparture.getPassengerIds().size();
	}
	
	private boolean canDepart(JointDeparture jointDeparture, QVehicle vehicle, QLinkInternalI qlink) {
		if (vehicle == null) return false;
		else if (vehicle.getDriver() == null) return false;
		else if (!allPassengersWaiting(jointDeparture, vehicle, qlink)) return false;
		else return true;
	}
	
	/*
	 * Check whether the driver and all passengers in the vehicle are as
	 * scheduled in the JointDeparture.
	 */
	private void checkDeparture(JointDeparture jointDeparture, QVehicle vehicle) {
		if (!vehicle.getCurrentLink().getId().equals(jointDeparture.getLinkId())) {
			throw new RuntimeException("JointDeparture " + jointDeparture.getId().toString() +
					"V ehicle " + vehicle.getId().toString() + 
					" is at link " + vehicle.getCurrentLink().getId().toString() +
					" but was scheduled to be at " + jointDeparture.getLinkId().toString() +
					"!");
		}
		if (!vehicle.getDriver().getId().equals(jointDeparture.getDriverId())) {
			throw new RuntimeException("JointDeparture " + jointDeparture.getId().toString() +
					" Vehicle " + vehicle.getId().toString() + 
					" has driver " + vehicle.getDriver().getId().toString() +
					" but agent " + jointDeparture.getDriverId().toString() +
					" was scheduled as driver!");
		}
		for (PassengerAgent passenger : vehicle.getPassengers()) {
			if (!jointDeparture.getPassengerIds().contains(passenger.getId())) {
				throw new RuntimeException("JointDeparture " + jointDeparture.getId().toString() +
						" Passenger " + passenger.getId().toString() + 
						" found in vehicle " + vehicle.getId().toString() +
						" but was not scheduled as passenger!");
			}
		}
	}
	
	private void handleJointDeparture(double now, QVehicle vehicle, QLinkInternalI qlink) {

		/*
		 * Check whether the driver's next leg ends at the current link.
		 */
		MobsimDriverAgent driver = vehicle.getDriver();
		if ( driver.getDestinationLinkId().equals(qlink.getLink().getId()) && (driver.chooseNextLinkId() == null)) {

			driver.endLegAndComputeNextState(now);
			
//			qlink.addParkedVehicle(vehicle);
			this.qNetsimEngine.internalInterface.arrangeNextAgentState(driver);
			
			/*
			 * Check for each passenger whether it has arrived at its destination link.
			 * If true, end its current leg. If not, the agents stays in the vehicle
			 * and waits until it departs again.
			 */
			for (PassengerAgent passenger : vehicle.getPassengers()) {
				MobsimAgent mobsimAgent = (MobsimAgent) passenger;
				if (mobsimAgent.getDestinationLinkId().equals(qlink.getLink().getId())) {
					mobsimAgent.endLegAndComputeNextState(now);
					this.qNetsimEngine.internalInterface.arrangeNextAgentState(mobsimAgent);
				}
			}			
		} else {
			// regular departure
			qlink.removeParkedVehicle(vehicle.getId());
			qlink.letVehicleDepart(vehicle, now);
		}
		
		// old behavior
//		qlink.removeParkedVehicle(vehicle.getId());
//		qlink.letVehicleDepart(vehicle, now);
	}
	
}
