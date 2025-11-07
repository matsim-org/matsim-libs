package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Holds data for time-distance chart and writes CSV file.
 */
public final class TrainTimeDistanceHandler {

	private final BufferedWriter writer;

	private final Map<Key, TimeDistanceData> targetData = new HashMap<>();

	/**
	 * If true, data will be collected in the map instead of written directly.
	 */
	private boolean collectData = false;

	@Inject
	public TrainTimeDistanceHandler(MatsimServices services) {
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
	void prepareInitialSimulation(RailsimEngine engine, TransitSchedule schedule, Vehicles vehicles) {

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
			writer.append(state.getPt().getVehicle().getId().toString()).append(',')
				.append(state.getPt().getTransitLine().getId().toString()).append(',')
				.append(state.getPt().getTransitRoute().getId().toString()).append(',')
				.append(state.getPt().getDeparture().getId().toString()).append(',')
				.append(String.valueOf(RailsimUtils.round(state.timestamp))).append(',')
				.append(String.valueOf(RailsimUtils.round(state.cumulativeDistance))).append(',')
				.append("simulated").append(',')
				.append(Objects.toString(state.getHeadLink(), "")).append(',')
				.append(atStop != null ? atStop.getId().toString() : "").append('\n');
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
