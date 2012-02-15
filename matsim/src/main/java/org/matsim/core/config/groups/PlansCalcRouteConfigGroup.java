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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Module;
import org.matsim.core.utils.collections.CollectionUtils;


/**
 * Config Module for PlansCalcRoute class.
 * Here you can specify the scale factors of freespeed travel time which are used
 * as travel time for not microsimulated modes.
 *
 * @author dgrether
 * @author mrieser
 */
public class PlansCalcRouteConfigGroup extends Module {

	public enum PtSpeedMode {freespeed, beeline}

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "planscalcroute";

	private static final String PT_SPEED_MODE = "ptSpeedMode";
	private static final String PT_SPEED_FACTOR = "ptSpeedFactor";

	private static final String PT_SPEED = "ptSpeed";
	private static final String WALK_SPEED = "walkSpeed";
	private static final String BIKE_SPEED = "bikeSpeed";

	private static final String BEELINE_DISTANCE_FACTOR = "beelineDistanceFactor";

	private static final String UNDEFINED_MODE_SPEED = "undefinedModeSpeed";
	
	private static final String NETWORK_MODES = "networkModes";
	private static final String TELEPORTED_MODES = "teleportedModes";
	private static final String TELEPORTED_MODE_SPEEDS = "teleportedModeSpeeds";

	private PtSpeedMode ptSpeedMode = PtSpeedMode.freespeed;
	private double ptSpeedFactor = 2.0;
	private double beelineDistanceFactor = 1.3;
	private double walkSpeed = 3.0 / 3.6; // 3.0 km/h --> m/s
	private double bikeSpeed = 15.0 / 3.6; // 15.0 km/h --> m/s
	private double ptSpeed = 25.0 / 3.6; // 25.0 km/h --> m/s
	private double undefinedModeSpeed = 50.0 / 3.6; // 50.0 km/h --> m/s
	private String[] networkModes = {TransportMode.car, TransportMode.ride}; 
	private String[] teleportedModes = {TransportMode.bike, TransportMode.walk};
	private Double[] teleportedModeSpeeds = {bikeSpeed, walkSpeed};
	
