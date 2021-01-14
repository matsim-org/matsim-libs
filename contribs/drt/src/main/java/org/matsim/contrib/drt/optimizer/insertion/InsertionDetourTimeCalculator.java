package org.matsim.contrib.drt.optimizer.insertion;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

import java.util.function.ToDoubleFunction;

import javax.annotation.Nullable;

import org.matsim.contrib.drt.optimizer.VehicleData;

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

	double calculatePickupDetourTimeLoss(InsertionWithDetourData<D> insertion) {
		VehicleData.Entry vEntry = insertion.getVehicleEntry();
		InsertionGenerator.InsertionPoint pickup = insertion.getPickup();
		int pickupIdx = pickup.index;

		boolean driveToPickup = true;
		boolean appendPickupToExistingStop = false;
		if (pickup.newWaypoint.getLink() == pickup.previousWaypoint.getLink()) {
			//check if possible to append -> no additional stop duration
			appendPickupToExistingStop = appendPickupToExistingStopIfSameLinkAsPrevious(vEntry, pickupIdx);

			if (pickupIdx != insertion.getDropoff().index) {
				// no detour, no existing drive task replaced
				return appendPickupToExistingStop ? 0 : stopDuration;
			}

			driveToPickup = false;
		}

		double toPickupTT = driveToPickup ? detourTime.applyAsDouble(insertion.getDetourToPickup()) : 0;
		double additionalStopDuration = appendPickupToExistingStop ? 0 : stopDuration;
		double fromPickupTT = detourTime.applyAsDouble(insertion.getDetourFromPickup());
		double replacedDriveTT = calculateReplacedDriveDuration(vEntry, pickupIdx);
		return toPickupTT + additionalStopDuration + fromPickupTT - replacedDriveTT;
	}

	private boolean appendPickupToExistingStopIfSameLinkAsPrevious(VehicleData.Entry vEntry, int pickupIdx) {
		if (pickupIdx > 0) {
			return true;
		}
		var startTask = vEntry.start.task;
		return startTask.isPresent() && STOP.isBaseTypeOf(startTask.get());
	}

	double calculateDropoffDetourTimeLoss(InsertionWithDetourData<D> insertion) {
		InsertionGenerator.InsertionPoint dropoff = insertion.getDropoff();

		if (dropoff.index == insertion.getPickup().index) {
			//toDropoffTT and replacedDriveTT taken into account in pickupDetourTimeLoss
			return stopDuration + detourTime.applyAsDouble(insertion.getDetourFromDropoff());
		}

		if (dropoff.newWaypoint.getLink() == dropoff.previousWaypoint.getLink()) {
			return 0; // no detour, no additional stop duration
		}

		double toDropoffTT = detourTime.applyAsDouble(insertion.getDetourToDropoff());
		double fromDropoffTT = detourTime.applyAsDouble(insertion.getDetourFromDropoff());
		double replacedDriveTT = calculateReplacedDriveDuration(insertion.getVehicleEntry(), dropoff.index);
		return toDropoffTT + stopDuration + fromDropoffTT - replacedDriveTT;
	}

	private double calculateReplacedDriveDuration(VehicleData.Entry vEntry, int insertionIdx) {
		if (insertionIdx == vEntry.stops.size()) {
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
