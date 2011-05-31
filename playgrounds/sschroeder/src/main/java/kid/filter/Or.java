package kid.filter;

import kid.Vehicle;

public class Or extends LogicVehicleFilter implements VehicleFilter{

	public Or(VehicleFilter f1, VehicleFilter f2){
		filters.add(f1);
		filters.add(f2);
	}
	
	public Or(){}
	
	public boolean judge(Vehicle vehicle) {
		for(VehicleFilter f : filters){
			if(f.judge(vehicle)){
				return true;
			}
		}
		return false;
	}

}
