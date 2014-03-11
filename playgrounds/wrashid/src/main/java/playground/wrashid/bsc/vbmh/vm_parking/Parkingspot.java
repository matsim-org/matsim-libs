package playground.wrashid.bsc.vbmh.vm_parking;

public class Parkingspot {
	boolean charge, ev_exclusive;
	private boolean occupied;
	public int charging_rate, parking_pricem, charging_pricem;
	public Parking parking;
	private double time_vehicle_parked;
	
	public double getTime_vehicle_parked() {
		return time_vehicle_parked;
	}
	public void setTime_vehicle_parked(double time_vehicle_parked) {
		this.time_vehicle_parked = time_vehicle_parked;
	}
	boolean isOccupied() {
		return occupied;
	}
	void setOccupied(boolean occupied) {
		this.occupied = occupied;
	}

}
