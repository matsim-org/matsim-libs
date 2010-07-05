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

package org.matsim.core.config.groups;

import java.util.TreeMap;

import org.matsim.core.config.Module;

public class ConfigConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "config";

	private static final String OUTPUT_FILE = "outputConfigFile";

	private String outputFile = null;


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
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
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
