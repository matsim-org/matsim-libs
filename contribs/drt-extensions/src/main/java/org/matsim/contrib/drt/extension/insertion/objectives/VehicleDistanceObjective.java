package org.matsim.contrib.drt.extension.insertion.objectives;

import org.matsim.contrib.drt.extension.insertion.DrtInsertionObjective;
import org.matsim.contrib.drt.extension.insertion.distances.DistanceCalculator;
import org.matsim.contrib.drt.extension.insertion.distances.InsertionDistanceCalculator;
import org.matsim.contrib.drt.extension.insertion.distances.InsertionDistanceCalculator.VehicleDistance;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;

public class VehicleDistanceObjective implements DrtInsertionObjective {
	private final InsertionDistanceCalculator insertionCalculator = new InsertionDistanceCalculator();
	private final DistanceCalculator distanceCalculator;
	private final VehcileDistanceWeights weights;

	public VehicleDistanceObjective(VehcileDistanceWeights weights, DistanceCalculator distanceCalculator) {
		this.weights = weights;
		this.distanceCalculator = distanceCalculator;
	}

	@Override
	public double calculateObjective(DrtRequest request, Insertion insertion, DetourTimeInfo detourTimeInfo) {
		VehicleDistance distance = insertionCalculator.calculateInsertionDistance(insertion, detourTimeInfo,
				distanceCalculator);

		return distance.occupiedDriveDistance() * weights.occupied + distance.emptyDriveDistance() * weights.empty
				+ distance.passengerDistance() * weights.passenger;
	}

	static public record VehcileDistanceWeights(double occupied, double empty, double passenger) {
	}
}
