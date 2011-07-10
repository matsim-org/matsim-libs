package playground.wrashid.parkingChoice.api;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public interface ParkingSelectionManager {

	// arrivalTime and estimatedParkingDuration can be both null, if not know at time of execution
	public Parking selectParking(Coord targtLocationCoord, ActInfo targetActInfo, Id personId, Double arrivalTime, Double estimatedParkingDuration);

}
