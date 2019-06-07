/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.mobsim.transitperformance;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.TransitRouteImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;

/**
 * An attempt to carve out the WaitTime/StopStopTime based transit emulation.
 * Largely cut & paste from PSim. Not tested with an actual model.
 *
 * @author Gunnar Flötteröd
 *
 */
public class TransitPerformanceFromEventBasedRouterInterfaces implements TransitEmulator {

	private WaitTime waitTimes;
	private StopStopTime stopStopTimes;
	private Map<Id<TransitLine>, TransitLine> transitLines;
	private Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities;

	@Inject
	public TransitPerformanceFromEventBasedRouterInterfaces(WaitTime waitTimes, StopStopTime stopStopTimes,
			TransitSchedule transitSchedule) {
		this.waitTimes = waitTimes;
		this.stopStopTimes = stopStopTimes;
		this.transitLines = transitSchedule.getTransitLines();
		this.stopFacilities = transitSchedule.getFacilities();
	}

	@Override
	public Trip findTrip(Leg prevLeg, double earliestDepartureTime_s) {

		ExperimentalTransitRoute route = (ExperimentalTransitRoute) prevLeg.getRoute();
		TransitLine line = this.transitLines.get(route.getLineId());
		TransitRoute transitRoute = line.getRoutes().get(route.getRouteId());

		final double accessTime_s = earliestDepartureTime_s + this.waitTimes.getRouteStopWaitTime(route.getLineId(),
				transitRoute.getId(), route.getAccessStopId(), earliestDepartureTime_s);
		final double egressTime_s = accessTime_s + this.findTransitTravelTime(route, accessTime_s);
		return new Trip(null, accessTime_s, egressTime_s);
	}

	private double findTransitTravelTime(ExperimentalTransitRoute route, double prevEndTime) {
		double travelTime = 0;
		double prevStopTime = prevEndTime;
		TransitRouteImpl transitRoute = (TransitRouteImpl) transitLines.get(route.getLineId()).getRoutes()
				.get(route.getRouteId());
		// cannot just get the indices of the two transitstops, because
		// sometimes routes visit the same stop facility more than once
		// and the transitroutestop is different for each time it stops at
		// the same facility
		TransitRouteStop orig = transitRoute.getStop(stopFacilities.get(route.getAccessStopId()));
		Id dest = route.getEgressStopId();
		int i = transitRoute.getStops().indexOf(orig);
		// int j = transitRoute.getStops().indexOf(dest);
		// if(i>=j){
		// throw new
		// RuntimeException(String.format("Cannot route from origin stop %s to
		// destination stop %s on route %s.",
		// orig.getStopFacility().getId().toString()
		// ,dest.getStopFacility().getId().toString()
		// ,route.getRouteId().toString()
		// ));
		//
		// }
		boolean destinationFound = false;
		while (i < transitRoute.getStops().size() - 1) {
			Id fromId = transitRoute.getStops().get(i).getStopFacility().getId();
			TransitRouteStop toStop = transitRoute.getStops().get(i + 1);
			Id toId = toStop.getStopFacility().getId();
			travelTime += stopStopTimes.getStopStopTime(fromId, toId, prevStopTime);
			prevStopTime += travelTime;
			if (toStop.getStopFacility().getId().equals(dest)) {
				destinationFound = true;
				break;
			}
			if (toStop.getStopFacility().getId().equals(route.getAccessStopId())) {
				// this is a repeating stop, for routes that loop on themselves more than once
				travelTime = 0;
				prevStopTime = prevEndTime;
			}
			i++;
		}
		if (destinationFound) {
			return travelTime;
		} else {

			return Double.NEGATIVE_INFINITY;
		}
	}

}
