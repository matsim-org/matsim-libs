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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;

public final class EmissionsConfigGroup extends ReflectiveConfigGroup {
	private static final Logger log = LogManager.getLogger( EmissionsConfigGroup.class );

	public static final String GROUP_NAME = "emissions";

	private static final String EMISSION_FACTORS_WARM_FILE_AVERAGE = "averageFleetWarmEmissionFactorsFile";
	private String averageFleetWarmEmissionFactorsFile = null;

	private static final String EMISSION_FACTORS_COLD_FILE_AVERAGE = "averageFleetColdEmissionFactorsFile";
	private String averageFleetColdEmissionFactorsFile = null;

	private static final String EMISSION_FACTORS_WARM_FILE_DETAILED = "detailedWarmEmissionFactorsFile" ;
	private String detailedWarmEmissionFactorsFile = null;

	private static final String EMISSION_FACTORS_COLD_FILE_DETAILED = "detailedColdEmissionFactorsFile";
	private String detailedColdEmissionFactorsFile;

	private static final String WRITING_EMISSIONS_EVENTS = "isWritingEmissionsEvents";
	private boolean isWritingEmissionsEvents = true;

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

	private static final String EMISSION_FACTORS_WARM_FILE_AVERAGE_CMT = "file with HBEFA vehicle type specific fleet average warm emission factors";
	private static final String EMISSION_FACTORS_COLD_FILE_AVERAGE_CMT = "file with HBEFA vehicle type specific fleet average cold emission factors";
	private static final String DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR_CMT = "Should the calculation bases on average or detailed emission factors? " + "\n\t\t\t" +
			DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort.name() + " : try detailed values. Abort if values are not found. Requires DETAILED" +
											      " emission factors. \n\t\t\t" +
			DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort.name() + " : try detailed values first, if not found try to use " +
											      "semi-detailed values for 'vehicleType,technology,average,average', if then not found abort. Requires DETAILED emission factors. \n\t\t\t" +
			DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable.name() + "try detailed values first, if not found try to " +
											      "use semi-detailed values for 'vehicleType,technology,average,average', if then not found try lookup in average table. Requires DETAILED and AVERAGE emission factors. \n\t\t\t" +
			DetailedVsAverageLookupBehavior.directlyTryAverageTable.name() + "only calculate from average table. Requires AVERAGE emission factors. " +
			"Default is " + DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort.name();
	private static final String EMISSION_FACTORS_WARM_FILE_DETAILED_CMT = "file with HBEFA detailed warm emission factors";
	private static final String EMISSION_FACTORS_COLD_FILE_DETAILED_CMT = "file with HBEFA detailed cold emission factors";
	private static final String HBEFA_TABLE_CONSISTENCY_CHECKING_LEVEL_CMT = "Define on which level the entries in the provided hbefa tables are checked for consistency" + "\n\t\t\t" +
			HbefaTableConsistencyCheckingLevel.allCombinations.name() + " : check if entries for all combinations of HbefaTrafficSituation, HbefaVehicleCategory, HbefaVehicleAttributes, HbefaComponent. " +
																"are available in the table. It only checks for parameters that are available in the table (e.g. if there is no HGV in the table, it can also pass. \n\t\t\t" +
			HbefaTableConsistencyCheckingLevel.consistent.name() + " : check if the entries for the two HbefaTrafficSituations 'StopAndGo' and 'FreeFlow' (nov 2020, may be subject to change) are consistently available in the table. \n\t\t\t" + //TODO
			HbefaTableConsistencyCheckingLevel.none.name() + " : There is no consistency check. This option is NOT recommended and only for backward capability to inputs from before spring 2020 . \n\t\t\t" +
			"Default is " + HbefaTableConsistencyCheckingLevel.allCombinations.name();

	private static final String WRITING_EMISSIONS_EVENTS_CMT = "if false, emission events will not appear in the events file.";

	private static final String HANDLE_HIGH_AVERAGE_SPEEDS_CMT = "if true, don't fail when average speed is higher than the link freespeed, but cap it instead.";

