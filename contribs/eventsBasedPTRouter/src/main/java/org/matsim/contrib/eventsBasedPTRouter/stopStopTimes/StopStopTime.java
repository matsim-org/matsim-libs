package org.matsim.contrib.eventsBasedPTRouter.stopStopTimes;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.Serializable;

public interface StopStopTime extends Serializable {

	//Methods
	public double getStopStopTime(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time);
	public double getStopStopTimeVariance(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time);
		
}
