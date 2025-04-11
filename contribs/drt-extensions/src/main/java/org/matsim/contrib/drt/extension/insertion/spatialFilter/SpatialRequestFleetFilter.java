/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.insertion.spatialFilter;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.RequestFleetFilter;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.utils.geometry.GeometryUtils;

import java.util.*;

    import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.getBaseTypeOrElseThrow;

/**
 * Filter that periodically updates a spatial search tree with current vehicle positions.
 * For a given request, only returns "nearby" vehicles.
 * Suitable for large scenarios with a certain degree of spatial coverage
 * Reduces insertion generation downstream.
 *
 * The spatial filter will start with a minimum expansion around the request origin and will
 * iteratively expand further by the increment factor until either the maximum expansion or
 * a minimum number of candidates is found.
 *
 *
 * @author nuehnel / MOIA
 */
public class SpatialRequestFleetFilter implements RequestFleetFilter {

    private double lastTreeUpdate = Double.NEGATIVE_INFINITY;
    private STRtree tree = new STRtree();

    private final Fleet fleet;
    private final MobsimTimer mobsimTimer;
    private final double expansionIncrementFactor;
    private final double maxExpansion;
    private final double minExpansion;

    private final boolean returnAllIfEmpty;

    private final int minCandidates;

    private final double updateInterval;

    public SpatialRequestFleetFilter(Fleet fleet, MobsimTimer mobsimTimer,
                                     DrtSpatialRequestFleetFilterParams params) {
        this.fleet = fleet;
        this.mobsimTimer = mobsimTimer;
        this.expansionIncrementFactor = params.getExpansionFactor();
        this.minExpansion = params.getMinExpansion();
        this.maxExpansion = params.getMaxExpansion();
        this.returnAllIfEmpty = params.isReturnAllIfEmpty();
        this.minCandidates = params.getMinCandidates();
        this.updateInterval = params.getUpdateInterval();
    }

    @Override
    public Collection<VehicleEntry> filter(DrtRequest drtRequest, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries, double now) {
        if (now >= lastTreeUpdate + updateInterval) {
            buildTree();
            // Alternative (to stick cadence to a "grid"):
            // lastTreeUpdate = Math.floor(now / updateInterval) * updateInterval;
            lastTreeUpdate = now;
        }
        return filterEntries(vehicleEntries, drtRequest);
    }

    private Collection<VehicleEntry> filterEntries(Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries, DrtRequest drtRequest) {
        Collection<VehicleEntry> result = Collections.emptyList();
        Point point = GeometryUtils.createGeotoolsPoint(drtRequest.getFromLink().getToNode().getCoord());

        for (double expansion = minExpansion; expansion <= maxExpansion && result.size() < minCandidates; expansion*= expansionIncrementFactor) {
            Envelope envelopeInternal = point.getEnvelopeInternal();
            envelopeInternal.expandBy(expansion);
            var ids = tree.query(envelopeInternal);
            result = extract(vehicleEntries, ids);
        }

        if(result.size() < minCandidates) {
            if(returnAllIfEmpty) {
                return vehicleEntries.values();
            }
            return Collections.emptySet();
        }

        return result;
    }

    private Collection<VehicleEntry> extract(Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries, List<Id<DvrpVehicle>> result) {
        Set<VehicleEntry> extracted = new LinkedHashSet<>();
        for (Id<DvrpVehicle> dvrpVehicleId : result) {
            // VehicleEntries only contains available vehicles. The spatial tree might be out of sync with this set of
            // vehicles and contain vehicles that are not available anymore. Hence, the need to check.
            if (vehicleEntries.containsKey(dvrpVehicleId)) {
                extracted.add(vehicleEntries.get(dvrpVehicleId));
            }
        }
        return extracted;
    }

    private void buildTree() {
        tree = new STRtree();
        for (DvrpVehicle vehicle : fleet.getVehicles().values()) {
            Schedule schedule = vehicle.getSchedule();
            Task startTask;

            if (schedule.getStatus() == Schedule.ScheduleStatus.STARTED) {
                startTask = schedule.getCurrentTask();

                switch (startTask) {
                    case StayTask stayTask -> insertVehicleInTree(tree, vehicle, stayTask.getLink().getCoord());
                    case DriveTask driveTask -> {
                        var diversionPoint = ((OnlineDriveTaskTracker) driveTask.getTaskTracker()).getDiversionPoint();
                        var link = diversionPoint != null ? diversionPoint.link : //diversion possible
                            driveTask.getPath().getToLink();// too late for diversion


                        insertVehicleInTree(tree, vehicle, link.getCoord());
                    }
                    case OperationalStop operationalStop -> {
                        var coord = operationalStop.getFacility().getCoord();
                        insertVehicleInTree(tree, vehicle, coord);
                    }
                    case null -> throw new RuntimeException("Current task is null for schedule "+schedule+" for vehicle "+vehicle);
                    default -> throw new RuntimeException("Unknown task type: " + startTask.getClass());
                }
            }
        }
    }

    private static void insertVehicleInTree(STRtree tree, DvrpVehicle vehicle, Coord coord) {
        tree.insert(GeometryUtils.createGeotoolsPoint(coord).getEnvelopeInternal(), vehicle.getId());
    }
}
