package org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.lanes.Lane;

public class DefaultFlowEfficiencyCalculator implements FlowEfficiencyCalculator {
	@Override
    public double calculateFlowEfficiency(QVehicle qVehicle, QVehicle previousQVehicle, Double timeGapToPreviousVeh, Link link, Id<Lane> laneId) {
        return qVehicle.getVehicle().getType().getFlowEfficiencyFactor();
	}
}
