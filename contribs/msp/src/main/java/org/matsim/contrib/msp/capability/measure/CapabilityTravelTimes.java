package org.matsim.contrib.msp.capability.measure;

import ch.sbb.matsim.routing.pt.raptor.OccupancyData;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The travel times the capability analysis needs: how long it takes to get from one place to another by
 * a given mode. Only the travel time matters, which is why this is not a full router - no routes are
 * built and no plans are modified.
 *
 * @see #freeSpeedNetwork(Network, double) for car, bike and any other mode the run routes on the network
 * @see #publicTransport(Scenario, CapabilityTravelTimes) for pt
 * @see #teleported(String, double, double) for walk
 */
public interface CapabilityTravelTimes {

	/**
	 * @return how the trip from one place to the other turns out, departing at the given time.
	 * {@link Trip#UNREACHABLE} if the destination cannot be reached at all.
	 */
	Trip travel(Facility from, Facility to, double departureTime);

	/**
	 * Anything this implementation has to say about the queries it answered, logged once at the end of
	 * the analysis. Nothing by default; {@link PublicTransport} reports how often it had to walk.
	 *
	 * @param mode the mode this instance answers for, which it does not otherwise know
	 */
	default void logStatistics(String mode) {
	}

	/**
	 * The modes MATSim spells as going on foot. A trip by one of them is walked end to end, which is
	 * what makes its whole distance a walking distance.
	 * <p>
	 * This is the one place that decides what counts as walking, so it is also the place a future
	 * per-person cap on the walking distance would read.
	 */
	Set<String> WALK_MODES = Set.of(TransportMode.walk, TransportMode.non_network_walk,
		TransportMode.transit_walk);

	/**
	 * @return whether a trip by this mode puts the traveller on foot for its whole length
	 */
	static boolean isOnFoot(String mode) {
		return WALK_MODES.contains(mode);
	}

	/**
	 * One trip by one mode: how long it takes, and how far the traveller walks while making it.
	 * <p>
	 * <b>Why the walking distance is part of the answer.</b> It is not a cost of the trip - the measure
	 * prices a detour in time, see {@link InsertionCost} - but a statement about who could actually make
	 * it. The walk to the stop is the part of a pt trip that a person with reduced mobility may not be
	 * able to do, and the part of a walk trip that is the whole trip. Recording it per trip is what lets
	 * the output be read per person later, and what a cap on the walking distance - ideally read from a
	 * person attribute - would be applied to.
	 *
	 * @param travelTime the time from leaving the origin to arriving at the destination [s], waiting
	 * included; {@link Double#POSITIVE_INFINITY} if the destination cannot be reached
	 * @param walkDistance how far of it is walked [m]. Zero for a mode that does not put the traveller
	 * on foot at all, which is a <b>simplification</b> for car and bike: the walk to where the vehicle
	 * is parked is not modelled.
	 */
	record Trip(double travelTime, double walkDistance) {

		/**
		 * No trip at all. The walking distance of a trip that cannot be made is unknown rather than
		 * zero, so it is {@link Double#NaN} and the output writes an empty cell for it.
		 */
		public static final Trip UNREACHABLE = new Trip(Double.POSITIVE_INFINITY, Double.NaN);

		/**
		 * A trip of a mode that does not put the traveller on foot, i.e. with a walking distance of zero.
		 */
		public static Trip withoutWalking(double travelTime) {
			return new Trip(travelTime, 0);
		}

		public boolean possible() {
			return !Double.isInfinite(this.travelTime);
		}
	}

