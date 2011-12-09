package playground.wrashid.parkingChoice.scoring;

import org.matsim.api.core.v01.Id;

public class ParkingInfo {

	private Id parkingId;
	private double arrivalTime;
	private double departureTime;

	public Id getParkingId() {
		return parkingId;
	}
	public double getArrivalTime() {
		return arrivalTime;
	}
	public double getDepartureTime() {
		return departureTime;
	}
	public ParkingInfo( Id parkingId, double arrivalTime, double departureTime) {
		super();
		this.parkingId = parkingId;
		this.arrivalTime = arrivalTime;
		this.departureTime = departureTime;
	}

	
}
