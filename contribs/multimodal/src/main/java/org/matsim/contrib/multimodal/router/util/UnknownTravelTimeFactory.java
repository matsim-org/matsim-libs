/* *********************************************************************** *
 * project: org.matsim.*
 * UnknownTravelTimeFactory.java
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

import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.router.util.TravelTime;

import jakarta.inject.Provider;

public class UnknownTravelTimeFactory implements Provider<TravelTime> {

	private final String mode;
	private final RoutingConfigGroup routingConfigGroup;

	public UnknownTravelTimeFactory(String mode, RoutingConfigGroup routingConfigGroup) {

		this.mode = mode;
		this.routingConfigGroup = routingConfigGroup;

		Double speed = routingConfigGroup.getTeleportedModeSpeeds().get(mode);
		Double speedFactor = routingConfigGroup.getTeleportedModeFreespeedFactors().get(mode);

		if (speed != null && speedFactor != null) {
			throw new RuntimeException("Speed as well as speed factor was found for mode " + mode +
					"!  Don't know which should be used. Aborting.");
		} else if (speed == null && speedFactor == null) {
			throw new RuntimeException("Neither speed nor speed factor was found for mode " + mode + "! Aborting.");
		}
	}

	@Override
	public TravelTime get() {
		return new UnknownTravelTime(this.mode, this.routingConfigGroup);
	}

}
