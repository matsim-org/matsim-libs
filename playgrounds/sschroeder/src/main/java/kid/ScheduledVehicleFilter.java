package kid;

import kid.filter.Filter;

public interface ScheduledVehicleFilter extends Filter {
	public boolean judge(ScheduledVehicle vehicle);
}