	private static final String NON_SCENARIO_VEHICLES_CMT = "Specifies the handling of non-scenario vehicles.  The options are: "
//			+ Arrays.stream(NonScenarioVehicles.values()).map(handling -> " " + handling.toString()).collect(Collectors.joining()) +"."
											    //    https://stackoverflow.com/questions/48300252/getting-stackoverflowerror-while-initializing-a-static-variable .
											    // really ugly compilation error with java8, difficult to find.  kai, nov'18
											    + Arrays.toString( NonScenarioVehicles.values() )
											    + " Should eventually be extended by 'getVehiclesFromMobsim'."
		  ;

	private static final String EMISSIONS_COMPUTATION_METHOD_CMT = "if true, the original fractional method from Hülsmann et al (2011) will be used to calculate emission factors";


	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();

		map.put(EMISSION_FACTORS_WARM_FILE_AVERAGE, EMISSION_FACTORS_WARM_FILE_AVERAGE_CMT);

		map.put(EMISSION_FACTORS_COLD_FILE_AVERAGE, EMISSION_FACTORS_COLD_FILE_AVERAGE_CMT);

		map.put(DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR, DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR_CMT);

		map.put(HBEFA_TABLE_CONSISTENCY_CHECKING_LEVEL, HBEFA_TABLE_CONSISTENCY_CHECKING_LEVEL_CMT);

		map.put(EMISSION_FACTORS_WARM_FILE_DETAILED, EMISSION_FACTORS_WARM_FILE_DETAILED_CMT) ;

		map.put(EMISSION_FACTORS_COLD_FILE_DETAILED, EMISSION_FACTORS_COLD_FILE_DETAILED_CMT);

		map.put(HBEFA_VEHICLE_DESCRIPTION_SOURCE, HBEFA_VEHICLE_DESCRIPTION_SOURCE_CMT) ;

		map.put(WRITING_EMISSIONS_EVENTS, WRITING_EMISSIONS_EVENTS_CMT);

		map.put(HANDLE_HIGH_AVERAGE_SPEEDS, HANDLE_HIGH_AVERAGE_SPEEDS_CMT);

		map.put(NON_SCENARIO_VEHICLES, NON_SCENARIO_VEHICLES_CMT);

		map.put(EMISSIONS_COMPUTATION_METHOD, EMISSIONS_COMPUTATION_METHOD_CMT);

		return map;
	}

	// ===============
	// ===============
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
	// ===============
	// ===============
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

	private static final String messageTrue = "Please use <param name=\"detailedVsAverageLookupBehavior\" value=\"tryDetailedThenTechnologyAverageThenAverageTable\" />";
	private static final String messageFalse = "Please use <param name=\"detailedVsAverageLookupBehavior\" value=\"directlyTryAverageTable\" />";

	// ===============
	// ===============
	/**
	 * @param detailedVsAverageLookupBehavior -- {@value  #DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR_CMT}
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
	// ===============
	// ===============
	/**
	 * @param hbefaTableConsistencyCheckingLevel -- {@value #HBEFA_TABLE_CONSISTENCY_CHECKING_LEVEL}
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
	// ============================================
	private static final String HANDLE_HIGH_AVERAGE_SPEEDS = "handleHighAverageSpeeds";

	private boolean handleHighAverageSpeeds = false;
	// yyyy should become an enum.  kai, jan'20
	@StringGetter(HANDLE_HIGH_AVERAGE_SPEEDS)
	public boolean getHandleHighAverageSpeeds() {
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
	// ============================================
	// ============================================
	@Override
	protected final void checkConsistency(Config config){
		switch( this.emissionsComputationMethod ){
			case StopAndGoFraction -> log.info("Please note that with setting of emissionsComputationMethod {} the emission factors for both freeFlow and StopAndGo fractions are looked up independently and are therefore following the fallback behaviour set in " + DETAILED_VS_AVERAGE_LOOKUP_BEHAVIOR + " independently. --> Depending on the input, it may be that e.g. for ff the detailed value is taken, while for the stopAndGo part a less detailed value is used, because the value with the same level of detail is missing.", EmissionsComputationMethod.StopAndGoFraction);
			case AverageSpeed -> log.warn("This setting of emissionsComputationMethod. {} is not covered by many test cases.", EmissionsComputationMethod.AverageSpeed);
			default -> throw new IllegalStateException( "Unexpected value: " + this.emissionsComputationMethod );
		}
	}

}
