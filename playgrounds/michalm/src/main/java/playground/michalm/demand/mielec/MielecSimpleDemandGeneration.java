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

package playground.michalm.demand.mielec;

import java.io.*;
import java.util.Map;

import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.xml.sax.SAXException;

import pl.poznan.put.util.array2d.*;
import playground.michalm.demand.*;
import playground.michalm.vrp.run.VrpConfigUtils;


public class MielecSimpleDemandGeneration
{
    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        String dirName = "D:\\michalm\\2013_07\\mielec-2-peaks-new\\";
        String networkFile = dirName + "network.xml";
        String zonesXmlFile = dirName + "zones.xml";
        String zonesShpFile = dirName + "GIS\\zones_with_no_zone.SHP";
        String odMatrixFile = dirName + "odMatrix.dat";
        String plansFile = dirName + "plans.xml";
        String idField = "NO";

        String taxiFile = dirName + "taxiCustomers_03_pc.txt";

        // double hours = 2;
        // double flowCoeff = 1;
        // double taxiProbability = 0;

        double hours = 1;
        double[] flowCoeff = { 0.2, 0.4, 0.6, 0.8, 0.6, 0.4, 0.2 };
        double taxiProbability = 0.03;

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);
        Map<Id, Zone> zones = Zone.readZones(scenario, zonesXmlFile, zonesShpFile, idField);

        ActivityGenerator lg = new DefaultActivityGenerator(scenario);
        ODDemandGenerator dg = new ODDemandGenerator(scenario, lg, zones);

        double[][] odMatrix = Array2DReader.getDoubleArray(new File(odMatrixFile), zones.size());
        double[][] odMatrixTransposed = Array2DUtils.transponse(odMatrix);

        double startTime = 6 * 3600;

        // symmetric morning peak
        for (int i = 0; i < flowCoeff.length; i++) {
            dg.generateSinglePeriod(odMatrixTransposed, "dummy", "dummy", hours, flowCoeff[i],
                    taxiProbability, startTime);
            startTime += 3600 * hours;
        }

        // evening peak
        for (int i = 0; i < flowCoeff.length; i++) {
            dg.generateSinglePeriod(odMatrix, "dummy", "dummy", hours, flowCoeff[i],
                    taxiProbability, startTime);
            startTime += 3600 * hours;
        }

        dg.write(plansFile);
        dg.writeTaxiCustomers(taxiFile);
    }
}
