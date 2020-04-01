/* *********************************************************************** *
 * project: org.matsim.*
 * DestinationChoiceConfigGroup.java
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

package org.matsim.contrib.locationchoice;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;
import java.util.TreeMap;

public class DestinationChoiceConfigGroup extends ReflectiveConfigGroup implements DestinationChoiceConfigGroupI {
	private final static Logger log = Logger.getLogger(DestinationChoiceConfigGroup.class);

	public enum Algotype { random, bestResponse, localSearchRecursive, localSearchSingleAct };
	public enum EpsilonDistributionTypes { gumbel, gaussian };
	public enum InternalPlanDataStructure { planImpl, lcPlan };
	public enum ApproximationLevel {completeRouting, localRouting, noRouting}

	public static final String GROUP_NAME = "locationchoice";
	
	private static final String RESTR_FCN_FACTOR = "restraintFcnFactor";
	private static final String RESTR_FCN_EXP = "restraintFcnExp";
	private static final String SCALEFACTOR = "scaleFactor";
	private static final String GLOBALTRAVELSPEEDCHANGE = "recursionTravelSpeedChange";
	private static final String GLOBALTRAVELSPEED_CAR = "travelSpeed_car";
	private static final String MAX_RECURSIONS = "maxRecursions";
	private static final String CENTER_NODE = "centerNode";
	private static final String RADIUS = "radius";
	private static final String FLEXIBLE_TYPES = "flexible_types";
	
	private static final String ALGO = "algorithm";
	private static final String PLANSELECTOR = "planSelector";
	
	private static final String SCALE_EPS = "epsilonScaleFactors";

	private static final String USE_CONFIG_PARAMS_FOR_SCORING = "useConfigParamsForScoring";
	private static final String USE_INDIVIDUAL_SCORING_PARAMETERS = "useIndividualScoringParameters";

	private double restraintFcnFactor = 0.0;
	private double restraintFcnExp = 0.0;
	private double scaleFactor = 1;
	private double recursionTravelSpeedChange = 0.1;
	private double travelSpeed_car = 8.5;
	private int maxRecursions = 1;
	private String centerNode = null;
	private Double radius = null;
	private String flexible_types = "null";	// TODO !!
	
	private Algotype algorithm = Algotype.bestResponse;
	private String planSelector = "SelectExpBeta";
	
	private String epsilonScaleFactors = null;


	public DestinationChoiceConfigGroup() {
		super(GROUP_NAME);
	}
	
	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();

		map.put(USE_CONFIG_PARAMS_FOR_SCORING, "Default is 'true'. Parameter was already present in the DCScoringFunction.");
		map.put(USE_INDIVIDUAL_SCORING_PARAMETERS, "MATSim supports individual scoring parameters for sub-populations or even single agents. "
				+ "If you use global parameters, this can be set to 'false' (default is 'true').");

		return map;
	}

	@Override @StringGetter( RESTR_FCN_FACTOR )
	public double getRestraintFcnFactor() {
		return this.restraintFcnFactor;
	}
	@StringSetter( RESTR_FCN_FACTOR )
	public void setRestraintFcnFactor(final double restraintFcnFactor) {
		this.restraintFcnFactor = restraintFcnFactor;
	}
	@Override @StringGetter( RESTR_FCN_EXP )
	public double getRestraintFcnExp() {
		return this.restraintFcnExp;
	}
	@StringSetter( RESTR_FCN_EXP )
	public void setRestraintFcnExp(final double restraintFcnExp) {
		this.restraintFcnExp = restraintFcnExp;
	}
	@Override @StringGetter( SCALEFACTOR )
	public double getScaleFactor() {
		return this.scaleFactor;
	}
	@StringSetter( SCALEFACTOR )
	public void setScaleFactor(final double scaleFactor) {
		this.scaleFactor = scaleFactor;
	}
	@StringGetter( GLOBALTRAVELSPEEDCHANGE )
	public double getRecursionTravelSpeedChange() {
		return this.recursionTravelSpeedChange;
	}
	@StringSetter( GLOBALTRAVELSPEEDCHANGE )
	public void setRecursionTravelSpeedChange(final double recursionTravelSpeedChange) {
		this.recursionTravelSpeedChange = recursionTravelSpeedChange;
	}
	@StringGetter( MAX_RECURSIONS )
	public int getMaxRecursions() {
		return this.maxRecursions;
	}
	@StringSetter( MAX_RECURSIONS )
	public void setMaxRecursions(final int maxRecursions) {
		this.maxRecursions = maxRecursions;
	}
	@StringGetter( GLOBALTRAVELSPEED_CAR )
	public double getTravelSpeed_car() {
		return this.travelSpeed_car;
	}
	@StringSetter( GLOBALTRAVELSPEED_CAR )
	public void setTravelSpeed_car(final double travelSpeed_car) {
		this.travelSpeed_car = travelSpeed_car;
	}
	@Override @StringGetter( CENTER_NODE )
	public String getCenterNode() {
		return this.centerNode;
	}
	@StringSetter( CENTER_NODE )
	public void setCenterNode(final String centerNode) {
		this.centerNode = centerNode;
	}
	@Override @StringGetter( RADIUS )
	public Double getRadius() {
		return this.radius;
	}
	@StringSetter( RADIUS )
	public void setRadius(final Double radius) {
		this.radius = radius;
	}
	@Override @StringGetter( FLEXIBLE_TYPES )
	public String getFlexibleTypes() {
		return this.flexible_types;
	}
	@StringSetter( FLEXIBLE_TYPES )
	public void setFlexibleTypes(final String flexibleTypes) {
		this.flexible_types = flexibleTypes;
	}
	@StringGetter( ALGO )
	public Algotype getAlgorithm() {
		return this.algorithm;
	}
	@StringSetter( ALGO )
	public void setAlgorithm(Algotype algorithm) {
		this.algorithm = algorithm;
	}
	@StringGetter( PLANSELECTOR )
	public String getPlanSelector() {
		return this.planSelector;
	}
	@StringSetter( PLANSELECTOR )
	public void setPlanSelector(String planSelector) {
		this.planSelector = planSelector;
	}
	@Override @StringGetter( SCALE_EPS )
	public String getEpsilonScaleFactors() {
		return this.epsilonScaleFactors;
	}
	@StringSetter( SCALE_EPS )
	public void setEpsilonScaleFactors(String epsilonScaleFactors) {
		this.epsilonScaleFactors = epsilonScaleFactors;
	}
}
