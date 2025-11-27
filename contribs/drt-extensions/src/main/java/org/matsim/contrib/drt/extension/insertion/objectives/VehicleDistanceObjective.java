package org.matsim.contrib.drt.extension.insertion.objectives;

import org.matsim.contrib.drt.extension.insertion.DrtInsertionObjective;
import org.matsim.contrib.drt.extension.insertion.distances.DistanceCalculator;
import org.matsim.contrib.drt.extension.insertion.distances.InsertionDistanceCalculator;
import org.matsim.contrib.drt.extension.insertion.distances.InsertionDistanceCalculator.VehicleDistance;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.load.DvrpLoadType;

public class VehicleDistanceObjective implements DrtInsertionObjective {
	private final InsertionDistanceCalculator insertionCalculator;
	private final DistanceCalculator distanceCalculator;
	private final VehcileDistanceWeights weights;

	public VehicleDistanceObjective(VehcileDistanceWeights weights, DistanceCalculator distanceCalculator, DvrpLoadType loadType) {
		this.weights = weights;
		this.distanceCalculator = distanceCalculator;
		this.insertionCalculator = new InsertionDistanceCalculator(loadType);
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
