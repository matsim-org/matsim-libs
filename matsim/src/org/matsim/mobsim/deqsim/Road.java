/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.mobsim.deqsim;

import java.util.LinkedList;
import org.matsim.network.Link;

public class Road extends SimUnit {

	protected Link link;

	// see method enterRequest for a detailed description of variable 'gap'
	private LinkedList<Double> gap;

	/**
	 * all roads, which are interested in entering the road, but wasn't allowed
	 * to do so yet
	 */
	private LinkedList<Vehicle> interestedInEnteringRoad = new LinkedList<Vehicle>();
	private double timeOfLastEnteringVehicle = Double.MIN_VALUE;
	protected double timeOfLastLeavingVehicle = Double.MIN_VALUE;

	/**
	 * the inverseFlowCapacity is simply the inverse of the respective
	 * capacities meaning, and corresponds to the minimal time between two cars
	 * entering/leaving the road
	 */
	private double inverseInFlowCapacity = 0;
	protected double inverseOutFlowCapacity = 0;

	/**
	 * this variable keeps track of the number of cars, which are not on the
	 * road, but which have been promised to enter the road (given a time in
	 * future, when they can enter the road)
	 */
	protected int noOfCarsPromisedToEnterRoad = 0;

	// maximum number of cars on the road at one time
	private long maxNumberOfCarsOnRoad = 0;

	// the time it takes for a gap to get to the back of the road
	private double gapTravelTime = 0;

	// the cars, which are currently on the road
	protected LinkedList<Vehicle> carsOnTheRoad = new LinkedList<Vehicle>();
	/**
	 * for each of the cars in carsOnTheRoad, the earliest departure time from
	 * road is written here
	 */
	protected LinkedList<Double> earliestDepartureTimeOfCar = new LinkedList<Double>();
	/**
	 * when trying to enter a road, a deadlock prevention message is put into
	 * the queue this allows a car to enter the road, even if no space on it
	 */
	private LinkedList<DeadlockPreventionMessage> deadlockPreventionMessages = new LinkedList<DeadlockPreventionMessage>();

	public Road(Scheduler scheduler, Link link) {
		super(scheduler);
		this.link = link;

		/*
		 * calculate the maximum number of cars, which can be on the road at the
		 * same time
		 */
		maxNumberOfCarsOnRoad = Math.round(link.getLength()
				* link.getLanesAsInt(SimulationParameters.linkCapacityPeriod)
				* SimulationParameters.storageCapacityFactor / SimulationParameters.carSize);

		/**
		 * it is assured here, that a road must have the space of at least one
		 * car
		 */
		if (maxNumberOfCarsOnRoad == 0) {
			maxNumberOfCarsOnRoad = 1;
		}

		double maxInverseInFlowCapacity = 3600 / (SimulationParameters.minimumInFlowCapacity
				* SimulationParameters.flowCapacityFactor * link
				.getLanesAsInt(SimulationParameters.linkCapacityPeriod));

		inverseOutFlowCapacity = 1 / (link.getFlowCapacity(SimulationParameters.linkCapacityPeriod) * SimulationParameters.flowCapacityFactor);

		if (inverseOutFlowCapacity > maxInverseInFlowCapacity) {
			inverseInFlowCapacity = maxInverseInFlowCapacity;
		} else {
			inverseInFlowCapacity = inverseOutFlowCapacity;
		}

		gapTravelTime = link.getLength() / SimulationParameters.gapTravelSpeed;

		// gap must be initialized to null because of the application logic
		gap = null;
	}

	public void leaveRoad(Vehicle vehicle, double simTime) {
		assert (carsOnTheRoad.getFirst() == vehicle);

		carsOnTheRoad.removeFirst();
		earliestDepartureTimeOfCar.removeFirst();
		timeOfLastLeavingVehicle = simTime;

		/*
		 * the next car waiting for entering the road should now be alloted a
		 * time for entering the road
		 */
		if (interestedInEnteringRoad.size() > 0) {
			Vehicle nextVehicle = interestedInEnteringRoad.removeFirst();
			DeadlockPreventionMessage m = deadlockPreventionMessages.removeFirst();
			assert (m.vehicle == nextVehicle);
			scheduler.unschedule(m);

			double nextAvailableTimeForEnteringStreet = Math.max(timeOfLastEnteringVehicle
					+ inverseInFlowCapacity, simTime + gapTravelTime);

			noOfCarsPromisedToEnterRoad++;

			nextVehicle.scheduleEnterRoadMessage(nextAvailableTimeForEnteringStreet, this);
		} else {
			if (gap != null) {

				/*
				 * as long as the road is not full once, there is no need to
				 * keep track of the gaps
				 */
				gap.add(simTime + gapTravelTime);

				/*
				 * if no one is interested in entering this road (precondition)
				 * and there are no cars on the road, then reset gap (this is
				 * required, for enterRequest to function properly)
				 */
				if (carsOnTheRoad.size() == 0) {
					gap = null;
				}
			}
		}

		/*
		 * tell the car behind the fist car (which is the first car now), when
		 * it reaches the end of the read
		 */
		if (carsOnTheRoad.size() > 0) {
			Vehicle nextVehicle = carsOnTheRoad.getFirst();
			double nextAvailableTimeForLeavingStreet = Math.max(earliestDepartureTimeOfCar.getFirst(),
					timeOfLastLeavingVehicle + inverseOutFlowCapacity);
			nextVehicle.scheduleEndRoadMessage(nextAvailableTimeForLeavingStreet, this);
		}

	}

