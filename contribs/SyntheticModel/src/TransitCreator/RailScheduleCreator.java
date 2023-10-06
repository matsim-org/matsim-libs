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
import java.util.List;

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
		String configFilePath = "C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\config05.xml";
		Config config = ConfigUtils.loadConfig(configFilePath);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Import link network route
		List<Id<Link>> linkIds = Arrays.asList(
			Id.createLinkId("link-id_99.0_100.0"),    // length = 1
			Id.createLinkId("link-id_100.0_400.0"),    // length = 300
			Id.createLinkId("link-id_400.0_733.0"),    // length = 333
			Id.createLinkId("link-id_733.0_1083.0"),   // length = 350
			Id.createLinkId("link-id_1083.0_1483.0"),  // length = 400
			Id.createLinkId("link-id_1483.0_1893.0"),  // length = 410
			Id.createLinkId("link-id_1893.0_2343.0"),  // length = 450
			Id.createLinkId("link-id_2343.0_3343.0"),  // length = 1000
			Id.createLinkId("link-id_3343.0_4443.0"),  // length = 1100
			Id.createLinkId("link-id_4443.0_5943.0"),  // length = 1500
			Id.createLinkId("link-id_5943.0_7943.0"),   // length = 2000
			Id.createLinkId("link-id_7943.0_7944.0")   // length = 1

		);
		// Reverse route LinkIds_r in descending order of length
		List<Id<Link>> LinkIds_r = Arrays.asList(
			Id.createLinkId("link-id_7943.0_7944.0_r"),   // length = 1
			Id.createLinkId("link-id_5943.0_7943.0_r"),   // length = 2000
			Id.createLinkId("link-id_4443.0_5943.0_r"),   // length = 1500
			Id.createLinkId("link-id_3343.0_4443.0_r"),   // length = 1100
			Id.createLinkId("link-id_2343.0_3343.0_r"),   // length = 1000
			Id.createLinkId("link-id_1893.0_2343.0_r"),   // length = 450
			Id.createLinkId("link-id_1483.0_1893.0_r"),   // length = 410
			Id.createLinkId("link-id_1083.0_1483.0_r"),   // length = 400
			Id.createLinkId("link-id_733.0_1083.0_r"),    // length = 350
			Id.createLinkId("link-id_400.0_733.0_r"),     // length = 333
			Id.createLinkId("link-id_100.0_400.0_r"),      // length = 300
			Id.createLinkId("link-id_99.0_100.0_r")   // length = 1
		);

		String[] times = {
			// 6 AM to 6:45 AM: every 10 minutes
			"06:00:00", "06:10:00", "06:20:00", "06:30:00", "06:40:00",

			// 6:45 AM to 7:30 AM: every 7 minutes
			"06:45:00", "06:52:00", "06:59:00", "07:06:00", "07:13:00", "07:20:00", "07:27:00",

			// 7:30 AM to 8:15 AM: every 10 minutes
			"07:30:00", "07:40:00", "07:50:00", "08:00:00", "08:10:00", "08:20:00",

			// 8:15 AM to 4:15 PM: every 15 minutes
			"08:30:00", "08:45:00", "09:00:00", "09:15:00", "09:30:00", "09:45:00", "10:00:00", "10:15:00", "10:30:00",
			"10:45:00", "11:00:00", "11:15:00", "11:30:00", "11:45:00", "12:00:00", "12:15:00", "12:30:00", "12:45:00",
			"13:00:00", "13:15:00", "13:30:00", "13:45:00", "14:00:00", "14:15:00", "14:30:00", "14:45:00", "15:00:00",
			"15:15:00", "15:30:00", "15:45:00", "16:00:00",

			// 4:15 PM to 5 PM: every 10 minutes
			"16:10:00", "16:20:00", "16:30:00", "16:40:00", "16:50:00",

			// 5 PM to 6 PM: every 7 minutes
			"17:00:00", "17:07:00", "17:14:00", "17:21:00", "17:28:00", "17:35:00", "17:42:00", "17:49:00", "17:56:00",

			// 6 PM to 7 PM: every 10 minutes
			"18:00:00", "18:10:00", "18:20:00", "18:30:00", "18:40:00", "18:50:00",

			// 7 PM to 12 AM: every 15 minutes
			"19:00:00", "19:15:00", "19:30:00", "19:45:00", "20:00:00", "20:15:00", "20:30:00", "20:45:00", "21:00:00",
			"21:15:00", "21:30:00", "21:45:00", "22:00:00", "22:15:00", "22:30:00", "22:45:00", "23:00:00", "23:15:00"
		};

		String[] vehicleRefIds = new String[times.length];
		for (int i = 0; i < times.length; i++) {
			if (LocalTime.parse(times[i]).isAfter(LocalTime.of(8, 15)) && LocalTime.parse(times[i]).isBefore(LocalTime.of(17, 0))) {
				vehicleRefIds[i] = "tr_" + ((i % 4) + 1); // rotates between tr_1, tr_2, tr_3, tr_4
			} else {
				vehicleRefIds[i] = (i % 2 == 0) ? "tr_1" : "tr_2"; // alternates between tr_1 and tr_2
			}
		}

			RailScheduleCreator creator = new RailScheduleCreator(scenario);
			creator.createSchedule(linkIds, LinkIds_r, times, vehicleRefIds);
			new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\transitSchedule.xml");

	}
	}
