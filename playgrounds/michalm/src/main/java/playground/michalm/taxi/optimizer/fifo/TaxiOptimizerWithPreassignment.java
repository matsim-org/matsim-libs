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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration.Goal;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.scheduler.TaxiSchedulerParams;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathCost;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;


/**
 * Works similarly to OTS
 * 
 * @author michalm
 */
public class TaxiOptimizerWithPreassignment
{
    public static OTSTaxiOptimizer createOptimizer(MatsimVrpContext context,
            VrpPathCalculator calculator, double pickupDuration, double dropoffDuration,
            String reqIdToVehIdFile, String workingDir)
    {
        final Map<Id<Request>, Vehicle> reqIdToVehMap = readReqIdToVehMap(context, reqIdToVehIdFile);

        TaxiSchedulerParams params = new TaxiSchedulerParams(true, pickupDuration, dropoffDuration);
        TaxiScheduler scheduler = new TaxiScheduler(context, calculator, params);

        VehicleRequestPathFinder vrpFinder = createVrpFinder(calculator, scheduler, reqIdToVehMap);

        TaxiOptimizerConfiguration optimConfig = new TaxiOptimizerConfiguration(context,
                calculator, scheduler, vrpFinder, Goal.NULL, workingDir);

        return new OTSTaxiOptimizer(optimConfig);
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


    private static Map<Id<Request>, Vehicle> readReqIdToVehMap(MatsimVrpContext context,
            String reqIdToVehIdFile)
    {
        Scanner scanner = null;

        try {
            scanner = new Scanner(new File(reqIdToVehIdFile));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        List<Vehicle> vehicles = context.getVrpData().getVehicles();
        Map<Id<Request>, Vehicle> reqIdToVehMap = new HashMap<>();

        while (scanner.hasNext()) {
            Id<Request> reqId = Id.create(scanner.next(), Request.class);
            Vehicle veh = vehicles.get(scanner.nextInt());
            reqIdToVehMap.put(reqId, veh);
        }

        scanner.close();
        return reqIdToVehMap;
    }
}
