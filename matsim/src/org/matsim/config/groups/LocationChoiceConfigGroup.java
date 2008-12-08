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
import org.apache.log4j.Logger;
import org.matsim.config.Module;


public class LocationChoiceConfigGroup extends Module {

	public static final String GROUP_NAME = "locationchoice";
	
	// true; false
	private static final String CONSTRAINED = "constrained";	
	private static final String RESTR_FCN_FACTOR = "restraintFcnFactor";
	private static final String RESTR_FCN_EXP = "restraintFcnExp";
	private static final String SCALEFACTOR = "scaleFactor";
	private static final String RECURSIONTRAVELSPEEDCHANGE = "recursionTravelSpeedChange";
	private static final String RECURSIONTRAVELSPEED = "recursionTravelSpeed";
	private static final String MAX_RECURSIONS = "maxRecursions";

	//default values
	private String constrained = "false";
	private String restraintFcnFactor = "0.0";
	private String restraintFcnExp = "0.0";
	private String scaleFactor = "1";
	private String recursionTravelSpeedChange = "0.1";
	private String recursionTravelSpeed = "8.5";
	private String maxRecursions = "0";
	
	private final static Logger log = Logger.getLogger(LocationChoiceConfigGroup.class);
	

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
			return getScaleFactor();
		}
		if (RECURSIONTRAVELSPEEDCHANGE.equals(key)) {
			return getRecursionTravelSpeedChange();
		}
		if (RECURSIONTRAVELSPEED.equals(key)) {
			return getRecursionTravelSpeed();
		}
		if (MAX_RECURSIONS.equals(key)) {
			return getMaxRecursions();
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (CONSTRAINED.equals(key)) {
			if (!(value.equals("true") || value.equals("false"))) {
				log.warn("set 'constrained' to either 'true' or 'false'. Set to default value 'false'");
				setMode("false");
			}
			else {
				setMode(value);
			}
			
			
		} else if (RESTR_FCN_FACTOR.equals(key)) {
			if (Double.parseDouble(value) < 0.0) {
				log.warn("Restraint function factor is negative! " +
						"This means: The more people are in a facility, the more attractive the facility is expected to be");
			}
			setRestraintFcnFactor(value);
			
			
		} else if (RESTR_FCN_EXP.equals(key)) {
			if (Double.parseDouble(value) < 0.0) {
				log.warn("Restraint function exponent is negative! " +
						"This means: The penalty gets smaller the more people are in a facility.");
			}
			setRestraintFcnExp(value);
			
		} else if (SCALEFACTOR.equals(key)) {
			if (Double.parseDouble(value) < 1) {
				log.warn("Scale factor must be greater than 1! Scale factor is set to default value 1");
				setScaleFactor("1");
			}
			else {
				setScaleFactor(value);
			}
			
		} else if (RECURSIONTRAVELSPEEDCHANGE.equals(key)) {
			if (Double.parseDouble(value) < 0.0 || Double.parseDouble(value) > 1.0 ) {
				log.warn("'recursionTravelSpeedChange' must be [0..1]! Set to default value 0.1");
				setRecursionTravelSpeedChange("0.1");
			}
			else {
				setRecursionTravelSpeedChange(value);
			}
		} else if (RECURSIONTRAVELSPEED.equals(key)) {
			if (Double.parseDouble(value) < 0.0 ) {
				log.warn("'recursionTravelSpeed' must be positive! Set to default value 8.5");
				setRecursionTravelSpeed("8.5");
			}
			else {
				setRecursionTravelSpeed(value);
			}
			
		} else if (MAX_RECURSIONS.equals(key)) {
			if (Double.parseDouble(value) < 0.0) {
				log.warn("'max_recursions' must be greater than 0! Set to default value 10");
				setMaxRecursions("10");
			}
			else {
				setMaxRecursions(value);
			}
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
		this.addParameterToMap(map, RECURSIONTRAVELSPEEDCHANGE);
		this.addParameterToMap(map, RECURSIONTRAVELSPEED);
		this.addParameterToMap(map, MAX_RECURSIONS);
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
	public String getScaleFactor() {
		return this.scaleFactor;
	}
	public void setScaleFactor(final String scaleFactor) {
		this.scaleFactor = scaleFactor;
	}
	public String getRecursionTravelSpeedChange() {
		return recursionTravelSpeedChange;
	}
	public void setRecursionTravelSpeedChange(String recursionTravelSpeedChange) {
		this.recursionTravelSpeedChange = recursionTravelSpeedChange;
	}
	public String getMaxRecursions() {
		return maxRecursions;
	}
	public void setMaxRecursions(String maxRecursions) {
		this.maxRecursions = maxRecursions;
	}
	public String getRecursionTravelSpeed() {
		return recursionTravelSpeed;
	}
	public void setRecursionTravelSpeed(String recursionTravelSpeed) {
		this.recursionTravelSpeed = recursionTravelSpeed;
	}
}
