package playground.wrashid.PSF.parking;

import java.util.LinkedList;

import org.matsim.api.basic.v01.Id;

public class ParkingTimes {

	private LinkedList<ParkLog> parkingTimes=new LinkedList<ParkLog>();
	private double carLastTimeParked=0;
	private Id carLastTimeParkedLinkId;
	
	public void addParkLog(ParkLog parkLog){
		parkingTimes.add(parkLog);
	}

	public LinkedList<ParkLog> getParkingTimes() {
		return parkingTimes;
	}

	public double getCarLastTimeParked() {
		return carLastTimeParked;
	}

	public void setCarLastTimeParked(double carLastTimeParked) {
		this.carLastTimeParked = carLastTimeParked;
	}

	public Id getCarLastTimeParkedLinkId() {
		return carLastTimeParkedLinkId;
	}

	public void setCarLastTimeParkedLinkId(Id carLastTimeParkedLinkId) {
		this.carLastTimeParkedLinkId = carLastTimeParkedLinkId;
	}
	
}
