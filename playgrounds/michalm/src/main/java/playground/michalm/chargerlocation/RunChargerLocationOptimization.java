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
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.michalm.berlin.BerlinZoneUtils;
import playground.michalm.chargerlocation.ChargerLocationProblem.ChargerLocationSolution;
import playground.michalm.util.distance.*;
import playground.michalm.zone.Zone;


public class RunChargerLocationOptimization
{
    private ChargerLocationProblem problem;
    private ChargerLocationSolution initSolution;
    private ChargerLocationSolution finalSolution;

    private Map<Id<Zone>, Double> zonePotentials = new HashMap<>();
    private double totalPotential = 0;


    private enum EnergyConsumption
    {
        SUMMER_HEAT(69.75), NORMAL(41.75), WINTER_COLD(105.75), WINTER_COLD_FOSSIL_HEATING(41.75);

        private final double energyPerVehicle;//kWh


        private EnergyConsumption(double energyPerVehicle)
        {
            this.energyPerVehicle = energyPerVehicle;
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


    private void createProblem()
    {
        //TST'15 paper
        double chargerPower = 50;//kW
        EnergyConsumption energyConsumption = EnergyConsumption.NORMAL;
        double deltaSOC = 16;//kWh

        //ANT'15 paper
        TimeHorizon horizon = TimeHorizon._16H;

        //LP constraints
        double maxDistance = 5_000;//m
        int maxChargersInZone = 30;
        //int maxChargers = 450;
        double supplyBuffer = 1.1;

        String dir = "d:/svn-vsp/sustainability-w-michal-and-dlr/data/";
        String networkFile = dir + "scenarios/2015_02_basic_scenario_v6/berlin_brb.xml";
        String zonesXmlFile = dir + "shp_merged/berlin_zones.xml";
        String zonesShpFile = dir + "shp_merged/berlin_zones.shp";
        String potentialFile = dir + "taxi_berlin/2014/status/idleVehiclesPerZoneAndHour.txt";
        //String taxiRanksFile = dir + "scenarios/2015_02_basic_scenario_v6/berlin_ranks.xml";

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);

        //        DistanceCalculator calculator = DistanceCalculators
        //                .crateFreespeedDistanceCalculator(scenario.getNetwork());
        DistanceCalculator calculator = DistanceCalculators.BEELINE_DISTANCE_CALCULATOR;

        Map<Id<Zone>, Zone> zones = BerlinZoneUtils.readZones(scenario, zonesXmlFile, zonesShpFile);
        readPotentials(zones, potentialFile, horizon);

        double totalEnergyConsumed = Math.max(energyConsumption.energyPerVehicle - deltaSOC, 0)
                * horizon.vehicleCount;
        ZoneData zoneData = new ZoneData(zones, zonePotentials, totalEnergyConsumed
                / totalPotential);

        //read/create stations at either zone centroids or ranks 
        List<ChargingStation> stations = new ArrayList<>();
        for (Zone z : zones.values()) {
            Id<ChargingStation> id = Id.create(z.getId(), ChargingStation.class);
            ChargingStation station = new ChargingStation(id, z.getCoord(), chargerPower);
            stations.add(station);
        }
        ChargerData chargerData = new ChargerData(stations, horizon.hours);

        //        double maxEnergyProduced = Math.min(maxChargersInZone * stations.size(), maxChargers)
        //                * chargerPower * timeHorizon;
        //
        //        if (maxEnergyProduced < totalEnergyConsumed) {
        //            throw new RuntimeException("demand > supply: " + totalEnergyConsumed + " > "
        //                    + maxEnergyProduced);
        //        }

        int minMaxChargers = (int)Math.ceil(supplyBuffer * totalEnergyConsumed
                / (chargerPower * horizon.hours));
        System.out.println("minMaxChargers = " + minMaxChargers);

        problem = new ChargerLocationProblem(zoneData, chargerData, calculator, maxDistance,
                maxChargersInZone, minMaxChargers);
    }


    private void readPotentials(Map<Id<Zone>, Zone> zones, String potentialFile, TimeHorizon horizon)
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


    private void writeSolution(ChargerLocationSolution solution)
    {
        String dir = "d:/PP-rad/berlin/chargerLocation/";

        writeChargers(solution.x, dir + "chargers.csv");
        writeFlows(solution.f, dir + "flows.csv");
    }


    private void writeChargers(int[] x, String file)
    {
        List<ChargingStation> stations = problem.chargerData.stations;

        try (PrintWriter writer = new PrintWriter(file)) {
            for (int j = 0; j < problem.J; j++) {
                if (x[j] > 0) {
                    Id<ChargingStation> stationId = stations.get(j).getId();
                    writer.printf("%s,%d\n", stationId, x[j]);
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
        List<ChargingStation> stations = problem.chargerData.stations;

        try (PrintWriter writer = new PrintWriter(file)) {
            for (int i = 0; i < problem.I; i++) {
                for (int j = 0; j < problem.J; j++) {
                    if (f[i][j] > 1e-2) {
                        Id<Zone> zoneId = zoneEntries.get(i).zone.getId();
                        Id<ChargingStation> stationId = stations.get(j).getId();
                        writer.printf("%s,%s,%.2f\n", zoneId, stationId, f[i][j]);
                    }
                }
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private void solveProblem()
    {
        //initSolution = new ChargerLocationFinder(problem).findInitialSolution();
        finalSolution = new ChargerLocationSolver(problem).solve(initSolution);

        writeSolution(finalSolution);
    }


    public static void main(String[] args)
    {
        RunChargerLocationOptimization runOptimizer = new RunChargerLocationOptimization();
        runOptimizer.createProblem();
        runOptimizer.solveProblem();

    }
}
