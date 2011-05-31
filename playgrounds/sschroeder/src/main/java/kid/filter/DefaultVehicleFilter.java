package kid.filter;

import kid.Vehicle;

public class DefaultVehicleFilter implements VehicleFilter {

	public boolean judge(Vehicle vehicle) {
		return true;
	}

}
