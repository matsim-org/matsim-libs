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
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.*;
import org.matsim.vehicles.Vehicle;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CommandLine.Command(name = "scenario-cutout", description = "Cut out a scenario based on a shape file. Reduce population and network to the relevant area.")
public class CreateScenarioCutOut implements MATSimAppCommand, PersonAlgorithm, LinkEnterEventHandler, LinkLeaveEventHandler {

    private static final Logger log = LogManager.getLogger(CreateScenarioCutOut.class);
    private final Map<String, ThreadLocal<LeastCostPathCalculator>> routerCache = new HashMap<>(); //Router for every mode

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

	@CommandLine.Option(names = "--network-change-events-interval", description = "Interval of NetworkChangesToBeApplied. Unit is seconds. Will be ignored if --output-network-change-events is undefined", defaultValue="900", required = false)
	private double changeEventsInterval;

    @CommandLine.Option(names = "--network-modes", description = "Modes to consider when cutting network", defaultValue = "car,bike", split = ",")
    private Set<String> modes;

    @CommandLine.Mixin
    private CrsOptions crs;

    @CommandLine.Mixin
    private ShpOptions shp;

	//private variables for computing
	private static final GeometryFactory geoFactory = new GeometryFactory();
	private static final Map<String, Network> mode2modeOnlyNetwork = new HashMap();
	private static final Set<Id<Link>> linksToKeep = new HashSet<>(); //Links inside the shapefile
	private static final Set<Id<Link>> linksToDelete = new HashSet<>(); //Links outside the shapefile
	private static final Set<Id<Link>> linksToInclude = ConcurrentHashMap.newKeySet(); // additional links to include (may be outside the shapefile)
	private static final Set<Id<Person>> personsToDelete = ConcurrentHashMap.newKeySet(); // now delete irrelevant persons
	private static final EventsManager manager = EventsUtils.createEventsManager();
	private static final Map<Tuple<Id<Link>, Id<Vehicle>>, Double> linkIdVehicleId2enterTime = new HashMap<>();
	private static final Map<Id<Link>, List<Tuple<Double, Double>>> linkId2travelTimesAtTime = new HashMap<>();
	private static Population population;
	private static Network network;
	private static Geometry geom;
	private static ActivityFacilities facilities;

	private static boolean useFacilities = false;
	private static boolean useEvents = false;
	private static boolean useNetworkChangeEvents = false;

	public static void main(String[] args) {
        new CreateScenarioCutOut().execute(args);
    }

