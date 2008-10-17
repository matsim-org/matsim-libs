/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigConfigGroup.java
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

public class ConfigConfigGroup extends Module {

	public static final String GROUP_NAME = "config";

	private static final String OUTPUT_FILE = "outputConfigFile";
	private static final String OUTPUT_DTD = "outputConfigDTD";
	private static final String OUTPUT_VERSION = "outputVersion";

	private String outputFile = null;

	private static final Logger log = Logger.getLogger(ConfigConfigGroup.class);

	public ConfigConfigGroup() {
		super(ConfigConfigGroup.GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (ConfigConfigGroup.OUTPUT_FILE.equals(key)) {
			return getOutputFile();
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (ConfigConfigGroup.OUTPUT_FILE.equals(key)) {
			setOutputFile(value.replace('\\', '/'));
		} else if (ConfigConfigGroup.OUTPUT_DTD.equals(key) || ConfigConfigGroup.OUTPUT_VERSION.equals(key)) {
			log.info("The parameter " + key + " in module " + GROUP_NAME + " is no longer needed and should be removed from the configuration file.");
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		addParameterToMap(map, OUTPUT_FILE);
		return map;
	}

	/* direct access */

	public String getOutputFile() {
		return this.outputFile;
	}
	public void setOutputFile(final String outputFile) {
		this.outputFile = outputFile;
	}

}
