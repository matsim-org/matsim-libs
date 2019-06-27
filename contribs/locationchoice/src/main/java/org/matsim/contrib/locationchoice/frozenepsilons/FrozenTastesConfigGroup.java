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

package org.matsim.contrib.locationchoice.frozenepsilons;

import org.apache.log4j.Logger;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroupI;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class FrozenTastesConfigGroup extends ReflectiveConfigGroup implements DestinationChoiceConfigGroupI {
	private final static Logger log = Logger.getLogger( FrozenTastesConfigGroup.class );
	public static final String GROUP_NAME = "frozenTastes";

	public enum Algotype { random, bestResponse, localSearchRecursive, localSearchSingleAct };
	public enum EpsilonDistributionTypes { gumbel, gaussian };
	public enum InternalPlanDataStructure { planImpl, lcPlan };
	public enum ApproximationLevel {completeRouting, localRouting, noRouting}


	private static final String RESTR_FCN_FACTOR = "restraintFcnFactor";
	private static final String RESTR_FCN_EXP = "restraintFcnExp";
	private static final String SCALEFACTOR = "scaleFactor";
//	private static final String GLOBALTRAVELSPEEDCHANGE = "recursionTravelSpeedChange";
	private static final String GLOBALTRAVELSPEED_CAR = "travelSpeed_car";
//	private static final String GLOBALTRAVELSPEED_PT = "travelSpeed_pt";
//	private static final String MAX_RECURSIONS = "maxRecursions";
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
//	private static final String PROBCHOICESETSIZE = "probChoiceSetSize";
//	private static final String PROBCHOICEEXP = "probChoiceExponent";
	
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

	private static final String INTERNAL_PLAN_DATA_STRUCTURE = "internalPlanDataStructure";
	private static final String USE_CONFIG_PARAMS_FOR_SCORING = "useConfigParamsForScoring";
	private static final String USE_INDIVIDUAL_SCORING_PARAMETERS = "useIndividualScoringParameters";
	private static final String RE_USE_TEMPORARY_PLANS = "reUseTemporaryPlans";
	
	//default values
//	private static final double defaultScaleFactor = 1.0;
//	private static final double defaultRecursionTravelSpeedChange = 0.1;
//	private static final double defaultCarSpeed = 8.5;
//	private static final double defaultPtSpeed = 5.0;
//	private static final int defaultMaxRecursions = 10;
//	private static final long defaultRandomSeed = 221177;
//	private static final int defaultProbChoiceSetSize = 5;
//	private static final double defaultAnalysisBoundary = 200000;
//	private static final double defaultAnalysisBinSize = 20000;
//	private static final EpsilonDistributionTypes defaultEpsilonDistribution = EpsilonDistributionTypes.gumbel;
//	private static final InternalPlanDataStructure defaultInternalPlanDataStructure = InternalPlanDataStructure.planImpl;
	private static final boolean defaultUseConfigParamsForScoring = true;
	private static final boolean defaultUseIndividualScoringParameters = true;
//	private static final boolean defaultReUseTemporaryPlans = false;
	
	private double restraintFcnFactor = 0.0;
	private double restraintFcnExp = 0.0;
	private double scaleFactor = 1;
//	private double recursionTravelSpeedChange = 0.1;
	private double travelSpeed_car = 8.5;
//	private double travelSpeed_pt = 5.0;
//	private int maxRecursions = 1;
	private String centerNode = null;
	private Double radius = null;
	private String flexible_types = "null";	// TODO !!
	
	private Algotype algorithm = Algotype.bestResponse;
	private ApproximationLevel tt_approximationLevel = ApproximationLevel.localRouting;
	private double maxDistanceDCScore = -1.0;
	private String planSelector = "SelectExpBeta";
	
	private long randomSeed = 221177;
	private EpsilonDistributionTypes epsilonDistribution = EpsilonDistributionTypes.gumbel;
	private String epsilonScaleFactors = null;
//	private int probChoiceSetSize = 5;
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
	
	/* experimental stuff */
//	private InternalPlanDataStructure internalPlanDataStructure = defaultInternalPlanDataStructure;
	private boolean useConfigParamsForScoring = defaultUseConfigParamsForScoring;
	private boolean useIndividualScoringParameters = defaultUseIndividualScoringParameters;
//	private boolean reUseTemporaryPlans = defaultReUseTemporaryPlans;

	public FrozenTastesConfigGroup() {
		super(GROUP_NAME);
	}
	
	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();

		map.put(INTERNAL_PLAN_DATA_STRUCTURE, "During the location choice process, many alternative locations are evaluated. "
				+ "For each of them, a temporary plan is created. By default, MATSim regular plan objects are used ('planImpl'). "
				+ "However, using them results in a certain overhead and more objects to be cleared by the garbage collector. "
				+ "Instead, an alternative data structure can be used ('lcPlan') - this is still experimental, so use the default ('planImpl') "
				+ "unless you know what you are doing!  ");
		map.put(USE_CONFIG_PARAMS_FOR_SCORING, "Default is 'true'. Parameter was already present in the DCScoringFunction.");
		map.put(USE_INDIVIDUAL_SCORING_PARAMETERS, "MATSim supports individual scoring parameters for sub-populations or even single agents. "
				+ "If you use global parameters, this can be set to 'false' (default is 'true').");
		map.put(
				RE_USE_TEMPORARY_PLANS,
				"Default is 'false'. During the location choice process, many potential locations are evaluated. "
						+ "For each of them, a copy of the person's current plan is created, which results in a huge workload for the "
						+ "garbage collector as well as the memory bus. When this option is set to 'true', only one copy of the plan is created "
						+ "and re-used for each checked location. Note that this is still experimental! cdobler oct'15" );
		
		return map;
	}

	@StringGetter( RESTR_FCN_FACTOR )
	public double getRestraintFcnFactor() {
		return this.restraintFcnFactor;
	}
	@StringSetter( RESTR_FCN_FACTOR )
	public void setRestraintFcnFactor(final double restraintFcnFactor) {
		this.restraintFcnFactor = restraintFcnFactor;
	}
	@StringGetter( RESTR_FCN_EXP )
	public double getRestraintFcnExp() {
		return this.restraintFcnExp;
	}
	@StringSetter( RESTR_FCN_EXP )
	public void setRestraintFcnExp(final double restraintFcnExp) {
		this.restraintFcnExp = restraintFcnExp;
	}
	@StringGetter( SCALEFACTOR )
	public double getScaleFactor() {
		return this.scaleFactor;
	}
	@StringSetter( SCALEFACTOR )
	public void setScaleFactor(final double scaleFactor) {
		this.scaleFactor = scaleFactor;
	}
