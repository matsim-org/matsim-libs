/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.ConfigGroup;

/**
 * @author smetzler, dziemke
 */
public final class BicycleConfigGroup extends ConfigGroup {
	// necessary to have this public
	// TODO Change to reflective config group

	public static final String GROUP_NAME = "bicycle";

	private static final String INPUT_NETWORK_ATTRIBUTE_FILE = "inputNetworkObjectattributeFile";
	private static final String INPUT_COMFORT = "marginalUtilityOfComfort_m";
	private static final String INPUT_INFRASTRUCTURE = "marginalUtilityOfInfrastructure_m";
	private static final String INPUT_GRADIENT = "marginalUtilityOfGradient_m_100m";
	private static final String MAX_BICYCLE_SPEED_FOR_ROUTING = "maxBicycleSpeedForRouting";
	private static final String BICYCLE_MODE = "bicycleMode";
	private static final String MOTORIZED_INTERACTION = "motorizedInteraction";
	
	public static enum BicycleScoringType {legBased, linkBased};

	private String networkAttFile = null;
	private double marginalUtilityOfComfort;
	private double marginalUtilityOfInfrastructure;
	private double marginalUtilityOfGradient;
	private BicycleScoringType bicycleScoringType = BicycleScoringType.legBased;
	private double maxBicycleSpeedForRouting = 25.0/3.6;
	private String bicycleMode = "bicycle";
	private boolean motorizedInteraction = false;
	
	public BicycleConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final void addParam(final String key, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;

		if (INPUT_NETWORK_ATTRIBUTE_FILE.equals(key)) {
			setNetworkAttFile(value);
		} else if (INPUT_COMFORT.equals(key)) {
			setMarginalUtilityOfComfort_m(Double.parseDouble(value));
		} else if (INPUT_INFRASTRUCTURE.equals(key)) {
			setMarginalUtilityOfInfrastructure_m(Double.parseDouble(value));
		} else if (INPUT_GRADIENT.equals(key)) {
			setMarginalUtilityOfGradient_m_100m(Double.parseDouble(value));
		} else if (MAX_BICYCLE_SPEED_FOR_ROUTING.equals(key)) {
			setMaxBicycleSpeedForRouting(Double.parseDouble(value));
		} else if (BICYCLE_MODE.equals(key)) {
			setBicycleMode(value);
		} else if (MOTORIZED_INTERACTION.equals(key)) {
			setMotorizedInteraction(Boolean.valueOf(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final String getValue(final String key) {
		if (INPUT_NETWORK_ATTRIBUTE_FILE.equals(key)) {
			return getNetworkAttFile();
		} else if (INPUT_COMFORT.equals(key)) {
			return Double.toString(getMarginalUtilityOfComfort_m());
		} else if (INPUT_INFRASTRUCTURE.equals(key)) {
			return Double.toString(getMarginalUtilityOfInfrastructure_m());
		} else if (INPUT_GRADIENT.equals(key)) {
			return Double.toString(getMarginalUtilityOfGradient_m_100m());
		} else if (MAX_BICYCLE_SPEED_FOR_ROUTING.equals(key)) {
			return Double.toString(getMaxBicycleSpeedForRouting());
		} else if (BICYCLE_MODE.equals(key)) {
			return getBicycleMode();
		} else if (MOTORIZED_INTERACTION.equals(key)) {
			return Boolean.toString(isMotorizedInteraction());
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<>();
		map.put(INPUT_NETWORK_ATTRIBUTE_FILE, getValue(INPUT_NETWORK_ATTRIBUTE_FILE));
		map.put(INPUT_COMFORT, getValue(INPUT_COMFORT));
		map.put(INPUT_INFRASTRUCTURE, getValue(INPUT_INFRASTRUCTURE));
		map.put(INPUT_GRADIENT, getValue(INPUT_GRADIENT));
		map.put(MAX_BICYCLE_SPEED_FOR_ROUTING, getValue(MAX_BICYCLE_SPEED_FOR_ROUTING));
		return map;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(INPUT_NETWORK_ATTRIBUTE_FILE, "Path to a file containing information for the network's links (required file format: ObjectAttributes).");
		map.put(INPUT_COMFORT, "marginalUtilityOfSurfacetype");
		map.put(INPUT_INFRASTRUCTURE, "marginalUtilityOfStreettype");
		map.put(INPUT_GRADIENT, "marginalUtilityOfGradient");
		map.put(MAX_BICYCLE_SPEED_FOR_ROUTING, "maxBicycleSpeed");
		return map;
	}
	void setNetworkAttFile(String file) {
		this.networkAttFile = file;
	}

	public String getNetworkAttFile() {
		return this.networkAttFile;
	}
	
	public void setMarginalUtilityOfComfort_m(final double value) {
		this.marginalUtilityOfComfort = value;
	}

	public double getMarginalUtilityOfComfort_m() {
		return this.marginalUtilityOfComfort;
	}
	
	public void setMarginalUtilityOfInfrastructure_m(final double value) {
		this.marginalUtilityOfInfrastructure = value;
	}

	public double getMarginalUtilityOfInfrastructure_m() {
		return this.marginalUtilityOfInfrastructure;
	}
	
	public void setMarginalUtilityOfGradient_m_100m(final double value) {
		this.marginalUtilityOfGradient = value;
	}

	public double getMarginalUtilityOfGradient_m_100m() {
		return this.marginalUtilityOfGradient;
	}
	
	public void setBicycleScoringType(final BicycleScoringType value) {
		this.bicycleScoringType = value;
	}

	public BicycleScoringType getBicycleScoringType() {
		return this.bicycleScoringType;
	}
	
	public void setMaxBicycleSpeedForRouting(final double value) {
		this.maxBicycleSpeedForRouting = value;
	}

	public double getMaxBicycleSpeedForRouting() {
		return this.maxBicycleSpeedForRouting;
	}
	
	public String getBicycleMode() {
		return this.bicycleMode;
	}

	public void setBicycleMode(String bicycleMode) {
		this.bicycleMode = bicycleMode;
	}

	public boolean isMotorizedInteraction() {
		return motorizedInteraction;
	}

	public void setMotorizedInteraction(boolean motorizedInteraction) {
		this.motorizedInteraction = motorizedInteraction;
	}
}