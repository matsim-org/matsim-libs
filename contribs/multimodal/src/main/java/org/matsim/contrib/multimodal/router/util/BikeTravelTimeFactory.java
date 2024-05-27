/* *********************************************************************** *
 * project: org.matsim.*
 * BikeTravelTimeFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal.router.util;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.router.util.TravelTime;

import jakarta.inject.Provider;
import java.util.Map;

public class BikeTravelTimeFactory implements Provider<TravelTime> {

	private final RoutingConfigGroup routingConfigGroup;
	private final Map<Id<Link>, Double> linkSlopes;	// slope information in %

	public BikeTravelTimeFactory(RoutingConfigGroup routingConfigGroup) {
		this(routingConfigGroup, null);
	}

	public BikeTravelTimeFactory(RoutingConfigGroup routingConfigGroup,
															 Map<Id<Link>, Double> linkSlopes) {
		this.routingConfigGroup = routingConfigGroup;
		this.linkSlopes = linkSlopes;

		if (routingConfigGroup.getTeleportedModeSpeeds().get(TransportMode.bike) == null) {
			throw new RuntimeException("No speed was found for mode bike! Aborting.");
		}
	}

	@Override
	public TravelTime get() {
		return new BikeTravelTime(this.routingConfigGroup, this.linkSlopes);
	}

}
