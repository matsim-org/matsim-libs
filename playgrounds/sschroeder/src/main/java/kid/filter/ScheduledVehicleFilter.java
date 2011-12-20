package kid.filter;

import kid.ScheduledVehicle;

public interface ScheduledVehicleFilter extends Filter {
	public boolean judge(ScheduledVehicle vehicle);
}
