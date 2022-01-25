/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.emissions.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public final class EmissionsConfigGroup extends ReflectiveConfigGroup {
	private static final Logger log = Logger.getLogger( EmissionsConfigGroup.class );

	public static final String GROUP_NAME = "emissions";

	@Deprecated // See elsewhere in this class.  kai, oct'18
	private static final String EMISSION_ROADTYPE_MAPPING_FILE = "emissionRoadTypeMappingFile";
	@Deprecated // kai, oct'18
	private String emissionRoadTypeMappingFile = null;

	private static final String EMISSION_FACTORS_WARM_FILE_AVERAGE = "averageFleetWarmEmissionFactorsFile";
	private String averageFleetWarmEmissionFactorsFile = null;

	private static final String EMISSION_FACTORS_COLD_FILE_AVERAGE = "averageFleetColdEmissionFactorsFile";
	private String averageFleetColdEmissionFactorsFile = null;

	/**
	 * @deprecated -- use {{@link #DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR}}
	 */
	@Deprecated
	private static final String USING_DETAILED_EMISSION_CALCULATION = "usingDetailedEmissionCalculation";
//	private boolean isUsingDetailedEmissionCalculation = false;

	private static final String EMISSION_FACTORS_WARM_FILE_DETAILED = "detailedWarmEmissionFactorsFile" ;
	private String detailedWarmEmissionFactorsFile = null;

	private static final String EMISSION_FACTORS_COLD_FILE_DETAILED = "detailedColdEmissionFactorsFile";
	private String detailedColdEmissionFactorsFile;

	private static final String WRITING_EMISSIONS_EVENTS = "isWritingEmissionsEvents";
	private boolean isWritingEmissionsEvents = true;

	@Deprecated // see comments at getter/setter
	private static final String EMISSION_EFFICIENCY_FACTOR = "emissionEfficiencyFactor";
	@Deprecated // see comments at getter/setter
	private double emissionEfficiencyFactor = 1.0;

	@Deprecated // kai, oct'18
	private static final String EMISSION_COST_MULTIPLICATION_FACTOR = "emissionCostMultiplicationFactor";
//	@Deprecated // kai, oct'18
//	private double emissionCostMultiplicationFactor = 1.0;

	@Deprecated // kai, oct'18
	private static final String CONSIDERING_CO2_COSTS = "consideringCO2Costs";
//	@Deprecated // kai, oct'18
//	private boolean consideringCO2Costs = false;

	private static final String HANDLE_HIGH_AVERAGE_SPEEDS = "handleHighAverageSpeeds";
	private boolean handleHighAverageSpeeds = false;
	// yyyy should become an enum.  kai, jan'20

//	@Deprecated // See elsewhere in this class.  kai, oct'18
	public enum HbefaRoadTypeSource { fromFile, fromLinkAttributes, fromOsm }
//	@Deprecated // See elsewhere in this class.  kai, oct'18
	private static final String Hbefa_ROADTYPE_SOURCE = "hbefaRoadTypeSource";
//	@Deprecated // my preference would be to phase out the "fromFile" option and use "fromLinkAttributes" only.  It can always be solved after reading the network.  kai, oct'18
	// I am now thinking that it would be more expressive to keep that setting, because it makes users aware of the fact that there needs to be something
	// in the network file.  kai, dec'19
	private HbefaRoadTypeSource hbefaRoadTypeSource = HbefaRoadTypeSource.fromFile; // fromFile is to support backward compatibility

	public enum NonScenarioVehicles { ignore, abort }
	private static final String NON_SCENARIO_VEHICLES = "nonScenarioVehicles";
	private NonScenarioVehicles nonScenarioVehicles = NonScenarioVehicles.abort;

	public enum EmissionsComputationMethod {StopAndGoFraction,AverageSpeed}
	private static final String EMISSIONS_COMPUTATION_METHOD = "emissionsComputationMethod";
	private EmissionsComputationMethod emissionsComputationMethod = EmissionsComputationMethod.AverageSpeed;

	public enum DetailedVsAverageLookupBehavior{onlyTryDetailedElseAbort, tryDetailedThenTechnologyAverageElseAbort, tryDetailedThenTechnologyAverageThenAverageTable, directlyTryAverageTable}
	private static final String DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR = "detailedVsAverageLookupBehavior";
	private DetailedVsAverageLookupBehavior detailedVsAverageLookupBehavior = DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort;

	//This is the first quick fix for the issue https://github.com/matsim-org/matsim-libs/issues/1226.
	// Maybe other (smarter) strategies will be added later. kturner nov'20
	public enum HbefaTableConsistencyCheckingLevel { allCombinations, consistent, none}
	private static final String HBEFA_TABLE_CONSISTENCY_CHECKING_LEVEL = "hbefaTableConsistencyCheckingLevel";
	private HbefaTableConsistencyCheckingLevel hbefaTableConsistencyCheckingLevel = HbefaTableConsistencyCheckingLevel.allCombinations;

	@Deprecated // should be phased out.  kai, oct'18
	private static final String EMISSION_ROADTYPE_MAPPING_FILE_CMT = "REQUIRED if source of the HBEFA road type is set to "+HbefaRoadTypeSource.fromFile +". It maps from input road types to HBEFA 3.1 road type strings";
	private static final String EMISSION_FACTORS_WARM_FILE_AVERAGE_CMT = "file with HBEFA vehicle type specific fleet average warm emission factors";
	private static final String EMISSION_FACTORS_COLD_FILE_AVERAGE_CMT = "file with HBEFA vehicle type specific fleet average cold emission factors";
//	@Deprecated //Use DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR instead
//	private static final String USING_DETAILED_EMISSION_CALCULATION_CMT = "This is now deprecated. Please use " + DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR + " instead to declare if detailed or average tables should be used.";
	private static final String DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR_CMT = "Should the calculation bases on average or detailed emission factors? " + "\n\t\t" +
			DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort.name() + " : try detailed values. Abort if values are not found. Requires DETAILED" +
											      " emission factors. \n\t\t" +
			DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort.name() + " : try detailed values first, if not found try to use " +
											      "semi-detailed values for 'vehicleType,technology,average,average', if then not found abort. Requires DETAILED emission factors. \n\t\t" +
			DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable.name() + "try detailed values first, if not found try to " +
											      "use semi-detailed values for 'vehicleType,technology,average,average', if then not found try lookup in average table. Requires DETAILED and AVERAGE emission factors. \n\t\t" +
			DetailedVsAverageLookupBehavior.directlyTryAverageTable.name() + "only calculate from average table. Requires AVERAGE emission factors. " +
			"Default is " + DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort.name();
	private static final String EMISSION_FACTORS_WARM_FILE_DETAILED_CMT = "file with HBEFA detailed warm emission factors";
	private static final String EMISSION_FACTORS_COLD_FILE_DETAILED_CMT = "file with HBEFA detailed cold emission factors";
	@Deprecated // should be phased out.  kai, oct'18
	private static final String USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION_CMT = "The vehicle information (or vehicles file) should be passed to the scenario." +
															   "The definition of emission specifications:" +  "\n\t\t" +
															   " - REQUIRED: it must start with the respective HbefaVehicleCategory followed by `;'" + "\n\t\t" +
															   " - OPTIONAL: if detailed emission calculation is switched on, the emission specifications should aditionally contain" +
															   " HbefaVehicleAttributes (`Technology;SizeClasse;EmConcept'), corresponding to the strings in " + EMISSION_FACTORS_WARM_FILE_DETAILED+"."+
															   "\n\t\t" +
															   "TRUE (DO NOT USE except for backwards compatibility): vehicle type id is used for the emission specifications.\n\t\t" +
															   "FALSE (DO NOT USE except for backwards compability): vehicle description is used for the emission specifications. The emission specifications of a vehicle " +
															   "type should be surrounded by emission specification markers.\n\t\t" +
															   "do not actively set (or set to null) (default): hbefa vehicle type description comes from attribute in vehicle type." ;

	private static final String HBEFA_TABLE_CONSISTENCY_CHECKING_LEVEL_CMT = "Define on which level the entries in the provided hbefa tables are checked for consistency" + "\n\t\t" +
			HbefaTableConsistencyCheckingLevel.allCombinations.name() + " : check if entries for all combinations of HbefaTrafficSituation, HbefaVehicleCategory, HbefaVehicleAttributes, HbefaComponent. " +
																"are available in the table. It only checks for paramters that are available in the table (e.g. if there is no HGV in the table, it can also pass. \n\t\t" +
			HbefaTableConsistencyCheckingLevel.consistent.name() + " : check if the entries for the two HbefaTrafficSituations 'StopAndGo' and 'FreeFlow' (nov 2020, maybe subject to change) are consistently available in the table. \n\t\t" + //TODO
			HbefaTableConsistencyCheckingLevel.none.name() + " : There is no consistency check. This option is NOT recommended and only for backward capability to inputs from before spring 2020 . \n\t\t" +
			"Default is " + HbefaTableConsistencyCheckingLevel.allCombinations.name();

	// yyyy the EmissionsSpecificationMarker thing should be replaced by link attributes.  Did not exist when this functionality was written.  kai, oct'18

	private static final String WRITING_EMISSIONS_EVENTS_CMT = "if false, emission events will not appear in the events file.";

	private static final String EMISSION_EFFICIENCY_FACTOR_CMT = "A factor to include efficiency of the vehicles; all warn emissions are multiplied with this factor; the factor is applied to the whole fleet. ";
	@Deprecated
	private static final String EMISSION_COST_MULTIPLICATION_FACTOR_CMT = "A factor by which the emission cost factors from literature (Maibach et al. (2008)) are increased.";
	@Deprecated
	private static final String CONSIDERING_CO2_COSTS_CMT = "if true, only flat emissions will be considered irrespective of pricing either flat air pollution or exposure of air pollution.";

	private static final String HANDLE_HIGH_AVERAGE_SPEEDS_CMT = "if true, don't fail when average speed is higher than the link freespeed, but cap it instead.";

	private static final String NON_SCENARIO_VEHICLES_CMT = "Specifies the handling of non-scenario vehicles.  The options are: "
//			+ Arrays.stream(NonScenarioVehicles.values()).map(handling -> " " + handling.toString()).collect(Collectors.joining()) +"."
											    //    https://stackoverflow.com/questions/48300252/getting-stackoverflowerror-while-initializing-a-static-variable .
											    // really ugly compilation error with java8, difficult to find.  kai, nov'18
											    + NonScenarioVehicles.values()
											    + " Should eventually be extended by 'getVehiclesFromMobsim'."
		  ;

	private static final String EMISSIONS_COMPUTATION_METHOD_CMT = "if true, the original fractional method from Hülsmann et al (2011) will be used to calculate emission factors";


	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();


		map.put(EMISSION_ROADTYPE_MAPPING_FILE, EMISSION_ROADTYPE_MAPPING_FILE_CMT);

		{
			String Hbefa_ROADTYPE_SOURCE_CMT = "Source of the HBEFFA road type. The options are:"+ Arrays.stream(HbefaRoadTypeSource.values())
																		   .map(source -> " " + source.toString())
																		   .collect(Collectors.joining()) +"."
//			"\n"+HbefaRoadTypeSource.fromLinkAttributes+" is default i.e. put HBEFA road type directly to the link attributes." // unfortunately not default
				  ;

			map.put(Hbefa_ROADTYPE_SOURCE, Hbefa_ROADTYPE_SOURCE_CMT);
		}


		map.put(EMISSION_FACTORS_WARM_FILE_AVERAGE, EMISSION_FACTORS_WARM_FILE_AVERAGE_CMT);

		map.put(EMISSION_FACTORS_COLD_FILE_AVERAGE, EMISSION_FACTORS_COLD_FILE_AVERAGE_CMT);

//		map.put(USING_DETAILED_EMISSION_CALCULATION, USING_DETAILED_EMISSION_CALCULATION_CMT);	//is deprecated now. This functionality is integrated in DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR.
		map.put(DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR, DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR_CMT);

		map.put(HBEFA_TABLE_CONSISTENCY_CHECKING_LEVEL, HBEFA_TABLE_CONSISTENCY_CHECKING_LEVEL_CMT);

		map.put(EMISSION_FACTORS_WARM_FILE_DETAILED, EMISSION_FACTORS_WARM_FILE_DETAILED_CMT) ;

		map.put(EMISSION_FACTORS_COLD_FILE_DETAILED, EMISSION_FACTORS_COLD_FILE_DETAILED_CMT);

//		map.put(USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION, USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION_CMT);
		map.put(HBEFA_VEHICLE_DESCRIPTION_SOURCE, HBEFA_VEHICLE_DESCRIPTION_SOURCE_CMT) ;

		map.put(WRITING_EMISSIONS_EVENTS, WRITING_EMISSIONS_EVENTS_CMT);

		map.put(EMISSION_EFFICIENCY_FACTOR, EMISSION_EFFICIENCY_FACTOR_CMT);

//		map.put(EMISSION_COST_MULTIPLICATION_FACTOR, EMISSION_COST_MULTIPLICATION_FACTOR_CMT);

//		map.put(CONSIDERING_CO2_COSTS, CONSIDERING_CO2_COSTS_CMT);

		map.put(HANDLE_HIGH_AVERAGE_SPEEDS, HANDLE_HIGH_AVERAGE_SPEEDS_CMT);

		map.put(NON_SCENARIO_VEHICLES, NON_SCENARIO_VEHICLES_CMT);

		map.put(EMISSIONS_COMPUTATION_METHOD, EMISSIONS_COMPUTATION_METHOD_CMT);

		return map;
	}

	/**
	 * @param roadTypeMappingFile -- {@value #EMISSION_ROADTYPE_MAPPING_FILE_CMT}
	 * @noinspection JavadocReference
	 */
	@StringSetter(EMISSION_ROADTYPE_MAPPING_FILE)
	@Deprecated // See elsewhere in this class.  kai, oct'18
	public void setEmissionRoadTypeMappingFile(String roadTypeMappingFile) {
		this.emissionRoadTypeMappingFile = roadTypeMappingFile;
	}
	@StringGetter(EMISSION_ROADTYPE_MAPPING_FILE)
	@Deprecated // See elsewhere in this class.  kai, oct'18
	public String getEmissionRoadTypeMappingFile() {
		return this.emissionRoadTypeMappingFile;
	}

	@Deprecated // See elsewhere in this class.  kai, oct'18
	public URL getEmissionRoadTypeMappingFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, this.emissionRoadTypeMappingFile);
	}

	/**
	 * @param averageFleetWarmEmissionFactorsFile -- {@value #EMISSION_FACTORS_WARM_FILE_AVERAGE_CMT}
	 */
	@StringSetter(EMISSION_FACTORS_WARM_FILE_AVERAGE)
	public void setAverageWarmEmissionFactorsFile(String averageFleetWarmEmissionFactorsFile) {
		this.averageFleetWarmEmissionFactorsFile = averageFleetWarmEmissionFactorsFile;
	}
	@StringGetter(EMISSION_FACTORS_WARM_FILE_AVERAGE)
	public String getAverageWarmEmissionFactorsFile() {
		return this.averageFleetWarmEmissionFactorsFile;
	}

	public URL getAverageWarmEmissionFactorsFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, this.averageFleetWarmEmissionFactorsFile);
	}

	/**
	 * @param averageFleetColdEmissionFactorsFile -- {@value #EMISSION_FACTORS_COLD_FILE_AVERAGE_CMT}
	 */
	@StringSetter(EMISSION_FACTORS_COLD_FILE_AVERAGE)
	public void setAverageColdEmissionFactorsFile(String averageFleetColdEmissionFactorsFile) {
		this.averageFleetColdEmissionFactorsFile = averageFleetColdEmissionFactorsFile;
	}
	@StringGetter(EMISSION_FACTORS_COLD_FILE_AVERAGE)
	public String getAverageColdEmissionFactorsFile() {
		return this.averageFleetColdEmissionFactorsFile;
	}

	public URL getAverageColdEmissionFactorsFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, this.averageFleetColdEmissionFactorsFile);
	}
	// ===============
	private static final String message = "The " + USING_DETAILED_EMISSION_CALCULATION + " switch is now disabled.  ";
	private static final String messageTrue = "Please use <param name=\"detailedVsAverageLookupBehavior\" value=\"tryDetailedThenTechnologyAverageThenAverageTable\" />";
	private static final String messageFalse = "Please use <param name=\"detailedVsAverageLookupBehavior\" value=\"directlyTryAverageTable\" />";
	/** @noinspection MethodMayBeStatic*/ // ---
	@StringGetter(USING_DETAILED_EMISSION_CALCULATION)
	@Deprecated
	public Boolean isUsingDetailedEmissionCalculationStringGetter(){
		log.warn(message);
		log.warn("returning null so that config writer does not abort");
		return null ;
	}
	@StringSetter(USING_DETAILED_EMISSION_CALCULATION)
	public void setUsingDetailedEmissionCalculationStringSetter(final Boolean usingDetailedEmissionCalculation) {
		log.error( message );
		if ( usingDetailedEmissionCalculation==null ){
			log.warn( "null as entry in " + USING_DETAILED_EMISSION_CALCULATION + " has no meaning; ignoring it." );
		} else if ( usingDetailedEmissionCalculation ) {
			log.warn( messageTrue );
		} else {
			log.warn( messageFalse );
		}
		throw new RuntimeException( );
//		log.warn( message + " Will try to retrofit ...");
//		if ( usingDetailedEmissionCalculation==null ){
//			log.warn( "null as entry in " + USING_DETAILED_EMISSION_CALCULATION + " has no meaning; ignoring it." );
//		} else if ( usingDetailedEmissionCalculation ) {
//			this.detailedVsAverageLookupBehavior = DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable;
//		} else {
//			this.detailedVsAverageLookupBehavior = DetailedVsAverageLookupBehavior.directlyTryAverageTable;
//		}
	}
	// ---
	/**
	 * @param detailedVsAverageLookupBehavior -- {@value #DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR_CMT}
	 * @noinspection JavadocReference
	 */
	@StringSetter(DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR)
	public void setDetailedVsAverageLookupBehavior(DetailedVsAverageLookupBehavior detailedVsAverageLookupBehavior) {
		this.detailedVsAverageLookupBehavior = detailedVsAverageLookupBehavior;
	}

	@StringGetter(DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR)
	public DetailedVsAverageLookupBehavior getDetailedVsAverageLookupBehavior() {
		return this.detailedVsAverageLookupBehavior;
	}

	// ---
	/**
	 * @param hbefaTableConsistencyCheckingLevel -- {@value #HBEFA_TABLE_CONSISTENCY_CHECKING_LEVEL}
	 * @noinspection JavadocReference
	 */
	@StringSetter(HBEFA_TABLE_CONSISTENCY_CHECKING_LEVEL)
	public void setHbefaTableConsistencyCheckingLevel(HbefaTableConsistencyCheckingLevel hbefaTableConsistencyCheckingLevel) {
		this.hbefaTableConsistencyCheckingLevel = hbefaTableConsistencyCheckingLevel;
	}

	@StringGetter(HBEFA_TABLE_CONSISTENCY_CHECKING_LEVEL)
	public HbefaTableConsistencyCheckingLevel getHbefaTableConsistencyCheckingLevel() {
		return this.hbefaTableConsistencyCheckingLevel;
	}

	// ===============
	/**
	 * @param detailedWarmEmissionFactorsFile -- {@value #EMISSION_FACTORS_WARM_FILE_DETAILED_CMT}
	 */
	@StringSetter(EMISSION_FACTORS_WARM_FILE_DETAILED)
	public void setDetailedWarmEmissionFactorsFile(String detailedWarmEmissionFactorsFile) {
		this.detailedWarmEmissionFactorsFile = detailedWarmEmissionFactorsFile;
	}
	@StringGetter(EMISSION_FACTORS_WARM_FILE_DETAILED)
	public String getDetailedWarmEmissionFactorsFile() {
		return this.detailedWarmEmissionFactorsFile;
	}

	public URL getDetailedWarmEmissionFactorsFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, this.detailedWarmEmissionFactorsFile);
	}

	/**
	 * @param detailedColdEmissionFactorsFile -- {@value #EMISSION_FACTORS_COLD_FILE_DETAILED_CMT}
	 */
	@StringSetter(EMISSION_FACTORS_COLD_FILE_DETAILED)
	public void setDetailedColdEmissionFactorsFile(String detailedColdEmissionFactorsFile) {
		this.detailedColdEmissionFactorsFile = detailedColdEmissionFactorsFile;
	}
	@StringGetter(EMISSION_FACTORS_COLD_FILE_DETAILED)
	public String getDetailedColdEmissionFactorsFile(){
		return this.detailedColdEmissionFactorsFile;
	}

	public URL getDetailedColdEmissionFactorsFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, this.detailedColdEmissionFactorsFile);
	}

	public EmissionsConfigGroup()
	{
		super(GROUP_NAME);
	}
	// ============================================
	// ============================================
	@Deprecated private static final String USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPT_MSG = "replace Boolean by enum hbefaVehicleDescriptionSource.  true <--> usingVehicleTypeId; false <--> fromVehicleTypeDescription; null <--> asEngineInformationAttributes." ;
	@Deprecated // kai, oct'18
	private static final String USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION = "isUsingVehicleTypeIdAsVehicleDescription";
	@StringGetter(USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION)
	@Deprecated public Boolean isUsingVehicleTypeIdAsVehicleDescription() {
		log.error(USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPT_MSG);
		return null; // otherwise config writer will fail. kai, nov'21
/*
//		switch ( this.getHbefaVehicleDescriptionSource() ) {
//			case usingVehicleTypeId:
//				return true ;
//			case fromVehicleTypeDescription:
//				return false ;
//			case asEngineInformationAttributes:
//				return null ;
//			default:
//				throw new RuntimeException( "config switch setting not understood" ) ;
//		}
*/
	}
	/**
	 * @param usingVehicleIdAsVehicleDescription -- {@value #USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION_CMT}
	 */
	@StringSetter(USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION)
	@Deprecated // is there to tell people what to do.  kai, nov'21
	public void setUsingVehicleTypeIdAsVehicleDescription(Boolean usingVehicleIdAsVehicleDescription) {
		throw new RuntimeException( USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPT_MSG );
/*
//		if ( usingVehicleIdAsVehicleDescription==null ) {
//			this.setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource.asEngineInformationAttributes );
//		} else if ( usingVehicleIdAsVehicleDescription ) {
//			this.setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource.usingVehicleTypeId );
//		} else {
//			this.setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource.fromVehicleTypeDescription );
//		}
*/
	}
	// ---
	// ---
	// yy I now think that one can get away without the following.  kai, mar'19
	private static final String HBEFA_VEHICLE_DESCRIPTION_SOURCE="hbefaVehicleDescriptionSource" ;
	private static final String HBEFA_VEHICLE_DESCRIPTION_SOURCE_CMT="Each vehicle in matsim points to a VehicleType.  For the emissions package to work, " +
													 "each VehicleType needs to contain corresponding information.  This switch " +
													 "determines _where_ in VehicleType that information is contained.  default: " +
													 HbefaVehicleDescriptionSource.asEngineInformationAttributes.name() ;
	public enum HbefaVehicleDescriptionSource { usingVehicleTypeId, fromVehicleTypeDescription, asEngineInformationAttributes }
	private HbefaVehicleDescriptionSource hbefaVehicleDescriptionSource = HbefaVehicleDescriptionSource.asEngineInformationAttributes ;
	//	@Deprecated // is there for backwards compatibility; should eventually be removed.  kai, mar'19
	// I am now thinking that it would be more expressive to keep this setting, because it makes users aware of the fact that there needs to be something
	// in the vehicles file.  kai, dec'19
	@StringSetter(HBEFA_VEHICLE_DESCRIPTION_SOURCE)
	public void setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource hbefaVehicleDescriptionSource ) {
		this.hbefaVehicleDescriptionSource = hbefaVehicleDescriptionSource ;
	}
	//	@Deprecated // is there for backwards compatibility; should eventually be removed.  kai, mar'19
	// I am now thinking that it would be more expressive to keep this setting, because it makes users aware of the fact that there needs to be something
	// in the vehicles file.  kai, dec'19
	@StringGetter( HBEFA_VEHICLE_DESCRIPTION_SOURCE )
	public HbefaVehicleDescriptionSource getHbefaVehicleDescriptionSource() {
		return this.hbefaVehicleDescriptionSource ;
	}
	// ============================================
	// ============================================
	/**
	 * @return {@value #WRITING_EMISSIONS_EVENTS_CMT}
	 */
	@StringGetter(WRITING_EMISSIONS_EVENTS)
	public boolean isWritingEmissionsEvents() {
		return isWritingEmissionsEvents;
	}
	/**
	 * @param writingEmissionsEvents -- {@value #WRITING_EMISSIONS_EVENTS_CMT}
	 */
	@StringSetter(WRITING_EMISSIONS_EVENTS)
	public void setWritingEmissionsEvents(boolean writingEmissionsEvents) {
		isWritingEmissionsEvents = writingEmissionsEvents;
	}
	// ============================================