//	public double getRecursionTravelSpeedChange() {
//		return this.recursionTravelSpeedChange;
//	}
//	public void setRecursionTravelSpeedChange(final double recursionTravelSpeedChange) {
//		this.recursionTravelSpeedChange = recursionTravelSpeedChange;
//	}
//	public int getMaxRecursions() {
//		return this.maxRecursions;
//	}
//	public void setMaxRecursions(final int maxRecursions) {
//		this.maxRecursions = maxRecursions;
//	}
@StringGetter( GLOBALTRAVELSPEED_CAR )
	public double getTravelSpeed_car() {
		return this.travelSpeed_car;
	}
	@StringSetter( GLOBALTRAVELSPEED_CAR )
	public void setTravelSpeed_car(final double travelSpeed_car) {
		this.travelSpeed_car = travelSpeed_car;
	}
//	public double getTravelSpeed_pt() {
//		return this.travelSpeed_pt;
//	}
//	public void setTravelSpeed_pt(final double travelSpeed_pt) {
//		this.travelSpeed_pt = travelSpeed_pt;
//	}
@StringGetter( CENTER_NODE )
	public String getCenterNode() {
		return this.centerNode;
	}
	@StringSetter( CENTER_NODE )
	public void setCenterNode(final String centerNode) {
		this.centerNode = centerNode;
	}
	@StringGetter( RADIUS )
	public Double getRadius() {
		return this.radius;
	}
	@StringSetter( RADIUS )
	public void setRadius(final Double radius) {
		this.radius = radius;
	}
	@StringGetter( FLEXIBLE_TYPES )
	public String getFlexibleTypes() {
		return this.flexible_types;
	}
	@StringSetter( FLEXIBLE_TYPES )
	public void setFlexibleTypes(final String flexibleTypes) {
		this.flexible_types = flexibleTypes;
	}
	@StringGetter( PBETAS_FILE )
	public String getpBetasFile() {
		return pBetasFile;
	}
	@StringGetter( FATTRS_FILE )
	public String getfAttributesFile() {
		return fAttributesFile;
	}
	@StringSetter( PBETAS_FILE )
	public void setpBetasFile(String pBetasFile) {
		this.pBetasFile = pBetasFile;
	}
	@StringSetter( FATTRS_FILE )
	public void setfAttributesFile(String fAttributesFile) {
		this.fAttributesFile = fAttributesFile;
	}
	@StringGetter( ALGO )
	public Algotype getAlgorithm() {
		return this.algorithm;
	}
	@StringSetter( ALGO )
	public void setAlgorithm(Algotype algorithm) {
		this.algorithm = algorithm;
	}
	@StringGetter( TT_APPROX_LEVEL )
	public ApproximationLevel getTravelTimeApproximationLevel() {
		return this.tt_approximationLevel;
	}
	@StringSetter( TT_APPROX_LEVEL )
	public void setTravelTimeApproximationLevel(ApproximationLevel tt_approximationLevel) {
		this.tt_approximationLevel = tt_approximationLevel;
	}
	@StringGetter( MAXDISTANCEDCSCORE )
	public double getMaxDistanceDCScore() {
		return this.maxDistanceDCScore;
	}
	@StringSetter( MAXDISTANCEDCSCORE )
	public void setMaxDistanceDCScore(double maxSearchSpaceRadius) {
		this.maxDistanceDCScore = maxSearchSpaceRadius;
	}
	@StringGetter( PLANSELECTOR )
	public String getPlanSelector() {
		return this.planSelector;
	}
	@StringSetter( PLANSELECTOR )
	public void setPlanSelector(String planSelector) {
		this.planSelector = planSelector;
	}
	@StringGetter( RANDOMSEED )
	public long getRandomSeed() {
		return this.randomSeed;
	}
	@StringSetter( RANDOMSEED )
	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
	}
	// --------------------------------------------
	// --------------------------------------------
	@StringGetter( EPSDISTR )
	public EpsilonDistributionTypes getEpsilonDistribution() {
		return this.epsilonDistribution;
	}
	@Deprecated
	public void setEpsilonDistribution(String epsilonDistribution) {
		if (epsilonDistribution.equalsIgnoreCase( EpsilonDistributionTypes.gumbel.toString() )) {
			this.epsilonDistribution = EpsilonDistributionTypes.gumbel;
		} else if (epsilonDistribution.equalsIgnoreCase( EpsilonDistributionTypes.gaussian.toString() )) {
			this.epsilonDistribution = EpsilonDistributionTypes.gaussian;
		} else throw new RuntimeException("Unknown epsilon distribution type: " + epsilonDistribution + ". Aborting!");
	}

	@StringSetter( EPSDISTR )
	public void setEpsilonDistribution(EpsilonDistributionTypes epsilonDistribution) {
		this.epsilonDistribution = epsilonDistribution;
	}
	// --------------------------------------------
	// --------------------------------------------
	@StringGetter( SCALE_EPS )
	public String getEpsilonScaleFactors() {
		return this.epsilonScaleFactors;
	}
	@StringSetter( SCALE_EPS )
	public void setEpsilonScaleFactors(String epsilonScaleFactors) {
		this.epsilonScaleFactors = epsilonScaleFactors;
	}
