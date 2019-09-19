package org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency;

import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public class DefaultFlowEfficiencyCalculator implements FlowEfficiencyCalculator {
	@Override
	public double calculateFlowEfficiency(Vehicle vehicle, Link link) {
		return vehicle.getType().getFlowEfficiencyFactor();
	}
}
