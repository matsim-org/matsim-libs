package kid.filter;

import kid.KiDSchema;
import kid.Vehicle;

import org.apache.log4j.Logger;


public class LkwGroesser3Punkt5TFilter implements VehicleFilter {

	private static Logger logger = Logger.getLogger(LkwGroesser3Punkt5TFilter.class);
	
	public boolean judge(Vehicle vehicle) {
		String type = vehicle.getAttributes().get(KiDSchema.VEHICLE_TYPE);
		
		if(type.equals("04")){
			return true;
		}
		return false;
	}

}
