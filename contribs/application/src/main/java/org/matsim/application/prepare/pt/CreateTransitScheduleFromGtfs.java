package org.matsim.application.prepare.pt;

import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Stop;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.contrib.gtfs.TransitSchedulePostProcessTools;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.CreatePseudoNetworkWithLoopLinks;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.*;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * This script utilizes GTFS2MATSim and creates a pseudo network and vehicles using MATSim standard API functionality.
 *
 * @author rakow
 */
@CommandLine.Command(
	name = "transit-from-gtfs",
	description = "Create transit schedule and vehicles from GTFS data and merge pt network into existing network",
	showDefaultValues = true,
	mixinStandardHelpOptions = true
)
public class CreateTransitScheduleFromGtfs implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateTransitScheduleFromGtfs.class);

	@CommandLine.Parameters(arity = "1..*", paramLabel = "INPUT", description = "Input GTFS zip files")
	private List<Path> gtfsFiles;

	@CommandLine.Option(names = "--name", description = "Prefix of the output files", required = true)
	private String name;

	@CommandLine.Option(names = "--network", description = "Base network that will be merged with pt network.", required = true)
	private String networkFile;

	@CommandLine.Option(names = "--date", arity = "1..*", description = "The day for which the schedules will be extracted, can also be multiple", required = true)
	private List<LocalDate> date;

	@CommandLine.Option(names = "--output", description = "Output folder", required = true)
	private File output;

	@CommandLine.Option(names = "--copy-late-early", description = "Copy late/early departures to have at complete schedule")
	private boolean copyLateEarlyDepartures;

	@CommandLine.Option(names = "--validate", description = "Validate the resulting network and schedule", defaultValue = "true", negatable = true)
	private boolean validate;

	@CommandLine.Option(names = "--include-stops", description = "Fully qualified class name to a Predicate<Stop> for filtering certain stops")
	private Class<?> includeStops;

	@CommandLine.Option(names = "--transform-stops", description = "Fully qualified class name to a Consumer<Stop> for transforming stops before usage")
	private Class<?> transformStops;

	@CommandLine.Option(names = "--transform-routes", description = "Fully qualified class name to a Consumer<Route> for transforming routes before usage")
	private Class<?> transformRoutes;

	@CommandLine.Option(names = "--transform-schedule", description = "Fully qualified class name to a Consumer<TransitSchedule> to be executed after the schedule was created", arity = "0..*", split = ",")
	private List<Class<?>> transformSchedule;

	@CommandLine.Option(names = "--merge-stops", description = "Whether stops should be merged by coordinate", defaultValue = "doNotMerge")
	private GtfsConverter.MergeGtfsStops mergeStops;

	@CommandLine.Option(names = "--prefix", description = "Prefixes to add to the gtfs ids. Required if multiple inputs are used and ids are not unique.", split = ",")
	private List<String> prefixes = new ArrayList<>();

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions("EPSG:4326");

	@CommandLine.Option(names = "--shp", description = "Path to shape file for filtering stops. Can be multiple if different filters on each input file should be used.", arity = "0..*")
	private List<Path> shpFiles;

	@CommandLine.Option(names = "--shp-crs", description = "Overwrite coordinate system of the shape file")
	private String shpCrs;

	@CommandLine.Option(names = "--pseudo-network", description = "Define how the pseudo network should be created", defaultValue = "singleLinkBetweenStops")
	private PseudoNetwork pseudoNetwork;


	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateTransitScheduleFromGtfs()).execute(args));
	}

	private static void addHbefaMapping(VehicleType vehicleType, HbefaVehicleCategory category) {
		EngineInformation carEngineInformation = vehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory(carEngineInformation, String.valueOf(category));
		VehicleUtils.setHbefaTechnology(carEngineInformation, "average");
		VehicleUtils.setHbefaSizeClass(carEngineInformation, "average");
		VehicleUtils.setHbefaEmissionsConcept(carEngineInformation, "average");
		vehicleType.setNetworkMode(TransportMode.pt);
	}

	private static void increaseLinkFreespeedIfLower(Link link, double newFreespeed) {
		if (link.getFreespeed() < newFreespeed) {
			link.setFreespeed(newFreespeed);
		}
	}

	@Override
	public Integer call() throws Exception {

		CoordinateTransformation ct = crs.getTransformation();

		// Output files
		File scheduleFile = new File(output, name + "-transitSchedule.xml.gz");
		File networkPTFile = new File(output,
			FilenameUtils.getName(IOUtils.resolveFileOrResource(networkFile).getPath())
				.replace(".xml", "-with-pt.xml"));
		File transitVehiclesFile = new File(output, name + "-transitVehicles.xml.gz");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		log.info("Using shp files: {}", shpFiles);

		int i = 0;
		for (Path gtfsFile : gtfsFiles) {

			LocalDate date = this.date.size() > 1 ? this.date.get(i) : this.date.get(0);

			log.info("Converting {} at date {}", gtfsFile, date);

			GtfsConverter.Builder converter = GtfsConverter.newBuilder()
				.setScenario(scenario)
				.setTransform(ct)
				.setDate(date) // use first date for all sets
				.setFeed(gtfsFile)
				//.setIncludeAgency(agency -> agency.equals("rbg-70"))
				.setIncludeStop(createFilter(i))
				.setMergeStops(mergeStops);

			if (prefixes.size() > 0) {
				String prefix = prefixes.get(i);
				converter.setPrefix(prefix);
				log.info("Using prefix: {}", prefix);
			}

			if (transformStops != null) {
				converter.setTransformStop(createConsumer(transformStops, Stop.class));
			}

			if (transformRoutes != null) {
				converter.setTransformRoute(createConsumer(transformRoutes, Route.class));
			}


			converter.build().convert();
			i++;
		}

		if (copyLateEarlyDepartures) {
			// copy late/early departures to have at complete schedule from ca. 0:00 to ca. 30:00
			TransitSchedulePostProcessTools.copyLateDeparturesToStartOfDay(scenario.getTransitSchedule(), 24 * 3600, "copied", false);
			TransitSchedulePostProcessTools.copyEarlyDeparturesToFollowingNight(scenario.getTransitSchedule(), 6 * 3600, "copied");
		}

		if (transformSchedule != null && !transformSchedule.isEmpty()) {
			for (Class<?> c : transformSchedule) {
				Consumer<TransitSchedule> f = createConsumer(c, TransitSchedule.class);
				log.info("Applying {} to created schedule", c.getName());
				f.accept(scenario.getTransitSchedule());
			}
		}

		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			List<TransitRoute> routes = new ArrayList<>(line.getRoutes().values());
			for (TransitRoute route : routes) {
				if (route.getDepartures().isEmpty()) {
					log.warn("Route {} in line {} with no departures removed.", route.getId(), line.getId());
					line.removeRoute(route);
				}
			}
		}

		Network network = NetworkUtils.readNetwork(networkFile);

		Scenario ptScenario = getScenarioWithPseudoPtNetworkAndTransitVehicles(network, scenario.getTransitSchedule(), "pt_");

		for (TransitLine line : new ArrayList<>(scenario.getTransitSchedule().getTransitLines().values())) {
			if (line.getRoutes().isEmpty()) {
				log.warn("Line {} with no routes removed.", line.getId());
				scenario.getTransitSchedule().removeTransitLine(line);
			}
		}

		if (validate) {
			//Check schedule and network
			TransitScheduleValidator.ValidationResult checkResult = TransitScheduleValidator.validateAll(ptScenario.getTransitSchedule(), ptScenario.getNetwork());
			List<String> warnings = checkResult.getWarnings();
			if (!warnings.isEmpty())
				log.warn("TransitScheduleValidator warnings: {}", String.join("\n", warnings));

			if (checkResult.isValid()) {
				log.info("TransitSchedule and Network valid according to TransitScheduleValidator");
			} else {
				log.error("TransitScheduleValidator errors: {}", String.join("\n", checkResult.getErrors()));
				throw new RuntimeException("TransitSchedule and/or Network invalid");
			}
		}

		new TransitScheduleWriter(ptScenario.getTransitSchedule()).writeFile(scheduleFile.getAbsolutePath());

		log.info("Writing network to {}", networkPTFile.getAbsolutePath());

		new NetworkWriter(ptScenario.getNetwork()).write(networkPTFile.getAbsolutePath());
		new MatsimVehicleWriter(ptScenario.getTransitVehicles()).writeFile(transitVehiclesFile.getAbsolutePath());

		return 0;
	}

	@SuppressWarnings({"unchecked", "unused"})
	private <T> Consumer<T> createConsumer(Class<?> consumer, Class<T> type) throws ReflectiveOperationException {
		return (Consumer<T>) consumer.getDeclaredConstructor().newInstance();
	}

	@SuppressWarnings("unchecked")
	private Predicate<Stop> createFilter(int i) throws Exception {

		Predicate<Stop> filter = (stop) -> true;
		if (shpFiles != null && !shpFiles.isEmpty()) {
			ShpOptions shp = new ShpOptions(shpFiles.size() == 1 ? shpFiles.get(0) : shpFiles.get(i), shpCrs, null);

			log.info("Using shp file {} for input {}", shp.getShapeFile(), i);

			// default input is set to lat lon
			ShpOptions.Index index = shp.createIndex(crs.getInputCRS(), "_");
			filter = (stop) -> index.contains(new Coord(stop.stop_lon, stop.stop_lat));
		}

		if (includeStops != null) {
			filter = filter.and((Predicate<Stop>) includeStops.getDeclaredConstructor().newInstance());
		}

		return filter;
	}

	/**
	 * Creates the pt scenario and network.
	 */
	private Scenario getScenarioWithPseudoPtNetworkAndTransitVehicles(Network network, TransitSchedule schedule, String ptNetworkIdentifier) {
		ScenarioUtils.ScenarioBuilder builder = new ScenarioUtils.ScenarioBuilder(ConfigUtils.createConfig());
		builder.setNetwork(network);
		builder.setTransitSchedule(schedule);
		Scenario scenario = builder.build();

		// add pseudo network for pt
		switch (pseudoNetwork) {
			case singleLinkBetweenStops ->
				new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), ptNetworkIdentifier, 0.1, 100000.0).createNetwork();
			case withLoopLinks ->
				new CreatePseudoNetworkWithLoopLinks(scenario.getTransitSchedule(), scenario.getNetwork(), ptNetworkIdentifier, 0.1, 100000.0).createNetwork();
		}

		// create TransitVehicle types
		// see https://svn.vsp.tu-berlin.de/repos/public-svn/publications/vspwp/2014/14-24/ for veh capacities
		// the values set here are at the upper end of the typical capacity range, so on lines with high capacity vehicles the
		// capacity of the matsim vehicle equals roughly the real vehicles capacity and on other lines the Matsim vehicle
		// capacity is higher than the real used vehicle's capacity (gtfs provides no information on which vehicle type is used,
		// and this would be beyond scope here). - gleich sep'19
		VehiclesFactory vehicleFactory = scenario.getVehicles().getFactory();

		VehicleType reRbVehicleType = vehicleFactory.createVehicleType(Id.create("RE_RB_veh_type", VehicleType.class));
		{
			VehicleCapacity capacity = reRbVehicleType.getCapacity();
			capacity.setSeats(500);
			capacity.setStandingRoom(600);
			VehicleUtils.setDoorOperationMode(reRbVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(reRbVehicleType, 1.0 / 10.0); // 1s per boarding agent, distributed on 10 doors
			VehicleUtils.setEgressTime(reRbVehicleType, 1.0 / 10.0); // 1s per alighting agent, distributed on 10 doors

			addHbefaMapping(reRbVehicleType, HbefaVehicleCategory.NON_HBEFA_VEHICLE);
			scenario.getTransitVehicles().addVehicleType(reRbVehicleType);
		}
		VehicleType sBahnVehicleType = vehicleFactory.createVehicleType(Id.create("S-Bahn_veh_type", VehicleType.class));
		{
			VehicleCapacity capacity = sBahnVehicleType.getCapacity();
			capacity.setSeats(400);
			capacity.setStandingRoom(800);
			VehicleUtils.setDoorOperationMode(sBahnVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(sBahnVehicleType, 1.0 / 24.0); // 1s per boarding agent, distributed on 8*3 doors
			VehicleUtils.setEgressTime(sBahnVehicleType, 1.0 / 24.0); // 1s per alighting agent, distributed on 8*3 doors

			addHbefaMapping(sBahnVehicleType, HbefaVehicleCategory.NON_HBEFA_VEHICLE);
			scenario.getTransitVehicles().addVehicleType(sBahnVehicleType);
		}
		VehicleType uBahnVehicleType = vehicleFactory.createVehicleType(Id.create("U-Bahn_veh_type", VehicleType.class));
		{
			VehicleCapacity capacity = uBahnVehicleType.getCapacity();
			capacity.setSeats(300);
			capacity.setStandingRoom(600);
			VehicleUtils.setDoorOperationMode(uBahnVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(uBahnVehicleType, 1.0 / 18.0); // 1s per boarding agent, distributed on 6*3 doors
			VehicleUtils.setEgressTime(uBahnVehicleType, 1.0 / 18.0); // 1s per alighting agent, distributed on 6*3 doors

			addHbefaMapping(uBahnVehicleType, HbefaVehicleCategory.NON_HBEFA_VEHICLE);
			scenario.getTransitVehicles().addVehicleType(uBahnVehicleType);

		}
		VehicleType tramVehicleType = vehicleFactory.createVehicleType(Id.create("Tram_veh_type", VehicleType.class));
		{
			VehicleCapacity capacity = tramVehicleType.getCapacity();
			capacity.setSeats(80);
			capacity.setStandingRoom(170);
			VehicleUtils.setDoorOperationMode(tramVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(tramVehicleType, 1.0 / 5.0); // 1s per boarding agent, distributed on 5 doors
			VehicleUtils.setEgressTime(tramVehicleType, 1.0 / 5.0); // 1s per alighting agent, distributed on 5 doors

			addHbefaMapping(tramVehicleType, HbefaVehicleCategory.NON_HBEFA_VEHICLE);
			scenario.getTransitVehicles().addVehicleType(tramVehicleType);
		}
		VehicleType busVehicleType = vehicleFactory.createVehicleType(Id.create("Bus_veh_type", VehicleType.class));
		{
			VehicleCapacity capacity = busVehicleType.getCapacity();
			capacity.setSeats(50);
			capacity.setStandingRoom(100);
			VehicleUtils.setDoorOperationMode(busVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(busVehicleType, 1.0 / 3.0); // 1s per boarding agent, distributed on 3 doors
			VehicleUtils.setEgressTime(busVehicleType, 1.0 / 3.0); // 1s per alighting agent, distributed on 3 doors

			addHbefaMapping(busVehicleType, HbefaVehicleCategory.URBAN_BUS);
			scenario.getTransitVehicles().addVehicleType(busVehicleType);

		}
		VehicleType ferryVehicleType = vehicleFactory.createVehicleType(Id.create("Ferry_veh_type", VehicleType.class));
		{
			VehicleCapacity capacity = ferryVehicleType.getCapacity();
			capacity.setSeats(100);
			capacity.setStandingRoom(100);
			VehicleUtils.setDoorOperationMode(ferryVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(ferryVehicleType, 1.0 / 1.0); // 1s per boarding agent, distributed on 1 door
			VehicleUtils.setEgressTime(ferryVehicleType, 1.0 / 1.0); // 1s per alighting agent, distributed on 1 door

			addHbefaMapping(ferryVehicleType, HbefaVehicleCategory.NON_HBEFA_VEHICLE);
			scenario.getTransitVehicles().addVehicleType(ferryVehicleType);
		}

		VehicleType ptVehicleType = vehicleFactory.createVehicleType(Id.create("Pt_veh_type", VehicleType.class));
		{
			VehicleCapacity capacity = ptVehicleType.getCapacity();
			capacity.setSeats(100);
			capacity.setStandingRoom(100);
			VehicleUtils.setDoorOperationMode(ptVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(ptVehicleType, 1.0 / 1.0); // 1s per boarding agent, distributed on 1 door
			VehicleUtils.setEgressTime(ptVehicleType, 1.0 / 1.0); // 1s per alighting agent, distributed on 1 door

			addHbefaMapping(ptVehicleType, HbefaVehicleCategory.NON_HBEFA_VEHICLE);
			scenario.getTransitVehicles().addVehicleType(ptVehicleType);
		}

		// set link speeds and create vehicles according to pt mode
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			VehicleType lineVehicleType;
			String stopFilter = "";

			// identify veh type / mode using gtfs route type (3-digit code, also found at the end of the line id (gtfs: route_id))
			int gtfsTransitType;
			try {
				gtfsTransitType = Integer.parseInt((String) line.getAttributes().getAttribute("gtfs_route_type"));
			} catch (NumberFormatException e) {
				log.error("unknown transit mode! Line id was " + line.getId().toString() +
					"; gtfs route type was " + line.getAttributes().getAttribute("gtfs_route_type"));
				throw new RuntimeException("unknown transit mode");
			}

			switch (gtfsTransitType) {
				// the vbb gtfs file generally uses the new gtfs route types, but some lines use the old enum in the range 0 to 7
				// see https://sites.google.com/site/gtfschanges/proposals/route-type
				// and https://developers.google.com/transit/gtfs/reference/#routestxt
				// In GTFS-VBB-20181214.zip some RE lines are wrongly attributed as type 700 (bus)!

				// freespeed are set to make sure that no transit service is delayed
				// and arrivals are as punctual (not too early) as possible
				case 2:
				case 100:
				case 101:
				case 102:
				case 103:
				case 104:
				case 105:
				case 106:
				case 107:
				case 108:
					lineVehicleType = reRbVehicleType;
					stopFilter = "station_S/U/RE/RB";
					break;
				case 109:
					// S-Bahn-Berlin is agency id 1
					lineVehicleType = sBahnVehicleType;
					stopFilter = "station_S/U/RE/RB";
					break;
				case 1:
				case 400:
				case 401:
				case 402:
				case 403:
				case 404:
				case 405:
					lineVehicleType = uBahnVehicleType;
					stopFilter = "station_S/U/RE/RB";
					break;
				case 3: // bus, same as 700
				case 700:
				case 701:
				case 702:
				case 703:
				case 704:
					// BVG is agency id 796
					lineVehicleType = busVehicleType;
					break;
				case 0:
				case 900:
				case 901:
				case 902:
				case 903:
				case 904:
				case 905:
				case 906:
					lineVehicleType = tramVehicleType;
					break;
				case 4:
				case 1000:
				case 1200:
					lineVehicleType = ferryVehicleType;
					break;
				default:
					log.warn("unknown transit mode! Line id was " + line.getId().toString() +
						"; gtfs route type was " + line.getAttributes().getAttribute("gtfs_route_type"));

					lineVehicleType = ptVehicleType;
			}

			Set<TransitRoute> toRemove = new HashSet<>();

			for (TransitRoute route : line.getRoutes().values()) {
				int routeVehId = 0; // simple counter for vehicle id _per_ TransitRoute

				// increase speed if current freespeed is lower.
				List<TransitRouteStop> routeStops = route.getStops();
				if (routeStops.size() < 2) {
					log.warn("TransitRoute with less than 2 stops found: line {}, route {}", line.getId(), route.getId());
					toRemove.add(route);
					continue;
				}

				// create vehicles for Departures
				for (Departure departure : route.getDepartures().values()) {
					Vehicle veh = vehicleFactory.createVehicle(Id.create("pt_" + route.getId().toString() + "_" + Long.toString(routeVehId++), Vehicle.class), lineVehicleType);
					scenario.getTransitVehicles().addVehicle(veh);
					departure.setVehicleId(veh.getId());
				}

				double lastDepartureOffset = route.getStops().get(0).getDepartureOffset().seconds();
				// min. time spend at a stop, useful especially for stops whose arrival and departure offset is identical,
				// so we need to add time for passengers to board and alight
				double minStopTime = 30.0;

				List<Id<Link>> routeIds = new LinkedList<>();
				routeIds.add(route.getRoute().getStartLinkId());
				routeIds.addAll(route.getRoute().getLinkIds());
				routeIds.add(route.getRoute().getEndLinkId());

				for (int i = 1; i < routeStops.size(); i++) {
					TransitRouteStop routeStop = routeStops.get(i);
					// if there is no departure offset set (or infinity), it is the last stop of the line,
					// so we don't need to care about the stop duration
					double stopDuration = routeStop.getDepartureOffset().isDefined() ?
						routeStop.getDepartureOffset().seconds() - routeStop.getArrivalOffset().seconds() : minStopTime;
					// ensure arrival at next stop early enough to allow for 30s stop duration -> time for passengers to board / alight
					// if link freespeed had been set such that the pt veh arrives exactly on time, but departure tiome is identical
					// with arrival time the pt vehicle would have been always delayed
					// Math.max to avoid negative values of travelTime
					double travelTime = Math.max(1, routeStop.getArrivalOffset().seconds() - lastDepartureOffset - 1.0 -
						(stopDuration >= minStopTime ? 0 : (minStopTime - stopDuration)));


					Id<Link> stopLink = routeStop.getStopFacility().getLinkId();
					List<Id<Link>> subRoute = new LinkedList<>();
					do {
						Id<Link> linkId = routeIds.removeFirst();
						subRoute.add(linkId);
					} while (!subRoute.contains(stopLink));

					List<? extends Link> links = subRoute.stream().map(scenario.getNetwork().getLinks()::get)
						.toList();

					double length = links.stream().mapToDouble(Link::getLength).sum();

					for (Link link : links) {
						increaseLinkFreespeedIfLower(link, length / travelTime);
					}
					lastDepartureOffset = routeStop.getDepartureOffset().seconds();
				}

				// tag RE, RB, S- and U-Bahn stations for Drt stop filter attribute
				if (!stopFilter.isEmpty()) {
					for (TransitRouteStop routeStop : route.getStops()) {
						routeStop.getStopFacility().getAttributes().putAttribute("stopFilter", stopFilter);
					}
				}
			}

			toRemove.forEach(line::removeRoute);
		}

		return scenario;
	}

	public enum PseudoNetwork {
		/**
		 * Create links between all stops and possibly duplicate stops.
		 */
		singleLinkBetweenStops,
		/**
		 * Create a pseudo network with loop links at each stop.
		 */
		withLoopLinks,
	}

}
