package org.matsim.vis.snapshots.writers;

import org.matsim.api.core.v01.Identifiable;

public interface AgentSnapshotInfo extends Identifiable {

	public enum AgentState {AGENT_MOVING, AGENT_AT_ACTIVITY, TRANSIT_VEHICLE }

	double getEasting();

	double getNorthing();

	double getElevation();

	double getAzimuth();

	double getSpeed();
	void setSpeed( double tmp ) ;

	AgentState getAgentState();
	void setAgentState( AgentState state ) ;
	
	int getUserDefined() ;
	void setUserDefined( int tmp ) ;

	@Deprecated // yyyy I don't know what this is.  kai, jan'10
	public int getType();

	@Deprecated // yyyy I don't know what this is.  kai, jan'10
	public void setType(int tmp);

}