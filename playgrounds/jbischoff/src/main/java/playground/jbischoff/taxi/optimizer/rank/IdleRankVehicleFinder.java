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

package playground.jbischoff.taxi.optimizer.rank;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.util.DistanceUtils;

import com.google.common.collect.*;

import playground.jbischoff.energy.charging.taxi.ElectricTaxiChargingHandler;
import playground.jbischoff.taxi.vehicles.ElectricTaxi;
import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.scheduler.*;


/**
 * @author jbischoff
 */

public class IdleRankVehicleFinder
{
    private final MatsimVrpContext context;
    private final TaxiScheduler scheduler;
    private ElectricTaxiChargingHandler ecabHandler;
    private boolean IsElectric;
    private boolean useChargeOverTime;
    private boolean includeDriversWill = false;
    Random rnd;
    private static double MINIMUM_SOC_FOR_DISPATCH = 0.25;


    public IdleRankVehicleFinder(MatsimVrpContext context, TaxiScheduler scheduler)
    {
        this.context = context;
        this.scheduler = scheduler;
        this.IsElectric = false;
        this.useChargeOverTime = false;
        this.rnd = new Random(7);
        System.out.println("Using distance measure: Straight line");
    }


    public void addEcabHandler(ElectricTaxiChargingHandler ecabHandler)
    {
        this.ecabHandler = ecabHandler;
        this.IsElectric = true;
    }


    public void setUseChargeOverTime(boolean useChargeOverDistance)
    {
        this.useChargeOverTime = useChargeOverDistance;
    }


    private boolean hasEnoughCapacityForTask(Vehicle veh)
    {
        return (getVehicleSoc(veh) > MINIMUM_SOC_FOR_DISPATCH);
    }


    private double getVehicleSoc(Vehicle veh)
    {
        return this.ecabHandler.getRelativeTaxiSoC(Id.create(veh.getId(), ElectricTaxi.class));
    }


    public Vehicle findVehicleForRequest(Iterable<Vehicle> vehicles, TaxiRequest request)
    {
        if (this.useChargeOverTime) {

            //    		return findHighestChargedIdleVehicleDistanceSort(request);
            return findBestChargedVehicle(request);
            //    		return findHighestChargedIdleVehicle(request);

        }
        else
            return findClosestFIFOVehicle(request);
    }


//    public Iterable<Vehicle> filterVehiclesForRequest(Iterable<Vehicle> vehicles,
//            TaxiRequest request)
//    {
//        return Lists.newArrayList(findVehicleForRequest(vehicles, request));
//    }


    private Vehicle findBestChargedVehicle(TaxiRequest req)
    {
        Vehicle bestVeh = null;
        double bestDistance = 1e9;

        List<Vehicle> vehicles = new ArrayList<Vehicle>(context.getVrpData().getVehicles().values());
        Collections.shuffle(vehicles, rnd);

        for (Vehicle veh : vehicles) {
            if (this.IsElectric)
                if (!this.hasEnoughCapacityForTask(veh))
                    continue;

            double distance = calculateSquaredDistance(req, veh);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestVeh = veh;
            }
            else if (distance == bestDistance) {

                if (bestVeh == null) {
                    bestVeh = veh;
                    continue;
                }
                if (this.IsElectric) {
                    if (this.getVehicleSoc(veh) > this.getVehicleSoc(bestVeh)) {
                        bestVeh = veh;
                    }
                }
                //higher charge, if distance is equal	
            }
        }

