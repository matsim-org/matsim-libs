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

package playground.michalm.taxi.optimizer.fifo;

import java.io.*;
import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.vehreqpath.*;


/**
 * Works similarly to OTS
 * 
 * @author michalm
 */
public class TaxiOptimizerWithPreassignment
{
    public static FifoTaxiOptimizer createOptimizer(TaxiOptimizerConfiguration optimConfig,
            Map<Id<Request>, Vehicle> reqIdToVehMap)
    {
        VehicleRequestPathFinder vrpFinder = createVrpFinder(optimConfig.calculator,
                optimConfig.scheduler, reqIdToVehMap);

        TaxiOptimizerConfiguration newOptimConfig = new TaxiOptimizerConfiguration(
                optimConfig.context, optimConfig.calculator, optimConfig.scheduler, vrpFinder,
                optimConfig.filterFactory, optimConfig.goal, optimConfig.workingDirectory);

        return FifoTaxiOptimizer.createOptimizerWithoutRescheduling(newOptimConfig);
    }


    private static VehicleRequestPathFinder createVrpFinder(VrpPathCalculator calculator,
            TaxiScheduler scheduler, final Map<Id<Request>, Vehicle> reqIdToVehMap)
    {
        return new VehicleRequestPathFinder(calculator, scheduler) {
            @Override
            public VehicleRequestPath findBestVehicleForRequest(TaxiRequest req,
                    Iterable<? extends Vehicle> vehicles, VehicleRequestPathCost vrpCost)
            {
                Vehicle veh = reqIdToVehMap.get(req.getId());
                return super.findBestVehicleForRequest(req, Collections.singleton(veh), vrpCost);
            }
        };
    }


    public static Map<Id<Request>, Vehicle> readReqIdToVehMap(MatsimVrpContext context,
            String reqIdToVehIdFile)
    {
        try (Scanner scanner = new Scanner(new File(reqIdToVehIdFile))) {
            List<Vehicle> vehicles = context.getVrpData().getVehicles();
            Map<Id<Request>, Vehicle> reqIdToVehMap = new HashMap<>();

            while (scanner.hasNext()) {
                Id<Request> reqId = Id.create(scanner.next(), Request.class);
                Vehicle veh = vehicles.get(scanner.nextInt());
                reqIdToVehMap.put(reqId, veh);
            }

            return reqIdToVehMap;
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
