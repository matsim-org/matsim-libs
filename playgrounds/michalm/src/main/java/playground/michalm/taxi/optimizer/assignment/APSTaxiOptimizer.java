/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.optimizer.assignment;

import java.util.*;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.*;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.optimizer.immediaterequest.*;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class APSTaxiOptimizer
    extends OTSTaxiOptimizer
{
    public APSTaxiOptimizer(TaxiScheduler scheduler, MatsimVrpContext context)
    {
        super(scheduler, context);
    }


    protected void scheduleUnplannedRequests()
    {
        List<Vehicle> vehicles = new ArrayList<Vehicle>();
        for (Vehicle v : context.getVrpData().getVehicles()) {
            if (canBeUsed(v)) {
                vehicles.add(v);
            }
        }

        int vDim = vehicles.size();
        if (vDim == 0) {
            return;
        }

        for (Vehicle veh : context.getVrpData().getVehicles()) {
            scheduler.removePlannedRequests(TaxiSchedules.getSchedule(veh), unplannedRequests);
        }
        
        int rDim = Math.min(vDim, unplannedRequests.size());
        if (rDim == 0) {
            return;
        }

        TaxiRequest[] requests = new TaxiRequest[rDim];
        for (int r = 0; r < rDim; r++) {
            requests[r] = unplannedRequests.poll();
        }

        double[][] costMatrix = new double[vDim][rDim];
        VrpPathWithTravelData[][] paths = new VrpPathWithTravelData[vDim][rDim];

        for (int v = 0; v < vehicles.size(); v++) {
            Vehicle veh = vehicles.get(v);

            for (int r = 0; r < rDim; r++) {
                TaxiRequest req = requests[r];
                VrpPathWithTravelData path = scheduler.calculateVrpPath(veh, req);

                costMatrix[v][r] = path.getTravelCost();
                paths[v][r] = path;
            }

            // for (int r = rDim; r < vDim; r++) {
            //     costMatrix[v][r] = 0;//maybe cost of staying in a unattractive location
            //     paths[v][r] = null;
            // }
        }

        int[] assignment = new HungarianAlgorithm(costMatrix).execute();

        for (int v = 0; v < vehicles.size(); v++) {
            int r = assignment[v];
            if (r == -1) {
                continue;
            }

            VrpPathWithTravelData path = paths[v][r];

            if (path != null) {
                Vehicle veh = vehicles.get(v);
                TaxiRequest req = requests[r];
                scheduler.scheduleRequest(new VehicleRequestPath(veh, req, path));
            }
        }
    }


    public boolean canBeUsed(Vehicle veh)
    {
        double currentTime = context.getTime();
        Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(veh);

        // time window T1 exceeded
        if (currentTime >= veh.getT1()) {
            return false;// skip this vehicle
        }

        switch (schedule.getStatus()) {
            case UNPLANNED:
                return true;

            case PLANNED:
            case STARTED:
                TaxiTask lastTask = Schedules.getLastTask(schedule);

                return lastTask.getTaxiTaskType() == TaxiTaskType.WAIT_STAY;

            case COMPLETED:
                return false;

            default:
                throw new IllegalStateException();
        }
    }
}
