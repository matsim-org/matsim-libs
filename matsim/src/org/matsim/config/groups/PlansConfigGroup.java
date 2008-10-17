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

package org.matsim.config.groups;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.config.Module;

public class PlansConfigGroup extends Module {

	public static final String GROUP_NAME = "plans";

	private static final String SWITCH_OFF_PLANS_STREAMING = "switchOffPlansStreaming";
	private static final String OUTPUT_SAMPLE = "outputSample";

	private static final String INPUT_FILE= "inputPlansFile";
	private static final String OUTPUT_FILE = "outputPlansFile";
	private static final String OUTPUT_VERSION = "outputVersion";

	private static final String LOCAL_INPUT_DTD = "localInputDTD";
	private static final String INPUT_VERSION = "inputVersion";
	private static final String OUTPUT_DTD = "outputPlansDTD";

	private boolean switchOffPlansStreaming = true;
	private double outputSample = 1.0;

	private String inputFile = null;
	private String outputFile = null;
	private String outputVersion = "v4"; // use the newest version by default

	private static final Logger log = Logger.getLogger(PlansConfigGroup.class);

	public PlansConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (SWITCH_OFF_PLANS_STREAMING.equals(key)) {
			return switchOffPlansStreaming() ? "yes" : "no";
		} else if (OUTPUT_SAMPLE.equals(key)) {
			return Double.toString(getOutputSample());
		} else if (INPUT_FILE.equals(key)) {
			return getInputFile();
		} else if (OUTPUT_FILE.equals(key)) {
			return getOutputFile();
		} else if (OUTPUT_VERSION.equals(key)) {
			return getOutputVersion();
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (SWITCH_OFF_PLANS_STREAMING.equals(key)) {
			switchOffPlansStreaming("yes".equals(value) || "true".equals(value));
		} else if (OUTPUT_SAMPLE.equals(key)) {
			setOutputSample(Double.parseDouble(value));
		} else if (INPUT_FILE.equals(key)) {
			setInputFile(value.replace('\\', '/'));
		} else if (OUTPUT_FILE.equals(key)) {
			setOutputFile(value.replace('\\', '/'));
		} else if (OUTPUT_VERSION.equals(key)) {
			setOutputVersion(value);
		} else if (LOCAL_INPUT_DTD.equals(key) || INPUT_VERSION.equals(key) || OUTPUT_DTD.equals(key)) {
			log.info("The parameter " + key + " in module " + GROUP_NAME + " is no longer needed and should be removed from the configuration file.");
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(SWITCH_OFF_PLANS_STREAMING, getValue(SWITCH_OFF_PLANS_STREAMING));
		map.put(OUTPUT_SAMPLE, getValue(OUTPUT_SAMPLE));
		addParameterToMap(map, INPUT_FILE);
		addParameterToMap(map, OUTPUT_FILE);
		addParameterToMap(map, OUTPUT_VERSION);
		return map;
	}

	/* direct access */

	public boolean switchOffPlansStreaming() {
		return this.switchOffPlansStreaming;
	}
	public void switchOffPlansStreaming(final boolean switchOffPlansStreaming) {
		this.switchOffPlansStreaming = switchOffPlansStreaming;
	}

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
