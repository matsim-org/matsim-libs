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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
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

	private static final String UNDEFINED = "undefined";

	public enum PtSpeedMode {freespeed, beeline}

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "planscalcroute";

	private static final String PT_SPEED_MODE = "ptSpeedMode";
	private static final String PT_SPEED_FACTOR = "ptSpeedFactor";

	private static final String PT_SPEED = "ptSpeed";
	private static final String WALK_SPEED = "walkSpeed";
	private static final String BIKE_SPEED = "bikeSpeed";
	private static final String UNDEFINED_MODE_SPEED = "undefinedModeSpeed";

	private static final String BEELINE_DISTANCE_FACTOR = "beelineDistanceFactor";

	private static final String NETWORK_MODES = "networkModes";
	private static final String TELEPORTED_MODE_SPEEDS = "teleportedModeSpeed_";
	private static final String TELEPORTED_MODE_FREESPEED_FACTORS = "teleportedModeFreespeedFactor_";

	private boolean defaultsCleared = false;
	private double beelineDistanceFactor = 1.3;
	// private double ptSpeed = 25.0 / 3.6; // 25.0 km/h --> m/s

	private Collection<String> networkModes = Arrays.asList(TransportMode.car, TransportMode.ride); 
	private Map<String, Double> teleportedModeSpeeds = new HashMap<String, Double>();
	private Map<String, Double> teleportedModeFreespeedFactors = new HashMap<String, Double>();

	public PlansCalcRouteConfigGroup() {
		super(GROUP_NAME);
		teleportedModeSpeeds.put(TransportMode.bike, 15.0 / 3.6); // 15.0 km/h --> m/s
		teleportedModeSpeeds.put(TransportMode.walk, 3.0 / 3.6); // 3.0 km/h --> m/s
		// I'm not sure if anyone needs the "undefined" mode. In particular, it doesn't do anything for modes which are
		// really unknown, it is just a mode called "undefined". michaz 02-2012
		//
		// The original design idea was that some upstream module would figure out expected travel times and travel distances
		// for any modes, and the simulation would teleport all those modes it does not know anything about.
		// With the travel times and travel distances given by the mode.  In practice, it seems that people can live better
		// with the concept that mobsim figures it out by itself.  Although it is a much less flexible design.  kai, jun'2012
		teleportedModeSpeeds.put(UNDEFINED, 50.0 / 3.6); 
		teleportedModeFreespeedFactors.put(TransportMode.pt, 2.0);
	}

	@Override
	public String getValue(final String key) {
		throw new IllegalArgumentException(key + ": getValue access disabled; use direct getter");
	}

	@Override
	public void addParam(final String key, final String value) {
		if (PT_SPEED_FACTOR.equals(key)) {
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
			setNetworkModes(Arrays.asList(CollectionUtils.stringToArray(value)));
		} else if (key.startsWith(TELEPORTED_MODE_SPEEDS)) {
			clearDefaults();
			setTeleportedModeSpeed(key.substring(TELEPORTED_MODE_SPEEDS.length()), Double.parseDouble(value));
		} else if (key.startsWith(TELEPORTED_MODE_FREESPEED_FACTORS)) {
			clearDefaults();
			setTeleportedModeFreespeedFactor(key.substring(TELEPORTED_MODE_FREESPEED_FACTORS.length()), Double.parseDouble(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	private void clearDefaults() {
		if (!defaultsCleared) {
			Logger.getLogger(this.getClass()).warn("setting any of the teleported mode speed parameters clears ALL default values; " +
					"make sure this is what you want.") ;
			teleportedModeSpeeds.clear();
			teleportedModeFreespeedFactors.clear();
			defaultsCleared = true;
		}
	}

	@Override
	public final Map<String, String> getParams() {
		Map<String, String> map = super.getParams();
		map.put( BEELINE_DISTANCE_FACTOR, Double.toString(this.getBeelineDistanceFactor()) );
		map.put( NETWORK_MODES, CollectionUtils.arrayToString(this.networkModes.toArray(new String[this.networkModes.size()])));
		for (Entry<String, Double> entry : teleportedModeSpeeds.entrySet()) {
			map.put( TELEPORTED_MODE_SPEEDS + entry.getKey(), String.valueOf(entry.getValue()));
		}
		for (Entry<String, Double> entry : teleportedModeFreespeedFactors.entrySet()) {
			map.put( TELEPORTED_MODE_FREESPEED_FACTORS + entry.getKey(), String.valueOf(entry.getValue()));
		}
		return map;
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
		for (Entry<String, Double> entry : teleportedModeSpeeds.entrySet()) { 
			map.put(TELEPORTED_MODE_SPEEDS + entry.getKey(), "Speed for a teleported mode based on beeline-distance: (<beeline distance> * beelineDistanceFactor) / speed. Insert a line like this for every such mode.");
		}
		for (Entry<String, Double> entry : teleportedModeFreespeedFactors.entrySet()) { 
			map.put(TELEPORTED_MODE_FREESPEED_FACTORS + entry.getKey(), "Free-speed factor for a teleported mode based on freespeed: freespeedFactor * <freespeed car travel time>. Insert a line like this for every such mode.");
		}
		return map;
	}

	public double getPtSpeedFactor() {
		return getTeleportedModeFreespeedFactors().get(TransportMode.pt);
	}

	public double getWalkSpeed() {
		return getTeleportedModeSpeeds().get(TransportMode.walk);
	}

	public void setWalkSpeed(final double walkSpeed) {
		setTeleportedModeSpeed(TransportMode.walk, walkSpeed);
	}

	public double getBikeSpeed() {
		return getTeleportedModeSpeeds().get(TransportMode.bike);
	}

	public void setBikeSpeed(final double bikeSpeed) {
		setTeleportedModeSpeed(TransportMode.bike, bikeSpeed);
	}

	public double getUndefinedModeSpeed() {
		return getTeleportedModeSpeeds().get(UNDEFINED);
	}

	public void setUndefinedModeSpeed(final double undefinedModeSpeed) {
		setTeleportedModeSpeed(UNDEFINED, undefinedModeSpeed);
	}

	public void setPtSpeedFactor(final double ptSpeedFactor) {
		setTeleportedModeFreespeedFactor(TransportMode.pt, ptSpeedFactor);
	}

	public double getBeelineDistanceFactor() {
		return this.beelineDistanceFactor;
	}

	public void setBeelineDistanceFactor(double beelineDistanceFactor) {
		this.beelineDistanceFactor = beelineDistanceFactor;
	}

	public double getPtSpeed() {
		return getTeleportedModeSpeeds().get(TransportMode.pt);
	}

	public void setPtSpeed(double ptSpeed) {
		setTeleportedModeSpeed(TransportMode.pt, ptSpeed);
	}

	public Collection<String> getNetworkModes() {
		return this.networkModes;
	}

	public void setNetworkModes(Collection<String> networkModes) {
		this.networkModes = networkModes;
	}

	public Map<String, Double> getTeleportedModeSpeeds() {
		return Collections.unmodifiableMap(this.teleportedModeSpeeds);
	}

	public Map<String, Double> getTeleportedModeFreespeedFactors() {
		return teleportedModeFreespeedFactors;
	}

	public void setTeleportedModeFreespeedFactor(String mode, double freespeedFactor) {
		clearDefaults();
		teleportedModeFreespeedFactors.put(mode, freespeedFactor);
	}

	public void setTeleportedModeSpeed(String mode, double speed) {
		clearDefaults();
		teleportedModeSpeeds.put(mode, speed);
	}

}
