package org.matsim.application.prepare.scenario;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cuts out a part of the Population and Network which is relevant inside the specified shape of the shapefile.<br><br>
 * How the network is cut out:
 * <ul>
 * 		<li>Keep all links in shape file</li>
 * 		<li>Keep all links that are used by any agent in its route</li>
 * 		<li>if an agent has not route, a shortest-path route is calculated and used instead</li>
 * 		<li>This is done for all network modes</li>
 * </ul>
 * <p>
 * How the population is cut out:
 * <ul>
 * 		<li>All agents having any activity in the shape file are kept</li>
 * 		<li>Agents traveling through the shape file are kept, this is determined by the route</li>
 * 		<li>The buffer is not considered for the population cut out</li>
 * </ul>
 * <p>
 * How the network change events are generated:
 * <ul>
 *     <li>Travel time is computed using the given events</li>
 *     <li>Travel times for all links outside the shape file + buffer will be fixed</li>
 *     <li>The capacity of these links will be set to infinity</li>
 * </ul>
 *
 * <b>Limitations:</b><br>
 * Cutting out agents and fixing the speed is a challenging problem, because we lose sensitivity to policy cases
 * We can not use the speed limit and use the original capacity because this underestimates speed (because congestion can only decrease the speed)
 * Therefore the capacity is set to infinity, but these road don't react to changes<br>
 * <p>
 * The cut-out will create a buffer area around the shape in which the speed is simulated with the original network, so that it is sensitive to changes
 * Therefore, the buffer should not be too small
 * One should be careful creating too small cut-outs because of the mentioned limitations.
 *
 * @return 0 if successful, 2 if CRS is null
 */
