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

package org.matsim.contrib.taxi.fare;

import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * @author jbischoff
 * Config Group to set taxi or drt fares.
 */
public final class TaxiFareParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "taxifare";

	@Parameter("basefare")
	@Comment("Basefare per trip (positive or zero value)")
	@PositiveOrZero
	private double basefare;

	@Parameter
	@Comment("Minimum fare per trip (paid instead of the sum of base, time and distance fare if that sum would be lower than the minimum fare, positive or zero value).")
	@PositiveOrZero
	private double minFarePerTrip = 0.0;

	@Parameter
	@Comment("Daily subscription fee (positive or zero value)")
	@PositiveOrZero
	private double dailySubscriptionFee;

	@Parameter
	@Comment("taxi fare per hour (positive or zero value)")
	@PositiveOrZero
	private double timeFare_h;

	@Parameter
	@Comment("taxi fare per meter (positive or zero value)")
	@PositiveOrZero
	private double distanceFare_m;

	public TaxiFareParams() {
		super(SET_NAME);
	}

	@PositiveOrZero
	public double getBasefare() {
		return basefare;
	}

	public void setBasefare(@PositiveOrZero double basefare) {
		this.basefare = basefare;
	}

	@PositiveOrZero
	public double getMinFarePerTrip() {
		return minFarePerTrip;
	}

	public void setMinFarePerTrip(@PositiveOrZero double minFarePerTrip) {
		this.minFarePerTrip = minFarePerTrip;
	}

	@PositiveOrZero
	public double getDailySubscriptionFee() {
		return dailySubscriptionFee;
	}

	public void setDailySubscriptionFee(@PositiveOrZero double dailySubscriptionFee) {
		this.dailySubscriptionFee = dailySubscriptionFee;
	}

	@PositiveOrZero
	public double getTimeFare_h() {
		return timeFare_h;
	}

	public void setTimeFare_h(@PositiveOrZero double timeFare_h) {
		this.timeFare_h = timeFare_h;
	}

	@PositiveOrZero
	public double getDistanceFare_m() {
		return distanceFare_m;
	}

	public void setDistanceFare_m(@PositiveOrZero double distanceFare_m) {
		this.distanceFare_m = distanceFare_m;
	}
}
