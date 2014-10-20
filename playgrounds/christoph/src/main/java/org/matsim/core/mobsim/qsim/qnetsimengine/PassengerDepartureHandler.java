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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.vehicles.Vehicle;

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
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		
		String mode = agent.getMode();
		if (mode == null) {
			log.error("LegMode of agent " + agent.getId().toString() + " is null!");
		}
		if (driverMode.equals(mode)) {
			PlanElement planElement = ((PlanAgent) agent).getCurrentPlanElement();
			Leg leg = (Leg) planElement;
			JointDeparture jointDeparture = this.jointDepartureOrganizer.removeJointDepartureForLeg(agent.getId(), leg);
			if (jointDeparture == null) return false;
			
			QLinkInternalI qlink = (QLinkInternalI) qNetsimEngine.getNetsimNetwork().getNetsimLink(linkId);
			QVehicle vehicle = qlink.getParkedVehicle(jointDeparture.getVehicleId());
			
			handleDriverDeparture(jointDeparture, vehicle, qlink, (MobsimDriverAgent) agent, now);
			return true;
		} else if (passengerMode.equals(mode)) {
			PlanElement planElement = ((PlanAgent) agent).getCurrentPlanElement();
			Leg leg = (Leg) planElement;
			JointDeparture jointDeparture = this.jointDepartureOrganizer.removeJointDepartureForLeg(agent.getId(), leg);
			if (jointDeparture == null) return false;

			// this is necessary since it is not performed in the PersonDriverAgentImpl so far...
			((PassengerAgent) agent).setVehicle(null);
			
			QLinkInternalI qlink = (QLinkInternalI) qNetsimEngine.getNetsimNetwork().getNetsimLink(linkId);
			QVehicle vehicle = qlink.getParkedVehicle(jointDeparture.getVehicleId());
			
			handlePassengerDeparture(jointDeparture, vehicle, qlink, agent, leg, now);				
			
			return true;
		} else return false;
	}
	
	private void handleDriverDeparture(JointDeparture jointDeparture, QVehicle vehicle,
			QLinkInternalI qlink, MobsimDriverAgent driver, double now) {
		boolean canDepart = true;
		Id<Vehicle> vehicleId = driver.getPlannedVehicleId();
		
		if (!vehicleId.equals(jointDeparture.getVehicleId())) {
			throw new RuntimeException("The planned vehicle " + vehicleId.toString() +
					" of driver " + driver.getId().toString() + 
					" does not match the vehicle scheduled in the joint departure " +
					jointDeparture.getVehicleId().toString() + "!");
		}
		
		// check whether the driver has to wait for passengers
		if (!allPassengersWaiting(jointDeparture, vehicle, qlink)) {
			qlink.registerDriverAgentWaitingForPassengers(driver);
			canDepart = false;
		}
		
		// TODO: implement vehicle behavior as in VehicularDepartureHandler?
		// if the vehicle is not yet there
		if (vehicle == null) {
			qlink.registerDriverAgentWaitingForCar(driver);
			canDepart = false;
		} else {
			vehicle.setDriver(driver);
			driver.setVehicle(vehicle);
		}
		
		if (canDepart) {
			handleJointDeparture(jointDeparture, vehicle, qlink, now);
		}
	}
	
	private void handlePassengerDeparture(JointDeparture jointDeparture, QVehicle vehicle, 
			QLinkInternalI qlink, MobsimAgent passenger, Leg leg, double now) {	
		/*
		 * Check whether the agent is already passenger in the vehicle, e.g.
		 * because the vehicle has stopped to pick up or drop off another agent.
		 * If not, insert the agent into the vehicle. 
		 */
		if (((PassengerAgent) passenger).getVehicle() == null) {
			
//			// if the agent does not leave the vehicle immediately on the current link, insert it into the vehicle 
//			if (!leg.getRoute().getStartLinkId().equals(leg.getRoute().getEndLinkId())) {
				/*
				 * The qlink checks whether the vehicle is already available for the 
				 * passenger to enter. If not, the agent is added to a waiting list.
				 */
				boolean inserted = qlink.insertPassengerIntoVehicle(passenger, jointDeparture.getVehicleId(), now);
				
				if (!inserted && vehicle != null) {
					throw new RuntimeException("Passenger " + passenger.getId().toString() + 
							" could not be inserted into vehicle " + jointDeparture.getVehicleId().toString() +
							"! Probably there is no free seat left!");
				}
//			}
		}
		
		/*
		 * If all passengers are now waiting for the driver and / or the vehicle,
		 * the driver has no longer to be marked as "waiting for passengers".
		 * It is unmarked and insert as driver into the vehicle, if the later
		 * is already available.
		 */
		if (allPassengersWaiting(jointDeparture, vehicle, qlink)) {
			if (vehicle != null) {
				MobsimAgent driver = qlink.unregisterDriverAgentWaitingForPassengers(jointDeparture.getDriverId());
				if (driver != null) {
					/*
					 * Joint Departure can be performed since
					 * - all passengers are available
					 * - driver is available
					 * - vehicle is available
					 */
					vehicle.setDriver((MobsimDriverAgent) driver);
					((MobsimDriverAgent) driver).setVehicle(vehicle);
					handleJointDeparture(jointDeparture, vehicle, qlink, now);
				}
			} else return;
			
		} else return;
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
	
//	private boolean canDepart(JointDeparture jointDeparture, QVehicle vehicle, QLinkInternalI qlink) {
//		if (vehicle == null) return false;
//		else if (vehicle.getDriver() == null) return false;
//		else if (!allPassengersWaiting(jointDeparture, vehicle, qlink)) return false;
//		else return true;
//	}
	
	/*
	 * Check whether the driver and all passengers in the vehicle are as
	 * scheduled in the JointDeparture.
	 */
	private void checkDeparture(JointDeparture jointDeparture, QVehicle vehicle) {
		if (!vehicle.getCurrentLink().getId().equals(jointDeparture.getLinkId())) {
			throw new RuntimeException("JointDeparture " + jointDeparture.getId().toString() +
					" Vehicle " + vehicle.getId().toString() + 
					" with driver " + vehicle.getDriver().getId().toString() + 
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
	
	private void handleJointDeparture(JointDeparture jointDeparture, QVehicle vehicle, QLinkInternalI qlink, double now) {

		// throws an exception if something seems to be wrong
		checkDeparture(jointDeparture, vehicle);
		
		/*
		 * Departure will be performed therefore invalidate it to
		 * ensure that it is not performed a second time. 
		 */
		jointDeparture.setDeparted();

		/*
		 * Remove joint departure from set of scheduled but not yet processed
		 * departures. For debugging, remaining entries in the set are written 
		 * to a file after the simulation has ended.
		 */
		this.jointDepartureOrganizer.removeHandledJointDeparture(jointDeparture);
		
		JointDepartureEvent event = new JointDepartureEvent(now, jointDeparture.getId(), jointDeparture.getLinkId(),
				jointDeparture.getDriverId(), jointDeparture.getVehicleId(), jointDeparture.getPassengerIds());
		qNetsimEngine.getMobsim().getEventsManager().processEvent(event);
		
		
		/*
		 * Check whether the driver's next leg ends at the current link.
		 */
		MobsimDriverAgent driver = vehicle.getDriver();
		if (driver.getDestinationLinkId().equals(qlink.getLink().getId()) && (driver.chooseNextLinkId() == null)) {

			driver.endLegAndComputeNextState(now);
			
			/*
			 * With commit 25974, agents can end activities immediately after starting them,
			 * resulting in a activity duration of 0 seconds. For such activities,
			 * agent.endActivityAndComputeNextState(...) is called from ActivityEngine.handleActivity(...),
			 * and not anymore from ActivityEngine.doSimStep(...). As a result of this change,
			 * the endActivityAndComputeNextState(...) method is called earlier. Passengers have not
			 * left the car before the driver's activity is ended.
			 * Therefore, we have to allow passengers to leave the car before we can arrange the
			 * driver's next state.
			 */
//			this.qNetsimEngine.internalInterface.arrangeNextAgentState(driver);
			
			/*
			 * Check for each passenger whether it has arrived at its destination link.
			 * If true, end its current leg. If not, the agents stays in the vehicle
			 * and waits until it departs again.
			 */
			// identify leaving agents
			List<MobsimAgent> vehicleLeavingAgents = new ArrayList<MobsimAgent>();
			for (PassengerAgent passenger : vehicle.getPassengers()) {
				MobsimAgent mobsimAgent = (MobsimAgent) passenger;
				if (mobsimAgent.getDestinationLinkId().equals(qlink.getLink().getId())) {
					vehicleLeavingAgents.add(mobsimAgent);
				}
			}
			// let agents leave
			for (MobsimAgent leavingPassenger : vehicleLeavingAgents) {
				// remove passenger from vehicle and teleport it to the vehicle's position
				vehicle.removePassenger((PassengerAgent) leavingPassenger);
				((PassengerAgent) leavingPassenger).setVehicle(null);
				leavingPassenger.notifyArrivalOnLinkByNonNetworkMode(vehicle.getCurrentLink().getId());
				
				qNetsimEngine.getMobsim().getEventsManager().processEvent(
						new PersonLeavesVehicleEvent(now, leavingPassenger.getId(), vehicle.getId()));
				leavingPassenger.endLegAndComputeNextState(now);
				this.qNetsimEngine.internalInterface.arrangeNextAgentState(leavingPassenger);
			}
			
			/*
			 * Moved this line here due to changes from With commit 25974.
			 */
			this.qNetsimEngine.internalInterface.arrangeNextAgentState(driver);
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
