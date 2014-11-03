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
	private Vehicle vehicle;

	public CAVehicle(final Vehicle basicVehicle) {
		this.id = basicVehicle.getId();
		this.vehicle = basicVehicle;
	}

	@Override
	public Vehicle getVehicle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MobsimDriverAgent getDriver() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Id<Vehicle> getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Link getCurrentLink() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getSizeInEquivalents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean addPassenger(PassengerAgent passenger) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removePassenger(PassengerAgent passenger) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<? extends PassengerAgent> getPassengers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPassengerCapacity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	Id<Link> getNextLinkId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void moveOverNode(CALink nextLink, double time) {
		// TODO Auto-generated method stub
		
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

	

}
