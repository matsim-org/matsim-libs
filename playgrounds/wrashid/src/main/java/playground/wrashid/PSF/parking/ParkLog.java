package playground.wrashid.PSF.parking;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

public class ParkLog {

	private static final Logger log = Logger.getLogger(ParkLog.class);
	
	private Id linkId;
	private Id facilityId;

	private double startParkingTime;
	private double endParkingTime;

	public ParkLog(Id linkId, Id facilityId, double startParkingTime, double endParkingTime) {
		super();
		this.linkId = linkId;
		this.facilityId = facilityId;
		this.startParkingTime = startParkingTime;
		this.endParkingTime = endParkingTime;
		
		// this is possible, because parking can start in the evening and stop in the morning...
		if (startParkingTime>endParkingTime){
			//log.error("startParkingTime cannot be bigger than endParkingTime!");
			//System.exit(-1);
		}
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public Id getFacilityId() {
		return this.facilityId;
	}

	public double getStartParkingTime() {
		return startParkingTime;
	}

	public double getEndParkingTime() {
		return endParkingTime;
	}

}
