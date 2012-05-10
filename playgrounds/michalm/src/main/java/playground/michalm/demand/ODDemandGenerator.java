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
    extends DemandGenerator
{
    // private static final Logger log = Logger.getLogger(ODDemandGenerator.class);

    private final int zoneCount;
    private final double[][] odMatrix;

    private final double hours; // time of simulation (i.e. 1.5 hours)
    private final double flowCoeff; // artificial increase/decrease of the flow
    private final double taxiProbability; // for internal flow (i.e. between internal zones: 0-8)

    private final List<Person> taxiCustomers = new ArrayList<Person>();


    public ODDemandGenerator(String networkFileName, String zonesXMLFileName,
            String zonesShpFileName, String odMatrixFileName, String idField, double hours,
            double flowCoeff, double taxiProbability)
        throws IOException, SAXException, ParserConfigurationException
    {
        super(networkFileName, zonesXMLFileName, zonesShpFileName, idField);

        this.hours = hours;
        this.flowCoeff = flowCoeff;
        this.taxiProbability = taxiProbability;

        // read OD matrix
        zoneCount = fileOrderedZones.size();
        odMatrix = Array2DReader.getDoubleArray(new File(odMatrixFileName), zoneCount);
    }


    @Override
    public void generate()
    {
        PopulationFactory pf = getPopulationFactory();

        for (int i = 0; i < zoneCount; i++) {
            Zone oZone = fileOrderedZones.get(i);

            for (int j = 0; j < zoneCount; j++) {
                Zone dZone = fileOrderedZones.get(j);

                int odFlow = (int)Math.round(flowCoeff * odMatrix[i][j]);

                if (odFlow == 0) {
                    continue;
                }

                boolean isInternalFlow = oZone.getType() == Type.INTERNAL
                        && dZone.getType() == Type.INTERNAL;

                int count = (int)Math.round(hours * odFlow);

                for (int k = 0; k < count; k++) {
                    Plan plan = createPlan();

                    Coord oCoord = getRandomCoordInZone(oZone);
                    Activity act = createActivity(plan, "dummy", oCoord);

                    double timeStep = 3600. / odFlow;
                    act.setEndTime((int)uniform.nextDoubleFromTo(k * timeStep, (k + 1) * timeStep));

                    if (isInternalFlow && taxiProbability > 0
                            && uniform.nextDoubleFromTo(0, 1) < taxiProbability) {
                        // plan.addLeg(pf.createLeg(TaxiModeDepartureHandler.TAXI_MODE));

                        plan.addLeg(pf.createLeg(TransportMode.car));
                        taxiCustomers.add(plan.getPerson());
                    }
                    else {
                        plan.addLeg(pf.createLeg(TransportMode.car));
                    }

                    Coord dCoord = getRandomCoordInZone(dZone);
                    act = createActivity(plan, "dummy", dCoord);
                }
            }
        }
    }


    private void writeTaxiCustomers(String taxiCustomersFileName)
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


    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        String dirName = "D:\\PP-rad\\taxi\\mielec-nowe-OD\\";
        String networkFileName = dirName + "network.xml";
        String zonesXMLFileName = dirName + "zones.xml";
        String zonesShpFileName = dirName + "GIS\\zones_with_no_zone.SHP";
        String odMatrixFileName = dirName + "odMatrix.dat";
        String plansFileName = dirName + "plans.xml";
        String idField = "NO";

        double hours = 2;
        double flowCoeff = 0.5;
        double taxiProbability = 0.01;

        String taxiFileName = dirName + "taxiCustomers_" + ((int) (taxiProbability * 100))
                + "_pc.txt";

        ODDemandGenerator dg = new ODDemandGenerator(networkFileName, zonesXMLFileName,
                zonesShpFileName, odMatrixFileName, idField, hours, flowCoeff, taxiProbability);
        dg.generate();
        dg.write(plansFileName);
        dg.writeTaxiCustomers(taxiFileName);
    }
}
