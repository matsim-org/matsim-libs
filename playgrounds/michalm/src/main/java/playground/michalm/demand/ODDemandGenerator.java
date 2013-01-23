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

import java.io.*;
import java.util.*;

import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.xml.sax.SAXException;

import pl.poznan.put.util.array2d.Array2DReader;
import playground.michalm.demand.Zone.Type;


public class ODDemandGenerator
    extends AbstractDemandGenerator
{
    // private static final Logger log = Logger.getLogger(ODDemandGenerator.class);

    private final List<Person> taxiCustomers = new ArrayList<Person>();


    public ODDemandGenerator(String networkFileName, String zonesXMLFileName,
            String zonesShpFileName, String idField)
        throws IOException, SAXException, ParserConfigurationException
    {
        super(networkFileName, zonesXMLFileName, zonesShpFileName, idField);
    }


    public void generateMultiplePeriods(double[][] odMatrix, double[] hours, double[] flowCoeff,
            double[] taxiProbability)
    {
        double startTime = 0;
        for (int i = 0; i < hours.length; i++) {
            generateSinglePeriod(odMatrix, hours[i], flowCoeff[i], taxiProbability[i], startTime);
            startTime += 3600 * hours[i];
        }
    }


    public void generateMultiplePeriods(double[][][] odMatrix, double[] taxiProbability)
    {
        double startTime = 0;
        for (int i = 0; i < odMatrix.length; i++) {
            generateSinglePeriod(odMatrix[i], 1, 1, taxiProbability[i], startTime);
            startTime += 3600;
        }
    }


    public void generateSinglePeriod(double[][] odMatrix, double hours, double flowCoeff,
            double taxiProbability, double startTime)
    {
        PopulationFactory pf = getPopulationFactory();
        List<Zone> zones = getFileOrderedZones();
        int zoneCount = zones.size();

        for (int i = 0; i < zoneCount; i++) {
            Zone oZone = zones.get(i);

            for (int j = 0; j < zoneCount; j++) {
                Zone dZone = zones.get(j);

                double flow = hours * flowCoeff * odMatrix[i][j];//assumption: positive number!
                boolean roundUp = uniform.nextDoubleFromTo(0, 1) > (flow - (int)flow); 
                
                int trips = (int)flow + (roundUp ? 1 : 0);  
                
                if (trips == 0) {
                    continue;
                }

                boolean isInternalFlow = oZone.getType() == Type.INTERNAL
                        && dZone.getType() == Type.INTERNAL;

                double timeStep = hours * 3600 / trips;

                for (int k = 0; k < trips; k++) {
                    Plan plan = createPlan();

                    Coord oCoord = getRandomCoordInZone(oZone);
                    Activity startAct = createActivity(plan, "dummy", oCoord);

                    startAct.setEndTime((int) (startTime + k * timeStep + uniform.nextDoubleFromTo(
                            0, timeStep)));

                    if (isInternalFlow && taxiProbability > 0
                            && uniform.nextDoubleFromTo(0, 1) < taxiProbability) {
                        taxiCustomers.add(plan.getPerson());
                    }

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    Coord dCoord = getRandomCoordInZone(dZone);
                    /*Activity endAct = */createActivity(plan, "dummy", dCoord);

                    createAndInitPerson(plan);
                }
            }
        }
    }


    public void writeTaxiCustomers(String taxiCustomersFileName)
        throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(taxiCustomersFileName)));

        for (Person p : taxiCustomers) {
            bw.write(p.getId().toString());
            bw.newLine();
        }

        bw.close();
    }


    public static List<String> readTaxiCustomerIds(String taxiCustomersFileName)
        throws IOException
    {
        BufferedReader br = new BufferedReader(new FileReader(new File(taxiCustomersFileName)));
        List<String> taxiCustomerIds = new ArrayList<String>();

        String line;
        while ( (line = br.readLine()) != null) {
            taxiCustomerIds.add(line);
        }

        br.close();
        return taxiCustomerIds;
    }
    
    
    public double[][] readODMatrix(String odMatrixFileName)
    {
        try {
            return Array2DReader.getDoubleArray(new File(odMatrixFileName),
                getFileOrderedZones().size());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        String dirName = "D:\\PP-rad\\taxi\\mielec\\";
        String networkFileName = dirName + "network.xml";
        String zonesXMLFileName = dirName + "zones.xml";
        String zonesShpFileName = dirName + "GIS\\zones_with_no_zone.SHP";
        String odMatrixFileName = dirName + "odMatrix.dat";
        String plansFileName = dirName + "plans.xml";
        String idField = "NO";

        double hours = 2;
        double flowCoeff = 0.5;
        double taxiProbability = 0.10;

        String taxiFileName = dirName + "taxiCustomers_" + ((int) (taxiProbability * 100))
                + "_pc.txt";

        ODDemandGenerator dg = new ODDemandGenerator(networkFileName, zonesXMLFileName,
                zonesShpFileName, idField);

        double[][] odMatrix = dg.readODMatrix(odMatrixFileName);

        dg.generateMultiplePeriods(odMatrix, new double[] { hours }, new double[] { flowCoeff },
                new double[] { taxiProbability });
        dg.write(plansFileName);
        dg.writeTaxiCustomers(taxiFileName);
    }
}
