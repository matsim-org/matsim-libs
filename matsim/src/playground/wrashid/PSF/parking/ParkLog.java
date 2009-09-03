package playground.wrashid.PSF.parking;

import org.matsim.api.core.v01.population.Activity;

public class ParkLog {

	private Activity activity;

	private double startParkingTime;
	private double endParkingTime;

	public ParkLog(Activity activity, double startParkingTime, double endParkingTime) {
		super();
		this.activity = activity;
		this.startParkingTime = startParkingTime;
		this.endParkingTime = endParkingTime;
	}

	public Activity getActivity() {
		return activity;
	}

	public double getStartParkingTime() {
		return startParkingTime;
	}

	public double getEndParkingTime() {
		return endParkingTime;
	}

}
