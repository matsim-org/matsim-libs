package org.matsim.contrib.pseudosimulation.trafficinfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
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
 * Measures how long transit vehicles actually took between consecutive stops, per time-of-day bin,
 * and falls back to the timetable's own offset difference wherever a bin was never observed.
 *
 * <p>
 * Adapted from {@code org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorImpl}
 * by sergioo, removed from matsim-libs in October 2023. As with {@link PSimWaitTimeCalculator},
 * events emitted during a PSim iteration are ignored and the measurements are only discarded when
 * the next iteration is a QSim iteration, so a pseudo-simulation reads what the last queue
 * simulation measured rather than what pseudo-simulation itself produced.
 *
 * @author sergioo
 */
@Singleton
public class PSimStopStopTimeCalculator implements StopStopTime, Provider<StopStopTime>,
		VehicleArrivesAtFacilityEventHandler {

	private final double binSize;
	private final int binCount;
	private final MobSimSwitcher switcher;

	private final Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, MeanByTimeBin>> measured = new HashMap<>();
	private final Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, Double>> scheduled = new HashMap<>();
	private final Set<Id<Vehicle>> transitVehicles = new HashSet<>();
	private final Map<Id<Vehicle>, Tuple<Id<TransitStopFacility>, Double>> lastArrival = new HashMap<>();

	@Inject
	public PSimStopStopTimeCalculator(TransitSchedule transitSchedule, Config config, MobSimSwitcher switcher) {
		this(transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(),
				(int) (config.qsim().getEndTime().seconds() - config.qsim().getStartTime().seconds()), switcher);
	}

	public PSimStopStopTimeCalculator(TransitSchedule transitSchedule, double binSize, int totalTime,
			MobSimSwitcher switcher) {
		this.binSize = binSize;
		this.binCount = TimeBinUtils.getTimeBinCount(totalTime, binSize);
		this.switcher = switcher;

		Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, int[]>> observations = new HashMap<>();
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				accumulateScheduled(route, observations);
				for (Departure departure : route.getDepartures().values()) {
					transitVehicles.add(departure.getVehicleId());
				}
			}
		}
		averageScheduled(observations);
	}

	/**
	 * A stop pair can be served by several routes with different offsets, so the timetable
	 * fallback is their mean rather than whichever route happened to be visited last.
	 */
	private void accumulateScheduled(TransitRoute route,
			Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, int[]>> observations) {
		for (int index = 0; index < route.getStops().size() - 1; index++) {
			TransitRouteStop from = route.getStops().get(index);
			TransitRouteStop to = route.getStops().get(index + 1);
			Id<TransitStopFacility> fromId = from.getStopFacility().getId();
			Id<TransitStopFacility> toId = to.getStopFacility().getId();

			measured.computeIfAbsent(fromId, key -> new HashMap<>())
					.computeIfAbsent(toId, key -> new MeanByTimeBin(binCount));

			double offsetDifference = to.getArrivalOffset().or(to::getDepartureOffset).seconds()
					- from.getDepartureOffset().or(from::getArrivalOffset).seconds();
			scheduled.computeIfAbsent(fromId, key -> new HashMap<>()).merge(toId, offsetDifference, Double::sum);
			observations.computeIfAbsent(fromId, key -> new HashMap<>())
					.computeIfAbsent(toId, key -> new int[1])[0]++;
		}
	}

	private void averageScheduled(Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, int[]>> observations) {
		for (Map.Entry<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, Double>> from : scheduled.entrySet()) {
			for (Map.Entry<Id<TransitStopFacility>, Double> to : from.getValue().entrySet()) {
				int count = observations.get(from.getKey()).get(to.getKey())[0];
				to.setValue(to.getValue() / count);
			}
		}
	}

	@Override
	public StopStopTime get() {
		return this;
	}

	@Override
	public double getStopStopTime(Id<TransitStopFacility> fromStopId, Id<TransitStopFacility> toStopId, double time) {
		Map<Id<TransitStopFacility>, MeanByTimeBin> from = measured.get(fromStopId);
		if (from == null) {
			return 0.0;
		}
		MeanByTimeBin times = from.get(toStopId);
		if (times == null) {
			return 0.0;
		}
		int bin = times.binOf(time, binSize);
		if (times.count(bin) > 0) {
			return times.mean(bin);
		}
		return scheduled.get(fromStopId).get(toStopId);
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
		for (Map<Id<TransitStopFacility>, MeanByTimeBin> from : measured.values()) {
			for (MeanByTimeBin times : from.values()) {
				times.reset();
			}
		}
		lastArrival.clear();
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if (!recording() || !transitVehicles.contains(event.getVehicleId())) {
			return;
		}
		Tuple<Id<TransitStopFacility>, Double> previous = lastArrival.put(event.getVehicleId(),
				new Tuple<>(event.getFacilityId(), event.getTime()));
		if (previous == null) {
			return;
		}
		Map<Id<TransitStopFacility>, MeanByTimeBin> from = measured.get(previous.getFirst());
		if (from == null) {
			return;
		}
		MeanByTimeBin times = from.get(event.getFacilityId());
		if (times != null) {
			times.add(times.binOf(previous.getSecond(), binSize), event.getTime() - previous.getSecond());
		}
	}
}
