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

import java.util.concurrent.atomic.AtomicReference;

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

	private volatile double lastTreeUpdate = Double.NEGATIVE_INFINITY;
	private final AtomicReference<STRtree> treeRef = new AtomicReference<>(new STRtree());

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
		maybeUpdateTree(now);
		return filterEntries(vehicleEntries, drtRequest);
	}

	private synchronized void maybeUpdateTree(double now) {
		if (now >= lastTreeUpdate + updateInterval) {
			STRtree newTree = buildTree();
			treeRef.set(newTree);
			lastTreeUpdate = now;
		}
	}

	private Collection<VehicleEntry> filterEntries(Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries, DrtRequest drtRequest) {
		Collection<VehicleEntry> result = Collections.emptyList();
		Point point = GeometryUtils.createGeotoolsPoint(drtRequest.getFromLink().getToNode().getCoord());
		STRtree tree = treeRef.get();

		for (double expansion = minExpansion; expansion <= maxExpansion && result.size() < minCandidates; expansion *= expansionIncrementFactor) {
			Envelope envelopeInternal = point.getEnvelopeInternal();
			envelopeInternal.expandBy(expansion);
			List<?> ids = tree.query(envelopeInternal);
			result = extract(vehicleEntries, ids);
		}

		if (result.size() < minCandidates) {
			return returnAllIfEmpty ? vehicleEntries.values() : Collections.emptySet();
		}

		return result;
	}

	private Collection<VehicleEntry> extract(Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries, List<?> result) {
		Set<VehicleEntry> extracted = new LinkedHashSet<>();
		for (Object obj : result) {
			Id<DvrpVehicle> dvrpVehicleId = (Id<DvrpVehicle>) obj;
			if (vehicleEntries.containsKey(dvrpVehicleId)) {
				extracted.add(vehicleEntries.get(dvrpVehicleId));
			}
		}
		return extracted;
	}

	private STRtree buildTree() {
		STRtree newTree = new STRtree();
		for (DvrpVehicle vehicle : fleet.getVehicles().values()) {
			Schedule schedule = vehicle.getSchedule();
			Task startTask;

			if (schedule.getStatus() == Schedule.ScheduleStatus.STARTED) {
				startTask = schedule.getCurrentTask();

				switch (startTask) {
					case StayTask stayTask -> insertVehicleInTree(newTree, vehicle, stayTask.getLink().getCoord());
					case DriveTask driveTask -> {
						var diversionPoint = ((OnlineDriveTaskTracker) driveTask.getTaskTracker()).getDiversionPoint();
						var link = diversionPoint != null ? diversionPoint.link : driveTask.getPath().getToLink();
						insertVehicleInTree(newTree, vehicle, link.getCoord());
					}
					case OperationalStop operationalStop -> {
						var coord = operationalStop.getFacility().getCoord();
						insertVehicleInTree(newTree, vehicle, coord);
					}
					case null -> throw new RuntimeException("Current task is null for schedule " + schedule + " for vehicle " + vehicle);
					default -> throw new RuntimeException("Unknown task type: " + startTask.getClass());
				}
			}
		}
		return newTree;
	}

	private static void insertVehicleInTree(STRtree tree, DvrpVehicle vehicle, Coord coord) {
		tree.insert(GeometryUtils.createGeotoolsPoint(coord).getEnvelopeInternal(), vehicle.getId());
	}
}
