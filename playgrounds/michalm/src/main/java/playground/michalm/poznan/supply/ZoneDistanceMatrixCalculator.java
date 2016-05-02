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

package playground.michalm.poznan.supply;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.util.distance.*;
import org.matsim.contrib.zone.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrix;

import playground.michalm.chargerlocation.*;
import playground.michalm.util.matrices.MatricesTxtWriter;


public class ZoneDistanceMatrixCalculator
{
    public static void main(String[] args)
    {
        String inputDir = "d:/GoogleDrive/Poznan/";
        String networkFile = inputDir + "Matsim_2015_02/Poznan_2015_02_05_all.xml";

        String taxiZoneDir = "d:/PP-rad/poznan/poznan-taxi-supply/rejony/";
        String zonesXmlFile = taxiZoneDir + "taxi_zones.xml";
        String zonesShpFile = taxiZoneDir + "taxi_zones.shp";

        String matrixFile = "d:/PP-rad/poznan/test/distances_";

        ChargerLocation[] LOCATIONS = {
                ChargerLocations.createLocation(1, 629375.526287495, 5807049.09624582, 0),
                ChargerLocations.createLocation(2, 627667.270111601, 5806032.00666041, 0),
                ChargerLocations.createLocation(3, 629858.161036597, 5807986.09770554, 0),
                ChargerLocations.createLocation(4, 627670.750480261, 5808514.98110016, 0),
                ChargerLocations.createLocation(5, 628931.787064244, 5808428.14220098, 0),
                ChargerLocations.createLocation(6, 632665.914282043, 5806912.26666199, 0),
                ChargerLocations.createLocation(7, 631414.841065603, 5808351.76701608, 0),
                ChargerLocations.createLocation(8, 631350.921863240, 5807870.60505818, 0),
                ChargerLocations.createLocation(9, 631357.194136764, 5810714.22475246, 0),
                ChargerLocations.createLocation(10, 631256.667354371, 5811489.93034689, 0),
                ChargerLocations.createLocation(11, 630810.948612502, 5812932.31900434, 0),
                ChargerLocations.createLocation(12, 630740.137462418, 5806467.83836639, 0) //
        };

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

        DistanceCalculator calculator = DistanceCalculators.//
        //        BEELINE_DISTANCE_CALCULATOR;
        //        matrixFile += "beeline_";

        //        crateFreespeedDistanceCalculator(scenario.getNetwork());
        //        matrixFile += "shortest_";

                crateFreespeedTimeCalculator(scenario.getNetwork());
        matrixFile += "fastest_";

        Map<Id<Zone>, Zone> zones = Zones.readZones(zonesXmlFile, zonesShpFile);
        List<ChargerLocation> locations = Arrays.asList(LOCATIONS);

        Matrix zone2zone = DistanceMatrixUtils.calculateDistanceMatrix(calculator, zones.values(),
                zones.values());
        MatricesTxtWriter.createForSingleMatrix(zone2zone).write(matrixFile + "zone2zone");

        Matrix zone2charger = DistanceMatrixUtils.calculateDistanceMatrix(calculator,
                zones.values(), locations);
        MatricesTxtWriter.createForSingleMatrix(zone2charger).write(matrixFile + "zone2charger");

        Matrix charger2zone = DistanceMatrixUtils.calculateDistanceMatrix(calculator, locations,
                zones.values());
        MatricesTxtWriter.createForSingleMatrix(charger2zone).write(matrixFile + "charger2zone");
    }
}
