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

public class PoznanDemandGenerator
{
    public static void main(String[] args)
            throws ConfigurationException, IOException, SAXException, ParserConfigurationException
        {
            String dirName = "D:\\PP-rad\\taxi\\poznan\\";
            String networkFileName = dirName + "network.xml";
            String zonesXMLFileName = dirName + "zones.xml";
            String zonesShpFileName = dirName + "GIS\\zones_with_no_zone.SHP";
            String odMatrixFileName = dirName + "odMatrix.dat";
            String plansFileName = dirName + "plans.xml";
            String idField = "NO";

            double hours = 2;
            double flowCoeff = 0.1;
            double taxiProbability = 0.05;

            ODDemandGenerator dg = new ODDemandGenerator(networkFileName, zonesXMLFileName,
                    zonesShpFileName, odMatrixFileName, idField, hours, flowCoeff, taxiProbability);
            dg.generate();
            dg.write(plansFileName);
        }

}
