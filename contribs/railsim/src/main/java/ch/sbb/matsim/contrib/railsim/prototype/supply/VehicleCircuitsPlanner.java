package ch.sbb.matsim.contrib.railsim.prototype.supply;

import java.util.List;
import java.util.Map;

/**
 * Vehicle circuits planner
 * <p>
 * Takes a list of (built) transit line information objects, plan circuits and allocates a vehicle to each departure.
 *
 * @author Merlin Unterfinger
 */
public interface VehicleCircuitsPlanner {
	Map<TransitLineInfo, VehicleAllocationInfo> plan(List<TransitLineInfo> transitLineInfos);
}
