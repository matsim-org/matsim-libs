package org.matsim.contrib.eventsBasedPTRouter.stopStopTimes;

import org.matsim.api.core.v01.Id;

import java.io.Serializable;

public interface StopStopTime extends Serializable {

	//Methods
	public double getStopStopTime(Id stopOId, Id stopDId, double time);
	public double getStopStopTimeVariance(Id stopOId, Id stopDId, double time);
		
}
