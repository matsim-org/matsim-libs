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
	
	// true; false
	private static final String CONSTRAINED = "constrained";	
	private static final String RESTR_FCN_FACTOR = "restraintFcnFactor";
	private static final String RESTR_FCN_EXP = "restraintFcnExp";
	private static final String SCALEFACTOR = "scalefactor";

	private String constrained = null;
	private String restraintFcnFactor = null;
	private String restraintFcnExp = null;
	private String scalefactor = null;

	public LocationChoiceConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (CONSTRAINED.equals(key)) {
			return getMode();
		}
		if (RESTR_FCN_FACTOR.equals(key)) {
			return getRestraintFcnFactor();
		}
		if (RESTR_FCN_EXP.equals(key)) {
			return getRestraintFcnExp();
		}
		if (SCALEFACTOR.equals(key)) {
			return getScalefactor();
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (CONSTRAINED.equals(key)) {
			setMode(value);
		} else if (RESTR_FCN_FACTOR.equals(key)) {
			setRestraintFcnFactor(value);
		} else if (RESTR_FCN_EXP.equals(key)) {
			setRestraintFcnExp(value);
		} else if (SCALEFACTOR.equals(key)) {
			setScalefactor(value);
		} else		
		{
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		this.addParameterToMap(map, CONSTRAINED);
		this.addParameterToMap(map, RESTR_FCN_FACTOR);
		this.addParameterToMap(map, RESTR_FCN_EXP);
		this.addParameterToMap(map, SCALEFACTOR);
		return map;
	}


	public String getMode() {
		return this.constrained;
	}
	public void setMode(final String constrained) {
		this.constrained = constrained;
	}
	public String getRestraintFcnFactor() {
		return this.restraintFcnFactor;
	}
	public void setRestraintFcnFactor(final String restraintFcnFactor) {
		this.restraintFcnFactor = restraintFcnFactor;
	}
	public String getRestraintFcnExp() {
		return this.restraintFcnExp;
	}
	public void setRestraintFcnExp(final String restraintFcnExp) {
		this.restraintFcnExp = restraintFcnExp;
	}
	public String getScalefactor() {
		return this.scalefactor;
	}
	public void setScalefactor(final String scalefactor) {
		this.scalefactor = scalefactor;
	}
}
