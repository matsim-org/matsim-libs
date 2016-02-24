/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.michalm.chargerlocation;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.contrib.util.distance.*;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.michalm.berlin.BerlinZoneUtils;
import playground.michalm.chargerlocation.ChargerLocationProblem.ChargerLocationSolution;


public class RunChargerLocationOptimization
{
    //TST'15 paper
    private static final double DELTA_SOC = 16;//kWh

    //ANT'15 paper
    private static final TimeHorizon HORIZON = TimeHorizon._16H;

    //MIP
    //this restrictions influences mostly the outskirts (where speeds > 25km/h ==> TT < 6 min)
    private static final double MAX_DISTANCE = 2_500;//m

    //high value -> no influence (the current approach); low value -> lack of chargers at TXL
    private static final int MAX_CHARGERS_PER_ZONE = 30;


    private enum EScenario
    {
        //works with includeDeltaSoc=true/false
        //        STANDARD(41.75, 50, 1.5), //
        //        HOT_SUMMER(69.75, 50, 1.2), //
        //        COLD_WINTER(105.75, 25, 1.05), //
        //        COLD_WINTER_FOSSIL_HEATING(41.75, 25, 1.2);

        //includeDeltaSoc=false
        COLD_WINTER_FOSSIL_HEATING(41.75, 25, 1.1);

        private final double energyPerVehicle;//kWh
        private final double chargePower;//kW
        private final double oversupply; //smaller demand -> larger oversupply (also sensitive to includeDeltaSOC)


        private EScenario(double energyPerVehicle, double chargePower, double oversupply)
        {
            this.energyPerVehicle = energyPerVehicle;
            this.chargePower = chargePower;
            this.oversupply = oversupply;
        }
    }


    private enum TimeHorizon
    {
        //ANT'15 paper (Tue 4am - Wed 4am, 16-17 Apr 2014)
        _16H(16, 7, 32064), //
        _24H(24, 0, 40444);

        private final int hours;
        private final int fromHour;
        private final int toHour;
        private final int vehicleCount;


        private TimeHorizon(int hours, int fromHour, int vehicleHours)
        {
            this.hours = hours;
            this.fromHour = fromHour;

            toHour = fromHour + hours - 1;// 16h from 7am; 24h from 0am
            vehicleCount = vehicleHours / 16; //each vehicle operates 16 hours (2 shifts)
        }
    }


    //TST'15 paper
    private final boolean includeDeltaSoc;
    private final EScenario eScenario;

    private ChargerLocationProblem problem;
    private ChargerLocationSolution solution;

    private Map<Id<Zone>, Double> zonePotentials = new HashMap<>();
    private double totalPotential = 0;


    public RunChargerLocationOptimization(EScenario eScenario, boolean includeDeltaSoc)
    {
        this.eScenario = eScenario;
        this.includeDeltaSoc = includeDeltaSoc;

        String dir = "d:/svn-vsp/sustainability-w-michal-and-dlr/data/";
        String networkFile = dir + "scenarios/2015_02_basic_scenario_v6/berlin_brb.xml";
        String zonesXmlFile = dir + "shp_merged/berlin_zones.xml";
        String zonesShpFile = dir + "shp_merged/berlin_zones.shp";
        String potentialFile = dir + "taxi_berlin/2014/status/idleVehiclesPerZoneAndHour.txt";
        //String taxiRanksFile = dir + "scenarios/2015_02_basic_scenario_v6/berlin_ranks.xml";

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

        //        DistanceCalculator calculator = DistanceCalculators
        //                .crateFreespeedDistanceCalculator(scenario.getNetwork());
        DistanceCalculator calculator = DistanceCalculators.BEELINE_DISTANCE_CALCULATOR;

        Map<Id<Zone>, Zone> zones = BerlinZoneUtils.readZones(zonesXmlFile, zonesShpFile);
        readPotentials(zones, potentialFile, HORIZON);

        double totalEnergyConsumed = //
        Math.max(eScenario.energyPerVehicle - (includeDeltaSoc ? DELTA_SOC : 0), 0)
                * HORIZON.vehicleCount;
        ZoneData zoneData = new ZoneData(zones, zonePotentials,
                totalEnergyConsumed / totalPotential);

        //read/create stations at either zone centroids or ranks 
        List<ChargerLocation> locations = new ArrayList<>();
        for (Zone z : zones.values()) {
            Id<ChargerLocation> id = Id.create(z.getId(), ChargerLocation.class);
            ChargerLocation location = new ChargerLocation(id, z.getCoord(), eScenario.chargePower);
            locations.add(location);
        }
        ChargerData chargerData = new ChargerData(locations, HORIZON.hours);

        int maxChargers = (int)Math.ceil(eScenario.oversupply * totalEnergyConsumed
                / (eScenario.chargePower * HORIZON.hours));
        System.out.println("minMaxChargers = " + maxChargers);

        problem = new ChargerLocationProblem(zoneData, chargerData, calculator, MAX_DISTANCE,
                MAX_CHARGERS_PER_ZONE, maxChargers);
    }


