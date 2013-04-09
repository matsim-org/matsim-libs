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

package playground.michalm.demand.poznan;

import java.io.*;
import java.util.Map;

import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.xml.sax.SAXException;

import pl.poznan.put.util.array2d.Array2DReader;
import pl.poznan.put.util.random.RandomUtils;
import playground.michalm.demand.*;
import playground.michalm.vrp.run.VrpConfigUtils;


public class PoznanSimpleDemandGeneration
{
    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        String dirName = "D:\\eTaxi\\Poznan_MATSim\\";
        String networkFile = dirName + "network.xml";
        String zonesXmlFile = dirName + "zones.xml";
        String zonesShpFile = dirName + "GIS\\zones_with_no_zone.SHP";

        String odMatrixFilePrefix = dirName + "odMatrices\\odMatrix";
        String plansFile = dirName + "plans.xml.gz";
        String idField = "NO";
        int randomSeed = RandomUtils.DEFAULT_SEED;

        // String taxiFile = dirName + "taxiCustomers_07_pc.txt";

        // double hours = 2;
        // double flowCoeff = 1;
        // double taxiProbability = 0;

        // double[] hours = { 1, 1, 1, 1, 1, 1, 1 };
        // double[] flowCoeff = { 0.2, 0.4, 0.8, 1.0, 0.6, 0.4, 0.2 };
        // double[] taxiProbability = { 0, 0, 0, 0, 0, 0, 0 };

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);
        Map<Id, Zone> zones = Zone.readZones(scenario, zonesXmlFile, zonesShpFile, idField);

        ActivityGenerator lg = new DefaultActivityGenerator(scenario);
        ODDemandGenerator dg = new ODDemandGenerator(scenario, lg, zones);

        RandomUtils.reset(randomSeed);

        for (int i = 0; i < 24; i++) {
            String timePeriod = i + "-" + (i + 1);
            System.out.println("Generation for " + timePeriod);
            String odMatrixFile = odMatrixFilePrefix + timePeriod + ".dat";
            double[][] odMatrix = Array2DReader
                    .getDoubleArray(new File(odMatrixFile), zones.size());
            dg.generateSinglePeriod(odMatrix, "dummy", "dummy", 1, 1, 0, i * 3600);
        }

        dg.write(plansFile);
        // dg.writeTaxiCustomers(taxiFile);
    }
}
