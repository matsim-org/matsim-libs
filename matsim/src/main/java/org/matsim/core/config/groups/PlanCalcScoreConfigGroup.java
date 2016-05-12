/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelScoringConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

/**Design decisions:<ul>
 * <li> I have decided to modify those setters/getters that do not use SI units such that the units are attached.
 * This means all the utility parameters which are "per hour" instead of "per second".  kai, dec'10
 * <li> Note that a similar thing is not necessary for money units since money units do not need to be specified (they are always
 * implicit).  kai, dec'10
 * <li> The parameter names in the config file are <i>not</i> changed in this way since this would mean a public api change.  kai, dec'10
 * </ul>
 * @author nagel
 *
 */
public final class PlanCalcScoreConfigGroup extends ConfigGroup {

	private static final Logger log = Logger.getLogger(PlanCalcScoreConfigGroup.class);

	public static final String GROUP_NAME = "planCalcScore";

	private static final String LEARNING_RATE = "learningRate";
	private static final String BRAIN_EXP_BETA = "BrainExpBeta";
	private static final String PATH_SIZE_LOGIT_BETA = "PathSizeLogitBeta";
	private static final String LATE_ARRIVAL = "lateArrival";
	private static final String EARLY_DEPARTURE = "earlyDeparture";
	private static final String PERFORMING = "performing";

	private static final String WAITING  = "waiting";
	private static final String WAITING_PT  = "waitingPt";

	private static final String WRITE_EXPERIENCED_PLANS = "writeExperiencedPlans";

	private static final String MARGINAL_UTL_OF_MONEY = "marginalUtilityOfMoney" ;

	private static final String UTL_OF_LINE_SWITCH = "utilityOfLineSwitch" ;

	private final ReflectiveDelegate delegate = new ReflectiveDelegate();


	public PlanCalcScoreConfigGroup() {
		super(GROUP_NAME);

		this.addScoringParameters( new ScoringParameterSet() );

		this.addParameterSet( new ModeParams( TransportMode.car ) );
		this.addParameterSet( new ModeParams( TransportMode.pt ) );
		this.addParameterSet( new ModeParams( TransportMode.walk ) );
		this.addParameterSet( new ModeParams( TransportMode.bike ) );
		this.addParameterSet( new ModeParams( TransportMode.other ) );
		
		// yyyyyy find better solution for this. kai, dec'15
		{
			ActivityParams params = new ActivityParams("car interaction") ;
			params.setScoringThisActivityAtAll(false);
			this.addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("bike interaction") ;
			params.setScoringThisActivityAtAll(false);
			this.addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("other interaction") ;
			params.setScoringThisActivityAtAll(false);
			this.addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("walk interaction") ; 
			params.setScoringThisActivityAtAll(false);
			this.addActivityParams(params);
			// bushwhacking_walk---network_walk---bushwhacking_walk
		}
	}


	// ---
	
	private static final String USING_OLD_SCORING_BELOW_ZERO_UTILITY_DURATION = "usingOldScoringBelowZeroUtilityDuration" ;

	/**
	 * can't set this from outside java since for the time being it is not useful there. kai, dec'13
	 */
	private boolean memorizingExperiencedPlans = false ;

	/**
	 * This is the key for customizable.  where should this go?
	 */
	public static final String EXPERIENCED_PLAN_KEY = "experiencedPlan";
	
	// ---
	private static final String FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA = "fractionOfIterationsToStartScoreMSA" ;
	// ---

	@Override
	public String getValue(final String key) {
		throw new IllegalArgumentException(key + ": getValue access disabled; use direct getter");
	}

