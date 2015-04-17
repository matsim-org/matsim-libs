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

package playground.michalm.demand.poznan.ptap;

import java.util.Map;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrix;

import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.zone.*;


public class PoznanDemandGeneration
{
    public void generate(String inputDir, String plansFile, String transportMode)
    {
        String networkFile = inputDir + "Matsim_2015_02/pt_network.xml";
        String zonesXmlFile = inputDir + "Matsim_2015_02/zones.xml";
        String zonesShpFile = inputDir + "Osm_2015_02/zones.SHP";

        String demandDir = inputDir + "Visum_2014/demand/";
        String hourlySharesFile = demandDir + "hourly_shares_KZ.txt";
        String bindingsFile = demandDir + "bindings_KZ.txt";

        int randomSeed = RandomUtils.DEFAULT_SEED;
        RandomUtils.reset(randomSeed);

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);
        Map<Id<Zone>, Zone> zones = Zones.readZones(scenario, zonesXmlFile, zonesShpFile);

        ODDemandGenerator dg = new ODDemandGenerator(scenario, zones, false);

        Map<String, double[]> hourlyShares = VisumDemandDataReader
                .readHourlyShares(hourlySharesFile);
        Map<String, Matrix> odMatrices = VisumDemandDataReader.readODMatrices(bindingsFile,
                demandDir, zones);

        if (hourlyShares.size() != odMatrices.size()) {
            throw new RuntimeException();
        }

        for (String key : hourlyShares.keySet()) {
            double[] shares = hourlyShares.get(key);
            Matrix odMatrix = odMatrices.get(key);
            int countBefore = scenario.getPopulation().getPersons().size();

            for (int i = 0; i < shares.length; i++) {
                dg.generateSinglePeriod(odMatrix, key, key, transportMode, i * 3600, 3600,
                        shares[i]);
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
        String plansFile = "d:/PP-rad/poznan/test/plans.xml.gz";
        String transportMode = TransportMode.pt;
        new PoznanDemandGeneration().generate(inputDir, plansFile, transportMode);
    }
}
