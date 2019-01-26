/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.dynagent.examples.random;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dynagent.DriverDynLeg;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.vehicles.Vehicle;

public class RandomDynLeg implements DriverDynLeg {
	private final Network network;

	private Id<Link> currentLinkId;
	private Id<Link> nextLinkId;
	private Id<Link> destinationLinkId;

	public RandomDynLeg(Id<Link> fromLinkId, Network network) {
		this.network = network;
		currentLinkId = fromLinkId;

		doRandomChoice();
	}

	@Override
	public void movedOverNode(Id<Link> newLinkId) {
		currentLinkId = newLinkId;
		doRandomChoice();
	}

	@Override
	public Id<Link> getNextLinkId() {
		return nextLinkId;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return destinationLinkId;
	}

	private void doRandomChoice() {
		// Do I want to end at this link?
		if (MatsimRandom.getRandom().nextInt(10) == 0) {// 10% chance
			nextLinkId = null;
			destinationLinkId = currentLinkId;
		} else {
			// Where do I want to move next?
			Link currentLink = network.getLinks().get(currentLinkId);
			Map<Id<Link>, ?> possibleNextLinks = currentLink.getToNode().getOutLinks();

			// Let's choose the next link randomly
			nextLinkId = RandomDynAgentLogic.chooseRandomElement(possibleNextLinks.keySet());

			// at this point the destination can be anything, QSim does not take it into account
			destinationLinkId = null;
		}
	}

	@Override
	public String getMode() {
		return TransportMode.car;
	}

	@Override
	public void arrivedOnLinkByNonNetworkMode(Id<Link> linkId) {
		currentLinkId = linkId;
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return null;
	}

	@Override
	public Double getExpectedTravelTime() {
		return null;// teleportation unsupported
	}

	@Override
	public Double getExpectedTravelDistance() {
		return null;// teleportation unsupported
	}
}
