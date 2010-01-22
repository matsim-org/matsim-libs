package org.matsim.vis.snapshots.writers;

import org.matsim.api.core.v01.Identifiable;

public interface AgentSnapshotInfo extends Identifiable {

	public enum AgentState { PERSON_AT_ACTIVITY, PERSON_DRIVING_CAR, PERSON_OTHER_MODE, TRANSIT_DRIVER }

	double getEasting();

	double getNorthing();

	@Deprecated
	double getElevation();

	@Deprecated
	double getAzimuth();

	double getColorValueBetweenZeroAndOne();
	void setColorValueBetweenZeroAndOne( double tmp ) ;

	AgentState getAgentState();
	void setAgentState( AgentState state ) ;
	
	int getUserDefined() ;
	void setUserDefined( int tmp ) ; // needs to be a primitive type because of the byte buffer. kai, jan'10

	@Deprecated // yyyy I don't know what this is.  kai, jan'10
	public int getType();

	@Deprecated // yyyy I don't know what this is.  kai, jan'10
	public void setType(int tmp);

}