package org.matsim.contrib.eventsBasedPTRouter.waitTimes;

import com.google.inject.Provider;
import com.google.inject.Provides;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


public interface WaitTimeCalculator extends Provider<WaitTime> {
	double getRouteStopWaitTime(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<TransitStopFacility> stopId, double time);

	@Override
	@Provides
	WaitTime get();
}
