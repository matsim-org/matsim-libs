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
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;

/**
 * An attempt to carve out the TransitPerformance based transit emulation.
 * Largely cut & paste from PSim. Not tested with an actual model.
 *
 * @author Gunnar Flötteröd
 *
 */
public class TransitPerformanceFromPSimSpecificImplementation implements TransitEmulator {

	private TransitPerformance transitPerformance;

	private Map<Id<TransitLine>, TransitLine> transitLines;

	@Inject
	public TransitPerformanceFromPSimSpecificImplementation(TransitPerformance transitPerformance,
			TransitSchedule transitSchedule) {
		this.transitPerformance = transitPerformance;
		this.transitLines = transitSchedule.getTransitLines();
	}

	@Override
	public Trip findTrip(Leg prevLeg, double earliestDepartureTime_s) {

		ExperimentalTransitRoute route = (ExperimentalTransitRoute) prevLeg.getRoute();
		Id accessStopId = route.getAccessStopId();
		Id egressStopId = route.getEgressStopId();

		Tuple<Double, Double> routeTravelTime = transitPerformance.getRouteTravelTime(route.getLineId(),
				route.getRouteId(), accessStopId, egressStopId, earliestDepartureTime_s);
		final double accessTime_s = earliestDepartureTime_s + routeTravelTime.getFirst();
		final double egressTime_s = accessTime_s + routeTravelTime.getSecond();
		return new Trip(accessTime_s, egressTime_s);
	}
}
