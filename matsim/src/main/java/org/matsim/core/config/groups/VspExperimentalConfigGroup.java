/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.config.experimental.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.Time;

interface ConfigKey {}

/**
 * config group for experimental parameters. this group and its parameters should not be used outside of vsp.
 * @author dgrether
 * @author nagel
 */
public class VspExperimentalConfigGroup extends ReflectiveConfigGroup {
	
	private final static Logger log = Logger.getLogger(VspExperimentalConfigGroup.class);

	public static final String GROUP_NAME = "vspExperimental";

	// ---
	private static final String REMOVING_UNNECESSARY_PLAN_ATTRIBUTES = "removingUnnecessaryPlanAttributes" ;
	private boolean removingUnneccessaryPlanAttributes = false ;

	// ---

	public static enum ActivityDurationInterpretation { minOfDurationAndEndTime, tryEndTimeThenDuration, @Deprecated endTimeOnly } 

	// ---

	private static final String INPUT_MZ05_FILE = "inputMZ05File";
	private String inputMZ05File = "";

	// ---

	private static final String WRITING_OUTPUT_EVENTS = "writingOutputEvents" ;
	private boolean writingOutputEvents = false ;

	// ---

	private static final String MATSIM_GLOBAL_TIME_FORMAT = "matsimGlobalTimeformat" ;
	private String matsimGlobalTimeFormat = Time.TIMEFORMAT_HHMMSS;

	// ---

	public VspExperimentalConfigGroup() {
		super(GROUP_NAME);
	}

	// ---
	private static final String VSP_DEFAULTS_CHECKING_LEVEL = "vspDefaultsCheckingLevel" ;
	public static final String IGNORE = "ignore" ;
	public static final String WARN = "warn" ;
	public static final String ABORT = "abort" ;
	private String vspDefaultsCheckingLevel = WARN ;
	@StringGetter(VSP_DEFAULTS_CHECKING_LEVEL)
	public String getVspDefaultsCheckingLevel() {
		return vspDefaultsCheckingLevel;
	}
	@StringSetter(VSP_DEFAULTS_CHECKING_LEVEL)
	public void setVspDefaultsCheckingLevel(String vspDefaultsCheckingLevel) {
		this.vspDefaultsCheckingLevel = vspDefaultsCheckingLevel;
	}
	// ---
	private static final String LOGIT_SCALE_PARAM_FOR_PLANS_REMOVAL = "logitScaleParamForPlansRemoval" ;
	private double logitScaleParamForPlansRemoval = 1. ;
	@StringGetter(LOGIT_SCALE_PARAM_FOR_PLANS_REMOVAL)
	public double getLogitScaleParamForPlansRemoval() {
		return logitScaleParamForPlansRemoval;
	}
	@StringSetter(LOGIT_SCALE_PARAM_FOR_PLANS_REMOVAL)
	public void setLogitScaleParamForPlansRemoval(double logitScaleParamForPlansRemoval) {
		this.logitScaleParamForPlansRemoval = logitScaleParamForPlansRemoval;
	}
	// ---
	private static final String GENERATING_BOARDING_DENIED_EVENT = "isGeneratingBoardingDeniedEvent" ;  // seems to be singular.
	private boolean isGeneratingBoardingDeniedEvent = false ; // default is that this event is NOT generated.  kai, oct'12
	@StringGetter(GENERATING_BOARDING_DENIED_EVENT)
	public boolean isGeneratingBoardingDeniedEvents() {
		return isGeneratingBoardingDeniedEvent;
	}
	@StringSetter(GENERATING_BOARDING_DENIED_EVENT)
	public void setGeneratingBoardingDeniedEvent(boolean isGeneratingBoardingDeniedEvent) {
		this.isGeneratingBoardingDeniedEvent = isGeneratingBoardingDeniedEvent;
	}
	// ---
	private static final String ABLE_TO_OVERWRITE_PT_INTERACTION_PARAMS = "isAbleToOverwritePtInteractionParams" ; 
	private boolean isAbleToOverwritePtInteractionParams = false ; // default is that this NOT allowed.  kai, nov'12 
	@StringGetter(ABLE_TO_OVERWRITE_PT_INTERACTION_PARAMS)
	public boolean isAbleToOverwritePtInteractionParams() {
		return isAbleToOverwritePtInteractionParams;
	}
	@StringSetter(ABLE_TO_OVERWRITE_PT_INTERACTION_PARAMS)
	public void setAbleToOverwritePtInteractionParams(boolean isAbleToOverwritePtInteractionParams) {
		this.isAbleToOverwritePtInteractionParams = isAbleToOverwritePtInteractionParams;
	}
	// ---
	private static final String USING_OPPORTUNITY_COST_OF_TIME_FOR_LOCATION_CHOICE = "isUsingOpportunityCostOfTimeForLocationChoice" ; 
	private boolean isUsingOpportunityCostOfTimeForLocationChoice = true ;
	@StringGetter(USING_OPPORTUNITY_COST_OF_TIME_FOR_LOCATION_CHOICE)
	public boolean isUsingOpportunityCostOfTimeForLocationChoice() {
		return isUsingOpportunityCostOfTimeForLocationChoice;
	}
	@StringSetter(USING_OPPORTUNITY_COST_OF_TIME_FOR_LOCATION_CHOICE)
	public void setUsingOpportunityCostOfTimeForLocationChoice(boolean isUsingOpportunityCostOfTimeForLocationChoice) {
		this.isUsingOpportunityCostOfTimeForLocationChoice = isUsingOpportunityCostOfTimeForLocationChoice;
	}
	// ---



	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();

//		map.put(SCORE_MSA_STARTS_AT_ITERATION, "(deprecated, use " + FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA + ") first iteration of MSA score averaging. The matsim theory department " +
//				"suggests to use this together with switching of choice set innovation, but it has not been tested yet.") ;
		map.put( ABLE_TO_OVERWRITE_PT_INTERACTION_PARAMS, "(do not use except of you have to) There was a problem with pt interaction scoring.  Some people solved it by overwriting the " +
						"parameters of the pt interaction activity type.  Doing this now throws an Exception.  If you still insist on doing this, " +
				"set the following to true.") ;
		map.put(USING_OPPORTUNITY_COST_OF_TIME_FOR_LOCATION_CHOICE, "if an approximation of the opportunity cost of time is included into the radius calculation for location choice." +
				"`true' will be faster, but it is an approximation.  Default is `true'; `false' is available for backwards compatibility.") ;
		map.put(MATSIM_GLOBAL_TIME_FORMAT, "changes MATSim's global time format used in output files. Can be used to enforce writing fractional seconds e.g. in output_plans.  " +
		"default is `hh:mm:ss' (because of backwards compatibility). see Time.java for possible formats");

