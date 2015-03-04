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

package playground.michalm.demand.poznan.kbr;

import java.io.File;
import java.util.Map;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrix;

import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.util.array2d.Array2DReader;
import playground.michalm.util.matrices.MatrixUtils;
import playground.michalm.zone.*;


public class PoznanSimpleDemandGeneration
{
    public static void main(String[] args)
    {
        String dirName = "D:\\eTaxi\\Poznan_MATSim\\";
        String networkFile = dirName + "network.xml";
        String zonesXmlFile = dirName + "zones.xml";
        String zonesShpFile = dirName + "GIS\\zones.SHP";

        String odMatrixFilePrefix = dirName + "odMatrices\\odMatrix";
        String plansFile = dirName + "plans.xml.gz";
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
        Map<Id<Zone>, Zone> zones = Zones.readZones(scenario, zonesXmlFile, zonesShpFile);

        ODDemandGenerator dg = new ODDemandGenerator(scenario, zones, true);

        RandomUtils.reset(randomSeed);

        for (int i = 0; i < 24; i++) {
            String timePeriod = i + "-" + (i + 1);
            System.out.println("Generation for " + timePeriod);
            String odMatrixFile = odMatrixFilePrefix + timePeriod + ".dat";
            double[][] odMatrixFromFile = Array2DReader.getDoubleArray(new File(odMatrixFile),
                    zones.size());
            Matrix odMatrix = MatrixUtils.createSparseMatrix("m" + i, zones.keySet(),
                    odMatrixFromFile);

            dg.generateSinglePeriod(odMatrix, "dummy", "dummy", TransportMode.car, i * 3600, 3600,
                    1);
        }

        dg.write(plansFile);
        // dg.writeTaxiCustomers(taxiFile);
    }
}
