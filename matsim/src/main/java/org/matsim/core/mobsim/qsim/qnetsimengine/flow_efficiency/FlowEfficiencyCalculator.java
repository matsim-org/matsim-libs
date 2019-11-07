package org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public interface FlowEfficiencyCalculator {

	/**
	 * @param qVehicle          the vehicle that consumes efficiency
	 * @param followingQVehicle - may be null
	 * @param link              - the link the qVehicle is currently on
	 * @return
	 */
	double calculateFlowEfficiency(QVehicle qVehicle, QVehicle followingQVehicle, Link link);
}
