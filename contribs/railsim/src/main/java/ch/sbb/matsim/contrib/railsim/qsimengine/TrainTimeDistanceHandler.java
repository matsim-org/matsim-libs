package ch.sbb.matsim.contrib.railsim.qsimengine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Inject;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.PlannedArrival;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.SpeedProfile;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;

import static ch.sbb.matsim.contrib.railsim.RailsimUtils.objectIdToString;

/**
 * Holds data for time-distance chart and writes CSV file.
 */
public final class TrainTimeDistanceHandler {

	private final BufferedWriter writer;

	private final RailsimConfigGroup config;
	private final RailResourceManager resources;
	private final Map<Key, TimeDistanceData> targetData = new HashMap<>();

	/**
	 * If true, data will be collected in the map instead of written directly.
	 */
	private boolean collectData = false;

	@Inject
	public TrainTimeDistanceHandler(MatsimServices services, RailResourceManager resources) {
		this.config = ConfigUtils.addOrGetModule(services.getConfig(), RailsimConfigGroup.class);
		this.resources = resources;
		try {
			this.writer = prepareWriter(services);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private BufferedWriter prepareWriter(MatsimServices services) throws IOException {

		String timeDistanceCSV = services.getControllerIO().getIterationFilename(services.getIterationNumber(), "railsimTimeDistance.csv", services.getConfig().controller().getCompressionType());

		BufferedWriter writer = Files.newBufferedWriter(Path.of(timeDistanceCSV));
		writer.append("vehicle_id,line_id,route_id,departure_id,time,distance,type,link_id,stop_id\n");

		return writer;
	}

	/**
	 * Create departure data for a simple simulation.
	 */
	void preparePseudoSimulation(RailsimEngine engine, TransitSchedule schedule, Vehicles vehicles) {

		collectData = true;

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {

					Vehicle vehicle = vehicles.getVehicles().get(departure.getVehicleId());

					Key key = new Key(route.getId(), vehicle.getType().getId());

					if (targetData.containsKey(key))
						continue;

					targetData.put(key, new TimeDistanceData(line.getId(), route.getId()));

					List<TransitRouteStop> stops = route.getStops();

					List<Id<Link>> routeLinkIds = new ArrayList<>(route.getRoute().getLinkIds());
					routeLinkIds.addFirst(route.getRoute().getStartLinkId());
					routeLinkIds.add(route.getRoute().getEndLinkId());

					int fromRouteIdx = 0;

					for (int i = 0; i < stops.size() - 1; i++) {

						TransitRouteStop stop = stops.get(i);
						TransitRouteStop nextStop = stops.get(i + 1);

						Id<Link> nextLinkId = nextStop.getStopFacility().getLinkId();

						int toRouteIdx = fromRouteIdx + routeLinkIds.subList(fromRouteIdx, routeLinkIds.size()).indexOf(nextLinkId);

						List<Id<Link>> routeSegment = routeLinkIds.subList(fromRouteIdx, toRouteIdx + 1);

						NetworkRoute networkRoute = RouteUtils.createNetworkRoute(routeSegment);

						double time = stop.getDepartureOffset().orElse(stop.getArrivalOffset().orElse(0));

						MobsimDriverAgent driver = new SimpleRailsimAgent(
							line, route, departure, i, networkRoute, vehicle);

						engine.handleDeparture(
							time,
							driver,
							stop.getStopFacility().getLinkId(),
							networkRoute
						);

					}
				}
			}
		}
	}

