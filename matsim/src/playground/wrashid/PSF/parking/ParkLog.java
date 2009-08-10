package playground.wrashid.PSF.parking;

import org.matsim.api.basic.v01.Id;

public class ParkLog {

	private Id linkId;

	private double startParkingTime;
	private double endParkingTime;

	public ParkLog(Id linkId, double startParkingTime, double endParkingTime) {
		super();
		this.linkId = linkId;
		this.startParkingTime = startParkingTime;
		this.endParkingTime = endParkingTime;
	}

	public Id getLinkId() {
		return linkId;
	}

	public double getStartParkingTime() {
		return startParkingTime;
	}

	public double getEndParkingTime() {
		return endParkingTime;
	}

}