	public void enterRoad(Vehicle vehicle, double simTime) {
		// calculate time, when the car reaches the end of the road
		double nextAvailableTimeForLeavingStreet = Double.MIN_VALUE;
		nextAvailableTimeForLeavingStreet = simTime + link.getLength()
				/ link.getFreespeed(SimulationParameters.linkCapacityPeriod);

		noOfCarsPromisedToEnterRoad--;
		carsOnTheRoad.add(vehicle);

		/*
		 * needed to remove the following assertion because for deadlock
		 * prevention there might be more cars on the road than its capacity
		 * assert maxNumberOfCarsOnRoad >= carsOnTheRoad.size() : "There are
		 * more cars on the road, than its capacity!";
		 */
		earliestDepartureTimeOfCar.add(nextAvailableTimeForLeavingStreet);

		/*
		 * if we are in the front of the queue, then we can just drive with free
		 * speed to the front and have to have at least inverseFlowCapacity
		 * time-distance to the previous car
		 */
		if (carsOnTheRoad.size() == 1) {
			nextAvailableTimeForLeavingStreet = Math.max(nextAvailableTimeForLeavingStreet,
					timeOfLastLeavingVehicle + inverseOutFlowCapacity);
			vehicle.scheduleEndRoadMessage(nextAvailableTimeForLeavingStreet, this);
		} else {
			/*
			 * this car is not the front car in the street queue when the cars
			 * infront of the current car leave the street and this car becomes
			 * the front car, it will be waken up.
			 */
		}

	}

	public void enterRequest(Vehicle vehicle, double simTime) {
		double nextAvailableTimeForEnteringStreet = Double.MIN_VALUE;

		/*
		 * assert maxNumberOfCarsOnRoad >= carsOnTheRoad.size() : "There are
		 * more cars on the road, than its capacity!"; assert
		 * maxNumberOfCarsOnRoad >= carsOnTheRoad.size() +
		 * noOfCarsPromisedToEnterRoad : "You promised too many cars, that they
		 * can enter the street!"; These asserts has been commented out for
		 * deadlock prevention: If a car waits too long, it is allowed to enter
		 * the road.
		 */

		// is there any space on the road (including promised entries?)
		if (carsOnTheRoad.size() + noOfCarsPromisedToEnterRoad < maxNumberOfCarsOnRoad) {
			/*
			 * - check, if the gap needs to be considered for entering the road -
			 * we can find out, the time since when we have a free road for
			 * entrance for sure:
			 * 
			 */

			// the gap queue will only be empty in the beginning
			double arrivalTimeOfGap = Double.MIN_VALUE;
			// if the road has been full recently then find out, when the next
			// gap arrives
			if (gap != null && gap.size() > 0) {
				arrivalTimeOfGap = gap.remove();
			}

			noOfCarsPromisedToEnterRoad++;
			nextAvailableTimeForEnteringStreet = Math.max(Math.max(timeOfLastEnteringVehicle
					+ inverseInFlowCapacity, simTime), arrivalTimeOfGap);

			timeOfLastEnteringVehicle = nextAvailableTimeForEnteringStreet;
			vehicle.scheduleEnterRoadMessage(nextAvailableTimeForEnteringStreet, this);
		} else {
			/*
			 * - if the road was empty then create a new queue else empty the
			 * old queue As long as the gap is null, the road is not full (and
			 * there is no reason to keep track of the gaps => see leaveRoad)
			 * But when the road gets full once, we need to start keeping track
			 * of the gaps Once the road is empty again, gap is reset to null
			 * (see leaveRoad).
			 * 
			 * The gap variable in only needed for the situation, where the
			 * street has been full recently, but the interestedInEnteringRoad
			 * is empty and a new car arrives (or a few). So, if the street is
			 * long, it takes time for the gap to come back.
			 * 
			 * As long as interestedInEnteringRoad is not empty, newly generated
			 * gaps get used by the new cars (see leaveRoad)
			 */
			if (gap == null) {
				gap = new LinkedList<Double>();
			} else {
				gap.clear();
			}

			interestedInEnteringRoad.add(vehicle);

			/*
			 * the first car interested in entering a road has to wait
			 * 'stuckTime' the car behind has to wait an additional stuckTime
			 * (this logic was adapted to adhere to the C++ implementation)
			 */
			if (deadlockPreventionMessages.size() > 0) {
				deadlockPreventionMessages.add(vehicle.scheduleDeadlockPreventionMessage(
						deadlockPreventionMessages.getLast().getMessageArrivalTime()
								+ SimulationParameters.stuckTime, this));

			} else {
				deadlockPreventionMessages.add(vehicle.scheduleDeadlockPreventionMessage(simTime
						+ SimulationParameters.stuckTime, this));
			}

		}
	}

	public void giveBackPromisedSpaceToRoad() {
		noOfCarsPromisedToEnterRoad--;
	}

	public void incrementPromisedToEnterRoad() {
		noOfCarsPromisedToEnterRoad++;
	}

	public Link getLink() {
		return link;
	}

	public void setTimeOfLastEnteringVehicle(double timeOfLastEnteringVehicle) {
		this.timeOfLastEnteringVehicle = timeOfLastEnteringVehicle;
	}

	public void removeFirstDeadlockPreventionMessage(DeadlockPreventionMessage dpMessage) {

		assert (deadlockPreventionMessages.getFirst() == dpMessage) : "Inconsitency in logic!!! => this should only be invoked from the handler of this message";
		deadlockPreventionMessages.removeFirst();
	}

	public void removeFromInterestedInEnteringRoad() {
		interestedInEnteringRoad.removeFirst();
	}

	public static Road getRoad(String linkId) {
		return SimulationParameters.allRoads.get(linkId);
	}

}
