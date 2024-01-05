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

package org.matsim.core.mobsim.jdeqsim;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;

/**
 * The road is simulated as an active agent, moving arround vehicles.
 *
 * @author rashid_waraich
 */
public class Road extends SimUnit {

	// default
	static JDEQSimConfigGroup config = new JDEQSimConfigGroup();

	public static void setConfig(JDEQSimConfigGroup config) {
		Road.config = config;
	}

	/**
	 * this must be initialized before starting the simulation! mapping:
	 * key=linkId used to find a road corresponding to a link
	 */
	static HashMap<Id<Link>, Road> allRoads = null;

	public static HashMap<Id<Link>, Road> getAllRoads() {
		return allRoads;
	}

	public static void setAllRoads(HashMap<Id<Link>, Road> allRoads) {
		Road.allRoads = allRoads;
	}

	protected Link link;

	// see method enterRequest for a detailed description of variable 'gap'
	private LinkedList<Double> gap;

	/**
	 * all roads, which are interested in entering the road, but wasn't allowed
	 * to do so yet
	 */
	private LinkedList<Vehicle> interestedInEnteringRoad = new LinkedList<>();
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
	protected LinkedList<Vehicle> carsOnTheRoad = new LinkedList<>();
	/**
	 * for each of the cars in carsOnTheRoad, the earliest departure time from
	 * road is written here
	 */
	protected LinkedList<Double> earliestDepartureTimeOfCar = new LinkedList<>();
	/**
	 * when trying to enter a road, a deadlock prevention message is put into
	 * the queue this allows a car to enter the road, even if no space on it
	 */
	private LinkedList<DeadlockPreventionMessage> deadlockPreventionMessages = new LinkedList<>();

	public Road(Scheduler scheduler, Link link) {
		super(scheduler);
		this.link = link;

		/*
		 * calculate the maximum number of cars, which can be on the road at the
		 * same time
		 */
		this.maxNumberOfCarsOnRoad = Math.round(link.getLength()
				* NetworkUtils.getNumberOfLanesAsInt(link)
				* config.getStorageCapacityFactor() / config.getCarSize());

		/**
		 * it is assured here, that a road must have the space of at least one
		 * car
		 */
		if (this.maxNumberOfCarsOnRoad == 0) {
			this.maxNumberOfCarsOnRoad = 1;
		}

		double maxInverseInFlowCapacity = 3600 / (config.getMinimumInFlowCapacity()
				* config.getFlowCapacityFactor() * NetworkUtils.getNumberOfLanesAsInt(link));

		this.inverseOutFlowCapacity = 1 / (link.getFlowCapacityPerSec() * config.getFlowCapacityFactor());

		if (this.inverseOutFlowCapacity > maxInverseInFlowCapacity) {
			this.inverseInFlowCapacity = maxInverseInFlowCapacity;
		} else {
			this.inverseInFlowCapacity = this.inverseOutFlowCapacity;
		}

		this.gapTravelTime = link.getLength() / config.getGapTravelSpeed();

		// gap must be initialized to null because of the application logic
		this.gap = null;
	}

	public void leaveRoad(Vehicle vehicle, double simTime) {
		assert (this.carsOnTheRoad.getFirst() == vehicle);
		assert (this.interestedInEnteringRoad.size()==this.deadlockPreventionMessages.size());

		this.carsOnTheRoad.removeFirst();
		this.earliestDepartureTimeOfCar.removeFirst();
		this.timeOfLastLeavingVehicle = simTime;

		/*
		 * the next car waiting for entering the road should now be alloted a
		 * time for entering the road
		 */
		if (this.interestedInEnteringRoad.size() > 0) {
			Vehicle nextVehicle = this.interestedInEnteringRoad.removeFirst();
			DeadlockPreventionMessage m = this.deadlockPreventionMessages.removeFirst();
			assert (m.vehicle == nextVehicle);
			this.scheduler.unschedule(m);

			double nextAvailableTimeForEnteringStreet = Math.max(this.timeOfLastEnteringVehicle
					+ this.inverseInFlowCapacity, simTime + this.gapTravelTime);

			this.noOfCarsPromisedToEnterRoad++;

			nextVehicle.scheduleEnterRoadMessage(nextAvailableTimeForEnteringStreet, this);
		} else {
			if (this.gap != null) {

				/*
				 * as long as the road is not full once, there is no need to
				 * keep track of the gaps
				 */
				this.gap.add(simTime + this.gapTravelTime);

				/*
				 * if no one is interested in entering this road (precondition)
				 * and there are no cars on the road, then reset gap (this is
				 * required, for enterRequest to function properly)
				 */
				if (this.carsOnTheRoad.size() == 0) {
					this.gap = null;
				}
			}
		}

		/*
		 * tell the car behind the fist car (which is the first car now), when
		 * it reaches the end of the read
		 */
		if (this.carsOnTheRoad.size() > 0) {
			Vehicle nextVehicle = this.carsOnTheRoad.getFirst();
			double nextAvailableTimeForLeavingStreet = Math.max(this.earliestDepartureTimeOfCar.getFirst(),
					this.timeOfLastLeavingVehicle + this.inverseOutFlowCapacity);
			nextVehicle.scheduleEndRoadMessage(nextAvailableTimeForLeavingStreet, this);
		}

	}

