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

package playground.michalm.taxi.optimizer.immediaterequest;

import java.io.*;
import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.*;

import playground.michalm.taxi.model.TaxiRequest;


/**
 * Works similarly to OTS
 * 
 * @author michalm
 */
public class TaxiOptimizerWithPreassignment
{
    public static OTSTaxiOptimizer createOptimizer(MatsimVrpContext context,
            VrpPathCalculator calculator, double pickupDuration, double dropoffDuration,
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
        final Map<Id, Vehicle> reqIdToVehMap = new HashMap<Id, Vehicle>();

        int count = scanner.nextInt();
        Scenario scenario = context.getScenario();

        for (int i = 0; i < count; i++) {
            reqIdToVehMap.put(scenario.createId(i + ""), vehicles.get(scanner.nextInt()));
        }
        scanner.close();

        ImmediateRequestParams params = new ImmediateRequestParams(true, false, pickupDuration,
                dropoffDuration);

        TaxiScheduler scheduler = new TaxiScheduler(context, calculator, params) {
            @Override
            public VehicleRequestPath findBestVehicleRequestPath(TaxiRequest req,
                    Collection<Vehicle> vehicles, Comparator<VrpPathWithTravelData> pathComparator)
            {
                Vehicle veh = reqIdToVehMap.get(req.getId());
                return new VehicleRequestPath(veh, req, calculateVrpPath(veh, req));
            }
        };

        return new OTSTaxiOptimizer(scheduler);
    }
}