	public PlansCalcRouteConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		throw new IllegalArgumentException(key + ": getValue access disabled; use direct getter");
	}

	@Override
	public void addParam(final String key, final String value) {
		if (PT_SPEED_MODE.equals(key)) {
			setPtSpeedMode(PtSpeedMode.valueOf(value));
		} else if (PT_SPEED_FACTOR.equals(key)) {
			setPtSpeedFactor(Double.parseDouble(value));
		} else if (BEELINE_DISTANCE_FACTOR.equals(key)) {
			setBeelineDistanceFactor(Double.parseDouble(value));
		} else if (PT_SPEED.equals(key)) {
			setPtSpeed(Double.parseDouble(value));
		} else if (WALK_SPEED.equals(key)) {
			setWalkSpeed(Double.parseDouble(value));
		} else if (BIKE_SPEED.equals(key)) {
			setBikeSpeed(Double.parseDouble(value));
		} else if (UNDEFINED_MODE_SPEED.equals(key)) {
			setUndefinedModeSpeed(Double.parseDouble(value));
		} else if (NETWORK_MODES.equals(key)) {
			setNetworkModes(CollectionUtils.stringToArray(value));
		} else if (TELEPORTED_MODES.equals(key)) {
			setTeleportedModes(CollectionUtils.stringToArray(value));
		} else if (TELEPORTED_MODE_SPEEDS.equals(key)) {
			setTeleportedModeSpeeds(stringArrayToDoubleArray(CollectionUtils.stringToArray(value)));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final Map<String, String> getParams() {
		Map<String, String> map = super.getParams();
		map.put( PT_SPEED_MODE, this.getPtSpeedMode().toString() );
		map.put( PT_SPEED_FACTOR, Double.toString(this.getPtSpeedFactor()) );
		map.put( BEELINE_DISTANCE_FACTOR, Double.toString(this.getBeelineDistanceFactor()) );
		map.put( PT_SPEED, Double.toString(this.getPtSpeed()) );
		map.put( WALK_SPEED, Double.toString(this.getWalkSpeed()) );
		map.put( BIKE_SPEED, Double.toString(this.getBikeSpeed()) );
		map.put( UNDEFINED_MODE_SPEED, Double.toString(this.getUndefinedModeSpeed()) );
		map.put( NETWORK_MODES, CollectionUtils.arrayToString(this.networkModes) );
		map.put( TELEPORTED_MODES, CollectionUtils.arrayToString(this.teleportedModes) );
		map.put( TELEPORTED_MODE_SPEEDS, CollectionUtils.arrayToString(doubleArrayToStringArray(this.teleportedModeSpeeds)));
		return map;
	}

	private static String[] doubleArrayToStringArray(Double[] doubles) {
		// I want a less verbose programming language.
		String[] strings = new String[doubles.length];
		for (int i=0; i<doubles.length; ++i) {
			strings[i] = String.valueOf(doubles[i]);
		}
		return strings;
	}

	private Double[] stringArrayToDoubleArray(String[] strings) {
		Double[] doubles = new Double[strings.length];
		for (int i=0; i<strings.length; ++i) {
			doubles[i] = Double.parseDouble(strings[i]);
		}
		return doubles;
		// My brain hurts.
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(PT_SPEED_MODE, "Allowed values: freespeed, beeline. Determines if travel times for non-simulated pt legs are estimated by ptSpeedFactor * <freespeed car travel time> (\"freespeed\")" +
				" or by (<beeline distance> * beelineDistanceFactor) / ptSpeed (\"beeline\")");
		map.put(PT_SPEED_FACTOR, "factor with which times from the car freespeed travel time " +
				"calculation are multiplied in order to obtain the pt travel times.  Default is something like 2") ;
		map.put(BEELINE_DISTANCE_FACTOR, "factor with which beeline distances (and therefore times) " +
				"are multiplied in order to obtain an estimate of the network distances/times.  Default is something like 1.3") ;
		map.put(NETWORK_MODES, "All the modes for which the router is supposed to generate network routes (like car)") ;
		map.put(TELEPORTED_MODES, "All the modes which are not routed through the network but get speed estimates based on beeline distance.");
		map.put(TELEPORTED_MODE_SPEEDS, "Speeds for all teleportationModes, in order.");
		return map ;
	}

	public double getPtSpeedFactor() {
		return this.ptSpeedFactor;
	}

	public double getWalkSpeed() {
		return this.walkSpeed;
	}

	public void setWalkSpeed(final double walkSpeed) {
		this.walkSpeed = walkSpeed;
	}

	public double getBikeSpeed() {
		return this.bikeSpeed;
	}

	public void setBikeSpeed(final double bikeSpeed) {
		this.bikeSpeed = bikeSpeed;
	}

	public double getUndefinedModeSpeed() {
		return this.undefinedModeSpeed;
	}

	public void setUndefinedModeSpeed(final double undefinedModeSpeed) {
		this.undefinedModeSpeed = undefinedModeSpeed;
	}

	public void setPtSpeedFactor(final double ptSpeedFactor) {
		this.ptSpeedFactor = ptSpeedFactor;
	}

	public double getBeelineDistanceFactor() {
		return this.beelineDistanceFactor;
	}

	public void setBeelineDistanceFactor(double beelineDistanceFactor) {
		this.beelineDistanceFactor = beelineDistanceFactor;
	}

	public PtSpeedMode getPtSpeedMode() {
		return this.ptSpeedMode;
	}

	public void setPtSpeedMode(PtSpeedMode ptSpeedMode) {
		this.ptSpeedMode = ptSpeedMode;
	}

	public double getPtSpeed() {
		return this.ptSpeed;
	}

	public void setPtSpeed(double ptSpeed) {
		this.ptSpeed = ptSpeed;
	}

	public String[] getNetworkModes() {
		return this.networkModes;
	}
	
	public void setNetworkModes(String[] networkModes) {
		this.networkModes = networkModes;
	}

	public String[] getTeleportedModes() {
		return this.teleportedModes;
	}
	
	public void setTeleportedModes(String[] teleportedModes) {
		this.teleportedModes = teleportedModes;
	}

	public Double[] getTeleportedModeSpeeds() {
		return this.teleportedModeSpeeds;
	}

	public void setTeleportedModeSpeeds(Double[] teleportedModeSpeeds) {
		this.teleportedModeSpeeds = teleportedModeSpeeds;
	}
	
}