	/**
	 * Builds the travel times for every mode of the config, following the config of the run so that the
	 * analysis never contradicts the scenario it is analysing:
	 * <ol>
	 *     <li>a <b>transit mode</b> ({@code transit.transitModes}, usually {@code pt}) of a scenario that
	 *     carries a transit schedule is routed by {@link SwissRailRaptor};</li>
	 *     <li><b>walk</b> is teleported, beeline distance times the detour factor, whatever the run does
	 *     with it - see {@link #walk(Scenario, String)};</li>
	 *     <li>a mode the run routes on the <b>network</b> ({@code routing.networkModes}) is routed on the
	 *     network here as well;</li>
	 *     <li>every other mode is <b>teleported</b> with the parameters of the run.</li>
	 * </ol>
	 * The order matters for pt, which carries teleported routing parameters in MATSim's own config
	 * defaults - a scenario with a schedule is meant to be analysed on the schedule.
	 * <p>
	 * Every mode is wrapped in a {@link Metered}, so that {@link #logQueryStatistics(Map)} can say
	 * afterwards how much of the analysis went into routing which mode.
	 *
	 * @throws IllegalArgumentException for a mode that is neither a transit mode with a schedule, nor a
	 * network mode, nor a teleported mode of the config
	 */
	static Map<String, CapabilityTravelTimes> of(Scenario scenario, CapabilityConfigGroup config) {

		Map<String, CapabilityTravelTimes> byMode = new LinkedHashMap<>();
		Collection<String> transitModes = scenario.getConfig().transit().getTransitModes();

		// no mode is set up twice, which matters for the walk: the walk mode of the analysis and the
		// walk a pt trip falls back to are the same walk, so they are also the same object and are
		// reported once
		Map<String, CapabilityTravelTimes> shared = new HashMap<>();
		Function<String, CapabilityTravelTimes> build =
			mode -> shared.computeIfAbsent(mode, m -> forMode(scenario, m));

		// built once and shared, since one raptor can answer for every transit mode and preprocessing the
		// schedule is by far the most expensive part of setting it up
		CapabilityTravelTimes pt = null;

		for (String mode : config.getModes()) {

			// transit modes first: a scenario that carries a schedule is meant to be analysed on the
			// schedule, even though MATSim's own config defaults give pt teleported parameters as well
			if (transitModes.contains(mode)) {
				requireTransit(scenario, mode);
				if (pt == null) {
					// the fallback is built whether or not walk is one of the analysed modes, because it
					// is part of what pt means here rather than a second mode
					pt = publicTransport(scenario, build.apply(TransportMode.walk));
				}
				byMode.put(mode, new Metered(pt));
				continue;
			}

			byMode.put(mode, new Metered(build.apply(mode)));
		}

		return byMode;
	}

