package org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public class DefaultFlowEfficiencyCalculator implements FlowEfficiencyCalculator {
	@Override
    public double calculateFlowEfficiency(QVehicle qVehicle, QVehicle followingQVehicle, Link link) {
        return qVehicle.getVehicle().getType().getFlowEfficiencyFactor();
	}
}
