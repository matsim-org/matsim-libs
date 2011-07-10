package playground.wrashid.parkingChoice.apiDefImpl;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.wrashid.parkingChoice.ParkingManager;
import playground.wrashid.parkingChoice.api.ParkingScoringFunction;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public class PriceAndDistanceParkingSelectionManager extends ShortestWalkingDistanceParkingSelectionManager {

	private final ParkingScoringFunction parkingScoringFunction;

	public PriceAndDistanceParkingSelectionManager(ParkingManager parkingManager, ParkingScoringFunction parkingScoringFunction) {
		super(parkingManager);
		this.parkingScoringFunction = parkingScoringFunction;
	}

	public Parking selectParking(Coord targtLocationCoord, ActInfo targetActInfo, Id personId, Double arrivalTime,
			Double estimatedParkingDuration) {
		// TODO Auto-generated method stub
		return getParkingWithShortestWalkingDistance(targtLocationCoord,targetActInfo,personId);
	}
	
}
