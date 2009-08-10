package playground.wrashid.PSF.parking;

import java.util.LinkedList;

public class ParkingTimes {

	private LinkedList<ParkLog> parkingTimes=new LinkedList<ParkLog>();
	private double carLastTimeParked=0;
	
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
	
}
