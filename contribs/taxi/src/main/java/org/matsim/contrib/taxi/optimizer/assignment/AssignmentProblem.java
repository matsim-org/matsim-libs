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

package org.matsim.contrib.taxi.optimizer.assignment;

import java.util.*;

import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData.DestEntry;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerUtils;
import org.matsim.core.router.FastMultiNodeDijkstra;

import com.google.common.collect.Iterables;


public class AssignmentProblem
    extends AbstractAssignmentProblem<TaxiRequest>
{
    private final TaxiOptimizerContext optimContext;
    private final AssignmentTaxiOptimizerParams params;

    private AssignmentRequestData rData;


    public AssignmentProblem(TaxiOptimizerContext optimContext,
            AssignmentTaxiOptimizerParams params, FastMultiNodeDijkstra router,
            BackwardFastMultiNodeDijkstra backwardRouter)
    {
        super(router, backwardRouter,
                StraightLineKNNFinders.createTaxiRequestFinder(params.nearestRequestsLimit),
                StraightLineKNNFinders.createVehicleDepartureFinder(params.nearestVehiclesLimit));
        this.optimContext = optimContext;
        this.params = params;
    }


    public List<Dispatch<TaxiRequest>> findAssignments(SortedSet<TaxiRequest> unplannedRequests)
    {
        if (initDataAndCheckIfSchedulingRequired(unplannedRequests)) {
            PathData[][] pathDataMatrix = createPathDataMatrix();
            double[][] costMatrix = createCostMatrix(pathDataMatrix);
            int[] assignments = new HungarianAlgorithm(costMatrix).execute();
            return createDispatches(assignments, pathDataMatrix,
                    optimContext.travelTime);
        }
        else {
            return Collections.emptyList();
        }
    }


    private boolean initDataAndCheckIfSchedulingRequired(SortedSet<TaxiRequest> unplannedRequests)
    {
        dData = rData = new AssignmentRequestData(optimContext, 0);//only immediate reqs
        dData.init(unplannedRequests);
        if (dData.getSize() == 0) {
            return false;
        }

        int idleVehs = Iterables.size(Iterables.filter(optimContext.taxiData.getVehicles().values(),
                TaxiSchedulerUtils.createIsIdle(optimContext.scheduler)));

        if (idleVehs < rData.getUrgentReqCount()) {
            vData = new VehicleData(optimContext, params.vehPlanningHorizonUndersupply);
        }
        else {
            vData = new VehicleData(optimContext, params.vehPlanningHorizonOversupply);
        }

        return vData.dimension > 0;
    }


    public enum Mode
    {
        PICKUP_TIME, //
        //TTki

        ARRIVAL_TIME, //
        //DEPk + TTki
        //equivalent to REMAINING_WAIT_TIME, i.e. DEPk + TTki - Tcurr // TODO check this out for dummy cases

        TOTAL_WAIT_TIME, //
        //DEPk + TTki - T0i

        DSE;//
        //balance between demand (ARRIVAL_TIME) and supply (PICKUP_TIME)
    };


    private Mode currentMode;


    @Override
    protected double[][] createCostMatrix(
            org.matsim.contrib.taxi.optimizer.assignment.AbstractAssignmentProblem.PathData[][] pathDataMatrix)
    {
        currentMode = getCurrentMode();
        return super.createCostMatrix(pathDataMatrix);
    }


    @Override
    protected double calcCost(VehicleData.Entry departure, DestEntry<TaxiRequest> dest,
            PathData pathData)
    {
        double travelTime = pathData == null ? //
                params.nullPathCost : // no path (too far away)
                pathData.delay + pathData.path.travelTime;

        double pickupBeginTime = Math.max(dest.destination.getT0(), departure.time + travelTime);

        switch (currentMode) {
            case PICKUP_TIME:
                //this will work different than ARRIVAL_TIME at oversupply -> will reduce T_P and fairness
                return pickupBeginTime - departure.time;

            case ARRIVAL_TIME:
                //less fairness, higher throughput
                return pickupBeginTime;

            case TOTAL_WAIT_TIME:
                //more fairness, lower throughput
                //this will work different than than ARRIVAL_TIME at undersupply -> will reduce unfairness and throughput 
                return pickupBeginTime - dest.destination.getT0();

            default:
                throw new IllegalStateException();
        }
    }


    private Mode getCurrentMode()
    {
        if (params.mode != Mode.DSE) {
            return params.mode;
        }
        else {
            return rData.getUrgentReqCount() > vData.idleCount ? Mode.PICKUP_TIME : //we have too few vehicles
                    Mode.ARRIVAL_TIME; //we have too many vehicles
        }
    }
}