	/**
	 * Simple calculation to approximate target time distance data.
	 */
	void prepareTimeDistanceApproximation(TransitSchedule schedule, Vehicles vehicles, SpeedProfile speedProfile) {

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {

					Vehicle vehicle = vehicles.getVehicles().get(departure.getVehicleId());

					Key key = new Key(route.getId(), vehicle.getType().getId());
					targetData.computeIfAbsent(key, k -> initialTimeDistanceData(line, route, vehicle, speedProfile));

				}
			}
		}
	}

	/**
	 * Calculate the time distance data for a single route using approximation only considering acceleration.
	 */
	private TimeDistanceData initialTimeDistanceData(TransitLine line, TransitRoute route, Vehicle vehicle, SpeedProfile speedProfile) {

		List<RailLink> links = route.getRoute().getLinkIds().stream().map(resources::getLink).collect(Collectors.toList());
		links.addFirst(resources.getLink(route.getRoute().getStartLinkId()));
		links.add(resources.getLink(route.getRoute().getEndLinkId()));

		SimpleTrainState position = new SimpleTrainState(vehicle, route, links);

		double a = RailsimUtils.getTrainAcceleration(vehicle.getType(), config);
		double d = RailsimUtils.getTrainDeceleration(vehicle.getType(), config);

		TimeDistanceData data = new TimeDistanceData(line.getId(), route.getId());

		List<TransitRouteStop> stops = new ArrayList<>(route.getStops());

		TransitRouteStop first = stops.removeFirst();

		// Train is starting at the first stop
		data.add(0, 0, links.getFirst().getLinkId(), first.getStopFacility().getId());

		position.nextLink();
		position.nextStop();

		double departureTime = first.getDepartureOffset().orElse(0);
		double currentTime = departureTime;
		double cumulativeDistance = 0.0;

		for (TransitRouteStop stop : stops) {

			List<RailLink> segment = position.getRouteUntilNextStop();

			double currentSpeed = 0.0;
			double scheduledArrival = departureTime + stop.getArrivalOffset().or(stop.getDepartureOffset()).orElse(0);
			double targetArrivalAtStop = Math.max(currentTime, scheduledArrival);

			// compute remaining distance to the stop (over all links in this segment)
			double remainingToStop = 0.0;
			for (RailLink l : segment)
				remainingToStop += l.getLength();

			for (int i = 0; i < segment.size(); i++) {
				RailLink link = segment.get(i);
				double L = link.getLength();

				// target speed from profile with graceful fallback
				double profileTarget = speedProfile.getTargetSpeed(currentTime, position, new PlannedArrival(targetArrivalAtStop, position.getRouteUntilNextStop()));

				double allowed = link.getAllowedFreespeed(position.getDriver());
				double vLimit = Math.min(allowed, Double.isFinite(profileTarget) ? profileTarget : allowed);

				// Decelration to speed limits is not considered because this would involve the whole simulation logic
				// Instead assume the train is already at the speed limit when traversing the link
				if (currentSpeed > vLimit) {
					currentSpeed = vLimit;
				}

				boolean isLastLink = (i == segment.size() - 1);

				double linkTime;
				if (isLastLink) {
					// Calculate peak speed: either limited by vLimit or by what's needed to stop
					double vPeakTriangular = RailsimCalc.calcTargetSpeedForStop(L, a, d, currentSpeed);
					double vPeak = Math.min(vLimit, vPeakTriangular);

					// Calculate distances for acceleration and deceleration
					double tAcc = Math.max(0, (vPeak - currentSpeed) / a);
					double sAcc = RailsimCalc.calcTraveledDist(currentSpeed, tAcc, a);
					double tDec = vPeak / d;
					double sDec = RailsimCalc.calcTraveledDist(vPeak, tDec, -d);

					// Check if there's a cruising phase
					if (FuzzyUtils.lessThan(sAcc + sDec, L)) {
						// Train can cruise at vPeak before decelerating
						double sCruise = L - sAcc - sDec;
						double tCruise = sCruise / Math.max(1e-9, vPeak);
						linkTime = tAcc + tCruise + tDec;
					} else {
						// Triangular motion: need to recalculate vPeak to fit exactly within L
						vPeak = vPeakTriangular;
						tAcc = Math.max(0, (vPeak - currentSpeed) / a);
						tDec = vPeak / d;
						linkTime = tAcc + tDec;
					}
				} else {
					// intermediate link: accelerate up to bounded peak and possibly cruise
					double vReq = RailsimCalc.calcTargetSpeedForStop(remainingToStop, a, d, currentSpeed);
					double vTarget = Math.min(vLimit, vReq);

					// can we reach vTarget within this link?
					double sAcc = vTarget > currentSpeed ? (vTarget * vTarget - currentSpeed * currentSpeed) / (2 * a) : 0.0;
					if (sAcc >= L) {
						// pure acceleration on this link
						linkTime = RailsimCalc.solveTraveledDist(currentSpeed, L, a);
						currentSpeed = Math.min(vTarget, Math.sqrt(Math.max(0, currentSpeed * currentSpeed + 2 * a * L)));
					} else {
						double tAcc = currentSpeed < vTarget ? (vTarget - currentSpeed) / a : 0.0;
						double rem = L - sAcc;
						double tCruise = rem / Math.max(1e-9, vTarget);
						linkTime = tAcc + tCruise;
						currentSpeed = vTarget;
					}
				}

				currentTime += linkTime;
				cumulativeDistance += L;

				Id<TransitStopFacility> nextStopId = position.getNextStop() != null ? position.getNextStop().getId() : null;
				data.add(currentTime, cumulativeDistance, link.getLinkId(), isLastLink ? nextStopId : null);

				if (isLastLink) {
					// Time at which the train is still present at the stop
					double scheduledDeparture = departureTime + stop.getDepartureOffset().or(stop.getArrivalOffset()).orElse(0);
					data.add(scheduledDeparture, cumulativeDistance, link.getLinkId(), nextStopId);
				}

				position.nextLink();
				remainingToStop -= L;
			}

			// arrived at stop
			position.nextStop();
		}

		data.createIndex();

		return data;
	}

	/**
	 * Needs to be called after the initial simulation was performed to write the collected data.
	 */
	void writeInitialData(TransitSchedule schedule, Vehicles vehicles) {

		collectData = false;

		targetData.values().forEach(TimeDistanceData::createIndex);

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {

					Vehicle vehicle = vehicles.getVehicles().get(departure.getVehicleId());

					Key key = new Key(route.getId(), vehicle.getType().getId());

					TimeDistanceData data = targetData.get(key);

					for (TimeDistanceData.Row row : data.rows) {
						try {
							writer.append(vehicle.getId().toString()).append(',')
								.append(line.getId().toString()).append(',')
								.append(route.getId().toString()).append(',')
								.append(departure.getId().toString()).append(',')
								.append(String.valueOf(RailsimUtils.round(departure.getDepartureTime() + row.time()))).append(',')
								.append(String.valueOf(RailsimUtils.round(row.distance()))).append(',')
								.append("target").append(',')
								.append(Objects.toString(row.linkId(), "")).append(',')
								.append(Objects.toString(row.stopId(), "")).append('\n');
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}
				}
			}
		}


	}

	void writePosition(TrainState state, TransitStopFacility atStop) {

		if (state.getPt() == null)
			return;

		if (collectData) {

			Key key = new Key(state.getPt().getTransitRoute().getId(),
				state.getDriver().getVehicle().getVehicle().getType().getId());

			TimeDistanceData data = Objects.requireNonNull(targetData.get(key), "Time Distance data must not be null");
			data.add(state.timestamp, state.cumulativeDistance, state.headLink, atStop == null ? null : atStop.getId());
			return;
		}

		try {
			writer.append(objectIdToString(state.pt.getVehicle())).append(',')
				.append(objectIdToString(state.pt.getTransitLine())).append(',')
				.append(objectIdToString(state.pt.getTransitRoute())).append(',')
				.append(objectIdToString(state.pt.getDeparture())).append(',')
				.append(String.valueOf(RailsimUtils.round(state.timestamp))).append(',')
				.append(String.valueOf(RailsimUtils.round(state.cumulativeDistance))).append(',')
				.append("simulated").append(',')
				.append(Objects.toString(state.getHeadLink(), "")).append(',')
				.append(objectIdToString(atStop)).append('\n');
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Calculate the approximate delay and update the state.
	 */
	void calculateDelay(TrainState state) {

		if (state.pt == null)
			return;

		// Between routes during circulation the transit route is null
		if (state.pt.getTransitRoute() == null)
			return;

		Id<TransitRoute> transitRoute = state.pt.getTransitRoute().getId();
		Id<VehicleType> vehicleType = state.pt.getVehicle().getVehicle().getType().getId();

		TimeDistanceData data = targetData.get(new Key(transitRoute, vehicleType));

		if (data == null || data.distances == null)
			return;

		double departureTime = state.pt.getDeparture().getDepartureTime();

		state.delay = data.calcDelay(departureTime, state.cumulativeDistance, state.timestamp);

	}

	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private record Key(Id<TransitRoute> transitRoute, Id<VehicleType> vehicleType) {
	}

}
