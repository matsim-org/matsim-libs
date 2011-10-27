package kid.filter;

import kid.Vehicle;

import java.util.ArrayList;
import java.util.List;


public abstract class LogicVehicleFilter implements VehicleFilter{
	
	protected List<VehicleFilter> filters = new ArrayList<VehicleFilter>();
	
	public void addFilter(VehicleFilter filter){
		filters.add(filter);
	}

	public abstract boolean judge(Vehicle vehicle);
}
