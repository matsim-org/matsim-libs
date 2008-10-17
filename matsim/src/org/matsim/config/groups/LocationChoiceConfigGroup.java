/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoiceConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.matsim.config.Module;

public class LocationChoiceConfigGroup extends Module {

	public static final String GROUP_NAME = "locationchoice";
	
	// zh, ch
	// TODO: Provide center coord + radius to define an area for 
	// locatio choice. For the moment zh and ch scenario.
	private static final String AREA = "area";
	
	// true; false
	private static final String CONSTRAINED = "constrained";

	private String area = null;
	private String constrained = null;

	public LocationChoiceConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (AREA.equals(key)) {
			return getArea();
		}
		if (CONSTRAINED.equals(key)) {
			return getMode();
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (AREA.equals(key)) {
			setArea(value);
		} else if (CONSTRAINED.equals(key)) {
			setMode(value);
		} else		
		{
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		this.addParameterToMap(map, AREA);
		this.addParameterToMap(map, CONSTRAINED);
		return map;
	}

	/* direct access */

	public String getArea() {
		return this.area;
	}
	public void setArea(final String area) {
		this.area = area;
	}
	
	public String getMode() {
		return this.constrained;
	}
	public void setMode(final String constrained) {
		this.constrained = constrained;
	}
	
}