@CommandLine.Command(name = "scenario-cutout", description = "Cut out a scenario based on a shape file. Reduce population and network to the relevant area.")
public class CreateScenarioCutOut implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(CreateScenarioCutOut.class);

	/**
	 * Router cache for every mode. This is a thread-local cache, so that we can use the same router for every thread.
	 */
	private final Map<String, ThreadLocal<LeastCostPathCalculator>> routerCache = new ConcurrentHashMap<>();

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--network", description = "Path to network", required = true)
	private Path networkPath;

	@CommandLine.Option(names = "--facilities", description = "Path to facilities file. Only needed if there are agents with no coordinates or links", required = false)
	private Path facilityPath;

	@CommandLine.Option(names = "--events", description = "Input events used for travel time calculation. NOTE: Making a cutout without the events file will make the scenario inaccurate!", required = false)
	private Path eventPath;

	@CommandLine.Option(names = "--buffer", description = "Buffer around zones in meter", defaultValue = "5000")
	private double buffer;

	@CommandLine.Option(names = "--output-network", description = "Path to output network", required = true)
	private Path outputNetwork;

	@CommandLine.Option(names = "--output-population", description = "Path to output population", required = true)
	private Path outputPopulation;

	@CommandLine.Option(names = "--output-network-change-events", description = "Path to network change event output", required = false)
	private Path outputEvents;

	@CommandLine.Option(names = "--network-change-events-interval", description = "Interval of NetworkChangesToBeApplied. Unit is seconds. Will be ignored if --output-network-change-events is undefined", defaultValue = "900", required = false)
	private double changeEventsInterval;

	@CommandLine.Option(names = "--network-modes", description = "Modes to consider when cutting network", defaultValue = "car,bike", split = ",")
	private Set<String> modes;

	@CommandLine.Option(names = "--keep-modes", description = "Network modes of links that are always kept", defaultValue = TransportMode.pt, split = ",")
	private Set<String> keepModes;

	@CommandLine.Mixin
	private CrsOptions crs;

	@CommandLine.Mixin
	private ShpOptions shp;

	// External classes
	private final GeometryFactory geoFactory = new GeometryFactory();
	private TravelTimeCalculator tt;

	// Variables used for processing
	/**
	 * Map with mode-string as key and the mode-filtered Network as value
	 */
	private final Map<String, Network> mode2modeOnlyNetwork = new HashMap<>();

	/**
	 * Links that are inside the shapefile.
	 */
	private final Set<Id<Link>> linksToKeep = ConcurrentHashMap.newKeySet();

	/**
	 * Links that are outside the shapefile
	 */
	private final Set<Id<Link>> linksToDelete = ConcurrentHashMap.newKeySet();

	/**
	 * Additional links to include (may be outside the shapefile). Links are marked like this, if they are used in a plan of an agent, that
	 * is relevant.
	 */
	private final Set<Id<Link>> linksToInclude = ConcurrentHashMap.newKeySet();

	/**
	 * Agents, that are not relevant: Not in the shapefile or buffer, not route through the shapefile or buffer
	 */
	private final Set<Id<Person>> personsToDelete = ConcurrentHashMap.newKeySet();

	// Data inputs
	/**
	 * Full network
	 */
	private Network network;

	/**
	 * Shapefile as a {@link Geometry}
	 */
	private Geometry geom;

	/**
	 * Shapefile+buffer as a {@link Geometry}
	 */
	private Geometry geomBuffer;

	/**
	 * Facilities (is only set if used, otherwise not initialized)
	 */
	private ActivityFacilities facilities;

	private int emptyNetworkWarnings = 0;
	private int noActCoordsWarnings = 0;

	public static void main(String[] args) {
		new CreateScenarioCutOut().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		//Check CRS
		if (crs.getInputCRS() == null) {
			log.error("Input CRS must be specified");
			return 2;
		}

		//We need to make sure, that the network is a TimeVariantNetwork
		NetworkConfigGroup ncg = new NetworkConfigGroup();
		ncg.setTimeVariantNetwork(true);

		// Prepare required input-data
		Population population = PopulationUtils.readPopulation(input.toString());
		network = NetworkUtils.readNetwork(networkPath.toString(), ncg);

		geom = shp.getGeometry();
		geomBuffer = geom.buffer(buffer);

		for (String mode : modes)
			mode2modeOnlyNetwork.putIfAbsent(mode, filterNetwork(network, mode));

		// Prepare optional input-data
		if (facilityPath != null) {
			facilities = FacilitiesUtils.createActivityFacilities();
			new MatsimFacilitiesReader(crs.getInputCRS(), crs.getTargetCRS(), facilities).readFile(facilityPath.toString());
		}

		if (eventPath != null) {
			TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
			builder.setTimeslice(changeEventsInterval);

			EventsManager manager = EventsUtils.createEventsManager();

			tt = builder.build();
			manager.addHandler(tt);

			manager.initProcessing();
			EventsUtils.readEvents(manager, eventPath.toString());
			manager.finishProcessing();
		}

		boolean useNetworkChangeEvents = outputEvents != null;

		// Cut out the network: Filter for links inside the shapefile
		for (Link link : network.getLinks().values()) {
			if (geom.contains(MGC.coord2Point(link.getCoord()))
				|| geom.contains(MGC.coord2Point(link.getFromNode().getCoord()))
				|| geom.contains(MGC.coord2Point(link.getToNode().getCoord()))
				|| link.getAllowedModes().stream().anyMatch(keepModes::contains)) {
				// keep the link
				linksToKeep.add(link.getId());
			} else {
				linksToDelete.add(link.getId());
			}
		}

		// TODO: consider facilities, (see FilterRelevantAgents in OpenBerlin) -> (done, partially tested)
		// TODO: Use events for travel time calculation, (this is optional) -> (done, untested) -> It works and also generates an output, but speeds seem a bit off to me

		// Cut out the population and mark needed network parts
		ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors(), this);

		//Population
		log.info("Persons to delete: {}", personsToDelete.size());
		for (Id<Person> personId : personsToDelete) {
			population.removePerson(personId);
		}

		log.info("Persons in the scenario: {}", population.getPersons().size());

		PopulationUtils.writePopulation(population, outputPopulation.toString());

		//Network
		log.info("Links to add: {}", linksToKeep.size());

		log.info("Additional links from routes to include: {}", linksToInclude.size());

		log.info("number of links in original network: {}", network.getLinks().size());

		for (Id<Link> linkId : linksToDelete) {
			if (!linksToInclude.contains(linkId))
				network.removeLink(linkId);
		}

		// clean the network
		log.info("number of links before cleaning: {}", network.getLinks().size());
		log.info("number of nodes before cleaning: {}", network.getNodes().size());

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		cleaner.removeNodesWithoutLinks();

		for (String mode : modes) {
			log.info("Cleaning mode {}", mode);
			cleaner.run(Set.of(mode));
		}

		log.info("number of links after cleaning: {}", network.getLinks().size());
		log.info("number of nodes after cleaning: {}", network.getNodes().size());

		List<NetworkChangeEvent> events = generateNetworkChangeEvents(changeEventsInterval);

		NetworkUtils.writeNetwork(network, outputNetwork.toString());

		if (useNetworkChangeEvents) {
			new NetworkChangeEventsWriter().write(outputEvents.toString(), events);
		}

		return 0;
	}

	// Helper-Functions

	private Network filterNetwork(Network network, String mode) {
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);

		Network carOnlyNetwork = NetworkUtils.createNetwork();
		filter.filter(carOnlyNetwork, Set.of(mode));
		return carOnlyNetwork;
	}

	private LeastCostPathCalculator createRouter(Network network, String mode) {
		routerCache.putIfAbsent(mode, new ThreadLocal<>());
		LeastCostPathCalculator c = routerCache.get(mode).get();
		if (c == null) {
			FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
			LeastCostPathCalculatorFactory fastAStarLandmarksFactory = new SpeedyALTFactory();

			OnlyTimeDependentTravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);

			c = fastAStarLandmarksFactory.createPathCalculator(network, travelDisutility, travelTime);
			routerCache.putIfAbsent(mode, new ThreadLocal<>());
			routerCache.get(mode).set(c);
		}

		return c;
	}

	private Node getFromNode(Network network, Trip trip) {
		if (network.getLinks().isEmpty()) {
			if (emptyNetworkWarnings++ < 10)
				log.warn("Tried to get a from-node on an empty network. Maybe you defined a wrong mode? Skipping ...");

			return null;
		}

		Map<Id<Link>, ? extends Link> modeLinks = network.getLinks();
		if (trip.getOriginActivity().getLinkId() != null && modeLinks.get(trip.getOriginActivity().getLinkId()) != null) {
			return modeLinks.get(trip.getOriginActivity().getLinkId()).getFromNode();
		} else {
			return NetworkUtils.getNearestLink(network, getActivityCoord(trip.getOriginActivity())).getFromNode();
		}
	}

	private Node getToNode(Network network, Trip trip) {
		if (network.getLinks().isEmpty()) {
			if (emptyNetworkWarnings++ < 10)
				log.warn("Tried to get a to-node on an empty network. Maybe you defined a wrong mode? Skipping ...");

			return null;
		}
		Map<Id<Link>, ? extends Link> modeLinks = network.getLinks();
		if (trip.getDestinationActivity().getLinkId() != null && modeLinks.get(trip.getDestinationActivity().getLinkId()) != null) {
			return modeLinks.get(trip.getDestinationActivity().getLinkId()).getToNode();
		} else {
			return NetworkUtils.getNearestLink(network, getActivityCoord(trip.getDestinationActivity())).getToNode();
		}
	}

	private LeastCostPathCalculator.Path getModeNetworkPath(Network network, String mode, Trip trip) {
		LeastCostPathCalculator router = createRouter(network, mode);

		Node fromNode = getFromNode(network, trip);
		Node toNode = getToNode(network, trip);

		if (fromNode == null || toNode == null)
			return null;

		return router.calcLeastCostPath(fromNode, toNode, 0, null, null);
	}

	private Coord getActivityCoord(Activity activity) {
		if (facilities != null && activity.getFacilityId() != null && facilities.getFacilities().containsKey(activity.getFacilityId()))
			return facilities.getFacilities().get(activity.getFacilityId()).getCoord();

		return activity.getCoord();
	}

	/**
	 * Creates and applies the {@link NetworkChangeEvent}s to the network. It sets the capacity for all links outside the shapefile and bufferzone
	 * to infinite and reduces the freespeed of the links so that the agents behave somehow realistically outside the cutout-area.
	 *
	 * @param timeFrameLength interval length of time frames in seconds
	 * @return a list of the generated NetworkChangeEvents. Use is optional.
	 */
	private List<NetworkChangeEvent> generateNetworkChangeEvents(double timeFrameLength) {
		List<NetworkChangeEvent> events = new LinkedList<>();

		for (Link link : network.getLinks().values()) {

			// Don't generate events for links thar are in the shapefile + buffer
			if (geomBuffer.contains(MGC.coord2Point(link.getCoord())))
				continue;

			// Setting capacity outside shapefile (and buffer) to infinite
			link.setCapacity(Double.MAX_VALUE);

			// Do this for the whole 24-hour simulation run
			for (double time = 0; time < 86400; time += timeFrameLength) {

				// Setting freespeed to the link average
				double travelTime = tt.getLinkTravelTimes().getLinkTravelTime(link, time, null, null);
				NetworkChangeEvent event = new NetworkChangeEvent(time);
				event.setFreespeedChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, travelTime));
				NetworkUtils.addNetworkChangeEvent(network, event);
				event.addLink(link);
				events.add(event);
			}
		}

		return events;
	}

	// TODO Make that this method thread safe
	// Parallelized Implementation of PersonAlgorithm for cutout
	@Override
	public void run(Person person) {
		boolean keepPerson = false;

		List<Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());

		Set<Id<Link>> linkIds = new HashSet<>();

		for (Trip trip : trips) {
			Coord originCoord = getActivityCoord(trip.getOriginActivity());
			Coord destinationCoord = getActivityCoord(trip.getDestinationActivity());

			if (originCoord == null || destinationCoord == null) {
				if (noActCoordsWarnings++ < 10)
					log.info("Activity coords of trip is null. Skipping Trip...");

				continue;
			}

			// keep all agents starting or ending in area
			if (geom.contains(MGC.coord2Point(originCoord)) || geom.contains(MGC.coord2Point(destinationCoord))) {
				keepPerson = true;
			}

			LineString line = geoFactory.createLineString(new Coordinate[]{
				MGC.coord2Coordinate(originCoord),
				MGC.coord2Coordinate(destinationCoord)
			});

			// also keep persons traveling through or close to area (beeline)
			if (line.intersects(geom)) {
				keepPerson = true;
			}

			//Save route links
			for (Leg leg : trip.getLegsOnly()) {
				Route route = leg.getRoute();

				// TODO: selected plans may only contain one mode
				// Need to search for routes in all plans, but probably these plans are not really different
				// Need to route modes that are not present, e.g if there is a car plan, route bike as well
				// TODO currently only one mode is considered if a route is present

				if (route instanceof NetworkRoute && !((NetworkRoute) route).getLinkIds().isEmpty()) {
					// We have a NetworkRoute, thus we can just use it
					linkIds.addAll(((NetworkRoute) route).getLinkIds());
					if (((NetworkRoute) route).getLinkIds().stream().anyMatch(linksToKeep::contains)) {
						keepPerson = true;
					}
				} else {
					//No NetworkRoute is given. We need to route by ourselves using Shortest-Path.
					//Search/Generate the route for every mode
					for (String mode : modes) {
						LeastCostPathCalculator.Path path = getModeNetworkPath(mode2modeOnlyNetwork.get(mode), mode, trip);
						if (path != null) {
							// add all these links directly
							path.links.stream().map(Link::getId).forEach(linkIds::add);
							if (path.links.stream().map(Link::getId).anyMatch(linksToKeep::contains)) {
								keepPerson = true;
							}
						}
						//There is no additional link freespeed information, that we could save. So we are finished here
					}
				}
			}

			// TODO: this assumption must be reconsidered
			// TODO: facilities might be cut-out as well
			// activity link ids are reset, because it is not guaranteed they can be retained

			if (trip.getOriginActivity().getLinkId() != null) {
				linkIds.add(trip.getOriginActivity().getLinkId());
				trip.getOriginActivity().setLinkId(null);
			}

			if (trip.getDestinationActivity().getLinkId() != null) {
				linkIds.add(trip.getDestinationActivity().getLinkId());
				trip.getDestinationActivity().setLinkId(null);
			}
		}

		//Check whether this person is relevant or not
		if (keepPerson) {
			linksToInclude.addAll(linkIds);
		} else {
			personsToDelete.add(person.getId());
		}

		PopulationUtils.resetRoutes(person.getSelectedPlan());
	}
}
