package org.matsim.contrib.drt.optimizer.insertion;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

import javax.annotation.Nullable;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.InsertionPoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;

import com.google.common.base.MoreObjects;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionDetourTimeCalculator {

	private final IncrementalStopDurationEstimator stopDurationEstimator;

	// If the detour data uses approximated detour times (e.g. beeline or matrix-based), provide the same estimator for
	// replaced drives to avoid systematic errors
	// When null, the replaced drive duration is derived from the schedule
	@Nullable
	private final DetourTimeEstimator replacedDriveTimeEstimator;

	public InsertionDetourTimeCalculator(IncrementalStopDurationEstimator incrementalStopDurationEstimator,
			@Nullable DetourTimeEstimator replacedDriveTimeEstimator) {
		this.stopDurationEstimator = incrementalStopDurationEstimator;
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
			double additionalStopDuration = calcAdditionalPickupStopDurationIfSameLinkAsPrevious(vEntry, pickup.index, drtRequest);
			double departureTime = toPickupDepartureTime + additionalStopDuration;
			return new PickupDetourInfo(departureTime, additionalStopDuration);
		}

		double replacedDriveTT = calculateReplacedDriveDuration(vEntry, pickup.index, toPickupDepartureTime);
		double stopDuration = stopDurationEstimator.calcForPickup(vEntry.vehicle, null, drtRequest);
		double pickupTimeLoss = toPickupTT + stopDuration + fromPickupTT - replacedDriveTT;
		double departureTime = toPickupDepartureTime + toPickupTT + stopDuration;
		return new PickupDetourInfo(departureTime, pickupTimeLoss);
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
			double dropoffTimeLoss = calcAdditionalDropoffStopDurationIfSameLinkAsPrevious(vEntry, insertion.dropoff.index, drtRequest);
			double arrivalTime = dropoff.previousWaypoint.getArrivalTime() + pickupDetourInfo.pickupTimeLoss;
			return new DropoffDetourInfo(arrivalTime, dropoffTimeLoss);
		}

		double toDropoffDepartureTime = dropoff.previousWaypoint.getDepartureTime();
		double replacedDriveTT = calculateReplacedDriveDuration(vEntry, dropoff.index, toDropoffDepartureTime);
		double dropoffTimeLoss = toDropoffTT + stopDurationEstimator.calcForDropoff(vEntry.vehicle, null, drtRequest) + fromDropoffTT - replacedDriveTT;
		double arrivalTime = toDropoffDepartureTime + pickupDetourInfo.pickupTimeLoss + toDropoffTT;
		return new DropoffDetourInfo(arrivalTime, dropoffTimeLoss);
	}

	private PickupDetourInfo calcPickupDetourInfoIfPickupToDropoffDetour(VehicleEntry vEntry, InsertionPoint pickup,
			double toPickupTT, double fromPickupTT, DrtRequest drtRequest) {
		final double additionalPickupStopDuration;
		if (pickup.newWaypoint.getLink() == pickup.previousWaypoint.getLink()) {
			// no drive to pickup, but possible additional stop duration
			additionalPickupStopDuration = calcAdditionalPickupStopDurationIfSameLinkAsPrevious(vEntry, pickup.index, drtRequest);
		} else {
			additionalPickupStopDuration = stopDurationEstimator.calcForPickup(vEntry.vehicle, null, drtRequest);
		}

		double toPickupDepartureTime = pickup.previousWaypoint.getDepartureTime();
		double replacedDriveTT = calculateReplacedDriveDuration(vEntry, pickup.index, toPickupDepartureTime);

		double pickupTimeLoss = toPickupTT + additionalPickupStopDuration + fromPickupTT - replacedDriveTT;
		double departureTime = toPickupDepartureTime + toPickupTT + additionalPickupStopDuration;
		return new PickupDetourInfo(departureTime, pickupTimeLoss);
	}

	private DropoffDetourInfo calcDropoffDetourInfoIfPickupToDropoffDetour(double fromPickupToDropoffTT,
			double fromDropoffTT, PickupDetourInfo pickupDetourInfo, VehicleEntry vEntry, DrtRequest drtRequest) {
		double dropoffTimeLoss = stopDurationEstimator.calcForDropoff(vEntry.vehicle, null, drtRequest) + fromDropoffTT;
		double arrivalTime = pickupDetourInfo.departureTime + fromPickupToDropoffTT;
		return new DropoffDetourInfo(arrivalTime, dropoffTimeLoss);
	}

	private double calcAdditionalPickupStopDurationIfSameLinkAsPrevious(VehicleEntry vEntry, int insertionIdx, DrtRequest drtRequest) {
		DrtStopTask stopTask;

		if (insertionIdx == 0) {
			var startTask = vEntry.start.task;

			if (startTask.isPresent() && STOP.isBaseTypeOf(startTask.get())) {
				stopTask = (DrtStopTask) startTask.get();
			} else {
				stopTask = null;
			}
		} else {
			stopTask = vEntry.stops.get(insertionIdx - 1).task;
		}
		return stopDurationEstimator.calcForPickup(vEntry.vehicle, stopTask, drtRequest);
	}

	private double calcAdditionalDropoffStopDurationIfSameLinkAsPrevious(VehicleEntry vEntry, int insertionIdx, DrtRequest drtRequest) {
		DrtStopTask stopTask;

		if (insertionIdx == 0) {
			var startTask = vEntry.start.task;

			if (startTask.isPresent() && STOP.isBaseTypeOf(startTask.get())) {
				stopTask = (DrtStopTask) startTask.get();
			} else {
				stopTask = null;
			}
		} else {
			stopTask = vEntry.stops.get(insertionIdx - 1).task;
		}
		return stopDurationEstimator.calcForDropoff(vEntry.vehicle, stopTask, drtRequest);
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
			return MoreObjects.toStringHelper(this)
					.add("departureTime", departureTime)
					.add("pickupTimeLoss", pickupTimeLoss)
					.toString();
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
			return MoreObjects.toStringHelper(this)
					.add("arrivalTime", arrivalTime)
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
}
