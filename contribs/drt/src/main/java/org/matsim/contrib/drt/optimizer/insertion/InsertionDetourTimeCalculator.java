package org.matsim.contrib.drt.optimizer.insertion;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

import java.util.Collections;
import java.util.function.ToDoubleFunction;

import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtStopTask.StopDuration;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionDetourTimeCalculator<D> {
	private static int changeOngoingStopDurationWarningCount = 0;
	private final static Logger logger = Logger.getLogger(InsertionDetourTimeCalculator.class);
	
	private final StopDuration stopDuration;
	private final ToDoubleFunction<D> detourTime;

	// If the detour data uses approximated detour times (e.g. beeline or matrix-based), provide the same estimator for
	// replaced drives to avoid systematic errors
	// When null, the replaced drive duration is derived from the schedule
	@Nullable
	private final DetourTimeEstimator replacedDriveTimeEstimator;

	public InsertionDetourTimeCalculator(StopDuration stopDuration, ToDoubleFunction<D> detourTime,
			@Nullable DetourTimeEstimator replacedDriveTimeEstimator) {
		this.stopDuration = stopDuration;
		this.detourTime = detourTime;
		this.replacedDriveTimeEstimator = replacedDriveTimeEstimator;
	}

	public DetourTimeInfo calculateDetourTimeInfo(InsertionWithDetourData<D> insertion, DrtRequest request) {
		InsertionGenerator.InsertionPoint pickup = insertion.getPickup();
		InsertionGenerator.InsertionPoint dropoff = insertion.getDropoff();
		if (pickup.index == dropoff.index) {
			//handle the pickup->dropoff case separately
			return calculateDetourTimeInfoForIfPickupToDropoffDetour(insertion, request);
		}

		VehicleEntry vEntry = insertion.getVehicleEntry();

		final double departureTime;
		final double pickupTimeLoss;
		double toPickupDepartureTime = pickup.previousWaypoint.getDepartureTime();
		if (pickup.newWaypoint.getLink() == pickup.previousWaypoint.getLink()) {
			// no detour, but possible additional stop duration
			pickupTimeLoss = calcAdditionalStopDurationIfSameLinkAsPrevious(vEntry, pickup.index, request);
			departureTime = toPickupDepartureTime + pickupTimeLoss;
		} else {
			double toPickupTT = detourTime.applyAsDouble(insertion.getDetourToPickup());
			double fromPickupTT = detourTime.applyAsDouble(insertion.getDetourFromPickup());
			double replacedDriveTT = calculateReplacedDriveDuration(vEntry, pickup.index);
			
			double pickupStopDuration = stopDuration.calculateStopDuration(vEntry.vehicle, Collections.emptySet(), Collections.singleton(request));
			pickupTimeLoss = toPickupTT + pickupStopDuration + fromPickupTT - replacedDriveTT;
			departureTime = toPickupDepartureTime + toPickupTT + pickupStopDuration;
		}

		final double arrivalTime;
		final double dropoffTimeLoss;
		if (dropoff.newWaypoint.getLink() == dropoff.previousWaypoint.getLink()) {
			// no detour, but possible additional stop duration
			dropoffTimeLoss = calcAdditionalStopDurationIfSameLinkAsPrevious(vEntry, dropoff.index, request);
			arrivalTime = dropoff.previousWaypoint.getArrivalTime() + pickupTimeLoss;
		} else {
			double toDropoffTT = detourTime.applyAsDouble(insertion.getDetourToDropoff());
			double fromDropoffTT = detourTime.applyAsDouble(insertion.getDetourFromDropoff());
			double replacedDriveTT = calculateReplacedDriveDuration(insertion.getVehicleEntry(), dropoff.index);
			double dropoffStopDuration = stopDuration.calculateStopDuration(vEntry.vehicle, Collections.singleton(request), Collections.emptySet());
			dropoffTimeLoss = toDropoffTT + dropoffStopDuration + fromDropoffTT - replacedDriveTT;
			arrivalTime = dropoff.previousWaypoint.getDepartureTime() + pickupTimeLoss + toDropoffTT;
		}

		return new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss);
	}

	private DetourTimeInfo calculateDetourTimeInfoForIfPickupToDropoffDetour(InsertionWithDetourData<D> insertion, DrtRequest request) {
		VehicleEntry vEntry = insertion.getVehicleEntry();
		InsertionGenerator.InsertionPoint pickup = insertion.getPickup();

		final double toPickupTT;
		final double additionalPickupStopDuration;
		if (pickup.newWaypoint.getLink() == pickup.previousWaypoint.getLink()) {
			// no drive to pickup, but possible additional stop duration
			toPickupTT = 0;
			additionalPickupStopDuration = calcAdditionalStopDurationIfSameLinkAsPrevious(vEntry, pickup.index, request);
		} else {
			toPickupTT = detourTime.applyAsDouble(insertion.getDetourToPickup());
			additionalPickupStopDuration = stopDuration.calculateStopDuration(vEntry.vehicle, Collections.emptySet(), Collections.singleton(request));
		}

		double fromPickupToDropoffTT = detourTime.applyAsDouble(insertion.getDetourFromPickup());
		double fromDropoffTT = detourTime.applyAsDouble(insertion.getDetourFromDropoff());
		double replacedDriveTT = calculateReplacedDriveDuration(vEntry, pickup.index);
		double pickupTimeLoss = toPickupTT + additionalPickupStopDuration + fromPickupToDropoffTT - replacedDriveTT;
		double dropoffStopDuration = stopDuration.calculateStopDuration(vEntry.vehicle, Collections.singleton(request), Collections.emptySet());
		double dropoffTimeLoss = dropoffStopDuration + fromDropoffTT;

		double departureTime = pickup.previousWaypoint.getDepartureTime() + toPickupTT + additionalPickupStopDuration;
		double arrivalTime = departureTime + fromPickupToDropoffTT;

		return new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss);
	}

	private double calcAdditionalStopDurationIfSameLinkAsPrevious(VehicleEntry vEntry, int insertionIdx, DrtRequest request) {
		DrtStopTask stopTask;
		
		if (insertionIdx == 0) {
			var startTask = vEntry.start.task;
			
			if (startTask.isPresent() && STOP.isBaseTypeOf(startTask.get())) {
				stopTask = (DrtStopTask) startTask.get();
			} else {
				return stopDuration.calculateStopDuration(vEntry.vehicle, Collections.emptySet(), Collections.singleton(request));
			}
		} else {
			stopTask = vEntry.stops.get(insertionIdx - 1).task;
		}
		
		double intialDuration = stopDuration.calculateStopDuration(vEntry.vehicle, stopTask.getDropoffRequests().values(), stopTask.getPickupRequests().values());
		double updatedDuration = stopDuration.calculateStopDuration(vEntry.vehicle, stopTask.getDropoffRequests().values(), CollectionUtils.union(stopTask.getPickupRequests().values(), Collections.singleton(request)));
		double timeLoss = updatedDuration - intialDuration;
		
		if (insertionIdx == 0 && timeLoss > 0.0) {
			if (changeOngoingStopDurationWarningCount++ < 100) {
				logger.warn("Considering insertion where stop duration of the ongoing DrtStopTask is changed. " //
						+ "This is not yet supported by BusStopActivity");
			}
		}
		
		return timeLoss;
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

	//move to InsertionDetourTimeCalculator; make InsertionDetourTimeCalculator an interface?
	public static class DetourTimeInfo {
		// expected departure time for the new request
		public final double departureTime;
		// expected arrival time for the new request
		public final double arrivalTime;
		// time delay of each stop placed after the pickup insertion point
		public final double pickupTimeLoss;
		// ADDITIONAL time delay of each stop placed after the dropoff insertion point
		public final double dropoffTimeLoss;

		public DetourTimeInfo(double departureTime, double arrivalTime, double pickupTimeLoss, double dropoffTimeLoss) {
			this.departureTime = departureTime;
			this.arrivalTime = arrivalTime;
			this.pickupTimeLoss = pickupTimeLoss;
			this.dropoffTimeLoss = dropoffTimeLoss;
		}

		// TOTAL time delay of each stop placed after the dropoff insertion point
		// (this is the amount of extra time the vehicle will operate if this insertion is applied)
		public double getTotalTimeLoss() {
			return pickupTimeLoss + dropoffTimeLoss;
		}
	}
}
