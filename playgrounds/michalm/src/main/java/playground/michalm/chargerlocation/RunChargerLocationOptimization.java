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

import playground.michalm.chargerlocation.ChargerLocationProblem.ChargerLocationSolution;
import playground.michalm.util.distance.*;
import playground.michalm.zone.*;


public class RunChargerLocationOptimization
{
    private ChargerLocationProblem problem;
    private ChargerLocationSolution initSolution;
    private ChargerLocationSolution finalSolution;

    private Map<Id<Zone>, Double> zonePotentials = new HashMap<>();
    private double totalPotential = 0;


    private void createProblem()
    {
        //from TST paper (depends on scenario)
        double chargerPower = 50;//kW
        double timeHorizon = 16;//16 hours (for converring: kW ==> kWh)

        //from TST paper (depends on scenario)
        double energyConsumptionPerVehicle = 70;//kWh
        double deltaSOC = 16;//kWh
        double vehicleCount = 6000;

        //LP constraints
        double maxDistance = 10_000;//m
        int maxChargersInZone = 10;
        int maxChargers = 700;

        String dir = "d:/svn-vsp/sustainability-w-michal-and-dlr/data/";

        String networkFile = dir + "scenarios/2015_02_basic_scenario_v6/berlin_brb.xml";
        String zonesXmlFile = dir + "shp_merged/berlin_zones.xml";
        String zonesShpFile = dir + "shp_merged/berlin_zones.shp";
        String potentialFile = dir + "taxi_berlin/2014/status/idleVehiclesPerZoneAndHour.txt";
        //String taxiRanksFile = dir + "scenarios/2015_02_basic_scenario_v6/berlin_ranks.xml";

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);

        System.out.println("TTCalculations - begin");
        DistanceCalculator calculator = DistanceCalculators
                .crateFreespeedDistanceCalculator(scenario.getNetwork());
        System.out.println("TTCalculations - end");
        
        Map<Id<Zone>, Zone> zones = Zones.readZones(scenario, zonesXmlFile, zonesShpFile);
        readPotentials(zones, potentialFile);

        double totalEnergyConsumed = Math.max(energyConsumptionPerVehicle - deltaSOC, 0)
                * vehicleCount;
        ZoneData zoneData = new ZoneData(zones, zonePotentials, totalEnergyConsumed
                / totalPotential);

        //read/create stations at either zone centroids or ranks 
        List<ChargingStation> stations = new ArrayList<>();
        for (Zone z : zones.values()) {
            Id<ChargingStation> id = Id.create(z.getId(), ChargingStation.class);
            ChargingStation station = new ChargingStation(id, z.getCoord(), chargerPower);
            stations.add(station);
        }
        ChargerData chargerData = new ChargerData(stations, timeHorizon);

        double maxEnergyProduced = Math.min(maxChargersInZone * stations.size(), maxChargers)
                * chargerPower * timeHorizon;
        
        if (maxEnergyProduced < totalEnergyConsumed) {
            throw new RuntimeException("demand > supply");
        }

        problem = new ChargerLocationProblem(zoneData, chargerData, calculator, maxDistance,
                maxChargersInZone, maxChargers);
    }


    private void readPotentials(Map<Id<Zone>, Zone> zones, String potentialFile)
    {
        //from: 20140407000000
        //to: 20140413230000

        int fromDay = 1;//monday
        int toDay = 4;//thursday (incl.)

        int fromHour = 7;//7am
        int toHour = 7 + 15;//16h from 7am (incl.)

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
                        try {
                            double p = s.nextDouble();
                            if (fromDay <= d && d <= toDay && fromHour <= h && h <= toHour) {
                                potential += p;
                            }
                        }
                        catch (NoSuchElementException e) {
                            System.out.println("kicha");
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
        ChargerLocationFinder finder = new ChargerLocationFinder(problem);
        initSolution = finder.findInitialSolution();

        ChargerLocationSolver solver = new ChargerLocationSolver(problem);
        finalSolution = solver.solve(initSolution);
    }


    public static void main(String[] args)
    {
        RunChargerLocationOptimization runOptimizer = new RunChargerLocationOptimization();
        runOptimizer.createProblem();
        runOptimizer.solveProblem();

    }
}
