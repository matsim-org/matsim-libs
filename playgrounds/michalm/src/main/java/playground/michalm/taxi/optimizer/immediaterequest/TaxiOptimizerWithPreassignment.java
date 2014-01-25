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
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.schedule.Schedule;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.schedule.TaxiTask;


/**
 * Works similarly to OTS
 * 
 * @author michalm
 */
public class TaxiOptimizerWithPreassignment
    extends ImmediateRequestTaxiOptimizer
{
    private Map<Id, Vehicle> reqIdToVehMap;


    public TaxiOptimizerWithPreassignment(VrpData data, VrpPathCalculator calculator,
            double pickupDuration, double dropoffDuration, final Map<Id, Vehicle> reqIdToVehMap)
    {
        super(data, calculator, new Params(true, false, pickupDuration, dropoffDuration));
        this.reqIdToVehMap = reqIdToVehMap;
    }


    @Override
    protected VehiclePath findBestVehicle(TaxiRequest req, List<Vehicle> vehicles)
    {
        Vehicle veh = reqIdToVehMap.get(req.getId());
        return super.findBestVehicle(req, Arrays.asList(new Vehicle[] { veh }));
    }


    @Override
    protected boolean shouldOptimizeBeforeNextTask(Schedule<TaxiTask> schedule,
            boolean scheduleUpdated)
    {
        return false;
    }


    @Override
    protected boolean shouldOptimizeAfterNextTask(Schedule<TaxiTask> schedule,
            boolean scheduleUpdated)
    {
        return false;
    }


    public static TaxiOptimizerWithPreassignment createOptimizer(MatsimVrpData data,
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

        List<Vehicle> vehicles = data.getVrpData().getVehicles();
        Map<Id, Vehicle> reqIdToVehMap = new HashMap<Id, Vehicle>();

        int count = scanner.nextInt();
        Scenario scenario = data.getScenario();

        for (int i = 0; i < count; i++) {
            reqIdToVehMap.put(scenario.createId(i + ""), vehicles.get(scanner.nextInt()));
        }
        scanner.close();

        return new TaxiOptimizerWithPreassignment(data.getVrpData(), calculator, pickupDuration,
                dropoffDuration, reqIdToVehMap);
    }
}
