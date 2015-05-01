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

package playground.michalm.zone.util;

import java.util.Map;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrix;

import playground.michalm.util.matrices.MatricesTxtWriter;
import playground.michalm.zone.*;


public class ZoneDistanceMatrixCalculator
{
    private final Map<Id<Zone>, Zone> zones;


    public ZoneDistanceMatrixCalculator(Map<Id<Zone>, Zone> zones)
    {
        this.zones = zones;
    }


    public Matrix calculateDistanceMatrix(ZoneDistance calculator)
    {
        Matrix matrix = new Matrix("distance", "distance");

        for (Zone fromZone : zones.values()) {
            for (Zone toZone : zones.values()) {
                double distance = calculator.calcDistance(fromZone, toZone);
                matrix.createEntry(fromZone.getId().toString(), toZone.getId().toString(), distance);
            }
        }

        return matrix;
    }


    public static void main(String[] args)
    {
        String inputDir = "d:/GoogleDrive/Poznan/";
        String networkFile = inputDir + "Matsim_2015_02/Poznan_2015_02_05_all.xml";

        String taxiZoneDir = "d:/PP-rad/poznan/poznan-taxi-supply/rejony/";
        String zonesXmlFile = taxiZoneDir + "taxi_zones.xml";
        String zonesShpFile = taxiZoneDir + "taxi_zones.shp";

        String matrixFile = "d:/PP-rad/poznan/test/taxi_zone_distances_"+//
        //"beeline.txt";
        //"shortest.txt";
        "fastest.txt";

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);

        ZoneDistance calculator = ZoneDistances.//
        //BEELINE_DISTANCE_CALCULATOR;
        //crateFreespeedDistanceCalculator(scenario.getNetwork());
        crateFreespeedTimeCalculator(scenario.getNetwork());

        Map<Id<Zone>, Zone> zones = Zones.readZones(scenario, zonesXmlFile, zonesShpFile);
        Matrix distances = new ZoneDistanceMatrixCalculator(zones)
                .calculateDistanceMatrix(calculator);
        MatricesTxtWriter.createForSingleMatrix(distances).write(matrixFile);
    }
}