	/**
	 * The travel times of one mode that is not routed on a transit schedule: on the network if the run
	 * routes it there, teleported otherwise.
	 *
	 * @throws IllegalArgumentException for a mode that is neither
	 */
	private static CapabilityTravelTimes forMode(Scenario scenario, String mode) {

		// walking is always teleported and never routed on the network, even where the run routes it
		// there. Beeline distance times the detour factor is what MATSim itself does for walk, and it is
		// also exactly the walk the raptor builds for its own access and egress legs - so the walk mode,
		// the walk inside a pt trip and the walk a pt trip falls back to are all the same walk.
		if (isOnFoot(mode)) {
			return walk(scenario, mode);
		}

		// network modes next: a mode the run routes on the network is routed on the network here, even
		// if it happens to carry teleported parameters as well
		if (scenario.getConfig().routing().getNetworkModes().contains(mode)) {
			return networkMode(scenario, mode);
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
		return teleported(mode, params.getTeleportedModeSpeed(), beelineFactor);
	}

	/**
	 * Teleported walk: <code>beeline distance x beelineDistanceFactor</code>, at the teleported walk
	 * speed of the run. Never the network, see {@link #forMode(Scenario, String)}.
	 * <p>
	 * The parameter set is looked up for the given mode and, failing that, for the other modes MATSim
	 * spells as walking - the same chain {@code RaptorUtils.createStaticConfig} walks to find the beeline
	 * walk speed of the router. That is deliberate: it is what makes the walk of the analysis and the
	 * access, egress and transfer walks inside a pt trip come out of one and the same parameter set.
	 */
	private static CapabilityTravelTimes walk(Scenario scenario, String mode) {

		Map<String, TeleportedModeParams> byMode = scenario.getConfig().routing().getTeleportedModeParams();

		for (String candidate : walkLookupOrder(mode)) {
			TeleportedModeParams params = byMode.get(candidate);
			if (params == null || params.getTeleportedModeSpeed() == null) {
				continue;
			}
			double speed = params.getTeleportedModeSpeed();
			double beelineFactor = params.getBeelineDistanceFactor() != null ? params.getBeelineDistanceFactor() : 1.0;
			log().info("Mode '{}' is teleported at {} m/s with a detour factor of {}, from the '{}' teleported "
				+ "mode parameters. Walking is never routed on the network.", mode,
				String.format(Locale.ROOT, "%.4f", speed), beelineFactor, candidate);
			return teleported(mode, speed, beelineFactor);
		}

		throw new IllegalArgumentException("The capability analysis needs a teleported walk speed, and the "
			+ "config has none for any of " + WALK_MODES + ". Add a 'teleportedModeParameters' set for '"
			+ TransportMode.walk + "' to the 'routing' config group. It is needed even when walk is not one "
			+ "of the analysed modes, because a public transport trip that cannot be routed falls back to "
			+ "walking.");
	}

	/**
	 * @return the mode itself first, then the other walk modes, so that an explicitly configured mode
	 * always wins over a helper mode standing in for it
	 */
	private static List<String> walkLookupOrder(String mode) {
		List<String> order = new ArrayList<>();
		order.add(mode);
		for (String walkMode : List.of(TransportMode.walk, TransportMode.non_network_walk,
			TransportMode.transit_walk)) {
			if (!order.contains(walkMode)) {
				order.add(walkMode);
			}
		}
		return order;
	}

	/**
	 * Fails with an explanation rather than with an empty result if a transit mode is analysed but the
	 * scenario carries no schedule to route it on. An empty schedule would make every person unable to
	 * reach anything by pt, which looks like a result.
	 */
	private static void requireTransit(Scenario scenario, String mode) {
		if (scenario.getTransitSchedule() == null
			|| scenario.getTransitSchedule().getTransitLines().isEmpty()) {
			throw new IllegalArgumentException("Mode '" + mode + "' is a transit mode of the config, but the "
				+ "scenario carries no transit schedule, so the capability analysis cannot route it. Point the "
				+ "'transit' config group at a schedule and switch 'useTransit' on - RunCapabilityAnalysis does "
				+ "that for you when it finds a transit schedule next to the network - or remove the mode from '"
				+ CapabilityConfigGroup.GROUP_NAME + "'.");
		}
	}

	/**
	 * Logs how many travel time queries each mode took and how long they took, so that a long analysis
	 * can be attributed to the mode that caused it. A mode not built by {@link #of(Scenario,
	 * CapabilityConfigGroup)} is not metered and simply says nothing.
	 */
	static void logQueryStatistics(Map<String, CapabilityTravelTimes> byMode) {
		for (Map.Entry<String, CapabilityTravelTimes> entry : byMode.entrySet()) {
			entry.getValue().logStatistics(entry.getKey());
		}
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
	 * Door to door public transport travel times from {@link SwissRailRaptor}, the router the scenario
	 * itself uses: access walk, waiting, the rides, the transfers and the egress walk, all of it.
	 * <p>
	 * No matrix and no precomputation - every query is a full raptor search, which is why this is by far
	 * the most expensive mode of the analysis. {@link #logQueryStatistics(Map)} says how expensive.
	 *
	 * @param scenario needs a transit schedule, the transit vehicles and the config of the run, which is
	 * where the raptor takes its parameters from
	 * @param onFoot what a trip falls back to where the raptor finds no connection, see
	 * {@link PublicTransport}
	 */
	static CapabilityTravelTimes publicTransport(Scenario scenario, CapabilityTravelTimes onFoot) {
		return new PublicTransport(scenario, onFoot);
	}

	/**
	 * Teleported travel times: beeline distance times a detour factor, divided by a speed. This is what
	 * MATSim itself does for walk unless the mode is routed on a network.
	 */
	static CapabilityTravelTimes teleported(String mode, double speed, double beelineDistanceFactor) {
		boolean onFoot = isOnFoot(mode);
		return (from, to, departureTime) -> {
			Coord origin = from.getCoord();
			Coord destination = to.getCoord();
			if (origin == null || destination == null) {
				return Trip.UNREACHABLE;
			}
			// the detour factor is part of the distance, not only of the time: it is MATSim's own estimate
			// of how far the traveller really goes, which is what a walking distance has to be
			double distance = CoordUtils.calcEuclideanDistance(origin, destination) * beelineDistanceFactor;
			return new Trip(distance / speed, onFoot ? distance : 0);
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
	 * <p>
	 * Nothing routed here is walked - walking is always teleported, see {@link #walk(Scenario, String)} -
	 * so the walking distance of these trips is zero, which leaves out the walk to where the car or the
	 * bike is parked.
	 */
	final class FreeSpeedNetwork implements CapabilityTravelTimes {

		private static final Logger log = LogManager.getLogger(FreeSpeedNetwork.class);

		private final Network network;
		private final double maxSpeed;
		private final LeastCostPathCalculator router;
		private final Map<Id<Link>, Map<Id<Link>, Trip>> cache = new HashMap<>();

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
		public Trip travel(Facility from, Facility to, double departureTime) {

			Link fromLink = link(from);
			Link toLink = link(to);

			if (fromLink == null || toLink == null) {
				return Trip.UNREACHABLE;
			}
			if (fromLink.getId().equals(toLink.getId())) {
				return new Trip(0, 0);
			}

			return this.cache
				.computeIfAbsent(fromLink.getId(), id -> new HashMap<>())
				.computeIfAbsent(toLink.getId(), id -> compute(fromLink, toLink, departureTime));
		}

		private Trip compute(Link fromLink, Link toLink, double departureTime) {
			// free speed travel times do not depend on the departure time, which is what makes the
			// cache correct
			Path path = this.router.calcLeastCostPath(fromLink, toLink, departureTime, null, null);
			if (path == null) {
				return Trip.UNREACHABLE;
			}
			// the path runs between the nodes, so the destination link still has to be traversed - at the
			// capped speed as well, or it would be the one link of the path the mode speed does not apply to
			return Trip.withoutWalking(path.travelTime + linkTravelTime(toLink));
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

	/**
	 * Public transport travel times from {@link SwissRailRaptor}: the time from leaving the origin to
	 * arriving at the destination, <b>waiting included</b>. That is what makes pt different from every
	 * other mode here - when the agent leaves matters, so the travel time genuinely depends on the
	 * departure time and nothing can be cached by pair of places.
	 * <p>
	 * Two consequences worth knowing when reading a pt row of the output:
	 * <ul>
	 *     <li><b>A trip the raptor cannot route is walked instead.</b> The raptor discards any route
	 *     without a pt leg in it, because {@code intermodalLegOnlyHandling} defaults to {@code forbid},
	 *     so it answers "no connection" both where walking simply beats pt and where nothing runs at
	 *     all. Neither is a reason to call the place unreachable - the agent still has legs - so the
	 *     trip falls back to {@code onFoot}, and a pt row means "by pt, or on foot where pt is no use".
	 *     The fallback walk is the same beeline walk the raptor would have built itself, so the number
	 *     does not jump at the boundary. Where nothing runs <i>and</i> the place is far, the fallback
	 *     returns a walk of hours, and the insertion then fails on the deadline or on
	 *     {@code maxExtraTravelTime} - which is the honest outcome, and visible in the walking distance
	 *     of the row.</li>
	 *     <li><b>Both trips are routed from the earliest time the agent could set off, not from when it
	 *     would.</b> {@link InsertionCalculator} asks for {@code origin -> facility} at the start of the
	 *     free period and for {@code facility -> destination} at the moment the agent arrives. Those are
	 *     the only times it knows before the activity is placed, and they give the earliest possible
	 *     arrival, which is what {@code slack} needs - but the waiting they contain is then also charged
	 *     to {@code t_extra}. An agent whose free period starts at 00:00 is therefore routed as if it set
	 *     off at midnight, and where the timetable has a gap there the detour comes out far too long.
	 *     Optimising the departure time within the period is the obvious next step for pt.</li>
	 * </ul>
	 * <b>Not thread safe</b>, because {@code SwissRailRaptorCore} keeps the state of the search it is
	 * running. The analysis is single threaded, and a parallel one would need one raptor per thread over
	 * the same {@link SwissRailRaptorData}.
	 */
	final class PublicTransport implements CapabilityTravelTimes {

		private static final Logger log = LogManager.getLogger(PublicTransport.class);

		private final SwissRailRaptor raptor;
		private final CapabilityTravelTimes onFoot;

		private long queries;
		private long walked;
		private boolean distanceWarningShown;

		private PublicTransport(Scenario scenario, CapabilityTravelTimes onFoot) {

			if (onFoot == null) {
				throw new IllegalArgumentException("Public transport needs travel times to fall back on where "
					+ "the raptor finds no connection, and none were given.");
			}
			this.onFoot = onFoot;

			Config config = scenario.getConfig();
			RaptorStaticConfig staticConfig = RaptorUtils.createStaticConfig(config);

			long start = System.nanoTime();
			// the same preprocessing a run does, see SwissRailRaptorFactory. OccupancyData stays empty:
			// it only carries the loads of a running mobsim, which an analysis of a finished run has not
			// got, and it is read at all only with 'useCapacityConstraints'.
			SwissRailRaptorData data = SwissRailRaptorData.create(scenario.getTransitSchedule(),
				scenario.getTransitVehicles(), staticConfig, scenario.getNetwork(), new OccupancyData());
			this.raptor = new SwissRailRaptor.Builder(data, config).build();

			log.info("Public transport is routed by SwissRailRaptor on a schedule of {} lines and {} stops; "
					+ "preprocessing it took {} s.",
				scenario.getTransitSchedule().getTransitLines().size(),
				scenario.getTransitSchedule().getFacilities().size(),
				String.format(Locale.ROOT, "%.1f", (System.nanoTime() - start) / 1e9));
		}

		@Override
		public Trip travel(Facility from, Facility to, double departureTime) {

			this.queries++;

			// the raptor needs coordinates to find the stops around a place, so a place that only names a
			// link cannot be routed on the schedule at all
			if (from.getCoord() != null && to.getCoord() != null) {
				List<? extends PlanElement> legs = this.raptor.calcRoute(
					DefaultRoutingRequest.withoutAttributes(from, to, departureTime, null));
				if (legs != null && !legs.isEmpty()) {
					return tripOf(legs, departureTime);
				}
			}

			// no connection: the agent walks. See the class comment for why that is not the same as
			// calling the place unreachable.
			this.walked++;
			return this.onFoot.travel(from, to, departureTime);
		}

		/**
		 * Reads one raptor answer: when the agent arrives, and how far it walked getting there.
		 */
		private Trip tripOf(List<? extends PlanElement> legs, double departureTime) {

			// the legs carry absolute times, so the arrival is the end of the last of them. Taking the
			// arrival rather than summing the legs is what keeps the waiting time in: the first ride
			// departs when it departs, not when the access walk ends.
			double arrival = departureTime;
			double walkDistance = 0;

			for (PlanElement element : legs) {
				if (!(element instanceof Leg leg)) {
					continue;
				}
				double legDeparture = leg.getDepartureTime().orElse(arrival);
				arrival = Math.max(arrival, legDeparture + leg.getTravelTime().orElse(0));
				walkDistance += walkedOn(leg);
			}

			return new Trip(arrival - departureTime, walkDistance);
		}

		/**
		 * How far the agent walks on one leg of a pt trip: the access walk to the first stop, the walks
		 * between the stops of a transfer, and the egress walk from the last stop. A leg carrying a
		 * {@link TransitPassengerRoute} is a ride and is walked not at all.
		 * <p>
		 * Everything that is not a ride counts as walked, which is right for the access and egress the
		 * default stop finder builds. A scenario with <b>intermodal</b> access and egress configured -
		 * bike or car to the station - would have those legs counted as walking too; that is a gap to
		 * close when such a scenario is analysed.
		 */
		private double walkedOn(Leg leg) {

			if (leg.getRoute() == null || leg.getRoute() instanceof TransitPassengerRoute) {
				return 0;
			}

			double distance = leg.getRoute().getDistance();
			if (!Double.isFinite(distance)) {
				if (!this.distanceWarningShown) {
					log.warn("A '{}' leg of a pt route carries no distance, so it does not count towards the "
						+ "walking distance of the trip, which is therefore understated. Further occurrences "
						+ "are not reported.", leg.getMode());
					this.distanceWarningShown = true;
				}
				return 0;
			}
			return distance;
		}

		@Override
		public void logStatistics(String mode) {
			if (this.queries == 0) {
				return;
			}
			log.info("Mode '{}': {} of the {} trips ({}) had no pt connection and were walked instead.", mode,
				this.walked, this.queries,
				String.format(Locale.ROOT, "%.1f%%", 100.0 * this.walked / this.queries));
		}
	}

	/**
	 * Counts the queries put to one mode and the time they took. The analysis asks for travel times in
	 * its innermost loop - persons x modes x types x facilities x periods, three queries each - so this
	 * is what says afterwards whether an hour of runtime went into the pt router or somewhere else.
	 * <p>
	 * The counters are plain fields rather than atomics: the analysis is single threaded, and a number
	 * that is only ever logged is not worth synchronising for.
	 */
	final class Metered implements CapabilityTravelTimes {

		private static final Logger log = LogManager.getLogger(Metered.class);

		private final CapabilityTravelTimes delegate;

		private long queries;
		private long unreachable;
		private long nanos;

		Metered(CapabilityTravelTimes delegate) {
			this.delegate = delegate;
		}

		@Override
		public Trip travel(Facility from, Facility to, double departureTime) {
			long start = System.nanoTime();
			Trip trip = this.delegate.travel(from, to, departureTime);
			this.nanos += System.nanoTime() - start;
			this.queries++;
			if (!trip.possible()) {
				this.unreachable++;
			}
			return trip;
		}

		public long queries() {
			return this.queries;
		}

		/**
		 * @return the time the queries of this mode took [s]
		 */
		public double seconds() {
			return this.nanos / 1e9;
		}

		@Override
		public void logStatistics(String mode) {
			if (this.queries == 0) {
				log.info("Mode '{}': no travel time query at all.", mode);
				return;
			}
			log.info("Mode '{}': {} travel time queries in {} s, {} ms each. {} of them ({}) could not be "
					+ "routed at all.", mode, this.queries,
				String.format(Locale.ROOT, "%.1f", seconds()),
				String.format(Locale.ROOT, "%.4f", this.nanos / 1e6 / this.queries),
				this.unreachable,
				String.format(Locale.ROOT, "%.1f%%", 100.0 * this.unreachable / this.queries));
			// whatever the mode itself has to add, e.g. how often pt had to fall back to walking
			this.delegate.logStatistics(mode);
		}
	}
}