    private void readPotentials(Map<Id<Zone>, Zone> zones, String potentialFile,
            TimeHorizon horizon)
    {
        if (horizon == TimeHorizon._24H) {
            //TODO does not work for 24h: should be Mon 4am till Fri 4am
            throw new IllegalArgumentException();
        }

        //from: 20140407000000
        //to: 20140413230000 (incl.)
        final int FROM_DAY = 1;//monday
        final int TO_DAY = 4;//thursday (incl.)

        try (Scanner s = new Scanner(new File(potentialFile))) {
            while (s.hasNext()) {
                String zoneId = StringUtils.leftPad(s.next(), 8, '0');
                Id<Zone> id = Id.create(zoneId, Zone.class);
                if (!zones.containsKey(id)) {
                    s.nextLine();
                    continue;
                }

                double potential = 0;
                for (int d = 1; d <= 7; d++) {
                    for (int h = 1; h <= 24; h++) {
                        double p = s.nextDouble();
                        if (FROM_DAY <= d && d <= TO_DAY && horizon.fromHour <= h
                                && h <= horizon.toHour) {
                            potential += p;
                        }
                    }
                }

                zonePotentials.put(id, potential);
                totalPotential += potential;
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException();
        }
    }


    private void solveProblem()
    {
        solution = new ChargerLocationSolver(problem).solve(null);
        writeSolution(solution);
    }


    private void writeSolution(ChargerLocationSolution solution)
    {
        String dir = "d:/PP-rad/berlin/chargerLocation/";
        String name = eScenario.name() + (includeDeltaSoc ? "_DeltaSOC" : "_noDeltaSOC");
        writeChargers(solution.x,
                dir + "chargers_out_of_" + problem.maxChargers + "_" + name + ".csv");
        writeFlows(solution.f, dir + "flows_" + name + ".csv");
    }


    private void writeChargers(int[] x, String file)
    {
        List<ChargerLocation> locations = problem.chargerData.locations;

        try (PrintWriter writer = new PrintWriter(file)) {
            for (int j = 0; j < problem.J; j++) {
                if (x[j] > 0) {
                    Id<ChargerLocation> locationId = locations.get(j).getId();
                    writer.printf("%s,%d\n", locationId, x[j]);
                }
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private void writeFlows(double[][] f, String file)
    {
        List<ZoneData.Entry> zoneEntries = problem.zoneData.entries;
        List<ChargerLocation> locations = problem.chargerData.locations;

        try (PrintWriter writer = new PrintWriter(file)) {
            for (int i = 0; i < problem.I; i++) {
                for (int j = 0; j < problem.J; j++) {
                    if (f[i][j] > 1e-2) {
                        Id<Zone> zoneId = zoneEntries.get(i).zone.getId();
                        Id<ChargerLocation> locationId = locations.get(j).getId();
                        writer.printf("%s,%s,%.2f\n", zoneId, locationId, f[i][j]);
                    }
                }
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args)
    {
        for (EScenario es : EScenario.values()) {
            System.err.println("==========================" + es.name());
            //            new RunChargerLocationOptimization(es, true).solveProblem();
            new RunChargerLocationOptimization(es, false).solveProblem();
        }
    }
}
