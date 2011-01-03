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

package org.matsim.core.config.groups;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;


public class LocationChoiceConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "locationchoice";

	// true; false
	private static final String CONSTRAINED = "constrained";
	private static final String RESTR_FCN_FACTOR = "restraintFcnFactor";
	private static final String RESTR_FCN_EXP = "restraintFcnExp";
	private static final String SCALEFACTOR = "scaleFactor";
	private static final String RECURSIONTRAVELSPEEDCHANGE = "recursionTravelSpeedChange";
	private static final String RECURSIONTRAVELSPEED = "recursionTravelSpeed";
	private static final String MAX_RECURSIONS = "maxRecursions";
	private static final String FIX_BY_ACTTYPE = "fixByActType";
	private static final String SIMPLE_TG = "simple_tg";
	private static final String CENTER_NODE = "centerNode";
	private static final String RADIUS = "radius";
	private static final String FLEXIBLE_TYPES = "flexible_types";
	
	private static final String ALGO = "algorithm";
	private static final String TT_APPROX_LEVEL = "tt_approximationLevel";
	private static final String TT = "travelTimes";
	private static final String SEARCHSPACEBETA = "searchSpaceBeta";
	private static final String MAXRADIUS = "maxSearchSpaceRadius";
	private static final String PLANSELECTOR = "planSelector";

	//default values
	private String constrained = "false";
	private String restraintFcnFactor = "0.0";
	private String restraintFcnExp = "0.0";
	private String scaleFactor = "1";
	private String recursionTravelSpeedChange = "0.1";
	private String recursionTravelSpeed = "8.5";
	private String maxRecursions = "1";
	private String fixByActType = "false";
	private String simple_tg = "false";
	private String centerNode = "null";
	private String radius = "null";
	private String flexible_types = "null";
		
	private String algorithm = "null";
	private String tt_approximationLevel = "0";
	private String travelTimes = "true";
	private String searchSpaceBeta = "0.0001";
	private String maxSearchSpaceRadius = "-1.0";
	private String planSelector = "SelectExpBeta";

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
		if (FIX_BY_ACTTYPE.equals(key)) {
			return getFixByActType();
		}
		if (SIMPLE_TG.equals(key)) {
			return getSimpleTG();
		}
		if (CENTER_NODE.equals(key)) {
			return getCenterNode();
		}
		if (RADIUS.equals(key)) {
			return getRadius();
		}
		if (FLEXIBLE_TYPES.equals(key)) {
			return getFlexibleTypes();
		}
		if (ALGO.equals(key)) {
			return getAlgorithm();
		}
		if (TT_APPROX_LEVEL.equals(key)) {
			return getTravelTimeApproximationLevel();
		}
		if (TT.equals(key)) {
			return getTravelTimes();
		}
		if (SEARCHSPACEBETA.equals(key)) {
			return getSearchSpaceBeta();
		}
		if (MAXRADIUS.equals(key)) {
			return getMaxSearchSpaceRadius();
		}
		if (PLANSELECTOR.equals(key)) {
			return getPlanSelector();
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
		} else if (FIX_BY_ACTTYPE.equals(key)) {
			if (!(value.equals("true") || value.equals("false"))) {
				log.warn("set 'fixByActType' to either 'true' or 'false'. Set to default value 'false'");
				setFixByActType("false");
			}
			else {
				setFixByActType(value);
			}
		} else if (SIMPLE_TG.equals(key)) {
			if (!(value.equals("true") || value.equals("false"))) {
				log.warn("set 'simple_tg' to either 'true' or 'false'. Set to default value 'false'");
				setSimpleTG("false");
			}
			else {
				setSimpleTG(value);
			}
		} else if (CENTER_NODE.equals(key)) {
			setCenterNode(value);
		} else if (RADIUS.equals(key)) {
			setRadius(value);
		} else if (FLEXIBLE_TYPES.equals(key)) {
			if (value.length() == 0) {
				setFlexibleTypes("null");
			}
			else {
				setFlexibleTypes(value);
			}
		} else if (ALGO.equals(key)) {
			setAlgorithm(value);
		} else if (TT_APPROX_LEVEL.equals(key)) {
			if (!(value.equals("0") || value.equals("1") || value.equals("1"))) {
				log.warn("set travel time approximation level to 0, 1 or 2. Set to default value '0' now (no approximation)");
			}
			else {
				setTravelTimeApproximationLevel(value);
			}
		} else if (TT.equals(key)) {
			if (!(value.equals("true") || value.equals("false"))) {
				log.warn("set 'travelTimes' to either 'true' or 'false'. Set to default value 'true' now");
			}
			else {
				setTravelTimes(value);
			}
		} else if (SEARCHSPACEBETA.equals(key)) {
			if (Double.parseDouble(value) >= 0.0) {
				log.warn("set 'searchSpaceBeta' to a negative value. Set to default value '-0.0001' now.");
			}
			else {
				setSearchSpaceBeta(value);
			}
		} else if (MAXRADIUS.equals(key)) {
			setMaxSearchSpaceRadius(value);
		} else if (PLANSELECTOR.equals(key)) {
			if (!(value.equals("BestScore") || value.equals("SelectExpBeta") || value.equals("ChangeExpBeta") || value.equals("SelectRandom"))) {
				log.warn("set a valid plan selector for location choice. Set to default value 'SelectExpBeta' now");
			}
			else {
				setPlanSelector(value);
			}
		}
		else
		{
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		this.addParameterToMap(map, CONSTRAINED);
		this.addParameterToMap(map, RESTR_FCN_FACTOR);
		this.addParameterToMap(map, RESTR_FCN_EXP);
		this.addParameterToMap(map, SCALEFACTOR);
		this.addParameterToMap(map, RECURSIONTRAVELSPEEDCHANGE);
		this.addParameterToMap(map, RECURSIONTRAVELSPEED);
		this.addParameterToMap(map, MAX_RECURSIONS);
		this.addParameterToMap(map, FIX_BY_ACTTYPE);
		this.addParameterToMap(map, SIMPLE_TG);
		this.addParameterToMap(map, CENTER_NODE);
		this.addParameterToMap(map, RADIUS);
		this.addParameterToMap(map, FLEXIBLE_TYPES);
		this.addParameterToMap(map, ALGO);
		this.addParameterToMap(map, TT_APPROX_LEVEL);
		this.addParameterToMap(map, TT);
		this.addParameterToMap(map, SEARCHSPACEBETA);
		this.addParameterToMap(map, MAXRADIUS);
		this.addParameterToMap(map, PLANSELECTOR);
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
		return this.recursionTravelSpeedChange;
	}
	public void setRecursionTravelSpeedChange(final String recursionTravelSpeedChange) {
		this.recursionTravelSpeedChange = recursionTravelSpeedChange;
	}
	public String getMaxRecursions() {
		return this.maxRecursions;
	}
	public void setMaxRecursions(final String maxRecursions) {
		this.maxRecursions = maxRecursions;
	}
	public String getRecursionTravelSpeed() {
		return this.recursionTravelSpeed;
	}
	public void setRecursionTravelSpeed(final String recursionTravelSpeed) {
		this.recursionTravelSpeed = recursionTravelSpeed;
	}
	public String getFixByActType() {
		return this.fixByActType;
	}
	public void setFixByActType(final String fixByActType) {
		this.fixByActType = fixByActType;
	}
	public void setSimpleTG(final String simple_tg) {
		this.simple_tg = simple_tg;
	}
	public String getSimpleTG() {
		return this.simple_tg;
	}
	public String getCenterNode() {
		return this.centerNode;
	}
	public void setCenterNode(final String centerNode) {
		this.centerNode = centerNode;
	}
	public String getRadius() {
		return this.radius;
	}
	public void setRadius(final String radius) {
		this.radius = radius;
	}
	public String getFlexibleTypes() {
		return this.flexible_types;
	}
	public void setFlexibleTypes(final String flexibleTypes) {
		this.flexible_types = flexibleTypes;
	}
	public String getAlgorithm() {
		return algorithm;
	}
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}
	public String getTravelTimeApproximationLevel() {
		return tt_approximationLevel;
	}
	public void setTravelTimeApproximationLevel(String tt_approximationLevel) {
		this.tt_approximationLevel = tt_approximationLevel;
	}
	public String getTravelTimes() {
		return travelTimes;
	}
	public void setTravelTimes(String travelTimes) {
		this.travelTimes = travelTimes;
	}
	public String getSearchSpaceBeta() {
		return searchSpaceBeta;
	}
	public void setSearchSpaceBeta(String searchSpaceBeta) {
		this.searchSpaceBeta = searchSpaceBeta;
	}
	public String getMaxSearchSpaceRadius() {
		return maxSearchSpaceRadius;
	}
	public void setMaxSearchSpaceRadius(String maxSearchSpaceRadius) {
		this.maxSearchSpaceRadius = maxSearchSpaceRadius;
	}
	public String getPlanSelector() {
		return planSelector;
	}
	public void setPlanSelector(String planSelector) {
		this.planSelector = planSelector;
	}
}
