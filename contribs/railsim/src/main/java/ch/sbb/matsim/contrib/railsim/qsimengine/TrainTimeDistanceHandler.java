package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.PlannedArrival;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.SpeedProfile;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.MatsimServices;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Holds data for time-distance chart and writes CSV file.
 */
public class TrainTimeDistanceHandler {

	private final BufferedWriter writer;

	private final Map<Key, TimeDistanceData> targetData = new HashMap<>();
	private final RailResourceManager resources;

	@Inject
	public TrainTimeDistanceHandler(MatsimServices services, RailResourceManager resources, SpeedProfile speedProfile) throws IOException {
		this.resources = resources;
		String timeDistanceCSV = services.getControllerIO().getIterationFilename(services.getIterationNumber(), "railsimTimeDistance.csv", services.getConfig().controller().getCompressionType());
		writer = Files.newBufferedWriter(Path.of(timeDistanceCSV));

		for (TransitLine line : services.getScenario().getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {

					Vehicle vehicle = services.getScenario().getTransitVehicles().getVehicles().get(departure.getVehicleId());

					Key key = new Key(route.getId(), vehicle.getType().getId());

					targetData.computeIfAbsent(key, k -> initialTimeDistanceData(line, route, departure, vehicle, speedProfile));
				}
			}
		}
	}

	private TimeDistanceData initialTimeDistanceData(TransitLine line, TransitRoute route, Departure departure, Vehicle vehicle, SpeedProfile speedProfile) {

		List<RailLink> links = route.getRoute().getLinkIds().stream().map(resources::getLink).collect(Collectors.toList());
		links.addFirst(resources.getLink(route.getRoute().getStartLinkId()));
		links.add(resources.getLink(route.getRoute().getEndLinkId()));

		SimpleTrainState state = new SimpleTrainState(vehicle, route, links);

		double departureTime = departure.getDepartureTime();
		double currentTime = departureTime;

		for (TransitRouteStop stop : route.getStops()) {

			double arrivalTime = Math.max(currentTime, departureTime + stop.getArrivalOffset().or(stop.getDepartureOffset()).orElse(0));

			List<RailLink> routeUntilNextStop = state.getRouteUntilNextStop();

			for (RailLink link : routeUntilNextStop) {

				PlannedArrival plan = new PlannedArrival(arrivalTime, state.getRouteUntilNextStop());

				// TODO: time needs to be tracked correctly
				double targetSpeed = speedProfile.getTargetSpeed(0, state, plan);

				// TODO: calculate target per link
				// calculate acceleration, distance and correct tine
				// store the result in the data structure
				System.out.println(plan);

				state.nextLink();
			}

			currentTime = arrivalTime;
			state.nextStop();
		}

		return new TimeDistanceData();
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
