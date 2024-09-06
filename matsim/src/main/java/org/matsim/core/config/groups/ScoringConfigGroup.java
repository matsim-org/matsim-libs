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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

/**
 * Design decisions:
 * <ul>
 * <li>I have decided to modify those setters/getters that do not use SI units
 * such that the units are attached. This means all the utility parameters which
 * are "per hour" instead of "per second". kai, dec'10
 * <li>Note that a similar thing is not necessary for money units since money
 * units do not need to be specified (they are always implicit). kai, dec'10
 * <li>The parameter names in the config file are <i>not</i> changed in this way
 * since this would mean a public api change. kai, dec'10
 * </ul>
 *
 * @author nagel
 *
 */
public final class ScoringConfigGroup extends ConfigGroup {

	private static final Logger log = LogManager.getLogger(ScoringConfigGroup.class);

	public static final String GROUP_NAME = "scoring";

	private static final String LEARNING_RATE = "learningRate";
	private static final String BRAIN_EXP_BETA = "brainExpBeta";
	private static final String PATH_SIZE_LOGIT_BETA = "pathSizeLogitBeta";
	private static final String LATE_ARRIVAL = "lateArrival";
	private static final String EARLY_DEPARTURE = "earlyDeparture";
	private static final String PERFORMING = "performing";

	private static final String WAITING = "waiting";
	private static final String WAITING_PT = "waitingPt";

	private static final String WRITE_EXPERIENCED_PLANS = "writeExperiencedPlans";

	private static final String MARGINAL_UTL_OF_MONEY = "marginalUtilityOfMoney";

	private static final String UTL_OF_LINE_SWITCH = "utilityOfLineSwitch";

	private static final String WRITE_SCORE_EXPLANATIONS = "writeScoreExplanations";

	private final ReflectiveDelegate delegate = new ReflectiveDelegate();

	private boolean usesDeprecatedSyntax = false ;

	public ScoringConfigGroup() {
		super(GROUP_NAME);

		this.addScoringParameters(new ScoringParameterSet());

		// what follows now has weird consequences:
		// * the material is added to the ScoringParameterSet of the default subpopulation
		// * if someone uses the following in the config.xml:
		//      < ... planCalcScore ... >
		//            <... modeParams ... >
		//                   < ... mode ... abc ... />
		//    then abc will be _added_ to the modes info below (same for activities)
		//  * if, however, someone uses in the config.xml:
		//      < ... planCalcScore ... >
		//            < ... scoringParameters ... >
		//                  <... modeParams ... >
		//                        < ... mode ... abc ... />
		//     (= fully hierarchical format), then the default modes will be removed before adding mode abc.  The reason for this is that the second
		//     syntax clears the scoring params for the default subpopulation.

		//  Unfortunately, it continues:
		//  * Normally, we need a "clear defaults with first configured entry" (see PlansCalcRouteConfigGroup).  Otherwise, we fail the write-read
		//  test: Assume we end up with a config that has _less_ material than the defaults.  Then we write this to file, and read it back in.  If
		//  the defaults are not cleared, they would now be fully there.
		//  * The reason why this works here is that all the material is written out with the fully hierarchical format.  I.e. it actually clears the
		//  defaults when being read it.

		// I am not sure if it can stay the way it is right now; took me several hours to understand it (and fix a problem we had not by
		// trial-and-error but by understanding the root cause).  Considerations:
		// * Easiest would be to not have defaults.  However, defaults are helpful in particular to avoid that everybody uses different parameters.
		// * We could also have the "manual addition triggers clearing" logic.  In PlansCalcRouteConfigGroup I now have this with a warning, which
		// can be switched off with a switch.  I find this a good solution; I am, however, not 100% certain that it is robust since that switch is a
		// "state" while "clearing the defaults" is an action, and I am not sure if they can be mapped into each other in all cases.
		// * We could, together with the previous point, disallow the not fully hierarchical format.

		// kai, dec'19

		this.addModeParams(new ModeParams(TransportMode.car));
		this.addModeParams(new ModeParams(TransportMode.pt));
		this.addModeParams(new ModeParams(TransportMode.walk));
		this.addModeParams(new ModeParams(TransportMode.bike));
		this.addModeParams(new ModeParams(TransportMode.ride));
		this.addModeParams(new ModeParams(TransportMode.other));

		this.addActivityParams( new ActivityParams("dummy").setTypicalDuration(2. * 3600. ) );
		// (this is there so that an empty config prints out at least one activity type, so that the explanations of this
		// important concept show up e.g. in defaultConfig.xml, created from the GUI. kai, jul'17
//			params.setScoringThisActivityAtAll(false); // no longer minimal when included here. kai, jun'18

		// yyyyyy find better solution for this. kai, dec'15
		// Probably no longer needed; see checkConsistency method.  kai, jan'21
		this.addActivityParams( new ActivityParams(createStageActivityType( TransportMode.car ) ).setScoringThisActivityAtAll(false ) );
		this.addActivityParams( new ActivityParams(createStageActivityType( TransportMode.pt )).setScoringThisActivityAtAll(false ) );
		// (need this for self-programmed pseudo pt. kai, nov'16)
		this.addActivityParams( new ActivityParams(createStageActivityType( TransportMode.bike ) ).setScoringThisActivityAtAll(false ) );
		this.addActivityParams( new ActivityParams(createStageActivityType( TransportMode.drt ) ).setScoringThisActivityAtAll(false ) );
		this.addActivityParams( new ActivityParams(createStageActivityType( TransportMode.taxi ) ).setScoringThisActivityAtAll(false ) );
		this.addActivityParams( new ActivityParams(createStageActivityType( TransportMode.other ) ).setScoringThisActivityAtAll(false ) );
		this.addActivityParams( new ActivityParams(createStageActivityType( TransportMode.walk ) ).setScoringThisActivityAtAll(false ) );
		// (bushwhacking_walk---network_walk---bushwhacking_walk)
	}

	public static ActivityParams createStageActivityParams( String mode ) {
		return new ActivityParams( createStageActivityType( mode ) ).setScoringThisActivityAtAll( false );
	}

	// ---

	private static final String USING_OLD_SCORING_BELOW_ZERO_UTILITY_DURATION = "usingOldScoringBelowZeroUtilityDuration";

	/**
	 * can't set this from outside java since for the time being it is not
	 * useful there. kai, dec'13
	 */
	private boolean memorizingExperiencedPlans = false;

	/**
	 * This is the key for customizable. where should this go?
	 */
	public static final String EXPERIENCED_PLAN_KEY = "experiencedPlan";
	public static final String DEFAULT_SUBPOPULATION = "default";

	// ---
	private static final String FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA = "fractionOfIterationsToStartScoreMSA";

	public static String createStageActivityType( String mode ){
		return mode + " interaction";
	}

	// ---

	@Override
	public String getValue(final String key) {
		throw new IllegalArgumentException(key + ": getValue access disabled; use direct getter");
	}

	private static final String msg = " is deprecated config syntax; please use the more " +
								    "modern hierarchical format; your output_config.xml " +
								    "will be in the correct version; the old version will fail eventually, since we want to reduce the " +
								    "workload on this backwards compatibility (look into " +
								    "PlanCalcScoreConfigGroup or PlanCalcRouteConfigGroup if you want to know what we mean).";

