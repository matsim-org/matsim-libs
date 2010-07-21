package playground.wrashid.parkingSearch.planLevel.scoring;

import org.matsim.api.core.v01.Id;

public class ParkingTimeInfo {

	double startTime;
	double endTime;
	private final Id parkingFacilityId;

	public ParkingTimeInfo(double startTime, double endTime, Id parkingFacilityId) {
		super();
		this.startTime = startTime;
		this.endTime = endTime;
		this.parkingFacilityId = parkingFacilityId;
	}

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public Id getParkingFacilityId() {
		return parkingFacilityId;
	}

}
