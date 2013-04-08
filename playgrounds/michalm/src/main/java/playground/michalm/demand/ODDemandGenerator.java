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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.xml.sax.SAXException;

import pl.poznan.put.util.array2d.Array2DReader;
import pl.poznan.put.util.random.*;
import playground.michalm.demand.Zone.Type;

import com.google.common.collect.Lists;


public class ODDemandGenerator
// extends AbstractDemandGenerator
{
    private final UniformRandom uniform = RandomUtils.getGlobalUniform();

    private final Scenario scenario;
    private final LocationGenerator lg;
    private final List<Zone> zones;
    private final PopulationFactory pf;
    private final List<Person> taxiCustomers = new ArrayList<Person>();
    private int curentAgentId = 0;


    public ODDemandGenerator(Scenario scenario, LocationGenerator lg, Map<Id, Zone> zoneMap)
    {
        this.scenario = scenario;
        this.lg = lg;
        this.zones = Lists.newArrayList(zoneMap.values());
        pf = scenario.getPopulation().getFactory();
    }


    private Person createAndInitPerson(Plan plan)
    {
        String strId = String.format("%07d", curentAgentId);
        curentAgentId++;

        Person person = pf.createPerson(scenario.createId(strId));
        person.addPlan(plan);

        scenario.getPopulation().addPerson(person);
        return person;
    }


    public void generateSinglePeriod(double[][] odMatrix, String fromActivityType,
            String toActivityType, double hours, double flowCoeff, double taxiProbability,
            double startTime)
    {
        int zoneCount = zones.size();

        for (int i = 0; i < zoneCount; i++) {
            Zone oZone = zones.get(i);

            for (int j = 0; j < zoneCount; j++) {
                Zone dZone = zones.get(j);

                double flow = hours * flowCoeff * odMatrix[i][j];// assumption: positive number!
                boolean roundUp = uniform.nextDouble(0, 1) < (flow - (int)flow);

                int trips = (int)flow + (roundUp ? 1 : 0);

                if (trips == 0) {
                    continue;
                }

                boolean isInternalFlow = oZone.getType() == Type.INTERNAL
                        && dZone.getType() == Type.INTERNAL;

                double timeStep = hours * 3600 / trips;

                for (int k = 0; k < trips; k++) {
                    Plan plan = pf.createPlan();

                    // act 0
                    Link oLink = lg.getRandomLinkInZone(oZone, fromActivityType);
                    Activity startAct = pf
                            .createActivityFromLinkId(fromActivityType, oLink.getId());
                    startAct.setEndTime((int) (startTime + k * timeStep + uniform.nextDouble(0,
                            timeStep)));
                    plan.addActivity(startAct);

                    // leg
                    plan.addLeg(pf.createLeg(TransportMode.car));

                    // act1
                    Link dLink = lg.getRandomLinkInZone(dZone, toActivityType, oLink.getId());
                    Activity endAct = pf.createActivityFromLinkId(toActivityType, dLink.getId());
                    plan.addActivity(endAct);

                    createAndInitPerson(plan);

                    // isTaxi?
                    if (isInternalFlow && taxiProbability > 0
                            && uniform.nextDouble(0, 1) < taxiProbability) {
                        taxiCustomers.add(plan.getPerson());
                    }
                }
            }
        }
    }


    public void generateMultiplePeriods(double[][] odMatrix, String fromActivityType,
            String toActivityType, double[] hours, double[] flowCoeff, double[] taxiProbability)
    {
        double startTime = 0;
        for (int i = 0; i < hours.length; i++) {
            generateSinglePeriod(odMatrix, fromActivityType, toActivityType, hours[i],
                    flowCoeff[i], taxiProbability[i], startTime);
            startTime += 3600 * hours[i];
        }
    }


    public void generateMultiplePeriods(double[][][] odMatrix, String fromActivityType,
            String toActivityType, double[] taxiProbability)
    {
        double startTime = 0;
        for (int i = 0; i < odMatrix.length; i++) {
            generateSinglePeriod(odMatrix[i], fromActivityType, toActivityType, 1, 1,
                    taxiProbability[i], startTime);
            startTime += 3600;
        }
    }


    public void writeTaxiCustomers(String taxiCustomersFile)
        throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(taxiCustomersFile)));

        for (Person p : taxiCustomers) {
            bw.write(p.getId().toString());
            bw.newLine();
        }

        bw.close();
    }


    public static List<String> readTaxiCustomerIds(String taxiCustomersFile)
        throws IOException
    {
        BufferedReader br = new BufferedReader(new FileReader(new File(taxiCustomersFile)));
        List<String> taxiCustomerIds = new ArrayList<String>();

        String line;
        while ( (line = br.readLine()) != null) {
            taxiCustomerIds.add(line);
        }

        br.close();
        return taxiCustomerIds;
    }


    public void write(String plansFile)
    {
        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeV4(plansFile);
        System.out.println("Generated population written to: " + plansFile);
    }


    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        String dirName = "D:\\PP-rad\\taxi\\mielec\\";
        String networkFile = dirName + "network.xml";
        String zonesXmlFile = dirName + "zones.xml";
        String zonesShpFile = dirName + "GIS\\zones_with_no_zone.SHP";
        String odMatrixFile = dirName + "odMatrix.dat";
        String plansFile = dirName + "plans.xml";
        String idField = "NO";

        double hours = 2;
        double flowCoeff = 0.5;
        double taxiProbability = 0.10;

        String taxiFile = dirName + "taxiCustomers_" + ((int) (taxiProbability * 100)) + "_pc.txt";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);
        Map<Id, Zone> zones = Zone.readZones(scenario, zonesXmlFile, zonesShpFile, idField);

        LocationGenerator lg = new DefualtLocationGenerator(scenario);
        ODDemandGenerator dg = new ODDemandGenerator(scenario, lg, zones);

        double[][] odMatrix = Array2DReader.getDoubleArray(new File(odMatrixFile), zones.size());

        dg.generateMultiplePeriods(odMatrix, "dummy", "dummy", new double[] { hours },
                new double[] { flowCoeff }, new double[] { taxiProbability });
        dg.write(plansFile);
        dg.writeTaxiCustomers(taxiFile);
    }
}
