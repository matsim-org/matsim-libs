/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkConfigGroup.java
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

public class NetworkConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "network";

	private static final String INPUT_FILE= "inputNetworkFile";

	private static final String TIME_VARIANT_NETWORK = "timeVariantNetwork";
	private static final String CHANGE_EVENTS_INPUT_FILE = "inputChangeEventsFile";

	private static final String LANEDEFINITIONSINPUTFILE = "laneDefinitionsFile";

	private String inputFile = null;

	private String changeEventsInputFile = null;

	private boolean timeVariantNetwork = false;

	private String laneDefinitionsFile = null;

	public NetworkConfigGroup() {
		super(NetworkConfigGroup.GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (NetworkConfigGroup.INPUT_FILE.equals(key)) {
			return getInputFile();
		} else if (NetworkConfigGroup.CHANGE_EVENTS_INPUT_FILE.equals(key)) {
			return getChangeEventsInputFile();
		} else if (NetworkConfigGroup.TIME_VARIANT_NETWORK.equals(key)) {
			return isTimeVariantNetwork() ? "true" : "false";
		} else if (NetworkConfigGroup.LANEDEFINITIONSINPUTFILE.equals(key)){
			return getLaneDefinitionsFile();
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (NetworkConfigGroup.INPUT_FILE.equals(key)) {
			setInputFile(value);
		} else if (NetworkConfigGroup.CHANGE_EVENTS_INPUT_FILE.equals(key)) {
			setChangeEventInputFile(value);
		} else if (NetworkConfigGroup.TIME_VARIANT_NETWORK.equals(key)) {
			setTimeVariantNetwork("true".equals(value) || "yes".equals(value));
		} else if (NetworkConfigGroup.LANEDEFINITIONSINPUTFILE.equals(key)){
			setLaneDefinitionsFile(value);
		}	else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(NetworkConfigGroup.INPUT_FILE, getValue(NetworkConfigGroup.INPUT_FILE));
		map.put(NetworkConfigGroup.CHANGE_EVENTS_INPUT_FILE, getValue(NetworkConfigGroup.CHANGE_EVENTS_INPUT_FILE));
		map.put(NetworkConfigGroup.TIME_VARIANT_NETWORK, getValue(NetworkConfigGroup.TIME_VARIANT_NETWORK));
		map.put(NetworkConfigGroup.LANEDEFINITIONSINPUTFILE, getValue(NetworkConfigGroup.LANEDEFINITIONSINPUTFILE));
		return map;
	}

	/* direct access */

	public String getInputFile() {
		return this.inputFile;
	}
	public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}

	public void setChangeEventInputFile(final String changeEventsInputFile) {
		this.changeEventsInputFile = changeEventsInputFile;
	}
	public String getChangeEventsInputFile() {
		return this.changeEventsInputFile;
	}

	public void setTimeVariantNetwork(final boolean timeVariantNetwork) {
		this.timeVariantNetwork = timeVariantNetwork;
	}
	public boolean isTimeVariantNetwork() {
		return this.timeVariantNetwork;
	}

	public void setLaneDefinitionsFile(final String laneDefinitions) {
		this.laneDefinitionsFile = laneDefinitions;
	}

	public String getLaneDefinitionsFile(){
		return this.laneDefinitionsFile;
	}


}
