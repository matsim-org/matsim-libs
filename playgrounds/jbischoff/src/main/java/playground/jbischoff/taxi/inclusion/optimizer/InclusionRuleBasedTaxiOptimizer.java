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

package playground.jbischoff.taxi.inclusion.optimizer;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.zone.*;


public class InclusionRuleBasedTaxiOptimizer
    extends AbstractTaxiOptimizer
{
    protected final BestDispatchFinder dispatchFinder;

    protected final InclusionIdleTaxiZonalRegistry idleTaxiRegistry;
    private final UnplannedRequestZonalRegistry unplannedRequestRegistry;

    private final InclusionRuleBasedTaxiOptimizerParams params;


    public InclusionRuleBasedTaxiOptimizer(TaxiOptimizerContext optimContext,
    		InclusionRuleBasedTaxiOptimizerParams params)
    {
        this(optimContext, params, new SquareGridSystem(optimContext.network, params.cellSize));
    }


    public InclusionRuleBasedTaxiOptimizer(TaxiOptimizerContext optimContext,
    		InclusionRuleBasedTaxiOptimizerParams params, ZonalSystem zonalSystem)
    {
        super(optimContext, params, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR), false, false);

        this.params = params;

        if (optimContext.scheduler.getParams().vehicleDiversion) {
            throw new RuntimeException("Diversion is not supported by RuleBasedTaxiOptimizer");
        }

        dispatchFinder = new BestDispatchFinder(optimContext);
        idleTaxiRegistry = new InclusionIdleTaxiZonalRegistry(zonalSystem, optimContext.scheduler,params.INCLUSION_TAXI_PREFIX);
        unplannedRequestRegistry = new UnplannedRequestZonalRegistry(zonalSystem);
    }


    @Override
    protected void scheduleUnplannedRequests()
    {
     
            scheduleUnplannedRequestsImpl();//reduce T_W (regular NOS), for the time being, no overloads expected
        
    }

    //request-initiated scheduling
    private void scheduleUnplannedRequestsImpl()
    {
        int idleCount = idleTaxiRegistry.getVehicleCount();

        Iterator<TaxiRequest> reqIter = getUnplannedRequests().iterator();
        while (reqIter.hasNext() && idleCount > 0) {
            TaxiRequest req = reqIter.next();
            boolean barrierFreeRequest = req.getPassenger().getId().toString().startsWith(params.INCLUSION_CUSTOMER_PREFIX)? true : false;
            Iterable<Vehicle> selectedVehs = idleTaxiRegistry.findNearestVehicles(req.getFromLink().getFromNode(),barrierFreeRequest);

            if (barrierFreeRequest){
//            	Logger.getLogger(getClass()).info("barrier free request for : "+req.getPassenger().getId()+". Assigned Vehicles: "+selectedVehs.toString());
            }

            BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder
                    .findBestVehicleForRequest(req, selectedVehs);
            
            if (best!=null){
            getOptimContext().scheduler.scheduleRequest(best.vehicle, best.destination, best.path);

            reqIter.remove();
            unplannedRequestRegistry.removeRequest(req);
            idleCount--;
            }
        }
    }





    @Override
    public void requestSubmitted(Request request)
    {
        super.requestSubmitted(request);
        unplannedRequestRegistry.addRequest((TaxiRequest)request);
    }


    @Override
    public void nextTask(Vehicle vehicle)
    {
        super.nextTask(vehicle);

        Schedule schedule = vehicle.getSchedule();
        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            TaxiStayTask lastTask = (TaxiStayTask)Schedules.getLastTask(schedule);
            if (lastTask.getBeginTime() < vehicle.getServiceEndTime()) {
                idleTaxiRegistry.removeVehicle(vehicle);
            }
        }
        else if (getOptimContext().scheduler.isIdle(vehicle)) {
            idleTaxiRegistry.addVehicle(vehicle);
        }
        else {
            if (schedule.getCurrentTask().getTaskIdx() != 0) {//not first task
                TaxiTask previousTask = (TaxiTask)Schedules.getPreviousTask(schedule);
                if (isWaitStay(previousTask)) {
                    idleTaxiRegistry.removeVehicle(vehicle);
                }
            }
        }
    }


    @Override
    protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask)
    {
        return isWaitStay(newCurrentTask);
    }


    protected boolean isWaitStay(TaxiTask task)
    {
        return task.getTaxiTaskType() == TaxiTaskType.STAY;
    }
}
