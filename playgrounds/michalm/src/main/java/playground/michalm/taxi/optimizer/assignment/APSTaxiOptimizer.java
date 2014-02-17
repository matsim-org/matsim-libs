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

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiUtils;
import playground.michalm.taxi.optimizer.immediaterequest.*;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class APSTaxiOptimizer
    implements ImmediateRequestTaxiOptimizer
{
    private final TaxiScheduler scheduler;
    protected final MatsimVrpContext context;

    private final boolean seekDemandSupplyEquilibrium;

    private final Map<Id, TaxiRequest> unplannedRequests;

    private boolean requiresReoptimization = false;


    public APSTaxiOptimizer(TaxiScheduler scheduler, MatsimVrpContext context,
            boolean seekDemandSupplyEquilibrium)
    {
        this.scheduler = scheduler;
        this.context = context;
        this.seekDemandSupplyEquilibrium = seekDemandSupplyEquilibrium;

        int vehCount = context.getVrpData().getVehicles().size();

        unplannedRequests = new HashMap<Id, TaxiRequest>(vehCount);
    }


    protected void scheduleUnplannedRequests()
    {
        for (Vehicle veh : context.getVrpData().getVehicles()) {
            scheduler.removePlannedRequests(TaxiSchedules.getSchedule(veh), unplannedRequests);
        }

        int rDim = unplannedRequests.size();
        if (rDim == 0) {
            return;
        }

        List<Vehicle> vehicles = new ArrayList<Vehicle>();
        List<LinkTimePair> departures = new ArrayList<LinkTimePair>();
        int idleVehCount = 0;
        for (Vehicle v : context.getVrpData().getVehicles()) {
            LinkTimePair departure = scheduler.getEarliestIdleness(v);
            //LinkTimePair departure = scheduler.getClosestDiversion(v);

            if (departure != null) {
                vehicles.add(v);
                departures.add(departure);

                if (TaxiUtils.isIdle(v)) {
                    idleVehCount++;
                }
            }
        }

        int vDim = vehicles.size();
        if (vDim == 0) {
            return;
        }

        TaxiRequest[] requests = unplannedRequests.values().toArray(new TaxiRequest[rDim]);

        VrpPathCalculator calculator = scheduler.getCalculator();

        boolean reduceTP = seekDemandSupplyEquilibrium ? //
                rDim > idleVehCount : //
                scheduler.getParams().minimizePickupTripTime;

        double[][] costMatrix = new double[vDim][rDim];
        VrpPathWithTravelData[][] paths = new VrpPathWithTravelData[vDim][rDim];

        for (int v = 0; v < vDim; v++) {
            for (int r = 0; r < rDim; r++) {
                LinkTimePair departure = departures.get(v);
                TaxiRequest req = requests[r];

                VrpPathWithTravelData path = calculator.calcPath(departure.link, req.getFromLink(),
                        departure.time);

                costMatrix[v][r] = reduceTP ? //
                        path.getTravelTime() : //T_P
                        path.getArrivalTime();//T_W

                paths[v][r] = path;
            }
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
                unplannedRequests.remove(req.getId());
            }
        }
    }
    
    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        if (requiresReoptimization) {
            scheduleUnplannedRequests();
        }
    }


    @Override
    public void requestSubmitted(Request request)
    {
        unplannedRequests.put(request.getId(), (TaxiRequest)request);
        requiresReoptimization = true;
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> taxiSchedule = (Schedule<TaxiTask>)schedule;

        scheduler.updateBeforeNextTask(taxiSchedule);
        TaxiTask nextTask = taxiSchedule.nextTask();

        if (!scheduler.getParams().destinationKnown) {
            if (nextTask != null // schedule != COMPLETED
                    && nextTask.getTaxiTaskType() == TaxiTaskType.DROPOFF_DRIVE) {
                requiresReoptimization = true;
            }
        }
    }


    //TODO switch on/off
    @Override
    public void nextLinkEntered(DriveTask driveTask)
    {
        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> schedule = (Schedule<TaxiTask>)driveTask.getSchedule();

        double predictedEndTime = driveTask.getTaskTracker().predictEndTime(context.getTime());
        scheduler.updateCurrentAndPlannedTasks(schedule, predictedEndTime);

        //we may here possibly decide here whether or not to reoptimize
        //if (delays/speedups encountered) {requiresReoptimization = true;}
    }


    @Override
    public TaxiScheduler getScheduler()
    {
        return scheduler;
    }
}
