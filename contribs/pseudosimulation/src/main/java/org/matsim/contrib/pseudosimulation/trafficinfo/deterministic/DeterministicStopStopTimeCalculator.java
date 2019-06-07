package org.matsim.contrib.pseudosimulation.trafficinfo.deterministic;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DeterministicStopStopTimeCalculator implements StopStopTimeCalculator {
	private final TransitRouter transitRouter;
	private final TransitSchedule schedule;

	@Inject
	public DeterministicStopStopTimeCalculator(TransitRouter transitRouter, TransitSchedule schedule) {
		this.transitRouter = transitRouter;
		this.schedule = schedule;
	}

	@Override
	public double getStopStopTime(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
		// ATTENTION! This could be optimized. Basically, we probably don't need to
		// route here. However, finding the travel time even on one route is not a
		// computationally efficient task, because the same transit stop id may occur
		// multiple times on the same route.

		TransitStopFacility originFacility = schedule.getFacilities().get(stopOId);
		TransitStopFacility destinationFacility = schedule.getFacilities().get(stopDId);

		List<Leg> legs = transitRouter.calcRoute(originFacility, destinationFacility, time, null);
		return legs.stream().mapToDouble(l -> l.getTravelTime()).sum();
	}

	@Override
	public double getStopStopTimeVariance(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId,
			double time) {
		return 0.0;
	}

	@Override
	public StopStopTime get() {
		return new StopStopTime() {
			@Override
			public double getStopStopTimeVariance(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId,
					double time) {
				return 0.0;
			}

			@Override
			public double getStopStopTime(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId,
					double time) {
				return DeterministicStopStopTimeCalculator.this.getStopStopTime(stopOId, stopDId, time);
			}
		};
	}
}
