/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioConfigGroup
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

import org.matsim.core.config.ConfigGroup;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author dgrether
 *
 */
public class ScenarioConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "scenario";

	private static final String USE_LANES = "useLanes";
	private static final String USE_SIGNALSYSTMES = "useSignalsystems";
	private static final String USE_VEHICLES = "useVehicles";
	private static final String USE_HOUSEHOLDS = "useHouseholds";
	private static final String USE_TRANSIT = "useTransit";

	private boolean useLanes = false;
	private boolean useSignalSystems = false;
	private boolean useHouseholds = false;
	private boolean useVehicles = false;
	private boolean useTransit = false;


	public ScenarioConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (USE_LANES.equalsIgnoreCase(key)){
			this.useLanes = Boolean.parseBoolean(value.trim());
		}
		else if (USE_SIGNALSYSTMES.equalsIgnoreCase(key)){
			this.useSignalSystems = Boolean.parseBoolean(value.trim());
		}
		else if (USE_VEHICLES.equalsIgnoreCase(key)){
			this.useVehicles = Boolean.parseBoolean(value.trim());
		}
		else if (USE_HOUSEHOLDS.equalsIgnoreCase(key)){
			this.useHouseholds = Boolean.parseBoolean(value.trim());
		}
		else if (USE_TRANSIT.equalsIgnoreCase(key)){
			this.useTransit = Boolean.parseBoolean(value.trim());
		}
		else {
			throw new IllegalArgumentException(key + " is not a valid parameter of config group " + GROUP_NAME);
		}
	}

	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(USE_LANES, "Set this parameter to true if lanes should be used, false if not.");
		map.put(USE_SIGNALSYSTMES, "Set this parameter to true if signal systems should be used, false if not.");
		map.put(USE_HOUSEHOLDS, "Set this parameter to true if households should be used, false if not.");
		map.put(USE_VEHICLES, "Set this parameter to true if vehicles should be used, false if not.");
		map.put(USE_TRANSIT, "Set this parameter to true if transit should be simulated, false if not.");
		return map;
	}

	@Override
	public String getValue(final String key) {
		if (USE_LANES.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseLanes());
		}
		else if (USE_SIGNALSYSTMES.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseSignalSystems());
		}
		else if (USE_VEHICLES.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseVehicles());
		}
		else if (USE_HOUSEHOLDS.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseHouseholds());
		}
		else if (USE_TRANSIT.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseTransit());
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public Map<String, String> getParams() {
		TreeMap<String, String> m = new TreeMap<>();
		m.put(USE_LANES, getValue(USE_LANES));
		m.put(USE_SIGNALSYSTMES, getValue(USE_SIGNALSYSTMES));
		m.put(USE_VEHICLES, this.getValue(USE_VEHICLES));
		m.put(USE_HOUSEHOLDS, this.getValue(USE_HOUSEHOLDS));
		m.put(USE_TRANSIT, this.getValue(USE_TRANSIT));
		return m;
	}

	public boolean isUseLanes() {
		return this.useLanes;
	}

	public void setUseLanes(final boolean useLanes) {
		this.useLanes = useLanes;
	}

	public boolean isUseSignalSystems() {
		return this.useSignalSystems;
	}

	public void setUseSignalSystems(final boolean useSignalSystems) {
		this.useSignalSystems = useSignalSystems;
	}

	public boolean isUseHouseholds() {
		return this.useHouseholds;
	}

	public boolean isUseVehicles() {
		return this.useVehicles;
	}

	public boolean isUseTransit() {
		return this.useTransit;
	}

	public void setUseHouseholds(final boolean b) {
		this.useHouseholds = b;
	}

	public void setUseVehicles(final boolean b) {
		this.useVehicles = b;
	}

    public void setUseTransit(final boolean b) {
		this.useTransit = b;
	}

}
