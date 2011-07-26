package playground.wrashid.parkingChoice.apiDefImpl;

import java.util.Collection;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingChoice.ParkingManager;
import playground.wrashid.parkingChoice.api.ParkingScoringFunction;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.scoring.ParkingScoreAccumulator;

public class PriceAndDistanceParkingSelectionManager extends ShortestWalkingDistanceParkingSelectionManager {

	public static ParkingScoringFunction parkingScoringFunction;

	public PriceAndDistanceParkingSelectionManager(ParkingManager parkingManager, ParkingScoringFunction parkingScoringFunction) {
		super(parkingManager);
		PriceAndDistanceParkingSelectionManager.parkingScoringFunction = parkingScoringFunction;
	}

	public Parking selectParking(Coord targtLocationCoord, ActInfo targetActInfo, Id personId, Double arrivalTime,
			Double estimatedParkingDuration) {

		Collection<Parking> parkingsInSurroundings = getParkingsInSurroundings(targtLocationCoord, 200.0, personId, 0.0,
				targetActInfo, parkingManager.getParkings());

		PriorityQueue<ParkingImpl> priorityQueue = new PriorityQueue<ParkingImpl>();
		for (Parking parking : parkingsInSurroundings) {
			ParkingImpl parkingImpl = (ParkingImpl) parking;
			parkingScoringFunction.assignScore(parkingImpl, targtLocationCoord, targetActInfo, personId, arrivalTime,
					estimatedParkingDuration);
			priorityQueue.add(parkingImpl);
		}

		ParkingImpl selectedParking = priorityQueue.poll();

		double walkingDistance = GeneralLib.getDistance(targtLocationCoord, selectedParking.getCoord());
		ParkingScoreAccumulator.parkingWalkDistances.put(personId, walkingDistance);
		
		if (walkingDistance>2000){
		//	System.out.println();
		}
		
		return selectedParking;
	}

}
