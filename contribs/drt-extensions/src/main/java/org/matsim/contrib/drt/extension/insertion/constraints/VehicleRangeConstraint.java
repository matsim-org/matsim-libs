package org.matsim.contrib.drt.extension.insertion.constraints;

import javax.annotation.Nullable;

import org.matsim.contrib.drt.extension.insertion.DrtInsertionConstraint;
import org.matsim.contrib.drt.extension.insertion.distances.DistanceApproximator;
import org.matsim.contrib.drt.extension.insertion.distances.DistanceCalculator;
import org.matsim.contrib.drt.extension.insertion.distances.InsertionDistanceCalculator;
import org.matsim.contrib.drt.extension.insertion.distances.InsertionDistanceCalculator.VehicleDistance;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.load.DvrpLoadType;

public class VehicleRangeConstraint implements DrtInsertionConstraint {
	private final InsertionDistanceCalculator insertionCalculator;
	
	private final VehicleRangeSupplier rangeSupplier;
	private final DistanceCalculator distanceCalculator;
	private final DistanceApproximator distanceApproximator;

	public VehicleRangeConstraint(VehicleRangeSupplier rangeSupplier, DistanceCalculator distanceEstimator,
			@Nullable DistanceApproximator distanceApproximator, DvrpLoadType loadType) {
		this.rangeSupplier = rangeSupplier;
		this.distanceCalculator = distanceEstimator;
		this.distanceApproximator = distanceApproximator;
		this.insertionCalculator = new InsertionDistanceCalculator(loadType);
	}

	@Override
	public boolean checkInsertion(DrtRequest drtRequest, Insertion insertion, DetourTimeInfo detourTimeInfo) {
		VehicleDistance scheduledDistance = insertionCalculator.calculateScheduledDistance(insertion.vehicleEntry);
		double currentDistance = scheduledDistance.totalDriveDistance();
		double distanceSlack = rangeSupplier.getVehicleRange(insertion.vehicleEntry.vehicle) - currentDistance;

		if (distanceSlack <= 0) {
			return false; // already violated
		}

		if (distanceApproximator != null) {
			VehicleDistance approximatedInsertionDistance = insertionCalculator.calculateInsertionDistance(insertion,
					detourTimeInfo, distanceApproximator);
			double approximatedDistanceDelta = approximatedInsertionDistance.totalDriveDistance();

			if (approximatedDistanceDelta <= distanceSlack) {
				return true; // passes with pessimistic approximation
			}
		}

		VehicleDistance insertionDistance = insertionCalculator.calculateInsertionDistance(insertion, detourTimeInfo,
				distanceCalculator);
		double distanceDelta = insertionDistance.totalDriveDistance();

		return distanceDelta <= distanceSlack;
	}

	static public interface VehicleRangeSupplier {
		double getVehicleRange(DvrpVehicle vehicle);
	}
}
