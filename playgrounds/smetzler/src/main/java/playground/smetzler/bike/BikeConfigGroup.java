package playground.smetzler.bike;



/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


import org.matsim.core.config.ConfigGroup;

import java.util.Map;
import java.util.TreeMap;

public class BikeConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "bike";

	private static final String INPUT_NETWORK_ATTRIBUTE_FILE = "inputNetworkObjectattributeFile";
	private static final String INPUT_REFERENCE_BIKE_SPEED = "referenceBikeSpeed";
	private static final String INPUT_COMFORT = "marginalUtilityOfComfort";



	private String networkAttFile = null;
	private double referenceBikeSpeed;
	private double marginalUtilityOfComfort;
	


	public BikeConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final void addParam(final String key, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;

		if (INPUT_NETWORK_ATTRIBUTE_FILE.equals(key)) {
			setNetworkAttFile(value);
		} else if (INPUT_REFERENCE_BIKE_SPEED.equals(key)) {
			setReferenceBikeSpeed(Double.parseDouble(value));
		} else if (INPUT_COMFORT.equals(key)) {
			setMarginalUtilityOfComfort(Double.parseDouble(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}


	@Override
	public final String getValue(final String key) {
		if (INPUT_NETWORK_ATTRIBUTE_FILE.equals(key)) {
			return getNetworkAttFile();
		} else if (INPUT_REFERENCE_BIKE_SPEED.equals(key)) {
			return Double.toString(getReferenceBikeSpeed());
		} else if (INPUT_COMFORT.equals(key)) {
			return Double.toString(getMarginalUtilityOfComfort());
		} else {
			throw new IllegalArgumentException(key);
		}
	}



	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<>();
		map.put(INPUT_NETWORK_ATTRIBUTE_FILE, getValue(INPUT_NETWORK_ATTRIBUTE_FILE));
		map.put(INPUT_REFERENCE_BIKE_SPEED, getValue(INPUT_REFERENCE_BIKE_SPEED));
		map.put(INPUT_COMFORT, getValue(INPUT_COMFORT));
		return map;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(INPUT_NETWORK_ATTRIBUTE_FILE, "Path to a file containing information for the network's links (required file format: ObjectAttributes).");
		map.put(INPUT_REFERENCE_BIKE_SPEED, "ReferenceBikeSpeed // 6.01 according to Prakin and Rotheram");
		map.put(INPUT_COMFORT, "MarginalUtilityOfComfort");

		return map;
	}
	void setNetworkAttFile(String file) {
		this.networkAttFile = file;
	}

	public String getNetworkAttFile() {
		return this.networkAttFile;
	}

	public void setReferenceBikeSpeed(final double value) {
		this.referenceBikeSpeed = value;
	}

	public double getReferenceBikeSpeed() {
		return this.referenceBikeSpeed;
	}
	
	public void setMarginalUtilityOfComfort(final double value) {
		this.marginalUtilityOfComfort = value;
	}

	public double getMarginalUtilityOfComfort() {
		return this.marginalUtilityOfComfort;
	}

}