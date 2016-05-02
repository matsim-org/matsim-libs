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

package playground.michalm.poznan.demand.ptap;

import java.util.Map;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.contrib.zone.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.matrices.Matrix;

import playground.michalm.demand.ODDemandGenerator;


public class PoznanDemandGeneration
{
    public void generate(String inputDir, String plansFile, String transportMode)
    {
        String networkFile = inputDir + "Matsim_2015_02/only_A/pt_network.xml";
        //        String networkFile = inputDir + "Matsim_2015_02/Poznan_2015_02_05_all.xml";

        String zonesXmlFile = inputDir + "Matsim_2015_02/zones.xml";
        String zonesShpFile = inputDir + "Osm_2015_02/zones.SHP";

        String demandDir = inputDir + "Visum_2014/demand/";
        //        String hourlySharesFile = demandDir + "hourly_shares_KI.txt";
        //        String bindingsFile = demandDir + "bindings_KI-poj.txt";
        String hourlySharesFile = demandDir + "hourly_shares_KZ.txt";
        String activityPairsFile = demandDir + "activity_pairs_KZ.txt";
        String bindingsFile = demandDir + "bindings_KZ.txt";

        int randomSeed = RandomUtils.DEFAULT_SEED;
        RandomUtils.reset(randomSeed);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
        Map<Id<Zone>, Zone> zones = Zones.readZones(zonesXmlFile, zonesShpFile);

        ODDemandGenerator dg = new ODDemandGenerator(scenario, zones, false);

        Map<String, double[]> hourlyShares = VisumDemandDataReader
                .readHourlyShares(hourlySharesFile);

        Map<String, Tuple<String, String>> activityPairs = VisumDemandDataReader
                .readActivityPairs(activityPairsFile);

        Map<String, Matrix> odMatrices = VisumDemandDataReader.readODMatrices(bindingsFile,
                demandDir, zones);

        if (hourlyShares.size() != odMatrices.size()) {
            throw new RuntimeException();
        }

        for (String key : hourlyShares.keySet()) {
            double[] shares = hourlyShares.get(key);
            Tuple<String, String> activityPair = activityPairs.get(key);
            Matrix odMatrix = odMatrices.get(key);
            int countBefore = scenario.getPopulation().getPersons().size();

            for (int i = 0; i < shares.length; i++) {
                dg.generateSinglePeriod(odMatrix, activityPair.getFirst(), activityPair.getSecond(),
                        transportMode, i * 3600, 3600, shares[i]);
            }

            int countAfter = scenario.getPopulation().getPersons().size();
            int delta = countAfter - countBefore;
            System.out.println("#trips generated for " + key + ": " + delta);
        }

        dg.write(plansFile);
    }


    public static void main(String[] args)
    {
        String inputDir = "d:/GoogleDrive/Poznan/";
        //        String plansFile = "d:/PP-rad/poznan/test/pt_plans.xml.gz";
        //        String transportMode = TransportMode.pt;

        String plansFile = "d:/PP-rad/poznan/test/KI-poj_plans.xml.gz";
        String transportMode = TransportMode.car;

        new PoznanDemandGeneration().generate(inputDir, plansFile, transportMode);
    }
}
