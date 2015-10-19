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

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;

public class DestinationChoiceConfigGroup extends ConfigGroup {

	public static enum Algotype { random, bestResponse, localSearchRecursive, localSearchSingleAct };
	
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
	private static final double defaultScaleFactor = 1.0;
	private static final double defaultRecursionTravelSpeedChange = 0.1;
	private static final double defaultCarSpeed = 8.5;
	private static final double defaultPtSpeed = 5.0;
	private static final int defaultMaxRecursions = 10; 
	private static final int defaultTt_approximationLevel = 1;
	private static final long defaultRandomSeed = 221177;
	private static final int defaultProbChoiceSetSize = 5;
	private static final double defaultAnalysisBoundary = 200000;
	private static final double defaultAnalysisBinSize = 20000;
	
	private double restraintFcnFactor = 0.0;
	private double restraintFcnExp = 0.0;
	private double scaleFactor = 1;
	private double recursionTravelSpeedChange = 0.1;
	private double travelSpeed_car = 8.5;
	private double travelSpeed_pt = 5.0;
	private int maxRecursions = 1;
	private String centerNode = null;
	private Double radius = null;
	private String flexible_types = "null";	// TODO !!
	
	private Algotype algorithm = Algotype.bestResponse;
	private int tt_approximationLevel = 1;
	private double maxDistanceDCScore = -1.0;
	private String planSelector = "SelectExpBeta";
	
	private long randomSeed = 221177;
	private String epsilonDistribution = "gumbel";
	private String epsilonScaleFactors = null;
	private int probChoiceSetSize = 5;	
	private String pkValuesFile = null;
	private String fkValuesFile = null;
	private String pBetasFile = null;
	private String fAttributesFile = null;
	private String maxDCScoreFile = null;
	private String prefsFile = null;
	
	private double analysisBoundary = 200000;
	private double analysisBinSize = 20000;
//	private String idExclusion = Integer.toString(Integer.MAX_VALUE);
	private Long idExclusion = null;
	
	private double destinationSamplePercent = 100.0;

	private final static Logger log = Logger.getLogger(DestinationChoiceConfigGroup.class);


