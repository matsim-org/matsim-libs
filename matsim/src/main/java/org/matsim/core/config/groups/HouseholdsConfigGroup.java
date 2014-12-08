/* *********************************************************************** *
 * project: org.matsim.*
 * PlansConfigGroup.java
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
 * Config group for households
 * @author dgrether
 */
public class HouseholdsConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "households";

	private static final String INPUT_FILE= "inputFile";
	private static final String INPUT_HOUSEHOLD_ATTRIBUTES_FILE = "inputHouseholdAttributesFile";

	private String inputFile = null;
	private String inputHouseholdAttributesFile = null;

	public HouseholdsConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (INPUT_FILE.equals(key)) {
			return getInputFile();
		} else if (INPUT_HOUSEHOLD_ATTRIBUTES_FILE.equals(key)) {
			return getInputHouseholdAttributesFile();
		} else {
			throw new IllegalArgumentException(key);			
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (INPUT_FILE.equals(key)) {
			setInputFile(value);
		} else if (INPUT_HOUSEHOLD_ATTRIBUTES_FILE.equals(key)) {
			setInputHouseholdAttributesFile(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		addParameterToMap(map, INPUT_FILE);
		addParameterToMap(map, INPUT_HOUSEHOLD_ATTRIBUTES_FILE);
		return map;
	}

	/* direct access */
	
	public String getInputFile() {
		return this.inputFile;
	}
	
	public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}
	
	public String getInputHouseholdAttributesFile() {
		return this.inputHouseholdAttributesFile;
	}

	public void setInputHouseholdAttributesFile(String inputHouseholdAttributesFile) {
		this.inputHouseholdAttributesFile = inputHouseholdAttributesFile;
	}
}
