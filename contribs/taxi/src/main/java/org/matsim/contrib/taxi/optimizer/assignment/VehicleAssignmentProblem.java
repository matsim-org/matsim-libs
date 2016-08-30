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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData.DestEntry;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.Maps;


public class VehicleAssignmentProblem<D>
{
    public static interface AssignmentCost<D>
    {
        double calc(VehicleData.Entry departure, DestEntry<D> dest, PathData pathData);
    }


    public static class PathData
    {
        private Node node;//destination
        private double delay;//at both the first and last link
        private Path path;//shortest path


        public Node getNode()
        {
            return node;
        }


        public double getDelay()
        {
            return delay;
        }


        public Path getPath()
        {
            return path;
        }
    }


    private final TravelTime travelTime;
    private final FastMultiNodeDijkstra router;
    private final BackwardFastMultiNodeDijkstra backwardRouter;

    private final StraightLineKnnFinder<VehicleData.Entry, DestEntry<D>> destinationFinder;
    private final StraightLineKnnFinder<DestEntry<D>, VehicleData.Entry> vehicleFinder;

    private AssignmentCost<D> assignmentCost;
    private VehicleData vData;
    private AssignmentDestinationData<D> dData;


    public VehicleAssignmentProblem(TravelTime travelTime, FastMultiNodeDijkstra router,
            BackwardFastMultiNodeDijkstra backwardRouter)
    {
        this(travelTime, router, backwardRouter, null, null);
    }


    public VehicleAssignmentProblem(TravelTime travelTime, FastMultiNodeDijkstra router,
            BackwardFastMultiNodeDijkstra backwardRouter,
            StraightLineKnnFinder<VehicleData.Entry, DestEntry<D>> destinationFinder,
            StraightLineKnnFinder<DestEntry<D>, VehicleData.Entry> vehicleFinder)
    {
        this.travelTime = travelTime;
        this.router = router;
        this.backwardRouter = backwardRouter;
        this.destinationFinder = destinationFinder;
        this.vehicleFinder = vehicleFinder;
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
            Node fromNode = departure.link.getToNode();

            //TODO this kNN is slow
            List<DestEntry<D>> filteredDests = destinationFinder == null ? dData.getEntries()
                    : destinationFinder.findNearest(departure, dData.getEntries());

            Map<Id<Node>, InitialNode> destInitialNodes = Maps
                    .newHashMapWithExpectedSize(filteredDests.size());
            Map<Id<Node>, Path> pathsToDestNodes = Maps
                    .newHashMapWithExpectedSize(filteredDests.size());

            for (DestEntry<D> dest : filteredDests) {
                int d = dest.idx;
                PathData pathData = pathDataMatrix[v][d] = new PathData();

                if (departure.link == dest.link) {
                    //hack: we are basically there (on the same link), so let's use dest.link.toNode (== fromNode)
                    pathData.node = fromNode;
                    pathData.delay = 0;
                }
                else {
                    pathData.node = dest.link.getFromNode();
                    //simplified, but works for taxis, since pickup trips are short (about 5 mins)
                    //TODO delay can be computed after path search...
                    pathData.delay = 1 + dest.link.getFreespeed(departure.time);
                }

                if (!destInitialNodes.containsKey(pathData.node.getId())) {
                    InitialNode newInitialNode = new InitialNode(pathData.node, 0, 0);
                    destInitialNodes.put(pathData.node.getId(), newInitialNode);
                }
            }

            ImaginaryNode toNodes = router.createImaginaryNode(destInitialNodes.values());
            Path path = router.calcLeastCostPath(fromNode, toNodes, departure.time, null, null);
            Node bestDestNode = path.nodes.get(path.nodes.size() - 1);
            pathsToDestNodes.put(bestDestNode.getId(), path);

            //get paths for all remaining destNodes 
            for (InitialNode i : destInitialNodes.values()) {
                Node destNode = i.node;
                if (destNode.getId() != bestDestNode.getId()) {
                    path = router.constructPath(fromNode, destNode, departure.time);
                    pathsToDestNodes.put(destNode.getId(), path);
                }
            }

            for (DestEntry<D> dest : filteredDests) {
                int d = dest.idx;
                PathData pathData = pathDataMatrix[v][d];
                pathData.path = pathsToDestNodes.get(pathData.node.getId());
            }
        }
    }


    //TODO does not support adv reqs
    private void calcPathsForDestinations(PathData[][] pathDataMatrix)
    {
        for (int d = 0; d < dData.getSize(); d++) {
            DestEntry<D> dest = dData.getEntry(d);
            Node toNode = dest.link.getFromNode();

            //TODO this kNN is slow
            List<VehicleData.Entry> filteredVehs = vehicleFinder == null ? vData.getEntries()
                    : vehicleFinder.findNearest(dest, vData.getEntries());

            Map<Id<Node>, InitialNode> vehInitialNodes = Maps
                    .newHashMapWithExpectedSize(filteredVehs.size());
            Map<Id<Node>, Path> pathsFromVehNodes = Maps
                    .newHashMapWithExpectedSize(filteredVehs.size());

            for (VehicleData.Entry departure : filteredVehs) {
                int v = departure.idx;
                PathData pathData = pathDataMatrix[v][d] = new PathData();

                if (departure.link == dest.link) {
                    //hack: we are basically there (on the same link), so let's use dset.link.fromNode (== toNode)
                    pathData.node = toNode;
                    pathData.delay = 0;
                }
                else {
                    pathData.node = departure.link.getToNode();
                    //simplified, but works for taxis, since pickup trips are short (about 5 mins)
                    //TODO delay can be computed after path search...
                    pathData.delay = 1 + dest.link.getFreespeed(departure.time);
                }

                if (!vehInitialNodes.containsKey(pathData.node.getId())) {
                    InitialNode newInitialNode = new InitialNode(pathData.node, 0, 0);
                    vehInitialNodes.put(pathData.node.getId(), newInitialNode);
                }
            }

            ImaginaryNode fromNodes = backwardRouter.createImaginaryNode(vehInitialNodes.values());
            Path path = backwardRouter.calcLeastCostPath(toNode, fromNodes, dest.time, null, null);
            Node bestVehNode = path.nodes.get(path.nodes.size() - 1);
            pathsFromVehNodes.put(bestVehNode.getId(), path);

            //get paths for all remaining endNodes 
            for (InitialNode i : vehInitialNodes.values()) {
                Node vehNode = i.node;
                if (vehNode.getId() != bestVehNode.getId()) {
                    path = backwardRouter.constructPath(toNode, vehNode, dest.time);
                    pathsFromVehNodes.put(vehNode.getId(), path);
                }
            }

            for (VehicleData.Entry departure : filteredVehs) {
                int v = departure.idx;
                PathData pathData = pathDataMatrix[v][d];
                pathData.path = pathsFromVehNodes.get(pathData.node.getId());
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
                    VrpPaths.calcAndCreatePath(departure.link, dest.link, departure.time, router,
                            travelTime)
                    : VrpPaths.createPath(departure.link, dest.link, departure.time, pathData.path,
                            travelTime);

            dispatches.add(new Dispatch<>(departure.vehicle, dest.destination, vrpPath));
        }

        return dispatches;
    }
}
