/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.run;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;


public class ConvertOldPlanCalcScoreConfigGroup {
    public static void main( final String[] args ) {
        final String inputConfig = args[ 0 ];
        final String outputConfig = args[ 1 ];

        final Config config = new Config();

		config.addModule( new OldToNewPlanCalcScoreConfigGroup() );
		new ConfigReader( config ).readFile( inputConfig );
		new ConfigWriter( config ).write( outputConfig );
    }
}

final class OldToNewPlanCalcScoreConfigGroup extends ConfigGroup {

	private static final Logger log = LogManager.getLogger(OldToNewPlanCalcScoreConfigGroup.class);

	public static final String GROUP_NAME = "planCalcScore";

	private static final String LEARNING_RATE = "learningRate";
	private static final String BRAIN_EXP_BETA = "BrainExpBeta";
	private static final String PATH_SIZE_LOGIT_BETA = "PathSizeLogitBeta";
	private static final String LATE_ARRIVAL = "lateArrival";
	private static final String EARLY_DEPARTURE = "earlyDeparture";
	private static final String PERFORMING = "performing";

	private static final String TRAVELING = "traveling_";
	private static final String TRAVELING_CAR = "traveling";
	private static final String TRAVELING_PT = "travelingPt";
	private static final String TRAVELING_BIKE = "travelingBike";
	private static final String TRAVELING_WALK = "travelingWalk";
	private static final String TRAVELING_OTHER = "travelingOther";
	private static final String WAITING  = "waiting";
	private static final String WAITING_PT  = "waitingPt";

	private static final String CONSTANT = "constant_";
	private static final String CONSTANT_CAR = "constantCar" ;
	private static final String CONSTANT_BIKE = "constantBike" ;
	private static final String CONSTANT_WALK = "constantWalk" ;
	private static final String CONSTANT_OTHER = "constantOther" ;
	private static final String CONSTANT_PT = "constantPt" ;

	private static final String WRITE_EXPERIENCED_PLANS = "writeExperiencedPlans";

	private static final String MARGINAL_UTL_OF_DISTANCE_CAR = "marginalUtlOfDistanceCar";

	private static final String MARGINAL_UTL_OF_DISTANCE_PT = "marginalUtlOfDistancePt";
	private static final String MARGINAL_UTL_OF_DISTANCE = "marginalUtlOfDistance_";
	private static final String MARGINAL_UTL_OF_DISTANCE_WALK = "marginalUtlOfDistanceWalk";
	private static final String MARGINAL_UTL_OF_DISTANCE_OTHER = "marginalUtlOfDistanceOther";

	private static final String MARGINAL_UTL_OF_MONEY = "marginalUtilityOfMoney" ;

	@Deprecated
	private static final String MONETARY_DISTANCE_COST_RATE = "monetaryDistanceCostRate";

	private static final String MONETARY_DISTANCE_RATE_ = "monetaryDistanceRate_" ;
	private static final Object MONETARY_DISTANCE_RATE_CAR = "monetaryDistanceRateCar";
	private static final Object MONETARY_DISTANCE_RATE_PT = "monetaryDistanceRatePt";

	private static final String UTL_OF_LINE_SWITCH = "utilityOfLineSwitch" ;

	private static final String ACTIVITY_TYPE = "activityType_";
	private static final String ACTIVITY_PRIORITY = "activityPriority_";
	private static final String ACTIVITY_TYPICAL_DURATION = "activityTypicalDuration_";
	private static final String ACTIVITY_MINIMAL_DURATION = "activityMinimalDuration_";
	private static final String ACTIVITY_OPENING_TIME = "activityOpeningTime_";
	private static final String ACTIVITY_LATEST_START_TIME = "activityLatestStartTime_";
	private static final String ACTIVITY_EARLIEST_END_TIME = "activityEarliestEndTime_";
	private static final String ACTIVITY_CLOSING_TIME = "activityClosingTime_";

	private static final String SCORING_THIS_ACTIVITY_AT_ALL = "scoringThisActivityAtAll_" ;

	private final ReflectiveDelegate delegate = new ReflectiveDelegate();
	private final Map<String, ActivityParams> activityTypesByNumber = new HashMap< >();

	public OldToNewPlanCalcScoreConfigGroup() {
		super(GROUP_NAME);

		this.addParameterSet( new ModeParams( TransportMode.car ) );
		this.addParameterSet( new ModeParams( TransportMode.pt ) );
		this.addParameterSet( new ModeParams( TransportMode.walk ) );
		this.addParameterSet( new ModeParams( TransportMode.bike ) );
		this.addParameterSet( new ModeParams( TransportMode.other ) );
	}


