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
	
	private static final String USELANES = "useLanes";
	private static final String USESIGNALSYSTMES = "useSignalsystems";
	private static final String USEROADPRICING = "useRoadpricing";
	
	
	private boolean useLanes = false;
	private boolean useSignalSystems = false;
	private boolean useRoadpricing = false;
	
	public ScenarioConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public void addParam(String key, String value) {
		if (USELANES.equalsIgnoreCase(key)){
			this.useLanes = Boolean.parseBoolean(value.trim());
		}
		else if (USESIGNALSYSTMES.equalsIgnoreCase(key)){
			this.useSignalSystems = Boolean.parseBoolean(value.trim());
		}
		else if (USEROADPRICING.equalsIgnoreCase(key)){
			this.useRoadpricing = Boolean.parseBoolean(value.trim());
		}
		else {
			throw new IllegalArgumentException(value + " is not a valid parameter value for key: "+ key + " of config group " + this.GROUP_NAME);
		}
	}

	@Override
	protected Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(USELANES, "Set this parameter to true if lanes should be used, false if not.");
		map.put(USESIGNALSYSTMES, "Set this parameter to true if signal systems should be used, false if not.");
		map.put(USEROADPRICING, "Set this parameter to true if roadpricing should be used, false if not.");
		return map;
	}

	@Override
	public String getValue(String key) {
		if (USELANES.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseLanes());
		}
		else if (USESIGNALSYSTMES.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseSignalSystems());
		}
		else if (USEROADPRICING.equalsIgnoreCase(key)){
			return Boolean.toString(this.isUseRoadpricing());
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected Map<String, String> getParams() {
		TreeMap<String, String> m = new TreeMap<String, String>();
		m.put(USELANES, getValue(USELANES));
		m.put(USESIGNALSYSTMES, getValue(USESIGNALSYSTMES));
		m.put(USEROADPRICING, getValue(USEROADPRICING));
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

}
