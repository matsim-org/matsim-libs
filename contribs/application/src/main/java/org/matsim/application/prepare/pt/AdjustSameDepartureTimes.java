package org.matsim.application.prepare.pt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
				line.addRoute(newRoute);
			}
		}

	}

	private List<TransitRouteStop> adjustDepartures(TransitScheduleFactory f, TransitRoute route) {

		List<TransitRouteStop> stops = new ArrayList<>(route.getStops());

		boolean adjusted = false;
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
