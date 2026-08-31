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
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * An attempt to carve out the TransitPerformance based transit emulation.
 * Largely cut & paste from PSim.
 *
 * @author Gunnar Flötteröd
 *
 */
public class TransitPerformanceFromPSimSpecificImplementation implements TransitEmulator {

	private final Provider<TransitPerformance> transitPerformance;

	private Map<Id<TransitLine>, TransitLine> transitLines;

	/**
	 * TransitPerformanceRecorder replaces its TransitPerformance on every QSim iteration, so the
	 * emulator has to resolve the current one per lookup rather than capture one at construction.
	 */
	@Inject
	public TransitPerformanceFromPSimSpecificImplementation(Provider<TransitPerformance> transitPerformance,
			TransitSchedule transitSchedule) {
		this.transitPerformance = transitPerformance;
		this.transitLines = transitSchedule.getTransitLines();
	}

	public TransitPerformanceFromPSimSpecificImplementation(TransitPerformance transitPerformance,
			TransitSchedule transitSchedule) {
		this(() -> transitPerformance, transitSchedule);
	}

	@Override
	public Trip findTrip(Leg prevLeg, double earliestDepartureTime_s) {

		TransitPassengerRoute route = (TransitPassengerRoute) prevLeg.getRoute();
		Id<TransitStopFacility> accessStopId = route.getAccessStopId();
		Id<TransitStopFacility> egressStopId = route.getEgressStopId();

		Tuple<Double, Double> routeTravelTime = transitPerformance.get().getRouteTravelTime(route.getLineId(),
				route.getRouteId(), accessStopId, egressStopId, earliestDepartureTime_s);
		final double accessTime_s = earliestDepartureTime_s + routeTravelTime.getFirst();
		final double egressTime_s = accessTime_s + routeTravelTime.getSecond();
		return new Trip(null, accessTime_s, egressTime_s);
	}
}
