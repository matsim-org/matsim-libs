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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * config group for experimental parameters. this group and its parameters should not be used outside of vsp.
 * @author dgrether
 * @author nagel
 */
public final class VspExperimentalConfigGroup extends ReflectiveConfigGroup {

	@SuppressWarnings("unused")
	private final static Logger log = LogManager.getLogger(VspExperimentalConfigGroup.class);

	public static final String GROUP_NAME = "vspExperimental";



	// ---

//	private static final String INPUT_MZ05_FILE = "inputMZ05File";
//	private String inputMZ05File = "";

	// ---

	private static final String WRITING_OUTPUT_EVENTS = "writingOutputEvents" ;
	private boolean writingOutputEvents = true ;

	// ---

	public VspExperimentalConfigGroup() {
		super(GROUP_NAME);
	}

	// ---
	private static final String VSP_DEFAULTS_CHECKING_LEVEL = "vspDefaultsCheckingLevel" ;
	public static enum VspDefaultsCheckingLevel { ignore, info, warn, abort }

	private VspDefaultsCheckingLevel vspDefaultsCheckingLevel = VspDefaultsCheckingLevel.ignore ;
	@StringGetter(VSP_DEFAULTS_CHECKING_LEVEL)
	public VspDefaultsCheckingLevel getVspDefaultsCheckingLevel() {
		return vspDefaultsCheckingLevel;
	}
	@StringSetter(VSP_DEFAULTS_CHECKING_LEVEL)
	public void setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel vspDefaultsCheckingLevel) {
		testForLocked() ;
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
		testForLocked() ;
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
		testForLocked() ;
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
		testForLocked() ;
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
		testForLocked() ;
		this.isUsingOpportunityCostOfTimeForLocationChoice = isUsingOpportunityCostOfTimeForLocationChoice;
	}
	// ---
	public enum CheckingOfMarginalUtilityOfTravellng { allZero, none }

	private CheckingOfMarginalUtilityOfTravellng checkingOfMarginalUtilityOfTravellng = CheckingOfMarginalUtilityOfTravellng.allZero;
	public CheckingOfMarginalUtilityOfTravellng getCheckingOfMarginalUtilityOfTravellng(){
		return checkingOfMarginalUtilityOfTravellng;
	}
	public void setCheckingOfMarginalUtilityOfTravellng( CheckingOfMarginalUtilityOfTravellng checkingOfMarginalUtilityOfTravellng ){
		this.checkingOfMarginalUtilityOfTravellng = checkingOfMarginalUtilityOfTravellng;
	}
	// ---



	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();

		map.put( ABLE_TO_OVERWRITE_PT_INTERACTION_PARAMS, "(do not use except of you have to) There was a problem with pt interaction scoring.  Some people solved it by overwriting the " +
						"parameters of the pt interaction activity type.  Doing this now throws an Exception.  If you still insist on doing this, " +
				"set the following to true.") ;
		map.put(USING_OPPORTUNITY_COST_OF_TIME_FOR_LOCATION_CHOICE, "if an approximation of the opportunity cost of time is included into the radius calculation for location choice." +
				"`true' will be faster, but it is an approximation.  Default is `true'; `false' is available for backwards compatibility.") ;

		map.put(WRITING_OUTPUT_EVENTS, "if true then writes output_events in output directory.  default is `false'." +
		" Will only work when lastIteration is multiple of events writing interval" ) ;

		StringBuilder options = new StringBuilder() ;
		for ( VspDefaultsCheckingLevel option : VspDefaultsCheckingLevel.values() ) {
			options.append(option + " | ") ;
		}
		map.put( VSP_DEFAULTS_CHECKING_LEVEL,
				"Options: | " + options + ".  When violating VSP defaults, this results in " +
		"nothing, logfile infos, logfile warnings, or aborts.  Members of VSP should use `abort' or talk to kai.") ;

//		map.put(INPUT_MZ05_FILE, "(do not use) Set this filename of MZ05 daily analysis");

		return map;
	}
//	@StringGetter(INPUT_MZ05_FILE)
//	public String getInputMZ05File() {
//		return this.inputMZ05File;
//	}
//	@StringSetter(INPUT_MZ05_FILE)
//	public void setInputMZ05File(final String inputMZ05File) {
//		testForLocked() ;
//		this.inputMZ05File = inputMZ05File;
//	}
	@StringGetter(WRITING_OUTPUT_EVENTS)
	public boolean isWritingOutputEvents() {
		return this.writingOutputEvents ;
	}
	@StringSetter(WRITING_OUTPUT_EVENTS)
	public void setWritingOutputEvents(boolean writingOutputEvents) {
		testForLocked() ;
		this.writingOutputEvents = writingOutputEvents;
	}

	@Override
	protected void checkConsistency(Config config) {
	}

}
