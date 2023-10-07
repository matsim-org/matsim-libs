package TransitCreator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.io.File;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RailScheduleCreator {

	private final Scenario scenario;

		// Constructor
	public RailScheduleCreator(Scenario scenario) {
		this.scenario = scenario;
		}


	public void createSchedule(List<Id<Link>> linkIds, List<Id<Link>> LinkIds_r, String[] times, String[] vehicleRefIds) {
		// Access the schedule factory to create and modify transit components
		TransitScheduleFactory scheduleFactory = scenario.getTransitSchedule().getFactory();

		// Determine the first and last link IDs from the provided lists
		Id<Link> startLinkId = linkIds.get(0);
		Id<Link> endLinkId = linkIds.get(linkIds.size() - 1);
		Id<Link> StartLinkId_r = LinkIds_r.get(0);
		Id<Link> EndLinkId_r = LinkIds_r.get(LinkIds_r.size() - 1);

		// Create a network route from the provided link lists
		NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(startLinkId, linkIds.subList(1, linkIds.size()-1), endLinkId);
		NetworkRoute networkRoute_r = RouteUtils.createLinkNetworkRouteImpl(StartLinkId_r, LinkIds_r.subList(1, LinkIds_r.size()-1), EndLinkId_r);

		// Create stop facilities for both forward and reverse routes
		List<TransitStopFacility> stopFacilities = StopfacilityCreator(scheduleFactory, linkIds);
		List<TransitRouteStop> transitRouteStops = createTransitRouteStopsForFacilities(scheduleFactory, stopFacilities);

		List<TransitStopFacility> stopFacilities_r = StopfacilityCreator(scheduleFactory, LinkIds_r);
		List<TransitRouteStop> transitRouteStops_r = createTransitRouteStopsForFacilities(scheduleFactory, stopFacilities_r);

		// Create transit routes using the stop facilities and network routes
		TransitRoute transitRoute = scheduleFactory.createTransitRoute(Id.create("Suburbs", TransitRoute.class), networkRoute, transitRouteStops, "pt");
		TransitRoute transitRoute_r = scheduleFactory.createTransitRoute(Id.create("Centre", TransitRoute.class), networkRoute_r, transitRouteStops_r, "pt");

		// Populate the departures for the created routes
		DepartureCreator(scheduleFactory, transitRoute, times, vehicleRefIds);

		// Modify vehicle references and then populate departures for the reverse route
		vehicleRefIds = Arrays.stream(vehicleRefIds).map(s -> s.equals("tr_2") ? "tr_1" : s.equals("tr_1") ? "tr_2" : s).toArray(String[]::new);
		DepartureCreator(scheduleFactory, transitRoute_r, times, vehicleRefIds);

		// Group the routes under a transit line and add them to the scenario
		TransitLine transitLine = scheduleFactory.createTransitLine(Id.create("Shuttle", TransitLine.class));
		transitLine.addRoute(transitRoute);
		transitLine.addRoute(transitRoute_r);

		scenario.getTransitSchedule().addTransitLine(transitLine);


	}


	private List<TransitStopFacility> StopfacilityCreator(TransitScheduleFactory scheduleFactory, List<Id<Link>> linkIds) {
		List<TransitStopFacility> createdStopFacilities = new ArrayList<>();

		for (Id<Link> linkId : linkIds) {
			Link link = scenario.getNetwork().getLinks().get(linkId);
			Coord stopCoord = link.getFromNode().getCoord();
			Id<TransitStopFacility> stopId = Id.create("stop_" + linkId.toString(), TransitStopFacility.class);
			TransitStopFacility stopFacility = scheduleFactory.createTransitStopFacility(stopId, stopCoord, false);
			stopFacility.setLinkId(linkId); // Associate the stop with the link
			// Add the stop facility to the scenario's transit schedule
			scenario.getTransitSchedule().addStopFacility(stopFacility);
			createdStopFacilities.add(stopFacility);
		}
		return createdStopFacilities;
	}

	public List<TransitRouteStop> createTransitRouteStopsForFacilities(TransitScheduleFactory scheduleFactory, List<TransitStopFacility> stopFacilities) {
		List<TransitRouteStop> transitRouteStops = new ArrayList<>();

		// Assuming a fixed dwell time of 0 seconds at each stop and the same arrival and departure offsets for simplicity
		for (TransitStopFacility stopFacility : stopFacilities) {
			TransitRouteStop transitRouteStop = scheduleFactory.createTransitRouteStop(stopFacility, 0, 0);
			transitRouteStops.add(transitRouteStop);
		}

		return transitRouteStops;
	}

	private void DepartureCreator(TransitScheduleFactory scheduleFactory, TransitRoute transitRoute_r, String[] times, String[] vehicleRefIds) {
		for (int i = 0; i < times.length; i++) {
			Id<Departure> departureId = Id.create("d_" + (i+1), Departure.class);
			double departureTime = Time.parseTime(times[i]);
			Id<Vehicle> vehicleId = Id.create(vehicleRefIds[i], Vehicle.class);
			Departure departure = scheduleFactory.createDeparture(departureId, departureTime);
			departure.setVehicleId(vehicleId);
			transitRoute_r.addDeparture(departure);
		}
	}

	public static void main(String[] args) {
		// Initialise scenario
		String configFilePath = "C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\40kmx1km\\config07.xml";
		Config config = ConfigUtils.loadConfig(configFilePath);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Use RailLinkCreator to generate the link IDs
		RailLinkCreator linkCreator = new RailLinkCreator();
		linkCreator.generateLinks();
		List<Id<Link>> linkIds = linkCreator.getGeneratedLinkIds();
		List<Id<Link>> linkIds_r = new ArrayList<>(linkIds);
		Collections.reverse(linkIds_r);
		linkIds_r = linkIds_r.stream().map(id -> Id.createLinkId(id.toString() + "_r")).collect(Collectors.toList());

		String[] times = {
			// 5 AM to 6 AM: Early morning, every 15 minutes
			"05:00:00", "05:15:00", "05:30:00", "05:45:00",

			// 6 AM to 7:30 AM: Morning rush, every 5 minutes
			"06:00:00", "06:05:00", "06:10:00", "06:15:00", "06:20:00", "06:25:00", "06:30:00",
			"06:35:00", "06:40:00", "06:45:00", "06:50:00", "06:55:00", "07:00:00", "07:05:00",
			"07:10:00", "07:15:00", "07:20:00", "07:25:00", "07:30:00",

			// 7:30 AM to 9 AM: Peak rush, every 3-4 minutes
			"07:33:00", "07:37:00", "07:40:00", "07:44:00", "07:48:00", "07:52:00", "07:56:00",
			"08:00:00", "08:04:00", "08:08:00", "08:12:00", "08:16:00", "08:20:00", "08:24:00",
			"08:28:00", "08:32:00", "08:36:00", "08:40:00", "08:44:00", "08:48:00", "08:52:00",
			"08:56:00",

			// 9 AM to 4 PM: Off-peak, every 10 minutes
			"09:00:00", "09:10:00", "09:20:00", "09:30:00", "09:40:00", "09:50:00",
			"10:00:00", "10:10:00", "10:20:00", "10:30:00", "10:40:00", "10:50:00",
			"11:00:00", "11:10:00", "11:20:00", "11:30:00", "11:40:00", "11:50:00",
			"12:00:00", "12:10:00", "12:20:00", "12:30:00", "12:40:00", "12:50:00",
			"13:00:00", "13:10:00", "13:20:00", "13:30:00", "13:40:00", "13:50:00",
			"14:00:00", "14:10:00", "14:20:00", "14:30:00", "14:40:00", "14:50:00",
			"15:00:00", "15:10:00", "15:20:00", "15:30:00", "15:40:00", "15:50:00",

			// 4 PM to 7 PM: Evening rush, every 5 minutes
			"16:00:00", "16:05:00", "16:10:00", "16:15:00", "16:20:00", "16:25:00",
			"16:30:00", "16:35:00", "16:40:00", "16:45:00", "16:50:00", "16:55:00",
			"17:00:00", "17:05:00", "17:10:00", "17:15:00", "17:20:00", "17:25:00",
			"17:30:00", "17:35:00", "17:40:00", "17:45:00", "17:50:00", "17:55:00",

			// 7 PM to 10 PM: Evening, every 10 minutes
			"18:00:00", "18:10:00", "18:20:00", "18:30:00", "18:40:00", "18:50:00",
			"19:00:00", "19:10:00", "19:20:00", "19:30:00", "19:40:00", "19:50:00",
			"20:00:00", "20:10:00", "20:20:00", "20:30:00", "20:40:00", "20:50:00",
			"21:00:00", "21:10:00", "21:20:00", "21:30:00", "21:40:00", "21:50:00",

			// 10 PM to 12 AM: Late evening, every 15 minutes
			"22:00:00", "22:15:00", "22:30:00", "22:45:00", "23:00:00", "23:15:00", "23:30:00", "23:45:00",
		};


		String[] vehicleRefIds = new String[times.length];
		for (int i = 0; i < times.length; i++) {
			vehicleRefIds[i] = "tr_" + ((i % 25) + 1);  // rotates between tr_1, tr_2, .... tr_25
		}


			RailScheduleCreator creator = new RailScheduleCreator(scenario);
			creator.createSchedule(linkIds, linkIds_r, times, vehicleRefIds);
			new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\40kmx1km\\transitSchedule.xml");

	}
	}