//	public int getProbChoiceSetSize() {
//		return this.probChoiceSetSize;
//	}
//	public void setProbChoiceSetSize(int probChoiceSetSize) {
//		this.probChoiceSetSize = probChoiceSetSize;
//	}
@StringGetter( PKVALS_FILE )
	public String getpkValuesFile() {
		return this.pkValuesFile;
	}
	@StringSetter( PKVALS_FILE )
	public void setpkValuesFile(String kValuesFile) {
		this.pkValuesFile = kValuesFile;
	}
	@StringGetter( FKVALS_FILE )
	public String getfkValuesFile() {
		return fkValuesFile;
	}
	@StringSetter( FKVALS_FILE )
	public void setfkValuesFile(String kValuesFile) {
		this.fkValuesFile = kValuesFile;
	}
	@StringGetter( MAXDCS_FILE )
	public String getMaxEpsFile() {
		return this.maxDCScoreFile;
	}
	@StringSetter( MAXDCS_FILE )
	public void setMaxEpsFile(String maxEpsFile) {
		this.maxDCScoreFile = maxEpsFile;
	}
	@StringGetter( PREFS_FILE )
	public String getPrefsFile() {
		return this.prefsFile;
	}
	@StringSetter( PREFS_FILE )
	public void setPrefsFile(String prefsFile) {
		this.prefsFile = prefsFile;
	}
	@StringGetter( ANALYSIS_BOUNDARY )
	public double getAnalysisBoundary() {
		return this.analysisBoundary;
	}
	@StringSetter( ANALYSIS_BOUNDARY )
	public void setAnalysisBoundary(double analysisBoundary) {
		this.analysisBoundary = analysisBoundary;
	}
	@StringGetter( ANALYSIS_BINSIZE )
	public double getAnalysisBinSize() {
		return this.analysisBinSize;
	}
	@StringSetter( ANALYSIS_BINSIZE )
	public void setAnalysisBinSize(double analysisBinSize) {
		this.analysisBinSize = analysisBinSize;
	}
	// --------------------------------------------
	// --------------------------------------------
	@Deprecated // should be id, not long.  Should be a list --> better don't use
	@StringGetter( IDEXCLUSION )
	public Long getIdExclusion() {
		return this.idExclusion;
	}
	@Deprecated // should be id, not long.  Should be a list --> better don't use
	@StringSetter( IDEXCLUSION )
	public void setIdExclusion(Long idExclusion) {
		this.idExclusion = idExclusion;
	}
	// --------------------------------------------
	// --------------------------------------------
	@StringGetter( DESTINATIONSAMPLE_PCT )
	public double getDestinationSamplePercent() {
		return this.destinationSamplePercent;
	}
	@Deprecated
	public void setDestinationSamplePercent(String destinationSamplePercent) {
		this.setDestinationSamplePercent(Double.parseDouble(destinationSamplePercent));
	}
	@StringSetter( DESTINATIONSAMPLE_PCT )
	public void setDestinationSamplePercent(double destinationSamplePercent) {
		this.destinationSamplePercent = destinationSamplePercent;
	}
	
