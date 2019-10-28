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
	private static final String GLOBALTRAVELSPEED_CAR = "travelSpeed_car";
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

	private static final String USE_CONFIG_PARAMS_FOR_SCORING = "useConfigParamsForScoring";
	private static final String USE_INDIVIDUAL_SCORING_PARAMETERS = "useIndividualScoringParameters";

	private double restraintFcnFactor = 0.0;
	private double restraintFcnExp = 0.0;
	private double scaleFactor = 1;
	private double travelSpeed_car = 8.5;
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
	private String pkValuesFile = null;
	private String fkValuesFile = null;
	private String pBetasFile = null;
	private String fAttributesFile = null;
	private String maxDCScoreFile = null;
	private String prefsFile = null;
	
	private double analysisBoundary = 200000;
	private double analysisBinSize = 20000;
	private Long idExclusion = null;
	private double destinationSamplePercent = 100.0;
	
	/* experimental stuff */
	private boolean useConfigParamsForScoring = true;
	private boolean useIndividualScoringParameters = true;

	public FrozenTastesConfigGroup() {
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
	//	@StringGetter( RESTR_FCN_FACTOR )
	@Override @Deprecated // TODO replace by replacable function
	public double getRestraintFcnFactor() {
		return this.restraintFcnFactor;
	}
	/**
	 * penalty roughly is restr_factor * #persons^restr_exp.  Only needed when facility load penalties are used
	 */
	//	@StringSetter( RESTR_FCN_FACTOR )
	@Deprecated // TODO replace by replacable function
	public void setRestraintFcnFactor(final double restraintFcnFactor) {
		this.restraintFcnFactor = restraintFcnFactor;
	}
	//	@StringGetter( RESTR_FCN_EXP )
	@Override @Deprecated // TODO replace by replacable function
	public double getRestraintFcnExp() {
		return this.restraintFcnExp;
	}
	/**
	 * penalty roughly is restr_factor * #persons^restr_exp.  Only needed when facility load penalties are used
	 */
	//	@StringSetter( RESTR_FCN_EXP )
	@Deprecated // TODO replace by replacable function
	public void setRestraintFcnExp(final double restraintFcnExp) {
		this.restraintFcnExp = restraintFcnExp;
	}
	/**
	 * inverse of population sample size.  Only needed when facility load penalties are used
	 */
	@Override
	@StringGetter( SCALEFACTOR )
	public double getScaleFactor() {
		return this.scaleFactor;
	}
	/**
	 * inverse of population sample size.  Only needed when facility load penalties are used
	 */
	@StringSetter( SCALEFACTOR )
	public void setScaleFactor( final double scaleFactor ) {
		this.scaleFactor = scaleFactor;
	}
	@StringGetter( GLOBALTRAVELSPEED_CAR )
	@Deprecated // TODO take from regular config
	public double getTravelSpeed_car() {
		return this.travelSpeed_car;
	}
	@StringSetter( GLOBALTRAVELSPEED_CAR )
	@Deprecated // TODO take from regular config
	public void setTravelSpeed_car(final double travelSpeed_car) {
		this.travelSpeed_car = travelSpeed_car;
	}
	//@StringGetter( CENTER_NODE )
	@Override @Deprecated // TODO replace by replaceable implementation behind interface
	public String getCenterNode() {
		return this.centerNode;
	}
	/**
	 * restrict facility search to radius around centerNode (defined by node ID)
	 */
	//	@StringSetter( CENTER_NODE )
	@Deprecated // TODO replace by replaceable implementation behind interface
	public void setCenterNode(final String centerNode) {
		this.centerNode = centerNode;
	}
	//	@StringGetter( RADIUS )
	@Override @Deprecated // TODO replace by replaceable implementation behind interface
	public Double getRadius() {
		return this.radius;
	}
	/**
	 * restrict facility search to radius around centerNode (defined by node ID)
	 */
	//	@StringSetter( RADIUS )
	@Deprecated // TODO replace by replaceable implementation behind interface
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
//	@StringGetter( PBETAS_FILE )
	@Deprecated // TODO replace by Attributable
	public String getpBetasFile() {
		return pBetasFile;
	}
//	@StringGetter( FATTRS_FILE )
	@Deprecated // TODO replace by Attributable
	public String getfAttributesFile() {
		return fAttributesFile;
	}
//	@StringSetter( PBETAS_FILE )
	@Deprecated // TODO replace by Attributable
	public void setpBetasFile(String pBetasFile) {
		this.pBetasFile = pBetasFile;
	}
//	@StringSetter( FATTRS_FILE )
	@Deprecated // TODO replace by Attributable
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
	@StringGetter( EPSDISTR )
	public EpsilonDistributionTypes getEpsilonDistribution() {
		return this.epsilonDistribution;
	}
	@StringSetter( EPSDISTR )
	public void setEpsilonDistribution(EpsilonDistributionTypes epsilonDistribution) {
		this.epsilonDistribution = epsilonDistribution;
	}
	@Override @StringGetter( SCALE_EPS )
	public String getEpsilonScaleFactors() {
		return this.epsilonScaleFactors;
	}
	/**
	 * I think that this is how much to scale the epsilons for each activity type.  comma-separated list, corresponding to comma-separated list of flexible
	 * activity types.
	 */
	@StringSetter( SCALE_EPS )
	public void setEpsilonScaleFactors(String epsilonScaleFactors) {
		this.epsilonScaleFactors = epsilonScaleFactors;
	}
//	@StringGetter( PKVALS_FILE )
	@Deprecated // TODO replace by Attributable
	public String getpkValuesFile() {
		return this.pkValuesFile;
	}
//	@StringSetter( PKVALS_FILE )
	@Deprecated // TODO replace by Attributable
	public void setpkValuesFile(String kValuesFile) {
		this.pkValuesFile = kValuesFile;
	}
//	@StringGetter( FKVALS_FILE )
	@Deprecated // TODO replace by Attributable
	public String getfkValuesFile() {
		return fkValuesFile;
	}
//	@StringSetter( FKVALS_FILE )
	@Deprecated // TODO replace by Attributable
	public void setfkValuesFile(String kValuesFile) {
		this.fkValuesFile = kValuesFile;
	}
//	@StringGetter( MAXDCS_FILE )
	@Deprecated // TODO replace by Attributable
	public String getMaxEpsFile() {
		return this.maxDCScoreFile;
	}
//	@StringSetter( MAXDCS_FILE )
	@Deprecated // TODO replace by Attributable
	public void setMaxEpsFile(String maxEpsFile) {
		this.maxDCScoreFile = maxEpsFile;
	}
//	@StringGetter( PREFS_FILE )
	@Deprecated // TODO replace by Attributable (or don't use at all)
	public String getPrefsFile() {
		return this.prefsFile;
	}
//	@StringSetter( PREFS_FILE )
	@Deprecated // TODO replace by Attributable (or don't use at all)
	public void setPrefsFile(String prefsFile) {
		this.prefsFile = prefsFile;
	}
//	@StringGetter( ANALYSIS_BOUNDARY )
	@Deprecated // TODO replace by replaceable implementation behind interface (and move to analysis contrib)
	public double getAnalysisBoundary() {
		return this.analysisBoundary;
	}

	/**
	 * Some distance cut-off for {@link DistanceStats}.  But with a more flexible data structure there this would not be necessary.
	 */
//	@StringSetter( ANALYSIS_BOUNDARY )
	@Deprecated // TODO replace by replaceable implementation behind interface (and move to analysis contrib)
	public void setAnalysisBoundary(double analysisBoundary) {
		this.analysisBoundary = analysisBoundary;
	}
//	@StringGetter( ANALYSIS_BINSIZE )
	@Deprecated // TODO replace by replaceable implementation behind interface (and move to analysis contrib)
	public double getAnalysisBinSize() {
		return this.analysisBinSize;
	}

	/**
	 * Bin size for {@link DistanceStats}.
	 */
//	@StringSetter( ANALYSIS_BINSIZE )
	@Deprecated // TODO replace by replaceable implementation behind interface (and move to analysis contrib)
	public void setAnalysisBinSize(double analysisBinSize) {
		this.analysisBinSize = analysisBinSize;
	}
//	@StringGetter( IDEXCLUSION )
	@Deprecated // TODO should be id, not long.  Should be a list --> better don't use
	public Long getIdExclusion() {
		return this.idExclusion;
	}
//	@StringSetter( IDEXCLUSION )
	@Deprecated // TODO should be id, not long.  Should be a list --> better don't use
	public void setIdExclusion(Long idExclusion) {
		this.idExclusion = idExclusion;
	}
	@StringGetter( DESTINATIONSAMPLE_PCT )
	public double getDestinationSamplePercent() {
		return this.destinationSamplePercent;
	}
	@StringSetter( DESTINATIONSAMPLE_PCT )
	public void setDestinationSamplePercent(double destinationSamplePercent) {
		this.destinationSamplePercent = destinationSamplePercent;
	}
	@StringGetter( USE_CONFIG_PARAMS_FOR_SCORING )
	// yyyyyy should always be true
	public boolean getUseConfigParamsForScoring() {
		return this.useConfigParamsForScoring;
	}
	@StringSetter( USE_CONFIG_PARAMS_FOR_SCORING )
	// yyyyyy should always be true
	public void setUseConfigParamsForScoring(boolean useConfigParamsForScoring) {
		this.useConfigParamsForScoring = useConfigParamsForScoring;
	}

//	@StringGetter( USE_INDIVIDUAL_SCORING_PARAMETERS )
	public boolean getUseIndividualScoringParameters() {
		return this.useIndividualScoringParameters;
	}
//	@StringSetter( USE_INDIVIDUAL_SCORING_PARAMETERS )
	public void setUseIndividualScoringParameters(boolean useIndividualScoringParameters) {
		this.useIndividualScoringParameters = useIndividualScoringParameters;
	}
}
