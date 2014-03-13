package playground.wrashid.bsc.vbmh.vmParking;

public class ParkingSpot {
	boolean charge, evExclusive;
	private boolean occupied;
	public int chargingRate, parkingPriceM, chargingPriceM;
	public Parking parking;
	private double timeVehicleParked;
	
	public double getTimeVehicleParked() {
		return timeVehicleParked;
	}
	public void setTimeVehicleParked(double time_vehicle_parked) {
		this.timeVehicleParked = time_vehicle_parked;
	}
	boolean isOccupied() {
		return occupied;
	}
	void setOccupied(boolean occupied) {
		this.occupied = occupied;
	}

}
