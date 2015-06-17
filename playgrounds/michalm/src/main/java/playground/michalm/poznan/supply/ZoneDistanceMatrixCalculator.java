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
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrix;

import playground.michalm.poznan.supply.taxi.PotentialCharger;
import playground.michalm.util.distance.*;
import playground.michalm.util.matrices.MatricesTxtWriter;
import playground.michalm.zone.*;


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

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);

        DistanceCalculator calculator = DistanceCalculators.//
//        BEELINE_DISTANCE_CALCULATOR;
//        matrixFile += "beeline_";

//        crateFreespeedDistanceCalculator(scenario.getNetwork());
//        matrixFile += "shortest_";

        crateFreespeedTimeCalculator(scenario.getNetwork());
        matrixFile += "fastest_";

        Map<Id<Zone>, Zone> zones = Zones.readZones(scenario, zonesXmlFile, zonesShpFile);
        Map<Id<Centroid>, Centroid> centroids = Zones.getCentroids(zones);
        List<PotentialCharger> chargers = Arrays.asList(PotentialCharger.CHARGERS);

        Matrix zone2zone = DistanceMatrixUtils.calculateDistanceMatrix(calculator,
                centroids.values(), centroids.values());
        MatricesTxtWriter.createForSingleMatrix(zone2zone).write(matrixFile + "zone2zone");

        Matrix zone2charger = DistanceMatrixUtils.calculateDistanceMatrix(calculator,
                centroids.values(), chargers);
        MatricesTxtWriter.createForSingleMatrix(zone2charger).write(matrixFile + "zone2charger");

        Matrix charger2zone = DistanceMatrixUtils.calculateDistanceMatrix(calculator, chargers,
                centroids.values());
        MatricesTxtWriter.createForSingleMatrix(charger2zone).write(matrixFile + "charger2zone");
    }
}
