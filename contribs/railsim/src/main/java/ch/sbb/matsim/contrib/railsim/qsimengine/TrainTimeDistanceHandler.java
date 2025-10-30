package ch.sbb.matsim.contrib.railsim.qsimengine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Inject;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.PlannedArrival;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.SpeedProfile;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;

/**
 * Holds data for time-distance chart and writes CSV file.
 */
public class TrainTimeDistanceHandler {

	private final BufferedWriter writer;

	private final Map<Key, TimeDistanceData> targetData = new HashMap<>();
	private final RailResourceManager resources;
	private final RailsimConfigGroup config;

	@Inject
	public TrainTimeDistanceHandler(MatsimServices services, RailResourceManager resources, SpeedProfile speedProfile) throws IOException {
		this.resources = resources;
		this.config = ConfigUtils.addOrGetModule(services.getConfig(), RailsimConfigGroup.class);
		String timeDistanceCSV = services.getControllerIO().getIterationFilename(services.getIterationNumber(), "railsimTimeDistance.csv", services.getConfig().controller().getCompressionType());
		writer = Files.newBufferedWriter(Path.of(timeDistanceCSV));

		writer.append("vehicle_id,line_id,route_id,departure_id,time,distance,type,link_id,stop_id\n");

		for (TransitLine line : services.getScenario().getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {

					Vehicle vehicle = services.getScenario().getTransitVehicles().getVehicles().get(departure.getVehicleId());

					Key key = new Key(route.getId(), vehicle.getType().getId());

					TimeDistanceData data = targetData.computeIfAbsent(key, k -> initialTimeDistanceData(line, route, departure, vehicle, speedProfile));

					for (TimeDistanceData.Row row : data.rows) {
						writer.append(vehicle.getId().toString()).append(',')
							.append(line.getId().toString()).append(',')
							.append(route.getId().toString()).append(',')
							.append(departure.getId().toString()).append(',')
							.append(String.valueOf(departure.getDepartureTime() + row.time())).append(',')
							.append(String.valueOf(row.distance())).append(',')
							.append("target").append(',')
							.append(Objects.toString(row.linkId(), "")).append(',')
							.append(Objects.toString(row.stopId(), "")).append('\n');
					}
				}
			}
		}
	}

	private TimeDistanceData initialTimeDistanceData(TransitLine line, TransitRoute route, Departure departure, Vehicle vehicle, SpeedProfile speedProfile) {

		List<RailLink> links = route.getRoute().getLinkIds().stream().map(resources::getLink).collect(Collectors.toList());
		links.addFirst(resources.getLink(route.getRoute().getStartLinkId()));
		links.add(resources.getLink(route.getRoute().getEndLinkId()));

		SimpleTrainState position = new SimpleTrainState(vehicle, route, links);

		double a = RailsimUtils.getTrainAcceleration(vehicle.getType(), config);
		double d = RailsimUtils.getTrainDeceleration(vehicle.getType(), config);

		TimeDistanceData data = new TimeDistanceData(line.getId(), route.getId());

		double departureTime = departure.getDepartureTime();
		double currentTime = departureTime;
		double cumulativeDistance = 0.0;

		// TODO: vehicle starts at the end of the transit stop link

		for (TransitRouteStop stop : route.getStops()) {

			double currentSpeed = 0.0;
			double scheduledArrival = departureTime + stop.getArrivalOffset().or(stop.getDepartureOffset()).orElse(0);
			double targetArrivalAtStop = Math.max(currentTime, scheduledArrival);

			List<RailLink> segment = position.getRouteUntilNextStop();

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

				boolean isLastLink = (i == segment.size() - 1);

				double linkTime;
				if (L <= 0) {
					linkTime = 0;
				} else if (isLastLink) {
					// triangular motion within the last link to stop at its end
					double vPeak = Math.min(vLimit, RailsimCalc.calcTargetSpeedForStop(L, a, d, currentSpeed));
					double tAcc = Math.max(0, (vPeak - currentSpeed) / a);
					double tDec = vPeak / d;
					linkTime = tAcc + tDec;
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
				data.add(currentTime, cumulativeDistance, link.getLinkId(),
						isLastLink ? stop.getStopFacility().getId() : null);

				position.nextLink();
				remainingToStop -= L;
			}

			// arrived at stop
			position.nextStop();
		}

		return data;
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
