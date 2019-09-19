package org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency;

import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public interface FlowEfficiencyCalculator {
	double calculateFlowEfficiency(Vehicle vehicle, Link link);
}
