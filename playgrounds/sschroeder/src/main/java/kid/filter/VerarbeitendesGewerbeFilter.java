package kid.filter;

import kid.KiDSchema;
import kid.Vehicle;

public class VerarbeitendesGewerbeFilter implements VehicleFilter {

	public boolean judge(Vehicle vehicle) {
		String wz = vehicle.getAttributes().get(KiDSchema.COMPANY_WIRTSCHAFTSZWEIG);
		if(wz.equals("D")){
			return true;
		}
		return false;
	}

}
