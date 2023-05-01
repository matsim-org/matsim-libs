package ch.sbb.matsim.contrib.railsim.prototype;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.DefaultLinkSpeedCalculator;
import org.matsim.core.network.NetworkUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.HashSet;
import java.util.Set;

/**
 * A simple link speed calculator which accounts for the acceleration and deceleration of vehicles.
 *
 * @author Ihab Kaddoura
 */
public class RailsimLinkSpeedCalculatorImpl implements TransitDriverStartsEventHandler, RailsimLinkSpeedCalculator {
	private static final Logger log = LogManager.getLogger(RailsimLinkSpeedCalculatorImpl.class);

	DefaultLinkSpeedCalculator defaultLinkSpeedCalculator = new DefaultLinkSpeedCalculator();
	Set<Id<Vehicle>> transitVehicles = new HashSet<>();

	@Inject
	Scenario scenario;

	@Inject
	TrainStatistics statistics;

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		if (isTrain(vehicle)) {
			return getRailsimMaximumVelocity(vehicle.getVehicle(), link, time);
		} else {
			return defaultLinkSpeedCalculator.getMaximumVelocity(vehicle, link, time);
		}
	}

	@Override
	public double getRailsimMaximumVelocity(Vehicle vehicle, Link link, double time) {

		RailsimConfigGroup railSimConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimConfigGroup.class);

		double freespeed = Double.MIN_VALUE;

		{
			final double defaultFreespeed = Math.min(vehicle.getType().getMaximumVelocity(), link.getFreespeed(time));
			if (railSimConfigGroup.getTrainSpeedApproach() == RailsimConfigGroup.TrainSpeedApproach.constantValue) {
				freespeed = defaultFreespeed;

			} else if (railSimConfigGroup.getTrainSpeedApproach() == RailsimConfigGroup.TrainSpeedApproach.fromLinkAttributesForEachVehicleType) {
				freespeed = RailsimUtils.getLinkFreespeedForVehicleType(vehicle.getType().getId(), link);

			} else if (railSimConfigGroup.getTrainSpeedApproach() == RailsimConfigGroup.TrainSpeedApproach.fromLinkAttributesForEachLine) {
				Id<TransitLine> line = statistics.getVehicleId2currentTransitLine().get(vehicle.getId());
				freespeed = RailsimUtils.getLinkFreespeedForTransitLine(line, link);

			} else if (railSimConfigGroup.getTrainSpeedApproach() == RailsimConfigGroup.TrainSpeedApproach.fromLinkAttributesForEachLineAndRoute) {
				Id<TransitLine> line = statistics.getVehicleId2currentTransitLine().get(vehicle.getId());
				Id<TransitRoute> route = statistics.getVehicleId2currentTransitRoute().get(vehicle.getId());

				freespeed = RailsimUtils.getLinkFreespeedForTransitLineAndTransitRoute(line, route, link);

			} else {
				throw new RuntimeException("Unknown train speed approach. Aborting...");
			}

			if (freespeed <= 0) {
				// use the default if no information is provided in the link attributes...
				freespeed = defaultFreespeed;
			}
		}

		// now, account for acceleration and so on

		if (railSimConfigGroup.getTrainAccelerationApproach() == RailsimConfigGroup.TrainAccelerationApproach.without) {
			return freespeed;

		} else if (railSimConfigGroup.getTrainAccelerationApproach() == RailsimConfigGroup.TrainAccelerationApproach.euclideanDistanceBetweenStops) {

			final double beelineDistanceFactor = 1.3;

			Id<TransitStopFacility> lastStopId = statistics.getVehicle2lastStop().get(vehicle.getId());
			TransitStopFacility lastStop = scenario.getTransitSchedule().getFacilities().get(lastStopId);
			Coord lastStopCoord = scenario.getNetwork().getLinks().get(lastStop.getLinkId()).getCoord();

			final double minimumDistanceNonZero = 1.;
			double distanceFromLastStop = Math.max(minimumDistanceNonZero, NetworkUtils.getEuclideanDistance(lastStopCoord, link.getCoord()) * beelineDistanceFactor);
			double speedWithConsiderationOfAcceleration = getSpeedAfterAccelerating(distanceFromLastStop, vehicle, railSimConfigGroup);

			Id<TransitStopFacility> nextStopId = statistics.getVehicle2nextStop().get(vehicle.getId());
			double speedWithConsiderationOfDeceleration = Double.MAX_VALUE;
			if (nextStopId == null) {
				// train at final stop of transit line, the vehicle may be required for the next cycle in a different transit route...
			} else {
				TransitStopFacility nextStop = scenario.getTransitSchedule().getFacilities().get(nextStopId);
				Coord nextStopCoord = scenario.getNetwork().getLinks().get(nextStop.getLinkId()).getCoord();
				double distanceToNextStop = Math.max(minimumDistanceNonZero, NetworkUtils.getEuclideanDistance(link.getCoord(), nextStopCoord) * beelineDistanceFactor);
				speedWithConsiderationOfDeceleration = getSpeedBeforeDecelerating(distanceToNextStop, vehicle, railSimConfigGroup);
			}
			double maxVehicleVelocityWithAccelerationAndDeceleration = Math.min(speedWithConsiderationOfAcceleration, speedWithConsiderationOfDeceleration);

			if (freespeed == 0. || maxVehicleVelocityWithAccelerationAndDeceleration == 0.) {
				throw new RuntimeException("Velocity is 0. Aborting...");
			}
			// make sure the infrastructure limitations are taken into consideration
			double velocity = Math.min(freespeed, maxVehicleVelocityWithAccelerationAndDeceleration);
//			log.info("vehicle: " + vehicle.getId() + " / link: " + link.getId() + "/ velocity: " + velocity);
			return velocity;

		} else if (railSimConfigGroup.getTrainAccelerationApproach() == RailsimConfigGroup.TrainAccelerationApproach.speedOnPreviousLink) {

			// TODO: very experimental, needs tests etc.
			// The idea is to start with the speed on the previous link (or: when entering the current link).
			// The previous speed is then increased based on the acceleration and length of the current link.
			// Unclear: What do we do about the deceleration?
			// Unclear: The speed calculation is done for the front of the train path... and the front of the train is computed based on the
			double speedOnPreviousLink = statistics.getSpeedOnPreviousLink(vehicle.getId());
			double speedWithConsiderationOfAcceleration = getSpeedAfterAcceleratingFromPreviousSpeed(speedOnPreviousLink, link.getLength(), vehicle, railSimConfigGroup);

			if (freespeed == 0. || speedWithConsiderationOfAcceleration == 0.) {
				throw new RuntimeException("Velocity is 0. Aborting...");
			}

			// make sure the infrastructure limitations are taken into consideration
			double velocity = Math.min(freespeed, speedWithConsiderationOfAcceleration);
//			log.info("vehicle: " + vehicle.getId() + " / link: " + link.getId() + "/ velocity: " + velocity);
			return velocity;

		} else {
			throw new RuntimeException("Unknown train acceleration approach. Aborting...");
		}

	}

	/**
	 * @param distanceToNextStop
	 * @param vehicle
	 * @param railSimConfigGroup
	 * @return
	 */
	private double getSpeedBeforeDecelerating(double distanceToNextStop, Vehicle vehicle, RailsimConfigGroup railSimConfigGroup) {
		double deceleration = RailsimUtils.getTrainDeceleration(vehicle, railSimConfigGroup);
		double v = Math.sqrt(distanceToNextStop * 2 * deceleration);
		double maxVelocityVehicle = vehicle.getType().getMaximumVelocity();
		return Math.min(v, maxVelocityVehicle);
	}

	/**
	 * @param distanceFromLastStop
	 * @param vehicle
	 * @param railSimConfigGroup   s = a/2 * t^2
	 *                             a = v/t
	 *                             --> v = sqrt(s * 2 * a)
	 * @return
	 */
	private double getSpeedAfterAccelerating(double distanceFromLastStop, Vehicle vehicle, RailsimConfigGroup railSimConfigGroup) {
		double acceleration = RailsimUtils.getTrainAcceleration(vehicle, railSimConfigGroup);
		double v = Math.sqrt(distanceFromLastStop * 2 * acceleration);
		double maxVelocityVehicle = vehicle.getType().getMaximumVelocity();
		return Math.min(v, maxVelocityVehicle);
	}

	/**
	 * s = a/2 * t^2
	 * a = (v1 - v0) / (t1 - t0)
	 * --> v1 = v0 + sqrt(s * 2 * a)
	 *
	 * @return
	 */
	private double getSpeedAfterAcceleratingFromPreviousSpeed(double previousSpeed, double distance, Vehicle vehicle, RailsimConfigGroup railSimConfigGroup) {
		double acceleration = RailsimUtils.getTrainAcceleration(vehicle, railSimConfigGroup);
		double v1 = previousSpeed + Math.sqrt(distance * 2 * acceleration);
		double maxVelocityVehicle = vehicle.getType().getMaximumVelocity();
		return Math.min(v1, maxVelocityVehicle);
	}

	private boolean isTrain(QVehicle vehicle) {
		return this.transitVehicles.contains(vehicle.getVehicle().getId());
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitVehicles.add(event.getVehicleId());
	}

}
