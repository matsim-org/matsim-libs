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
	
	private static final String INPUT_SURFACE_INFORMATION_FILE = "inputSurfaceInformationFile";
	
	private String surfaceInformationFile = null;


	public BikeConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final void addParam(final String key, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;
		
		if (INPUT_SURFACE_INFORMATION_FILE.equals(key)) {
			setSurfaceInformationFile(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}
	
	@Override
	public final String getValue(final String key) {
		if (INPUT_SURFACE_INFORMATION_FILE.equals(key)) {
			return getSurfaceInformationFile();
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<>();
		map.put(INPUT_SURFACE_INFORMATION_FILE, getValue(INPUT_SURFACE_INFORMATION_FILE));
		return map;
	}
	
	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(INPUT_SURFACE_INFORMATION_FILE, "Path to a file containing surface information for the network's links (required file format: ObjectAttributes).");
		return map;
	}
	void setSurfaceInformationFile(String file) {
		this.surfaceInformationFile = file;
	}
	
	public String getSurfaceInformationFile() {
		return this.surfaceInformationFile;
	}
}