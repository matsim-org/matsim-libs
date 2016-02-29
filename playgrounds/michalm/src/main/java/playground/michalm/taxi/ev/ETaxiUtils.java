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

package playground.michalm.taxi.ev;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.taxi.util.stats.StatsCalculators;
import org.matsim.contrib.taxi.util.stats.StatsCollector.StatsCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.michalm.ev.*;
import playground.michalm.taxi.data.*;
import playground.michalm.taxi.util.stats.EStatsCalculators;


public class ETaxiUtils
{
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


    public static DriveDischargingHandler createDriveDischargingHandler(ETaxiData taxiData,
            Network network, TravelTime travelTime, ETaxiParams eTaxiParams)
    {
        Map<Id<Vehicle>, ETaxi> vehicleToTaxi = new HashMap<>();
        for (ETaxi t : taxiData.getETaxis().values()) {
            //we assume: dvrp's vehicle.id == matsim's vehicle.id
            //see: VrpAgentSource.insertAgentsIntoMobsim() 
            vehicleToTaxi.put(Id.create(t.getId(), Vehicle.class), t);
        }

        return new DriveDischargingHandler(vehicleToTaxi, network, travelTime);
    }


    public static ChargingAuxDischargingHandler createChargingAuxDischargingHandler(
            ETaxiData taxiData, Network network, TravelTime travelTime, ETaxiParams eTaxiParams)
    {
        return new ChargingAuxDischargingHandler(taxiData.getChargers().values(),
                eTaxiParams.chargeTimeStep, taxiData.getETaxis().values(),
                eTaxiParams.auxDischargeTimeStep);
    }


    public static StatsCalculator<String> createStatsCollection(ETaxiData taxiData)
    {
        return StatsCalculators.combineStatsCalculators(
                EStatsCalculators.createMeanSocCalculator(taxiData),
                EStatsCalculators.createDischargedVehiclesCounter(taxiData));
    }
}
