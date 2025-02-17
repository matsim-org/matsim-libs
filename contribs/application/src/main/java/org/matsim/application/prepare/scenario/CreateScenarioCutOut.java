package org.matsim.application.prepare.scenario;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import picocli.CommandLine;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	 * Maps mode to the {@link ThreadLocal} containing the router for the mode. This is a thread-local cache, so that we can use the same router for every thread.
	 */
	private final Map<String, ThreadLocal<LeastCostPathCalculator>> mode2routerCache = new ConcurrentHashMap<>();

	@CommandLine.Option(names = "--population", description = "Path to input population", required = true)
	private String populationPath;

	@CommandLine.Option(names = "--network", description = "Path to network", required = true)
	private String networkPath;

	@CommandLine.Option(names = "--facilities", description = "Path to facilities file. Only needed if there are agents with no coordinates or links", required = false)
	private String facilityPath;

	@CommandLine.Option(names = "--events", description = "Input events used for travel time calculation. NOTE: Making a cutout without the events file will make the scenario inaccurate!", required = false)
	private String eventPath;

	@CommandLine.Option(names = "--buffer", description = "Buffer around zones in meter", defaultValue = "5000")
	private double buffer;

	@CommandLine.Option(names = "--output-network", description = "Path to output network", required = true)
	private String outputNetwork;

	@CommandLine.Option(names = "--output-population", description = "Path to output population", required = true)
	private String outputPopulation;

	@CommandLine.Option(names = "--output-facilities", description = "Path to output facilities (required if facilities were given).", required = false)
	private String outputFacilities;

	@CommandLine.Option(names = "--output-network-change-events", description = "Path to network change event output", required = false)
	private String outputEvents;

	@CommandLine.Option(names = "--network-change-events-interval", description = "Interval of NetworkChangesToBeApplied. Unit is seconds. Will be ignored if --output-network-change-events is undefined", defaultValue = "900", required = false)
	private double changeEventsInterval;

	@CommandLine.Option(names = "--network-change-events-maxTime", description = "Interval of NetworkChangesToBeApplied. Unit is seconds. Will be ignored if --output-network-change-events is undefined", defaultValue = "86400", required = false)
	private int changeEventsMaxTime;

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
	 * Map with mode-string as key and the mode-filtered Network as value.
	 */
	private final Map<String, Network> mode2modeOnlyNetwork = new HashMap<>();

	/**
	 * Links that are inside the shapefile.
	 */
	private final Set<Id<Link>> linksToKeep = ConcurrentHashMap.newKeySet();

	/**
	 * Links that are outside the shapefile.
	 */
	private final Set<Id<Link>> linksToDelete = ConcurrentHashMap.newKeySet();

	/**
	 * Additional links to include (may be outside the shapefile). Links are marked like this, if they are used in a plan of an agent, that
	 * is relevant.
	 */
	private final Set<Id<Link>> linksToInclude = ConcurrentHashMap.newKeySet();

	/**
	 * Agents, that are not relevant: Not in the shapefile or buffer, no route through the shapefile or buffer.
	 */
	private final Set<Id<Person>> personsToDelete = ConcurrentHashMap.newKeySet();

	/**
	 * Save which facilities are within the shapefile, or belong to a link in the network, or are used by a person after filtering.
	 */
	private final Set<Id<ActivityFacility>> facilitiesToInclude = ConcurrentHashMap.newKeySet();

	// Data inputs
	/**
	 * Scenario with network, population and facilities.
	 */
	private Scenario scenario;

	/**
	 * Shapefile as a {@link Geometry}.
	 */
	private Geometry geom;

	/**
	 * Shapefile+buffer as a {@link Geometry}.
	 */
	private Geometry geomBuffer;

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

		if (facilityPath != null && outputFacilities == null) {
			log.error("Facilities were given as input, that means --output-facilities must be set.");
			return 2;
		}

		if (eventPath != null && outputEvents == null) {
			log.error("Events were given as input, that means --output-network-change-events must be set.");
			return 2;
		}

		// Prepare required input-data
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(crs.getInputCRS());
		config.plans().setInputFile(populationPath);
		config.network().setInputFile(networkPath);
		config.network().setTimeVariantNetwork(true);
		if (facilityPath != null) {
			config.facilities().setInputFile(facilityPath);
		}
		scenario = ScenarioUtils.loadScenario(config);

		geom = shp.getGeometry(crs.getInputCRS());
		geomBuffer = geom.buffer(buffer);

		for (String mode : modes)
			mode2modeOnlyNetwork.putIfAbsent(mode, filterNetwork(scenario.getNetwork(), mode));

		if (eventPath != null) {
			TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(scenario.getNetwork());
			builder.setTimeslice(changeEventsInterval);
			builder.setMaxTime(changeEventsMaxTime);

			EventsManager manager = EventsUtils.createEventsManager();

			tt = builder.build();
			manager.addHandler(tt);

			manager.initProcessing();
			EventsUtils.readEvents(manager, eventPath);
			manager.finishProcessing();
		}

		// Cut out the network: Filter for links inside the shapefile
		for (Link link : scenario.getNetwork().getLinks().values()) {
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

		log.info("Links in shape: {} out of {}", linksToKeep.size(), scenario.getNetwork().getLinks().size());

		if (linksToKeep.isEmpty()) {
			log.error("No links are in the resulting network. Check your input shape file and coordinate system");
			log.error("If using EPSG:4326 (WGS84), -Dorg.geotools.referencing.forceXY=true might help");
			return 1;
		}

		// Cut out the population and mark needed network parts
		ParallelPersonAlgorithmUtils.run(scenario.getPopulation(), Runtime.getRuntime().availableProcessors(), this);

		//Population
		log.info("Persons in the original population: {}", scenario.getPopulation().getPersons().size());
		log.info("Persons to delete: {}", personsToDelete.size());
		for (Id<Person> personId : personsToDelete) {
			scenario.getPopulation().removePerson(personId);
		}

		log.info("Persons in the resulting scenario: {}", scenario.getPopulation().getPersons().size());

		if (scenario.getPopulation().getPersons().isEmpty()) {
			log.error("No persons are in the resulting population. Check if your input shape file and coordinate system is correct. Exiting ...");
			return 1;
		}

		// Network
		log.info("Additional links from routes to include: {}", linksToInclude.size());

		log.info("number of links in original network: {}", scenario.getNetwork().getLinks().size());

		for (Id<Link> linkId : linksToDelete) {
			if (!linksToInclude.contains(linkId))
				scenario.getNetwork().removeLink(linkId);
		}

		// clean the network
		log.info("number of links before cleaning: {}", scenario.getNetwork().getLinks().size());
		log.info("number of nodes before cleaning: {}", scenario.getNetwork().getNodes().size());

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(scenario.getNetwork());
		cleaner.removeNodesWithoutLinks();

		for (String mode : modes) {
			log.info("Cleaning mode {}", mode);
			cleaner.run(Set.of(mode));
		}

		log.info("number of links after cleaning: {}", scenario.getNetwork().getLinks().size());
		log.info("number of nodes after cleaning: {}", scenario.getNetwork().getNodes().size());

		ParallelPersonAlgorithmUtils.run(scenario.getPopulation(), Runtime.getRuntime().availableProcessors(), new CleanPersonLinkIds());

		PopulationUtils.writePopulation(scenario.getPopulation(), outputPopulation);

		if (facilityPath != null) {

			log.info("number of facilities before filtering: {}", scenario.getActivityFacilities().getFacilities().size());

			filterFacilities();

			new FacilitiesWriter(scenario.getActivityFacilities()).write(outputFacilities);
			log.info("number of facilities after filtering: {}", scenario.getActivityFacilities().getFacilities().size());
		}

		if (eventPath != null) {
			List<NetworkChangeEvent> events = generateNetworkChangeEvents(changeEventsInterval);
			new NetworkChangeEventsWriter().write(outputEvents, events);
		}

		NetworkUtils.writeNetwork(scenario.getNetwork(), outputNetwork);

		return 0;
	}

	// Helper-Functions

	/**
	 * Filters the network to the given mode.
	 */
	private Network filterNetwork(Network network, String mode) {
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);

		Network carOnlyNetwork = NetworkUtils.createNetwork();
		filter.filter(carOnlyNetwork, Set.of(mode));
		return carOnlyNetwork;
	}

	/**
	 * Filter facilities.
	 */
	private void filterFacilities() {

		for (ActivityFacility f : scenario.getActivityFacilities().getFacilities().values()) {
			if (facilitiesToInclude.contains(f.getId()))
				continue;

			if (f.getLinkId() != null && scenario.getNetwork().getLinks().containsKey(f.getLinkId())) {
				facilitiesToInclude.add(f.getId());
				continue;
			}

			// Expensive check last
			if (f.getCoord() != null && geom.contains(MGC.coord2Point(f.getCoord())))
				facilitiesToInclude.add(f.getId());
		}

		Set<Id<ActivityFacility>> facilityIds = new HashSet<>(scenario.getActivityFacilities().getFacilities().keySet());
		for (Id<ActivityFacility> id : facilityIds) {
			if (!facilitiesToInclude.contains(id))
				scenario.getActivityFacilities().getFacilities().remove(id);
		}
/*
		scenario.getActivityFacilities().getFacilities().keySet()
			.removeIf(Predicate.not(facilitiesToInclude::contains));
*/
	}

	/**
	 * Creates a {@link LeastCostPathCalculator} for the given network and saves it in {@code mode2routerCache}.
	 * The network should be filtered to the expected mode using {@link CreateScenarioCutOut#filterNetwork}.
	 */
	private LeastCostPathCalculator createRouter(Network network, String mode) {
		mode2routerCache.putIfAbsent(mode, new ThreadLocal<>());
		LeastCostPathCalculator c = mode2routerCache.get(mode).get();
		if (c == null) {
			FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
			LeastCostPathCalculatorFactory fastAStarLandmarksFactory = new SpeedyALTFactory();

			OnlyTimeDependentTravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);

			c = fastAStarLandmarksFactory.createPathCalculator(network, travelDisutility, travelTime);
			mode2routerCache.putIfAbsent(mode, new ThreadLocal<>());
			mode2routerCache.get(mode).set(c);
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

	/**
	 * Routes a Path in the given network and mode.
	 */
	private LeastCostPathCalculator.Path getModeNetworkPath(Network network, String mode, Trip trip) {
		LeastCostPathCalculator router = createRouter(network, mode);

		Node fromNode = getFromNode(network, trip);
		Node toNode = getToNode(network, trip);

		if (fromNode == null || toNode == null)
			return null;

		return router.calcLeastCostPath(fromNode, toNode, 0, null, null);
	}

	/**
	 * Returns {@link Coord} of an activity, either from the activity itself or from the facility.
	 */
	private Coord getActivityCoord(Activity activity) {
		if (scenario.getActivityFacilities() != null && activity.getFacilityId() != null && scenario.getActivityFacilities().getFacilities().containsKey(activity.getFacilityId()))
			return scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId()).getCoord();

		if (activity.getCoord() != null)
			return activity.getCoord();

		if (activity.getLinkId() != null && scenario.getNetwork().getLinks().containsKey(activity.getLinkId()))
			return scenario.getNetwork().getLinks().get(activity.getLinkId()).getCoord();

		return null;
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

		for (Link link : scenario.getNetwork().getLinks().values()) {

			// Don't generate events for links thar are in the shapefile + buffer
			if (geomBuffer.contains(MGC.coord2Point(link.getCoord())))
				continue;

			// Setting capacity outside shapefile (and buffer) to infinite
			link.setCapacity(Double.MAX_VALUE);

			// Do this for the whole simulation run
			for (double time = 0; time < changeEventsMaxTime; time += timeFrameLength) {

				// Setting freespeed to the link average
				double freespeed = link.getLength() / tt.getLinkTravelTimes().getLinkTravelTime(link, time, null, null);
				NetworkChangeEvent event = new NetworkChangeEvent(time);
				event.setFreespeedChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, freespeed));
				NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), event);
				event.addLink(link);
				events.add(event);
			}
		}

		return events;
	}

	/**
	 * Parallelized Implementation of PersonAlgorithm for cutout. It checks whether this {@link Person} is relevant and if it is, it adds the
	 * {@link Link}s of the persons-route into {@code linksToInclude}.
	 * If this {@link Person} is not relevant, it is added into {@code personsToDelete}.
	 */
	@Override
	public void run(Person person) {
		boolean keepPerson = false;

		List<Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
		List<Activity> activities = TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
		Set<Id<Link>> linkIds = new HashSet<>();
		Set<Id<ActivityFacility>> facilityIds = new HashSet<>();

		// Check activities first, some agent might be stationary
		for (Activity act : activities) {
			Coord coord = getActivityCoord(act);

			if (coord == null && noActCoordsWarnings++ < 10) {
				log.info("Activity coords for agent {} are null. Skipping Trip...", person.getId());
			}

			if (coord != null && geom.contains(MGC.coord2Point(coord))) {
				keepPerson = true;
			}

			if (act.getFacilityId() != null)
				facilityIds.add(act.getFacilityId());

			if (act.getLinkId() != null)
				linkIds.add(act.getLinkId());

		}

		for (Trip trip : trips) {
			Coord originCoord = getActivityCoord(trip.getOriginActivity());
			Coord destinationCoord = getActivityCoord(trip.getDestinationActivity());

			if (originCoord != null && destinationCoord != null) {
				LineString line = geoFactory.createLineString(new Coordinate[]{
					MGC.coord2Coordinate(originCoord),
					MGC.coord2Coordinate(destinationCoord)
				});

				// also keep persons traveling through or close to area (beeline)
				if (line.intersects(geom)) {
					keepPerson = true;
				}
			}

			//Save route links
			for (Leg leg : trip.getLegsOnly()) {
				Route route = leg.getRoute();

				// Need to search for routes in all plans, but probably these plans are not really different
				// Need to route modes that are not present, e.g if there is a car plan, route bike as well

				// Store which modes needs to be routed
				Set<String> routingModes = new HashSet<>(modes);

				if (route instanceof NetworkRoute && !((NetworkRoute) route).getLinkIds().isEmpty()) {
					// We have a NetworkRoute, thus we can just use it
					linkIds.addAll(((NetworkRoute) route).getLinkIds());
					if (((NetworkRoute) route).getLinkIds().stream().anyMatch(linksToKeep::contains)) {
						keepPerson = true;
					}

					routingModes.remove(leg.getRoutingMode());
				}

				// Now compute routes for all other modes (all modes if no NetworkRoute is given) using shortest-path.
				// Search/Generate the route for every mode
				for (String mode : routingModes) {
					LeastCostPathCalculator.Path path = getModeNetworkPath(mode2modeOnlyNetwork.get(mode), mode, trip);
					if (path != null) {
						// add all these links directly
						path.links.stream().map(Link::getId).forEach(linkIds::add);
						if (path.links.stream().map(Link::getId).anyMatch(linksToKeep::contains)) {
							keepPerson = true;
						}
					}
					// There is no additional link freespeed information, that we could save. So we are finished here
				}
			}
		}

		//Check whether this person is relevant or not
		if (keepPerson) {
			linksToInclude.addAll(linkIds);
			facilitiesToInclude.addAll(facilityIds);
		} else {
			personsToDelete.add(person.getId());
		}

		// Remove all unselected plans because these are not handled
		List<Plan> plans = new ArrayList<>(person.getPlans());
		for(Plan p : plans){
			if (p != person.getSelectedPlan()){
				person.removePlan(p);
			}
		}
	}


	private final class CleanPersonLinkIds implements PersonAlgorithm {


		@Override
		public void run(Person person) {

			Plan plan = person.getSelectedPlan();
			Network network = scenario.getNetwork();

			for (Trip trip : TripStructureUtils.getTrips(plan)) {
				// activity link ids are reset, if they are not retained in the cleaned network
				if (trip.getOriginActivity().getLinkId() != null) {
					if (!network.getLinks().containsKey(trip.getOriginActivity().getLinkId()))
						trip.getOriginActivity().setLinkId(null);
				}

				if (trip.getDestinationActivity().getLinkId() != null) {
					if (!network.getLinks().containsKey(trip.getDestinationActivity().getLinkId()))
						trip.getDestinationActivity().setLinkId(null);
				}
			}

			for (Leg leg : TripStructureUtils.getLegs(plan)) {

				if (!(leg.getRoute() instanceof NetworkRoute r))
					continue;

				Stream<Id<Link>> stream = Stream.concat(Stream.of(r.getStartLinkId(), r.getEndLinkId()), r.getLinkIds().stream());

				boolean valid = stream.allMatch(l -> {

					Link link = network.getLinks().get(l);

					// Check if link is present in the network
					if (link == null)
						return false;

					// Check if the link has the needed mode
					return link.getAllowedModes().contains(leg.getMode());
				});

				if (!valid) {
					PopulationUtils.resetRoutes(plan);
					break;
				}
			}
		}
	}

}
