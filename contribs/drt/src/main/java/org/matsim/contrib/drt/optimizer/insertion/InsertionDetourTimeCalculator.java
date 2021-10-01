package org.matsim.contrib.drt.optimizer.insertion;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

import java.util.function.ToDoubleFunction;

import javax.annotation.Nullable;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.DetourTimeInfo;

/**
 * @author Michal Maciejewski (michalm)
 */
class InsertionDetourTimeCalculator<D> {
	private final double stopDuration;
	private final ToDoubleFunction<D> detourTime;

	// If the detour data uses approximated detour times (e.g. beeline or matrix-based), provide the same estimator for
	// replaced drives to avoid systematic errors
	// When null, the replaced drive duration is derived from the schedule
	@Nullable
	private final DetourTimeEstimator replacedDriveTimeEstimator;

	InsertionDetourTimeCalculator(double stopDuration, ToDoubleFunction<D> detourTime,
			@Nullable DetourTimeEstimator replacedDriveTimeEstimator) {
		this.stopDuration = stopDuration;
		this.detourTime = detourTime;
		this.replacedDriveTimeEstimator = replacedDriveTimeEstimator;
	}

	DetourTimeInfo calculateDetourTimeInfo(InsertionWithDetourData<D> insertion) {
		InsertionGenerator.InsertionPoint pickup = insertion.getPickup();
		InsertionGenerator.InsertionPoint dropoff = insertion.getDropoff();
		if (pickup.index == dropoff.index) {
			//handle the pickup->dropoff case separately
			return calculateDetourTimeInfoForIfPickupToDropoffDetour(insertion);
		}

		VehicleEntry vEntry = insertion.getVehicleEntry();

		final double departureTime;
		final double pickupTimeLoss;
		double toPickupDepartureTime = pickup.previousWaypoint.getDepartureTime();
		if (pickup.newWaypoint.getLink() == pickup.previousWaypoint.getLink()) {
			// no detour, but possible additional stop duration
			double additionalStopDuration = calcAdditionalPickupStopDurationIfSameLinkAsPrevious(vEntry, pickup.index);
			pickupTimeLoss = additionalStopDuration;
			departureTime = toPickupDepartureTime + additionalStopDuration;
		} else {
			double toPickupTT = detourTime.applyAsDouble(insertion.getDetourToPickup());
			double fromPickupTT = detourTime.applyAsDouble(insertion.getDetourFromPickup());
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
			double toDropoffTT = detourTime.applyAsDouble(insertion.getDetourToDropoff());
			double fromDropoffTT = detourTime.applyAsDouble(insertion.getDetourFromDropoff());
			double replacedDriveTT = calculateReplacedDriveDuration(insertion.getVehicleEntry(), dropoff.index);
			dropoffTimeLoss = toDropoffTT + stopDuration + fromDropoffTT - replacedDriveTT;
			arrivalTime = dropoff.previousWaypoint.getDepartureTime() + pickupTimeLoss + toDropoffTT;
		}

		return new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss);
	}

	private DetourTimeInfo calculateDetourTimeInfoForIfPickupToDropoffDetour(InsertionWithDetourData<D> insertion) {
		VehicleEntry vEntry = insertion.getVehicleEntry();
		InsertionGenerator.InsertionPoint pickup = insertion.getPickup();

		final double toPickupTT;
		final double additionalPickupStopDuration;
		if (pickup.newWaypoint.getLink() == pickup.previousWaypoint.getLink()) {
			// no drive to pickup, but possible additional stop duration
			toPickupTT = 0;
			additionalPickupStopDuration = calcAdditionalPickupStopDurationIfSameLinkAsPrevious(vEntry, pickup.index);
		} else {
			toPickupTT = detourTime.applyAsDouble(insertion.getDetourToPickup());
			additionalPickupStopDuration = stopDuration;
		}

		double fromPickupToDropoffTT = detourTime.applyAsDouble(insertion.getDetourFromPickup());
		double fromDropoffTT = detourTime.applyAsDouble(insertion.getDetourFromDropoff());
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
}
