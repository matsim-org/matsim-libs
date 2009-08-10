package playground.wrashid.PSF.parking;

import org.matsim.api.basic.v01.Id;

public class ParkLog {

	private Id facilityId;

	private double startParkingTime;
	private double endParkingTime;

	public ParkLog(Id facilityId, double startParkingTime, double endParkingTime) {
		super();
		this.facilityId = facilityId;
		this.startParkingTime = startParkingTime;
		this.endParkingTime = endParkingTime;
	}

	public Id getFacilityId() {
		return facilityId;
	}

	public double getStartParkingTime() {
		return startParkingTime;
	}

	public double getEndParkingTime() {
		return endParkingTime;
	}

}
