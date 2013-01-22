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

package playground.michalm.demand;

import java.io.IOException;

import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import cern.jet.random.engine.MersenneTwister;


public class PoznanDemandGenerator
{
    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        String dirName = "D:\\eTaxi\\Poznan_MATSim\\";
        String networkFileName = dirName + "network.xml";
        String zonesXMLFileName = dirName + "zones.xml";
        String zonesShpFileName = dirName + "GIS\\zones_with_no_zone.SHP";
        String odMatrixFileNamePrefix = dirName + "odMatrices\\odMatrix";
        String plansFileName = dirName + "plans.xml.gz";
        String idField = "NO";
        int randomSeed = MersenneTwister.DEFAULT_SEED; 

        // String taxiFileName = dirName + "taxiCustomers_07_pc.txt";

        // double hours = 2;
        // double flowCoeff = 1;
        // double taxiProbability = 0;

        // double[] hours = { 1, 1, 1, 1, 1, 1, 1 };
        // double[] flowCoeff = { 0.2, 0.4, 0.8, 1.0, 0.6, 0.4, 0.2 };
        // double[] taxiProbability = { 0, 0, 0, 0, 0, 0, 0 };

        ODDemandGenerator dg = new ODDemandGenerator(networkFileName, zonesXMLFileName,
                zonesShpFileName, idField);
        
        dg.resetRandomEngine(randomSeed);

        for (int i = 0; i < 24; i++) {
            String timePeriod = i + "-" + (i + 1);
            System.out.println("Generation for " + timePeriod);
            String odMatrixFileName = odMatrixFileNamePrefix + timePeriod + ".dat";
            double[][] odMatrix = dg.readODMatrix(odMatrixFileName);
            dg.generateSinglePeriod(odMatrix, 1, 1, 0, i * 3600);
        }

        dg.write(plansFileName);
        // dg.writeTaxiCustomers(taxiFileName);
    }
}
