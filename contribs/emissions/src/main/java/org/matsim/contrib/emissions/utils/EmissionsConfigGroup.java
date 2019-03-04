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

import com.sun.org.apache.bcel.internal.classfile.Unknown;
import org.matsim.contrib.emissions.EmissionUtils;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public final class EmissionsConfigGroup
extends ReflectiveConfigGroup
{
	public static final String GROUP_NAME = "emissions";
	
	@Deprecated // See elsewhere in this class.  kai, oct'18
	private static final String EMISSION_ROADTYPE_MAPPING_FILE = "emissionRoadTypeMappingFile";
	@Deprecated // kai, oct'18
	private String emissionRoadTypeMappingFile = null;

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
	
	private static final String WRITING_EMISSIONS_EVENTS = "isWritingEmissionsEvents";
	private boolean isWritingEmissionsEvents = true;

	private static final String EMISSION_EFFICIENCY_FACTOR = "emissionEfficiencyFactor";
	private double emissionEfficiencyFactor = 1.0;
	
	@Deprecated // kai, oct'18
	private static final String EMISSION_COST_MULTIPLICATION_FACTOR = "emissionCostMultiplicationFactor";
	@Deprecated // kai, oct'18
	private double emissionCostMultiplicationFactor = 1.0;
	
	@Deprecated // kai, oct'18
	private static final String CONSIDERING_CO2_COSTS = "consideringCO2Costs";
	@Deprecated // kai, oct'18
	private boolean consideringCO2Costs = false;

	private static final String HANDLE_HIGH_AVERAGE_SPEEDS = "handleHighAverageSpeeds";
	private boolean handleHighAverageSpeeds = false;
	
	@Deprecated // See elsewhere in this class.  kai, oct'18
	public enum HbefaRoadTypeSource { fromFile, fromLinkAttributes, fromOsm }
	@Deprecated // See elsewhere in this class.  kai, oct'18
	private static final String Hbefa_ROADTYPE_SOURCE = "hbefaRoadTypeSource";
	@Deprecated // my preference would be to phase out the "fromFile" option and use "fromLinkAttributes" only.  It can always be solved after reading the network.  kai, oct'18
	private HbefaRoadTypeSource hbefaRoadTypeSource = HbefaRoadTypeSource.fromFile; // fromFile is to support backward compatibility

	public enum NonScenarioVehicles { ignore, abort }
	private static final String NON_SCENARIO_VEHICLES = "nonScenarioVehicles";
	private NonScenarioVehicles nonScenarioVehicles = NonScenarioVehicles.abort;

	public enum EmissionsComputationMethod {StopAndGoFraction,AverageSpeed}
	private static final String EMISSIONS_COMPUTATION_METHOD = "emissionsComputationMethod";
	private EmissionsComputationMethod emissionsComputationMethod = EmissionsComputationMethod.StopAndGoFraction;
	
	@Deprecated // should be phased out.  kai, oct'18
	private static final String EMISSION_ROADTYPE_MAPPING_FILE_CMT = "REQUIRED if source of the HBEFA road type is set to "+HbefaRoadTypeSource.fromFile +". It maps from input road types to HBEFA 3.1 road type strings";
	private static final String EMISSION_FACTORS_WARM_FILE_AVERAGE_CMT = "REQUIRED: file with HBEFA 3.1 fleet average warm emission factors";
	private static final String EMISSION_FACTORS_COLD_FILE_AVERAGE_CMT = "REQUIRED: file with HBEFA 3.1 fleet average cold emission factors";
	private static final String USING_DETAILED_EMISSION_CALCULATION_CMT = "if true then detailed emission factor files must be provided!";
	private static final String EMISSION_FACTORS_WARM_FILE_DETAILED_CMT = "OPTIONAL: file with HBEFA 3.1 detailed warm emission factors";
	private static final String EMISSION_FACTORS_COLD_FILE_DETAILED_CMT = "OPTIONAL: file with HBEFA 3.1 detailed cold emission factors";
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

	// yyyy the EmissionsSpecificationMarker thing should be replaced by link attributes.  Did not exist when this functionality was written.  kai, oct'18

	private static final String WRITING_EMISSIONS_EVENTS_CMT = "if false, emission events will not appear in the events file.";

	private static final String EMISSION_EFFICIENCY_FACTOR_CMT = "A factor to include efficiency of the vehicles; all warn emissions are multiplied with this factor; the factor is applied to the whole fleet. ";
	@Deprecated
	private static final String EMISSION_COST_MULTIPLICATION_FACTOR_CMT = "A factor, by which the emission cost factors from literature (Maibach et al. (2008)) are increased.";
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

		map.put(USING_DETAILED_EMISSION_CALCULATION, USING_DETAILED_EMISSION_CALCULATION_CMT);

		map.put(EMISSION_FACTORS_WARM_FILE_DETAILED, EMISSION_FACTORS_WARM_FILE_DETAILED_CMT) ;

		map.put(EMISSION_FACTORS_COLD_FILE_DETAILED, EMISSION_FACTORS_COLD_FILE_DETAILED_CMT);

		map.put(USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION, USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION_CMT);

		map.put(WRITING_EMISSIONS_EVENTS, WRITING_EMISSIONS_EVENTS_CMT);

		map.put(EMISSION_EFFICIENCY_FACTOR, EMISSION_EFFICIENCY_FACTOR_CMT);

		map.put(EMISSION_COST_MULTIPLICATION_FACTOR, EMISSION_COST_MULTIPLICATION_FACTOR_CMT);

		map.put(CONSIDERING_CO2_COSTS, CONSIDERING_CO2_COSTS_CMT);

		map.put(HANDLE_HIGH_AVERAGE_SPEEDS, HANDLE_HIGH_AVERAGE_SPEEDS_CMT);
		
		map.put(NON_SCENARIO_VEHICLES, NON_SCENARIO_VEHICLES_CMT);

        map.put(EMISSIONS_COMPUTATION_METHOD, EMISSIONS_COMPUTATION_METHOD_CMT);

		return map;
	}

	/**
	 * @param roadTypeMappingFile -- {@value #EMISSION_ROADTYPE_MAPPING_FILE_CMT}
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

	@StringGetter(USING_DETAILED_EMISSION_CALCULATION)
	public boolean isUsingDetailedEmissionCalculation(){
		return this.isUsingDetailedEmissionCalculation;
	}
	/**
	 * @param isUsingDetailedEmissionCalculation -- {@value #USING_DETAILED_EMISSION_CALCULATION_CMT}
	 */
	@StringSetter(USING_DETAILED_EMISSION_CALCULATION)
	public void setUsingDetailedEmissionCalculation(final boolean isUsingDetailedEmissionCalculation) {
		this.isUsingDetailedEmissionCalculation = isUsingDetailedEmissionCalculation;
	}
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
	@Deprecated // kai, oct'18
	private static final String USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION = "isUsingVehicleTypeIdAsVehicleDescription";
	@StringGetter(USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION)
	@Deprecated // is there for backwards compatibility; should eventually be removed.  kai, oct'18
	public Boolean isUsingVehicleTypeIdAsVehicleDescription() {
		switch ( this.getHbefaVehicleDescriptionSource() ) {
			case usingVehicleTypeId:
				return true ;
			case fromVehicleTypeDescription:
				return false ;
			case asVehicleTypeAttribute:
				return null ;
			default:
				throw new RuntimeException( "config switch setting not understood" ) ;
		}
	}
	/**
	 * @param usingVehicleIdAsVehicleDescription -- {@value #USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION_CMT}
	 */
	@StringSetter(USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION)
	@Deprecated // is there for backwards compatibility; should eventually be removed.  kai, oct'18
	public void setUsingVehicleTypeIdAsVehicleDescription(Boolean usingVehicleIdAsVehicleDescription) {
		if ( usingVehicleIdAsVehicleDescription==null ) {
			this.setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource.asVehicleTypeAttribute );
		} else if ( usingVehicleIdAsVehicleDescription ) {
			this.setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource.usingVehicleTypeId );
		} else {
			this.setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource.fromVehicleTypeDescription );
		}
	}
	// ============================================
	// ============================================
	// yy I now think that one can get away without the following.  kai, mar'19
