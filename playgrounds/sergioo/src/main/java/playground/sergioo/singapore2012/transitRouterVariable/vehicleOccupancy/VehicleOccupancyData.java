package playground.sergioo.singapore2012.transitRouterVariable.vehicleOccupancy;

public interface VehicleOccupancyData {

	double getVehicleOccupancy(int i);
	void addVehicleOccupancy(final int timeSlot, final double vehicleOccupancy);
	void resetVehicleOccupancies();

}
