package org.matsim.contribs.discrete_mode_choice.components.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Utility to estimate waiting times in public transport. By default, the
 * ExperimentalTransitRoute given by the MATSim PT router does not give
 * information how long an agent will wait for the next public transit vehicle.
 * However, this is scored in MATSim so it would be good if one can estimate
 * this waiting time in the utility calculation for the discrete mode choice.
 * This class performs such an estimation although it is some computational
 * overhead. Ideally, this information would be included in the route of the
 * agents.
 * 
 * @author sebhoerl
 */
public class ScheduleWaitingTimeEstimator implements PTWaitingTimeEstimator {
	private static final Logger logger = LogManager.getLogger(ScheduleWaitingTimeEstimator.class);

	private final TransitSchedule transitSchedule;
	private final Map<Tuple<Id<TransitLine>, Id<TransitRoute>>, List<Double>> orderedDepartureTimes = new HashMap<>();

	public ScheduleWaitingTimeEstimator(TransitSchedule transitSchedule) {
		this.transitSchedule = transitSchedule;

		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				List<Double> departureTimes = transitRoute.getDepartures().values().stream()
						.map(Departure::getDepartureTime).sorted().collect(Collectors.toList());
				orderedDepartureTimes.put(createId(transitLine, transitRoute), departureTimes);
			}
		}
	}

	private Tuple<Id<TransitLine>, Id<TransitRoute>> createId(TransitLine transitLine, TransitRoute transitRoute) {
		return new Tuple<>(transitLine.getId(), transitRoute.getId());
	}

	@Override
	public double estimateWaitingTime(List<? extends PlanElement> elements) {
		double totalWaitingTime = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg leg && leg.getMode().equals(TransportMode.pt)) {
				TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
				totalWaitingTime += this.estimateWaitingTime(leg.getDepartureTime().seconds(), route);
			}
		}

		return totalWaitingTime;
	}

	@Override
	public double estimateWaitingTime(double agentDepartureTime, TransitPassengerRoute route) {
		TransitLine transitLine = transitSchedule.getTransitLines().get(route.getLineId());
		TransitRoute transitRoute = transitLine.getRoutes().get(route.getRouteId());

		List<Double> departureTimes = orderedDepartureTimes.get(createId(transitLine, transitRoute));

		List<Double> offsets = transitRoute.getStops().stream()
				.filter(stop -> stop.getStopFacility().getId().equals(route.getAccessStopId()))
				.map(TransitRouteStop::getDepartureOffset).map(OptionalTime::seconds).collect(Collectors.toList());

		double minimalWaitingTime = Double.POSITIVE_INFINITY;

		for (double routeDepartureTime : departureTimes) {
			for (double offset : offsets) {
				if (minimalWaitingTime == 0.0) {
					return 0.0;
				}

				double stopDepartureTime = routeDepartureTime + offset;

				if (stopDepartureTime >= agentDepartureTime) {
					double waitingTime = stopDepartureTime - agentDepartureTime;
					minimalWaitingTime = Math.min(minimalWaitingTime, waitingTime);
				}
			}
		}

		if (Double.isFinite(minimalWaitingTime)) {
			return minimalWaitingTime;
		} else {
			logger.error(String.format(
					"Unable to find waiting time for departure on Line %s, Route %s, at Stop %s, after %s. Falling back to 0s.",
					transitLine.getId(), transitRoute.getId(), route.getAccessStopId(),
					Time.writeTime(agentDepartureTime)));
			return 0.0;
		}
	}
}
