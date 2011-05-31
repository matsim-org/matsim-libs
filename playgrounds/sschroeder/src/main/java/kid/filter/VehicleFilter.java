package kid.filter;

import kid.Vehicle;

public interface VehicleFilter extends Filter {
	
	public boolean judge(Vehicle vehicle);

}
