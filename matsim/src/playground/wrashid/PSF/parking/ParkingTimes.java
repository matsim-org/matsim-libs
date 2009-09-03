package playground.wrashid.PSF.parking;

import java.util.LinkedList;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.ActivityImpl;

public class ParkingTimes {

	private LinkedList<ParkLog> parkingTimes=new LinkedList<ParkLog>();
	private double firstParkingDepartTime=0;
	private double lastParkingArrivalTime=0;
	private Activity activity;
	
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

	public Activity getCarLastTimeParkedActivity() {
		return activity;
	}

	public void setCarLastTimeParkedActivity(Activity activity) {
		this.activity = activity;
	}

	public double getFirstParkingDepartTime() {
		return firstParkingDepartTime;
	}

	public void setFirstParkingDepartTime(double firstParkingDepartTime) {
		this.firstParkingDepartTime = firstParkingDepartTime;
	}
	
}