	@Override
	public void addParam(final String key, final String value) {
		testForLocked();
		if (key.startsWith("monetaryDistanceCostRate")) {
			throw new RuntimeException("Please use monetaryDistanceRate (without `cost').  Even better, use config v2, "
					+ "mode-parameters (see output of any recent run), and mode-specific monetary " + "distance rate.");
		} else if (WAITING_PT.equals(key)) {
			setMarginalUtlOfWaitingPt_utils_hr(Double.parseDouble(value));
		}

		// backward compatibility: underscored
		else if (key.startsWith("activityType_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;

			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityType_".length()));

			actParams.setActivityType(value);
			getScoringParameters(null).removeParameterSet(actParams);
			addActivityParams(actParams);
		} else if (key.startsWith("activityPriority_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityPriority_".length()));
			actParams.setPriority(Double.parseDouble(value));
		} else if (key.startsWith("activityTypicalDuration_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityTypicalDuration_".length()));
			actParams.typicalDuration = Time.parseOptionalTime(value);
		} else if (key.startsWith("activityMinimalDuration_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityMinimalDuration_".length()));
			actParams.minimalDuration = Time.parseOptionalTime(value);
		} else if (key.startsWith("activityOpeningTime_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityOpeningTime_".length()));
			actParams.openingTime=Time.parseOptionalTime(value);
		} else if (key.startsWith("activityLatestStartTime_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityLatestStartTime_".length()));
			actParams.latestStartTime = Time.parseOptionalTime(value);
		} else if (key.startsWith("activityEarliestEndTime_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityEarliestEndTime_".length()));
			actParams.earliestEndTime = Time.parseOptionalTime(value);
		} else if (key.startsWith("activityClosingTime_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ActivityParams actParams = getActivityTypeByNumber(key.substring("activityClosingTime_".length()));
			actParams.closingTime = Time.parseOptionalTime(value);
		} else if (key.startsWith("scoringThisActivityAtAll_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ActivityParams actParams = getActivityTypeByNumber(key.substring("scoringThisActivityAtAll_".length()));
			actParams.setScoringThisActivityAtAll(Boolean.parseBoolean(value));
		} else if (key.startsWith("traveling_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ModeParams modeParams = getOrCreateModeParams(key.substring("traveling_".length()));
			modeParams.setMarginalUtilityOfTraveling(Double.parseDouble(value));
		} else if (key.startsWith("marginalUtlOfDistance_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ModeParams modeParams = getOrCreateModeParams(key.substring("marginalUtlOfDistance_".length()));
			modeParams.setMarginalUtilityOfDistance(Double.parseDouble(value));
		} else if (key.startsWith("monetaryDistanceRate_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ModeParams modeParams = getOrCreateModeParams(key.substring("monetaryDistanceRate_".length()));
			modeParams.setMonetaryDistanceRate(Double.parseDouble(value));
		} else if ("monetaryDistanceRateCar".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ModeParams modeParams = getOrCreateModeParams(TransportMode.car);
			modeParams.setMonetaryDistanceRate(Double.parseDouble(value));
		} else if ("monetaryDistanceRatePt".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ModeParams modeParams = getOrCreateModeParams(TransportMode.pt);
			modeParams.setMonetaryDistanceRate(Double.parseDouble(value));
		} else if (key.startsWith("constant_")) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			ModeParams modeParams = getOrCreateModeParams(key.substring("constant_".length()));
			modeParams.setConstant(Double.parseDouble(value));
		}

		// backward compatibility: "typed" traveling
		else if ("traveling".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			this.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(Double.parseDouble(value));
		} else if ("travelingPt".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			this.getModes().get(TransportMode.pt).setMarginalUtilityOfTraveling(Double.parseDouble(value));
		} else if ("travelingWalk".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			this.getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(Double.parseDouble(value));
		} else if ("travelingOther".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			this.getModes().get(TransportMode.other).setMarginalUtilityOfTraveling(Double.parseDouble(value));
		} else if ("travelingBike".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			this.getModes().get(TransportMode.bike).setMarginalUtilityOfTraveling(Double.parseDouble(value));
		}

		// backward compatibility: "typed" util of distance
		else if ("marginalUtlOfDistanceCar".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			this.getModes().get(TransportMode.car).setMarginalUtilityOfDistance(Double.parseDouble(value));
		} else if ("marginalUtlOfDistancePt".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			this.getModes().get(TransportMode.pt).setMarginalUtilityOfDistance(Double.parseDouble(value));
		} else if ("marginalUtlOfDistanceWalk".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			this.getModes().get(TransportMode.walk).setMarginalUtilityOfDistance(Double.parseDouble(value));
		} else if ("marginalUtlOfDistanceOther".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			this.getModes().get(TransportMode.other).setMarginalUtilityOfDistance(Double.parseDouble(value));
		}

		// backward compatibility: "typed" constants
		else if ("constantCar".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			getModes().get(TransportMode.car).setConstant(Double.parseDouble(value));
		} else if ("constantWalk".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			getModes().get(TransportMode.walk).setConstant(Double.parseDouble(value));
		} else if ("constantOther".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			getModes().get(TransportMode.other).setConstant(Double.parseDouble(value));
		} else if ("constantPt".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			getModes().get(TransportMode.pt).setConstant(Double.parseDouble(value));
		} else if ("constantBike".equals(key)) {
			log.warn( key + msg );
			usesDeprecatedSyntax = true ;
			getModes().get(TransportMode.bike).setConstant(Double.parseDouble(value));
		}

		// old-fashioned scoring parameters: default subpopulation
		else if (Arrays
				.asList(LATE_ARRIVAL, EARLY_DEPARTURE, PERFORMING, MARGINAL_UTL_OF_MONEY, UTL_OF_LINE_SWITCH, WAITING)
				.contains(key)) {
//			log.warn( key + msg );
//			usesDeprecatedSyntax = true ;
			// this is the stuff with the default subpopulation

			getScoringParameters(null).addParam(key, value);
		}

		else {
			delegate.addParam(key, value);
		}
	}

	/* for the backward compatibility nonsense */
	private final Map<String, ActivityParams> activityTypesByNumber = new HashMap<>();

	private ActivityParams getActivityTypeByNumber(final String number) {
		ActivityParams actType = this.activityTypesByNumber.get(number);
		if ((actType == null)) {
			// not sure what this means, but I found it so...
			// TD, sep'14
			actType = new ActivityParams(number);
			this.activityTypesByNumber.put(number, actType);
			addParameterSet(actType);
		}
		return actType;
	}

	public ModeParams getOrCreateModeParams(String modeName) {
		return getScoringParameters(null).getOrCreateModeParams(modeName);
	}

	@Override
	public Map<String, String> getParams() {
		return delegate.getParams();
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA,
				"fraction of iterations at which MSA score averaging is started. The matsim theory department "
						+ "suggests to use this together with switching off choice set innovation (where a similar switch exists), but it has not been tested yet.");
		map.put(USING_OLD_SCORING_BELOW_ZERO_UTILITY_DURATION,
				"There used to be a plateau between duration=0 and duration=zeroUtilityDuration. "
						+ "This caused durations to evolve to zero once they were below zeroUtilityDuration, causing problems.  Only use this switch if you need to be "
						+ "backwards compatible with some old results.  (changed nov'13)");
		map.put(PERFORMING,
				"[utils/hr] marginal utility of doing an activity.  normally positive.  also the opportunity cost of "
						+ "time if agent is doing nothing.  MATSim separates the resource value of time from the direct (dis)utility of travel time, see, e.g., "
						+ "Boerjesson and Eliasson, TR-A 59 (2014) 144-158.");
		map.put(LATE_ARRIVAL,
				"[utils/hr] utility for arriving late (i.e. after the latest start time).  normally negative");
		map.put(EARLY_DEPARTURE,
				"[utils/hr] utility for departing early (i.e. before the earliest end time).  Normally negative.  Probably "
						+ "implemented correctly, but not tested.");
		map.put(WAITING,
				"[utils/hr] additional marginal utility for waiting. normally negative. this comes on top of the opportunity cost of time.  Probably "
						+ "implemented correctly, but not tested.");
		map.put(WAITING_PT,
				"[utils/hr] additional marginal utility for waiting for a pt vehicle. normally negative. this comes on top of the opportunity cost "
						+ "of time. Default: if not set explicitly, it is equal to traveling_pt!!!");
		map.put(BRAIN_EXP_BETA,
				"logit model scale parameter. default: 1.  Has name and default value for historical reasons "
						+ "(see Bryan Raney's phd thesis).");
		map.put(LEARNING_RATE,
				"new_score = (1-learningRate)*old_score + learningRate * score_from_mobsim.  learning rates "
						+ "close to zero emulate score averaging, but slow down initial convergence");
		map.put(UTL_OF_LINE_SWITCH, "[utils] utility of switching a line (= transfer penalty).  Normally negative");
		map.put(MARGINAL_UTL_OF_MONEY,
				"[utils/unit_of_money] conversion of money (e.g. toll, distance cost) into utils. Normall positive (i.e. toll/cost/fare are processed as negative amounts of money).");
		map.put(WRITE_EXPERIENCED_PLANS,
				"write a plans file in each iteration directory which contains what each agent actually did, and the score it received.");

		map.put(WRITE_SCORE_EXPLANATIONS,
				 "Write detailed score composition into plan attributes after execution.");

		return map;
	}

	/*
	 *
	 * @returns a list of all Activities over all Subpopulations (if existent)
	 */
	public Collection<String> getActivityTypes() {
		if (getScoringParameters(null) != null)
			return getScoringParameters(null).getActivityParamsPerType().keySet();
		else{
			Set<String> activities = new HashSet<>();
			getScoringParametersPerSubpopulation().values().forEach(item -> activities.addAll(item.getActivityParamsPerType().keySet()));
			return activities;
	}
	}

	/*
	 *
	 * @returns a list of all Modes over all Subpopulations (if existent)
	 */
	public Collection<String> getAllModes() {
		if (getScoringParameters(null) != null) {
			return getScoringParameters(null).getModes().keySet();

		} else {
			Set<String> modes = new HashSet<>();
			getScoringParametersPerSubpopulation().values().forEach(item -> modes.addAll(item.getModes().keySet()));
			return modes;
		}

	}

	public Collection<ActivityParams> getActivityParams() {
		if (getScoringParameters(null) != null)
			return getScoringParameters(null).getActivityParams();
		else if (getScoringParameters(DEFAULT_SUBPOPULATION) != null)
			return getScoringParameters(DEFAULT_SUBPOPULATION).getActivityParams();
		else
			throw new RuntimeException("Default subpopulation is not defined");
	}

	public Map<String, ModeParams> getModes() {
		if (getScoringParameters(null) != null)
			return getScoringParameters(null).getModes();
		else if (getScoringParameters(DEFAULT_SUBPOPULATION) != null)
			return getScoringParameters(DEFAULT_SUBPOPULATION).getModes();
		else
			throw new RuntimeException("Default subpopulation is not defined");
	}



	public Map<String, ScoringParameterSet> getScoringParametersPerSubpopulation() {
		@SuppressWarnings("unchecked")
		final Collection<ScoringParameterSet> parameters = (Collection<ScoringParameterSet>) getParameterSets(
				ScoringParameterSet.SET_TYPE);
		final Map<String, ScoringParameterSet> map = new LinkedHashMap<>();

		for (ScoringParameterSet pars : parameters) {
			if (this.isLocked()) {
				pars.setLocked();
			}
			map.put(pars.getSubpopulation(), pars);
		}

		return map;
	}

	/* direct access */

	public double getMarginalUtlOfWaitingPt_utils_hr() {
		if (getScoringParameters(null) != null)
			return getScoringParameters(null).getMarginalUtlOfWaitingPt_utils_hr();
		else if (getScoringParameters(DEFAULT_SUBPOPULATION) != null)
			return getScoringParameters(DEFAULT_SUBPOPULATION).getMarginalUtlOfWaitingPt_utils_hr();
		else
			throw new RuntimeException("Default subpopulation is not defined");

	}

	public void setMarginalUtlOfWaitingPt_utils_hr(double val) {
		getScoringParameters(null).setMarginalUtlOfWaitingPt_utils_hr(val);
	}

	public ActivityParams getActivityParams(final String actType) {
		if (getScoringParameters(null) != null)
			return getScoringParameters(null).getActivityParams(actType);
		else if (getScoringParameters(DEFAULT_SUBPOPULATION) != null)
			return getScoringParameters(DEFAULT_SUBPOPULATION).getActivityParams(actType);
		else
			throw new RuntimeException("Default subpopulation is not defined");

	}

	public ScoringParameterSet getScoringParameters(String subpopulation) {
		final ScoringParameterSet params = getScoringParametersPerSubpopulation().get(subpopulation);
		// If no config parameters defined for a specific subpopulation,
		// use the ones of the "default" subpopulation
		return params != null ? params : getScoringParametersPerSubpopulation().get(null);
	}

	public ScoringParameterSet getOrCreateScoringParameters(String subpopulation) {
		ScoringParameterSet params = getScoringParametersPerSubpopulation().get(subpopulation);

		if (params == null) {
			params = new ScoringParameterSet(subpopulation);
			this.addScoringParameters(params);
		}

		return params;
	}

	@Override
	public void addParameterSet(final ConfigGroup set) {
		switch (set.getName()) {
		case ActivityParams.SET_TYPE:
			addActivityParams((ActivityParams) set);
			break;
		case ModeParams.SET_TYPE:
			addModeParams((ModeParams) set);
			break;
		case ScoringParameterSet.SET_TYPE:
			addScoringParameters((ScoringParameterSet) set);
			break;
		default:
			throw new IllegalArgumentException(set.getName());
		}
	}

	private void addScoringParameters( final ScoringParameterSet params ) {
		final ScoringParameterSet previous = this.getScoringParameters(params.getSubpopulation());

		if (previous != null) {
			log.info("scoring parameters for subpopulation " + previous.getSubpopulation() + " were just replaced.");

			final boolean removed = removeParameterSet(previous);
			if (!removed)
				throw new RuntimeException("problem replacing scoring params ");
		}

		super.addParameterSet(params);
	}

	public void addModeParams(final ModeParams params) {
		getScoringParameters(null).addModeParams(params);
	}

	public void addActivityParams(final ActivityParams params) {
		getScoringParameters(null).addActivityParams(params);
	}

	public enum TypicalDurationScoreComputation {
		uniform, relative
	}

	/* parameter set handling */
	@Override
	public ConfigGroup createParameterSet(final String type) {
		switch (type) {
		case ActivityParams.SET_TYPE:
			return new ActivityParams();
		case ModeParams.SET_TYPE:
			return new ModeParams();
		case ScoringParameterSet.SET_TYPE:
			return new ScoringParameterSet();
		default:
			throw new IllegalArgumentException(type);
		}
	}

	@Override
	protected void checkParameterSet(final ConfigGroup module) {
		switch (module.getName()) {
		case ScoringParameterSet.SET_TYPE:
			if (!(module instanceof ScoringParameterSet)) {
				throw new RuntimeException("wrong class for " + module);
			}
			final String s = ((ScoringParameterSet) module).getSubpopulation();
			if (getScoringParameters(s) != null) {
				throw new IllegalStateException("already a parameter set for subpopulation " + s);
			}
			break;
		default:
			throw new IllegalArgumentException(module.getName());
		}
	}

	@Override
	protected final void checkConsistency(final Config config) {
		super.checkConsistency(config);

		if ( usesDeprecatedSyntax && !config.global().isInsistingOnDeprecatedConfigVersion() ) {
			throw new RuntimeException( msg ) ;
		}

		if (getScoringParametersPerSubpopulation().size()>1){
			if (!getScoringParametersPerSubpopulation().containsKey(ScoringConfigGroup.DEFAULT_SUBPOPULATION)){
				throw new RuntimeException("Using several subpopulations in "+ ScoringConfigGroup.GROUP_NAME+" requires defining a \""+ ScoringConfigGroup.DEFAULT_SUBPOPULATION+" \" subpopulation."
						+ " Otherwise, crashes can be expected.");
			}
		}
//		if (!config.plansCalcRoute().getAccessEgressType().equals(PlansCalcRouteConfigGroup.AccessEgressType.none)) {

		// there are modes such as pt or drt that need interaction params even without accessEgress switched on.  The policy so far was that
		// they had to define them by themselves.  For drt, this needs to be done manually (adjustDrtConfigGroup) since it is not a core
		// contrib.  At least from a user perspective, it will be easier if they are all generated here.  Result of current variant is that they are now also generated
		// for situations where accessEgress routing is not switched on.  Since, in general, our data model assumes that there is always
		// access/egress, I think that this is acceptable.  kai, jan'21

			// adding the interaction activities that result from access/egress routing. this is strictly speaking not a consistency
			// check, but I don't know a better place where to add this. kai, jan'18

			for (ScoringParameterSet scoringParameterSet : this.getScoringParametersPerSubpopulation().values()) {

				for (String mode : config.routing().getNetworkModes()) {
					createAndAddInteractionActivity( scoringParameterSet, mode );
				}
				// (In principle, the for loop following next should be sufficient, i.e. taking the necessary modes from scoring.
				// There is, however, a test that checks if all network modes from planCalcRoute have
				// interaction activities.  So we rather satisfy it than changing the test.  kai, jan'21

				for( String mode : scoringParameterSet.getModes().keySet() ){
					createAndAddInteractionActivity( scoringParameterSet, mode );
				}
			}
//		}

		for (ActivityParams params : this.getActivityParams()) {
			if (params.isScoringThisActivityAtAll() && params.getTypicalDuration().isUndefined()) {
				throw new RuntimeException("In activity type=" + params.getActivityType()
						+ ", the typical duration is undefined.  This will lead to errors that are difficult to debug, "
						+ "so rather aborting here.");
			}
		}

	}
	private static void createAndAddInteractionActivity( ScoringParameterSet scoringParameterSet, String mode ){
		String interactionActivityType = createStageActivityType( mode );
		ActivityParams set = scoringParameterSet.getActivityParamsPerType().get( interactionActivityType );
		if( set == null ){
//						 (we do not want to overwrite this if the use has already set it with other params!)
			scoringParameterSet.addActivityParams( createStageActivityParams( mode ) );
		}
	}

	public boolean isMemorizingExperiencedPlans() {
		return this.memorizingExperiencedPlans;
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

	public void setExplainScores(boolean value) {
		delegate.setWriteScoreExplanations(value);
	}

	public boolean isWriteScoreExplanations() {
		return delegate.isWriteScoreExplanations();
	}

	public double getPathSizeLogitBeta() {
		return delegate.getPathSizeLogitBeta();
	}

	public void setPathSizeLogitBeta(double beta) {
		delegate.setPathSizeLogitBeta(beta);
	}

	public double getLateArrival_utils_hr() {

		if (getScoringParameters(null) != null)
			return getScoringParameters(null).getLateArrival_utils_hr();
		else if (getScoringParameters(DEFAULT_SUBPOPULATION) != null)
			return getScoringParameters(DEFAULT_SUBPOPULATION).getLateArrival_utils_hr();
		else
			throw new RuntimeException("Default subpopulation is not defined");

	}

	public void setLateArrival_utils_hr(double lateArrival) {
		getScoringParameters(null).setLateArrival_utils_hr(lateArrival);
	}

	public double getEarlyDeparture_utils_hr() {
		if (getScoringParameters(null) != null)
			return getScoringParameters(null).getEarlyDeparture_utils_hr();
		else if (getScoringParameters(DEFAULT_SUBPOPULATION) != null)
			return getScoringParameters(DEFAULT_SUBPOPULATION).getEarlyDeparture_utils_hr();
		else
			throw new RuntimeException("Default subpopulation is not defined");

	}

	public void setEarlyDeparture_utils_hr(double earlyDeparture) {
		getScoringParameters(null).setEarlyDeparture_utils_hr(earlyDeparture);
	}

	public double getPerforming_utils_hr() {
		if (getScoringParameters(null) != null)
			return getScoringParameters(null).getPerforming_utils_hr();
		else if (getScoringParameters(DEFAULT_SUBPOPULATION) != null)
			return getScoringParameters(DEFAULT_SUBPOPULATION).getPerforming_utils_hr();
		else
			throw new RuntimeException("Default subpopulation is not defined");

	}

	public void setPerforming_utils_hr(double performing) {
		getScoringParameters(null).setPerforming_utils_hr(performing);
	}

	public double getMarginalUtilityOfMoney() {
		if (getScoringParameters(null) != null)
			return getScoringParameters(null).getMarginalUtilityOfMoney();
		else if (getScoringParameters(DEFAULT_SUBPOPULATION) != null)
			return getScoringParameters(DEFAULT_SUBPOPULATION).getMarginalUtilityOfMoney();
		else
			throw new RuntimeException("Default subpopulation is not defined");

	}

	public void setMarginalUtilityOfMoney(double marginalUtilityOfMoney) {
		getScoringParameters(null).setMarginalUtilityOfMoney(marginalUtilityOfMoney);
	}

	public double getUtilityOfLineSwitch() {
		if (getScoringParameters(null) != null)
			return getScoringParameters(null).getUtilityOfLineSwitch();
		else if (getScoringParameters(DEFAULT_SUBPOPULATION) != null)
			return getScoringParameters(DEFAULT_SUBPOPULATION).getUtilityOfLineSwitch();
		else
			throw new RuntimeException("Default subpopulation is not defined");

	}

	public void setUtilityOfLineSwitch(double utilityOfLineSwitch) {
		getScoringParameters(null).setUtilityOfLineSwitch(utilityOfLineSwitch);
	}

	public boolean isUsingOldScoringBelowZeroUtilityDuration() {
		return delegate.isUsingOldScoringBelowZeroUtilityDuration();
	}

	public void setUsingOldScoringBelowZeroUtilityDuration(boolean usingOldScoringBelowZeroUtilityDuration) {
		delegate.setUsingOldScoringBelowZeroUtilityDuration(usingOldScoringBelowZeroUtilityDuration);
	}

	public boolean isWriteExperiencedPlans() {
		return delegate.isWriteExperiencedPlans();
	}

	public void setWriteExperiencedPlans(boolean writeExperiencedPlans) {
		delegate.setWriteExperiencedPlans(writeExperiencedPlans);
	}

	public double getMarginalUtlOfWaiting_utils_hr() {
		if (getScoringParameters(null) != null)
			return getScoringParameters(null).getMarginalUtlOfWaiting_utils_hr();
		else if (getScoringParameters(DEFAULT_SUBPOPULATION) != null)
			return getScoringParameters(DEFAULT_SUBPOPULATION).getMarginalUtlOfWaiting_utils_hr();
		else
			throw new RuntimeException("Default subpopulation is not defined");

	}

	public void setMarginalUtlOfWaiting_utils_hr(double waiting) {
		getScoringParameters(null).setMarginalUtlOfWaiting_utils_hr(waiting);
	}

	public void setFractionOfIterationsToStartScoreMSA(Double val) {
		delegate.setFractionOfIterationsToStartScoreMSA(val);
	}

	public Double getFractionOfIterationsToStartScoreMSA() {
		return delegate.getFractionOfIterationsToStartScoreMSA();
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
		// ActivityParams. I will try to pass the locked setting through the getters. kai, jun'15

		public final static String SET_TYPE = "activityParams";

		// ---

		private static final String TYPICAL_DURATION_SCORE_COMPUTATION = "typicalDurationScoreComputation";
		private TypicalDurationScoreComputation typicalDurationScoreComputation = TypicalDurationScoreComputation.relative;

		// --- typical duration:

		public static final String TYPICAL_DURATION = "typicalDuration";
		public static final String TYPICAL_DURATION_CMT = "typical duration of activity.  needs to be defined and non-zero.  in sec.";

		/**
		 * {@value TYPICAL_DURATION_CMT}
		 */
		@StringGetter(TYPICAL_DURATION)
		private String getTypicalDurationString() {
			return Time.writeTime(getTypicalDuration());
		}

		/**
		 * {@value TYPICAL_DURATION_CMT}
		 */
		public OptionalTime getTypicalDuration() {
			return this.typicalDuration;
		}

		/**
		 * {@value TYPICAL_DURATION_CMT}
		 */
		@StringSetter(TYPICAL_DURATION)
		private ActivityParams setTypicalDuration(final String typicalDuration) {
			testForLocked();
			this.typicalDuration = Time.parseOptionalTime(typicalDuration);
			return this;
		}

		/**
		 * {@value TYPICAL_DURATION_CMT}
		 */
		public ActivityParams setTypicalDuration(final double typicalDuration) {
			testForLocked();
			this.typicalDuration = OptionalTime.defined(typicalDuration);
			return this ;
		}

		// --- activity type:

		public static final String ACTIVITY_TYPE = "activityType";
		private String type;
		public static final String ACVITITY_TYPE_CMT = "all activity types that occur in the plans file need to be defined by their own sections here";

		/**
		 * {@value -- ACVITITY_TYPE_CMT}
		 */
		@StringGetter(ACTIVITY_TYPE)
		public String getActivityType() {
			return this.type;
		}

		/**
		 * {@value -- ACVITITY_TYPE_CMT}
		 */
		@StringSetter(ACTIVITY_TYPE)
		public void setActivityType(final String type) {
			testForLocked();
			this.type = type;
		}

		// ---

		private double priority = 1.0;
		private OptionalTime typicalDuration = OptionalTime.undefined();
		private OptionalTime minimalDuration = OptionalTime.undefined();
		private OptionalTime openingTime =     OptionalTime.undefined();
		private OptionalTime latestStartTime = OptionalTime.undefined();
		private OptionalTime earliestEndTime = OptionalTime.undefined();
		private OptionalTime closingTime =     OptionalTime.undefined();

		public ActivityParams() {
			super(SET_TYPE);
		}

		public ActivityParams(final String type) {
			super(SET_TYPE);
			this.type = type;
		}

		@Override
		public Map<String, String> getComments() {
			final Map<String, String> map = super.getComments();
			// ---
			StringBuilder str = new StringBuilder();
			str.append("method to compute score at typical duration.  Options: | ");
			for (TypicalDurationScoreComputation value : TypicalDurationScoreComputation.values()) {
				str.append(value.name());
				str.append(" | ");
			}
			str.append("Use ");
			str.append(TypicalDurationScoreComputation.uniform.name());
			str.append(" for backwards compatibility (all activities same score; higher proba to drop long acts).");
			map.put(TYPICAL_DURATION_SCORE_COMPUTATION, str.toString());
			// ---
			map.put(TYPICAL_DURATION, TYPICAL_DURATION_CMT);
			// ---
			return map;
		}

		@StringGetter(TYPICAL_DURATION_SCORE_COMPUTATION)
		public TypicalDurationScoreComputation getTypicalDurationScoreComputation() {
			return this.typicalDurationScoreComputation;
		}

		@StringSetter(TYPICAL_DURATION_SCORE_COMPUTATION)
		public ActivityParams setTypicalDurationScoreComputation(TypicalDurationScoreComputation str) {
			testForLocked();
			this.typicalDurationScoreComputation = str;
			return this ;
		}

		@StringGetter("priority")
		public double getPriority() {
			return this.priority;
		}

		@StringSetter("priority")
		public ActivityParams setPriority(final double priority) {
			testForLocked();
			this.priority = priority;
			return this ;
		}

		@StringGetter("minimalDuration")
		private String getMinimalDurationString() {
			return Time.writeTime(minimalDuration);
		}

		public OptionalTime getMinimalDuration() {
			return minimalDuration;
		}

		@StringSetter("minimalDuration")
		private ActivityParams setMinimalDuration(final String minimalDuration) {
			testForLocked();
			this.minimalDuration = Time.parseOptionalTime(minimalDuration);
			return this;
		}

		private static int minDurCnt = 0;

		public ActivityParams setMinimalDuration(final double minimalDuration) {
			testForLocked();
			if (minDurCnt < 1) {
				minDurCnt++;
				log.warn(
						"Setting minimalDuration different from zero is discouraged.  It is probably implemented correctly, "
								+ "but there is as of now no indication that it makes the results more realistic.  KN, Sep'08"
								+ Gbl.ONLYONCE);
			}
			this.minimalDuration = OptionalTime.defined(minimalDuration);
			return this ;
		}

		@StringGetter("openingTime")
		private String getOpeningTimeString() {
			return Time.writeTime(this.openingTime);
		}

		public OptionalTime getOpeningTime() {
			return openingTime;
		}

		@StringSetter("openingTime")
		private ActivityParams setOpeningTime(final String openingTime) {
			testForLocked();
			this.openingTime =Time.parseOptionalTime(openingTime);
			return this ;
		}

		public ActivityParams setOpeningTime(final double openingTime) {
			testForLocked();
			this.openingTime = OptionalTime.defined(openingTime);
			return this ;
		}

		@StringGetter("latestStartTime")
		private String getLatestStartTimeString() {
			return Time.writeTime(latestStartTime);
		}

		public OptionalTime getLatestStartTime() {
			return this.latestStartTime;
		}

		@StringSetter("latestStartTime")
		private ActivityParams setLatestStartTime(final String latestStartTime) {
			testForLocked();
			this.latestStartTime = Time.parseOptionalTime(latestStartTime);
			return this ;
		}

		public ActivityParams setLatestStartTime(final double latestStartTime) {
			testForLocked();
			this.latestStartTime = OptionalTime.defined(latestStartTime);
			return this ;
		}

		@StringGetter("earliestEndTime")
		private String getEarliestEndTimeString() {
			return Time.writeTime(earliestEndTime);
		}

		public OptionalTime getEarliestEndTime() {
			return earliestEndTime;
		}

		@StringSetter("earliestEndTime")
		private ActivityParams setEarliestEndTime(final String earliestEndTime) {
			testForLocked();
			this.earliestEndTime = Time.parseOptionalTime(earliestEndTime);
			return this ;
		}

		public ActivityParams setEarliestEndTime(final double earliestEndTime) {
			testForLocked();
			this.earliestEndTime = OptionalTime.defined(earliestEndTime);
			return this ;
		}

		@StringGetter("closingTime")
		private String getClosingTimeString() {
			return Time.writeTime(closingTime);
		}

		public OptionalTime getClosingTime() {
			return closingTime;
		}

		@StringSetter("closingTime")
		private ActivityParams setClosingTime(final String closingTime) {
			testForLocked();
			this.closingTime = (Time.parseOptionalTime(closingTime));
			return this ;
		}

		public ActivityParams setClosingTime(final double closingTime) {
			testForLocked();
			this.closingTime = OptionalTime.defined(closingTime);
			return this ;
		}

		// ---

		static final String SCORING_THIS_ACTIVITY_AT_ALL = "scoringThisActivityAtAll";

		private boolean scoringThisActivityAtAll = true;

		@StringGetter(SCORING_THIS_ACTIVITY_AT_ALL)
		public boolean isScoringThisActivityAtAll() {
			return scoringThisActivityAtAll;
		}

		@StringSetter(SCORING_THIS_ACTIVITY_AT_ALL)
		public ActivityParams setScoringThisActivityAtAll(boolean scoringThisActivityAtAll) {
			testForLocked();
			this.scoringThisActivityAtAll = scoringThisActivityAtAll;
			return this ;
		}
	}

	public static class ModeParams extends ReflectiveConfigGroup implements MatsimParameters {

		final static String SET_TYPE = "modeParams";

		private static final String MONETARY_DISTANCE_RATE = "monetaryDistanceRate";
		private static final String MONETARY_DISTANCE_RATE_CMT = "[unit_of_money/m] conversion of distance into money. Normally negative.";

		private static final String MARGINAL_UTILITY_OF_TRAVELING = "marginalUtilityOfTraveling_util_hr";

		private static final String CONSTANT = "constant";
		private static final String CONSTANT_CMT = "[utils] alternative-specific constant.  Normally per trip, but that is probably buggy for multi-leg trips.";

		public static final String MODE = "mode";

		private static final String DAILY_MONETARY_CONSTANT = "dailyMonetaryConstant";
		private static final String DAILY_MONETARY_CONSTANT_CMT = "[unit_of_money/day] Fixed cost of mode, per day.";

		private static final String DAILY_UTILITY_CONSTANT = "dailyUtilityConstant";

		private String mode = null;
		private double traveling = -6.0;
		private double distance = 0.0;
		private double monetaryDistanceRate = 0.0;
		private double constant = 0.0;
		private double dailyMonetaryConstant = 0.0;
		private double dailyUtilityConstant = 0.0;

		// @Override public String toString() {
		// String str = super.toString();
		// str += "[mode=" + mode + "]" ;
		// str += "[const=" + constant + "]" ;
		// str += "[beta_trav=" + traveling + "]" ;
		// str += "[beta_dist=" + distance + "]" ;
		// return str ;
		// }

		public ModeParams(final String mode) {
			super(SET_TYPE);
			setMode(mode);
		}

		ModeParams() {
			super(SET_TYPE);
		}

		@Override
		public Map<String, String> getComments() {
			final Map<String, String> map = super.getComments();
			map.put(MARGINAL_UTILITY_OF_TRAVELING,
					"[utils/hr] additional marginal utility of traveling.  normally negative.  this comes on top "
							+ "of the opportunity cost of time");
			map.put("marginalUtilityOfDistance_util_m",
					"[utils/m] utility of traveling (e.g. walking or driving) per m, normally negative.  this is "
							+ "on top of the time (dis)utility.");
			map.put(MONETARY_DISTANCE_RATE, MONETARY_DISTANCE_RATE_CMT);
			map.put(CONSTANT, CONSTANT_CMT );
			map.put(DAILY_UTILITY_CONSTANT, "[utils] daily utility constant. "
					+ "default=0 to be backwards compatible");
			map.put(DAILY_MONETARY_CONSTANT, DAILY_MONETARY_CONSTANT_CMT ) ;
			return map;
		}

		@StringSetter(MODE)
		public ModeParams setMode(final String mode) {
			testForLocked();
			this.mode = mode;
			return this ;
		}
		@StringGetter(MODE)
		public String getMode() {
			return mode;
		}
		// ---
		@StringSetter(MARGINAL_UTILITY_OF_TRAVELING)
		public ModeParams setMarginalUtilityOfTraveling(double traveling) {
			testForLocked();
			this.traveling = traveling;
			return this ;
		}
		@StringGetter(MARGINAL_UTILITY_OF_TRAVELING)
		public double getMarginalUtilityOfTraveling() {
			return this.traveling;
		}
		// ---
		@StringGetter("marginalUtilityOfDistance_util_m")
		public double getMarginalUtilityOfDistance() {
			return distance;
		}
		@StringSetter("marginalUtilityOfDistance_util_m")
		public ModeParams setMarginalUtilityOfDistance(double distance) {
			testForLocked();
			this.distance = distance;
			return this ;
		}

		/**
		 * @return {@value #CONSTANT_CMT}
		 */
		// ---
		@StringGetter(CONSTANT)
		public double getConstant() {
			return this.constant;
		}
		/**
		 * @param constant -- {@value #CONSTANT_CMT}
		 */
		@StringSetter(CONSTANT)
		public ModeParams setConstant(double constant) {
			testForLocked();
			this.constant = constant;
			return this ;
		}
		// ---
		/**
		 * @return {@value #MONETARY_DISTANCE_RATE_CMT}
		 */
		@StringGetter(MONETARY_DISTANCE_RATE)
		public double getMonetaryDistanceRate() {
			return this.monetaryDistanceRate;
		}

		/**
		 * @param monetaryDistanceRate -- {@value #MONETARY_DISTANCE_RATE_CMT}
		 */
		@StringSetter(MONETARY_DISTANCE_RATE)
		public ModeParams setMonetaryDistanceRate(double monetaryDistanceRate) {
			testForLocked();
			this.monetaryDistanceRate = monetaryDistanceRate;
			return this ;
		}
		/**
		 * @return {@value #DAILY_MONETARY_CONSTANT_CMT}
		 */
		@StringGetter(DAILY_MONETARY_CONSTANT)
		public double getDailyMonetaryConstant() {
			return dailyMonetaryConstant;
		}

		/**
		 * @param dailyMonetaryConstant -- {@value #DAILY_MONETARY_CONSTANT_CMT}
		 */
		@StringSetter(DAILY_MONETARY_CONSTANT)
		public ModeParams setDailyMonetaryConstant(double dailyMonetaryConstant) {
			this.dailyMonetaryConstant = dailyMonetaryConstant;
			return this ;
		}

		@StringGetter(DAILY_UTILITY_CONSTANT)
		public double getDailyUtilityConstant() {
			return dailyUtilityConstant;
		}

		@StringSetter(DAILY_UTILITY_CONSTANT)
		public ModeParams setDailyUtilityConstant(double dailyUtilityConstant) {
			this.dailyUtilityConstant = dailyUtilityConstant;
			return this ;
		}


	}

	public static class ScoringParameterSet extends ReflectiveConfigGroup {
		public static final String SET_TYPE = "scoringParameters";

		private ScoringParameterSet(final String subpopulation) {
			this();
			this.subpopulation = subpopulation;
		}

		private ScoringParameterSet() {
			super(SET_TYPE);
		}

		private String subpopulation = null;

		private double lateArrival = -18.0;
		private double earlyDeparture = -0.0;
		private double performing = +6.0;

		private double waiting = -0.0;

		private double marginalUtilityOfMoney = 1.0;

		private double utilityOfLineSwitch = -1;

		private Double waitingPt = null; // if not actively set by user, it will
											// later be set to "travelingPt".

		@StringGetter(LATE_ARRIVAL)
		public double getLateArrival_utils_hr() {
			return lateArrival;
		}

		@StringSetter(LATE_ARRIVAL)
		public void setLateArrival_utils_hr(double lateArrival) {
			testForLocked();
			this.lateArrival = lateArrival;
		}

		@StringGetter(EARLY_DEPARTURE)
		public double getEarlyDeparture_utils_hr() {
			return earlyDeparture;
		}

		@StringSetter(EARLY_DEPARTURE)
		public void setEarlyDeparture_utils_hr(double earlyDeparture) {
			testForLocked();
			this.earlyDeparture = earlyDeparture;
		}

		@StringGetter(PERFORMING)
		public double getPerforming_utils_hr() {
			return performing;
		}

		@StringSetter(PERFORMING)
		public void setPerforming_utils_hr(double performing) {
			this.performing = performing;
		}

		@StringGetter(MARGINAL_UTL_OF_MONEY)
		public double getMarginalUtilityOfMoney() {
			return marginalUtilityOfMoney;
		}

		@StringSetter(MARGINAL_UTL_OF_MONEY)
		public void setMarginalUtilityOfMoney(double marginalUtilityOfMoney) {
			testForLocked();
			this.marginalUtilityOfMoney = marginalUtilityOfMoney;
		}

		@StringGetter(UTL_OF_LINE_SWITCH)
		public double getUtilityOfLineSwitch() {
			return utilityOfLineSwitch;
		}

		@StringSetter(UTL_OF_LINE_SWITCH)
		public void setUtilityOfLineSwitch(double utilityOfLineSwitch) {
			testForLocked();
			this.utilityOfLineSwitch = utilityOfLineSwitch;
		}

		@StringGetter(WAITING)
		public double getMarginalUtlOfWaiting_utils_hr() {
			return this.waiting;
		}

		@StringSetter(WAITING)
		public void setMarginalUtlOfWaiting_utils_hr(final double waiting) {
			testForLocked();
			this.waiting = waiting;
		}

		@StringGetter("subpopulation")
		public String getSubpopulation() {
			return subpopulation;
		}

		/**
		 * This method is there to make the StringSetter/Getter automagic happy, but it is not meant to be used.
		 */
		@StringSetter("subpopulation")
		public void setSubpopulation(String subpopulation) {
			// TODO: handle case of default subpopulation
			if (this.subpopulation != null) {
				throw new IllegalStateException(
						"cannot change subpopulation in a scoring parameter set, as it is used for indexing.");
			}

			this.subpopulation = subpopulation;
		}

		@StringGetter(WAITING_PT)
		public double getMarginalUtlOfWaitingPt_utils_hr() {
			return waitingPt != null ? waitingPt
					: this.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling();
		}

		@StringSetter(WAITING_PT)
		public void setMarginalUtlOfWaitingPt_utils_hr(final Double waitingPt) {
			this.waitingPt = waitingPt;
		}

		/* parameter set handling */
		@Override
		public ConfigGroup createParameterSet(final String type) {
			switch (type) {
			case ActivityParams.SET_TYPE:
				return new ActivityParams();
			case ModeParams.SET_TYPE:
				return new ModeParams();
			default:
				throw new IllegalArgumentException(type);
			}
		}

		@Override
		protected void checkParameterSet(final ConfigGroup module) {
			switch (module.getName()) {
			case ActivityParams.SET_TYPE:
				if (!(module instanceof ActivityParams)) {
					throw new RuntimeException("wrong class for " + module);
				}
				final String t = ((ActivityParams) module).getActivityType();
				if (getActivityParams(t) != null) {
					throw new IllegalStateException("already a parameter set for activity type " + t);
				}
				break;
			case ModeParams.SET_TYPE:
				if (!(module instanceof ModeParams)) {
					throw new RuntimeException("wrong class for " + module);
				}
				final String m = ((ModeParams) module).getMode();
				if (getModes().get(m) != null) {
					throw new IllegalStateException("already a parameter set for mode " + m);
				}
				break;
			default:
				throw new IllegalArgumentException(module.getName());
			}
		}

		public Collection<String> getActivityTypes() {
			return this.getActivityParamsPerType().keySet();
		}

		public Collection<ActivityParams> getActivityParams() {
			@SuppressWarnings("unchecked")
			Collection<ActivityParams> collection = (Collection<ActivityParams>) getParameterSets(
					ActivityParams.SET_TYPE);
			for (ActivityParams params : collection) {
				if (this.isLocked()) {
					params.setLocked();
				}
			}
			return collection;
		}

		public Map<String, ActivityParams> getActivityParamsPerType() {
			final Map<String, ActivityParams> map = new LinkedHashMap<>();

			for (ActivityParams pars : getActivityParams()) {
				map.put(pars.getActivityType(), pars);
			}

			return map;
		}

		public ActivityParams getActivityParams(final String actType) {
			return this.getActivityParamsPerType().get(actType);
		}

		public ActivityParams getOrCreateActivityParams(final String actType) {
			ActivityParams params = this.getActivityParamsPerType().get(actType);

			if (params == null) {
				params = new ActivityParams(actType);
				addActivityParams(params);
			}

			return params;
		}

		public Map<String, ModeParams> getModes() {
			@SuppressWarnings("unchecked")
			final Collection<ModeParams> modes = (Collection<ModeParams>) getParameterSets(ModeParams.SET_TYPE);
			final Map<String, ModeParams> map = new LinkedHashMap<>();

			for (ModeParams pars : modes) {
				if (this.isLocked()) {
					pars.setLocked();
				}
				map.put(pars.getMode(), pars);
			}
			if (this.isLocked()) {
				return Collections.unmodifiableMap(map);
			} else {
				return map;
			}
		}

		public ModeParams getOrCreateModeParams(String modeName) {
			ModeParams modeParams = getModes().get(modeName);
			if (modeParams == null) {
				modeParams = new ModeParams(modeName);
				addParameterSet(modeParams);
			}
			return modeParams;
		}

		public void addModeParams(final ModeParams params) {
			final ModeParams previous = this.getModes().get(params.getMode());

			if (previous != null) {
				final boolean removed = removeParameterSet(previous);
				if (!removed)
					throw new RuntimeException("problem replacing mode params ");
				log.info("mode parameters for mode " + previous.getMode() + " were just overwritten.");
			}

			super.addParameterSet(params);
		}

		public void addActivityParams(final ActivityParams params) {
			final ActivityParams previous = this.getActivityParams(params.getActivityType());

			if (previous != null) {
				if (previous.getActivityType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
					log.error("ERROR: Activity parameters for activity type " + previous.getActivityType()
							+ " were just overwritten. This happens most "
							+ "likely because you defined them in the config file and the Controler overwrites them.  Or the other way "
							+ "round.  pt interaction has problems, but doing what you are doing here will just cause "
							+ "other (less visible) problem. Please take the effort to discuss with the core team "
							+ "what needs to be done.  kai, nov'12");
				} else {
					log.info("activity parameters for activity type " + previous.getActivityType()
							+ " were just overwritten.");
				}

				final boolean removed = removeParameterSet(previous);
				if (!removed)
					throw new RuntimeException("problem replacing activity params ");
			}

			super.addParameterSet(params);
		}

		/**
		 * Checks whether all the settings make sense or if there are some
		 * problems with the parameters currently set. Currently, this checks
		 * that for at least one activity type opening AND closing times are
		 * defined.
		 */
		@Override
		public void checkConsistency(Config config) {
			super.checkConsistency(config);


			boolean hasOpeningAndClosingTime = false;
			boolean hasOpeningTimeAndLatePenalty = false;


			// This cannot be done in ActivityParams (where it would make more
			// sense),
			// because some global properties are also checked
			for (ActivityParams actType : this.getActivityParams()) {
				if (actType.isScoringThisActivityAtAll()) {
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
				log.info("NO OPENING OR CLOSING TIMES DEFINED!\n\n"
						+ "There is no activity type that has an opening *and* closing time (or opening time and late penalty) defined.\n"
						+ "This usually means that the activity chains can be shifted by an arbitrary\n"
						+ "number of hours without having an effect on the score of the plans, and thus\n"
						+ "resulting in wrong results / traffic patterns.\n"
						+ "If you are using MATSim without time adaptation, you can ignore this warning.\n\n");
			}
			if (this.getMarginalUtlOfWaiting_utils_hr() != 0.0) {
				log.warn("marginal utl of wait set to: " + this.getMarginalUtlOfWaiting_utils_hr()
						+ ". Setting this different from zero is "
						+ "discouraged since there is already the marginal utility of time as a resource. The parameter was also used "
						+ "in the past for pt routing; if you did that, consider setting the new "
						+ "parameter waitingPt instead.");
			}
		}

	}

	private static class ReflectiveDelegate extends ReflectiveConfigGroup {
		private ReflectiveDelegate() {
			super(ScoringConfigGroup.GROUP_NAME);
		}

		private double learningRate = 1.0;
		private double brainExpBeta = 1.0;
		private double pathSizeLogitBeta = 1.0;

		private boolean writeExperiencedPlans = false;

		private Double fractionOfIterationsToStartScoreMSA = null;

		private boolean usingOldScoringBelowZeroUtilityDuration = false;

		private boolean explainScores = false;

		@StringGetter(FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA)
		public Double getFractionOfIterationsToStartScoreMSA() {
			return fractionOfIterationsToStartScoreMSA;
		}

		@StringSetter(FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA)
		public void setFractionOfIterationsToStartScoreMSA(Double fractionOfIterationsToStartScoreMSA) {
			testForLocked();
			this.fractionOfIterationsToStartScoreMSA = fractionOfIterationsToStartScoreMSA;
		}

		@StringGetter(LEARNING_RATE)
		public double getLearningRate() {
			return learningRate;
		}

		@StringSetter(LEARNING_RATE)
		public void setLearningRate(double learningRate) {
			testForLocked();
			this.learningRate = learningRate;
		}

		@StringGetter(BRAIN_EXP_BETA)
		public double getBrainExpBeta() {
			return brainExpBeta;
		}

		@StringSetter(BRAIN_EXP_BETA)
		public void setBrainExpBeta(double brainExpBeta) {
			testForLocked();
			this.brainExpBeta = brainExpBeta;
		}

		@StringGetter(PATH_SIZE_LOGIT_BETA)
		public double getPathSizeLogitBeta() {
			return pathSizeLogitBeta;
		}

		@StringSetter(PATH_SIZE_LOGIT_BETA)
		public void setPathSizeLogitBeta(double beta) {
			testForLocked();
			if (beta != 0.) {
				log.warn("Setting pathSizeLogitBeta different from zero is experimental.  KN, Sep'08");
			}
			this.pathSizeLogitBeta = beta;
		}

		@StringGetter(USING_OLD_SCORING_BELOW_ZERO_UTILITY_DURATION)
		public boolean isUsingOldScoringBelowZeroUtilityDuration() {
			return usingOldScoringBelowZeroUtilityDuration;
		}

		@StringSetter(USING_OLD_SCORING_BELOW_ZERO_UTILITY_DURATION)
		public void setUsingOldScoringBelowZeroUtilityDuration(boolean usingOldScoringBelowZeroUtilityDuration) {
			// should be disabled like in the following.  kai, may'22
//			if ( usingOldScoringBelowZeroUtilityDuration ) {
//				throw new RuntimeException( "using old scoringBelowZeroUtility duration is no longer possible.  Use matsim version 14.0 " +
//									    "or older if you truly need this for backwards compatibility." )
//			}
			testForLocked();
			this.usingOldScoringBelowZeroUtilityDuration = usingOldScoringBelowZeroUtilityDuration;
		}

		@StringGetter(WRITE_EXPERIENCED_PLANS)
		public boolean isWriteExperiencedPlans() {
			return writeExperiencedPlans;
		}

		@StringSetter(WRITE_EXPERIENCED_PLANS)
		public void setWriteExperiencedPlans(boolean writeExperiencedPlans) {
			testForLocked();
			this.writeExperiencedPlans = writeExperiencedPlans;
		}

		@StringSetter(WRITE_SCORE_EXPLANATIONS)
		public void setWriteScoreExplanations(boolean explainScores) {
			this.explainScores = explainScores;
		}

		@StringGetter(WRITE_SCORE_EXPLANATIONS)
		public boolean isWriteScoreExplanations() {
			return explainScores;
		}
	}
}
