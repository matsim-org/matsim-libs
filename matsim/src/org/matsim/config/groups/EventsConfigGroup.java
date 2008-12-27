/* *********************************************************************** *
 * project: org.matsim.*
 * EventsConfigGroup.java
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

public class EventsConfigGroup extends Module {

	public static final String GROUP_NAME = "events";

	private static final String INPUT_FILE= "inputFile";

	private static final String INPUT_FORMAT= "inputFormat";
	private static final String OUTPUT_FORMAT = "outputFormat";
	private static final String OUTPUT_FILE = "outputFile";

	private String inputFile = null;

	private static final Logger log = Logger.getLogger(EventsConfigGroup.class);

	public EventsConfigGroup() {
		super(EventsConfigGroup.GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (EventsConfigGroup.INPUT_FILE.equals(key)) {
			return getInputFile();
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (EventsConfigGroup.INPUT_FILE.equals(key)) {
			setInputFile(value.replace('\\', '/'));
		} else if (EventsConfigGroup.OUTPUT_FORMAT.equals(key) || EventsConfigGroup.INPUT_FORMAT.equals(key) || EventsConfigGroup.OUTPUT_FILE.equals(key)) {
			log.info("The parameter " + key + " in module " + GROUP_NAME + " is no longer needed and should be removed from the configuration file.");
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		addParameterToMap(map, INPUT_FILE);
		return map;
	}

	/* direct access */

	public String getInputFile() {
		return this.inputFile;
	}
	public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}

}
