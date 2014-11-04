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

	public CAVehicle(Id<Vehicle> vehicleId, MobsimDriverAgent agent) {
		this.id = vehicleId;
		this.agent = agent;
	}

	@Override
	public Vehicle getVehicle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MobsimDriverAgent getDriver() {
		return this.agent;
	}

	public void setDriver( MobsimDriverAgent driver) {
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
		this.agent.notifyMoveOverNode(nextLink.getLink().getId());
		
	}

	@Override
	public CANetworkEntity getCurrentCANetworkEntity() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void moveToNode(CANode n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Link getCurrentLink() {
		return null;
	}

///////////////////////////////////////
	
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