//	private static final String HBEFA_VEHICLE_DESCRIPTION_SOURCE="hbefaVehicleDescriptionSource" ;
	private enum HbefaVehicleDescriptionSource { usingVehicleTypeId, fromVehicleTypeDescription, asVehicleTypeAttribute }
	private HbefaVehicleDescriptionSource hbefaVehicleDescriptionSource = HbefaVehicleDescriptionSource.fromVehicleTypeDescription ;
	@Deprecated // is there for backwards compatibility; should eventually be removed.  kai, mar'19
//	@StringSetter(HBEFA_VEHICLE_DESCRIPTION_SOURCE)
	private void setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource hbefaVehicleDescriptionSource ) {
		this.hbefaVehicleDescriptionSource = hbefaVehicleDescriptionSource ;
	}
	@Deprecated // is there for backwards compatibility; should eventually be removed.  kai, mar'19
//	@StringGetter( HBEFA_VEHICLE_DESCRIPTION_SOURCE )
	private HbefaVehicleDescriptionSource getHbefaVehicleDescriptionSource() {
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
	// ---
	/**
	 * @return {@value #EMISSION_EFFICIENCY_FACTOR_CMT}
	 */
	@StringGetter(EMISSION_EFFICIENCY_FACTOR)
	public double getEmissionEfficiencyFactor() {
		return emissionEfficiencyFactor;
	}
	/**
	 * @param emissionEfficiencyFactor -- {@value #EMISSION_EFFICIENCY_FACTOR_CMT}
	 */
	@StringSetter(EMISSION_EFFICIENCY_FACTOR)
	public void setEmissionEfficiencyFactor(double emissionEfficiencyFactor) {
		this.emissionEfficiencyFactor = emissionEfficiencyFactor;
	}
	// ---
//	@StringGetter(EMISSION_COST_MULTIPLICATION_FACTOR)
	// not used in contrib itself --> does not belong here; disable xml functionality and set deprecated in code.  kai, oct'18
	@Deprecated // kai, oct'18
	public double getEmissionCostMultiplicationFactor() {
		return emissionCostMultiplicationFactor;
	}
	/**
	 * @param emissionCostMultiplicationFactor -- {@value #EMISSION_COST_MULTIPLICATION_FACTOR_CMT}
	 */
//	@StringSetter(EMISSION_COST_MULTIPLICATION_FACTOR)
	// not used in contrib itself --> does not belong here; disable xml functionality and set deprecated in code.  kai, oct'18
	@Deprecated // kai, oct'18
	public void setEmissionCostMultiplicationFactor(double emissionCostMultiplicationFactor) {
		this.emissionCostMultiplicationFactor = emissionCostMultiplicationFactor;
	}
	// ---
	// 	@StringGetter(CONSIDERING_CO2_COSTS)
	// not used in contrib itself --> does not belong here; disable xml functionality and set deprecated in code.  kai, oct'18
	@Deprecated // kai, oct'18
	public boolean isConsideringCO2Costs() {
		return consideringCO2Costs;
	}
	/**
	 * @param consideringCO2Costs -- {@value #CONSIDERING_CO2_COSTS_CMT}
	 */
	//	@StringSetter(CONSIDERING_CO2_COSTS)
	// not used in contrib itself --> does not belong here; disable xml functionality and set deprecated in code.  kai, oct'18
	@Deprecated // kai, oct'18
	public void setConsideringCO2Costs(boolean consideringCO2Costs) {
		this.consideringCO2Costs = consideringCO2Costs;
	}

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
	@StringGetter(Hbefa_ROADTYPE_SOURCE)
	@Deprecated // kai, oct'18
	public HbefaRoadTypeSource getHbefaRoadTypeSource() {
		return hbefaRoadTypeSource;
	}

	@StringSetter(Hbefa_ROADTYPE_SOURCE)
	@Deprecated // kai, oct'18
	public void setHbefaRoadTypeSource(HbefaRoadTypeSource hbefaRoadTypeSource) {
		this.hbefaRoadTypeSource = hbefaRoadTypeSource;
	}

	@StringGetter(NON_SCENARIO_VEHICLES)
	public NonScenarioVehicles getNonScenarioVehicles() {
		return nonScenarioVehicles;
	}

	@StringSetter(NON_SCENARIO_VEHICLES)
	public void setNonScenarioVehicles(NonScenarioVehicles nonScenarioVehicles) {
		this.nonScenarioVehicles = nonScenarioVehicles;
	}

    @StringGetter(EMISSIONS_COMPUTATION_METHOD)
    public EmissionsComputationMethod getEmissionsComputationMethod() {
        return emissionsComputationMethod;
    }

    @StringSetter(EMISSIONS_COMPUTATION_METHOD)
    public void setEmissionsComputationMethod(EmissionsComputationMethod emissionsComputationMethod) {
        this.emissionsComputationMethod = emissionsComputationMethod;
    }

}
