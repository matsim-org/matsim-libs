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

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;


/**
 * Works similarly to OTS
 * 
 * @author michalm
 */
public class TaxiOptimizerWithPreassignment
    extends ImmediateRequestTaxiOptimizer
{
    private Vehicle[] reqIdToVehMapping;


    public TaxiOptimizerWithPreassignment(VrpData data, int pickupDuration,
            final Vehicle[] reqIdToVehMapping)
    {
        super(data, true, false, pickupDuration);
        this.reqIdToVehMapping = reqIdToVehMapping;
    }


    @Override
    protected VehicleDrive findBestVehicle(Request req, List<Vehicle> vehicles)
    {
        Vehicle veh = reqIdToVehMapping[req.getId()];
        return super.findBestVehicle(req, Arrays.asList(new Vehicle[] { veh }));
    }


    @Override
    protected boolean shouldOptimizeBeforeNextTask(Vehicle vehicle, boolean scheduleUpdated)
    {
        return false;
    }


    @Override
    protected boolean shouldOptimizeAfterNextTask(Vehicle vehicle, boolean scheduleUpdated)
    {
        return false;
    }


    public static TaxiOptimizerWithPreassignment createOptimizer(VrpData data, int pickupDuration,
            String reqIdToVehIdFile)
    {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(reqIdToVehIdFile));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        List<Vehicle> vehicles = data.getVehicles();
        Vehicle[] reqIdToVehMapping = new Vehicle[scanner.nextInt()];

        for (int i = 0; i < reqIdToVehMapping.length; i++) {
            reqIdToVehMapping[i] = vehicles.get(scanner.nextInt());
        }

        return new TaxiOptimizerWithPreassignment(data, pickupDuration, reqIdToVehMapping);
    }
}
