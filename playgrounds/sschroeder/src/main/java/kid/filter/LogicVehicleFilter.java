package kid.filter;

import java.util.ArrayList;
import java.util.List;

import kid.Vehicle;


public abstract class LogicVehicleFilter implements VehicleFilter{
	
	protected List<VehicleFilter> filters = new ArrayList<VehicleFilter>();
	
	public void addFilter(VehicleFilter filter){
		filters.add(filter);
	}

	public abstract boolean judge(Vehicle vehicle);
}
