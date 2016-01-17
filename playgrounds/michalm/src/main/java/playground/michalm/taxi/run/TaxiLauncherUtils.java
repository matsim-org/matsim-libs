/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.run;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.*;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.michalm.ev.*;
import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.file.*;
import playground.michalm.taxi.util.stats.*;
import playground.michalm.taxi.util.stats.StatsCollector.StatsCalculator;


public class TaxiLauncherUtils
{
    public static ETaxiData initTaxiData(Scenario scenario, String taxisFile, String ranksFile)
    {
        ETaxiData taxiData = new ETaxiData();

        new ETaxiReader(scenario, taxiData).parse(taxisFile);
        new TaxiRankReader(scenario, taxiData).parse(ranksFile);

        return taxiData;
    }


    public static void initChargersAndVehicles(ETaxiData taxiData)
    {
        // TODO reduce charging speed in winter
        for (Charger c : taxiData.getChargers().values()) {
            new PartialFastChargingWithQueueingLogic(c);
        }

        // TODO variable AUX -- depends on weather etc...
        // TODO add the Leaf consumption model for driving 

        double driveRate = 150. * 3.6; //15 kWh / 100 km == 150 Wh/km; converted into J/m
        double auxPower = 500; //0.5 kW 

        for (ETaxi t : taxiData.getETaxis().values()) {
            t.setDriveEnergyConsumption(
                    EnergyConsumptions.createFixedDriveEnergyConsumption(t, driveRate));
            t.setAuxEnergyConsumption(new ETaxiAuxEnergyConsumption(t, auxPower));
        }
    }


    //no need to simulate with 1-second time step
    private static final int CHARGE_TIME_STEP = 5; //5 s ==> 0.35% SOC (fast charging, 50 kW) 
    private static final int AUX_DISCHARGE_TIME_STEP = 60;// 1 min ==> 0.25% SOC (3 kW aux power)


    public static void initChargingAndDischargingHandlers(ETaxiData taxiData, Network network,
            QSim qSim, TravelTime travelTime)
    {
        Map<Id<Vehicle>, ETaxi> vehicleToTaxi = new HashMap<>();
        for (ETaxi t : taxiData.getETaxis().values()) {
            vehicleToTaxi.put(Id.create(t.getId(), Vehicle.class ), t);
        }

        qSim.getEventsManager()
                .addHandler(new DriveDischargingHandler(vehicleToTaxi, network, travelTime));

        qSim.addQueueSimulationListeners(
                new ChargingAuxDischargingHandler(taxiData.getChargers().values(), CHARGE_TIME_STEP,
                        taxiData.getETaxis().values(), AUX_DISCHARGE_TIME_STEP));
    }


    public static void initStatsCollection(ETaxiData taxiData, QSim qSim, String output)
    {
        StatsCalculator<String> socStatsCalc = StatsCalculators.combineStatsCalculators(
                StatsCalculators.createMeanSocCalculator(taxiData),
                StatsCalculators.createDischargedVehiclesCounter(taxiData));
        qSim.addQueueSimulationListeners(
                new StatsCollector<>(socStatsCalc, 600, "mean [kWh]\tdischarged", output));
    }
}
