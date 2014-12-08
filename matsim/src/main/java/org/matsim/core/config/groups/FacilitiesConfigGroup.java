/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import java.util.TreeMap;

import org.matsim.core.config.ConfigGroup;

/**
 * @author mrieser / Senozon AG
 */
public class FacilitiesConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "facilities";

	private static final String INPUT_FILE= "inputFacilitiesFile";
	private static final String INPUT_FACILITY_ATTRIBUTES_FILE = "inputFacilityAttributesFile";

	private String inputFile = null;
	private String inputFacilitiesAttributesFile = null;

	public FacilitiesConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (INPUT_FILE.equals(key)) {
			return getInputFile();
		} else if (INPUT_FACILITY_ATTRIBUTES_FILE.equals(key)) {
			return getInputFacilitiesAttributesFile();
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (INPUT_FILE.equals(key)) {
			setInputFile(value);
		} else if (INPUT_FACILITY_ATTRIBUTES_FILE.equals(key)) {
			setInputFacilitiesAttributesFile(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		addParameterToMap(map, INPUT_FILE);
		addParameterToMap(map, INPUT_FACILITY_ATTRIBUTES_FILE);
		return map;
	}

	/* direct access */

	public String getInputFile() {
		return this.inputFile;
	}
	public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}

	public String getInputFacilitiesAttributesFile() {
		return this.inputFacilitiesAttributesFile;
	}

	public void setInputFacilitiesAttributesFile(String inputFacilitiesAttributesFile) {
		this.inputFacilitiesAttributesFile = inputFacilitiesAttributesFile;
	}

}
