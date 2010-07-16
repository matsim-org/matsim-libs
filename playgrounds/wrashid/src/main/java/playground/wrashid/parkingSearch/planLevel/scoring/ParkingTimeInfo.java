package playground.wrashid.parkingSearch.planLevel.scoring;

public class ParkingTimeInfo {

	double startTime;
	double endTime;

	public ParkingTimeInfo(double startTime, double endTime) {
		super();
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}

}
