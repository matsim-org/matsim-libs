package kid.filter;

import kid.KiDSchema;
import kid.Vehicle;

public class LkwKleiner3Punkt5TFilter implements VehicleFilter {

	public boolean judge(Vehicle vehicle) {
		String type = vehicle.getAttributes().get(KiDSchema.VEHICLE_TYPE);
		if(type.equals("03")){
			return true;
		}
		return false;
	}

}
