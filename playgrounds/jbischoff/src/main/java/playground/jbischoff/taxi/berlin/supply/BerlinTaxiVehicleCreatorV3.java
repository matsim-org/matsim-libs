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

package playground.jbischoff.taxi.berlin.supply;

import java.text.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.*;
import org.matsim.matrices.*;

import playground.michalm.berlin.BerlinZoneUtils;
import playground.michalm.util.matrices.MatrixUtils;


public class BerlinTaxiVehicleCreatorV3
{
    //used in status data
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
	//used in many other files
	private static final SimpleDateFormat ORG = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final Logger log = Logger.getLogger(BerlinTaxiVehicleCreatorV3.class);

    private Map<Date, Integer> taxisOverTime = new TreeMap<Date,Integer>();
    private double[] taxisOverTimeHourlyAverage;//24h from startDate, e.g. from 4am to 3am
    private WeightedRandomSelection<Id<Zone>> wrs;

    private Scenario scenario;
    private Map<Id<Zone>, Zone> zones;
    private List<Vehicle> vehicles;

    double evShare;
    double maxTime;
    double minTime;


    public static void main(String[] args)
        throws ParseException
    {
        //String dir = "C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/";
        String dir = "d:/svn-vsp/sustainability-w-michal-and-dlr/data/";
        
        
//        String taxisOverTimeFile = dir + "taxi_berlin/2014_10_bahnstreik/VEH_IDs_2014-10/oct/oct_taxis.txt";
//        String taxisOverTimeFile = dir + "taxi_berlin/2013/status/taxisovertime.csv";
        String taxisOverTimeFile = dir + "taxi_berlin/2013/vehicles/taxisweekly.csv";

        //String networkFile = dir + "scenarios/2015_02_basic_scenario_v6/berlin_brb.xml";
        String networkFile = dir + "network/berlin.xml";//only Berlin!!!
        
        
        
        String zoneShpFile = dir + "shp_merged/zones.shp";
        String zoneXmlFile = dir + "shp_merged/zones.xml";
        
        //String vehicleFile = dir + "scenarios/2015_02_basic_scenario_v6/taxis4to4_EV";
        String vehicleFile = dir + "scenarios/2015_08_only_berlin_v1/taxis4to4_EV";
        
        
        String statusMatrixFile = dir + "taxi_berlin/2013/status/statusMatrixAvg.xml";
        
        
        BerlinTaxiVehicleCreatorV3 btv = new BerlinTaxiVehicleCreatorV3();
        btv.evShare = 0.0;
        btv.minTime = 4.0 * 3600;
        btv.maxTime = 17.0 * 3600;
        btv.readTaxisOverTime(taxisOverTimeFile);
//        btv.createAverages(SDF.parse("2014-10-15 03:30:00"));
        btv.createAverages(SDF.parse("2013-04-16 03:30:00"));
        btv.prepareNetwork(networkFile, zoneShpFile, zoneXmlFile);
        btv.prepareMatrices(statusMatrixFile);
        btv.createVehicles();

        btv.writeVehicles(vehicleFile);
    }


    private void prepareNetwork(String networkFile, String zoneShpFile, String zoneXmlFile)
    {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
        zones = BerlinZoneUtils.readZones(zoneXmlFile, zoneShpFile);
    }


    private void readTaxisOverTime(String taxisOverTimeFile)
    {
        TabularFileParserConfig config = new TabularFileParserConfig();
        log.info("parsing " + taxisOverTimeFile);
        config.setDelimiterTags(new String[] { "\t" });
        config.setFileName(taxisOverTimeFile);

        new TabularFileParser().parse(config, new TabularFileHandler() {
            @Override
            public void startRow(String[] row)
            {
                try {
                    taxisOverTime.put(SDF.parse(row[0]), (int) Double.parseDouble(row[1]));
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        log.info("done. (parsing " + taxisOverTimeFile + ")");
    }


    @SuppressWarnings("deprecation")
    private void createAverages(Date start)
    {
        if (start.getMinutes() != 30 || start.getSeconds() != 0) {
            //we want to obtain estimates for full hours, i.e. 4:00 or 5:00
            //by calculating averages for 3:30-4:29:59 and 4:30-5:29:59  
            throw new RuntimeException("Must start with hh:30:00");
        }

        final int HOURS = 25;//we want to have vehicles for full 24 hours, e.g. 4am to 4am next day  
        
        long startTime = start.getTime() / 1000;//in seconds
        long endTime = startTime + HOURS * 3600;//in seconds

        taxisOverTimeHourlyAverage = new double[HOURS];
        int sum = 0;
        int hour = 0;
        int n = 0;

        for (long t = startTime; t < endTime; t++) {
            if (this.taxisOverTime.containsKey(new Date(t * 1000))){
                sum += this.taxisOverTime.get(new Date(t * 1000));//seconds -> milliseconds
                n++;
            }
            if ( t % 3600 == 1799) {//t == hh:29:59
                taxisOverTimeHourlyAverage[hour] += (double)sum / n;
                sum = 0;
                hour++;
                n = 0;
            }
        }

        for (int i = 0; i<HOURS;i++){
            System.out.println(i+ " : "+ taxisOverTimeHourlyAverage[i]);
        }
    }


    private void prepareMatrices(String statusMatrixFile)
    {
        wrs = new WeightedRandomSelection<Id<Zone>>();
        Matrix avestatus = MatrixUtils.readMatrices(statusMatrixFile).getMatrix("avg");

        for (Map.Entry<String, ArrayList<Entry>> fromLOR : avestatus.getFromLocations().entrySet()) {
            if (BerlinZoneUtils.isInBerlin(fromLOR.getKey())) {
                wrs.add(Id.create(fromLOR.getKey(), Zone.class), MatrixUtils.calculateTotalValue(fromLOR.getValue()));
            }
        }
    }


    private void createVehicles()
    {
        BerlinTaxiCreator btc = new BerlinTaxiCreator(scenario, zones, wrs, evShare);
        VehicleGenerator vg = new VehicleGenerator(4*3600, maxTime, btc);
        vg.generateVehicles(taxisOverTimeHourlyAverage, 4 * 3600, 3600);
        vehicles = vg.getVehicles();
    }


    private void writeVehicles(String vehicleFile)
    {
        new VehicleWriter(vehicles).write(vehicleFile + evShare + ".xml");
    }
}