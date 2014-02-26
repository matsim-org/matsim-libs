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
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.fifo.*;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;


public class APSTaxiOptimizer
    implements TaxiOptimizer
{
    private final TaxiOptimizerConfiguration optimConfig;

    private final boolean seekDemandSupplyEquilibrium;

    private final Map<Id, TaxiRequest> unplannedRequests;

    private boolean requiresReoptimization = false;


    public APSTaxiOptimizer(TaxiOptimizerConfiguration optimConfig, boolean seekDemandSupplyEquilibrium)
    {
        this.optimConfig = optimConfig;
        this.seekDemandSupplyEquilibrium = seekDemandSupplyEquilibrium;

        int vehCount = optimConfig.context.getVrpData().getVehicles().size();

        unplannedRequests = new HashMap<Id, TaxiRequest>(vehCount);
    }


    @Override
    public TaxiOptimizerConfiguration getConfiguration()
    {
        // TODO Auto-generated method stub
        return null;
    }


    protected void scheduleUnplannedRequests()
    {
        for (Vehicle veh : optimConfig.context.getVrpData().getVehicles()) {
            optimConfig.scheduler.removePlannedRequests(TaxiSchedules.getSchedule(veh), unplannedRequests);
        }

        int rDim = unplannedRequests.size();
        if (rDim == 0) {
            return;
        }

        List<Vehicle> vehicles = new ArrayList<Vehicle>();
        List<LinkTimePair> departures = new ArrayList<LinkTimePair>();
        int idleVehCount = 0;

        double maxDepartureTime = -Double.MAX_VALUE;
        for (Vehicle v : optimConfig.context.getVrpData().getVehicles()) {
            LinkTimePair departure = optimConfig.scheduler.getEarliestIdleness(v);
            //LinkTimePair departure = scheduler.getClosestDiversion(v);

            if (departure != null) {
                vehicles.add(v);
                departures.add(departure);

                if (departure.time > maxDepartureTime) {
                    maxDepartureTime = departure.time;
                }

                if (TaxiUtils.isIdle(v)) {
                    idleVehCount++;
                }
            }
        }

        int vDim = vehicles.size();
        if (vDim == 0) {
            return;
        }

        //        double now = context.getTime();
        //
        TaxiRequest[] requests = unplannedRequests.values().toArray(new TaxiRequest[rDim]);
        int awaitingReqCount = DemandSupplyEquilibriumUtils.countAwaitingUnplannedRequests(
                unplannedRequests.values(), optimConfig.context.getTime());

        VrpPathCalculator calculator = optimConfig.calculator;

        boolean reduceTP = seekDemandSupplyEquilibrium ? //
                awaitingReqCount > idleVehCount : //
                    optimConfig.params.minimizePickupTripTime;

        double[][] costMatrix = new double[vDim][rDim];
        VrpPathWithTravelData[][] paths = new VrpPathWithTravelData[vDim][rDim];

        for (int r = 0; r < rDim; r++) {
            TaxiRequest req = requests[r];
            
            //if ()
            
            for (int v = 0; v < vDim; v++) {
                LinkTimePair departure = departures.get(v);

                VrpPathWithTravelData path = calculator.calcPath(departure.link, req.getFromLink(),
                        departure.time);

                double pickupBeginTime = Math.max(req.getT0(), path.getArrivalTime());

                costMatrix[v][r] = reduceTP ? //
                        pickupBeginTime - departure.time : //T_P
                        pickupBeginTime;//T_W

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
                optimConfig.scheduler.scheduleRequest(new VehicleRequestPath(veh, req, path));
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

        optimConfig.scheduler.updateBeforeNextTask(taxiSchedule);
        TaxiTask nextTask = taxiSchedule.nextTask();

        if (!optimConfig.params.destinationKnown) {
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

        double predictedEndTime = driveTask.getTaskTracker().predictEndTime(optimConfig.context.getTime());
        optimConfig.scheduler.updateCurrentAndPlannedTasks(schedule, predictedEndTime);

        //we may here possibly decide here whether or not to reoptimize
        //if (delays/speedups encountered) {requiresReoptimization = true;}
    }
}
