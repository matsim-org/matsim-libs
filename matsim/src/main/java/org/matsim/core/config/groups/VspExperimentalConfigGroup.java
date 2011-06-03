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
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * config group for experimental parameters. this group and its parameters should not be used outside of vsp.
 * @author dgrether
 */
public class VspExperimentalConfigGroup extends org.matsim.core.config.Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "vspExperimental";
	
	// ---
	private static final String REMOVING_UNNECESSARY_PLAN_ATTRIBUTES = "removingUnnecessaryPlanAttributes" ;
	private boolean removingUnneccessaryPlanAttributes = false ;

	// ---

	@Deprecated
	private static final String USE_ACTIVITY_DURATIONS = "useActivityDurations";
	private static final String ACTIVITY_DURATION_INTERPRETATION="activityDurationInterpretation" ;

	public static final String MIN_OF_DURATION_AND_END_TIME="minOfDurationAndEndTime" ;
	public static final String TRY_END_TIME_THEN_DURATION="tryEndTimeThenDuration" ;
	public static final String END_TIME_ONLY="endTimeOnly" ;

//	@Deprecated
//	private boolean useActivityDurations = true;
	private String activityDurationInterpretation = MIN_OF_DURATION_AND_END_TIME ;

	// ---
	
	private static final String INPUT_MZ05_FILE = "inputMZ05File";
	private String inputMZ05File = "";

	// ---
	
	private static final String MODES_FOR_SUBTOURMODECHOICE = "modes";
	private static final String CHAIN_BASED_MODES = "chainBasedModes";

	private String modesForSubTourModeChoice = "car, pt";
	private String chainBasedModes = "car";
	
	// ---
	
	private static final String COLORING="coloring" ;
	
	public static final String COLORING_STANDARD = "standard" ;
	public static final String COLORING_BVG = "bvg" ;

	private String coloring = COLORING_STANDARD ;
	
	// ---
	@Deprecated //somewhen should be deleted, Yu 03.2011
	private static final String OFFSET_WALK = "offsetWalk";
	// private double offsetWalk = 0d;

	// ---
	
	private static final String USING_OPPORTUNITY_COST_OF_TIME_FOR_PT_ROUTING = 
		"usingOpportunityCostOfTimeForPtRouting" ;
	private boolean isUsingOpportunityCostOfTimeInPtRouting = false ;
	
	// ---
	
	private static final String VSP_DEFAULTS_CHECKING_LEVEL = "vspDefaultsCheckingLevel" ;
	
	public static final String IGNORE = "ignore" ;
	public static final String WARN = "warn" ;
	public static final String ABORT = "abort" ;
	
	private String vspDefaultsCheckingLevel = IGNORE ;



	public VspExperimentalConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		
		map.put(VSP_DEFAULTS_CHECKING_LEVEL, "Options: `"+IGNORE+"', `"+WARN+"', `"+ABORT+"'.  Default: either `"+IGNORE+"' or `"
				+WARN+"'.\n  When violating VSP defaults, this results in " +
				"nothing, warnings, or aborts.  Members of VSP should use `abort' or talk to kai.") ;
		
		map.put(USE_ACTIVITY_DURATIONS, "(deprecated, use " + ACTIVITY_DURATION_INTERPRETATION 
				+ " instead) Set this flag to false if the duration attribute of the activity should not be considered in QueueSimulation");
		map.put(ACTIVITY_DURATION_INTERPRETATION, "String: " + MIN_OF_DURATION_AND_END_TIME + "', '" + TRY_END_TIME_THEN_DURATION 
				+ "', '" + END_TIME_ONLY + "'") ;
		
		map.put(REMOVING_UNNECESSARY_PLAN_ATTRIBUTES, "(not yet implemented) will remove plan attributes that are presumably not used, such as " +
				"activityStartTime. default=false") ;

		map.put(INPUT_MZ05_FILE, "Set this filename of MZ05 daily analysis");

		map.put(MODES_FOR_SUBTOURMODECHOICE, "(do not use) set the traffic mode option for subTourModeChoice by Yu");
		map.put(CHAIN_BASED_MODES, "(do not use) set chainBasedModes for subTourModeChoice by Yu. E.g. \"car,bike\", \"car\"");

		map.put(OFFSET_WALK, "(deprecated, use corresponding option in planCalcScore) " +
				"set offset for mode \"walk\" in leg scoring function");
		
		map.put(COLORING, "coloring scheme for otfvis.  Currently (2010) allowed values: ``standard'', ``bvg''") ;
		map.put(USING_OPPORTUNITY_COST_OF_TIME_FOR_PT_ROUTING, 
				"indicates if, for routing, the opportunity cost of time should be added to the mode-specific marginal " +
				"utilities of time.\n\t\t" +
				"Default is false although some of us (bk, kn) think this should always be true.\n" +
				"This is only a suggestion since there is (by matsim design) no way to enforce that mental modules " +
				"obey this." ) ;
		return map;
	}

	@Override
	public String getValue(final String key) {
		throw new RuntimeException(" use direct getter; aborting ... " ) ;
	}

	@Override
	public void addParam(final String key, final String value) {
		if (USE_ACTIVITY_DURATIONS.equalsIgnoreCase(key)) {
//			this.setUseActivityDurations(Boolean.parseBoolean(value));
			if ( Boolean.parseBoolean(value) ) {
				setActivityDurationInterpretation( MIN_OF_DURATION_AND_END_TIME ) ;
			} else {
				setActivityDurationInterpretation( END_TIME_ONLY ) ; 
			}
		} else if ( ACTIVITY_DURATION_INTERPRETATION.equalsIgnoreCase(key)) {
			setActivityDurationInterpretation(value) ;
		} else if ( REMOVING_UNNECESSARY_PLAN_ATTRIBUTES.equalsIgnoreCase(key)) {
			setRemovingUnneccessaryPlanAttributes(Boolean.parseBoolean(value)) ;
		} else if ( COLORING.equalsIgnoreCase(key) ) {
			setColoring( value ) ;
		} else if (INPUT_MZ05_FILE.equalsIgnoreCase(key)) {
			setInputMZ05File(value);
		} else if (MODES_FOR_SUBTOURMODECHOICE.equalsIgnoreCase(key)) {
			setModesForSubTourModeChoice(value);
		} else if ( USING_OPPORTUNITY_COST_OF_TIME_FOR_PT_ROUTING.equals(key) ) {
			this.setUsingOpportunityCostOfTimeInPtRouting(Boolean.parseBoolean(value)) ;
		} else if (CHAIN_BASED_MODES.equalsIgnoreCase(key)) {
			setChainBasedModes(value);
		} else if (OFFSET_WALK.equalsIgnoreCase(key)) {
//			this.setOffsetWalk(Double.parseDouble(value));
			throw new RuntimeException( "offsetWalk in vspExperimentalConfigGroup is no longer; use the (alternative-specific) " +
					"constants in planCalcScore.  Aborting since you need to fix this ..." ) ;
		} else if ( VSP_DEFAULTS_CHECKING_LEVEL.equals(key) ) {
			this.setVspDefaultsCheckingLevel(value) ;
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
//		map.put(USE_ACTIVITY_DURATIONS, isUseActivityDurations() );
		map.put(ACTIVITY_DURATION_INTERPRETATION, getActivityDurationInterpretation()) ;
		
		map.put(REMOVING_UNNECESSARY_PLAN_ATTRIBUTES, Boolean.toString(isRemovingUnneccessaryPlanAttributes()) ) ;

		map.put(INPUT_MZ05_FILE, getInputMZ05File() ) ;

		map.put(MODES_FOR_SUBTOURMODECHOICE, getModesForSubTourModeChoice() ) ;
		map.put(CHAIN_BASED_MODES, getChainBasedModes() );

//		map.put(OFFSET_WALK, Double.toString( this.getOffsetWalk() ) );
		
		map.put(USING_OPPORTUNITY_COST_OF_TIME_FOR_PT_ROUTING, 
				Boolean.toString( this.isUsingOpportunityCostOfTimeInPtRouting()) ) ;
		map.put(COLORING, getColoring() ) ;
		
		map.put( VSP_DEFAULTS_CHECKING_LEVEL, this.getVspDefaultsCheckingLevel() ) ;
		
		return map;
	}
	
	@Override
	protected void checkConsistency() throws RuntimeException {
		Logger.getLogger(this.getClass()).warn( "entering checkVspDefaults ...") ;
		
		// begin vsp default definitions
		final boolean usingOpportunityCostOfTimeInPtRoutingDefault = true ;
		// end vsp default definitions
		
		boolean problem = false ;
		if ( this.getVspDefaultsCheckingLevel().equals( VspExperimentalConfigGroup.WARN )
				|| this.getVspDefaultsCheckingLevel().equals( VspExperimentalConfigGroup.ABORT ) ) {

			if ( this.isUsingOpportunityCostOfTimeInPtRouting() != usingOpportunityCostOfTimeInPtRoutingDefault ) {
				Logger.getLogger(this.getClass()).warn( "violating VSP defaults; usingOpportunityCostOfTimeInPtRouting should be " +
						"set to: " + usingOpportunityCostOfTimeInPtRoutingDefault ) ;
				problem = true ;
			}

		}

		if ( this.getVspDefaultsCheckingLevel().equals(VspExperimentalConfigGroup.ABORT) && problem ) {
			String str = "violating VSP defaults and vspDefaultsCheckingLevel set to `abort', thus aborting ..." ;
			Logger.getLogger(this.getClass()).fatal( str ) ;
			throw new RuntimeException( str ) ;
		}

		Logger.getLogger(this.getClass()).warn( "leaving checkVspDefaults ...") ;
	}



//	public boolean isUseActivityDurations() {
//		return this.useActivityDurations;
//	}
//
//	public void setUseActivityDurations(final boolean useActivityDurations) {
//		this.useActivityDurations = useActivityDurations;
//	}
	
	public String getColoring() {
		return coloring ;
	}
	public void setColoring( String value ) {
		coloring = value ;
	}

	public String getInputMZ05File() {
		return inputMZ05File;
	}

	public void setInputMZ05File(final String inputMZ05File) {
		this.inputMZ05File = inputMZ05File;
	}

	public String getModesForSubTourModeChoice() {
		return modesForSubTourModeChoice;
	}

	public void setModesForSubTourModeChoice(final String modesForSubTourModeChoice) {
		this.modesForSubTourModeChoice = modesForSubTourModeChoice;
	}

	public String getChainBasedModes() {
		return chainBasedModes;
	}

	public void setChainBasedModes(final String chainBasedModes) {
		this.chainBasedModes = chainBasedModes;
	}

//	public double getOffsetWalk() {
//		return this.offsetWalk;
//	}

//	public void setOffsetWalk(final double offsetWalk) {
//		this.offsetWalk = offsetWalk;
//	}

	public String getActivityDurationInterpretation() {
		return activityDurationInterpretation;
	}

	public void setActivityDurationInterpretation(String activityDurationInterpretation) {
		this.activityDurationInterpretation = activityDurationInterpretation;
	}

	public boolean isRemovingUnneccessaryPlanAttributes() {
		return removingUnneccessaryPlanAttributes;
	}

	public void setRemovingUnneccessaryPlanAttributes(boolean removingUnneccessaryPlanAttributes) {
		this.removingUnneccessaryPlanAttributes = removingUnneccessaryPlanAttributes;
	}

	public boolean isUsingOpportunityCostOfTimeInPtRouting() {
		return isUsingOpportunityCostOfTimeInPtRouting;
	}

	public void setUsingOpportunityCostOfTimeInPtRouting(boolean tmp) {
		this.isUsingOpportunityCostOfTimeInPtRouting = tmp;
	}

	public void setVspDefaultsCheckingLevel(String vspDefaultsCheckingLevel) {
		this.vspDefaultsCheckingLevel = vspDefaultsCheckingLevel;
	}

	public String getVspDefaultsCheckingLevel() {
		return vspDefaultsCheckingLevel;
	}

}
