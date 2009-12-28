package playground.wrashid.PSF.parking;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;

import playground.wrashid.PSF.energy.charging.ChargeLog;

public class ParkLog {

	private static final Logger log = Logger.getLogger(ParkLog.class);
	
	private Activity activity;

	private double startParkingTime;
	private double endParkingTime;

	public ParkLog(Activity activity, double startParkingTime, double endParkingTime) {
		super();
		this.activity = activity;
		this.startParkingTime = startParkingTime;
		this.endParkingTime = endParkingTime;
		
		// this is possible, because parking can start in the evening and stop in the morning...
		if (startParkingTime>endParkingTime){
			//log.error("startParkingTime cannot be bigger than endParkingTime!");
			//System.exit(-1);
		}
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
