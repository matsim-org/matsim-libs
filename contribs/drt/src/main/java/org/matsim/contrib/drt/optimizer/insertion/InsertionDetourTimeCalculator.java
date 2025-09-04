package org.matsim.contrib.drt.optimizer.insertion;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

import jakarta.annotation.Nullable;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.InsertionPoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.drt.stops.StopTimeCalculator.Dropoff;
import org.matsim.contrib.drt.stops.StopTimeCalculator.Pickup;

import com.google.common.base.MoreObjects;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionDetourTimeCalculator {

	private final StopTimeCalculator stopTimeCalculator;

	// If the detour data uses approximated detour times (e.g. beeline or matrix-based), provide the same estimator for
	// replaced drives to avoid systematic errors
	// When null, the replaced drive duration is derived from the schedule
	@Nullable
	private final DetourTimeEstimator replacedDriveTimeEstimator;

	public InsertionDetourTimeCalculator(StopTimeCalculator stopTimeCalculator,
			@Nullable DetourTimeEstimator replacedDriveTimeEstimator) {
		this.stopTimeCalculator = stopTimeCalculator;
		this.replacedDriveTimeEstimator = replacedDriveTimeEstimator;
	}

	public DetourTimeInfo calculateDetourTimeInfo(Insertion insertion, InsertionDetourData detourData, DrtRequest drtRequest) {
		var followedByDropoff = insertion.pickup.index == insertion.dropoff.index;

		double toPickupTT = detourData.detourToPickup.getTravelTime();
		double fromPickupTT = detourData.detourFromPickup.getTravelTime();
		var pickupDetourInfo = calcPickupDetourInfo(insertion.vehicleEntry, insertion.pickup, toPickupTT, fromPickupTT,
				followedByDropoff, drtRequest);

		double toDropoffTT = followedByDropoff ? fromPickupTT : detourData.detourToDropoff.getTravelTime();
		double fromDropoffTT = detourData.detourFromDropoff.getTravelTime();
		var dropoffDetourInfo = calcDropoffDetourInfo(insertion, toDropoffTT, fromDropoffTT, pickupDetourInfo, drtRequest);

		return new DetourTimeInfo(pickupDetourInfo, dropoffDetourInfo);
	}

	public PickupDetourInfo calcPickupDetourInfo(VehicleEntry vEntry, InsertionPoint pickup, double toPickupTT,
												 double fromPickupTT, boolean followedByDropoff, DrtRequest drtRequest) {
		if (followedByDropoff) {
			//handle the pickup->dropoff case separately
			return calcPickupDetourInfoIfPickupToDropoffDetour(vEntry, pickup, toPickupTT, fromPickupTT, drtRequest);
		}

		double toPickupDepartureTime = pickup.previousWaypoint.getDepartureTime();
		if (pickup.newWaypoint.getLink() == pickup.previousWaypoint.getLink()) {
			// no detour, but possible additional stop duration
			var info = calculatePickupIfSameLink(vEntry, pickup.index, toPickupDepartureTime, drtRequest);
			return new PickupDetourInfo(info.vehicleDepartureTime, info.requestPickupTime, info.additionalStopDuration);
		}

		double arrivalTime = toPickupDepartureTime + toPickupTT;
		Pickup pickupTime = stopTimeCalculator.initEndTimeForPickup(vEntry.vehicle, arrivalTime, drtRequest);
		double stopDuration = pickupTime.endTime() - arrivalTime;
		double replacedDriveTT = calculateReplacedDriveDuration(vEntry, pickup.index, toPickupDepartureTime);
		double pickupTimeLoss = toPickupTT + stopDuration + fromPickupTT - replacedDriveTT;
		return new PickupDetourInfo(pickupTime.endTime(), pickupTime.pickupTime(), pickupTimeLoss);
	}

	public DropoffDetourInfo calcDropoffDetourInfo(Insertion insertion, double toDropoffTT, double fromDropoffTT,
			PickupDetourInfo pickupDetourInfo, DrtRequest drtRequest) {
		if (insertion.pickup.index == insertion.dropoff.index) {
			//handle the pickup->dropoff case separately
			return calcDropoffDetourInfoIfPickupToDropoffDetour(toDropoffTT, fromDropoffTT, pickupDetourInfo, insertion.vehicleEntry, drtRequest);
		}

		InsertionPoint dropoff = insertion.dropoff;
		VehicleEntry vEntry = insertion.vehicleEntry;

		if (dropoff.newWaypoint.getLink() == dropoff.previousWaypoint.getLink()) {
			double remainingPickupTimeLoss = calculateRemainingPickupTimeLossAtDropoff(insertion, pickupDetourInfo);
			double arrivalTime = dropoff.previousWaypoint.getArrivalTime() + remainingPickupTimeLoss;

			DrtStopTask stopTask = findStopTaskIfSameLinkAsPrevious(vEntry, dropoff.index);
			Dropoff dropoffTime = stopTimeCalculator.updateEndTimeForDropoff(vEntry.vehicle, stopTask, arrivalTime, drtRequest);
			double departureTime = dropoffTime.endTime();
			
			double initialStopDuration = stopTask.getEndTime() - stopTask.getBeginTime();
			double additionalStopDuration = departureTime - arrivalTime - initialStopDuration;
			
			return new DropoffDetourInfo(arrivalTime, dropoffTime.dropoffTime(), additionalStopDuration);
		}

		double toDropoffDepartureTime = dropoff.previousWaypoint.getDepartureTime();
		double remainingPickupTimeLoss = calculateRemainingPickupTimeLossAtDropoff(insertion, pickupDetourInfo);
		double arrivalTime = toDropoffDepartureTime + remainingPickupTimeLoss + toDropoffTT;
		Dropoff dropoffTime = stopTimeCalculator.initEndTimeForDropoff(vEntry.vehicle, arrivalTime, drtRequest);
		double departureTime = dropoffTime.endTime();
		double stopDuration = departureTime - arrivalTime;
		double replacedDriveTT = calculateReplacedDriveDuration(vEntry, dropoff.index, toDropoffDepartureTime);
		double dropoffTimeLoss = toDropoffTT + stopDuration + fromDropoffTT - replacedDriveTT;
		return new DropoffDetourInfo(arrivalTime, dropoffTime.dropoffTime(), dropoffTimeLoss);
	}

	private PickupDetourInfo calcPickupDetourInfoIfPickupToDropoffDetour(VehicleEntry vEntry, InsertionPoint pickup,
			double toPickupTT, double fromPickupTT, DrtRequest drtRequest) {
		double toPickupDepartureTime = pickup.previousWaypoint.getDepartureTime();

		final PickupTimeInfo info;
		
		if (pickup.newWaypoint.getLink() == pickup.previousWaypoint.getLink()) {
			// no drive to pickup, but possible additional stop duration
			info = calculatePickupIfSameLink(vEntry, pickup.index, toPickupDepartureTime, drtRequest);
		} else {
			Pickup pickupTime = stopTimeCalculator.initEndTimeForPickup(vEntry.vehicle, toPickupDepartureTime + toPickupTT, drtRequest);
			double additionalStopDuration = pickupTime.endTime() - toPickupDepartureTime - toPickupTT;
			info = new PickupTimeInfo(pickupTime.endTime(), pickupTime.pickupTime(), additionalStopDuration);
		}

		double replacedDriveTT = calculateReplacedDriveDuration(vEntry, pickup.index, toPickupDepartureTime);
		double pickupTimeLoss = toPickupTT + info.additionalStopDuration + fromPickupTT - replacedDriveTT;
		return new PickupDetourInfo(info.vehicleDepartureTime, info.requestPickupTime, pickupTimeLoss);
	}

	private DropoffDetourInfo calcDropoffDetourInfoIfPickupToDropoffDetour(double fromPickupToDropoffTT,
			double fromDropoffTT, PickupDetourInfo pickupDetourInfo, VehicleEntry vEntry, DrtRequest drtRequest) {
		double arrivalTime = pickupDetourInfo.vehicleDepartureTime + fromPickupToDropoffTT;
		Dropoff dropoffTime = stopTimeCalculator.initEndTimeForDropoff(vEntry.vehicle, arrivalTime, drtRequest);
		double departureTime = dropoffTime.endTime();
		double stopDuration = departureTime - arrivalTime;
		double dropoffTimeLoss = stopDuration + fromDropoffTT;
		return new DropoffDetourInfo(arrivalTime, dropoffTime.dropoffTime(), dropoffTimeLoss);
	}

	private PickupTimeInfo calculatePickupIfSameLink(VehicleEntry vEntry, int pickupIndex, double toPickupDepartureTime, DrtRequest request) {
		DrtStopTask stopTask = findStopTaskIfSameLinkAsPrevious(vEntry, pickupIndex);

		if (stopTask == null) {
			// case 1: previous waypoint is a drive, we create a new stop
			// insertion time is the end time of previous waypoint
			Pickup pickupTime = stopTimeCalculator.initEndTimeForPickup(vEntry.vehicle, toPickupDepartureTime,
					request);
			double stopDuration = pickupTime.endTime() - toPickupDepartureTime;
			return new PickupTimeInfo(pickupTime.endTime(), pickupTime.pickupTime(), stopDuration);
		} else if (pickupIndex > 0) {
			// case 2: previous waypoint is a planned stop, not started yet
			// insertion time is the beginning of the planned stop
			double insertionTime = stopTask.getBeginTime();
			Pickup pickupTime = stopTimeCalculator.updateEndTimeForPickup(vEntry.vehicle, stopTask,
				insertionTime, request);
			double departureTime = pickupTime.endTime();
			double additionalStopDuration = departureTime - stopTask.getEndTime();
			return new PickupTimeInfo(departureTime, pickupTime.pickupTime(), additionalStopDuration);
		} else {
			// case 3: previous waypoint is an ongoing (started) stop
			// insertion is a soon as possible (now)
			Pickup pickupTime = stopTimeCalculator.updateEndTimeForPickup(vEntry.vehicle, stopTask, vEntry.createTime, request);
			double departureTime = pickupTime.endTime();
			double additionalStopDuration = departureTime - stopTask.getEndTime();
			return new PickupTimeInfo(departureTime, pickupTime.pickupTime(), additionalStopDuration);
		}
	}

	private DrtStopTask findStopTaskIfSameLinkAsPrevious(VehicleEntry vEntry, int insertionIdx) {
		if (insertionIdx == 0) {
			var startTask = vEntry.start.task;

			if (startTask.isPresent() && STOP.isBaseTypeOf(startTask.get())) {
				// case 1: we have a preceding and ongoing (started) stop task
				return (DrtStopTask) startTask.get();
			}
		} else {
			// case 2: we have a preceding (planned) stop task
			return (DrtStopTask) vEntry.stops.get(insertionIdx - 1).getTask();
		}

		return null; // otherwise, there is no stop task before
	}

	private double calculateReplacedDriveDuration(VehicleEntry vEntry, int insertionIdx, double detourStartTime) {
		if (vEntry.isAfterLastStop(insertionIdx)) {
			return 0;// end of route - bus would wait there
		}

		if (replacedDriveTimeEstimator != null) {
			//use the approximated drive times instead of deriving (presumably more accurate) times from the schedule
			return replacedDriveTimeEstimator.estimateTime(vEntry.getWaypoint(insertionIdx).getLink(),
					vEntry.getWaypoint(insertionIdx + 1).getLink(), detourStartTime);
		}

		double replacedDriveStartTime = vEntry.getWaypoint(insertionIdx).getDepartureTime();
		double replacedDriveEndTime = vEntry.stops.get(insertionIdx).getTask().getBeginTime();

		// reduce by the idle time before the next stop, to get the actual drive time
		return replacedDriveEndTime - replacedDriveStartTime - vEntry.getPrecedingStayTime(insertionIdx);
	}

	/*
	 * When inserting a pickup, we generate a "pickup loss" which describes by how
	 * much time we have to shift all following tasks to the future.
	 *
	 * In the case that some of the following stops are prebooked, however, there
	 * may be a stay time buffer between the insertion point and the stop. Hence, if
	 * a following stop only happens in four hours, we may not need to shift the
	 * task to the future. A preceding stay time, hence, reduces the introduced
	 * pickup loss.
	 *
	 * The present function calculates the remaining pickup loss at the dropoff
	 * insertion point after deducting all the stay times up to the dropoff.
	 */
	public static double calculateRemainingPickupTimeLossAtDropoff(Insertion insertion, PickupDetourInfo pickupDetourInfo) {
		VehicleEntry vEntry = insertion.vehicleEntry;
		double remainingPickupTimeLoss = pickupDetourInfo.pickupTimeLoss;

		for (int i = insertion.pickup.index + 1; i < insertion.dropoff.index; i++) {
			remainingPickupTimeLoss = Math.max(remainingPickupTimeLoss - vEntry.getPrecedingStayTime(i), 0.0);
		}

		return remainingPickupTimeLoss;
	}

	public static class PickupDetourInfo {
		// expected departure time for the vehicle from the stop
		public final double vehicleDepartureTime;
		// expected pickup time for the inserted request
		public final double requestPickupTime;
		// time delay of each stop placed after the pickup insertion point
		public final double pickupTimeLoss;

		public PickupDetourInfo(double vehicleDepartureTime, double requestPickupTime, double pickupTimeLoss) {
			this.vehicleDepartureTime = vehicleDepartureTime;
			this.requestPickupTime = requestPickupTime;
			this.pickupTimeLoss = pickupTimeLoss;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("vehicleDepartureTime", vehicleDepartureTime)
					.add("requestPickupTime", requestPickupTime)
					.add("pickupTimeLoss", pickupTimeLoss)
					.toString();
		}
	}

	public static class DropoffDetourInfo {
		// expected arrival time for the vehicle at the stop
		public final double vehicleArrivalTime;
		// expected dropoff time for the inserted request
		public final double requestDropoffTime;
		// ADDITIONAL time delay of each stop placed after the dropoff insertion point
		public final double dropoffTimeLoss;

		public DropoffDetourInfo(double vehicleArrivalTime, double requestDropoffTime, double dropoffTimeLoss) {
			this.vehicleArrivalTime = vehicleArrivalTime;
			this.requestDropoffTime = requestDropoffTime;
			this.dropoffTimeLoss = dropoffTimeLoss;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("vehicleArrivalTime", vehicleArrivalTime)
					.add("requestDropoffTime", requestDropoffTime)
					.add("dropoffTimeLoss", dropoffTimeLoss)
					.toString();
		}
	}

	//move to InsertionDetourTimeCalculator; make InsertionDetourTimeCalculator an interface?
	public static class DetourTimeInfo {
		public final PickupDetourInfo pickupDetourInfo;
		public final DropoffDetourInfo dropoffDetourInfo;

		public DetourTimeInfo(PickupDetourInfo pickupDetourInfo, DropoffDetourInfo dropoffDetourInfo) {
			this.pickupDetourInfo = pickupDetourInfo;
			this.dropoffDetourInfo = dropoffDetourInfo;
		}

		/**
		 * @return TOTAL time delay of each stop placed after the dropoff insertion point
		 * 		(this is the amount of extra time the vehicle will operate if this insertion is applied)
		 */
		public double getTotalTimeLoss() {
			return pickupDetourInfo.pickupTimeLoss + dropoffDetourInfo.dropoffTimeLoss;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("pickupDetourInfo", pickupDetourInfo)
					.add("dropoffDetourInfo", dropoffDetourInfo)
					.toString();
		}
	}
	
	private record PickupTimeInfo(double vehicleDepartureTime, double requestPickupTime, double additionalStopDuration) {
	}
}
