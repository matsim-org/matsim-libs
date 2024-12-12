/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleImpl.java
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

package org.matsim.core.mobsim.qsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;

import java.util.Collection;


public class TransitQVehicle implements DistributedMobsimVehicle, TransitVehicle {

	private TransitStopHandler stopHandler;
	private final QVehicleImpl baseVehicle;

	public TransitQVehicle(final Vehicle basicVehicle) {
		baseVehicle = new QVehicleImpl(basicVehicle);

		VehicleCapacity capacity = basicVehicle.getType().getCapacity();
		if (capacity == null) {
			throw new NullPointerException("No capacity set in vehicle type.");
		}
		// New default is that vehicle is created with capacity. Initial values are 1 seat for the driver and no standing room {@link org.matsim.vehicles.VehicleCapacity} (sep 19, KMT)
		// PT does not count the driver, so only warn if there is indeed no real capacity (mar 21, mrieser)
		if (capacity.getSeats() == 0 && capacity.getStandingRoom() == 0) {
			throw new NullPointerException("No capacity set in vehicle type.");
		}
	}

	public TransitQVehicle(QVehicleImpl.Msg message) {
		baseVehicle = new QVehicleImpl(message);
	}

	public void setStopHandler(TransitStopHandler stopHandler) {
		this.stopHandler = stopHandler;
	}

	@Override
	public TransitStopHandler getStopHandler() {
		return this.stopHandler;
	}

	@Override
	public void setCurrentLinkId(Id<Link> link) {
		baseVehicle.setCurrentLinkId(link);
	}

	@Override
	public void setDriver(DriverAgent driver) {
		baseVehicle.setDriver(driver);
	}

	@Override
	public double getLinkEnterTime() {
		return baseVehicle.getLinkEnterTime();
	}

	@Override
	public void setLinkEnterTime(double linkEnterTime) {
		baseVehicle.setLinkEnterTime(linkEnterTime);
	}

	@Override
	public double getMaximumVelocity() {
		return baseVehicle.getMaximumVelocity();
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return baseVehicle.getCurrentLinkId();
	}

	@Override
	public Vehicle getVehicle() {
		return baseVehicle.getVehicle();
	}

	@Override
	public MobsimDriverAgent getDriver() {
		return baseVehicle.getDriver();
	}

	@Override
	public double getSizeInEquivalents() {
		return baseVehicle.getSizeInEquivalents();
	}

	@Override
	public boolean addPassenger(PassengerAgent passenger) {
		return baseVehicle.addPassenger(passenger);
	}

	@Override
	public boolean removePassenger(PassengerAgent passenger) {
		return baseVehicle.removePassenger(passenger);
	}

	@Override
	public Collection<? extends PassengerAgent> getPassengers() {
		return baseVehicle.getPassengers();
	}

	@Override
	public int getPassengerCapacity() {
		return baseVehicle.getPassengerCapacity();
	}

	@Override
	public double getEarliestLinkExitTime() {
		return baseVehicle.getEarliestLinkExitTime();
	}

	@Override
	public void setEarliestLinkExitTime(double earliestLinkEndTime) {
		baseVehicle.setEarliestLinkExitTime(earliestLinkEndTime);
	}

	@Override
	public Id<Vehicle> getId() {
		return baseVehicle.getId();
	}

	@Override
	public Message toMessage() {
		var handler = stopHandler.toMessage();
		var baseMessage = baseVehicle.toMessage();
		return new Msg(baseMessage, handler);
	}

	record Msg(Message baseMessage, Message handlerMessage) implements Message {
	}
}
