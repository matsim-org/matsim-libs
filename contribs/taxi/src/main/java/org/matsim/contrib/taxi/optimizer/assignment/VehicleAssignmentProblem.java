/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import java.lang.reflect.Array;
import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData.DestEntry;
import org.matsim.core.router.*;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.Lists;


public class VehicleAssignmentProblem<D>
{
    public static interface AssignmentCost<D>
    {
        double calc(VehicleData.Entry departure, DestEntry<D> dest, PathData pathData);
    }


    private final TravelTime travelTime;
    private final FastAStarEuclidean euclideanRouter;

    private final OneToManyPathSearch forwardPathSearch;
    private final OneToManyPathSearch backwardPathSearch;

    private final LinkProvider<DestEntry<D>> destLinkProvider = LinkProviders
            .createDestEntryToLink();

    private final StraightLineKnnFinder<VehicleData.Entry, DestEntry<D>> destinationFinder;
    private final StraightLineKnnFinder<DestEntry<D>, VehicleData.Entry> vehicleFinder;

    private AssignmentCost<D> assignmentCost;
    private VehicleData vData;
    private AssignmentDestinationData<D> dData;


    public VehicleAssignmentProblem(TravelTime travelTime, FastMultiNodeDijkstra router,
            BackwardFastMultiNodeDijkstra backwardRouter)
    {
        //we do not need Euclidean router when there is not kNN filtering
        this(travelTime, router, backwardRouter, null, -1, -1);
    }


    public VehicleAssignmentProblem(TravelTime travelTime, FastMultiNodeDijkstra router,
            BackwardFastMultiNodeDijkstra backwardRouter, FastAStarEuclidean euclideanRouter,
            int nearestDestinationLimit, int nearestVehicleLimit)
    {
        this.travelTime = travelTime;
        this.euclideanRouter = euclideanRouter;

        forwardPathSearch = OneToManyPathSearch.createForwardSearch(router);
        backwardPathSearch = OneToManyPathSearch.createBackwardSearch(backwardRouter);

        //TODO this kNN is slow
        destinationFinder = StraightLineKnnFinders.createDestEntryFinder(nearestDestinationLimit);
        vehicleFinder = StraightLineKnnFinders.createVehicleDepartureFinder(nearestVehicleLimit);
    }


    public List<Dispatch<D>> findAssignments(VehicleData vData, AssignmentDestinationData<D> dData,
            AssignmentCost<D> assignmentCost)
    {
        this.vData = vData;
        this.dData = dData;
        this.assignmentCost = assignmentCost;

        PathData[][] pathDataMatrix = createPathDataMatrix();
        double[][] costMatrix = createCostMatrix(pathDataMatrix);
        int[] assignments = new HungarianAlgorithm(costMatrix).execute();
        return createDispatches(assignments, pathDataMatrix, travelTime);
    }


    //private static int calcPathsForVehiclesCount = 0;
    //private static int calcPathsForDestinationsCount = 0;

    private PathData[][] createPathDataMatrix()
    {
        PathData[][] pathDataMatrix = (PathData[][])Array.newInstance(PathData.class,
                vData.getSize(), dData.getSize());

        if (dData.getSize() > vData.getSize()) {
            calcPathsForVehicles(pathDataMatrix);
            //calcPathsForVehiclesCount++;
        }
        else {
            calcPathsForDestinations(pathDataMatrix);
            //calcPathsForDestinationsCount++;
        }

        //if ( (calcPathsForDestinationsCount + calcPathsForVehiclesCount) % 100 == 0) {
        //    System.err.println("PathsForDestinations = " + calcPathsForDestinationsCount
        //        + " PathsForVehicles = " + calcPathsForVehiclesCount);
        //    System.err.println("dests = " + dData.getSize() + " vehs = " + vData.getSize()
        //        + " idleVehs = " + vData.getIdleCount());
        //}

        return pathDataMatrix;
    }


    private void calcPathsForVehicles(PathData[][] pathDataMatrix)
    {
        for (int v = 0; v < vData.getSize(); v++) {
            VehicleData.Entry departure = vData.getEntry(v);

            List<DestEntry<D>> filteredDests = destinationFinder == null ? dData.getEntries()
                    : destinationFinder.findNearest(departure, dData.getEntries());
            List<Link> toLinks = Lists.transform(filteredDests, destLinkProvider);
            PathData[] paths = forwardPathSearch.calcPaths(departure.link, toLinks, departure.time);

            for (int i = 0; i < filteredDests.size(); i++) {
                int d = filteredDests.get(i).idx;
                pathDataMatrix[v][d] = paths[i];
            }
        }
    }


    //TODO does not support adv reqs
    private void calcPathsForDestinations(PathData[][] pathDataMatrix)
    {
        for (int d = 0; d < dData.getSize(); d++) {
            DestEntry<D> dest = dData.getEntry(d);

            List<VehicleData.Entry> filteredVehs = vehicleFinder == null ? vData.getEntries()
                    : vehicleFinder.findNearest(dest, vData.getEntries());
            List<Link> toLinks = Lists.transform(filteredVehs, LinkProviders.VEHICLE_ENTRY_TO_LINK);
            PathData[] paths = backwardPathSearch.calcPaths(dest.link, toLinks, dest.time);

            for (int i = 0; i < filteredVehs.size(); i++) {
                int v = filteredVehs.get(i).idx;
                pathDataMatrix[v][d] = paths[i];
            }
        }
    }


    private double[][] createCostMatrix(PathData[][] pathDataMatrix)
    {

        double[][] costMatrix = new double[vData.getSize()][dData.getSize()];

        for (int v = 0; v < vData.getSize(); v++) {
            VehicleData.Entry departure = vData.getEntry(v);
            for (int r = 0; r < dData.getSize(); r++) {
                costMatrix[v][r] = assignmentCost.calc(departure, dData.getEntry(r),
                        pathDataMatrix[v][r]);
            }
        }

        return costMatrix;
    }


    private List<Dispatch<D>> createDispatches(int[] assignments, PathData[][] pathDataMatrix,
            TravelTime travelTime)
    {
        List<Dispatch<D>> dispatches = new ArrayList<>(Math.min(vData.getSize(), dData.getSize()));
        for (int v = 0; v < assignments.length; v++) {
            int d = assignments[v];
            if (d == -1 || //no request assigned
                    d >= dData.getSize()) {// non-existing (dummy) request assigned
                continue;
            }

            VehicleData.Entry departure = vData.getEntry(v);
            DestEntry<D> dest = dData.getEntry(d);
            PathData pathData = pathDataMatrix[v][d];

            //TODO if null is frequent we may be more efficient by increasing the neighbourhood
            VrpPathWithTravelData vrpPath = pathData == null ? //
                    VrpPaths.calcAndCreatePath(departure.link, dest.link, departure.time,
                            euclideanRouter, travelTime)
                    : VrpPaths.createPath(departure.link, dest.link, departure.time, pathData.path,
                            travelTime);

            dispatches.add(new Dispatch<>(departure.vehicle, dest.destination, vrpPath));
        }

        return dispatches;
    }
}
