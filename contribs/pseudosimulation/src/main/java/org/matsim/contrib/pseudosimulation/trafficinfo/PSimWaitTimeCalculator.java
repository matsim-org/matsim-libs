package org.matsim.contrib.pseudosimulation.trafficinfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.trafficmonitoring.TimeBinUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Measures how long passengers actually waited at each stop of each route, per time-of-day bin,
 * and falls back to the wait implied by the timetable wherever a bin was never observed.
 *
 * <p>
 * Adapted from {@code org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculatorImpl} by
 * sergioo, removed from matsim-libs in October 2023, with two changes that pseudo-simulation
 * requires. Both are about keeping the preceding queue simulation's measurements intact:
 *
 * <ul>
 * <li>events emitted during a PSim iteration are ignored, because a PSim iteration derives its
 * boarding times from this very structure and feeding them back would close a loop;</li>
 * <li>the measurements are only discarded when the next iteration is a QSim iteration.</li>
 * </ul>
 *
 * <p>
 * Register this object itself as the event handler. Registering a delegate would leave the guard
 * below inert, which is precisely the defect that made PSimTravelTimeCalculator's guard dead code.
 *
 * @author sergioo
 */
@Singleton
public class PSimWaitTimeCalculator implements WaitTime, Provider<WaitTime>,
		PersonDepartureEventHandler, PersonEntersVehicleEventHandler,
		TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler {

	private final double binSize;
	private final int binCount;
	private final MobSimSwitcher switcher;

	private final Map<Tuple<Id<TransitLine>, Id<TransitRoute>>, Map<Id<TransitStopFacility>, MeanByTimeBin>> measured =
			new HashMap<>();
	private final Map<Tuple<Id<TransitLine>, Id<TransitRoute>>, Map<Id<TransitStopFacility>, double[]>> scheduled =
			new HashMap<>();

	private final Map<Id<Person>, Double> waitingSince = new HashMap<>();
	private final Map<Id<Vehicle>, Tuple<Id<TransitLine>, Id<TransitRoute>>> lineRouteOfVehicle = new HashMap<>();
	private final Map<Id<Vehicle>, Id<TransitStopFacility>> stopOfVehicle = new HashMap<>();

	@Inject
	public PSimWaitTimeCalculator(TransitSchedule transitSchedule, Config config, MobSimSwitcher switcher) {
		this(transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(),
				(int) (config.qsim().getEndTime().seconds() - config.qsim().getStartTime().seconds()), switcher);
	}

	public PSimWaitTimeCalculator(TransitSchedule transitSchedule, double binSize, int totalTime,
			MobSimSwitcher switcher) {
		this.binSize = binSize;
		this.binCount = TimeBinUtils.getTimeBinCount(totalTime, binSize);
		this.switcher = switcher;
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				Tuple<Id<TransitLine>, Id<TransitRoute>> key = new Tuple<>(line.getId(), route.getId());
				measured.put(key, measuredStopsOf(route));
				scheduled.put(key, scheduledStopsOf(route));
			}
		}
	}

	private Map<Id<TransitStopFacility>, MeanByTimeBin> measuredStopsOf(TransitRoute route) {
		Map<Id<TransitStopFacility>, MeanByTimeBin> stops = new HashMap<>();
		for (TransitRouteStop stop : route.getStops()) {
			stops.put(stop.getStopFacility().getId(), new MeanByTimeBin(binCount));
		}
		return stops;
	}

	/**
	 * The wait a passenger arriving at the end of each bin would face before the next scheduled
	 * arrival of this route, wrapping past midnight when the route has no later departure.
	 */
	private Map<Id<TransitStopFacility>, double[]> scheduledStopsOf(TransitRoute route) {
		double[] departures = new double[route.getDepartures().size()];
		int index = 0;
		for (Departure departure : route.getDepartures().values()) {
			departures[index++] = departure.getDepartureTime();
		}
		Arrays.sort(departures);

		Map<Id<TransitStopFacility>, double[]> stops = new HashMap<>();
		for (TransitRouteStop stop : route.getStops()) {
			double offset = stop.getArrivalOffset().or(stop::getDepartureOffset).seconds();
			double[] waits = new double[binCount];
			for (int bin = 0; bin < binCount; bin++) {
				waits[bin] = scheduledWaitAt(departures, offset, binSize * (bin + 1));
			}
			stops.put(stop.getStopFacility().getId(), waits);
		}
		return stops;
	}

	private static double scheduledWaitAt(double[] sortedDepartures, double stopOffset, double time) {
		if (sortedDepartures.length == 0) {
			return 0.0;
		}
		for (double departure : sortedDepartures) {
			double arrival = departure + stopOffset;
			if (arrival >= time) {
				return arrival - time;
			}
		}
		return sortedDepartures[0] + 24 * 3600 + stopOffset - time;
	}

	@Override
	public WaitTime get() {
		return this;
	}

	@Override
	public double getRouteStopWaitTime(Id<TransitLine> lineId, Id<TransitRoute> routeId,
			Id<TransitStopFacility> stopId, double time) {
		Tuple<Id<TransitLine>, Id<TransitRoute>> key = new Tuple<>(lineId, routeId);
		Map<Id<TransitStopFacility>, MeanByTimeBin> stops = measured.get(key);
		if (stops == null) {
			return 0.0;
		}
		MeanByTimeBin waits = stops.get(stopId);
		if (waits == null) {
			return 0.0;
		}
		int bin = waits.binOf(time, binSize);
		if (waits.count(bin) > 0) {
			return waits.mean(bin);
		}
		return scheduled.get(key).get(stopId)[bin];
	}

	/** A PSim iteration reads these measurements; it must not also write to them. */
	private boolean recording() {
		return switcher == null || switcher.isQSimIteration();
	}

	@Override
	public void reset(int iteration) {
		if (!recording()) {
			return;
		}
		for (Map<Id<TransitStopFacility>, MeanByTimeBin> stops : measured.values()) {
			for (MeanByTimeBin waits : stops.values()) {
				waits.reset();
			}
		}
		waitingSince.clear();
		lineRouteOfVehicle.clear();
		stopOfVehicle.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if (!recording()) {
			return;
		}
		lineRouteOfVehicle.put(event.getVehicleId(),
				new Tuple<>(event.getTransitLineId(), event.getTransitRouteId()));
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if (!recording()) {
			return;
		}
		if (lineRouteOfVehicle.containsKey(event.getVehicleId())) {
			stopOfVehicle.put(event.getVehicleId(), event.getFacilityId());
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!recording()) {
			return;
		}
		if (TransportMode.pt.equals(event.getLegMode())) {
			waitingSince.putIfAbsent(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!recording()) {
			return;
		}
		Double since = waitingSince.remove(event.getPersonId());
		if (since == null) {
			return;
		}
		Tuple<Id<TransitLine>, Id<TransitRoute>> lineRoute = lineRouteOfVehicle.get(event.getVehicleId());
		Id<TransitStopFacility> stopId = stopOfVehicle.get(event.getVehicleId());
		if (lineRoute == null || stopId == null) {
			return;
		}
		MeanByTimeBin waits = measured.get(lineRoute).get(stopId);
		if (waits != null) {
			waits.add(waits.binOf(since, binSize), event.getTime() - since);
		}
	}
}
