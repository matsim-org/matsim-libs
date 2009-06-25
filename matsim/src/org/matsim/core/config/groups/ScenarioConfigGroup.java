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

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.Module;


/**
 * @author dgrether
 *
 */
public class ScenarioConfigGroup extends Module {

	private static final long serialVersionUID = -1279388236689564520L;

	public static final String GROUP_NAME = "scenario";
	
	private static final String USE_LANES = "useLanes";
	private static final String USE_SIGNALSYSTMES = "useSignalsystems";
	private static final String USE_ROADPRICING = "useRoadpricing";
	private static final String USE_VEHICLES = "useVehicles";
	private static final String USE_HOUSEHOLDS = "useHouseholds";
	private static final String USE_KNOWLEDGE = "useKnowledge";
	
	private boolean useLanes = false;
	private boolean useSignalSystems = false;
	private boolean useRoadpricing = false;
	private boolean useHouseholds = false;
	private boolean useVehicles = false;
	private boolean useKnowledge = true;
	
	
	public ScenarioConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public void addParam(String key, String value) {
		if (USE_LANES.equalsIgnoreCase(key)){
			this.useLanes = Boolean.parseBoolean(value.trim());
		}
		else if (USE_SIGNALSYSTMES.equalsIgnoreCase(key)){
			this.useSignalSystems = Boolean.parseBoolean(value.trim());
		}
		else if (USE_ROADPRICING.equalsIgnoreCase(key)){
			this.useRoadpricing = Boolean.parseBoolean(value.trim());
		}
		else if (USE_VEHICLES.equalsIgnoreCase(key)){
			this.useVehicles = Boolean.parseBoolean(value.trim());
		}
		else if (USE_HOUSEHOLDS.equalsIgnoreCase(key)){
			this.useHouseholds = Boolean.parseBoolean(value.trim());
		}
		else if (USE_KNOWLEDGE.equalsIgnoreCase(key)){
			this.useKnowledge = Boolean.parseBoolean(value.trim());
		}
		else {
			throw new IllegalArgumentException(value + " is not a valid parameter value for key: "+ key + " of config group " + this.GROUP_NAME);
		}
	}

	@Override
	protected Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(USE_LANES, "Set this parameter to true if lanes should be used, false if not.");
		map.put(USE_SIGNALSYSTMES, "Set this parameter to true if signal systems should be used, false if not.");
		map.put(USE_ROADPRICING, "Set this parameter to true if roadpricing should be used, false if not.");
		map.put(USE_KNOWLEDGE, "Set this parameter to true if knowledge should be used, false if not.");
		map.put(USE_HOUSEHOLDS, "Set this parameter to true if households should be used, false if not.");
		map.put(USE_VEHICLES, "Set this parameter to true if vehicles should be used, false if not.");
		return map;
	}

	@Override
	public String getValue(String key) {
		if (USE_LANES.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseLanes());
		}
		else if (USE_SIGNALSYSTMES.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseSignalSystems());
		}
		else if (USE_ROADPRICING.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseRoadpricing());
		}
		else if (USE_KNOWLEDGE.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseKnowledges());
		}
		else if (USE_VEHICLES.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseVehicles());
		}
		else if (USE_HOUSEHOLDS.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseHouseholds());
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected Map<String, String> getParams() {
		TreeMap<String, String> m = new TreeMap<String, String>();
		m.put(USE_LANES, getValue(USE_LANES));
		m.put(USE_SIGNALSYSTMES, getValue(USE_SIGNALSYSTMES));
		m.put(USE_ROADPRICING, getValue(USE_ROADPRICING));
		m.put(USE_VEHICLES, this.getValue(USE_VEHICLES));
		m.put(USE_HOUSEHOLDS, this.getValue(USE_HOUSEHOLDS));
		m.put(USE_KNOWLEDGE, this.getValue(USE_KNOWLEDGE));
		return m;
	}
	
	public boolean isUseLanes() {
		return useLanes;
	}
	
	public void setUseLanes(boolean useLanes) {
		this.useLanes = useLanes;
	}

	
	public boolean isUseSignalSystems() {
		return useSignalSystems;
	}

	
	public void setUseSignalSystems(boolean useSignalSystems) {
		this.useSignalSystems = useSignalSystems;
	}

	
	public boolean isUseRoadpricing() {
		return useRoadpricing;
	}

	
	public void setUseRoadpricing(boolean useRoadpricing) {
		this.useRoadpricing = useRoadpricing;
	}

	public boolean isUseKnowledges() {
		return this.useKnowledge;
	}

	public boolean isUseHouseholds() {
		return this.useHouseholds;
	}

	public boolean isUseVehicles() {
		return this.useVehicles;
	}

	public void setUseHouseholds(boolean b) {
		this.useHouseholds = b;
	}
	
	public void setUseVehicles(boolean b){
		this.useVehicles = b;
	}
	
	public void setUseKnowledge(boolean b){
		this.useKnowledge = b;
	}
}
