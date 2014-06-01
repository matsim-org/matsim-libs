package playground.vbmh.vmParking;

/**
 * Represents a single parking space.
 * 
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */
public class ParkingSpot {
	boolean charge, evExclusive;
	private boolean occupied;
	double chargingRate;
	public int parkingPriceM, chargingPriceM;
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
	public boolean isCharge() {
		return charge;
	}
	public double getChargingRate() {
		return chargingRate;
	}
	
	public String toString(){
		return "Parking ID "+this.parking.id+ " Occupied "+this.isOccupied()+" charge "+this.isCharge();
	}

}
