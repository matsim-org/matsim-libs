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
import org.matsim.core.trafficmonitoring.AveragingTravelTimeGetter;
import org.matsim.core.utils.misc.Time;

interface ConfigKey {}

/**
 * config group for experimental parameters. this group and its parameters should not be used outside of vsp.
 * @author dgrether
 * @author nagel
 */
public class VspExperimentalConfigGroup extends ReflectiveModule {

	// !! the below cannot be renamed ... since they are at the same time the config file keys !!
	public static enum VspExperimentalConfigKey implements ConfigKey {
		//			activityDurationInterpretation,
		vspDefaultsCheckingLevel,
		logitScaleParamForPlansRemoval,
		scoreMSAStartsAtIteration,
		isGeneratingBoardingDeniedEvent,
		isAbleToOverwritePtInteractionParams,
		isUsingOpportunityCostOfTimeForLocationChoice
	}
	// !! the above cannot be renamed ... since they are at the same time the config file keys !!

	private final Map<ConfigKey,String> typedParam = new TreeMap<ConfigKey,String>();

	public void addParam( final ConfigKey key, final String value ) {
		String retVal = this.typedParam.put( key,value );
		if ( retVal != null ) {
			Logger.getLogger(this.getClass()).info(key + ": replacing >" + retVal + "< (old) with >" + value + "< (new)") ;
		}
	}

	public String getValue( final ConfigKey key ) {
		return this.typedParam.get(key) ;
	}

	// === testing area end ===

	private final static Logger log = Logger.getLogger(VspExperimentalConfigGroup.class);

	public static final String GROUP_NAME = "vspExperimental";

	// ---
	private static final String REMOVING_UNNECESSARY_PLAN_ATTRIBUTES = "removingUnnecessaryPlanAttributes" ;
	private boolean removingUnneccessaryPlanAttributes = false ;

	// ---

	@Deprecated
	private static final String USE_ACTIVITY_DURATIONS = "useActivityDurations";
	private static final String ACTIVITY_DURATION_INTERPRETATION="activityDurationInterpretation" ;

	public static enum ActivityDurationInterpretation { minOfDurationAndEndTime, tryEndTimeThenDuration, @Deprecated endTimeOnly } 

	private ActivityDurationInterpretation activityDurationInterpretation = ActivityDurationInterpretation.minOfDurationAndEndTime ;
//	private ActivityDurationInterpretation activityDurationInterpretation = ActivityDurationInterpretation.tryEndTimeThenDuration ;
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

	@Deprecated
	private static final String USING_OPPORTUNITY_COST_OF_TIME_FOR_PT_ROUTING =
		"usingOpportunityCostOfTimeForPtRouting" ;
	@Deprecated
	private boolean isUsingOpportunityCostOfTimeInPtRouting = true ;

	// ---

	//	private static final String VSP_DEFAULTS_CHECKING_LEVEL = "vspDefaultsCheckingLevel" ;

	public static final String IGNORE = "ignore" ;
	public static final String WARN = "warn" ;
	public static final String ABORT = "abort" ;

	//	private String vspDefaultsCheckingLevel = IGNORE ;

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