	/**
	 * Cuts out a part of the Population and Network which is relevant inside the specified shape of the shapefile.<br><br>
	 *
	 * How the network is cut out:
	 * <ul>
	 * 		<li>Keep all links in shape file + configured buffer area</li>
	 * 		<li>Keep all links that are used by any agent in its route</li>
	 * 		<li>if an agent has not route, a shortest-path route is calculated and used instead</li>
	 * 		<li>This is done for all network modes</li>
	 * </ul>
	 *
	 * How the population is cut out:
	 * <ul>
	 * 		<li>All agents having any activity in the shape file are kept</li>
	 * 		<li>Agents traveling through the shape file are kept, this is determined by the route</li>
	 * 		<li>The buffer is not considered for the population cut out</li>
	 * </ul>
	 *
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
	 *
	 * The cut-out will create a buffer area around the shape in which the speed is simulated with the original network, so that it is sensitive to changes
	 * Therefore, the buffer should not be too small
	 * One should be careful creating too small cut-outs because of the mentioned limitations.
	 *
	 * @return 0 if successfull, 2 if CRS is null
	 * @throws Exception
	 */
    @Override
    public Integer call() throws Exception {
		//Check CRS
		if (crs.getInputCRS() == null) {
			log.error("Input CRS must be specified");
			return 2;
		}

		// Prepare required input-data
        population = PopulationUtils.readPopulation(input.toString());
		network = NetworkUtils.readNetwork(networkPath.toString());
		geom = shp.getGeometry().buffer(buffer);
		for(String mode : modes) mode2modeOnlyNetwork.putIfAbsent(mode, filterNetwork(network, mode));
		// Prepare optional input-data
		if(facilityPath != null){
			useFacilities = true;
			facilities = FacilitiesUtils.createActivityFacilities();
			new MatsimFacilitiesReader(crs.getInputCRS(), crs.getTargetCRS(), facilities).readFile(facilityPath.toString());
		}
		if(eventPath != null){
			useEvents = true;
			manager.addHandler(new TravelTimeCalculator.Builder(network).build());
			EventsUtils.readEvents(manager, eventPath.toString());
		}
		if(outputEvents != null){
			useNetworkChangeEvents = true;
		}

		// Cut out the network: Filter for links inside the shapefile
        for (Link link : network.getLinks().values()) {
            if (geom.contains(MGC.coord2Point(link.getCoord()))
                    || geom.contains(MGC.coord2Point(link.getFromNode().getCoord()))
                    || geom.contains(MGC.coord2Point(link.getToNode().getCoord()))
                    || link.getAllowedModes().contains(TransportMode.pt)) {
                // keep the link
                linksToKeep.add(link.getId());
            } else {
                linksToDelete.add(link.getId());
            }
        }

		// TODO: consider facilities, (see FilterRelevantAgents in OpenBerlin) -> (done, untested)
		// TODO: Use events for travel time calculation, (this is optional) -> (done, untested)
		// TODO: Add optionality of events-file -> (done, untested)
		// TODO: Output network change events should only be calculated when events are provided
		// TODO: consider all network modes for routing, currently only car is considered -> (done, untested)

		//Cut out the population and mark needed network parts
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

		if(useNetworkChangeEvents){
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

	private Node getFromNode(Network network, Trip trip){
		Map<Id<Link>, ? extends Link> modeLinks = network.getLinks();
		if (trip.getOriginActivity().getLinkId() != null && modeLinks.get(trip.getOriginActivity().getLinkId()) != null) {
			return modeLinks.get(trip.getOriginActivity().getLinkId()).getFromNode();
		} else {
			return NetworkUtils.getNearestLink(network, getActivityCoord(trip.getOriginActivity())).getFromNode();
		}
	}

	private Node getToNode(Network network, Trip trip){
		Map<Id<Link>, ? extends Link> modeLinks = network.getLinks();
		if (trip.getDestinationActivity().getLinkId() != null && modeLinks.get(trip.getDestinationActivity().getLinkId()) != null) {
			return modeLinks.get(trip.getDestinationActivity().getLinkId()).getFromNode();
		} else {
			return NetworkUtils.getNearestLink(network, getActivityCoord(trip.getDestinationActivity())).getFromNode();
		}
	}

	private LeastCostPathCalculator.Path getModeNetworkPath(Network network, String mode, Trip trip){
		LeastCostPathCalculator router = createRouter(network, mode);

		Node fromNode = getFromNode(network, trip);
		Node toNode = getToNode(network, trip);

		return router.calcLeastCostPath(fromNode, toNode, 0, null, null);
	}

	private Coord getActivityCoord(Activity activity) {
		if(!useFacilities) return activity.getCoord();
		return facilities.getFacilities().get(activity.getFacilityId()).getCoord();
	}

	// TravelTime Calculation

	/**
	 * This function approximates the travel time on each link by distributing the travelTime along the links via the euclidean length and freespeed.<br>
	 * <i>NOTE: This is not accurate but better than noting. It is advised to use an events file, which will use
	 * {@link CreateScenarioCutOut#computeAverageLinkFreespeedDuringTimeFrame} and  greatly improve the accuracy!</i>
	 * @param route Route for which links we want to calculate the freespeeds
	 * @return Sorted list with tuples (linkId, freespeed), containing the given links. Order is copied from route
	 */
	private List<Tuple<Id<Link>, Double>> computeApproximatedLinkFreespeedForLinks(NetworkRoute route){
		double theoreticalTT = 0;
		List<Tuple<Id<Link>, Double>> freespeeds = new LinkedList<>();
		//Compute the theoretical Travel Time if there would be no congestion
		for(Id<Link> linkId : route.getLinkIds()) theoreticalTT += network.getLinks().get(linkId).getLength() / network.getLinks().get(linkId).getFreespeed();
		//Now apply the actual travel time to the links by altering the freespeeds
		for(Id<Link> linkId : route.getLinkIds()){
			Link link = network.getLinks().get(linkId);
			double actualTT = (link.getLength()/link.getFreespeed()) * (route.getTravelTime().seconds()/theoreticalTT);
			freespeeds.add(new Tuple<>(link.getId(), link.getLength()/actualTT));
		}
		return freespeeds;
	}

	/**
	 * Computes the average speed of a link for the given timeframe using the events file.
	 * <i>NOTE: Do not make the time frames too small. If no linkEnter/Leave Events are found during a time period, the methods uses
	 * a single reference value, which is not optimal.</i>
	 * @param linkId link-id to compute the avereage freespeed for
	 * @param fromTimeToTime Tuple (start, end) of representing the time interval
	 * @return computed average freespeed
	 */
	private double computeAverageLinkFreespeedDuringTimeFrame(Id<Link> linkId, Tuple<Double, Double> fromTimeToTime){
		double sum = 0;
		int found = 0;
		for(Tuple<Double, Double> e : linkId2travelTimesAtTime.get(linkId)){
			if(e.getSecond() >= fromTimeToTime.getFirst() && e.getSecond() <= fromTimeToTime.getSecond()) {
				//Vehicle left this link during the timeframe
				sum += e.getFirst();
				found++;
			}
		}
		if(found == 0){
			log.warn("No travel time for link {} during time window {}, {}. This may be caused by too small timeframes!", linkId, fromTimeToTime.getFirst(), fromTimeToTime.getSecond());

			for(Tuple<Double, Double> e : linkId2travelTimesAtTime.get(linkId)) if(e.getSecond() >= fromTimeToTime.getFirst()) return network.getLinks().get(linkId).getLength()/e.getFirst();
		}
		return network.getLinks().get(linkId).getLength()/(sum/found);
	}

	/**
	 * Creates and applies the {@link NetworkChangeEvent}s to the network. It sets the capacity for all links outside the shapefile and bufferzone
	 * to infinite and reduces the freespeed of the links so that the agents behave somehow realistically outside the cutout-area.
	 * @param timeFrameLength interval length of time frames in seconds
	 * @return a list of the generated NetworkChangeEvents. Use is optional.
	 */
	private List<NetworkChangeEvent> generateNetworkChangeEvents(double timeFrameLength){
		List<NetworkChangeEvent> events = new LinkedList<>();
		// Setting capacity outside of shapefile (and buffer) to infinite
		for(Id<Link> linkId : linksToDelete){
			if(linksToInclude.contains(linkId)){
				NetworkChangeEvent event = new NetworkChangeEvent(0);
				event.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(
					NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS,
					Double.MAX_VALUE));
				NetworkUtils.addNetworkChangeEvent(network, event);
				events.add(event);
			}
		}
		for(double time = 0; time < 129600; time+=timeFrameLength){
			//Do this for the whole 36-hour simulation run
			for(Link link : network.getLinks().values()){
				double avgFreespeed = computeAverageLinkFreespeedDuringTimeFrame(link.getId(), new Tuple<>(time, time+timeFrameLength));
				NetworkChangeEvent event = new NetworkChangeEvent(time);
				event.setFreespeedChange(new NetworkChangeEvent.ChangeValue(
					NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS,
					avgFreespeed));
				NetworkUtils.addNetworkChangeEvent(network, event);
				events.add(event);
			}
		}
		return events;
	}

	// Parallelized Implementation of PersonAlgorithm for cutout

	@Override
	public void run(Person person) {
		boolean keepPerson = false;

		List<Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());

		Set<Id<Link>> linkIds = new HashSet<>();

		for (Trip trip : trips) {
			// keep all agents starting or ending in area
			if (geom.contains(MGC.coord2Point(getActivityCoord(trip.getOriginActivity()))) || geom.contains(MGC.coord2Point(getActivityCoord(trip.getDestinationActivity())))) {
				keepPerson = true;
			}

			Coord originCoord = getActivityCoord(trip.getOriginActivity());
			Coord destinationCoord = getActivityCoord(trip.getDestinationActivity());

			LineString line = geoFactory.createLineString(new Coordinate[]{
				MGC.coord2Coordinate(originCoord),
				MGC.coord2Coordinate(destinationCoord)
			});

			// also keep persons traveling through or close to area (beeline)
			if (line.intersects(geom)) {
				keepPerson = true;
			}

			// TODO: only route if there is no route -> (done, untested)
			// TODO: create network routes for all network modes -> (done, untested)

			//Save route links
			for (Leg leg : trip.getLegsOnly()) {
				Route route = leg.getRoute();
				if (route instanceof NetworkRoute) { // TODO Check how this behaves for pt/teleport routes
					// We have a NetworkRoute, thus we can just use it
					linkIds.addAll(((NetworkRoute) route).getLinkIds());
					if (((NetworkRoute) route).getLinkIds().stream().anyMatch(linksToKeep::contains)) {
						keepPerson = true;
					}
					//If we are working without an events-file we need to save the travel time via route information
					if(!useEvents){
						List<Tuple<Id<Link>, Double>> linkId2freespeeds = computeApproximatedLinkFreespeedForLinks(((NetworkRoute) route));
						double currentTime = linkId2freespeeds.getFirst().getSecond();
						for(Tuple<Id<Link>, Double> e : linkId2freespeeds){
							linkId2travelTimesAtTime.get(e.getFirst()).add(new Tuple<>(e.getSecond(), currentTime+e.getSecond()));
							currentTime += e.getSecond();
						}
					}
				} else {
					//No NetworkRoute is given. We need to route by ourselves using Shortest-Path.
					//Search/Generate the route for every mode which is not included in the selected plan
					for(String mode : modes){
						LeastCostPathCalculator.Path path = getModeNetworkPath(mode2modeOnlyNetwork.get(mode), mode, trip);
						if (path != null) {
							// add all these links directly
							path.links.stream().map(Link::getId).forEach(linkIds::add);
							if (path.links.stream().map(Link::getId).anyMatch(linksToKeep::contains)) {
								keepPerson = true;
							}
						}
						//There is no additional link freespeed information, that we could save.
					}
				}
			}

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

	// XMLParser methods

	@Override
	public void handleEvent(LinkEnterEvent event) {
		linkIdVehicleId2enterTime.put(new Tuple<>(event.getLinkId(), event.getVehicleId()), event.getTime());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		assert linkIdVehicleId2enterTime.containsKey(new Tuple<>(event.getLinkId(), event.getVehicleId())); //If this fails, the events file may be corrupted
		double travelTime = event.getTime() - linkIdVehicleId2enterTime.get(new Tuple<>(event.getLinkId(), event.getVehicleId()));
		linkIdVehicleId2enterTime.remove(new Tuple<>(event.getLinkId(), event.getVehicleId()));
		linkId2travelTimesAtTime.putIfAbsent(event.getLinkId(), new LinkedList<>());
		linkId2travelTimesAtTime.get(event.getLinkId()).add(new Tuple<>(travelTime, event.getTime()));
	}
}
