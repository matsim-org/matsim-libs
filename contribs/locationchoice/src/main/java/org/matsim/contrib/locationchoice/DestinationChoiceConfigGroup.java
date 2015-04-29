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

package org.matsim.contrib.locationchoice;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;

public class DestinationChoiceConfigGroup extends ConfigGroup {

	public static enum Algotype { random, bestResponse, localSearchRecursive, localSearchSingleAct }
	public static final String GROUP_NAME = "locationchoice";
	private static final String RESTR_FCN_FACTOR = "restraintFcnFactor";
	private static final String RESTR_FCN_EXP = "restraintFcnExp";
	private static final String SCALEFACTOR = "scaleFactor";
	private static final String GLOBALTRAVELSPEEDCHANGE = "recursionTravelSpeedChange";
	private static final String GLOBALTRAVELSPEED_CAR = "travelSpeed_car";
	private static final String GLOBALTRAVELSPEED_PT = "travelSpeed_pt";
	private static final String MAX_RECURSIONS = "maxRecursions";
	private static final String CENTER_NODE = "centerNode";
	private static final String RADIUS = "radius";
	private static final String FLEXIBLE_TYPES = "flexible_types";
	
	private static final String ALGO = "algorithm";
	private static final String TT_APPROX_LEVEL = "tt_approximationLevel";
	private static final String MAXDISTANCEDCSCORE = "maxDistanceDCScore";
	private static final String PLANSELECTOR = "planSelector";
	
	private static final String RANDOMSEED = "randomSeed";
	private static final String EPSDISTR = "epsilonDistribution";
	private static final String SCALE_EPS = "epsilonScaleFactors";
	private static final String PROBCHOICESETSIZE = "probChoiceSetSize";
	private static final String PROBCHOICEEXP = "probChoiceExponent";
	
	private static final String PKVALS_FILE = "pkValuesFile";
	private static final String FKVALS_FILE = "fkValuesFile";
	private static final String PBETAS_FILE = "pBetasFileName";
	private static final String FATTRS_FILE = "fAttributesFileName";
	private static final String MAXDCS_FILE = "maxDCScoreFile";
	private static final String PREFS_FILE = "prefsFile";
	
	private static final String ANALYSIS_BOUNDARY = "analysisBoundary";
	private static final String ANALYSIS_BINSIZE = "analysisBinSize";
	private static final String IDEXCLUSION = "idExclusion";
	
	private static final String DESTINATIONSAMPLE_PCT = "destinationSamplePercent";

	//default values
	private String restraintFcnFactor = "0.0";
	private String restraintFcnExp = "0.0";
	private String scaleFactor = "1";
	private String recursionTravelSpeedChange = "0.1";
	private String travelSpeed_car = "8.5";
	private String travelSpeed_pt = "5.0";
	private String maxRecursions = "1";
	private String centerNode = "null";
	private String radius = "null";
	private String flexible_types = "null";
		
	private Algotype algorithm = Algotype.bestResponse ;
	private String tt_approximationLevel = "1";
	private String maxDistanceDCScore = "-1.0";
	private String planSelector = "SelectExpBeta";
	
	private String randomSeed = "221177";
	private String epsilonDistribution = "gumbel";
	private String epsilonScaleFactors = "null";
	private String probChoiceSetSize = "5";	
	private String pkValuesFile = "null";
	private String fkValuesFile = "null";
	private String pBetasFile = "null";
	private String fAttributesFile = "null";
	private String maxDCScoreFile = "null";
	private String prefsFile = "null";
	
	private String analysisBoundary = "200000";
	private String analysisBinSize = "20000";
	private String idExclusion = Integer.toString(Integer.MAX_VALUE);
	
	private String destinationSamplePercent = "100.0";

	private final static Logger log = Logger.getLogger(DestinationChoiceConfigGroup.class);


