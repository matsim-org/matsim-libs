package org.matsim.application.prepare.pt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Some providers set the same departure and arrival times for multiple stops.
 * This usually happens with on-demand buses, but can also happen on night buses where multiple stops can have a departure at the same minute.
 * These departure times will lead to artifacts when using a pseudo network.
 * The purpose of this class is to spread these departures more evently.
 * However, in the case of on demand buses, the travel times will likely still be too optimistic.
 */
public class AdjustSameDepartureTimes implements Consumer<TransitSchedule> {

	private static Logger log = LogManager.getLogger(AdjustSameDepartureTimes.class);

	private static OptionalTime add(OptionalTime x, double t) {
		if (!x.isDefined()) {
			return x;
		}
		return OptionalTime.defined(x.seconds() + t);
	}

	@Override
	public void accept(TransitSchedule schedule) {
		for (TransitLine line : schedule.getTransitLines().values()) {

			List<TransitRoute> routes = new ArrayList<>(line.getRoutes().values());
			for (TransitRoute route : routes) {
				List<TransitRouteStop> newStops = adjustDepartures(schedule.getFactory(), route);

				if (newStops == null) {
					continue;
				}

				log.info("Adjusted departures for route {} in line {}", route.getId(), line.getId());

				line.removeRoute(route);

				TransitRoute newRoute = schedule.getFactory().createTransitRoute(route.getId(), route.getRoute(), newStops, route.getTransportMode());
				newRoute.setDescription(route.getDescription());
				for (Map.Entry<String, Object> e : route.getAttributes().getAsMap().entrySet()) {
					newRoute.getAttributes().putAttribute(e.getKey(), e.getValue());
				}
				route.getDepartures().values().forEach(newRoute::addDeparture);

				line.addRoute(newRoute);
			}
		}

	}

	private List<TransitRouteStop> adjustDepartures(TransitScheduleFactory f, TransitRoute route) {

		List<TransitRouteStop> stops = new ArrayList<>(route.getStops());

		boolean adjusted = false;

		// Check if the times at the end of the schedule are the same
		// These need to be calculated and can not be interpolated
		// The arrival at the last stop is shifted by small travel time
		if (stops.size() > 1) {
			TransitRouteStop last = stops.getLast();
			TransitRouteStop secondLast = stops.get(stops.size() - 2);

			OptionalTime lastDep = last.getDepartureOffset().or(last.getArrivalOffset());
			OptionalTime secondLastDep = secondLast.getDepartureOffset().or(secondLast.getArrivalOffset());

			if (lastDep.isDefined() && secondLastDep.isDefined() && lastDep.equals(secondLastDep)) {
				// Calculate the time between the last two stops
				double dist = Math.max(20, CoordUtils.calcEuclideanDistance(last.getStopFacility().getCoord(), secondLast.getStopFacility().getCoord()));
				double time = dist / 10; // 10 m/s

				// Calculate the time for the last stop
				TransitRouteStop newStop = f.createTransitRouteStop(
					last.getStopFacility(),
					add(last.getArrivalOffset(), time),
					add(last.getDepartureOffset(), time)
				);

				newStop.setAwaitDepartureTime(last.isAwaitDepartureTime());
				newStop.setAllowAlighting(last.isAllowAlighting());
				newStop.setAllowBoarding(last.isAllowBoarding());

				stops.set(stops.size() - 1, newStop);
				adjusted = true;
			}
		}

		for (int i = 0; i < stops.size() - 1; ) {

			TransitRouteStop stop = stops.get(i);
			OptionalTime dep = stop.getDepartureOffset().or(stop.getArrivalOffset());

			if (!dep.isDefined()) {
				i++;
				continue;
			}

			OptionalTime arr = null;
			int j = i + 1;
			for (; j < stops.size(); j++) {
				TransitRouteStop nextStop = stops.get(j);
				arr = nextStop.getArrivalOffset().or(nextStop.getDepartureOffset());
				if (!dep.equals(arr)) {
					break;
				}
			}

			if (arr == null) {
				i++;
				continue;
			}

			if (j > i + 1) {
				double time = dep.seconds();
				double diff = (arr.seconds() - time) / (j - i);

				for (int k = i + 1; k < j; k++) {
					TransitRouteStop stopToAdjust = stops.get(k);
					int add = (int) (diff * (k - i));

					TransitRouteStop newStop = f.createTransitRouteStop(
						stopToAdjust.getStopFacility(),
						add(stopToAdjust.getArrivalOffset(), add),
						add(stopToAdjust.getDepartureOffset(), add)
					);

					newStop.setAwaitDepartureTime(stopToAdjust.isAwaitDepartureTime());
					newStop.setAllowAlighting(stopToAdjust.isAllowAlighting());
					newStop.setAllowBoarding(stopToAdjust.isAllowBoarding());

					stops.set(k, newStop);
					adjusted = true;
				}
			}

			i = j;
		}

		if (adjusted)
			return stops;

		return null;
	}
}
