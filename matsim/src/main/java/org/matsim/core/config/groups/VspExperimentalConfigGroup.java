/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;

/**
 * config group for experimental parameters. this group and its parameters should not be used outside of vsp.
 * @author dgrether
 */
public class VspExperimentalConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "vspExperimental";

	private static final String USE_ACTIVITY_DURATIONS = "useActivityDurations";

	private boolean useActivityDurations = true;

	private static final Logger log = Logger.getLogger(VspExperimentalConfigGroup.class);

	public VspExperimentalConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	protected Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(USE_ACTIVITY_DURATIONS, "Set this flag to false if the duration attribute of the activity should not be considered in QueueSimulation");
		return map;
	}

	@Override
	public String getValue(final String key) {
		if (USE_ACTIVITY_DURATIONS.equalsIgnoreCase(key)) {
			return Boolean.toString(this.isUseActivityDurations());
		}
			throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (USE_ACTIVITY_DURATIONS.equalsIgnoreCase(key)){
			this.setUseActivityDurations(Boolean.parseBoolean(value));
		} 
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(USE_ACTIVITY_DURATIONS, getValue(USE_ACTIVITY_DURATIONS));
		return map;
	}

	public boolean isUseActivityDurations() {
		return useActivityDurations;
	}

	public void setUseActivityDurations(boolean useActivityDurations) {
		this.useActivityDurations = useActivityDurations;
	}
	
}
