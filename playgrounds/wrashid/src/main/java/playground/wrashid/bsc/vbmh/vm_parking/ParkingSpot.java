package playground.wrashid.bsc.vbmh.vm_parking;

public class ParkingSpot {
	boolean charge, ev_exclusive;
	private boolean occupied;
	public int chargingRate, parkingPriceM, chargingPriceM;
	public Parking parking;
	private double time_vehicle_parked;
	
	public double getTimeVehicleParked() {
		return time_vehicle_parked;
	}
	public void setTimeVehicleParked(double time_vehicle_parked) {
		this.time_vehicle_parked = time_vehicle_parked;
	}
	boolean isOccupied() {
		return occupied;
	}
	void setOccupied(boolean occupied) {
		this.occupied = occupied;
	}

}
