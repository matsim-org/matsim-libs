package kid.filter;

import kid.KiDUtils;
import kid.ScheduledVehicle;
import kid.ScheduledVehicleFilter;

public class GeoCodableFilter implements ScheduledVehicleFilter {

	public boolean judge(ScheduledVehicle vehicle) {
		if(KiDUtils.isGeoCodable(vehicle)){
			return true;
		}
		return false;
	}

}
