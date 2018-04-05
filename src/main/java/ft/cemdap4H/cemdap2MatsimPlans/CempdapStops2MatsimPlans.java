/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package ft.cemdap4H.cemdap2MatsimPlans;

import java.io.IOException;

/**
 * Created by amit on 22.09.17.
 */

public class CempdapStops2MatsimPlans {


    /**
     * The plan is now:
     * - generate the matsim_plans without any coordinates for each file of cempdap_output (i.e. same process 5 times)
     * - the zone information is added to the activity types e.g. home_510
     * - sample first matsim_plans
     * - take sampled plans, add other plans of the sampled persons from other matsim_plans file and combine them in a file
     * - add the acitivity locations based on CORINE land cover data and zone information
     */

    public static void main(String[] args) throws IOException {
        
    	String inbase = "D:/cemdap-vw/";
        String cemdapDataRoot = inbase+"cemdap_output/";
        for (int i = 1; i<=5;i++){
        	if (i == 4) continue;
        int numberOfFirstCemdapOutputFile = i;
        int numberOfPlans = 1;
        int numberOfPlansFile = i;
        String outputDirectory = cemdapDataRoot+ "/" + numberOfPlansFile + "/";
        String zonalShapeFile1 = inbase+ "add_data/shp/nssa.shp";
        String zoneIdTag1 = "AGS";
        String zonalShapeFile2 = inbase+ "add_data/shp/wvi-zones.shp";
        String zoneIdTag2 = "NO";
        boolean allowVariousWorkAndEducationLocations = true;
        boolean addStayHomePlan = true;
        boolean useLandCoverData = true;
        String landCoverFile = inbase+ "add_data/shp/corine-nssa.shp";
        String stopFile = "Stops.out";
        String activityFile = "Activity.out";
        boolean simplifyGeometries = true;
        boolean assignCoordinatesToActivities = true;
        boolean combiningGeoms = false;

      

        CemdapStops2MatsimPlansConverter.convert(cemdapDataRoot, numberOfFirstCemdapOutputFile, numberOfPlans, outputDirectory,
                zonalShapeFile1, zoneIdTag1, zonalShapeFile2, zoneIdTag2, allowVariousWorkAndEducationLocations, addStayHomePlan,
                useLandCoverData, landCoverFile, stopFile, activityFile,simplifyGeometries, combiningGeoms, assignCoordinatesToActivities);
        
    }
    }
}
