/* *********************************************************************** *
 * project: org.matsim.*
 * SimVehicle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The ``Q'' implementation of the MobsimVehicle.
 * <p></p>
 * Design thoughts:<ul>
 * <li> This needs to be public since the ``Q'' version of the
 * vehicle is used by more than one package.  This interfaces should, however, not be used outside the relevant
 * netsimengines.  In particular, the information should not be used for visualization.  kai, nov'11
 * <li> Might be possible to make all methods in this hierarchy protected and just inherit to a QueueVehicle.  kai, nov'11
 * </ul>
 *
 * @author nagel
 */

public class QVehicleImpl implements QVehicle, DistributedMobsimVehicle {

	private static final Logger log = LogManager.getLogger(QVehicleImpl.class);

	private static int warnCount = 0;

	// This parameter is about to be removed. Look in Mobsim Vehicle for deprecation notes
	@Deprecated
	private double linkEnterTime = 0.;
	private double earliestLinkExitTime = 0;
	private DriverAgent driver = null;
	private Collection<PassengerAgent> passengers;
	private final Id<Vehicle> id;
	private Id<Link> currentLinkId = null;
	private final Vehicle vehicle;
	private final int passengerCapacity;
	private final double pceScalingFactor;

	/**
	 * Creates a new QVehicle and takes the pce of the supplied vehicle type as is.
	 *
	 * @param basicVehicle the material for the QVehicle
	 */
	public QVehicleImpl(final Vehicle basicVehicle) {
		this(basicVehicle, 1.0);
	}

	/**
	 * Creates a new QVehicle
	 *
	 * @param basicVehicle     material for the QVehicle
	 * @param pceScalingFactor scales the pce of the vehicle type. The scaled pce = vehicleType.pce * pceScalingFactor
	 */
	public QVehicleImpl(final Vehicle basicVehicle, double pceScalingFactor) {
		this.id = basicVehicle.getId();
		this.vehicle = basicVehicle;
		this.pceScalingFactor = pceScalingFactor;

		VehicleCapacity capacity = basicVehicle.getType().getCapacity();
		if (capacity == null) {
			this.passengerCapacity = 4;
			if (warnCount < 10) {
				log.warn("No VehicleCapacity (= maximum number of passengers) set in Vehicle. "
					+ "Using default value of 4.  This is only a problem if you need vehicles with different "
					+ "capacities, e.g. for minibuses.");
				warnCount++;
				if (warnCount == 10) {
					log.warn(Gbl.FUTURE_SUPPRESSED);
				}
			}
		} else {
			// do *not* subtract one for the driver! Most pt vehicles define the capacity without the driver.
			// for private cars, think about if we should subtract one from the capacity if the driver is set?
			// But if we do, change the number of seats of the default vehicle from 4 to 5.
			this.passengerCapacity = capacity.getSeats() +
				(capacity.getStandingRoom() == null ? 0 : capacity.getStandingRoom());
		}
	}

	/**
	 * Creates a QVehicle from a message. This is used in the DSim implementation
	 * The constructor re-creates the vehicle as well as the state it had, when it was
	 * converted into a message.
	 */
	public QVehicleImpl(Msg message) {
		this.id = message.vehicle().getId();
		this.vehicle = message.vehicle();
		this.passengerCapacity = message.passengerCapacity();
		this.linkEnterTime = message.linkEnterTime();
		this.earliestLinkExitTime = message.earliestLinkExitTime();
		this.currentLinkId = message.currentLinkId();
		this.pceScalingFactor = message.pceScalingFactor();
	}

	@Override
	public void setCurrentLinkId(final Id<Link> linkId) {
		this.currentLinkId = linkId;
	}

	@Override
	public double getEarliestLinkExitTime() {
		return this.earliestLinkExitTime;
	}

	@Override
	public void setEarliestLinkExitTime(final double time) {
		this.earliestLinkExitTime = time;
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return this.currentLinkId;
	}

	@Override
	public MobsimDriverAgent getDriver() {
		if (this.driver instanceof MobsimDriverAgent) {
			return (MobsimDriverAgent) this.driver;
		} else if (this.driver == null) {
			return null;
		} else {
			throw new RuntimeException("error (downstream methods need to be made to accept DriverAgent)");
		}
	}

	@Override
	public void setDriver(final DriverAgent driver) {
		if (driver != null) {
			if (this.driver != null && !this.driver.getId().equals(driver.getId())) {
				throw new RuntimeException("A driver (" + this.driver.getId() + ") " +
					"is already set in vehicle " + this.getId() + ". " +
					"Setting agent " + driver.getId().toString() + " is not possible!");
			}
		}
		// TODO: To make this check possible, we would need something like removeDriver().
//		else {
//			throw new RuntimeException( "Driver to be set in vehicle " + this.getId() +
//					" is null!");
//		}

		this.driver = driver;
	}

	@Override
	public Id<Vehicle> getId() {
		return this.id;
	}

	@Override
	public double getSizeInEquivalents() {
		return vehicle.getType().getPcuEquivalents() * pceScalingFactor;
	}

	@Override
	public Vehicle getVehicle() {
		return this.vehicle;
	}

	@Override
	public String toString() {
		return "Vehicle Id " + getId() + ", driven by (personId) " + this.driver.getId()
			+ ", on link " + this.currentLinkId;
	}

	public double getMaximumVelocity() {
		return vehicle.getType().getMaximumVelocity();
	}

	@Override
	public Collection<? extends PassengerAgent> getPassengers() {
		// List.of() also returns an empty list singleton, in case this method is called often, but with no passengers,
		// returning List.of(), should be fast enough.
		return passengers != null ? Collections.unmodifiableCollection(passengers) : List.of();
	}

	@Override
	public boolean addPassenger(PassengerAgent passenger) {

		// initialize lazily
		if (passengers == null) {
			this.passengers = new ArrayList<>();
		}

		if (this.passengers.size() < this.passengerCapacity) {
			return this.passengers.add(passenger);
		}
		return false;
	}

	@Override
	public boolean removePassenger(PassengerAgent passenger) {

		return passengers != null && passengers.remove(passenger);
	}

	@Override
	public int getPassengerCapacity() {
		return this.passengerCapacity;
	}

	@Override
	@Deprecated
	public final double getLinkEnterTime() {
		return this.linkEnterTime;
	}

	@Override
	@Deprecated
	public final void setLinkEnterTime(double linkEnterTime) {
		// yyyyyy use in code!
		this.linkEnterTime = linkEnterTime;
	}

	@Override
	public Message toMessage() {
		return new Msg(
			this.linkEnterTime,
			this.earliestLinkExitTime,
			this.currentLinkId,
			this.vehicle,
			this.passengerCapacity,
			this.pceScalingFactor
		);
	}

	/**
	 * Class to represent a vehicle as message object.
	 */
	public record Msg(
		double linkEnterTime,
		double earliestLinkExitTime,
		Id<Link> currentLinkId,
		Vehicle vehicle,
		int passengerCapacity,
		double pceScalingFactor
	) implements Message {
	}
}
