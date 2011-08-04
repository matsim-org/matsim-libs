package kid.filter;

import kid.KiDSchema;
import kid.Vehicle;

public class KernstadtFilter implements VehicleFilter {

	@Override
	public boolean judge(Vehicle vehicle) {
		String agglomerationsTyp = vehicle.getAttributes().get(KiDSchema.COMPANY_KREISTYP);
		if(agglomerationsTyp.equals("1")){
			return true;
		}
		return false;
	}

}
