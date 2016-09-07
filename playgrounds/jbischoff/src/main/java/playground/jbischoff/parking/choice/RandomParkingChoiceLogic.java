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
package playground.jbischoff.parking.choice;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;

/**
 * @author jbischoff
 *
 */

public class RandomParkingChoiceLogic implements ParkingChoiceLogic {

	private Network network;
	private final Random random = MatsimRandom.getLocalInstance();

	/**
	 * {@link Network} the network
	 */
	public RandomParkingChoiceLogic(Network network) {

		this.network = network;
	}

	@Override
	public Id<Link> getNextLink(Id<Link> currentLinkId) {
		Link currentLink = network.getLinks().get(currentLinkId);
		List<Id<Link>> keys = new ArrayList<>(currentLink.getToNode().getOutLinks().keySet());
		Id<Link> randomKey = keys.get(random.nextInt(keys.size()));
		return randomKey;

	}
}