	private Double waitingPt = null ;  // if not actively set by user, it will later be set to "travelingPt".

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
		// backward compatibility: underscored
		if (key.startsWith(ACTIVITY_TYPE)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_TYPE.length()), true);

			actParams.setActivityType(value);
			this.removeParameterSet( actParams );
			addActivityParams( actParams );
		}
		else if (key.startsWith(ACTIVITY_PRIORITY)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_PRIORITY.length()), true);
			actParams.setPriority(Double.parseDouble(value));
		}
		else if (key.startsWith(ACTIVITY_TYPICAL_DURATION)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_TYPICAL_DURATION.length()), true);
			actParams.setTypicalDuration(Time.parseTime(value));
		}
		else if (key.startsWith(ACTIVITY_MINIMAL_DURATION)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_MINIMAL_DURATION.length()), true);
			actParams.setMinimalDuration(Time.parseTime(value));
		}
		else if (key.startsWith(ACTIVITY_OPENING_TIME)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_OPENING_TIME.length()), true);
			actParams.setOpeningTime(Time.parseTime(value));
		}
		else if (key.startsWith(ACTIVITY_LATEST_START_TIME)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_LATEST_START_TIME.length()), true);
			actParams.setLatestStartTime(Time.parseTime(value));
		}
		else if (key.startsWith(ACTIVITY_EARLIEST_END_TIME)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_EARLIEST_END_TIME.length()), true);
			actParams.setEarliestEndTime(Time.parseTime(value));
		}
		else if (key.startsWith(ACTIVITY_CLOSING_TIME)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_CLOSING_TIME.length()), true);
			actParams.setClosingTime(Time.parseTime(value));
		}
		else if (key.startsWith(SCORING_THIS_ACTIVITY_AT_ALL)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(SCORING_THIS_ACTIVITY_AT_ALL.length()), true);
			actParams.setScoringThisActivityAtAll( Boolean.parseBoolean(value) );
		}
		else if (key.startsWith(TRAVELING)) {
			ModeParams modeParams = getOrCreateModeParams(key.substring(TRAVELING.length()));
			modeParams.setMarginalUtilityOfTraveling(Double.parseDouble(value));
		}
		else if (key.startsWith(MARGINAL_UTL_OF_DISTANCE)) {
			ModeParams modeParams = getOrCreateModeParams(key.substring(MARGINAL_UTL_OF_DISTANCE.length()));
			modeParams.setMarginalUtilityOfDistance(Double.parseDouble(value));
		}
		else if (key.startsWith(MONETARY_DISTANCE_COST_RATE)) {
			throw new RuntimeException("Please use monetaryDistanceRate (without `cost').  Even better, use config v2, "
					+ "mode-parameters (see output of any recent run), and mode-specific monetary "
					+ "distance rate.") ;
		}
		else if (key.startsWith(MONETARY_DISTANCE_RATE_)) {
			ModeParams modeParams = getOrCreateModeParams(key.substring(MONETARY_DISTANCE_RATE_.length()));
			modeParams.setMonetaryDistanceRate(Double.parseDouble(value));
		}
		else if ( MONETARY_DISTANCE_RATE_CAR.equals(key) ){
			ModeParams modeParams = getOrCreateModeParams( TransportMode.car ) ;
			modeParams.setMonetaryDistanceRate(Double.parseDouble(value));
		}
		else if ( MONETARY_DISTANCE_RATE_PT.equals(key) ){
			ModeParams modeParams = getOrCreateModeParams( TransportMode.pt ) ;
			modeParams.setMonetaryDistanceRate(Double.parseDouble(value));
		}
		else if (key.startsWith(CONSTANT)) {
			ModeParams modeParams = getOrCreateModeParams(key.substring(CONSTANT.length()));
			modeParams.setConstant(Double.parseDouble(value));
		}

		// backward compatibility: "typed" traveling
		else if (TRAVELING_CAR.equals(key)) {
			setTraveling_utils_hr(Double.parseDouble(value));
		}
		else if (TRAVELING_PT.equals(key)) {
			setTravelingPt_utils_hr(Double.parseDouble(value));
		}
		else if (TRAVELING_WALK.equals(key)) {
			setTravelingWalk_utils_hr(Double.parseDouble(value));
		}
		else if (TRAVELING_OTHER.equals(key)) {
			setTravelingOther_utils_hr(Double.parseDouble(value));
		}
		else if (TRAVELING_BIKE.equals(key)) {
			setTravelingBike_utils_hr(Double.parseDouble(value));
		}

		// backward compatibility: "typed" util of distance
		else if (MARGINAL_UTL_OF_DISTANCE_CAR.equals(key)){
			setMarginalUtlOfDistanceCar(Double.parseDouble(value));
		}
		else if (MARGINAL_UTL_OF_DISTANCE_PT.equals(key)){
			setMarginalUtlOfDistancePt(Double.parseDouble(value));
		}
		else if (MARGINAL_UTL_OF_DISTANCE_WALK.equals(key)){
			setMarginalUtlOfDistanceWalk(Double.parseDouble(value));
		}
		else if (MARGINAL_UTL_OF_DISTANCE_OTHER.equals(key)){
			setMarginalUtlOfDistanceOther(Double.parseDouble(value));
		}

		// backward compatibility: "typed" constants
		else if ( CONSTANT_CAR.equals(key)) {
			this.setConstantCar(Double.parseDouble(value)) ;
		}
		else if ( CONSTANT_WALK.equals(key)) {
			this.setConstantWalk(Double.parseDouble(value)) ;
		}
		else if ( CONSTANT_OTHER.equals(key)) {
			this.setConstantOther(Double.parseDouble(value)) ;
		}
		else if ( CONSTANT_PT.equals(key)) {
			this.setConstantPt(Double.parseDouble(value)) ;
		}
		else if ( CONSTANT_BIKE.equals(key)) {
			this.setConstantBike(Double.parseDouble(value)) ;
		}

		else if ( WAITING_PT.equals( key ) ) {
			setMarginalUtlOfWaitingPt_utils_hr( Double.parseDouble( value ) );
		}

		else {
			delegate.addParam( key , value );
		}
	}

	public ModeParams getOrCreateModeParams(String modeName) {
		ModeParams modeParams = getModes().get(modeName);
		if (modeParams == null) {
			modeParams = new ModeParams( modeName );
			addParameterSet( modeParams );
		}
		return modeParams;
	}

	@Override
	public Map<String, String> getParams() {
		final Map<String, String> params = delegate.getParams();
		if ( waitingPt != null ) {
			params.put( WAITING_PT , waitingPt.toString() );
		}
		return params;
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

	private ActivityParams getActivityTypeByNumber(final String number, final boolean createIfMissing) {
		ActivityParams actType = this.activityTypesByNumber.get(number);
		if ((actType == null) && createIfMissing) {
			// not sure what this means, but I found it so...
			// TD, sep'14
			actType = new ActivityParams(number);
			this.activityTypesByNumber.put(number, actType);
			addParameterSet( actType );
		}
		return actType;
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


	/** Checks whether all the settings make sense or if there are some problems with the parameters
	 * currently set. Currently, this checks that for at least one activity type opening AND closing
	 * times are defined. */
	@Override
	public void checkConsistency(Config config) {
		super.checkConsistency(config);
		boolean hasOpeningAndClosingTime = false;
		boolean hasOpeningTimeAndLatePenalty = false ;

		// This cannot be done in ActivityParams (where it would make more sense),
		// because some global properties are also checked
		for ( ActivityParams actType : this.getActivityParams() ) {
			if ( actType.isScoringThisActivityAtAll() ) {
				// (checking consistency only if activity is scored at all)

				if (actType.getOpeningTime().isDefined() && actType.getClosingTime().isDefined()) {
					hasOpeningAndClosingTime = true;

					if (actType.getOpeningTime().seconds() == 0. && actType.getClosingTime().seconds() > 24. * 3600 - 1) {
						log.error("it looks like you have an activity type with opening time set to 0:00 and closing "
								+ "time set to 24:00. This is most probably not the same as not setting them at all.  "
								+ "In particular, activities which extend past midnight may not accumulate scores.");
					}

				}
				if (actType.getOpeningTime().isDefined() && (getLateArrival_utils_hr() < -0.001)) {
					hasOpeningTimeAndLatePenalty = true;
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

	/* direct access */

	public double getMarginalUtlOfWaitingPt_utils_hr() {
	   return this.waitingPt == null ?
		   this.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() :
		   this.waitingPt ;
	}

	public double getTraveling_utils_hr() {
		return this.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling();
	}
	public void setTraveling_utils_hr(final double traveling) {
		this.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
	}

	public double getTravelingPt_utils_hr() {
		return this.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling();
	}
	public void setTravelingPt_utils_hr(final double travelingPt) {
		this.getModes().get(TransportMode.pt).setMarginalUtilityOfTraveling(travelingPt);
	}

	public double getTravelingBike_utils_hr() {
		return this.getModes().get(TransportMode.bike).getMarginalUtilityOfTraveling();
	}
	public void setTravelingBike_utils_hr(final double travelingBike) {
		this.getModes().get(TransportMode.bike).setMarginalUtilityOfTraveling(travelingBike);
	}

	public double getTravelingWalk_utils_hr() {
		return this.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling();
	}
	public void setTravelingWalk_utils_hr(final double travelingWalk) {
		this.getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(travelingWalk);
	}
	/**
	 * @return the marginal utility of distance for mode walk per meter
	 * <p></p>
	 * This was discouraged for some time but currently I think this makes sense. kai, mar'12
	 */
	public double getMarginalUtlOfDistanceWalk() {
		return this.getModes().get(TransportMode.walk).getMarginalUtilityOfDistance();
	}
	/**
	 * @param marginalUtlOfDistanceWalk the marginal utility of distance for mode walk per meter
	 * <p></p>
	 * This was discouraged for some time but currently I think this makes sense. kai, mar'12
	 */
	public void setMarginalUtlOfDistanceWalk(final double marginalUtlOfDistanceWalk) {
		this.getModes().get(TransportMode.walk).setMarginalUtilityOfDistance(marginalUtlOfDistanceWalk);
	}

	/**
	 * @param marginalUtlOfDistancePt the marginal utility of distance for mode pt per meter
	 */
	private void setMarginalUtlOfDistancePt(final double marginalUtlOfDistancePt) {
		this.getModes().get(TransportMode.pt).setMarginalUtilityOfDistance(marginalUtlOfDistancePt);
	}

	/**
	 * @param marginalUtlOfDistanceCar the marginal utility of distance for mode car per meter
	 */
		private void setMarginalUtlOfDistanceCar(final double marginalUtlOfDistanceCar) {
		this.getModes().get(TransportMode.car).setMarginalUtilityOfDistance(marginalUtlOfDistanceCar);
	}


	public void setMarginalUtlOfWaitingPt_utils_hr(double val) {
		this.waitingPt = val ;
	}

	public ActivityParams getActivityParams(final String actType) {
		return this.getActivityParamsPerType().get(actType);
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
			default:
				throw new IllegalArgumentException( set.getName() );
		}
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

	public static enum TypicalDurationScoreComputation { uniform, relative }

	/* complex classes */
	public static class ActivityParams extends ReflectiveConfigGroup implements MatsimParameters {
		// in normal pgm execution, code will presumably lock instance of PlanCalcScoreConfigGroup, but not instance of
		// ActivityParams.  I will try to pass the locked setting through the getters. kai, jun'15

		private static final String TYPICAL_DURATION_SCORE_COMPUTATION = "typicalDurationScoreComputation";
		final static String SET_TYPE = "activityParams";
		private String type;
		private double priority = 1.0;
		private OptionalTime typicalDuration = OptionalTime.undefined();
		private OptionalTime minimalDuration = OptionalTime.undefined();
		private OptionalTime openingTime = OptionalTime.undefined();
		private OptionalTime latestStartTime = OptionalTime.undefined();
		private OptionalTime earliestEndTime = OptionalTime.undefined();
		private OptionalTime closingTime = OptionalTime.undefined();
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

		public OptionalTime getTypicalDuration() {
			return this.typicalDuration;
		}

		@StringSetter( "typicalDuration" )
		private void setTypicalDuration(final String typicalDuration) {
			testForLocked() ;
			setTypicalDuration( Time.parseTime( typicalDuration ) );
		}

		public void setTypicalDuration(final double typicalDuration) {
			testForLocked() ;
			this.typicalDuration = OptionalTime.defined(typicalDuration);
		}

		@StringGetter( "minimalDuration" )
		private String getMinimalDurationString() {
			return Time.writeTime( getMinimalDuration() );
		}

		public OptionalTime getMinimalDuration() {
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
			this.minimalDuration = OptionalTime.defined(minimalDuration);
			if (this.minimalDuration.isDefined() && minDurCnt < 1 ) {
				minDurCnt++ ;
				log.warn("Setting minimalDuration different from zero is discouraged.  It is probably implemented correctly, " +
						"but there is as of now no indication that it makes the results more realistic.  KN, Sep'08" + Gbl.ONLYONCE );
			}
		}

		@StringGetter( "openingTime" )
		private String getOpeningTimeString() {
			return Time.writeTime( getOpeningTime() );
		}

		public OptionalTime getOpeningTime() {
			return this.openingTime;
		}
		@StringSetter( "openingTime" )
		private void setOpeningTime(final String openingTime) {
			testForLocked() ;
			setOpeningTime( Time.parseTime( openingTime ) );
		}

		public void setOpeningTime(final double openingTime) {
			testForLocked() ;
			this.openingTime = OptionalTime.defined(openingTime);
		}

		@StringGetter( "latestStartTime" )
		private String getLatestStartTimeString() {
			return Time.writeTime( getLatestStartTime() );
		}

		public OptionalTime getLatestStartTime() {
			return this.latestStartTime;
		}
		@StringSetter( "latestStartTime" )
		private void setLatestStartTime(final String latestStartTime) {
			testForLocked() ;
			setLatestStartTime( Time.parseTime( latestStartTime ) );
		}

		public void setLatestStartTime(final double latestStartTime) {
			testForLocked() ;
			this.latestStartTime = OptionalTime.defined(latestStartTime);
		}

		@StringGetter( "earliestEndTime" )
		private String getEarliestEndTimeString() {
			return Time.writeTime( getEarliestEndTime() );
		}

		public OptionalTime getEarliestEndTime() {
			return this.earliestEndTime;
		}
		@StringSetter( "earliestEndTime" )
		private void setEarliestEndTime(final String earliestEndTime) {
			testForLocked() ;
			setEarliestEndTime( Time.parseTime( earliestEndTime ) );
		}

		public void setEarliestEndTime(final double earliestEndTime) {
			testForLocked() ;
			this.earliestEndTime = OptionalTime.defined(earliestEndTime);
		}

		@StringGetter( "closingTime" )
		private String getClosingTimeString() {
			return Time.writeTime( getClosingTime() );
		}

		public OptionalTime getClosingTime() {
			return this.closingTime;
		}
		@StringSetter( "closingTime" )
		private void setClosingTime(final String closingTime) {
			testForLocked() ;
			setClosingTime( Time.parseTime( closingTime ) );
		}

		public void setClosingTime(final double closingTime) {
			testForLocked() ;
			this.closingTime = OptionalTime.defined(closingTime);
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
			map.put( "marginalUtilityOfTraveling_util_hr", "[utils/hr] additional marginal utility of traveling.  normally negative.  this comes on top " +
					"of the opportunity cost of time");
			map.put( "marginalUtilityOfDistance_util_m", "[utils/m] utility of walking per m, normally negative.  this is " +
					"on top of the time (dis)utility.") ;
			map.put("monetaryDistanceCostRate", "[unit_of_money/m] conversion of distance into money. Probably needs to be negative to work." ) ;
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

		@StringSetter( "marginalUtilityOfTraveling_util_hr" )
		public void setMarginalUtilityOfTraveling(double traveling) {
			testForLocked() ;
			this.traveling = traveling;
		}

		@StringGetter( "marginalUtilityOfTraveling_util_hr" )
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

		@StringGetter( "monetaryDistanceRate" )
		public double getMonetaryDistanceRate() {
			return this.monetaryDistanceRate;
		}

		@StringSetter( "monetaryDistanceRate" )
		public void setMonetaryDistanceRate(double monetaryDistanceRate) {
			testForLocked() ;
			this.monetaryDistanceRate = monetaryDistanceRate;
		}

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
				if ( getModes().get( m ) != null ) {
					throw new IllegalStateException( "already a parameter set for mode "+m );
				}
				break;
			default:
				throw new IllegalArgumentException( module.getName() );
		}
	}

	@Deprecated // use ModeParams (getModes().get...) directly
	public double getMonetaryDistanceRateCar() {
		return this.getModes().get(TransportMode.car).getMonetaryDistanceRate();
	}
	@Deprecated // use ModeParams (getModes().get...) directly
	public void setMonetaryDistanceRateCar(double monetaryDistanceRateCar) {
		this.getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar);
	}
	@Deprecated // use ModeParams (getModes().get...) directly
	public double getMonetaryDistanceRatePt() {
		return this.getModes().get(TransportMode.pt).getMonetaryDistanceRate();
	}

	@Deprecated // use ModeParams (getModes().get...) directly
	public void setMonetaryDistanceRatePt(double monetaryDistanceRatePt) {
		this.getModes().get(TransportMode.pt).setMonetaryDistanceRate(monetaryDistanceRatePt);
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public double getConstantCar() {
		return getModes().get(TransportMode.car).getConstant();
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public void setConstantCar(double constantCar) {
		getModes().get(TransportMode.car).setConstant(constantCar);
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public double getConstantWalk() {
		return getModes().get(TransportMode.walk).getConstant();
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public void setConstantWalk(double constantWalk) {
		getModes().get(TransportMode.walk).setConstant(constantWalk);
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public double getConstantPt() {
		return getModes().get(TransportMode.pt).getConstant();
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public void setConstantPt(double constantPt) {
		getModes().get(TransportMode.pt).setConstant(constantPt);
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public double getConstantBike() {
		return getModes().get(TransportMode.bike).getConstant();
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public void setConstantBike(double constantBike) {
		getModes().get(TransportMode.bike).setConstant(constantBike);
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public double getTravelingOther_utils_hr() {
		return getModes().get(TransportMode.other).getMarginalUtilityOfTraveling();
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public double getConstantOther() {
		return getModes().get(TransportMode.other).getConstant();
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public double getMarginalUtlOfDistanceOther() {
		return getModes().get(TransportMode.other).getMarginalUtilityOfDistance();
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public void setMarginalUtlOfDistanceOther(double marginalUtlOfDistanceOther) {
		this.getModes().get(TransportMode.other).setMarginalUtilityOfDistance(marginalUtlOfDistanceOther);
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public void setConstantOther(double constantOther) {
		getModes().get(TransportMode.other).setConstant(constantOther);
	}


	@Deprecated // use ModeParams (getModes().get...) directly
	public void setTravelingOther_utils_hr(double travelingOtherUtilsHr) {
		this.getModes().get(TransportMode.other).setMarginalUtilityOfTraveling(travelingOtherUtilsHr);
	}

	public boolean isMemorizingExperiencedPlans() {
		return this.memorizingExperiencedPlans ;
	}

	public void setMemorizingExperiencedPlans(boolean memorizingExperiencedPlans) {
		this.memorizingExperiencedPlans = memorizingExperiencedPlans;
	}

	private static class ReflectiveDelegate extends ReflectiveConfigGroup {
		private ReflectiveDelegate() {
			super( OldToNewPlanCalcScoreConfigGroup.GROUP_NAME );
		}

		private double learningRate = 1.0;
		private double brainExpBeta = 1.0;
		private double pathSizeLogitBeta = 1.0;
		private double lateArrival = -18.0;
		private double earlyDeparture = -0.0;
		private double performing = +6.0;

		private double waiting = -0.0;


		private double marginalUtilityOfMoney = 1.0 ;

		private double utilityOfLineSwitch = - 1 ;

		private boolean usingOldScoringBelowZeroUtilityDuration = false ;

		private boolean writeExperiencedPlans = false;

		private Double fractionOfIterationsToStartScoreMSA = null ;

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
		return delegate.getLateArrival_utils_hr();
	}

	public void setLateArrival_utils_hr(double lateArrival) {
		delegate.setLateArrival_utils_hr(lateArrival);
	}

	public double getEarlyDeparture_utils_hr() {
		return delegate.getEarlyDeparture_utils_hr();
	}

	public void setEarlyDeparture_utils_hr(double earlyDeparture) {
		delegate.setEarlyDeparture_utils_hr(earlyDeparture);
	}

	public double getPerforming_utils_hr() {
		return delegate.getPerforming_utils_hr();
	}

	public void setPerforming_utils_hr(double performing) {
		delegate.setPerforming_utils_hr(performing);
	}

	public double getMarginalUtilityOfMoney() {
		return delegate.getMarginalUtilityOfMoney();
	}

	public void setMarginalUtilityOfMoney(double marginalUtilityOfMoney) {
		delegate.setMarginalUtilityOfMoney(marginalUtilityOfMoney);
	}

	public double getUtilityOfLineSwitch() {
		return delegate.getUtilityOfLineSwitch();
	}

	public void setUtilityOfLineSwitch(double utilityOfLineSwitch) {
		delegate.setUtilityOfLineSwitch(utilityOfLineSwitch);
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
		return delegate.getMarginalUtlOfWaiting_utils_hr();
	}
	public void setMarginalUtlOfWaiting_utils_hr(double waiting) {
		delegate.setMarginalUtlOfWaiting_utils_hr(waiting);
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
}
