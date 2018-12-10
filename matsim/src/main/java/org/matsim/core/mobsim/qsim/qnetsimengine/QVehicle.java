package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;

public interface QVehicle extends QItem, MobsimVehicle {
	void setCurrentLink( Link link );
	
	void setDriver( DriverAgent driver );
	
	double getLinkEnterTime();
	
	void setLinkEnterTime( double linkEnterTime );
	
	double getMaximumVelocity();
	
	double getFlowCapacityConsumptionInEquivalents();
}