//	// ============================================
//	/**
//	 * @return {@value #EMISSION_EFFICIENCY_FACTOR_CMT}
//	 *
//	 * @deprecated -- I cannot see a goot use case for this: Since this is not even by vehicle type, it could easily be done in the events file
//	 * postprocessing.  kai, jan'20
//	 */
//	@Deprecated
//	@StringGetter(EMISSION_EFFICIENCY_FACTOR)
//	public double getEmissionEfficiencyFactor() {
//		return emissionEfficiencyFactor;
//	}
//	/**
//	 * @param emissionEfficiencyFactor -- {@value #EMISSION_EFFICIENCY_FACTOR_CMT}
//	 *
//	 * @deprecated -- I cannot see a goot use case for this: Since this is not even by vehicle type, it could easily be done in the events file
//	 * postprocessing.  kai, jan'20
//	 */
//	@Deprecated
//	@StringSetter(EMISSION_EFFICIENCY_FACTOR)
//	public void setEmissionEfficiencyFactor(double emissionEfficiencyFactor) {
//		this.emissionEfficiencyFactor = emissionEfficiencyFactor;
//	}
//	// ============================================
	private static final String COSTS_MSG="Cost calculations are not part of the emissions contrib.  Do not use this config setting.";
	// ============================================
//	@StringGetter(EMISSION_COST_MULTIPLICATION_FACTOR)
	// not used in contrib itself --> does not belong here; disable xml functionality and set deprecated in code.  kai, oct'18