	@Override
	public void addParam(final String key, final String value) {
        if (key.startsWith("monetaryDistanceCostRate")) {
			throw new RuntimeException("Please use monetaryDistanceRate (without `cost').  Even better, use config v2, "
					+ "mode-parameters (see output of any recent run), and mode-specific monetary "
					+ "distance rate.") ;
		}
		else if ( WAITING_PT.equals( key ) ) {
			setMarginalUtlOfWaitingPt_utils_hr( Double.parseDouble( value ) );
		}

		// backward compatibility: underscored
		else if (key.startsWith("activityType_")) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityType_".length()));

			actParams.setActivityType(value);
			getScoringParameters( null ).removeParameterSet(actParams);
			addActivityParams( actParams );
		}
		else if (key.startsWith("activityPriority_")) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityPriority_".length()));
			actParams.setPriority(Double.parseDouble(value));
		}
		else if (key.startsWith("activityTypicalDuration_")) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityTypicalDuration_".length()));
			actParams.setTypicalDuration(Time.parseTime(value));
		}
		else if (key.startsWith("activityMinimalDuration_")) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityMinimalDuration_".length()));
			actParams.setMinimalDuration(Time.parseTime(value));
		}
		else if (key.startsWith("activityOpeningTime_")) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityOpeningTime_".length()));
			actParams.setOpeningTime(Time.parseTime(value));
		}
		else if (key.startsWith("activityLatestStartTime_")) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityLatestStartTime_".length()));
			actParams.setLatestStartTime(Time.parseTime(value));
		}
		else if (key.startsWith("activityEarliestEndTime_")) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityEarliestEndTime_".length()));
			actParams.setEarliestEndTime(Time.parseTime(value));
		}
		else if (key.startsWith("activityClosingTime_")) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityClosingTime_".length()));
			actParams.setClosingTime(Time.parseTime(value));
		}
		else if (key.startsWith("scoringThisActivityAtAll_")) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring("scoringThisActivityAtAll_".length()));
			actParams.setScoringThisActivityAtAll( Boolean.parseBoolean(value) );
		}
		else if (key.startsWith("traveling_")) {
			ModeParams modeParams = getOrCreateModeParams(key.substring("traveling_".length()));
			modeParams.setMarginalUtilityOfTraveling(Double.parseDouble(value));
		}
		else if (key.startsWith("marginalUtlOfDistance_")) {
			ModeParams modeParams = getOrCreateModeParams(key.substring("marginalUtlOfDistance_".length()));
			modeParams.setMarginalUtilityOfDistance(Double.parseDouble(value));
		}
		else if (key.startsWith("monetaryDistanceRate_")) {
			ModeParams modeParams = getOrCreateModeParams(key.substring("monetaryDistanceRate_".length()));
			modeParams.setMonetaryDistanceRate(Double.parseDouble(value));
		}
		else if ( "monetaryDistanceRateCar".equals(key) ){
			ModeParams modeParams = getOrCreateModeParams( TransportMode.car ) ;
			modeParams.setMonetaryDistanceRate(Double.parseDouble(value));
		}
		else if ( "monetaryDistanceRatePt".equals(key) ){
			ModeParams modeParams = getOrCreateModeParams( TransportMode.pt ) ;
			modeParams.setMonetaryDistanceRate(Double.parseDouble(value));
		}
		else if (key.startsWith("constant_")) {
			ModeParams modeParams = getOrCreateModeParams(key.substring("constant_".length()));
			modeParams.setConstant(Double.parseDouble(value));
		}

		// backward compatibility: "typed" traveling
		else if ("traveling".equals(key)) {
			this.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(Double.parseDouble(value));
		}
		else if ("travelingPt".equals(key)) {
			this.getModes().get(TransportMode.pt).setMarginalUtilityOfTraveling(Double.parseDouble(value));
		}
		else if ("travelingWalk".equals(key)) {
			this.getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(Double.parseDouble(value));
		}
		else if ("travelingOther".equals(key)) {
			this.getModes().get(TransportMode.other).setMarginalUtilityOfTraveling(Double.parseDouble(value));
		}
		else if ("travelingBike".equals(key)) {
			this.getModes().get(TransportMode.bike).setMarginalUtilityOfTraveling(Double.parseDouble(value));
		}

		// backward compatibility: "typed" util of distance
		else if ("marginalUtlOfDistanceCar".equals(key)){
			this.getModes().get(TransportMode.car).setMarginalUtilityOfDistance(Double.parseDouble(value));
		}
		else if ("marginalUtlOfDistancePt".equals(key)){
			this.getModes().get(TransportMode.pt).setMarginalUtilityOfDistance(Double.parseDouble(value));
		}
		else if ("marginalUtlOfDistanceWalk".equals(key)){
			this.getModes().get(TransportMode.walk).setMarginalUtilityOfDistance(Double.parseDouble(value));
		}
		else if ("marginalUtlOfDistanceOther".equals(key)){
			this.getModes().get(TransportMode.other).setMarginalUtilityOfDistance(Double.parseDouble(value));
		}

		// backward compatibility: "typed" constants
		else if ( "constantCar".equals(key)) {
			getModes().get(TransportMode.car).setConstant(Double.parseDouble(value));
		}
		else if ( "constantWalk".equals(key)) {
			getModes().get(TransportMode.walk).setConstant(Double.parseDouble(value));
		}
		else if ( "constantOther".equals(key)) {
			getModes().get(TransportMode.other).setConstant(Double.parseDouble(value));
		}
		else if ( "constantPt".equals(key)) {
			getModes().get(TransportMode.pt).setConstant(Double.parseDouble(value));
		}
		else if ( "constantBike".equals(key)) {
			getModes().get(TransportMode.bike).setConstant(Double.parseDouble(value));
		}

		// old-fashioned scoring parameters: default subpopulation
		else if ( Arrays.asList( LATE_ARRIVAL ,
						EARLY_DEPARTURE ,
						PERFORMING ,
						MARGINAL_UTL_OF_MONEY ,
						UTL_OF_LINE_SWITCH ,
						WAITING ).contains( key ) ) {
			getScoringParameters( null ).addParam( key , value );
		}

		else {
			delegate.addParam(key, value);
		}
	}

	/* for the backward compatibility nonsense */
	private final Map<String, ActivityParams> activityTypesByNumber = new HashMap< >();
	private ActivityParams getActivityTypeByNumber(final String number) {
		ActivityParams actType = this.activityTypesByNumber.get(number);
		if ( (actType == null) ) {
			// not sure what this means, but I found it so...
			// TD, sep'14
			actType = new ActivityParams(number);
			this.activityTypesByNumber.put(number, actType);
			addParameterSet( actType );
		}
		return actType;
	}


	public ModeParams getOrCreateModeParams(String modeName) {
		return getScoringParameters( null ).getOrCreateModeParams( modeName );
	}

	@Override
	public Map<String, String> getParams() {
		return delegate.getParams();
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA, "fraction of iterations at which MSA score averaging is started. The matsim theory department " +
				"suggests to use this together with switching off choice set innovation (where a similar switch exists), but it has not been tested yet.") ;
		map.put(USING_OLD_SCORING_BELOW_ZERO_UTILITY_DURATION, "There used to be a plateau between duration=0 and duration=zeroUtilityDuration. "
				+ "This caused durations to evolve to zero once they were below zeroUtilityDuration, causing problems.  Only use this switch if you need to be "
				+ "backwards compatible with some old results.  (changed nov'13)") ;
		map.put(PERFORMING,"[utils/hr] marginal utility of doing an activity.  normally positive.  also the opportunity cost of " +
				"time if agent is doing nothing.  MATSim separates the resource value of time from the direct (dis)utility of travel time, see, e.g., "
				+ "Boerjesson and Eliasson, TR-A 59 (2014) 144-158.");
		map.put(LATE_ARRIVAL, "[utils/hr] utility for arriving late (i.e. after the latest start time).  normally negative") ;
		map.put(EARLY_DEPARTURE, "[utils/hr] utility for departing early (i.e. before the earliest end time).  Normally negative.  Probably " +
				"implemented correctly, but not tested." );
		map.put(WAITING, "[utils/hr] additional marginal utility for waiting. normally negative. this comes on top of the opportunity cost of time.  Probably " +
				"implemented correctly, but not tested.") ;
		map.put(WAITING_PT, "[utils/hr] additional marginal utility for waiting for a pt vehicle. normally negative. this comes on top of the opportunity cost " +
				"of time. Default: if not set explicitly, it is equal to traveling_pt!!!" ) ;
		map.put(BRAIN_EXP_BETA, "logit model scale parameter. default: 1.  Has name and default value for historical reasons " +
				"(see Bryan Raney's phd thesis).") ;
		map.put(LEARNING_RATE, "new_score = (1-learningRate)*old_score + learningRate * score_from_mobsim.  learning rates " +
				"close to zero emulate score averaging, but slow down initial convergence") ;
		map.put(UTL_OF_LINE_SWITCH, "[utils] utility of switching a line (= transfer penalty).  Normally negative") ;
		map.put(MARGINAL_UTL_OF_MONEY, "[utils/unit_of_money] conversion of money (e.g. toll, distance cost) into utils. Normall positive (i.e. toll/cost/fare are processed as negative amounts of money)." ) ;
		map.put(WRITE_EXPERIENCED_PLANS, "write a plans file in each iteration directory which contains what each agent actually did, and the score it received.");

		return map;
	}

	public Collection<String> getActivityTypes() {
		return getScoringParameters( null ).getActivityParamsPerType().keySet();
	}

	public Collection<ActivityParams> getActivityParams() {
		return getScoringParameters( null ).getActivityParams();
	}

	public Map<String, ModeParams> getModes() {
		return getScoringParameters( null ).getModes();
	}

	public Map<String, ScoringParameterSet> getScoringParametersPerSubpopulation() {
		@SuppressWarnings("unchecked")
		final Collection<ScoringParameterSet> parameters = (Collection<ScoringParameterSet>) getParameterSets( ScoringParameterSet.SET_TYPE );
		final Map<String, ScoringParameterSet> map = new LinkedHashMap< >();

		for ( ScoringParameterSet pars : parameters ) {
			if ( this.isLocked() ) {
				pars.setLocked();
			}
			map.put( pars.getSubpopulation() , pars );
		}

		return map;
	}

	/* direct access */

	public double getMarginalUtlOfWaitingPt_utils_hr() {
		return getScoringParameters( null ).getMarginalUtlOfWaitingPt_utils_hr( );
	}

	public void setMarginalUtlOfWaitingPt_utils_hr(double val) {
		getScoringParameters( null ).setMarginalUtlOfWaitingPt_utils_hr( val );
	}

	public ActivityParams getActivityParams(final String actType) {
		return getScoringParameters( null ).getActivityParams( actType );
	}

	public ScoringParameterSet getScoringParameters(String subpopulation) {
		final ScoringParameterSet params = getScoringParametersPerSubpopulation().get( subpopulation );
		// If no config parameters defined for a specific subpopulation,
		// use the ones of the "default" subpopulation
		return params != null ? params : getScoringParametersPerSubpopulation().get( null );
	}

	public ScoringParameterSet getOrCreateScoringParameters(String subpopulation) {
		ScoringParameterSet params = getScoringParametersPerSubpopulation().get( subpopulation );

		if ( params ==null ) {
			params = new ScoringParameterSet( subpopulation );
			this.addScoringParameters( params );
		}

		return params;
	}

	@Override
	public void addParameterSet( final ConfigGroup set ) {
		switch ( set.getName() ) {
			case ActivityParams.SET_TYPE:
				addActivityParams( (ActivityParams) set );
				break;
			case ModeParams.SET_TYPE:
				addModeParams( (ModeParams) set );
				break;
			case ScoringParameterSet.SET_TYPE:
				addScoringParameters( (ScoringParameterSet) set );
				break;
			default:
				throw new IllegalArgumentException( set.getName() );
		}
	}

	public void addScoringParameters(final ScoringParameterSet params) {
		final ScoringParameterSet previous = this.getScoringParameters(params.getSubpopulation());

		if ( previous != null ) {
			log.info("scoring parameters for subpopulation " + previous.getSubpopulation() + " were just overwritten.") ;

			final boolean removed = removeParameterSet( previous );
			if ( !removed ) throw new RuntimeException( "problem replacing scoring params " );
		}

		super.addParameterSet( params );
	}


	public void addModeParams(final ModeParams params) {
		getScoringParameters( null ).addModeParams( params );
	}

	public void addActivityParams(final ActivityParams params) {
		getScoringParameters( null ).addActivityParams( params );
	}
	
	public enum TypicalDurationScoreComputation { uniform, relative } ;


	/* parameter set handling */
	@Override
	public ConfigGroup createParameterSet(final String type) {
		switch ( type ) {
			case ActivityParams.SET_TYPE:
				return new ActivityParams();
			case ModeParams.SET_TYPE:
				return new ModeParams();
			case ScoringParameterSet.SET_TYPE:
				return new ScoringParameterSet();
			default:
				throw new IllegalArgumentException( type );
		}
	}

	@Override
	protected void checkParameterSet( final ConfigGroup module ) {
		switch ( module.getName() ) {
			case ScoringParameterSet.SET_TYPE:
				if ( !(module instanceof ScoringParameterSet) ) {
					throw new RuntimeException( "wrong class for "+module );
				}
				final String s = ((ScoringParameterSet) module).getSubpopulation();
				if ( getScoringParameters( s ) != null ) {
					throw new IllegalStateException( "already a parameter set for subpopulation "+s );
				}
				break;
			default:
				throw new IllegalArgumentException( module.getName() );
		}
	}


	public boolean isMemorizingExperiencedPlans() {
		return this.memorizingExperiencedPlans ;
	}

	public void setMemorizingExperiencedPlans(boolean memorizingExperiencedPlans) {
		this.memorizingExperiencedPlans = memorizingExperiencedPlans;
	}


	public double getLearningRate() {
		return delegate.getLearningRate();
	}

	public void setLearningRate(double learningRate) {
		delegate.setLearningRate(learningRate);
	}

	public double getBrainExpBeta() {
		return delegate.getBrainExpBeta();
	}

	public void setBrainExpBeta(double brainExpBeta) {
		delegate.setBrainExpBeta(brainExpBeta);
	}

	public double getPathSizeLogitBeta() {
		return delegate.getPathSizeLogitBeta();
	}

	public void setPathSizeLogitBeta(double beta) {
		delegate.setPathSizeLogitBeta(beta);
	}

	public double getLateArrival_utils_hr() {
		return getScoringParameters( null ).getLateArrival_utils_hr();
	}

	public void setLateArrival_utils_hr(double lateArrival) {
		getScoringParameters( null ).setLateArrival_utils_hr(lateArrival);
	}

	public double getEarlyDeparture_utils_hr() {
		return getScoringParameters( null ).getEarlyDeparture_utils_hr();
	}

	public void setEarlyDeparture_utils_hr(double earlyDeparture) {
		getScoringParameters( null ).setEarlyDeparture_utils_hr(earlyDeparture);
	}

	public double getPerforming_utils_hr() {
		return getScoringParameters( null ).getPerforming_utils_hr();
	}

	public void setPerforming_utils_hr(double performing) {
		getScoringParameters( null ).setPerforming_utils_hr(performing);
	}

	public double getMarginalUtilityOfMoney() {
		return getScoringParameters( null ).getMarginalUtilityOfMoney();
	}

	public void setMarginalUtilityOfMoney(double marginalUtilityOfMoney) {
		getScoringParameters( null ).setMarginalUtilityOfMoney(marginalUtilityOfMoney);
	}

	public double getUtilityOfLineSwitch() {
		return getScoringParameters( null ).getUtilityOfLineSwitch();
	}

	public void setUtilityOfLineSwitch(double utilityOfLineSwitch) {
		getScoringParameters( null ).setUtilityOfLineSwitch(utilityOfLineSwitch);
	}

	public boolean isUsingOldScoringBelowZeroUtilityDuration() {
		return delegate.isUsingOldScoringBelowZeroUtilityDuration();
	}

	public void setUsingOldScoringBelowZeroUtilityDuration(
			boolean usingOldScoringBelowZeroUtilityDuration) {
		delegate.setUsingOldScoringBelowZeroUtilityDuration(usingOldScoringBelowZeroUtilityDuration);
	}

	public boolean isWriteExperiencedPlans() {
		return delegate.isWriteExperiencedPlans();
	}

	public void setWriteExperiencedPlans(boolean writeExperiencedPlans) {
		delegate.setWriteExperiencedPlans(writeExperiencedPlans);
	}

	public double getMarginalUtlOfWaiting_utils_hr() {
		return getScoringParameters( null ).getMarginalUtlOfWaiting_utils_hr();
	}
	public void setMarginalUtlOfWaiting_utils_hr(double waiting) {
		getScoringParameters( null ).setMarginalUtlOfWaiting_utils_hr(waiting);
	}
	
	public void setFractionOfIterationsToStartScoreMSA( Double val ) {
		delegate.setFractionOfIterationsToStartScoreMSA(val);
	}
	public Double getFractionOfIterationsToStartScoreMSA() {
		return delegate.getFractionOfIterationsToStartScoreMSA() ;
	}
	@Override
	public final void setLocked() {
		super.setLocked();
		this.delegate.setLocked();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// CLASSES
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static class ActivityParams extends ReflectiveConfigGroup implements MatsimParameters {
		// in normal pgm execution, code will presumably lock instance of PlanCalcScoreConfigGroup, but not instance of
		// ActivityParams.  I will try to pass the locked setting through the getters. kai, jun'15

		private static final String TYPICAL_DURATION_SCORE_COMPUTATION = "typicalDurationScoreComputation";
		final static String SET_TYPE = "activityParams";
		private String type;
		private double priority = 1.0;
		private double typicalDuration = Time.UNDEFINED_TIME;
		private double minimalDuration = Time.UNDEFINED_TIME;
		private double openingTime = Time.UNDEFINED_TIME;
		private double latestStartTime = Time.UNDEFINED_TIME;
		private double earliestEndTime = Time.UNDEFINED_TIME;
		private double closingTime = Time.UNDEFINED_TIME;
		private boolean scoringThisActivityAtAll = true ;

		private TypicalDurationScoreComputation typicalDurationScoreComputation = TypicalDurationScoreComputation.uniform ;


		public ActivityParams() {
			super( SET_TYPE );
		}

		public ActivityParams(final String type) {
			super( SET_TYPE );
			this.type = type;
		}

		@Override
		public Map<String, String> getComments() {
			final Map<String, String> map = super.getComments();
			map.put( TYPICAL_DURATION_SCORE_COMPUTATION,  "method to compute score at typical duration.  Use "
					+ TypicalDurationScoreComputation.uniform + " for backwards compatibility (all activities same score; higher proba to drop long acts).") ;
			return map ;
		}

		@StringGetter(TYPICAL_DURATION_SCORE_COMPUTATION)
		public TypicalDurationScoreComputation getTypicalDurationScoreComputation() {
			return this.typicalDurationScoreComputation ;
		}
		@StringSetter(TYPICAL_DURATION_SCORE_COMPUTATION)
		public void setTypicalDurationScoreComputation( TypicalDurationScoreComputation str ) {
			testForLocked() ;
			this.typicalDurationScoreComputation = str ;
		}

		@StringGetter( "activityType" )
		public String getActivityType() {
			return this.type;
		}

		@StringSetter( "activityType" )
		public void setActivityType(final String type) {
			testForLocked() ;
			this.type = type;
		}

		@StringGetter( "priority" )
		public double getPriority() {
			return this.priority;
		}

		@StringSetter( "priority" )
		public void setPriority(final double priority) {
			testForLocked() ;
			this.priority = priority;
		}

		@StringGetter( "typicalDuration" )
		private String getTypicalDurationString() {
			return Time.writeTime( getTypicalDuration() );
		}

		public double getTypicalDuration() {
			return this.typicalDuration;
		}

		@StringSetter( "typicalDuration" )
		private void setTypicalDuration(final String typicalDuration) {
			testForLocked() ;
			setTypicalDuration( Time.parseTime( typicalDuration ) );
		}

		public void setTypicalDuration(final double typicalDuration) {
			testForLocked() ;
			this.typicalDuration = typicalDuration;
		}

		@StringGetter( "minimalDuration" )
		private String getMinimalDurationString() {
			return Time.writeTime( getMinimalDuration() );
		}

		public double getMinimalDuration() {
			return this.minimalDuration;
		}

		@StringSetter( "minimalDuration" )
		private void setMinimalDuration(final String minimalDuration) {
			testForLocked() ;
			setMinimalDuration( Time.parseTime( minimalDuration ) );
		}

		private static int minDurCnt=0 ;
		public void setMinimalDuration(final double minimalDuration) {
			testForLocked() ;
			if ((minimalDuration != Time.UNDEFINED_TIME) && (minDurCnt<1) ) {
				minDurCnt++ ;
				log.warn("Setting minimalDuration different from zero is discouraged.  It is probably implemented correctly, " +
						"but there is as of now no indication that it makes the results more realistic.  KN, Sep'08" + Gbl.ONLYONCE );
			}
			this.minimalDuration = minimalDuration;
		}

		@StringGetter( "openingTime" )
		private String getOpeningTimeString() {
			return Time.writeTime( getOpeningTime() );
		}

		public double getOpeningTime() {
			return this.openingTime;
		}
		@StringSetter( "openingTime" )
		private void setOpeningTime(final String openingTime) {
			testForLocked() ;
			setOpeningTime( Time.parseTime( openingTime ) );
		}

		public void setOpeningTime(final double openingTime) {
			testForLocked() ;
			this.openingTime = openingTime;
		}

		@StringGetter( "latestStartTime" )
		private String getLatestStartTimeString() {
			return Time.writeTime( getLatestStartTime() );
		}

		public double getLatestStartTime() {
			return this.latestStartTime;
		}
		@StringSetter( "latestStartTime" )
		private void setLatestStartTime(final String latestStartTime) {
			testForLocked() ;
			setLatestStartTime( Time.parseTime( latestStartTime ) );
		}

		public void setLatestStartTime(final double latestStartTime) {
			testForLocked() ;
			this.latestStartTime = latestStartTime;
		}

		@StringGetter( "earliestEndTime" )
		private String getEarliestEndTimeString() {
			return Time.writeTime( getEarliestEndTime() );
		}

		public double getEarliestEndTime() {
			return this.earliestEndTime;
		}
		@StringSetter( "earliestEndTime" )
		private void setEarliestEndTime(final String earliestEndTime) {
			testForLocked() ;
			setEarliestEndTime( Time.parseTime( earliestEndTime ) );
		}

		public void setEarliestEndTime(final double earliestEndTime) {
			testForLocked() ;
			this.earliestEndTime = earliestEndTime;
		}

		@StringGetter( "closingTime" )
		private String getClosingTimeString() {
			return Time.writeTime( getClosingTime() );
		}

		public double getClosingTime() {
			return this.closingTime;
		}
		@StringSetter( "closingTime" )
		private void setClosingTime(final String closingTime) {
			testForLocked() ;
			setClosingTime( Time.parseTime( closingTime ) );
		}

		public void setClosingTime(final double closingTime) {
			testForLocked() ;
			this.closingTime = closingTime;
		}

		@StringGetter( "scoringThisActivityAtAll" )
		public boolean isScoringThisActivityAtAll() {
			return scoringThisActivityAtAll;
		}

		@StringSetter( "scoringThisActivityAtAll" )
		public void setScoringThisActivityAtAll(boolean scoringThisActivityAtAll) {
			testForLocked() ;
			this.scoringThisActivityAtAll = scoringThisActivityAtAll;
		}
	}

	public static class ModeParams extends ReflectiveConfigGroup implements MatsimParameters {

		final static String SET_TYPE = "modeParams";

		private static final String MONETARY_DISTANCE_RATE = "monetaryDistanceRate";
		private static final String MARGINAL_UTILITY_OF_TRAVELING = "marginalUtilityOfTraveling_util_hr";

		private String mode = null;
		private double traveling = -6.0;
		private double distance = 0.0;
		private double monetaryDistanceRate = 0.0;
		private double constant = 0.0;

		public ModeParams(final String mode) {
			super( SET_TYPE );
			setMode( mode );
		}

		ModeParams() {
			super( SET_TYPE );
		}

		@Override
		public Map<String, String> getComments() {
			final Map<String, String> map = super.getComments();
			map.put( MARGINAL_UTILITY_OF_TRAVELING, "[utils/hr] additional marginal utility of traveling.  normally negative.  this comes on top " +
					"of the opportunity cost of time");
			map.put( "marginalUtilityOfDistance_util_m", "[utils/m] utility of walking per m, normally negative.  this is " +
					"on top of the time (dis)utility.") ;
			map.put(MONETARY_DISTANCE_RATE, "[unit_of_money/m] conversion of distance into money. Normally negative." ) ;
			map.put("constant",  "[utils] alternative-specific constant.  no guarantee that this is used anywhere. " +
					"default=0 to be backwards compatible for the time being" ) ;
			return map;
		}

		@StringSetter( "mode" )
		public void setMode( final String mode ) {
			testForLocked() ;
			this.mode = mode;
		}

		@StringGetter( "mode" )
		public String getMode() {
			return mode;
		}

		@StringSetter( MARGINAL_UTILITY_OF_TRAVELING )
		public void setMarginalUtilityOfTraveling(double traveling) {
			testForLocked() ;
			this.traveling = traveling;
		}

		@StringGetter( MARGINAL_UTILITY_OF_TRAVELING )
		public double getMarginalUtilityOfTraveling() {
			return this.traveling;
		}

		@StringGetter( "marginalUtilityOfDistance_util_m" )
		public double getMarginalUtilityOfDistance() {
			return distance;
		}

		@StringSetter( "marginalUtilityOfDistance_util_m" )
		public void setMarginalUtilityOfDistance(double distance) {
			testForLocked() ;
			this.distance = distance;
		}

		@StringGetter( "constant" )
		public double getConstant() {
			return this.constant;
		}

		@StringSetter( "constant" )
		public void setConstant(double constant) {
			testForLocked() ;
			this.constant = constant;
		}

		@StringGetter( MONETARY_DISTANCE_RATE )
		public double getMonetaryDistanceRate() {
			return this.monetaryDistanceRate;
		}

		@StringSetter( MONETARY_DISTANCE_RATE )
		public void setMonetaryDistanceRate(double monetaryDistanceRate) {
			testForLocked() ;
			this.monetaryDistanceRate = monetaryDistanceRate;
		}

	}

	public static class ScoringParameterSet extends ReflectiveConfigGroup {
		public static final String SET_TYPE = "scoringParameters";

		private ScoringParameterSet( final String subpopulation ) {
			this();
			this.subpopulation = subpopulation;
		}

		private ScoringParameterSet() {
			super( SET_TYPE );
		}

		private String subpopulation = null;

		private double lateArrival = -18.0;
		private double earlyDeparture = -0.0;
		private double performing = +6.0;

		private double waiting = -0.0;

		private double marginalUtilityOfMoney = 1.0 ;

		private double utilityOfLineSwitch = - 1 ;

		private Double waitingPt = null ;  // if not actively set by user, it will later be set to "travelingPt".

		@StringGetter( LATE_ARRIVAL )
		public double getLateArrival_utils_hr() {
			return lateArrival;
		}

		@StringSetter( LATE_ARRIVAL )
		public void setLateArrival_utils_hr(double lateArrival) {
			testForLocked() ;
			this.lateArrival = lateArrival;
		}

		@StringGetter( EARLY_DEPARTURE )
		public double getEarlyDeparture_utils_hr() {
			return earlyDeparture;
		}

		@StringSetter( EARLY_DEPARTURE )
		public void setEarlyDeparture_utils_hr(double earlyDeparture) {
			testForLocked() ;
			this.earlyDeparture = earlyDeparture;
		}

		@StringGetter( PERFORMING )
		public double getPerforming_utils_hr() {
			return performing;
		}

		@StringSetter( PERFORMING )
		public void setPerforming_utils_hr(double performing) {
			this.performing = performing;
		}

		@StringGetter( MARGINAL_UTL_OF_MONEY )
		public double getMarginalUtilityOfMoney() {
			return marginalUtilityOfMoney;
		}

		@StringSetter( MARGINAL_UTL_OF_MONEY )
		public void setMarginalUtilityOfMoney(double marginalUtilityOfMoney) {
			testForLocked() ;
			this.marginalUtilityOfMoney = marginalUtilityOfMoney;
		}

		@StringGetter( UTL_OF_LINE_SWITCH )
		public double getUtilityOfLineSwitch() {
			return utilityOfLineSwitch;
		}

		@StringSetter( UTL_OF_LINE_SWITCH )
		public void setUtilityOfLineSwitch(double utilityOfLineSwitch) {
			testForLocked() ;
			this.utilityOfLineSwitch = utilityOfLineSwitch;
		}

		private static int setWaitingCnt=0 ;

		@StringGetter( WAITING )
		public double getMarginalUtlOfWaiting_utils_hr() {
			return this.waiting;
		}

		@StringSetter( WAITING )
		public void setMarginalUtlOfWaiting_utils_hr(final double waiting) {
			testForLocked() ;
			if ( (waiting != 0.) && (setWaitingCnt<1) ) {
				setWaitingCnt++ ;
				log.warn("Setting betaWaiting different from zero is discouraged.  It is probably implemented correctly, " +
						"but there is as of now no indication that it makes the results more realistic." + Gbl.ONLYONCE );
			}
			this.waiting = waiting;
		}

		@StringGetter( "subpopulation" )
		public String getSubpopulation() {
			return subpopulation;
		}

		@StringSetter( "subpopulation" )
		public void setSubpopulation(String subpopulation) {
			//TODO: handle case of default subpopulation
			if ( this.subpopulation != null ) {
				throw new IllegalStateException( "cannot change subpopulation in a scoring parameter set, as it is used for indexing." );
			}

			this.subpopulation = subpopulation;
		}

		@StringGetter( WAITING_PT )
		public double getMarginalUtlOfWaitingPt_utils_hr() {
			return waitingPt != null ? waitingPt :
				this.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling();
		}

		@StringSetter( WAITING_PT )
		public void setMarginalUtlOfWaitingPt_utils_hr( final Double waitingPt ) {
			this.waitingPt = waitingPt;
		}

		/* parameter set handling */
		@Override
		public ConfigGroup createParameterSet(final String type) {
			switch ( type ) {
				case ActivityParams.SET_TYPE:
					return new ActivityParams();
				case ModeParams.SET_TYPE:
					return new ModeParams();
				default:
					throw new IllegalArgumentException( type );
			}
		}

		@Override
		protected void checkParameterSet( final ConfigGroup module ) {
			switch ( module.getName() ) {
				case ActivityParams.SET_TYPE:
					if ( !(module instanceof ActivityParams) ) {
						throw new RuntimeException( "wrong class for "+module );
					}
					final String t = ((ActivityParams) module).getActivityType();
					if ( getActivityParams( t  ) != null ) {
						throw new IllegalStateException( "already a parameter set for activity type "+t );
					}
					break;
				case ModeParams.SET_TYPE:
					if ( !(module instanceof ModeParams) ) {
						throw new RuntimeException( "wrong class for "+module );
					}
					final String m = ((ModeParams) module).getMode();
					if ( getModes().get(m) != null ) {
						throw new IllegalStateException( "already a parameter set for mode "+m );
					}
					break;
				default:
					throw new IllegalArgumentException( module.getName() );
			}
		}

		public Collection<String> getActivityTypes() {
			return this.getActivityParamsPerType().keySet();
		}

		public Collection<ActivityParams> getActivityParams() {
				@SuppressWarnings("unchecked")
				Collection<ActivityParams> collection = (Collection<ActivityParams>) getParameterSets( ActivityParams.SET_TYPE );
				for ( ActivityParams params : collection ) {
					if ( this.isLocked() ) {
						params.setLocked();
					}
				}
				return collection ;
		}

		public Map<String, ActivityParams> getActivityParamsPerType() {
			final Map<String, ActivityParams> map = new LinkedHashMap< >();

			for ( ActivityParams pars : getActivityParams() ) {
				map.put( pars.getActivityType() , pars );
			}

			return map;
		}

		public ActivityParams getActivityParams(final String actType) {
			return this.getActivityParamsPerType().get(actType);
		}

		public ActivityParams getOrCreateActivityParams(final String actType) {
			ActivityParams params = this.getActivityParamsPerType().get(actType);

			if ( params == null ) {
				params = new ActivityParams( actType );
				addActivityParams( params );
			}

			return  params;
		}

		public Map<String, ModeParams> getModes() {
			@SuppressWarnings("unchecked")
			final Collection<ModeParams> modes = (Collection<ModeParams>) getParameterSets( ModeParams.SET_TYPE );
			final Map<String, ModeParams> map = new LinkedHashMap< >();

			for ( ModeParams pars : modes ) {
				if ( this.isLocked() ) {
					pars.setLocked();
				}
				map.put( pars.getMode() , pars );
			}

			return map;
		}

		public ModeParams getOrCreateModeParams(String modeName) {
			ModeParams modeParams = getModes().get(modeName);
			if (modeParams == null) {
				modeParams = new ModeParams( modeName );
				addParameterSet(modeParams);
			}
			return modeParams;
		}

		public void addModeParams(final ModeParams params) {
			final ModeParams previous = this.getModes().get( params.getMode() );

			if ( previous != null ) {
				log.info("mode parameters for mode " + previous.getMode() + " were just overwritten.") ;

				final boolean removed = removeParameterSet( previous );
				if ( !removed ) throw new RuntimeException( "problem replacing mode params " );
			}

			super.addParameterSet( params );
		}

		public void addActivityParams(final ActivityParams params) {
			final ActivityParams previous = this.getActivityParams( params.getActivityType() );

			if ( previous != null ) {
				if ( previous.getActivityType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
					log.error("ERROR: Activity parameters for activity type " + previous.getActivityType() + " were just overwritten. This happens most " +
							"likely because you defined them in the config file and the Controler overwrites them.  Or the other way " +
							"round.  pt interaction has problems, but doing what you are doing here will just cause " +
							"other (less visible) problem. Please take the effort to discuss with the core team " +
							"what needs to be done.  kai, nov'12") ;
				} else {
					log.info("activity parameters for activity type " + previous.getActivityType() + " were just overwritten.") ;
				}

				final boolean removed = removeParameterSet( previous );
				if ( !removed ) throw new RuntimeException( "problem replacing activity params " );
			}

			super.addParameterSet( params );
		}

		/** Checks whether all the settings make sense or if there are some problems with the parameters
		 * currently set. Currently, this checks that for at least one activity type opening AND closing
		 * times are defined. */
		@Override
		public void checkConsistency() {
			super.checkConsistency();
			boolean hasOpeningAndClosingTime = false;
			boolean hasOpeningTimeAndLatePenalty = false ;

			// This cannot be done in ActivityParams (where it would make more sense),
			// because some global properties are also checked
			for ( ActivityParams actType : this.getActivityParams() ) {
				if ( actType.isScoringThisActivityAtAll() ) {
					// (checking consistency only if activity is scored at all)

					if ((actType.getOpeningTime() != Time.UNDEFINED_TIME) && (actType.getClosingTime() != Time.UNDEFINED_TIME)) {
						hasOpeningAndClosingTime = true;
					}
					if ((actType.getOpeningTime() != Time.UNDEFINED_TIME) && (getLateArrival_utils_hr() < -0.001)) {
						hasOpeningTimeAndLatePenalty = true;
					}
					if ( actType.getOpeningTime()==0. && actType.getClosingTime()>24.*3600-1 ) {
						log.error("it looks like you have an activity type with opening time set to 0:00 and closing " +
								"time set to 24:00. This is most probably not the same as not setting them at all.  " +
								"In particular, activities which extend past midnight may not accumulate scores.") ;
					}
				}
			}
			if (!hasOpeningAndClosingTime && !hasOpeningTimeAndLatePenalty) {
				log.info("NO OPENING OR CLOSING TIMES DEFINED!\n\n\n"
						+"There is no activity type that has an opening *and* closing time (or opening time and late penalty) defined.\n"
						+"This usually means that the activity chains can be shifted by an arbitrary\n"
						+"number of hours without having an effect on the score of the plans, and thus\n"
						+"resulting in wrong results / traffic patterns.\n"
						+"If you are using MATSim without time adaptation, you can ignore this warning.\n\n\n");
			}
			if ( this.getMarginalUtlOfWaiting_utils_hr() != 0.0 ) {
				log.warn( "marginal utl of wait set to: " + this.getMarginalUtlOfWaiting_utils_hr() + ". Setting this different from zero is " +
						"discouraged. The parameter was also abused for pt routing; if you did that, consider setting the new " +
						"parameter waitingPt instead.");
			}
		}

	}

	private static class ReflectiveDelegate extends ReflectiveConfigGroup {
		private ReflectiveDelegate() {
			super( PlanCalcScoreConfigGroup.GROUP_NAME );
		}

		private double learningRate = 1.0;
		private double brainExpBeta = 1.0;
		private double pathSizeLogitBeta = 1.0;

		private boolean writeExperiencedPlans = false;

		private Double fractionOfIterationsToStartScoreMSA = null ;

		private boolean usingOldScoringBelowZeroUtilityDuration = false;

		@StringGetter(FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA)
		public Double getFractionOfIterationsToStartScoreMSA() {
			return fractionOfIterationsToStartScoreMSA;
		}
		@StringSetter(FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA)
		public void setFractionOfIterationsToStartScoreMSA(Double fractionOfIterationsToStartScoreMSA) {
			testForLocked() ;
			this.fractionOfIterationsToStartScoreMSA = fractionOfIterationsToStartScoreMSA;
		}

		@StringGetter( LEARNING_RATE )
		public double getLearningRate() {
			return learningRate;
		}
		@StringSetter( LEARNING_RATE )
		public void setLearningRate(double learningRate) {
			testForLocked() ;
			this.learningRate = learningRate;
		}

		@StringGetter( BRAIN_EXP_BETA )
		public double getBrainExpBeta() {
			return brainExpBeta;
		}

		@StringSetter( BRAIN_EXP_BETA )
		public void setBrainExpBeta(double brainExpBeta) {
			testForLocked() ;
			this.brainExpBeta = brainExpBeta;
		}

		@StringGetter( PATH_SIZE_LOGIT_BETA )
		public double getPathSizeLogitBeta() {
			return pathSizeLogitBeta;
		}

		@StringSetter( PATH_SIZE_LOGIT_BETA )
		public void setPathSizeLogitBeta(double beta) {
			testForLocked() ;
			if ( beta != 0. ) {
				log.warn("Setting pathSizeLogitBeta different from zero is experimental.  KN, Sep'08") ;
			}
			this.pathSizeLogitBeta = beta;
		}


		@StringGetter( USING_OLD_SCORING_BELOW_ZERO_UTILITY_DURATION )
		public boolean isUsingOldScoringBelowZeroUtilityDuration() {
			return usingOldScoringBelowZeroUtilityDuration;
		}

		@StringSetter( USING_OLD_SCORING_BELOW_ZERO_UTILITY_DURATION )
		public void setUsingOldScoringBelowZeroUtilityDuration(
				boolean usingOldScoringBelowZeroUtilityDuration) {
			testForLocked() ;
			this.usingOldScoringBelowZeroUtilityDuration = usingOldScoringBelowZeroUtilityDuration;
		}

		@StringGetter( WRITE_EXPERIENCED_PLANS )
		public boolean isWriteExperiencedPlans() {
			return writeExperiencedPlans;
		}

		@StringSetter( WRITE_EXPERIENCED_PLANS )
		public void setWriteExperiencedPlans(boolean writeExperiencedPlans) {
			testForLocked() ;
			this.writeExperiencedPlans = writeExperiencedPlans;
		}

	}

}
