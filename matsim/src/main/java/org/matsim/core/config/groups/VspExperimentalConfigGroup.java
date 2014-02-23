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
import org.matsim.core.config.experimental.ReflectiveModule;
import org.matsim.core.utils.misc.Time;

interface ConfigKey {}

/**
 * config group for experimental parameters. this group and its parameters should not be used outside of vsp.
 * @author dgrether
 * @author nagel
 */
public class VspExperimentalConfigGroup extends ReflectiveModule {
	
	private final static Logger log = Logger.getLogger(VspExperimentalConfigGroup.class);

	public static final String GROUP_NAME = "vspExperimental";

	// ---
	private static final String REMOVING_UNNECESSARY_PLAN_ATTRIBUTES = "removingUnnecessaryPlanAttributes" ;
	private boolean removingUnneccessaryPlanAttributes = false ;

	// ---

	private static final String ACTIVITY_DURATION_INTERPRETATION="activityDurationInterpretation" ;

	public static enum ActivityDurationInterpretation { minOfDurationAndEndTime, tryEndTimeThenDuration, @Deprecated endTimeOnly } 

//	private ActivityDurationInterpretation activityDurationInterpretation = ActivityDurationInterpretation.minOfDurationAndEndTime ;
	private ActivityDurationInterpretation activityDurationInterpretation = ActivityDurationInterpretation.tryEndTimeThenDuration ;
	// making this change brings tests in ReRoutingTest essentially to a halt.  Happens at the diffutil near its end.

	// ---

	private static final String INPUT_MZ05_FILE = "inputMZ05File";
	private String inputMZ05File = "";

	// ---

	private static final String MODES_FOR_SUBTOURMODECHOICE = "modes";
	private static final String CHAIN_BASED_MODES = "chainBasedModes";

	private String modesForSubTourModeChoice = "car, pt";
	private String chainBasedModes = "car";

	// ---

	private static final String EMISSION_ROADTYPE_MAPPING_FILE = "emissionRoadTypeMappingFile";
	private String emissionRoadTypeMappingFile = null;

	private static final String EMISSION_VEHICLE_FILE = "emissionVehicleFile";
	private String emissionVehicleFile = null;

	private static final String EMISSION_FACTORS_WARM_FILE_AVERAGE = "averageFleetWarmEmissionFactorsFile";
	private String averageFleetWarmEmissionFactorsFile = null;

	private static final String EMISSION_FACTORS_COLD_FILE_AVERAGE = "averageFleetColdEmissionFactorsFile";
	private String averageFleetColdEmissionFactorsFile = null;

	private static final String USING_DETAILED_EMISSION_CALCULATION = "usingDetailedEmissionCalculation";
	private boolean isUsingDetailedEmissionCalculation = false;

	private static final String EMISSION_FACTORS_WARM_FILE_DETAILED = "detailedWarmEmissionFactorsFile" ;
	private String detailedWarmEmissionFactorsFile = null;

