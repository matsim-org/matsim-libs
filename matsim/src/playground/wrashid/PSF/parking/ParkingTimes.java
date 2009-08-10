package playground.wrashid.PSF.parking;

import java.util.LinkedList;

import org.matsim.api.basic.v01.Id;

public class ParkingTimes {

	private LinkedList<ParkLog> parkingTimes=new LinkedList<ParkLog>();
	private double firstParkingDepartTime=0;
	private double lastParkingArrivalTime=0;
	private Id carLastTimeParkedLinkId;
	
	public void addParkLog(ParkLog parkLog){
		parkingTimes.add(parkLog);
	}

	public LinkedList<ParkLog> getParkingTimes() {
		return parkingTimes;
	}

	public double getLastParkingArrivalTime() {
		return lastParkingArrivalTime;
	}

	public void setCarLastTimeParked(double carLastTimeParked) {
		this.lastParkingArrivalTime = carLastTimeParked;
	}

	public Id getCarLastTimeParkedLinkId() {
		return carLastTimeParkedLinkId;
	}

	public void setCarLastTimeParkedLinkId(Id carLastTimeParkedLinkId) {
		this.carLastTimeParkedLinkId = carLastTimeParkedLinkId;
	}

	public double getFirstParkingDepartTime() {
		return firstParkingDepartTime;
	}

	public void setFirstParkingDepartTime(double firstParkingDepartTime) {
		this.firstParkingDepartTime = firstParkingDepartTime;
	}
	
}