		map.put(WRITING_OUTPUT_EVENTS, "if true then writes output_events in output directory.  default is `false'." +
		" Will only work when lastIteration is multiple of events writing interval" ) ;



		map.put( VSP_DEFAULTS_CHECKING_LEVEL, 
				"Options: `"+IGNORE+"', `"+WARN+"', `"+ABORT+"'.  Default: either `"+IGNORE+"' or `"
				+WARN+"'.\n\t\t" +
				"When violating VSP defaults, this results in " +
		"nothing, warnings, or aborts.  Members of VSP should use `abort' or talk to kai.") ;

		map.put(REMOVING_UNNECESSARY_PLAN_ATTRIBUTES, "(not tested) will remove plan attributes that are presumably not used, such as " +
		"activityStartTime. default=false") ;

		map.put(INPUT_MZ05_FILE, "(do not use) Set this filename of MZ05 daily analysis");

//		map.put(MODES_FOR_SUBTOURMODECHOICE, "(do not use) set the traffic mode option for subTourModeChoice by Yu");
//		map.put(CHAIN_BASED_MODES, "(do not use) set chainBasedModes for subTourModeChoice by Yu. E.g. \"car,bike\", \"car\"");

		return map;
	}
	@StringGetter(INPUT_MZ05_FILE)
	public String getInputMZ05File() {
		return this.inputMZ05File;
	}
	@StringSetter(INPUT_MZ05_FILE)
	public void setInputMZ05File(final String inputMZ05File) {
		this.inputMZ05File = inputMZ05File;
	}
//	@StringGetter(MODES_FOR_SUBTOURMODECHOICE)
//	public String getModesForSubTourModeChoice() {
//		return this.modesForSubTourModeChoice;
//	}
//	@StringSetter(MODES_FOR_SUBTOURMODECHOICE)
//	public void setModesForSubTourModeChoice(final String modesForSubTourModeChoice) {
//		this.modesForSubTourModeChoice = modesForSubTourModeChoice;
//	}
////	@StringGetter(CHAIN_BASED_MODES)
////	public String getChainBasedModes() {
////		return this.chainBasedModes;
////	}
//	@StringSetter(CHAIN_BASED_MODES)
//	public void setChainBasedModes(final String chainBasedModes) {
//		this.chainBasedModes = chainBasedModes;
//	}
	@StringGetter(REMOVING_UNNECESSARY_PLAN_ATTRIBUTES)
	public boolean isRemovingUnneccessaryPlanAttributes() {
		return this.removingUnneccessaryPlanAttributes;
	}
	@StringSetter(REMOVING_UNNECESSARY_PLAN_ATTRIBUTES)
	public void setRemovingUnneccessaryPlanAttributes(final boolean removingUnneccessaryPlanAttributes) {
		this.removingUnneccessaryPlanAttributes = removingUnneccessaryPlanAttributes;
	}

	@StringGetter(WRITING_OUTPUT_EVENTS)
	public boolean isWritingOutputEvents() {
		return this.writingOutputEvents ;
	}
	@StringSetter(WRITING_OUTPUT_EVENTS)
	public void setWritingOutputEvents(boolean writingOutputEvents) {
		this.writingOutputEvents = writingOutputEvents;
	}
	@StringGetter(MATSIM_GLOBAL_TIME_FORMAT)
	public String getMatsimGlobalTimeFormat() {
		return this.matsimGlobalTimeFormat;
	}
	@StringSetter(MATSIM_GLOBAL_TIME_FORMAT)
	public void setMatsimGlobalTimeFormat(String format) {
		this.matsimGlobalTimeFormat = format;
		Time.setDefaultTimeFormat(format) ;
	}
	
	@Override
	protected void checkConsistency() {
//		if ( getScoreMSAStartsAtIteration()!=null ) {
//			log.warn( "config option " + SCORE_MSA_STARTS_AT_ITERATION + " is deprecated; use " + FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA ) ;
//		}
//		if ( getScoreMSAStartsAtIteration()!=null && getFractionOfIterationsToStartScoreMSA()!=null ) {
//			throw new RuntimeException("cannot set both of " + SCORE_MSA_STARTS_AT_ITERATION + " and " + 
//					FRACTION_OF_ITERATIONS_TO_START_SCORE_MSA + " to non-null.  Aborting ...") ;
//		}
	}
}
