package playground.gregor.ctsim.simulation.physics;
/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;

import java.util.List;

/**
 * Created by laemmel on 12/10/15.
 */
public class SimpleCTNetworkWalker implements DriverAgent {

	private List<Id<Link>> links;
	private int idx = 1;

	public SimpleCTNetworkWalker(List<Id<Link>> links) {
		this.links = links;
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		return links.get(idx);
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		idx++;
		if (idx == links.size()) {
			idx = 0;
		}
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		throw new RuntimeException("unimplemented method!");
	}

	@Override
	public Id<Person> getId() {
		return null;
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		if (idx == 0) {
			return links.get(links.size() - 1);
		}
		return links.get(idx - 1);
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		throw new RuntimeException("unimplemented method!");
	}

	@Override
	public String getMode() {
		throw new RuntimeException("unimplemented method!");
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		throw new RuntimeException("unimplemented method!");
	}

	@Override
	public MobsimVehicle getVehicle() {
		throw new RuntimeException("unimplemented method!");
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		throw new RuntimeException("unimplemented method!");
	}


}
