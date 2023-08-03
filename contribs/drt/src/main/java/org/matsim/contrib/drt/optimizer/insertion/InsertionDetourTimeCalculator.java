package org.matsim.contrib.drt.optimizer.insertion;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

import javax.annotation.Nullable;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.InsertionPoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.stops.StopDurationProvider;
import org.matsim.contrib.drt.stops.StopTimeCalculator;

import com.google.common.base.MoreObjects;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionDetourTimeCalculator {

	private final StopTimeCalculator stopTimeCalculator;

	// If the detour data uses approximated detour times (e.g. beeline or
	// matrix-based), provide the same estimator for
	// replaced drives to avoid systematic errors
	// When null, the replaced drive duration is derived from the schedule
	@Nullable
	private final DetourTimeEstimator replacedDriveTimeEstimator;

	public InsertionDetourTimeCalculator(StopTimeCalculator stopTimeCalculator,
			@Nullable DetourTimeEstimator replacedDriveTimeEstimator) {
		this.stopTimeCalculator = stopTimeCalculator;
		this.replacedDriveTimeEstimator = replacedDriveTimeEstimator;
	}

	public DetourTimeInfo calculateDetourTimeInfo(Insertion insertion, InsertionDetourData detourData,
			DrtRequest drtRequest) {
		var followedByDropoff = insertion.pickup.index == insertion.dropoff.index;

		double toPickupTT = detourData.detourToPickup.getTravelTime();
		double fromPickupTT = detourData.detourFromPickup.getTravelTime();
		var pickupDetourInfo = calcPickupDetourInfo(insertion.vehicleEntry, insertion.pickup, toPickupTT, fromPickupTT,
				drtRequest);

		double toDropoffTT = followedByDropoff ? fromPickupTT : detourData.detourToDropoff.getTravelTime();
		double fromDropoffTT = detourData.detourFromDropoff.getTravelTime();
		var dropoffDetourInfo = calcDropoffDetourInfo(insertion, toDropoffTT, fromDropoffTT, pickupDetourInfo,
				drtRequest);

		return new DetourTimeInfo(pickupDetourInfo, dropoffDetourInfo);
	}

	public PickupDetourInfo calcPickupDetourInfo(VehicleEntry vEntry, InsertionPoint pickup, double toPickupTT,
			double fromPickupTT, DrtRequest drtRequest) {
		/*
		 * This method calculates the timing information when inserting a new pickup
		 * into the task sequence of a vehicle. There are two cases:
		 * 
		 * - (1) The request is inserted on a different link than the current one. In
		 * this case, we have necessarily the preceding task (which can be anything like
		 * an ongoing or planned drive or stop task) that needs to be followed by a
		 * "toPickupDrive" indicated by toPickupTT. The new pickup stop starts at latest
		 * when we have arrived at the new pickup location.
		 * 
		 * - (2) The request is inserted on the same link as the current one. In this
		 * case we don't have a "toPickupDrive" and toPickupTT = 0. There are two cases
		 * determining when the pickup starts:
		 * 
		 * - (2a) In the standard case, we append the pickup to a preceding stop task
		 * somewhere in the schedule or an ongoing drive task that needs to be stopped.
		 * In both cases, the pickup starts at the time that is indicated as the
		 * departure time from the START waypoint.
		 * 
		 * - (2b) In the special case of an ongoing preceding stop we may insert the
		 * pickup as soon as possible ("now"). The stop task will then be extended
		 * accordingly.
		 */

		boolean sameLinkAsPrevious = pickup.newWaypoint.getLink() == pickup.previousWaypoint.getLink();
		DrtStopTask stopTask = sameLinkAsPrevious ? findStopTaskIfSameLinkAsPrevious(vEntry, pickup.index) : null;

		if (sameLinkAsPrevious && stopTask != null) {
			// IRTX TODO: Update this with a real now!
			double now = stopTask.getBeginTime();
			
			// arrival time for pickup cases 2b (now) vs 2a
			double arrivalTime = Math.max(stopTask.getBeginTime(), now);
			
			// calculate expected departure time based on existing requests in the stop
			double departureTime = stopTimeCalculator.updateEndTimeForPickup(vEntry.vehicle, stopTask, arrivalTime,
					drtRequest);
			
			double additionalStopDuration = departureTime - stopTask.getEndTime();
			double pickupTimeLoss = additionalStopDuration + fromPickupTT;
			
			return new PickupDetourInfo(departureTime, pickupTimeLoss);
		} else {
			// no preceding stop task, but toPickupTT may be zero if it is the same link (case 1)
			double arrivalTime = pickup.previousWaypoint.getDepartureTime() + toPickupTT;

			// calculate expected departure time for a newly created stop
			double departureTime = stopTimeCalculator.initEndTimeForPickup(vEntry.vehicle, arrivalTime, drtRequest);
			
			// calculate the drive time that we replace by inserting the new pickup + access and egress route
			double replacedDriveTT = sameLinkAsPrevious ?  0.0 : //
					calculateReplacedDriveDuration(vEntry, pickup.index, pickup.previousWaypoint.getDepartureTime());
			
			double stopDuration = departureTime - arrivalTime;
			double pickupTimeLoss = toPickupTT + stopDuration + fromPickupTT - replacedDriveTT;
			
			return new PickupDetourInfo(departureTime, pickupTimeLoss);
		}
	}

	public DropoffDetourInfo calcDropoffDetourInfo(Insertion insertion, double toDropoffTT, double fromDropoffTT,
			PickupDetourInfo pickupDetourInfo, DrtRequest drtRequest) {
		/*
		 * This method calculates the timing when we insert a dropoff into the stop
		 * sequence of a vehicle. There are three cases:
		 * 
		 * - (1) In the standard case we insert the dropoff on a different link than the
		 * one from the preceding waypoint. This means we need to have a
		 * "toDropoffDrive" with a corresponding toDropoffTT. The earliest moment at
		 * which the dropoff can start is the arrival time at the new waypoint.
		 * 
		 * - (2) We may insert a dropoff after/into a preceding task that is on the same
		 * link as the dropoff. In this case, we don't have a "toDropoffDrive" and
		 * toDropoffTT = 0. The earliest time to start the dropoff is the departure time
		 * indicated in the preceding waypoint.
		 * 
		 * Note that there is no distinction like 2a and 2b for the pickup, because a
		 * dropoff is always inserted after a pickup. There is, hence, now chance that
		 * we every want to append a dropoff to an ongoing stop task.
		 * 
		 * - (3) The dropoff follows directly after the pickup. In this case we should
		 * not count the toDropoffTT double as it has already been counted in the
		 * fromPickupTT when inserting the pickup. Otherwise, the case follows case 1.
		 */
		VehicleEntry vEntry = insertion.vehicleEntry;

		if (insertion.pickup.index == insertion.dropoff.index) {
			// special case 3: dropoff follows directly after the pickup
			
			double arrivalTime = pickupDetourInfo.departureTime + toDropoffTT;
			double departureTime = stopTimeCalculator.initEndTimeForDropoff(vEntry.vehicle, arrivalTime,
					drtRequest);

			double stopDuration = departureTime - arrivalTime;
			double dropoffTimeLoss = stopDuration + fromDropoffTT; // don't count toDropoffTT

			return new DropoffDetourInfo(arrivalTime, dropoffTimeLoss);
		}

		InsertionPoint dropoff = insertion.dropoff;

		boolean sameLinkAsPrevious = dropoff.newWaypoint.getLink() == dropoff.previousWaypoint.getLink();
		DrtStopTask stopTask = sameLinkAsPrevious ? findStopTaskIfSameLinkAsPrevious(vEntry, dropoff.index) : null;

		if (sameLinkAsPrevious && stopTask != null) {
			// arrival time at the insertion stop plus delay caused by pickup
			double arrivalTime = stopTask.getBeginTime() + pickupDetourInfo.pickupTimeLoss;

			// calculate expected departure time when adding the new dropoff to the stop
			double departureTime = stopTimeCalculator.updateEndTimeForDropoff(vEntry.vehicle, stopTask, arrivalTime,
					drtRequest);
			
			double initialStopDuration = stopTask.getEndTime() - stopTask.getBeginTime();
			double additionalStopDuration = departureTime - arrivalTime - initialStopDuration;
			double dropoffTimeLoss = additionalStopDuration + fromDropoffTT;
			
			return new DropoffDetourInfo(arrivalTime, dropoffTimeLoss);
		} else {
			// no preceding stop task, but toDropoffTT may be zero if it is the same link
			double arrivalTime = dropoff.previousWaypoint.getDepartureTime() + toDropoffTT + pickupDetourInfo.pickupTimeLoss;

			// calculate the expected departure time when creating a new stop for the dropoff
			double departureTime = stopTimeCalculator.initEndTimeForDropoff(vEntry.vehicle, arrivalTime, drtRequest);
			
			// calculate the drive time that we replace by inserting the new pickup + access and egress route
			double replacedDriveTT = sameLinkAsPrevious ? 0.0 : //
					calculateReplacedDriveDuration(vEntry, dropoff.index, dropoff.previousWaypoint.getDepartureTime());
			
			double stopDuration = departureTime - arrivalTime;
			double dropoffTimeLoss = toDropoffTT + stopDuration + fromDropoffTT - replacedDriveTT;
			
			return new DropoffDetourInfo(arrivalTime, dropoffTimeLoss);
		}
	}

	private DrtStopTask findStopTaskIfSameLinkAsPrevious(VehicleEntry vEntry, int insertionIdx) {
		if (insertionIdx == 0) {
			var startTask = vEntry.start.task;

			if (startTask.isPresent() && STOP.isBaseTypeOf(startTask.get())) {
				return (DrtStopTask) startTask.get();
			}
		} else {
			return vEntry.stops.get(insertionIdx - 1).task;
		}

		return null;
	}

	private double calculateReplacedDriveDuration(VehicleEntry vEntry, int insertionIdx, double detourStartTime) {
		if (vEntry.isAfterLastStop(insertionIdx)) {
			return 0;// end of route - bus would wait there
		}

		if (replacedDriveTimeEstimator != null) {
			// use the approximated drive times instead of deriving (presumably more
			// accurate) times from the schedule
			return replacedDriveTimeEstimator.estimateTime(vEntry.getWaypoint(insertionIdx).getLink(),
					vEntry.getWaypoint(insertionIdx + 1).getLink(), detourStartTime);
		}

		double replacedDriveStartTime = vEntry.getWaypoint(insertionIdx).getDepartureTime();
		double replacedDriveEndTime = vEntry.stops.get(insertionIdx).task.getBeginTime();
		return replacedDriveEndTime - replacedDriveStartTime;
	}

	public static class PickupDetourInfo {
		// expected departure time for the new request
		public final double departureTime;
		// time delay of each stop placed after the pickup insertion point
		public final double pickupTimeLoss;

		public PickupDetourInfo(double departureTime, double pickupTimeLoss) {
			this.departureTime = departureTime;
			this.pickupTimeLoss = pickupTimeLoss;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("departureTime", departureTime)
					.add("pickupTimeLoss", pickupTimeLoss).toString();
		}
	}

	public static class DropoffDetourInfo {
		// expected arrival time for the new request
		public final double arrivalTime;
		// ADDITIONAL time delay of each stop placed after the dropoff insertion point
		public final double dropoffTimeLoss;

		public DropoffDetourInfo(double arrivalTime, double dropoffTimeLoss) {
			this.arrivalTime = arrivalTime;
			this.dropoffTimeLoss = dropoffTimeLoss;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("arrivalTime", arrivalTime)
					.add("dropoffTimeLoss", dropoffTimeLoss).toString();
		}
	}

	// move to InsertionDetourTimeCalculator; make InsertionDetourTimeCalculator an
	// interface?
	public static class DetourTimeInfo {
		public final PickupDetourInfo pickupDetourInfo;
		public final DropoffDetourInfo dropoffDetourInfo;

		public DetourTimeInfo(PickupDetourInfo pickupDetourInfo, DropoffDetourInfo dropoffDetourInfo) {
			this.pickupDetourInfo = pickupDetourInfo;
			this.dropoffDetourInfo = dropoffDetourInfo;
		}

		/**
		 * @return TOTAL time delay of each stop placed after the dropoff insertion
		 *         point (this is the amount of extra time the vehicle will operate if
		 *         this insertion is applied)
		 */
		public double getTotalTimeLoss() {
			return pickupDetourInfo.pickupTimeLoss + dropoffDetourInfo.dropoffTimeLoss;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("pickupDetourInfo", pickupDetourInfo)
					.add("dropoffDetourInfo", dropoffDetourInfo).toString();
		}
	}
}
