/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.trafficmonitoring;

import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.router.util.TravelTime;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpModeLimitedMaxSpeedTravelTimeModule extends AbstractDvrpModeModule {
	private final double timeStepSize;
	private final double maxSpeed;

	public DvrpModeLimitedMaxSpeedTravelTimeModule(String mode, double timeStepSize, double maxSpeed) {
		super(mode);
		this.timeStepSize = timeStepSize;
		this.maxSpeed = maxSpeed;
	}

	@Override
	public void install() {
		var qSimFreeSpeedTravelTimeWithMaxSpeedLimit = new QSimFreeSpeedTravelTimeWithMaxSpeedLimit(timeStepSize,
				maxSpeed);
		bindModal(TravelTime.class).toProvider(modalProvider(
				getter -> TravelTimeUtils.maxOfTravelTimes(qSimFreeSpeedTravelTimeWithMaxSpeedLimit,
						getter.getNamed(TravelTime.class, DvrpTravelTimeModule.DVRP_ESTIMATED))));
	}
}