        return bestVeh;
    }


    private Vehicle findHighestChargedIdleVehicle(TaxiRequest req)
    {
        Vehicle bestVeh = null;
        double bestSoc = 0;

        List<Vehicle> vehicles = new ArrayList<Vehicle>(context.getVrpData().getVehicles().values());
        Collections.shuffle(vehicles, rnd);

        for (Vehicle veh : Iterables.filter(vehicles, TaxiSchedulerUtils.createIsIdle(scheduler))) {
            if (this.IsElectric)
                if (!this.hasEnoughCapacityForTask(veh))
                    continue;
            double soc = this.getVehicleSoc(veh);
            if (soc > bestSoc) {
                bestSoc = soc;
                bestVeh = veh;
            }
        }

        return bestVeh;

    }


    private Vehicle findHighestChargedIdleVehicleDistanceSort(TaxiRequest req)
    {
        Vehicle bestVeh = null;
        double bestSoc = 0;

        List<Vehicle> vehicles = new ArrayList<Vehicle>(context.getVrpData().getVehicles().values());
        Collections.shuffle(vehicles, rnd);

        for (Vehicle veh : Iterables.filter(vehicles, TaxiSchedulerUtils.createIsIdle(scheduler))) {
            if (this.IsElectric)
                if (!this.hasEnoughCapacityForTask(veh))
                    continue;
            double soc = this.getVehicleSoc(veh);
            if (soc > bestSoc) {
                bestSoc = soc;
                bestVeh = veh;
            }
            else if (soc == bestSoc) {
                if (bestVeh == null) {
                    bestVeh = veh;
                    continue;
                }
                if (this.calculateSquaredDistance(req, veh) < this.calculateSquaredDistance(req,
                        bestVeh)) {
                    bestVeh = veh;
                }
            }
        }

        return bestVeh;

    }


    private Vehicle findClosestFIFOVehicle(TaxiRequest req)
    {
        List<Vehicle> vehicles = new ArrayList<Vehicle>(context.getVrpData().getVehicles().values());
        Collections.shuffle(vehicles, rnd);

        Vehicle bestVeh = null;
        //          double bestDistance = Double.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE / 2;
        for (Vehicle veh : Iterables.filter(vehicles, TaxiSchedulerUtils.createIsIdle(scheduler))) {
            if (this.IsElectric)
                if (!this.hasEnoughCapacityForTask(veh))
                    continue;
            double distance = calculateSquaredDistance(req, veh);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestVeh = veh;
            }
            else if (distance == bestDistance) {

                if (bestVeh == null) {
                    bestVeh = veh;
                    continue;
                }
                if (veh.getSchedule().getCurrentTask().getBeginTime() < bestVeh.getSchedule()
                        .getCurrentTask().getBeginTime())
                    bestVeh = veh;

                //FIFO, if distance is equal	
            }
        }

        return bestVeh;
    }


    private Vehicle findClosestWillingVehicle(TaxiRequest req)
    {
        List<Vehicle> vehicles = new ArrayList<Vehicle>(context.getVrpData().getVehicles().values());
        Collections.shuffle(vehicles, rnd);

        Vehicle bestVeh = null;
        //          double bestDistance = Double.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE / 2;
        for (Vehicle veh : Iterables.filter(vehicles, TaxiSchedulerUtils.createIsIdle(scheduler))) {
            if (this.IsElectric)
                if (!this.hasEnoughCapacityForTask(veh))
                    continue;
            if (!vehicleWillingToServeRequest(veh, req))
                continue;
            double distance = calculateSquaredDistance(req, veh);
            if (distance < bestDistance) {

                bestDistance = distance;
                bestVeh = veh;

            }
            else if (distance == bestDistance) {

                if (bestVeh == null) {
                    bestVeh = veh;
                    continue;
                }
                if (veh.getSchedule().getCurrentTask().getBeginTime() < bestVeh.getSchedule()
                        .getCurrentTask().getBeginTime())
                    bestVeh = veh;

                //FIFO, if distance is equal    
            }
        }

        return bestVeh;
    }


    /**
     * 
     */
    private boolean vehicleWillingToServeRequest(Vehicle veh, TaxiRequest req)
    {

        return true;

    }


    private double calculateSquaredDistance(TaxiRequest req, Vehicle veh)
    {
        LinkTimePair departure = scheduler.getEarliestIdleness(veh);
        Link fromLink;
        if (departure == null) {
            return Double.MAX_VALUE;
        }
        fromLink = departure.link;

        // from means here the request's from, to is therefore the taxis destination to pick up customer (just a reminder for myself)
        Link toLink = req.getFromLink();

        return DistanceUtils.calculateSquaredDistance(fromLink, toLink);
    }
}
