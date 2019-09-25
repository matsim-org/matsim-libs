package org.matsim.contrib.eventsBasedPTRouter.stopStopTimes;

import com.google.inject.Provider;
import com.google.inject.Provides;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;



public interface StopStopTimeCalculator extends Provider<StopStopTime> {

	//Methods
	double getStopStopTime(Id<org.matsim.facilities.Facility> stopOId, Id<org.matsim.facilities.Facility> stopDId, double time);

	double getStopStopTimeVariance(Id<org.matsim.facilities.Facility> stopOId, Id<org.matsim.facilities.Facility> stopDId, double time);

	@Override
	@Provides
	StopStopTime get();
}