//	public InternalPlanDataStructure getInternalPlanDataStructure() {
//		return this.internalPlanDataStructure;
//	}
	
//	public void setInternalPlanDataStructure(InternalPlanDataStructure internalPlanDataStructure) {
//		this.internalPlanDataStructure = internalPlanDataStructure;
//	}
@StringGetter( USE_CONFIG_PARAMS_FOR_SCORING )
	public boolean getUseConfigParamsForScoring() {
		return this.useConfigParamsForScoring;
	}
	@StringSetter( USE_CONFIG_PARAMS_FOR_SCORING )
	public void setUseConfigParamsForScoring(boolean useConfigParamsForScoring) {
		this.useConfigParamsForScoring = useConfigParamsForScoring;
	}
	@StringGetter( USE_INDIVIDUAL_SCORING_PARAMETERS )
	public boolean getUseIndividualScoringParameters() {
		return this.useIndividualScoringParameters;
	}
	@StringSetter( USE_INDIVIDUAL_SCORING_PARAMETERS )
	public void setUseIndividualScoringParameters(boolean useIndividualScoringParameters) {
		this.useIndividualScoringParameters = useIndividualScoringParameters;
	}
	
//	public boolean getReUseTemporaryPlans() {
//		return this.reUseTemporaryPlans;
//	}
	
//	public void setReUseTemporaryPlans(boolean reUseTemporaryPlans) {
//		this.reUseTemporaryPlans = reUseTemporaryPlans;
//	}
}