		// the following somewhat curious syntax is so that both the compiler and the runtime system notice if an entry
		// is missing
		for ( VspExperimentalConfigKey key : VspExperimentalConfigKey.values() ) {
			switch(key) {
			case vspDefaultsCheckingLevel:
				this.addParam( key, IGNORE ) ;
				break ;
			case logitScaleParamForPlansRemoval:
				this.addParam( key, "1." ) ; 
				break;
			case scoreMSAStartsAtIteration:
				this.addParam( key, "null") ;
				break;
			case isGeneratingBoardingDeniedEvent:
				this.addParam( key, "false" ) ; // default is that this event is NOT generated.  kai, oct'12 
				break;
			case isAbleToOverwritePtInteractionParams:
				this.addParam( key, "false" ) ; // default is that this NOT allowed.  kai, nov'12 
				break;
			case isUsingOpportunityCostOfTimeForLocationChoice:
				this.addParam( key, "true" ) ; 
				break;
			}
		}
	}

	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();

		for ( VspExperimentalConfigKey key : VspExperimentalConfigKey.values() ) {
			switch(key) {
			case logitScaleParamForPlansRemoval:
				//				map.put(key.toString(), "comment") ;
				break;
			case scoreMSAStartsAtIteration:
				map.put(key.toString(), "first iteration of MSA score averaging. The matsim theory department " +
				"suggests to use this together with switching of choice set innovation, but it has not been tested yet.") ;
				break;
			case vspDefaultsCheckingLevel:
				break;
			case isGeneratingBoardingDeniedEvent:
				break;
			case isAbleToOverwritePtInteractionParams:
				map.put(key.toString(), "(do not use except of you have to) There was a problem with pt interaction scoring.  Some people solved it by overwriting the " +
						"parameters of the pt interaction activity type.  Doing this now throws an Exception.  If you still insist on doing this, " +
				"set the following to true.") ;
				break;
			case isUsingOpportunityCostOfTimeForLocationChoice:
				map.put(key.toString(), "if an approximation of the opportunity cost of time is included into the radius calculation for location choice." +
				"`true' will be faster, but it is an approximation.  Default is `true'; `false' is available for backwards compatibility.") ;
				break;
			}
		}
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

		map.put(VspExperimentalConfigKey.vspDefaultsCheckingLevel.toString(), 
				"Options: `"+IGNORE+"', `"+WARN+"', `"+ABORT+"'.  Default: either `"+IGNORE+"' or `"
				+WARN+"'.\n\t\t" +
				"When violating VSP defaults, this results in " +
		"nothing, warnings, or aborts.  Members of VSP should use `abort' or talk to kai.") ;

		//		map.put(USE_ACTIVITY_DURATIONS, "(deprecated, use " + ACTIVITY_DURATION_INTERPRETATION
		//				+ " instead) Set this flag to false if the duration attribute of the activity should not be considered in QueueSimulation");

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

		//		map.put(OFFSET_WALK, "(deprecated, use corresponding option in planCalcScore) " +
		//		"set offset for mode \"walk\" in leg scoring function");

		//		map.put(COLORING, "coloring scheme for otfvis.  Currently (2010) allowed values: ``standard'', ``bvg''") ;
		map.put(USING_OPPORTUNITY_COST_OF_TIME_FOR_PT_ROUTING,
				"indicates if, for routing, the opportunity cost of time should be added to the mode-specific marginal " +
				"utilities of time.\n\t\t" +
				"Default is true; false is possible only for backwards compatibility.\n\t\t" +
				"This is only a suggestion since there is (by matsim design) no way to enforce that mental modules " +
		"obey this." ) ;

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
	public String getActivityDurationInterpretation() {
		return this.activityDurationInterpretation.toString();
	}
	@StringSetter(ACTIVITY_DURATION_INTERPRETATION)
	public void setActivityDurationInterpretation(final ActivityDurationInterpretation activityDurationInterpretation) {
		if ( ActivityDurationInterpretation.endTimeOnly.equals(activityDurationInterpretation) ){
			/*
			 * I don't think this is the correct place for consistency checks but this bug is so hard to find that the user should be warned in any case. dg 08-2012
			 */
			log.warn("You are using " + activityDurationInterpretation + " as activityDurationInterpretation. " +
			"This is not working in conjunction with the pt module as pt interaction activities then will never end!");
			log.warn("ActivityDurationInterpreation " + activityDurationInterpretation + " is deprecated; use " 
					+ ActivityDurationInterpretation.minOfDurationAndEndTime + " instead. kai, jan'13") ;
		}
		this.activityDurationInterpretation = activityDurationInterpretation;
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
	@StringSetter(USING_DETAILED_EMISSION_CALCULATION)
	public boolean isUsingDetailedEmissionCalculation(){
		return this.isUsingDetailedEmissionCalculation;
	}
	@StringGetter(USING_DETAILED_EMISSION_CALCULATION)
	public void setIsUsingDetailedEmissionCalculation(final boolean isUsingDetailedEmissionCalculation) {
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
	@StringSetter(WRITING_OUTPUT_EVENTS)
	public boolean isWritingOutputEvents() {
		return this.writingOutputEvents ;
	}
	@StringGetter(WRITING_OUTPUT_EVENTS)
	public void setWritingOutputEvents(boolean writingOutputEvents) {
		this.writingOutputEvents = writingOutputEvents;
	}
	@StringSetter(MATSIM_GLOBAL_TIME_FORMAT)
	public String getMatsimGlobalTimeFormat() {
		return this.matsimGlobalTimeFormat;
	}
	@StringGetter(MATSIM_GLOBAL_TIME_FORMAT)
	public void setMatsimGlobalTimeFormat(String format) {
		this.matsimGlobalTimeFormat = format;
		Time.setDefaultTimeFormat(format) ;
	}
}
