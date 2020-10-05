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

import java.util.Map;

import javax.validation.constraints.PositiveOrZero;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author jbischoff
 * Config Group to set taxi or drt fares.
 */
public final class DrtFareParams extends ReflectiveConfigGroup {

	public static final String SET_NAME = "drtfare";

	public static final String BASEFARE = "basefare";
	public static final String MINFARE_PER_TRIP = "minFarePerTrip";
	public static final String DAILY_FEE = "dailySubscriptionFee";
	public static final String TIMEFARE = "timeFare_h";
	public static final String DISTANCEFARE = "distanceFare_m";
	public static final String MODE = "mode";

	@PositiveOrZero
	private double basefare;
	@PositiveOrZero
	private double minFarePerTrip = 0.0;
	@PositiveOrZero
	private double dailySubscriptionFee;
	@PositiveOrZero
	private double timeFare_h;
	@PositiveOrZero
	private double distanceFare_m;

	public DrtFareParams() {
		super(SET_NAME);

	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(BASEFARE, "Basefare per trip (positive or zero value)");
		map.put(MINFARE_PER_TRIP,
				"Minimum fare per trip (paid instead of the sum of base, time and distance fare if that sum would be lower than the minimum fare, positive or zero value).");
		map.put(DAILY_FEE, "Daily subscription fee (positive or zero value)");
		map.put(TIMEFARE, "drt fare per hour (positive or zero value)");
		map.put(DISTANCEFARE, "drt fare per meter (positive or zero value)");
		map.put(MODE, "transport mode for which the fare applies. Default: drt");
		return map;
	}

	@StringGetter(BASEFARE)
	public double getBasefare() {
		return basefare;
	}

	@StringSetter(BASEFARE)
	public void setBasefare(double basefare) {
		this.basefare = basefare;
	}

	@StringGetter(MINFARE_PER_TRIP)
	public double getMinFarePerTrip() {
		return minFarePerTrip;
	}

	@StringSetter(MINFARE_PER_TRIP)
	public void setMinFarePerTrip(double minFarePerTrip) {
		this.minFarePerTrip = minFarePerTrip;
	}

	@StringGetter(DAILY_FEE)
	public double getDailySubscriptionFee() {
		return dailySubscriptionFee;
	}

	@StringSetter(DAILY_FEE)
	public void setDailySubscriptionFee(double dailySubscriptionFee) {
		this.dailySubscriptionFee = dailySubscriptionFee;
	}

	@StringGetter(TIMEFARE)
	public double getTimeFare_h() {
		return timeFare_h;
	}

	@StringSetter(TIMEFARE)
	public void setTimeFare_h(double timeFare_h) {
		this.timeFare_h = timeFare_h;
	}

	@StringGetter(DISTANCEFARE)
	public double getDistanceFare_m() {
		return distanceFare_m;
	}

	@StringSetter(DISTANCEFARE)
	public void setDistanceFare_m(double distanceFare_m) {
		this.distanceFare_m = distanceFare_m;
	}
}
