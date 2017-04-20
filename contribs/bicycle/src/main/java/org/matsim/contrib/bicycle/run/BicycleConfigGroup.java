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

import org.matsim.core.config.ConfigGroup;

import java.util.Map;
import java.util.TreeMap;

public class BicycleConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "bike";

	private static final String INPUT_NETWORK_ATTRIBUTE_FILE = "inputNetworkObjectattributeFile";
//	private static final String INPUT_REFERENCE_BIKE_SPEED = "referenceBikeSpeed";
//	private static final String INPUT_COMFORT = "marginalUtilityOfComfort";
	private static final String INPUT_SURFACETYPE = "marginalUtilityOfSurfacetype";
	private static final String INPUT_STREETTYPE = "marginalUtilityOfStreettype";

	private String networkAttFile = null;
//	private double referenceBikeSpeed;
//	private double marginalUtilityOfComfort;
	private double marginalUtilityOfSurfacetype;
	private double marginalUtilityOfStreettype;
	
	public BicycleConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final void addParam(final String key, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;

		if (INPUT_NETWORK_ATTRIBUTE_FILE.equals(key)) {
			setNetworkAttFile(value);
//		} else if (INPUT_REFERENCE_BIKE_SPEED.equals(key)) {
//			setReferenceBikeSpeed(Double.parseDouble(value));
//		} else if (INPUT_COMFORT.equals(key)) {
//			setMarginalUtilityOfComfort(Double.parseDouble(value));
		} else if (INPUT_SURFACETYPE.equals(key)) {
			setMarginalUtilityOfSurfacetype(Double.parseDouble(value));
		} else if (INPUT_STREETTYPE.equals(key)) {
			setMarginalUtilityOfStreettype(Double.parseDouble(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final String getValue(final String key) {
		if (INPUT_NETWORK_ATTRIBUTE_FILE.equals(key)) {
			return getNetworkAttFile();
//		} else if (INPUT_REFERENCE_BIKE_SPEED.equals(key)) {
//			return Double.toString(getReferenceBikeSpeed());
//		} else if (INPUT_COMFORT.equals(key)) {
//			return Double.toString(getMarginalUtilityOfComfort());
		} else if (INPUT_SURFACETYPE.equals(key)) {
			return Double.toString(getMarginalUtilityOfSurfacetype());
		} else if (INPUT_STREETTYPE.equals(key)) {
			return Double.toString(getMarginalUtilityOfStreettype());
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<>();
		map.put(INPUT_NETWORK_ATTRIBUTE_FILE, getValue(INPUT_NETWORK_ATTRIBUTE_FILE));
//		map.put(INPUT_REFERENCE_BIKE_SPEED, getValue(INPUT_REFERENCE_BIKE_SPEED));
	//	map.put(INPUT_COMFORT, getValue(INPUT_COMFORT));
		map.put(INPUT_SURFACETYPE, getValue(INPUT_SURFACETYPE));
		map.put(INPUT_STREETTYPE, getValue(INPUT_STREETTYPE));
		return map;
	}


	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(INPUT_NETWORK_ATTRIBUTE_FILE, "Path to a file containing information for the network's links (required file format: ObjectAttributes).");
	//	map.put(INPUT_REFERENCE_BIKE_SPEED, "ReferenceBikeSpeed // 6.01 according to Prakin and Rotheram");
	//	map.put(INPUT_COMFORT, "MarginalUtilityOfComfort");
		map.put(INPUT_SURFACETYPE, "marginalUtilityOfSurfacetype");
		map.put(INPUT_STREETTYPE, "marginalUtilityOfStreettype");
		return map;
	}
	void setNetworkAttFile(String file) {
		this.networkAttFile = file;
	}

	public String getNetworkAttFile() {
		return this.networkAttFile;
	}

//	public void setReferenceBikeSpeed(final double value) {
//		this.referenceBikeSpeed = value;
//	}
//
//	public double getReferenceBikeSpeed() {
//		return this.referenceBikeSpeed;
//	}
//	
//	public void setMarginalUtilityOfComfort(final double value) {
//		this.marginalUtilityOfComfort = value;
//	}
//
//	public double getMarginalUtilityOfComfort() {
//		return this.marginalUtilityOfComfort;
//	}
	
	public void setMarginalUtilityOfSurfacetype(final double value) {
		this.marginalUtilityOfSurfacetype = value;
	}

	public double getMarginalUtilityOfSurfacetype() {
		return this.marginalUtilityOfSurfacetype;
	}
	
	public void setMarginalUtilityOfStreettype(final double value) {
		this.marginalUtilityOfStreettype = value;
	}

	public double getMarginalUtilityOfStreettype() {
		return this.marginalUtilityOfStreettype;
	}
}