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

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.Module;

public class PlansConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "plans";

	private static final String OUTPUT_SAMPLE = "outputSample";

	private static final String INPUT_FILE= "inputPlansFile";
	private static final String OUTPUT_FILE = "outputPlansFile";
	private static final String OUTPUT_VERSION = "outputVersion";

	private double outputSample = 1.0;

	private String inputFile = null;
	private String outputFile = null;
	private String outputVersion = "v4"; // use the newest version by default

	public PlansConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(OUTPUT_FILE, "Path where plans file should be written to.  As far as I know, this variable is ignored when the controler is used.");
		return map;
	}

	@Override
	public String getValue(final String key) {
		if (OUTPUT_SAMPLE.equals(key)) {
			return Double.toString(getOutputSample());
		} else if (INPUT_FILE.equals(key)) {
			return getInputFile();
		} else if (OUTPUT_FILE.equals(key)) {
			return getOutputFile();
		} else if (OUTPUT_VERSION.equals(key)) {
			return getOutputVersion();
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (OUTPUT_SAMPLE.equals(key)) {
			setOutputSample(Double.parseDouble(value));
		} else if (INPUT_FILE.equals(key)) {
			setInputFile(value.replace('\\', '/'));
		} else if (OUTPUT_FILE.equals(key)) {
			setOutputFile(value.replace('\\', '/'));
		} else if (OUTPUT_VERSION.equals(key)) {
			setOutputVersion(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(OUTPUT_SAMPLE, getValue(OUTPUT_SAMPLE));
		addParameterToMap(map, INPUT_FILE);
		addParameterToMap(map, OUTPUT_FILE);
		addParameterToMap(map, OUTPUT_VERSION);
		return map;
	}

	/* direct access */

	public double getOutputSample() {
		return this.outputSample;
	}
	public void setOutputSample(final double outputSample) {
		this.outputSample = outputSample;
	}

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

	public String getOutputVersion() {
		return this.outputVersion;
	}
	public void setOutputVersion(final String outputVersion) {
		this.outputVersion = outputVersion;
	}
}