	public DestinationChoiceConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (RESTR_FCN_FACTOR.equals(key)) {
			return getRestraintFcnFactor();
		}
		if (RESTR_FCN_EXP.equals(key)) {
			return getRestraintFcnExp();
		}
		if (SCALEFACTOR.equals(key)) {
			return getScaleFactor();
		}
		if (GLOBALTRAVELSPEEDCHANGE.equals(key)) {
			return getRecursionTravelSpeedChange();
		}
		if (GLOBALTRAVELSPEED_CAR.equals(key)) {
			return getTravelSpeed_car();
		}
		if (GLOBALTRAVELSPEED_PT.equals(key)) {
			return getTravelSpeed_pt();
		}
		if (MAX_RECURSIONS.equals(key)) {
			return getMaxRecursions();
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
			throw new RuntimeException("getValue access disabled; used direct getter. kai, jan'13") ;
		}
		if (TT_APPROX_LEVEL.equals(key)) {
			return getTravelTimeApproximationLevel();
		}
		if (MAXDISTANCEDCSCORE.equals(key)) {
			return getMaxDistanceDCScore();
		}
		if (PLANSELECTOR.equals(key)) {
			return getPlanSelector();
		}
		if (RANDOMSEED.equals(key)) {
			return getRandomSeed();
		}
		if (EPSDISTR.equals(key)) {
			return getEpsilonDistribution();
		}
		if (SCALE_EPS.equals(key)) {
			return getEpsilonScaleFactors();
		}
		if (PROBCHOICESETSIZE.equals(key)) {
			return getProbChoiceSetSize();
		}
		if (PKVALS_FILE.equals(key)) {
			return getpkValuesFile();
		}
		if (FKVALS_FILE.equals(key)) {
			return getfkValuesFile();
		}
		if (PBETAS_FILE.equals(key)) {
			return getpBetasFile();
		}
		if (FATTRS_FILE.equals(key)) {
			return getfAttributesFile();
		}
		if (MAXDCS_FILE.equals(key)) {
			return getMaxEpsFile();
		}
		if (PREFS_FILE.equals(key)) {
			return getPrefsFile();
		}
		if (ANALYSIS_BOUNDARY.equals(key)) {
			return getAnalysisBoundary();
		}
		if (ANALYSIS_BINSIZE.equals(key)) {
			return getAnalysisBinSize();
		}
		if (IDEXCLUSION.equals(key)) {
			return getIdExclusion();
		}
		if (DESTINATIONSAMPLE_PCT.equals(key)) {
			return getDestinationSamplePercent();
		}
		throw new IllegalArgumentException(key);
	}
	
	@Override
	public void addParam(final String key, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;
		
		if (RESTR_FCN_FACTOR.equals(key)) {
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
		} else if (GLOBALTRAVELSPEEDCHANGE.equals(key)) {
			if (Double.parseDouble(value) < 0.0 || Double.parseDouble(value) > 1.0 ) {
				log.warn("'recursionTravelSpeedChange' must be [0..1]! Set to default value 0.1");
				setRecursionTravelSpeedChange("0.1");
			}
			else {
				setRecursionTravelSpeedChange(value);
			}
		} else if (GLOBALTRAVELSPEED_CAR.equals(key)) {
			if (Double.parseDouble(value) < 0.0 ) {
				log.warn("'travelSpeed' must be positive! Set to default value 8.5");
				setTravelSpeed_car("8.5");
			}
			else {
				setTravelSpeed_car(value);
			}
		} else if (GLOBALTRAVELSPEED_PT.equals(key)) {
			if (Double.parseDouble(value) < 0.0 ) {
				log.warn("'travelSpeed' must be positive! Set to default value 5.0");
				setTravelSpeed_pt("5.0");
			}
			else {
				setTravelSpeed_pt(value);
			}
		} else if (MAX_RECURSIONS.equals(key)) {
			if (Double.parseDouble(value) < 0.0) {
				log.warn("'max_recursions' must be greater than 0! Set to default value 10");
				setMaxRecursions("10");
			}
			else {
				setMaxRecursions(value);
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
//			if (!(value.equals("localSearchRecursive") || value.equals("localSearchSingleAct") 
//					|| value.equals("random") || value.equals("bestResponse"))) {
//			}
//			else {
				for ( Algotype algo : Algotype.values() ) {
					if ( algo.toString().equalsIgnoreCase(value) ) {
						setAlgorithm(algo) ; 
						return ;
					}
				}
				log.warn("define algorithm: 'localSearchRecursive', 'localSearchSingleAct', 'random', 'bestResponse'. Setting to default value 'bestResponse' now");
				setAlgorithm(Algotype.bestResponse) ;
//			}
		} else if (TT_APPROX_LEVEL.equals(key)) {
			if (!(value.equals("0") || value.equals("1") || value.equals("2"))) {
				log.warn("set travel time approximation level to 0, 1 or 2. Set to default value '1' now");
			}
			else {
				setTravelTimeApproximationLevel(value);
			}
		} else if (MAXDISTANCEDCSCORE.equals(key)) {
			setMaxDistanceDCScore(value);
		} else if (PLANSELECTOR.equals(key)) {
			if (!(value.equals("BestScore") || value.equals("SelectExpBeta") || value.equals("ChangeExpBeta") || value.equals("SelectRandom"))) {
				log.warn("set a valid plan selector for location choice. Set to default value 'SelectExpBeta' now");
			}
			else {
				setPlanSelector(value);
			}
		} else if (RANDOMSEED.equals(key)) {
			if (value.length() == 0) {
				log.warn("set a random seed. Set to default value '221177' now");
			}
			else {
				setRandomSeed(value);
			}
		} else if (EPSDISTR.equals(key)) {
			if (!(value.equals("gumbel") || value.equals("gaussian"))) {
				log.warn("set a distribution for the random error terms. Set to default value 'gumbel' now");
			}
			else {
				setEpsilonDistribution(value);
			}
		} else if (SCALE_EPS.equals(key)) {
			if (value.length() == 0) {
				log.warn("set scaling factors for random error terms.");
			}
			else {
				setEpsilonScaleFactors(value);
			}
		} else if (PROBCHOICESETSIZE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define size of canditate set for probabilistic choice. Set to default value '5' now");
			}
			else {
				setProbChoiceSetSize(value);
			}
		} else if (PROBCHOICEEXP.equals(key)) {
			log.error("location choice key " + PROBCHOICEEXP + " is no longer used.  Please remove.  This will be enforced more strictly in the future.  kai, jan'13") ;
		} else if (PKVALS_FILE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define a persons k values file if available. Set to default value 'null' now");
			}
			else {
				setpkValuesFile(value);
			}
		} else if (FKVALS_FILE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define a facilities k values file if available. Set to default value 'null' now");
			}
			else {
				setfkValuesFile(value);
			}
		} else if (FATTRS_FILE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define a facilities attributess file if available. Set to default value 'null' now");
			}
			else {
				setfAttributesFile(value);
			}
		} else if (PBETAS_FILE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define a person betas file if available. Set to default value 'null' now");
			}
			else {
				setpBetasFile(value);
			}
			
		} else if (MAXDCS_FILE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define a max eps file if available. Set to default value 'null' now");
			}
			else {
				setMaxEpsFile(value);
			}
		} else if (PREFS_FILE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define a prefs file if available. Set to default value 'null' now");
			}
			else {
				setPrefsFile(value);
			}
		} else if (ANALYSIS_BOUNDARY.equals(key)) {
			if (value.length() == 0) {
				log.warn("define an analysis region. Set to default value '200km' now");
			}
			else {
				setAnalysisBoundary(value);
			}
		} else if (ANALYSIS_BINSIZE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define an analysis bin size. Set to default value '20km' now");
			}
			else {
				setAnalysisBinSize(value);
			}
		} else if (IDEXCLUSION.equals(key)) {
			if (value.length() == 0) {
				log.warn("define the highest id to be included in analysis. Set to default value 'maxint' now");
			}
			else {
				setIdExclusion(value);
			}
		} else if (DESTINATIONSAMPLE_PCT.equals(key)) {
			if (value.length() > 0) {
				this.setDestinationSamplePercent(value);
			}
		} else
		{
			throw new IllegalArgumentException(key);
		}
	}
		
	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		this.addParameterToMap(map, RESTR_FCN_FACTOR);
		this.addParameterToMap(map, RESTR_FCN_EXP);
		this.addParameterToMap(map, SCALEFACTOR);
		this.addParameterToMap(map, GLOBALTRAVELSPEEDCHANGE);
		this.addParameterToMap(map, GLOBALTRAVELSPEED_CAR);
		this.addParameterToMap(map, GLOBALTRAVELSPEED_PT);
		this.addParameterToMap(map, MAX_RECURSIONS);
		this.addParameterToMap(map, CENTER_NODE);
		this.addParameterToMap(map, RADIUS);
		this.addParameterToMap(map, FLEXIBLE_TYPES);
		map.put(ALGO, this.getAlgorithm().toString()  ) ;
		this.addParameterToMap(map, TT_APPROX_LEVEL);
		this.addParameterToMap(map, MAXDISTANCEDCSCORE);
		this.addParameterToMap(map, PLANSELECTOR);
		this.addParameterToMap(map, RANDOMSEED);
		this.addParameterToMap(map, EPSDISTR);
		this.addParameterToMap(map, SCALE_EPS);
		this.addParameterToMap(map, PROBCHOICESETSIZE);
		this.addParameterToMap(map, PKVALS_FILE);
		this.addParameterToMap(map, FKVALS_FILE);
		this.addParameterToMap(map, PBETAS_FILE);
		this.addParameterToMap(map, FATTRS_FILE);
		this.addParameterToMap(map, MAXDCS_FILE);
		this.addParameterToMap(map, PREFS_FILE);
		this.addParameterToMap(map, ANALYSIS_BOUNDARY);
		this.addParameterToMap(map, ANALYSIS_BINSIZE);
		this.addParameterToMap(map, IDEXCLUSION);
		this.addParameterToMap(map, DESTINATIONSAMPLE_PCT);
		return map;
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
	public String getTravelSpeed_car() {
		return this.travelSpeed_car;
	}
	public void setTravelSpeed_car(final String travelSpeed_car) {
		this.travelSpeed_car = travelSpeed_car;
	}
	public String getTravelSpeed_pt() {
		return this.travelSpeed_pt;
	}
	public void setTravelSpeed_pt(final String travelSpeed_pt) {
		this.travelSpeed_pt = travelSpeed_pt;
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
	public String getpBetasFile() {
		return pBetasFile;
	}
	public String getfAttributesFile() {
		return fAttributesFile;
	}
	public void setpBetasFile(String pBetasFile) {
		this.pBetasFile = pBetasFile;
	}
	public void setfAttributesFile(String fAttributesFile) {
		this.fAttributesFile = fAttributesFile;
	}
	public Algotype getAlgorithm() {
		return algorithm;
	}
	public void setAlgorithm(Algotype algorithm) {
		this.algorithm = algorithm;
	}
	public String getTravelTimeApproximationLevel() {
		return tt_approximationLevel;
	}
	public void setTravelTimeApproximationLevel(String tt_approximationLevel) {
		this.tt_approximationLevel = tt_approximationLevel;
	}
	public String getMaxDistanceDCScore() {
		return maxDistanceDCScore;
	}
	public void setMaxDistanceDCScore(String maxSearchSpaceRadius) {
		this.maxDistanceDCScore = maxSearchSpaceRadius;
	}
	public String getPlanSelector() {
		return planSelector;
	}
	public void setPlanSelector(String planSelector) {
		this.planSelector = planSelector;
	}
	public String getRandomSeed() {
		return randomSeed;
	}
	public void setRandomSeed(String randomSeed) {
		this.randomSeed = randomSeed;
	}
	public String getEpsilonDistribution() {
		return epsilonDistribution;
	}
	public void setEpsilonDistribution(String epsilonDistribution) {
		this.epsilonDistribution = epsilonDistribution;
	}
	public String getEpsilonScaleFactors() {
		return this.epsilonScaleFactors;
	}
	public void setEpsilonScaleFactors(String epsilonScaleFactors){
		this.epsilonScaleFactors = epsilonScaleFactors;
	}
	public String getProbChoiceSetSize() {
		return probChoiceSetSize;
	}
	public void setProbChoiceSetSize(String probChoiceSetSize) {
		this.probChoiceSetSize = probChoiceSetSize;
	}
	public String getpkValuesFile() {
		return pkValuesFile;
	}
	public void setpkValuesFile(String kValuesFile) {
		this.pkValuesFile = kValuesFile;
	}
	public String getfkValuesFile() {
		return fkValuesFile;
	}
	public void setfkValuesFile(String kValuesFile) {
		this.fkValuesFile = kValuesFile;
	}
	public String getMaxEpsFile() {
		return maxDCScoreFile;
	}
	public void setMaxEpsFile(String maxEpsFile) {
		this.maxDCScoreFile = maxEpsFile;
	}
	public String getPrefsFile() {
		return prefsFile;
	}
	public void setPrefsFile(String prefsFile) {
		this.prefsFile = prefsFile;
	}
	public String getAnalysisBoundary() {
		return this.analysisBoundary;
	}
	public void setAnalysisBoundary(String analysisBoundary) {
		this.analysisBoundary = analysisBoundary;
	}
	public String getAnalysisBinSize() {
		return this.analysisBinSize;
	}
	public void setAnalysisBinSize(String analysisBinSize) {
		this.analysisBinSize = analysisBinSize;
	}
	public String getIdExclusion() {
		return this.idExclusion;
	}
	public void setIdExclusion(String idExclusion) {
		this.idExclusion = idExclusion;
	}
	public String getDestinationSamplePercent() {
		return destinationSamplePercent;
	}
	public void setDestinationSamplePercent(String destinationSamplePercent) {
		this.destinationSamplePercent = destinationSamplePercent;
	}	
}
