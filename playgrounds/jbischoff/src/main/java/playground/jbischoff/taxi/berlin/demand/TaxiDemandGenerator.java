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

package playground.jbischoff.taxi.berlin.demand;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatsimMatricesReader;

import playground.michalm.demand.DefaultPersonCreator;
import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.zone.Zone;
import playground.michalm.zone.Zones;


public class TaxiDemandGenerator
{
//    private static final String DATADIR = "C:/local_jb/data/";
    private static final String DATADIR = "/Users/jb/sustainability-w-michal-and-dlr/data/";
    private static final String NETWORKFILE = DATADIR+"scenarios/2015_02_strike/berlin_brb.xml";
    private static final String ZONESSHP = DATADIR+"/shp_merged/zones.shp";
    private static final String ZONESXML = DATADIR+"/shp_merged/zones.xml";
    private static final String ODMATRIX = DATADIR+"taxi_berlin/2014_10_bahnstreik/OD_BRB_2014-10/oct/demandMatrices.xml";
    private static final String PLANSFILE = DATADIR+"scenarios/2015_02_strike/plans4to3";
    private ODDemandGenerator odd;
    private Map<Id<Zone>, Zone> zones;
    private Scenario scenario;
    private Matrices matrices;


    public static void main(String[] args)
    {

        for (double i = 1.0; i<5.1; i = i+0.1){
        TaxiDemandGenerator tdg = new TaxiDemandGenerator();
        tdg.generateDemand("20141015040000", "20141016030000",i);
        tdg.writeDemand(i);
        }
    }


    private void writeDemand(double i)
    {
        double iPretty = Math.round(i*10)/10.0;
        String outfile = PLANSFILE+"_"+iPretty+".xml.gz";
        new PopulationWriter(scenario.getPopulation()).write(outfile);
    }


    private void generateDemand(String start, String end, double i)
    {
        String currentHr = start;
        int dayOffset = 0;
        do {
            Matrix matrix = this.matrices.getMatrix(currentHr);
            int currentHour = getHour(currentHr);
            double startTime = dayOffset*24*3600 + currentHour*3600; 
            odd.generateSinglePeriod(matrix, "departure", "arrival", "taxi", startTime, 3600, i);
            if (currentHour == 23) dayOffset = 1; 
            currentHr = getNextTimeString(currentHr);
        }

        while (!currentHr.equals(end));
    }


    public static String getNextTimeString(String currentHr)
    {
        String end = "0000";
        String begin = currentHr.substring(0, 6);
        String day = currentHr.substring(6, 8);
        int chr = Integer.parseInt(currentHr.substring(8, 10));

        if (chr == 23) {
            Integer d = Integer.parseInt(currentHr.substring(6, 8));
            d++;
            day = String.format("%02d", d);
            chr = 0;

        }
        else {
            chr++;
        }
        String h = String.format("%02d", chr);
        String newHr = begin + day + h + end;
        return newHr;
    }


    private static int getHour(String timeString)
    {
        String hr = timeString.substring(8, 10);
        return Integer.parseInt(hr);
    }


    TaxiDemandGenerator()
    {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(NETWORKFILE);
        this.matrices = new Matrices();
        new MatsimMatricesReader(matrices, scenario).readFile(ODMATRIX);
        this.zones = Zones.readZones(scenario, ZONESXML, ZONESSHP);
        this.odd = new ODDemandGenerator(scenario, zones,
                true, new BerlinTaxiActivityCreator(scenario), new DefaultPersonCreator(scenario, "p%05d"));
    }
}
