package kid.filter;

import kid.KiDSchema;
import kid.Vehicle;

public class PkwPrivatFilter implements VehicleFilter {

	public boolean judge(Vehicle vehicle) {
		String type = vehicle.getAttributes().get(KiDSchema.VEHICLE_TYPE);
		String wz = vehicle.getAttributes().get(KiDSchema.VEHICLE_WIRTSCHAFTSZWEIG);
		if(type.equals("02") && wz.equals("P")){
			return true;
		}
		return false;
	}

}
