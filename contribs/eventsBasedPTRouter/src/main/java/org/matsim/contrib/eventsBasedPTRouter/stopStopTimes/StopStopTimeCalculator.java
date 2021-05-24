package org.matsim.contrib.eventsBasedPTRouter.stopStopTimes;

import com.google.inject.Provider;
import com.google.inject.Provides;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;



public interface StopStopTimeCalculator extends Provider<StopStopTime> {

	//Methods
	double getStopStopTime(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time);

	double getStopStopTimeVariance(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time);

	@Override
	@Provides
	StopStopTime get();
}
