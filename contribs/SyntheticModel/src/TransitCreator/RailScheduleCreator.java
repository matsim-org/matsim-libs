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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RailScheduleCreator {

		private final Scenario scenario;

		// Constructor
		public RailScheduleCreator(Scenario scenario) {
			this.scenario = scenario;
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

	public void createSchedule(List<Id<Link>> linkIds, List<Id<Link>> LinkIds_r, String[] times, String[] vehicleRefIds) {
		// Ensure the necessary factories and other objects are available
		TransitScheduleFactory scheduleFactory = scenario.getTransitSchedule().getFactory();

		Id<Link> startLinkId = linkIds.get(0);
		Id<Link> endLinkId = linkIds.get(linkIds.size() - 1);

		Id<Link> StartLinkId_r = LinkIds_r.get(0);
		Id<Link> EndLinkId_r = LinkIds_r.get(LinkIds_r.size() - 1);

		NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(startLinkId, linkIds, endLinkId);
		NetworkRoute networkRoute_r = RouteUtils.createLinkNetworkRouteImpl(StartLinkId_r, LinkIds_r, EndLinkId_r);

		StopfacilityCreator(scheduleFactory, linkIds);

		StopfacilityCreator(scheduleFactory, LinkIds_r);

		List<TransitStopFacility> stopFacilities = new ArrayList<>(scenario.getTransitSchedule().getFacilities().values());
		List<TransitRouteStop> transitRouteStops = createTransitRouteStopsForFacilities(scheduleFactory, stopFacilities);
		List<TransitStopFacility> stopFacilities_r = new ArrayList<>(scenario.getTransitSchedule().getFacilities().values());
		List<TransitRouteStop> transitRouteStops_r = createTransitRouteStopsForFacilities(scheduleFactory, stopFacilities_r);

		TransitRoute transitRoute = scheduleFactory.createTransitRoute(Id.create("Suburbs", TransitRoute.class), null, transitRouteStops, "train");
		TransitRoute transitRoute_r = scheduleFactory.createTransitRoute(Id.create("Centre", TransitRoute.class), null, transitRouteStops_r, "train");



		DepartureCreator(scheduleFactory, transitRoute, times, vehicleRefIds);

		vehicleRefIds = Arrays.stream(vehicleRefIds).map(s -> s.equals("tr_2") ? "tr_1" : s).toArray(String[]::new);
		DepartureCreator(scheduleFactory, transitRoute_r, times, vehicleRefIds);

		TransitLine transitLine = scheduleFactory.createTransitLine(Id.create("Shuttle", TransitLine.class));
			transitLine.addRoute(transitRoute);
			transitLine.addRoute(transitRoute_r);

		scenario.getTransitSchedule().addTransitLine(transitLine);


	}

	private void StopfacilityCreator(TransitScheduleFactory scheduleFactory, List<Id<Link>> linkIds) {
		for (Id<Link> linkId : linkIds) {
			Link link = scenario.getNetwork().getLinks().get(linkId);
			Coord stopCoord = link.getFromNode().getCoord();
			Id<TransitStopFacility> stopId = Id.create("stop_" + linkId.toString(), TransitStopFacility.class);
			TransitStopFacility stopFacility = scheduleFactory.createTransitStopFacility(stopId, stopCoord, false);
			stopFacility.setLinkId(linkId); // Associate the stop with the link
			// Add the stop facility to the scenario's transit schedule
			scenario.getTransitSchedule().addStopFacility(stopFacility);
		}
	}

	private void DepartureCreator(TransitScheduleFactory scheduleFactory, TransitRoute transitRoute_r, String[] times, String[] vehicleRefIds) {
		for (int i = 0; i < times.length; i++) {
			Id<Departure> departureId = Id.create("departure_" + (i+1), Departure.class);
			double departureTime = Time.parseTime(times[i]);
			Id<Vehicle> vehicleId = Id.create(vehicleRefIds[i], Vehicle.class);
			Departure departure = scheduleFactory.createDeparture(departureId, departureTime);
			departure.setVehicleId(vehicleId);
			transitRoute_r.addDeparture(departure);
		}
	}

	public static void main(String[] args) {
		String configFilePath = "C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\config19.xml";
		Config config = ConfigUtils.loadConfig(configFilePath);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Import link network route
		List<Id<Link>> linkIds = Arrays.asList(
			Id.createLinkId("link-id_100.0_400.0"),    // length = 300
			Id.createLinkId("link-id_400.0_733.0"),    // length = 333
			Id.createLinkId("link-id_733.0_1083.0"),   // length = 350
			Id.createLinkId("link-id_1083.0_1483.0"),  // length = 400
			Id.createLinkId("link-id_1483.0_1893.0"),  // length = 410
			Id.createLinkId("link-id_1893.0_2343.0"),  // length = 450
			Id.createLinkId("link-id_2343.0_3343.0"),  // length = 1000
			Id.createLinkId("link-id_3343.0_4443.0"),  // length = 1100
			Id.createLinkId("link-id_4443.0_5943.0"),  // length = 1500
			Id.createLinkId("link-id_5943.0_7943.0")   // length = 2000
		);
// Reverse route LinkIds_r in descending order of length
		List<Id<Link>> LinkIds_r = Arrays.asList(
			Id.createLinkId("link-id_5943.0_7943.0_r"),   // length = 2000
			Id.createLinkId("link-id_4443.0_5943.0_r"),   // length = 1500
			Id.createLinkId("link-id_3343.0_4443.0_r"),   // length = 1100
			Id.createLinkId("link-id_2343.0_3343.0_r"),   // length = 1000
			Id.createLinkId("link-id_1893.0_2343.0_r"),   // length = 450
			Id.createLinkId("link-id_1483.0_1893.0_r"),   // length = 410
			Id.createLinkId("link-id_1083.0_1483.0_r"),   // length = 400
			Id.createLinkId("link-id_733.0_1083.0_r"),    // length = 350
			Id.createLinkId("link-id_400.0_733.0_r"),     // length = 333
			Id.createLinkId("link-id_100.0_400.0_r")      // length = 300
		);

		String[] times = {
			"06:00:00", "06:15:00", "06:30:00", "06:40:00", "06:50:00", "07:00:00",
			"07:10:00", "07:20:00", "07:30:00", "07:40:00", "07:50:00", "08:00:00",
			"08:15:00", "08:30:00", "09:00:00", "09:30:00", "10:00:00", "10:30:00",
			"11:00:00", "12:00:00", "13:00:00", "14:00:00", "15:00:00", "15:30:00",
			"16:00:00", "16:15:00", "16:30:00", "16:40:00", "16:50:00", "17:00:00",
			"17:10:00", "17:20:00", "17:30:00", "17:40:00", "17:50:00", "18:00:00",
			"18:10:00", "18:20:00", "18:30:00", "18:40:00", "18:50:00", "19:00:00",
			"19:15:00", "19:30:00", "19:45:00", "20:00:00", "20:30:00", "21:00:00",
			"22:00:00", "23:00:00"
		};

		String[] vehicleRefIds = {
			"tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1",
			"tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1",
			"tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1",
			"tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1",
			"tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1", "tr_2", "tr_1"
		};

			RailScheduleCreator creator = new RailScheduleCreator(scenario);
			creator.createSchedule(linkIds, LinkIds_r, times, vehicleRefIds);
			new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\transitSchedule.xml");

	}
	}
