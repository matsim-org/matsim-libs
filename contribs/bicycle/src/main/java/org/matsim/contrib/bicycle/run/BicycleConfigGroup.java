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
package org.matsim.contrib.bicycle.run;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.ConfigGroup;

/**
 * @author smetzler, dziemke
 */
public class BicycleConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "bicycle";

	private static final String INPUT_NETWORK_ATTRIBUTE_FILE = "inputNetworkObjectattributeFile";
	private static final String INPUT_COMFORT = "marginalUtilityOfComfort_m";
	private static final String INPUT_INFRASTRUCTURE = "marginalUtilityOfInfrastructure_m";
	private static final String INPUT_GRADIENT = "marginalUtilityOfGradient_m_100m";

	private String networkAttFile = null;
	private double marginalUtilityOfComfort;
	private double marginalUtilityOfInfrastructure;
	private double marginalUtilityOfGradient;
	
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
		return map;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(INPUT_NETWORK_ATTRIBUTE_FILE, "Path to a file containing information for the network's links (required file format: ObjectAttributes).");
		map.put(INPUT_COMFORT, "marginalUtilityOfSurfacetype");
		map.put(INPUT_INFRASTRUCTURE, "marginalUtilityOfStreettype");
		map.put(INPUT_GRADIENT, "marginalUtilityOfGradient");
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
}