/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.parking.parkingsearch.search;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.vehicles.Vehicle;

import java.util.List;
import java.util.Random;

/**
 * @author jbischoff
 *
 */

public class RandomParkingSearchLogic implements ParkingSearchLogic {

	private Network network;
	private final Random random = MatsimRandom.getLocalInstance();

	/**
	 * {@link Network} the network
	 * 
	 */
	public RandomParkingSearchLogic(Network network) {
		this.network = network;
	}

	@Override
	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Vehicle> vehicleId, String mode) {
		Link currentLink = network.getLinks().get(currentLinkId);
		List<Link> keys = ParkingUtils.getOutgoingLinksForMode(currentLink, mode);
		Id<Link> randomKey = keys.get(random.nextInt(keys.size())).getId();
		return randomKey;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.jbischoff.parking.choice.ParkingChoiceLogic#reset()
	 */
	@Override
	public void reset() {
	}

}
