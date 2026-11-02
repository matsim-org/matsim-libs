package org.matsim.contrib.msp.capability.measure;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The travel times the capability analysis needs: how long it takes to get from one place to another by
 * a given mode. Only the travel time matters, which is why this is not a full router - no routes are
 * built and no plans are modified.
 *
 * @see #freeSpeedNetwork(Network, double) for car, bike and any other mode the run routes on the network
 * @see #teleported(String, double, double) for walk
 */
public interface CapabilityTravelTimes {

	/**
	 * @return the travel time [s] from one place to the other, departing at the given time.
	 * {@link Double#POSITIVE_INFINITY} if the destination cannot be reached.
	 */
	double travelTime(Facility from, Facility to, double departureTime);

	/**
	 * Builds the travel times for every mode of the config. A mode the run routes on the network
	 * ({@code routing.networkModes}) is routed on the network here as well, every other mode is
	 * teleported with the parameters of the run.
	 *
	 * @throws IllegalArgumentException for a mode that is neither a network mode nor a teleported mode
	 * of the config
	 */
	static Map<String, CapabilityTravelTimes> of(Scenario scenario, CapabilityConfigGroup config) {

		Map<String, CapabilityTravelTimes> byMode = new LinkedHashMap<>();
		Collection<String> networkModes = scenario.getConfig().routing().getNetworkModes();

		for (String mode : config.getModes()) {

			// network modes first: a mode the run routes on the network is routed on the network here,
			// even if it happens to carry teleported parameters as well
			if (networkModes.contains(mode)) {
				byMode.put(mode, networkMode(scenario, mode));
				continue;
			}

			// everything else is teleported with the speed the run itself uses for that mode, so the
			// analysis stays consistent with the scenario
			TeleportedModeParams params = scenario.getConfig().routing().getTeleportedModeParams().get(mode);
			if (params == null || params.getTeleportedModeSpeed() == null) {
				throw new IllegalArgumentException("Mode '" + mode + "' is neither a network mode nor a teleported "
					+ "mode of the config, so the capability analysis does not know how to compute its travel times. "
					+ "Configure it in the 'routing' config group - as a 'networkModes' entry to route it on the "
					+ "network, or with a teleported speed - or remove it from '" + CapabilityConfigGroup.GROUP_NAME
					+ "'.");
			}
			double beelineFactor = params.getBeelineDistanceFactor() != null ? params.getBeelineDistanceFactor() : 1.0;
			byMode.put(mode, teleported(mode, params.getTeleportedModeSpeed(), beelineFactor));
		}

		return byMode;
	}

	/**
	 * Free speed travel times of one network mode: least cost paths over the links that allow the mode,
	 * at {@code min(link free speed, }{@link #maxSpeed(Scenario, String) mode speed}{@code )}.
	 */
	private static CapabilityTravelTimes networkMode(Scenario scenario, String mode) {

		Network modeNetwork = modeNetwork(scenario.getNetwork(), mode);
		if (modeNetwork.getLinks().isEmpty()) {
			throw new IllegalArgumentException("No link of the network allows '" + mode + "', so the capability "
				+ "analysis cannot route it. Check the allowed modes of the network, or remove the mode from '"
				+ CapabilityConfigGroup.GROUP_NAME + "'.");
		}

		double maxSpeed = maxSpeed(scenario, mode);
		log().info("Mode '{}' is routed on the {} of {} links that allow it, at {}.", mode,
			modeNetwork.getLinks().size(), scenario.getNetwork().getLinks().size(),
			Double.isInfinite(maxSpeed)
				? "link free speed - no vehicle type of the scenario limits it"
				: String.format(Locale.ROOT, "min(link free speed, %.2f m/s) from vehicle type '%s'", maxSpeed, mode));

		return freeSpeedNetwork(modeNetwork, maxSpeed);
	}

	/**
	 * How fast the mode itself can go, regardless of what the links allow. Read from the vehicle type
	 * named after the mode, which is MATSim's own convention for mode speeds (see
	 * {@code modeVehicleTypesFromVehiclesData}) and what {@code contribs/bicycle} reads as well - so the
	 * analysis uses the speed of the scenario rather than carrying one of its own.
	 *
	 * @return the {@link VehicleType#getMaximumVelocity()} [m/s] of the vehicle type whose id is the
	 * mode, or {@link Double#POSITIVE_INFINITY} if the scenario has no such type. Infinity means
	 * "limited by the link free speed alone", which is also what a vehicle type that does not set a
	 * maximum velocity means - it is MATSim's own default for the field.
	 */
	static double maxSpeed(Scenario scenario, String mode) {
		VehicleType type = scenario.getVehicles().getVehicleTypes().get(Id.create(mode, VehicleType.class));
		return type != null ? type.getMaximumVelocity() : Double.POSITIVE_INFINITY;
	}

	/**
	 * @return the part of the network whose links allow the given mode, so that paths never run over
	 * links the mode may not use
	 */
	static Network modeNetwork(Network network, String mode) {
		Network subNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(subNetwork, Set.of(mode));
		return subNetwork;
	}

	/**
	 * Travel times at free speed, i.e. the travel times of an empty network. Congested travel times would
	 * need the travel times of a run and are not implemented yet.
	 *
	 * @param modeNetwork the links the mode may use, see {@link #modeNetwork(Network, String)}
	 * @param maxSpeed how fast the mode itself can go [m/s]; {@link Double#POSITIVE_INFINITY} to let the
	 * link free speed alone decide, which is what car does
	 */
	static CapabilityTravelTimes freeSpeedNetwork(Network modeNetwork, double maxSpeed) {
		return new FreeSpeedNetwork(modeNetwork, maxSpeed);
	}

