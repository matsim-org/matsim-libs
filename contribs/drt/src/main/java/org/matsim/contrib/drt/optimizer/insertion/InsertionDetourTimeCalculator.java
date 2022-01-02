package org.matsim.contrib.drt.optimizer.insertion;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

import java.util.function.ToDoubleFunction;

import javax.annotation.Nullable;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.InsertionPoint;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionDetourTimeCalculator<D> {
	private final double stopDuration;
	private final ToDoubleFunction<D> detourTime;

	// If the detour data uses approximated detour times (e.g. beeline or matrix-based), provide the same estimator for
	// replaced drives to avoid systematic errors
	// When null, the replaced drive duration is derived from the schedule
	@Nullable
	private final DetourTimeEstimator replacedDriveTimeEstimator;

	public InsertionDetourTimeCalculator(double stopDuration, ToDoubleFunction<D> detourTime,
			@Nullable DetourTimeEstimator replacedDriveTimeEstimator) {
		this.stopDuration = stopDuration;
		this.detourTime = detourTime;
		this.replacedDriveTimeEstimator = replacedDriveTimeEstimator;
	}

	public DetourTimeInfo calculateDetourTimeInfo(InsertionWithDetourData<D> insertionWithDetourData) {
		var insertion = insertionWithDetourData.getInsertion();
		InsertionPoint pickup = insertion.pickup;
		InsertionPoint dropoff = insertion.dropoff;
		if (pickup.index == dropoff.index) {
			//handle the pickup->dropoff case separately
			return calculateDetourTimeInfoForIfPickupToDropoffDetour(insertionWithDetourData);
		}

		VehicleEntry vEntry = insertion.vehicleEntry;
		var detourData = insertionWithDetourData.getDetourData();

		final double departureTime;
		final double pickupTimeLoss;
		double toPickupDepartureTime = pickup.previousWaypoint.getDepartureTime();
		if (pickup.newWaypoint.getLink() == pickup.previousWaypoint.getLink()) {
			// no detour, but possible additional stop duration
			double additionalStopDuration = calcAdditionalPickupStopDurationIfSameLinkAsPrevious(vEntry, pickup.index);
			pickupTimeLoss = additionalStopDuration;
			departureTime = toPickupDepartureTime + additionalStopDuration;
		} else {
			double toPickupTT = detourTime.applyAsDouble(detourData.detourToPickup);
			double fromPickupTT = detourTime.applyAsDouble(detourData.detourFromPickup);
			double replacedDriveTT = calculateReplacedDriveDuration(vEntry, pickup.index);
			pickupTimeLoss = toPickupTT + stopDuration + fromPickupTT - replacedDriveTT;
			departureTime = toPickupDepartureTime + toPickupTT + stopDuration;
		}

		final double arrivalTime;
		final double dropoffTimeLoss;
		if (dropoff.newWaypoint.getLink() == dropoff.previousWaypoint.getLink()) {
			// no detour, no additional stop duration
			dropoffTimeLoss = 0;
			arrivalTime = dropoff.previousWaypoint.getArrivalTime() + pickupTimeLoss;
		} else {
			double toDropoffTT = detourTime.applyAsDouble(detourData.detourToDropoff);
			double fromDropoffTT = detourTime.applyAsDouble(detourData.detourFromDropoff);
			double replacedDriveTT = calculateReplacedDriveDuration(vEntry, dropoff.index);
			dropoffTimeLoss = toDropoffTT + stopDuration + fromDropoffTT - replacedDriveTT;
			arrivalTime = dropoff.previousWaypoint.getDepartureTime() + pickupTimeLoss + toDropoffTT;
		}

		return new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss);
	}

	private DetourTimeInfo calculateDetourTimeInfoForIfPickupToDropoffDetour(
			InsertionWithDetourData<D> insertionWithDetourData) {
		var insertion = insertionWithDetourData.getInsertion();
		VehicleEntry vEntry = insertion.vehicleEntry;
		InsertionPoint pickup = insertion.pickup;
		var detourData = insertionWithDetourData.getDetourData();

		final double toPickupTT;
		final double additionalPickupStopDuration;
		if (pickup.newWaypoint.getLink() == pickup.previousWaypoint.getLink()) {
			// no drive to pickup, but possible additional stop duration
			toPickupTT = 0;
			additionalPickupStopDuration = calcAdditionalPickupStopDurationIfSameLinkAsPrevious(vEntry, pickup.index);
		} else {
			toPickupTT = detourTime.applyAsDouble(detourData.detourToPickup);
			additionalPickupStopDuration = stopDuration;
		}

		double fromPickupToDropoffTT = detourTime.applyAsDouble(detourData.detourFromPickup);
		double fromDropoffTT = detourTime.applyAsDouble(detourData.detourFromDropoff);
		double replacedDriveTT = calculateReplacedDriveDuration(vEntry, pickup.index);
		double pickupTimeLoss = toPickupTT + additionalPickupStopDuration + fromPickupToDropoffTT - replacedDriveTT;
		double dropoffTimeLoss = stopDuration + fromDropoffTT;

		double departureTime = pickup.previousWaypoint.getDepartureTime() + toPickupTT + additionalPickupStopDuration;
		double arrivalTime = departureTime + fromPickupToDropoffTT;

		return new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss);
	}

	private double calcAdditionalPickupStopDurationIfSameLinkAsPrevious(VehicleEntry vEntry, int pickupIdx) {
		if (pickupIdx > 0) {
			return 0;
		}
		var startTask = vEntry.start.task;
		return startTask.isPresent() && STOP.isBaseTypeOf(startTask.get()) ? 0 : stopDuration;
	}

	private double calculateReplacedDriveDuration(VehicleEntry vEntry, int insertionIdx) {
		if (vEntry.isAfterLastStop(insertionIdx)) {
			return 0;// end of route - bus would wait there
		}

		if (replacedDriveTimeEstimator != null) {
			//use the approximated drive times instead of deriving (presumably more accurate) times from the schedule
			return replacedDriveTimeEstimator.estimateTime(vEntry.getWaypoint(insertionIdx).getLink(),
					vEntry.getWaypoint(insertionIdx + 1).getLink());
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
	}

	//move to InsertionDetourTimeCalculator; make InsertionDetourTimeCalculator an interface?
	public static class DetourTimeInfo {
		public final PickupDetourInfo pickupDetourInfo;
		public final DropoffDetourInfo dropoffDetourInfo;

		public DetourTimeInfo(PickupDetourInfo pickupDetourInfo, DropoffDetourInfo dropoffDetourInfo) {
			this.pickupDetourInfo = pickupDetourInfo;
			this.dropoffDetourInfo = dropoffDetourInfo;
		}

		public DetourTimeInfo(double departureTime, double arrivalTime, double pickupTimeLoss, double dropoffTimeLoss) {
			this(new PickupDetourInfo(departureTime, pickupTimeLoss),
					new DropoffDetourInfo(arrivalTime, dropoffTimeLoss));
		}

		// TOTAL time delay of each stop placed after the dropoff insertion point
		// (this is the amount of extra time the vehicle will operate if this insertion is applied)
		public double getTotalTimeLoss() {
			return pickupDetourInfo.pickupTimeLoss + dropoffDetourInfo.dropoffTimeLoss;
		}
	}
}