//	@Deprecated // kai, oct'18
//	public double getEmissionCostMultiplicationFactor() {
//		throw new RuntimeException(COSTS_MSG);
//	}
//	/**
//	 * @param emissionCostMultiplicationFactor -- {@value #EMISSION_COST_MULTIPLICATION_FACTOR_CMT}
//	 */
//	@StringSetter(EMISSION_COST_MULTIPLICATION_FACTOR)
	// not used in contrib itself --> does not belong here; disable xml functionality and set deprecated in code.  kai, oct'18
	@Deprecated // kai, oct'18
	public void setEmissionCostMultiplicationFactor(double emissionCostMultiplicationFactor) {
		throw new RuntimeException(COSTS_MSG);
	}
	// ============================================
	// ============================================
	// 	@StringGetter(CONSIDERING_CO2_COSTS)
	// not used in contrib itself --> does not belong here; disable xml functionality and set deprecated in code.  kai, oct'18
//	@Deprecated // kai, oct'18
//	public boolean isConsideringCO2Costs() {
//		throw new RuntimeException(COSTS_MSG);
//	}
//	/**
//	 * @param consideringCO2Costs -- {@value #CONSIDERING_CO2_COSTS_CMT}
//	 */
	//	@StringSetter(CONSIDERING_CO2_COSTS)
	// not used in contrib itself --> does not belong here; disable xml functionality and set deprecated in code.  kai, oct'18
	@Deprecated // kai, oct'18
	public void setConsideringCO2Costs(boolean consideringCO2Costs) {
		throw new RuntimeException(COSTS_MSG);
	}
	// ============================================
	// ============================================
	@StringGetter(HANDLE_HIGH_AVERAGE_SPEEDS)
	public boolean handlesHighAverageSpeeds() {
		return handleHighAverageSpeeds;
	}
	/**
	 * @param handleHighAverageSpeeds -- {@value #HANDLE_HIGH_AVERAGE_SPEEDS_CMT}
	 */
	@StringSetter(HANDLE_HIGH_AVERAGE_SPEEDS)
	public void setHandlesHighAverageSpeeds(boolean handleHighAverageSpeeds) {
		this.handleHighAverageSpeeds = handleHighAverageSpeeds;
	}

	// ============================================
	// ============================================
	@StringGetter(Hbefa_ROADTYPE_SOURCE)
	@Deprecated
	public HbefaRoadTypeSource getHbefaRoadTypeSource() {
		return hbefaRoadTypeSource;
	}

	/**
	 * @param hbefaRoadTypeSource this is ignored. The Enum will be removed in future releases
	 * @deprecated This used to set how the contrib was guessing HBEFA-Road types. The contib now expects the road types
	 * to be set explicitly as link attribute. {@link org.matsim.contrib.emissions.EmissionUtils#setHbefaRoadType(Link, String)}
	 * Also, there are mappers to set these attributes from Osm, Visum-files or by guessing based on the freespeed of links
	 * {@link org.matsim.contrib.emissions.OsmHbefaMapping}, {@link org.matsim.contrib.emissions.VisumHbefaRoadTypeMapping} or
	 * {@link org.matsim.contrib.emissions.VspHbefaRoadTypeMapping}
	 */
	@StringSetter(Hbefa_ROADTYPE_SOURCE)
	@Deprecated
	public void setHbefaRoadTypeSource(HbefaRoadTypeSource hbefaRoadTypeSource) {
		log.warn("This property is deprecated and will be removed soon. The emission contrib expects HbefaRaodTypes to be set as link attributes explicitly");
		this.hbefaRoadTypeSource = hbefaRoadTypeSource;
	}

	// ============================================
	// ============================================
	@StringGetter(NON_SCENARIO_VEHICLES)
	public NonScenarioVehicles getNonScenarioVehicles() {
		return nonScenarioVehicles;
	}
	@StringSetter(NON_SCENARIO_VEHICLES)
	public void setNonScenarioVehicles(NonScenarioVehicles nonScenarioVehicles) {
		this.nonScenarioVehicles = nonScenarioVehicles;
	}
	// ============================================
	// ============================================
	@StringGetter(EMISSIONS_COMPUTATION_METHOD)
	public EmissionsComputationMethod getEmissionsComputationMethod() {
		return emissionsComputationMethod;
	}
	@StringSetter(EMISSIONS_COMPUTATION_METHOD)
	public void setEmissionsComputationMethod(EmissionsComputationMethod emissionsComputationMethod) {
		this.emissionsComputationMethod = emissionsComputationMethod;
	}

	@Override
	protected final void checkConsistency(Config config){
		switch (this.emissionsComputationMethod){
			case StopAndGoFraction:
				log.info("Please note that with setting of emissionsComputationMethod "+ EmissionsComputationMethod.StopAndGoFraction+ "" +
						" the emission factors for both freeFlow and StopAndGo fractions are looked up independently and are " +
						"therefore following the fallback behaviour set in " + DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR +
						" independently. --> Depending on the input, it may be that e.g. for ff the detailed value is taken, while for the stopAndGo part " +
						"a less detailed value is used, because the value with the same level of detail is missing.");
				break;
			case AverageSpeed:
				log.warn("This setting of emissionsComputationMethod. "+ EmissionsComputationMethod.AverageSpeed + " is not covered by many test cases.");
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + this.emissionsComputationMethod);
		}
	}

}