	/**
	 * Teleported travel times: beeline distance times a detour factor, divided by a speed. This is what
	 * MATSim itself does for walk unless the mode is routed on a network.
	 */
	static CapabilityTravelTimes teleported(String mode, double speed, double beelineDistanceFactor) {
		return (from, to, departureTime) -> {
			Coord origin = from.getCoord();
			Coord destination = to.getCoord();
			if (origin == null || destination == null) {
				return Double.POSITIVE_INFINITY;
			}
			return CoordUtils.calcEuclideanDistance(origin, destination) * beelineDistanceFactor / speed;
		};
	}

	private static Logger log() {
		// an interface cannot hold a private field, and a public one would be part of the API
		return LogManager.getLogger(CapabilityTravelTimes.class);
	}

	/**
	 * Least cost paths on the network of one mode at free speed, with the results cached per pair of
	 * links: the analysis asks for the same pairs over and over, once per person that lives or works on
	 * the same link.
	 */
	final class FreeSpeedNetwork implements CapabilityTravelTimes {

		private static final Logger log = LogManager.getLogger(FreeSpeedNetwork.class);

		private final Network network;
		private final double maxSpeed;
		private final LeastCostPathCalculator router;
		private final Map<Id<Link>, Map<Id<Link>, Double>> cache = new HashMap<>();

		/**
		 * The link a coordinate was snapped to, see {@link #link(Facility)}. Cached because the lookup
		 * sits in the innermost loop of the analysis: a person's home coordinate is asked for once per
		 * place, and every place once per person. The map therefore ends up holding one entry per
		 * distinct coordinate of the analysis - the places plus the activities of the population - which
		 * is the price of not looking the same coordinate up a million times.
		 */
		private final Map<Coord, Link> snapped = new HashMap<>();

		private FreeSpeedNetwork(Network network, double maxSpeed) {
			this.network = network;
			this.maxSpeed = maxSpeed;
			// least travel time, not least cost: the disutility is the travel time itself, so the path
			// the router finds is the quickest one on an empty network
			CappedFreeSpeed freeSpeed = new CappedFreeSpeed(maxSpeed);
			this.router = new SpeedyALTFactory().createPathCalculator(network, freeSpeed, freeSpeed);
		}

		@Override
		public double travelTime(Facility from, Facility to, double departureTime) {

			Link fromLink = link(from);
			Link toLink = link(to);

			if (fromLink == null || toLink == null) {
				return Double.POSITIVE_INFINITY;
			}
			if (fromLink.getId().equals(toLink.getId())) {
				return 0;
			}

			return this.cache
				.computeIfAbsent(fromLink.getId(), id -> new HashMap<>())
				.computeIfAbsent(toLink.getId(), id -> compute(fromLink, toLink, departureTime));
		}

		private double compute(Link fromLink, Link toLink, double departureTime) {
			// free speed travel times do not depend on the departure time, which is what makes the
			// cache correct
			Path path = this.router.calcLeastCostPath(fromLink, toLink, departureTime, null, null);
			if (path == null) {
				return Double.POSITIVE_INFINITY;
			}
			// the path runs between the nodes, so the destination link still has to be traversed - at the
			// capped speed as well, or it would be the one link of the path the mode speed does not apply to
			return path.travelTime + linkTravelTime(toLink);
		}

		private double linkTravelTime(Link link) {
			return link.getLength() / Math.min(link.getFreespeed(), this.maxSpeed);
		}

		/**
		 * Where a place sits in <i>this mode's</i> network: the nearest link of it to the place's own
		 * coordinate.
		 * <p>
		 * <b>The coordinate wins over a link the place may name</b>, which is the other way round from
		 * what one would expect. The reason is that a named link is a position in somebody else's
		 * network - the link a run assigned to an activity is a car link, and a link in the places csv
		 * was chosen without knowing which modes would be analysed - and taking it over would measure
		 * this mode's trip from where another mode starts. A cyclist whose nearest link is the cycleway
		 * behind the house starts there, not on the street the car is parked in. That matters most where
		 * it is most interesting: around car-free streets and zones, where the two can be hundreds of
		 * metres apart.
		 * <p>
		 * A named link is therefore used only when there is no coordinate to snap from, which the places
		 * csv allows ({@code link_id} alone is a location).
		 *
		 * @return the link, or null if the place cannot be placed in this mode's network at all
		 */
		private Link link(Facility facility) {

			Coord coord = facility.getCoord();
			if (coord != null) {
				return this.snapped.computeIfAbsent(coord, at -> NetworkUtils.getNearestLink(this.network, at));
			}

			// no coordinate, so the link the place names is all there is to go on. It may well not be one
			// this mode can use, and then the place is simply out of reach for it.
			Id<Link> linkId = facility.getLinkId();
			if (linkId == null) {
				return null;
			}
			Link link = this.network.getLinks().get(linkId);
			if (link == null) {
				log.warn("A place sits on link {} and carries no coordinate, so it cannot be placed in the network "
					+ "of a mode that may not use that link. It counts as unreachable by that mode.", linkId);
			}
			return link;
		}

		/**
		 * Free speed, capped by what the mode itself can do - the same {@code min(mode speed, link free
		 * speed)} that {@code contribs/bicycle} applies. Travel time and disutility are one and the same
		 * here, so that the router returns the quickest path rather than the cheapest one.
		 */
		private record CappedFreeSpeed(double maxSpeed) implements TravelTime, TravelDisutility {

			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return travelTime(link);
			}

			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return travelTime(link);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return travelTime(link);
			}

			private double travelTime(Link link) {
				return link.getLength() / Math.min(link.getFreespeed(), this.maxSpeed);
			}
		}
	}
}
