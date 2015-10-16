package playground.wrashid.ABMT.vehicleShare;

import org.matsim.api.core.v01.Coord;

public interface EVScenario {
	public double calcParkingFee(Coord coord, String actType, double arrivalTime, double departureTime);
	public boolean belongsToTolledArea(Coord coord);
}