	private static final String EMISSION_FACTORS_COLD_FILE_DETAILED = "detailedColdEmissionFactorsFile";
	private String detailedColdEmissionFactorsFile;

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
	private String vspDefaultsCheckingLevel = IGNORE ;
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
	private static final String SCORE_MSA_STARTS_AT_ITERATION = "scoreMSAStartsAtIteration" ;
	private Integer scoreMSAStartsAtIteration = null ;
	@StringGetter(SCORE_MSA_STARTS_AT_ITERATION)
	public Integer getScoreMSAStartsAtIteration() {
		return scoreMSAStartsAtIteration;
	}
	@StringSetter(SCORE_MSA_STARTS_AT_ITERATION)
	public void setScoreMSAStartsAtIteration(Integer scoreMSAStartsAtIteration) {
		this.scoreMSAStartsAtIteration = scoreMSAStartsAtIteration;
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

		map.put(SCORE_MSA_STARTS_AT_ITERATION, "first iteration of MSA score averaging. The matsim theory department " +
				"suggests to use this together with switching of choice set innovation, but it has not been tested yet.") ;
		map.put( ABLE_TO_OVERWRITE_PT_INTERACTION_PARAMS, "(do not use except of you have to) There was a problem with pt interaction scoring.  Some people solved it by overwriting the " +
						"parameters of the pt interaction activity type.  Doing this now throws an Exception.  If you still insist on doing this, " +
				"set the following to true.") ;
		map.put(USING_OPPORTUNITY_COST_OF_TIME_FOR_LOCATION_CHOICE, "if an approximation of the opportunity cost of time is included into the radius calculation for location choice." +
				"`true' will be faster, but it is an approximation.  Default is `true'; `false' is available for backwards compatibility.") ;
		map.put(MATSIM_GLOBAL_TIME_FORMAT, "changes MATSim's global time format used in output files. Can be used to enforce writing fractional seconds e.g. in output_plans.  " +
		"default is `hh:mm:ss' (because of backwards compatibility). see Time.java for possible formats");

		map.put(WRITING_OUTPUT_EVENTS, "if true then writes output_events in output directory.  default is `false'." +
		" Will only work when lastIteration is multiple of events writing interval" ) ;

		map.put(EMISSION_ROADTYPE_MAPPING_FILE, "REQUIRED: mapping from input road types to HBEFA 3.1 road type strings");

		map.put(EMISSION_VEHICLE_FILE, "definition of a vehicle for every person (who is allowed to choose a vehicle in the simulation):" + "\n\t\t" +
				" - REQUIRED: vehicle type Id must start with the respective HbefaVehicleCategory followed by `;'" + "\n\t\t" +
				" - OPTIONAL: if detailed emission calculation is switched on, vehicle type Id should aditionally contain" +
				" HbefaVehicleAttributes (`Technology;SizeClasse;EmConcept'), corresponding to the strings in " + EMISSION_FACTORS_WARM_FILE_DETAILED);

		map.put(EMISSION_FACTORS_WARM_FILE_AVERAGE, "REQUIRED: file with HBEFA 3.1 fleet average warm emission factors");

		map.put(EMISSION_FACTORS_COLD_FILE_AVERAGE, "REQUIRED: file with HBEFA 3.1 fleet average cold emission factors");

		map.put(USING_DETAILED_EMISSION_CALCULATION, "if true then detailed emission factor files must be provided!");

		map.put(EMISSION_FACTORS_WARM_FILE_DETAILED, "OPTIONAL: file with HBEFA 3.1 detailed warm emission factors") ;

		map.put(EMISSION_FACTORS_COLD_FILE_DETAILED, "OPTIONAL: file with HBEFA 3.1 detailed cold emission factors");

		map.put( VSP_DEFAULTS_CHECKING_LEVEL, 
				"Options: `"+IGNORE+"', `"+WARN+"', `"+ABORT+"'.  Default: either `"+IGNORE+"' or `"
				+WARN+"'.\n\t\t" +
				"When violating VSP defaults, this results in " +
		"nothing, warnings, or aborts.  Members of VSP should use `abort' or talk to kai.") ;

		StringBuilder str = new StringBuilder() ;
		for ( ActivityDurationInterpretation itp : ActivityDurationInterpretation.values() ) {
			str.append(" ").append(itp.toString());
		}
		map.put(ACTIVITY_DURATION_INTERPRETATION, "String:" + str + ". Anything besides " 
				+ ActivityDurationInterpretation.minOfDurationAndEndTime + " will internally use a different " +
		"(simpler) version of the TimeAllocationMutator.") ;

		map.put(REMOVING_UNNECESSARY_PLAN_ATTRIBUTES, "(not tested) will remove plan attributes that are presumably not used, such as " +
		"activityStartTime. default=false") ;

		map.put(INPUT_MZ05_FILE, "(do not use) Set this filename of MZ05 daily analysis");

		map.put(MODES_FOR_SUBTOURMODECHOICE, "(do not use) set the traffic mode option for subTourModeChoice by Yu");
		map.put(CHAIN_BASED_MODES, "(do not use) set chainBasedModes for subTourModeChoice by Yu. E.g. \"car,bike\", \"car\"");

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
	@StringGetter(MODES_FOR_SUBTOURMODECHOICE)
	public String getModesForSubTourModeChoice() {
		return this.modesForSubTourModeChoice;
	}
	@StringSetter(MODES_FOR_SUBTOURMODECHOICE)
	public void setModesForSubTourModeChoice(final String modesForSubTourModeChoice) {
		this.modesForSubTourModeChoice = modesForSubTourModeChoice;
	}
	@StringGetter(CHAIN_BASED_MODES)
	public String getChainBasedModes() {
		return this.chainBasedModes;
	}
	@StringSetter(CHAIN_BASED_MODES)
	public void setChainBasedModes(final String chainBasedModes) {
		this.chainBasedModes = chainBasedModes;
	}
	@StringGetter(ACTIVITY_DURATION_INTERPRETATION)
	public ActivityDurationInterpretation getActivityDurationInterpretation() {
		return this.activityDurationInterpretation ;
	}
//	public void setActivityDurationInterpretation(final String str) {
//		ActivityDurationInterpretation actDurInterpret = ActivityDurationInterpretation.valueOf(str) ;
//		this.setActivityDurationInterpretation(actDurInterpret);
//	}
	@StringSetter(ACTIVITY_DURATION_INTERPRETATION)
	public void setActivityDurationInterpretation( final ActivityDurationInterpretation actDurInterpret ) {
		if ( ActivityDurationInterpretation.endTimeOnly.equals(actDurInterpret) ){
			/*
			 * I don't think this is the correct place for consistency checks but this bug is so hard to find that the user should be warned in any case. dg 08-2012
			 */
			log.warn("You are using " + actDurInterpret + " as activityDurationInterpretation. " +
			"This is not working in conjunction with the pt module as pt interaction activities then will never end!");
			log.warn("ActivityDurationInterpreation " + actDurInterpret + " is deprecated; use " 
					+ ActivityDurationInterpretation.minOfDurationAndEndTime + " instead. kai, jan'13") ;
		}
		this.activityDurationInterpretation = actDurInterpret;
	}
	@StringGetter(REMOVING_UNNECESSARY_PLAN_ATTRIBUTES)
	public boolean isRemovingUnneccessaryPlanAttributes() {
		return this.removingUnneccessaryPlanAttributes;
	}
	@StringSetter(REMOVING_UNNECESSARY_PLAN_ATTRIBUTES)
	public void setRemovingUnneccessaryPlanAttributes(final boolean removingUnneccessaryPlanAttributes) {
		this.removingUnneccessaryPlanAttributes = removingUnneccessaryPlanAttributes;
	}
	@StringSetter(EMISSION_ROADTYPE_MAPPING_FILE)
	public void setEmissionRoadTypeMappingFile(String roadTypeMappingFile) {
		this.emissionRoadTypeMappingFile = roadTypeMappingFile;
	}
	@StringGetter(EMISSION_ROADTYPE_MAPPING_FILE)
	public String getEmissionRoadTypeMappingFile() {
		return this.emissionRoadTypeMappingFile;
	}
	@StringSetter(EMISSION_VEHICLE_FILE)
	public void setEmissionVehicleFile(String emissionVehicleFile) {
		this.emissionVehicleFile = emissionVehicleFile;
	}
	@StringGetter(EMISSION_VEHICLE_FILE)
	public String getEmissionVehicleFile() {
		return this.emissionVehicleFile;
	}
	@StringSetter(EMISSION_FACTORS_WARM_FILE_AVERAGE)
	public void setAverageWarmEmissionFactorsFile(String averageFleetWarmEmissionFactorsFile) {
		this.averageFleetWarmEmissionFactorsFile = averageFleetWarmEmissionFactorsFile;
	}
	@StringGetter(EMISSION_FACTORS_WARM_FILE_AVERAGE)
	public String getAverageWarmEmissionFactorsFile() {
		return this.averageFleetWarmEmissionFactorsFile;
	}
	@StringSetter(EMISSION_FACTORS_COLD_FILE_AVERAGE)
	public void setAverageColdEmissionFactorsFile(String averageFleetColdEmissionFactorsFile) {
		this.averageFleetColdEmissionFactorsFile = averageFleetColdEmissionFactorsFile;
	}
	@StringGetter(EMISSION_FACTORS_COLD_FILE_AVERAGE)
	public String getAverageColdEmissionFactorsFile() {
		return this.averageFleetColdEmissionFactorsFile;
	}
	@StringGetter(USING_DETAILED_EMISSION_CALCULATION)
	public boolean isUsingDetailedEmissionCalculation(){
		return this.isUsingDetailedEmissionCalculation;
	}
	@StringSetter(USING_DETAILED_EMISSION_CALCULATION)
	public void setUsingDetailedEmissionCalculation(final boolean isUsingDetailedEmissionCalculation) {
		this.isUsingDetailedEmissionCalculation = isUsingDetailedEmissionCalculation;
	}
	@StringSetter(EMISSION_FACTORS_WARM_FILE_DETAILED)
	public void setDetailedWarmEmissionFactorsFile(String detailedWarmEmissionFactorsFile) {
		this.detailedWarmEmissionFactorsFile = detailedWarmEmissionFactorsFile;
	}
	@StringGetter(EMISSION_FACTORS_WARM_FILE_DETAILED)
	public String getDetailedWarmEmissionFactorsFile() {
		return this.detailedWarmEmissionFactorsFile;
	}
	@StringSetter(EMISSION_FACTORS_COLD_FILE_DETAILED)
	public void setDetailedColdEmissionFactorsFile(String detailedColdEmissionFactorsFile) {
		this.detailedColdEmissionFactorsFile = detailedColdEmissionFactorsFile;
	}
	@StringGetter(EMISSION_FACTORS_COLD_FILE_DETAILED)
	public String getDetailedColdEmissionFactorsFile(){
		return this.detailedColdEmissionFactorsFile;
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
}
