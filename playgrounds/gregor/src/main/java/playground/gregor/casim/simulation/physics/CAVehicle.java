/* *********************************************************************** *
 * project: org.matsim.*
 * CAVehicle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.casim.simulation.physics;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;

public class CAVehicle extends CAMoveableEntity implements MobsimVehicle {

	private Id<Vehicle> id;
	private MobsimDriverAgent agent;
	private CANetworkEntity currentCANetorkEntity;
	private final int hash;
	private CANetworkEntity lastCANetworkEntity;

	public CAVehicle(Id<Vehicle> vehicleId, MobsimDriverAgent agent, CALink link) {
		this.id = vehicleId;
		this.agent = agent;
		this.currentCANetorkEntity = link;
		this.hash = (vehicleId.toString() + "CAVehicle").hashCode();
	}

	public Id<Vehicle> getVehicleId() {
		return this.id;
	}

	@Override
	public MobsimDriverAgent getDriver() {
		return this.agent;
	}

	public void setDriver(MobsimDriverAgent driver) {
		this.agent = null;
	}

	@Override
	public Id<Vehicle> getId() {
		return this.id;
	}

	@Override
	Id<Link> getNextLinkId() {
		return this.agent.chooseNextLinkId();
	}

	@Override
	void moveOverNode(CALink nextLink, double time) {
		this.currentCANetorkEntity = nextLink;
		if (this.getPos() == 0) {
			this.agent.notifyMoveOverNode(nextLink.getLink().getId());
		} else {
			this.agent.notifyMoveOverNode(nextLink.getUpstreamLink().getId());
		}
	}

	@Override
	public CANetworkEntity getCurrentCANetworkEntity() {
		return this.currentCANetorkEntity;
	}

	@Override
	public void moveToNode(CANode n) {
		this.lastCANetworkEntity = this.currentCANetorkEntity;
		this.currentCANetorkEntity = n;
	}

	@Override
	public CANetworkEntity getLastCANetworkEntity() {
		return this.lastCANetworkEntity;
	}

	@Override
	public Link getCurrentLink() {
		throw new RuntimeException("unsed method");
	}

	@Override
	public Vehicle getVehicle() {
		throw new RuntimeException("unimplemented method");
	}

	// /////////////////////////////////////

	@Override
	public double getSizeInEquivalents() {
		throw new RuntimeException("unsed method");
	}

	@Override
	public boolean addPassenger(PassengerAgent passenger) {
		throw new RuntimeException("unsed method");
	}

	@Override
	public boolean removePassenger(PassengerAgent passenger) {
		throw new RuntimeException("unsed method");
	}

	@Override
	public Collection<? extends PassengerAgent> getPassengers() {
		throw new RuntimeException("unsed method");
	}

	@Override
	public int getPassengerCapacity() {
		throw new RuntimeException("unsed method");
	}

}
