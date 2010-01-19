package org.matsim.vis.snapshots.writers;

import org.matsim.api.core.v01.Identifiable;

public interface AgentSnapshotInfo extends Identifiable {

	public enum AgentState {AGENT_MOVING, AGENT_AT_ACTIVITY, TRANSIT_VEHICLE }

	double getEasting();

	double getNorthing();

	double getElevation();

	double getAzimuth();

	double getSpeed();

	AgentState getAgentState();

}