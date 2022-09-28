/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.fare;

import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * @author jbischoff
 * Config Group to set taxi or drt fares.
 */
public final class DrtFareParams extends ReflectiveConfigGroup {

	public static final String SET_NAME = "drtfare";

	@Parameter("basefare")
	@Comment("Basefare per trip (positive or zero value)")
	@PositiveOrZero
	public double baseFare;

	@Parameter
	@Comment("Minimum fare per trip (paid instead of the sum of base, time and distance fare if that sum would be lower than the minimum fare, positive or zero value).")
	@PositiveOrZero
	public double minFarePerTrip = 0.0;

	@Parameter
	@Comment("Daily subscription fee (positive or zero value)")
	@PositiveOrZero
	public double dailySubscriptionFee;

	@Parameter
	@Comment("drt fare per hour (positive or zero value)")
	@PositiveOrZero
	public double timeFare_h;

	@Parameter
	@Comment("drt fare per meter (positive or zero value)")
	@PositiveOrZero
	public double distanceFare_m;

	public DrtFareParams() {
		super(SET_NAME);
	}
}
