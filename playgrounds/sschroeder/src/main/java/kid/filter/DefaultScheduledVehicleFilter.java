package kid.filter;

import kid.ScheduledVehicle;

public class DefaultScheduledVehicleFilter implements ScheduledVehicleFilter {

	public boolean judge(ScheduledVehicle vehicle) {
		return true;
	}

}
