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

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class EmissionsConfigGroup
    extends ReflectiveConfigGroup
{
    public static final String GROUP_NAME = "emissions";

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
    
    @Override
    public Map<String, String> getComments() {
        Map<String,String> map = super.getComments();


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



        return map;
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
    
    public EmissionsConfigGroup()
    {
        super(GROUP_NAME);
    }

}
