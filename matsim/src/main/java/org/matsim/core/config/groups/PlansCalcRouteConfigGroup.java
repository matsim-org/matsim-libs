/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcRouteConfigGroup
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.config.groups;

import java.util.Map;

import org.matsim.core.config.Module;


/**
 * Config Module for PlansCalcRoute class.
 * Here you can specify the scale factors of freespeed travel time which are used
 * as travel time for not microsimulated modes.
 *
 * @author dgrether
 * @author mrieser
 */
public class PlansCalcRouteConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "planscalcroute";

	private static final String PT_SPEED_FACTOR = "ptSpeedFactor";
	private static final String WALK_SPEED = "walkSpeed";
	private static final String BIKE_SPEED = "bikeSpeed";
	private static final String UNDEFINED_MODE_SPEED = "undefinedModeSpeed";

	private double ptSpeedFactor = 2.0;

	private double walkSpeed = 3.0 / 3.6; // 3.0 km/h --> m/s

	private double bikeSpeed = 15.0 / 3.6; // 15.0 km/h --> m/s

	private double undefinedModeSpeed = 50.0 / 3.6; // 50.0 km/h --> m/s

	public PlansCalcRouteConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (PT_SPEED_FACTOR.equals(key)) {
			return Double.toString(getPtSpeedFactor());
		} else if (WALK_SPEED.equals(key)) {
			return Double.toString(getWalkSpeed());
		} else if (BIKE_SPEED.equals(key)) {
			return Double.toString(getBikeSpeed());
		} else if (UNDEFINED_MODE_SPEED.equals(key)) {
			return Double.toString(getUndefinedModeSpeed());
		}

		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (PT_SPEED_FACTOR.equals(key)) {
			setPtSpeedFactor(Double.parseDouble(value));
		} else if (WALK_SPEED.equals(key)) {
			setWalkSpeed(Double.parseDouble(value));
		} else if (BIKE_SPEED.equals(key)) {
			setBikeSpeed(Double.parseDouble(value));
		} else if (UNDEFINED_MODE_SPEED.equals(key)) {
			setUndefinedModeSpeed(Double.parseDouble(value));
		} else if (key.endsWith("Factor")) {
			throw new IllegalArgumentException("The parameter " + key + " does no longer exist. It was renamed to " + key.substring(0, key.length() - 6) + " to indicate that it is NOT a factor but the actual speed.");
		}
		else {
			throw new IllegalArgumentException(key);
		}

	}

	@Override
	protected final Map<String, String> getParams() {
		Map<String, String> map = super.getParams();
		super.addParameterToMap(map, PT_SPEED_FACTOR);
		super.addParameterToMap(map, WALK_SPEED);
		super.addParameterToMap(map, BIKE_SPEED);
		super.addParameterToMap(map, UNDEFINED_MODE_SPEED);
		return map;
	}

	public double getPtSpeedFactor() {
		return this.ptSpeedFactor;
	}

	public double getWalkSpeed() {
		return walkSpeed;
	}

	public void setWalkSpeed(double walkSpeed) {
		this.walkSpeed = walkSpeed;
	}

	public double getBikeSpeed() {
		return bikeSpeed;
	}

	public void setBikeSpeed(double bikeSpeed) {
		this.bikeSpeed = bikeSpeed;
	}

	public double getUndefinedModeSpeed() {
		return undefinedModeSpeed;
	}

	public void setUndefinedModeSpeed(double undefinedModeSpeed) {
		this.undefinedModeSpeed = undefinedModeSpeed;
	}

	public void setPtSpeedFactor(double ptSpeedFactor) {
		this.ptSpeedFactor = ptSpeedFactor;
	}

}
