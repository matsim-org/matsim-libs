package playground.wrashid.parkingSearch.planLevel.occupancy;

/**
 * This class severs to contain the time during which a parking is full.
 * @author rashid_waraich
 *
 */
public class ParkingCapacityFullItem {
	
	private double startTime;
	private double endTime;

	public ParkingCapacityFullItem(double startTime, double endTime) {
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
