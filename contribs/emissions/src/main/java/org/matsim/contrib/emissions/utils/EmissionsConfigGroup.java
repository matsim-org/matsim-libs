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

import java.net.URL;
import java.util.Map;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public class EmissionsConfigGroup
extends ReflectiveConfigGroup
{
	public static final String GROUP_NAME = "emissions";

	private static final String EMISSION_ROADTYPE_MAPPING_FILE = "emissionRoadTypeMappingFile";
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

	private static final String USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION = "isUsingVehicleTypeIdAsVehicleDescription";
	private boolean isUsingVehicleIdAsVehicleDescription = false;

	private static final String IGNORING_EMISSIONS_FROM_EVENTS_FILE = "isIgnoringEmissionsFromEventsFile";
	private boolean isIgnoringEmissionsFromEventsFile = false;

	private static final String EMISSION_EFFICIENCY_FACTOR = "emissionEfficiencyFactor";
	private double emissionEfficiencyFactor = 1.0;

	static final String EMISSION_ROADTYPE_MAPPING_FILE_CMT = "REQUIRED: mapping from input road types to HBEFA 3.1 road type strings";
	static final String EMISSION_FACTORS_WARM_FILE_AVERAGE_CMT = "REQUIRED: file with HBEFA 3.1 fleet average warm emission factors";
	static final String EMISSION_FACTORS_COLD_FILE_AVERAGE_CMT = "REQUIRED: file with HBEFA 3.1 fleet average cold emission factors";
	static final String USING_DETAILED_EMISSION_CALCULATION_CMT = "if true then detailed emission factor files must be provided!";
	static final String EMISSION_FACTORS_WARM_FILE_DETAILED_CMT = "OPTIONAL: file with HBEFA 3.1 detailed warm emission factors";
	static final String EMISSION_FACTORS_COLD_FILE_DETAILED_CMT = "OPTIONAL: file with HBEFA 3.1 detailed cold emission factors";
	static final String USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION_CMT = "The vehicle information (or vehicles file) should be passed to the scenario." +
			"The definition of emission specifications:" +  "\n\t\t" +
			" - REQUIRED: it must start with the respective HbefaVehicleCategory followed by `;'" + "\n\t\t" +
			" - OPTIONAL: if detailed emission calculation is switched on, the emission specifications should aditionally contain" +
			" HbefaVehicleAttributes (`Technology;SizeClasse;EmConcept'), corresponding to the strings in " + EMISSION_FACTORS_WARM_FILE_DETAILED+"."+
			"\n" +
			"TRUE: for backward compatibility; vehicle type id is used for the emission specifications. " + "\n"+
			"FALSE: vehicle description is used for the emission specifications." +
			"The emission specifications of a vehicle type should be surrounded by emission specification markers i.e."+
			EmissionSpecificationMarker.BEGIN_EMISSIONS + " and " + EmissionSpecificationMarker.END_EMISSIONS + "." ;

	static final String IGNORING_EMISSIONS_FROM_EVENTS_FILE_CMT = "if true, emission events will not appear in the events file.";

	static final String EMISSION_EFFICIENCY_FACTOR_CMT = "A factor to include efficiency of the vehicles; the factor is applied to the whole fleet. ";

	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();


		map.put(EMISSION_ROADTYPE_MAPPING_FILE, EMISSION_ROADTYPE_MAPPING_FILE_CMT);

		map.put(EMISSION_FACTORS_WARM_FILE_AVERAGE, EMISSION_FACTORS_WARM_FILE_AVERAGE_CMT);

		map.put(EMISSION_FACTORS_COLD_FILE_AVERAGE, EMISSION_FACTORS_COLD_FILE_AVERAGE_CMT);

		map.put(USING_DETAILED_EMISSION_CALCULATION, USING_DETAILED_EMISSION_CALCULATION_CMT);

		map.put(EMISSION_FACTORS_WARM_FILE_DETAILED, EMISSION_FACTORS_WARM_FILE_DETAILED_CMT) ;

		map.put(EMISSION_FACTORS_COLD_FILE_DETAILED, EMISSION_FACTORS_COLD_FILE_DETAILED_CMT);

		map.put(USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION, USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION_CMT);

		map.put(IGNORING_EMISSIONS_FROM_EVENTS_FILE, IGNORING_EMISSIONS_FROM_EVENTS_FILE_CMT);

		return map;
	}

	/**
	 * @param roadTypeMappingFile -- {@value #EMISSION_ROADTYPE_MAPPING_FILE_CMT}
	 */
	@StringSetter(EMISSION_ROADTYPE_MAPPING_FILE)
	public void setEmissionRoadTypeMappingFile(String roadTypeMappingFile) {
		this.emissionRoadTypeMappingFile = roadTypeMappingFile;
	}
	@StringGetter(EMISSION_ROADTYPE_MAPPING_FILE)
	public String getEmissionRoadTypeMappingFile() {
		return this.emissionRoadTypeMappingFile;
	}

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

	@StringGetter(USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION)
	public boolean isUsingVehicleTypeIdAsVehicleDescription() {
		return isUsingVehicleIdAsVehicleDescription;
	}

	/**
	 * @param usingVehicleIdAsVehicleDescription -- {@value #USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION_CMT}
	 */
	@StringSetter(USING_VEHICLE_TYPE_ID_AS_VEHICLE_DESCRIPTION)
	public void setUsingVehicleTypeIdAsVehicleDescription(boolean usingVehicleIdAsVehicleDescription) {
		isUsingVehicleIdAsVehicleDescription = usingVehicleIdAsVehicleDescription;
	}

	@StringGetter(IGNORING_EMISSIONS_FROM_EVENTS_FILE)
	public boolean isIgnoringEmissionsFromEventsFile() {
		return isIgnoringEmissionsFromEventsFile;
	}

	/**
	 * @param ignoringEmissionsFromEventsFile -- {@value #IGNORING_EMISSIONS_FROM_EVENTS_FILE_CMT}
	 */
	@StringSetter(IGNORING_EMISSIONS_FROM_EVENTS_FILE)
	public void setIgnoringEmissionsFromEventsFile(boolean ignoringEmissionsFromEventsFile) {
		isIgnoringEmissionsFromEventsFile = ignoringEmissionsFromEventsFile;
	}

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
}
