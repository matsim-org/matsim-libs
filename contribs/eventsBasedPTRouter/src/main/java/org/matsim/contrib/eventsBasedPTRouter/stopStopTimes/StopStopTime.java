package org.matsim.contrib.eventsBasedPTRouter.stopStopTimes;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.Serializable;

public interface StopStopTime extends Serializable {

	//Methods
	public double getStopStopTime(Id<org.matsim.facilities.Facility> stopOId, Id<org.matsim.facilities.Facility> stopDId, double time);
	public double getStopStopTimeVariance(Id<org.matsim.facilities.Facility> stopOId, Id<org.matsim.facilities.Facility> stopDId, double time);
		
}
