package kid.filter;

import kid.Vehicle;

public class And extends LogicVehicleFilter implements VehicleFilter{

	public And(VehicleFilter f1, VehicleFilter f2){
		filters.add(f1);
		filters.add(f2);
	}
	
	public And(){}
	
	public boolean judge(Vehicle vehicle) {
		for(VehicleFilter f : filters){
			if(!f.judge(vehicle)){
				return false;
			}
		}
		return true;
	}
	
}
