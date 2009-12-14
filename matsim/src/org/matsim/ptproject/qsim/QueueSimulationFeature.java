package org.matsim.ptproject.qsim;

import org.matsim.api.core.v01.network.Link;

public interface QueueSimulationFeature {

	void afterPrepareSim();

	void beforeCleanupSim();

	void beforeHandleAgentArrival(DriverAgent agent);

	void afterAfterSimStep(double time);

	void beforeHandleUnknownLegMode(double now, DriverAgent agent, Link link);

	void afterCreateAgents();

}
