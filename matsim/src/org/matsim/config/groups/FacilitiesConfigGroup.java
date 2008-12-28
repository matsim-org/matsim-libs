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

package org.matsim.config.groups;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.config.Module;

public class FacilitiesConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "facilities";

	private static final String INPUT_FILE= "inputFacilitiesFile";
	private static final String LOCAL_INPUT_DTD = "localInputDTD";
	private static final String INPUT_VERSION = "inputVersion";
	private static final String OUTPUT_FILE = "outputFacilitiesFile";
	private static final String OUTPUT_DTD = "outputFacilitiesDTD";
	private static final String OUTPUT_VERSION = "outputVersion";

	private String inputFile = null;
	private String outputFile = null;

	private static final Logger log = Logger.getLogger(FacilitiesConfigGroup.class);

	public FacilitiesConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (INPUT_FILE.equals(key)) {
			return getInputFile();
		} else if (OUTPUT_FILE.equals(key)) {
			return getOutputFile();
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (INPUT_FILE.equals(key)) {
			setInputFile(value.replace('\\', '/'));
		} else if (OUTPUT_FILE.equals(key)) {
			setOutputFile(value.replace('\\', '/'));
		} else if (LOCAL_INPUT_DTD.equals(key) || INPUT_VERSION.equals(key) || OUTPUT_DTD.equals(key) || OUTPUT_VERSION.equals(key)) {
			log.info("The parameter " + key + " in module " + GROUP_NAME + " is no longer needed and should be removed from the configuration file.");
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		addParameterToMap(map, INPUT_FILE);
		addParameterToMap(map, OUTPUT_FILE);
		return map;
	}

	/* direct access */

	public String getInputFile() {
		return this.inputFile;
	}
	public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}

	public String getOutputFile() {
		return this.outputFile;
	}
	public void setOutputFile(final String outputFile) {
		this.outputFile = outputFile;
	}

}