	public void enterRoad(Vehicle vehicle, double simTime) {
		// calculate time, when the car reaches the end of the road
		double nextAvailableTimeForLeavingStreet = simTime + this.link.getLength()
				/ this.link.getFreespeed(simTime);

		this.noOfCarsPromisedToEnterRoad--;
		this.carsOnTheRoad.add(vehicle);

		/*
		 * needed to remove the following assertion because for deadlock
		 * prevention there might be more cars on the road than its capacity
		 * assert maxNumberOfCarsOnRoad >= carsOnTheRoad.size() : "There are
		 * more cars on the road, than its capacity!";
		 */
		this.earliestDepartureTimeOfCar.add(nextAvailableTimeForLeavingStreet);

		/*
		 * if we are in the front of the queue, then we can just drive with free
		 * speed to the front and have to have at least inverseFlowCapacity
		 * time-distance to the previous car
		 */
		if (this.carsOnTheRoad.size() == 1) {
			nextAvailableTimeForLeavingStreet = Math.max(nextAvailableTimeForLeavingStreet,
					this.timeOfLastLeavingVehicle + this.inverseOutFlowCapacity);
			vehicle.scheduleEndRoadMessage(nextAvailableTimeForLeavingStreet, this);
//		} else { // empty else clause
			/*
			 * this car is not the front car in the street queue when the cars
			 * in front of the current car leave the street and this car becomes
			 * the front car, it will be waken up.
			 */
		}

	}

	public void enterRequest(Vehicle vehicle, double simTime) {
		assert (this.interestedInEnteringRoad.size()==this.deadlockPreventionMessages.size());
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
		if (this.carsOnTheRoad.size() + this.noOfCarsPromisedToEnterRoad < this.maxNumberOfCarsOnRoad) {
			/*
			 * - check, if the gap needs to be considered for entering the road -
			 * we can find out, the time since when we have a free road for
			 * entrance for sure:
			 */

			// the gap queue will only be empty in the beginning
			double arrivalTimeOfGap = Double.MIN_VALUE;
			// if the road has been full recently then find out, when the next
			// gap arrives
			if ((this.gap != null) && (this.gap.size() > 0)) {
				arrivalTimeOfGap = this.gap.remove();
			}

			this.noOfCarsPromisedToEnterRoad++;
			double nextAvailableTimeForEnteringStreet = Math.max(Math.max(this.timeOfLastEnteringVehicle
					+ this.inverseInFlowCapacity, simTime), arrivalTimeOfGap);

			this.timeOfLastEnteringVehicle = nextAvailableTimeForEnteringStreet;
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
			if (this.gap == null) {
				this.gap = new LinkedList<>();
			} else {
				this.gap.clear();
			}

			this.interestedInEnteringRoad.add(vehicle);

			/*
			 * the first car interested in entering a road has to wait
			 * 'stuckTime' the car behind has to wait an additional stuckTime
			 * (this logic was adapted to adhere to the C++ implementation)
			 */
			if (this.deadlockPreventionMessages.size() > 0) {
				this.deadlockPreventionMessages.add(vehicle.scheduleDeadlockPreventionMessage(
						this.deadlockPreventionMessages.getLast().getMessageArrivalTime()
								+ config.getSqueezeTime(), this));

			} else {
				this.deadlockPreventionMessages.add(vehicle.scheduleDeadlockPreventionMessage(simTime
						+ config.getSqueezeTime(), this));
			}

			assert (this.interestedInEnteringRoad.size()==this.deadlockPreventionMessages.size()) :this.interestedInEnteringRoad.size() + " - " + this.deadlockPreventionMessages.size();
		}
	}

	public void giveBackPromisedSpaceToRoad() {
		this.noOfCarsPromisedToEnterRoad--;
	}

	public void incrementPromisedToEnterRoad() {
		this.noOfCarsPromisedToEnterRoad++;
	}

	public Link getLink() {
		return this.link;
	}

	public void setTimeOfLastEnteringVehicle(double timeOfLastEnteringVehicle) {
		this.timeOfLastEnteringVehicle = timeOfLastEnteringVehicle;
	}

	public void removeFirstDeadlockPreventionMessage(DeadlockPreventionMessage dpMessage) {

		assert (this.deadlockPreventionMessages.getFirst() == dpMessage) : "Inconsitency in logic!!! => this should only be invoked from the handler of this message";
		this.deadlockPreventionMessages.removeFirst();
	}

	public void removeFromInterestedInEnteringRoad() {
		this.interestedInEnteringRoad.removeFirst();
		assert (this.interestedInEnteringRoad.size()==this.deadlockPreventionMessages.size());
	}

	public static Road getRoad(Id<Link> linkId) {
		return getAllRoads().get(linkId);
	}

}