	public DestinationChoiceConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (RESTR_FCN_FACTOR.equals(key)) {
			return String.valueOf(getRestraintFcnFactor());
		}
		if (RESTR_FCN_EXP.equals(key)) {
			return String.valueOf(getRestraintFcnExp());
		}
		if (SCALEFACTOR.equals(key)) {
			return String.valueOf(getScaleFactor());
		}
		if (GLOBALTRAVELSPEEDCHANGE.equals(key)) {
			return String.valueOf(getRecursionTravelSpeedChange());
		}
		if (GLOBALTRAVELSPEED_CAR.equals(key)) {
			return String.valueOf(getTravelSpeed_car());
		}
		if (GLOBALTRAVELSPEED_PT.equals(key)) {
			return String.valueOf(getTravelSpeed_pt());
		}
		if (MAX_RECURSIONS.equals(key)) {
			return String.valueOf(getMaxRecursions());
		}
		if (CENTER_NODE.equals(key)) {
			return getCenterNode();
		}
		if (RADIUS.equals(key)) {
			return String.valueOf(getRadius());
		}
		if (FLEXIBLE_TYPES.equals(key)) {
			return getFlexibleTypes();
		}
		if (ALGO.equals(key)) {
			throw new RuntimeException("getValue access disabled; used direct getter. kai, jan'13") ;
		}
		if (TT_APPROX_LEVEL.equals(key)) {
			return String.valueOf(getTravelTimeApproximationLevel());
		}
		if (MAXDISTANCEDCSCORE.equals(key)) {
			return String.valueOf(getMaxDistanceDCScore());
		}
		if (PLANSELECTOR.equals(key)) {
			return getPlanSelector();
		}
		if (RANDOMSEED.equals(key)) {
			return String.valueOf(getRandomSeed());
		}
		if (EPSDISTR.equals(key)) {
			return getEpsilonDistribution();
		}
		if (SCALE_EPS.equals(key)) {
			return getEpsilonScaleFactors();
		}
		if (PROBCHOICESETSIZE.equals(key)) {
			return String.valueOf(getProbChoiceSetSize());
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
			return String.valueOf(getAnalysisBoundary());
		}
		if (ANALYSIS_BINSIZE.equals(key)) {
			return String.valueOf(getAnalysisBinSize());
		}
		if (IDEXCLUSION.equals(key)) {
			return String.valueOf(getIdExclusion());
		}
		if (DESTINATIONSAMPLE_PCT.equals(key)) {
			return String.valueOf(getDestinationSamplePercent());
		}
		throw new IllegalArgumentException(key);
	}
	
	@Override
	public void addParam(final String key, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;
		
		if (RESTR_FCN_FACTOR.equals(key)) {
			double doubleValue = Double.parseDouble(value);
			if (doubleValue < 0.0) {
				log.warn("Restraint function factor is negative! " +
						"This means: The more people are in a facility, the more attractive the facility is expected to be");
			}
			this.setRestraintFcnFactor(doubleValue);
		} else if (RESTR_FCN_EXP.equals(key)) {
			double doubleValue = Double.parseDouble(value);
			if (doubleValue < 0.0) {
				log.warn("Restraint function exponent is negative! " +
						"This means: The penalty gets smaller the more people are in a facility.");
			}
			this.setRestraintFcnExp(doubleValue);
		} else if (SCALEFACTOR.equals(key)) {
			double doubleValue = Double.parseDouble(value);
			if (doubleValue < 1) {
				log.warn("Scale factor must be greater than 1! Scale factor is set to default value " + defaultScaleFactor);
				this.setScaleFactor(defaultScaleFactor);
			}
			else {
				this.setScaleFactor(doubleValue);
			}
		} else if (GLOBALTRAVELSPEEDCHANGE.equals(key)) {
			double doubleValue = Double.parseDouble(value);
			if (doubleValue < 0.0 || doubleValue > 1.0 ) {
				log.warn("'recursionTravelSpeedChange' must be [0..1]! Set to default value " + defaultRecursionTravelSpeedChange);
				this.setRecursionTravelSpeedChange(defaultRecursionTravelSpeedChange);
			}
			else {
				this.setRecursionTravelSpeedChange(doubleValue);
			}
		} else if (GLOBALTRAVELSPEED_CAR.equals(key)) {
			double doubleValue = Double.parseDouble(value);
			if (doubleValue < 0.0 ) {
				log.warn("'travelSpeed' must be positive! Set to default value " + defaultCarSpeed);
				this.setTravelSpeed_car(defaultCarSpeed);
			}
			else {
				this.setTravelSpeed_car(doubleValue);
			}
		} else if (GLOBALTRAVELSPEED_PT.equals(key)) {
			double doubleValue = Double.parseDouble(value);
			if (doubleValue < 0.0 ) {
				log.warn("'travelSpeed' must be positive! Set to default value " + defaultPtSpeed);
				this.setTravelSpeed_pt(defaultPtSpeed);
			}
			else {
				this.setTravelSpeed_pt(doubleValue);
			}
		} else if (MAX_RECURSIONS.equals(key)) {
			int intValue = Integer.parseInt(value);
			if (intValue < 0) {
				log.warn("'max_recursions' must be greater than 0! Set to default value " + defaultMaxRecursions);
				this.setMaxRecursions(defaultMaxRecursions);
			}
			else {
				this.setMaxRecursions(intValue);
			}
		} else if (CENTER_NODE.equals(key)) {
			if (value.equals("null")) this.setCenterNode(null);
			else this.setCenterNode(value);
		} else if (RADIUS.equals(key)) {
			if (value.equals("null")) {
				Double d = null;
				this.setRadius(d);
			} else setRadius(Double.parseDouble(value));
		} else if (FLEXIBLE_TYPES.equals(key)) {
			if (value.length() == 0) {
				this.setFlexibleTypes("null");
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
				setAlgorithm(Algotype.bestResponse);
//			}
		} else if (TT_APPROX_LEVEL.equals(key)) {
			int intValue = Integer.parseInt(value);
			if (intValue != 0 && intValue != 1 && intValue != 2) {
				log.warn("set travel time approximation level to 0, 1 or 2. Set to default value '" + defaultTt_approximationLevel + "' now");
				this.setTravelTimeApproximationLevel(defaultTt_approximationLevel);
			}
			else {
				this.setTravelTimeApproximationLevel(intValue);
			}
		} else if (MAXDISTANCEDCSCORE.equals(key)) {
			double doubleValue = Double.parseDouble(value);
			this.setMaxDistanceDCScore(doubleValue);
		} else if (PLANSELECTOR.equals(key)) {
			if (!(value.equals("BestScore") || value.equals("SelectExpBeta") || value.equals("ChangeExpBeta") || value.equals("SelectRandom"))) {
				log.warn("set a valid plan selector for location choice. Set to default value 'SelectExpBeta' now");
			}
			else {
				setPlanSelector(value);
			}
		} else if (RANDOMSEED.equals(key)) {
			if (value.length() == 0) {
				log.warn("set a random seed. Set to default value '" + defaultRandomSeed + "' now");
				this.setRandomSeed(defaultRandomSeed);
			}
			else {
				long longValue = Long.parseLong(value);
				this.setRandomSeed(longValue);
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
			else if (value.equals("null")) {
				this.setEpsilonScaleFactors(null);
			}
			else {
				this.setEpsilonScaleFactors(value);
			}
		} else if (PROBCHOICESETSIZE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define size of canditate set for probabilistic choice. Set to default value '" + defaultProbChoiceSetSize + "' now");
				this.setProbChoiceSetSize(defaultProbChoiceSetSize);
			}
			else {
				int intValue = Integer.parseInt(value);
				this.setProbChoiceSetSize(intValue);
			}
		} else if (PROBCHOICEEXP.equals(key)) {
			log.error("location choice key " + PROBCHOICEEXP + " is no longer used.  Please remove.  This will be enforced more strictly in the future.  kai, jan'13") ;
		} else if (PKVALS_FILE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define a persons k values file if available. Set to default value 'null' now");
				this.setpkValuesFile(null);
			}
			else if (value.equals("null")) {
				this.setpkValuesFile(null);
			}
			else {
				this.setpkValuesFile(value);
			}
		} else if (FKVALS_FILE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define a facilities k values file if available. Set to default value 'null' now");
				this.setfkValuesFile(null);
			}
			else if (value.equals("null")) {
				this.setfkValuesFile(null);
			}
			else {
				this.setfkValuesFile(value);
			}
		} else if (FATTRS_FILE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define a facilities attributess file if available. Set to default value 'null' now");
				this.setfAttributesFile(null);
			}
			else if (value.equals("null")) {
				this.setfAttributesFile(null);
			}
			else {
				this.setfAttributesFile(value);
			}
		} else if (PBETAS_FILE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define a person betas file if available. Set to default value 'null' now");
				this.setpBetasFile(null);
			}
			else if (value.equals("null")) {
				this.setpBetasFile(null);
			}
			else {
				this.setpBetasFile(value);
			}
			
		} else if (MAXDCS_FILE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define a max eps file if available. Set to default value 'null' now");
				this.setMaxEpsFile(null);
			}
			else if (value.equals("null")) {
				this.setMaxEpsFile(null);
			}
			else {
				this.setMaxEpsFile(value);
			}
		} else if (PREFS_FILE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define a prefs file if available. Set to default value 'null' now");
				this.setPrefsFile(null);
			}
			else if (value.equals("null")) {
				this.setPrefsFile(null);
			}
			else {
				this.setPrefsFile(value);
			}
		} else if (ANALYSIS_BOUNDARY.equals(key)) {
			if (value.length() == 0) {
				log.warn("define an analysis region. Set to default value '" + defaultAnalysisBoundary + "' now");
				this.setAnalysisBoundary(defaultAnalysisBoundary);
			}
			else {
				double doubleValue = Double.parseDouble(value);
				this.setAnalysisBoundary(doubleValue);
			}
		} else if (ANALYSIS_BINSIZE.equals(key)) {
			if (value.length() == 0) {
				log.warn("define an analysis bin size. Set to default value '" + defaultAnalysisBinSize + "' now");
				this.setAnalysisBinSize(defaultAnalysisBinSize);
			}
			else {
				double doubleValue = Double.parseDouble(value);
				setAnalysisBinSize(doubleValue);
			}
		} else if (IDEXCLUSION.equals(key)) {
			if (value.length() == 0) {
				log.warn("define the highest id to be included in analysis. Set to default value 'maxint' now");
			}
			else if (value.equals("null")) {
				this.setIdExclusion(null);
			}
			else {
				long longValue = Long.parseLong(value);
				this.setIdExclusion(longValue);
			}
		} else if (DESTINATIONSAMPLE_PCT.equals(key)) {
			if (value.length() > 0) {
				this.setDestinationSamplePercent(Double.parseDouble(value));
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
		map.put(ALGO, this.getAlgorithm().toString());
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

	public double getRestraintFcnFactor() {
		return this.restraintFcnFactor;
	}
	public void setRestraintFcnFactor(final double restraintFcnFactor) {
		this.restraintFcnFactor = restraintFcnFactor;
	}
	public double getRestraintFcnExp() {
		return this.restraintFcnExp;
	}
	public void setRestraintFcnExp(final double restraintFcnExp) {
		this.restraintFcnExp = restraintFcnExp;
	}
	public double getScaleFactor() {
		return this.scaleFactor;
	}
	public void setScaleFactor(final double scaleFactor) {
		this.scaleFactor = scaleFactor;
	}
	public double getRecursionTravelSpeedChange() {
		return this.recursionTravelSpeedChange;
	}
	public void setRecursionTravelSpeedChange(final double recursionTravelSpeedChange) {
		this.recursionTravelSpeedChange = recursionTravelSpeedChange;
	}
	public int getMaxRecursions() {
		return this.maxRecursions;
	}
	public void setMaxRecursions(final int maxRecursions) {
		this.maxRecursions = maxRecursions;
	}
	public double getTravelSpeed_car() {
		return this.travelSpeed_car;
	}
	public void setTravelSpeed_car(final double travelSpeed_car) {
		this.travelSpeed_car = travelSpeed_car;
	}
	public double getTravelSpeed_pt() {
		return this.travelSpeed_pt;
	}
	public void setTravelSpeed_pt(final double travelSpeed_pt) {
		this.travelSpeed_pt = travelSpeed_pt;
	}
	public String getCenterNode() {
		return this.centerNode;
	}
	public void setCenterNode(final String centerNode) {
		this.centerNode = centerNode;
	}
	public Double getRadius() {
		return this.radius;
	}
	public void setRadius(final Double radius) {
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
		return this.algorithm;
	}
	public void setAlgorithm(Algotype algorithm) {
		this.algorithm = algorithm;
	}
	public int getTravelTimeApproximationLevel() {
		return this.tt_approximationLevel;
	}
	public void setTravelTimeApproximationLevel(int tt_approximationLevel) {
		this.tt_approximationLevel = tt_approximationLevel;
	}
	public double getMaxDistanceDCScore() {
		return this.maxDistanceDCScore;
	}
	public void setMaxDistanceDCScore(double maxSearchSpaceRadius) {
		this.maxDistanceDCScore = maxSearchSpaceRadius;
	}
	public String getPlanSelector() {
		return this.planSelector;
	}
	public void setPlanSelector(String planSelector) {
		this.planSelector = planSelector;
	}
	public long getRandomSeed() {
		return this.randomSeed;
	}
	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
	}
	public String getEpsilonDistribution() {
		return this.epsilonDistribution;
	}
	public void setEpsilonDistribution(String epsilonDistribution) {
		this.epsilonDistribution = epsilonDistribution;
	}
	public String getEpsilonScaleFactors() {
		return this.epsilonScaleFactors;
	}
	public void setEpsilonScaleFactors(String epsilonScaleFactors) {
		this.epsilonScaleFactors = epsilonScaleFactors;
	}
	public int getProbChoiceSetSize() {
		return this.probChoiceSetSize;
	}
	public void setProbChoiceSetSize(int probChoiceSetSize) {
		this.probChoiceSetSize = probChoiceSetSize;
	}
	public String getpkValuesFile() {
		return this.pkValuesFile;
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
		return this.maxDCScoreFile;
	}
	public void setMaxEpsFile(String maxEpsFile) {
		this.maxDCScoreFile = maxEpsFile;
	}
	public String getPrefsFile() {
		return this.prefsFile;
	}
	public void setPrefsFile(String prefsFile) {
		this.prefsFile = prefsFile;
	}
	public double getAnalysisBoundary() {
		return this.analysisBoundary;
	}
	public void setAnalysisBoundary(double analysisBoundary) {
		this.analysisBoundary = analysisBoundary;
	}
	public double getAnalysisBinSize() {
		return this.analysisBinSize;
	}
	public void setAnalysisBinSize(double analysisBinSize) {
		this.analysisBinSize = analysisBinSize;
	}
	public Long getIdExclusion() {
		return this.idExclusion;
	}
	public void setIdExclusion(Long idExclusion) {
		this.idExclusion = idExclusion;
	}
	public double getDestinationSamplePercent() {
		return this.destinationSamplePercent;
	}
	@Deprecated
	public void setDestinationSamplePercent(String destinationSamplePercent) {
		this.setDestinationSamplePercent(Double.parseDouble(destinationSamplePercent));
	}
	public void setDestinationSamplePercent(double destinationSamplePercent) {
		this.destinationSamplePercent = destinationSamplePercent;
	}	
}