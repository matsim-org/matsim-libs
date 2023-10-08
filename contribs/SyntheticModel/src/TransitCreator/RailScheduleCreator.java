package TransitCreator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.*;

public class RailScheduleCreator {




	public void generateSchedule(String scenarioPath, String[] times, String[] vehicleRefIds, double[] distances) {

		// Initialise scenario

		// Add Vehicles, rail specification
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Id<VehicleType> typeId = Id.create("rail",  VehicleType.class);
		VehicleType railtype = scenario.getVehicles().getFactory().createVehicleType(typeId);
		railtype.setNetworkMode(TransportMode.pt); // set rail vehicles to pt
		railtype.getCapacity().setSeats(333); // set seating and standing room
		railtype.getCapacity().setStandingRoom(667);
		railtype.setLength(36);

		new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(scenarioPath + "/transitVehicles.xml");

		// Add Network Elements
		List<Id<Link>> linkIds = new ArrayList<>();
		List<Id<Link>> linkIds_r = new ArrayList<>();
		Network network = NetworkUtils.readNetwork(scenarioPath + "/network.xml");
		Coord coordStart = new Coord(100, 500);
		Coord initialExtraCoord = new Coord(coordStart.getX() - 1, coordStart.getY());
		// Create the starting node to act as a station for turn around
		Node InitialStation = network.getFactory().createNode(Id.createNodeId("station" + initialExtraCoord.getX()), initialExtraCoord);
		network.addNode(InitialStation);  // Add this node to the network
		Node previousNode = network.getFactory().createNode(Id.createNodeId("station" + coordStart.getX()), coordStart);
		network.addNode(previousNode);  // Add this node to the network
		createRailLink(network, InitialStation, previousNode,  linkIds, linkIds_r);

		// Iterate to create the main nodes and links
        for (double distance : distances) {
            // Calculate the end coordinate for the current link
            Coord coordEnd = new Coord(coordStart.getX() + distance, coordStart.getY());
            String nodeId = "station" + coordEnd.getX();
            System.out.println("Creating node with ID: " + nodeId + " and Coordinates: (" + coordEnd.getX() + ", " + coordEnd.getY() + ")");
            Node currentNode = network.getFactory().createNode(Id.createNodeId(nodeId), coordEnd);
            network.addNode(currentNode);
            createRailLink(network, previousNode, currentNode, linkIds, linkIds_r);  // Call RailLink to create forward and reverse link
			previousNode = currentNode;
            coordStart = coordEnd;
        }

		// Create a final extra node to the right of the last coordEnd
		Coord finalExtraCoord = new Coord(coordStart.getX() + 1, coordStart.getY());
		Node finalExtraNode = network.getFactory().createNode(Id.createNodeId("station" + finalExtraCoord.getX()), finalExtraCoord);
		network.addNode(finalExtraNode);
		createRailLink(network, previousNode, finalExtraNode, linkIds, linkIds_r);

		// Write the modified network back to the XML file
		new NetworkWriter(network).write(scenarioPath + "/network_pt.xml");

		Config config = ConfigUtils.loadConfig(scenarioPath + "/config.xml");
		scenario = ScenarioUtils.loadScenario(config);

		// Access the schedule factory to create and modify transit components
		TransitScheduleFactory scheduleFactory = scenario.getTransitSchedule().getFactory();

		// Determine the first and last link IDs from the provided lists
		Id<Link> startLinkId = linkIds.get(0);
		Id<Link> endLinkId = linkIds.get(linkIds.size() - 1);
		Id<Link> StartLinkId_r = linkIds_r.get(0);
		Id<Link> EndLinkId_r = linkIds_r.get(linkIds_r.size() - 1);
		System.out.println("Total links: " + linkIds.size());
		System.out.println("Total links: " + linkIds_r.size());
		List<Id<Link>>  IntlinkIds = linkIds.subList(1, linkIds.size()-1);
		List<Id<Link>>  IntlinkIds_r = linkIds.subList(1, linkIds.size()-1);

		// Create a network route from the provided link lists
		NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(startLinkId, linkIds, endLinkId);
		NetworkRoute networkRoute_r = RouteUtils.createLinkNetworkRouteImpl(StartLinkId_r, linkIds_r, EndLinkId_r);
		System.out.println("Total links: " +  scenario.getNetwork().getLinks().get("402623_402624"));


		System.out.println("Total links: " +  scenario.getNetwork().getLinks().get("402623_402624"));


		// Create stop facilities for both forward and reverse routes
		List<TransitStopFacility> stopFacilities = StopfacilityCreator(scenario,scheduleFactory, linkIds);
		System.out.println("Total stop facilities: " + scenario.getTransitSchedule().getFacilities().size());
		List<TransitRouteStop> transitRouteStops = createTransitRouteStopsForFacilities(scenario,scheduleFactory, stopFacilities);

		List<TransitStopFacility> stopFacilities_r = StopfacilityCreator(scenario,scheduleFactory, linkIds_r);
		List<TransitRouteStop> transitRouteStops_r = createTransitRouteStopsForFacilities(scenario, scheduleFactory, stopFacilities_r);

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

		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(scenarioPath + "/transitschedule.xml");

	}

	public void createRailLink(Network network, Node fromNode, Node toNode, List<Id<Link>> linkIds, List<Id<Link>> linkIds_r) {
		// Create the original link
		Link link = network.getFactory().createLink(Id.createLinkId("link-id_" + fromNode.getId() + "_" + toNode.getId()), fromNode, toNode);
		Set<String> allowedModes = new HashSet<>();
		allowedModes.add("pt");
		link.setAllowedModes(allowedModes);
		link.setCapacity(100);
		link.setFreespeed(12);
		network.addLink(link);
		linkIds.add(link.getId()); // Add the link ID to linkIds list

		// Create the reverse link
		Link reverseLink = network.getFactory().createLink(Id.createLinkId("link-id_" + fromNode.getId() + "_" + toNode.getId() + "_r"), toNode, fromNode);
		reverseLink.setAllowedModes(allowedModes);
		reverseLink.setCapacity(100);
		reverseLink.setFreespeed(12);
		network.addLink(reverseLink);
		linkIds_r.add(reverseLink.getId()); // Add the reverse link ID to linkIds_r list

		System.out.println("Created Link: " + link.getId());
	}

	public  List<TransitStopFacility> StopfacilityCreator(Scenario scenario, TransitScheduleFactory scheduleFactory, List<Id<Link>> linkIds) {
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
			System.out.println("Adding stop facility for link ID: " + linkId);
			System.out.println("Total stop facilities: " + scenario.getTransitSchedule().getFacilities().size());

		}
		return createdStopFacilities;
	}

	public List<TransitRouteStop> createTransitRouteStopsForFacilities(Scenario scenario, TransitScheduleFactory scheduleFactory, List<TransitStopFacility> stopFacilities) {
		List<TransitRouteStop> transitRouteStops = new ArrayList<>();

		// Assuming a fixed dwell time of 0 seconds at each stop and the same arrival and departure offsets for simplicity
		for (TransitStopFacility stopFacility : stopFacilities) {
			TransitRouteStop transitRouteStop = scheduleFactory.createTransitRouteStop(stopFacility, 0, 0);
			transitRouteStops.add(transitRouteStop);
		}
		System.out.println("Total stop facilities: " + scenario.getTransitSchedule().getFacilities().size());
		return transitRouteStops;
	}

	private void DepartureCreator(TransitScheduleFactory scheduleFactory, TransitRoute transitRoute_r, String[] times, String[] vehicleRefIds) {
		for (int i = 0; i < times.length; i++) {
			Id<Departure> departureId = Id.create("d_" + (i + 1), Departure.class);
			double departureTime = Time.parseTime(times[i]);
			Id<Vehicle> vehicleId = Id.create(vehicleRefIds[i], Vehicle.class);
			Departure departure = scheduleFactory.createDeparture(departureId, departureTime);
			departure.setVehicleId(vehicleId);
			transitRoute_r.addDeparture(departure);
		}
	}

	public static class DepTimeGenerator {
		public static String[] generateTimes(int normalInterval, int rushHourMorningInterval, int rushHourEveningInterval) {
			List<String> timesList = new ArrayList<>();
			// Define the time ranges for rush hours (for simplicity)
			int rushHourMorningStart = 6; // 6 AM
			int rushHourMorningEnd = 9;   // 9 AM
			int rushHourEveningStart = 16; // 4 PM
			int rushHourEveningEnd = 19;   // 7 PM
			for (int hour = 5; hour < 24; hour++) {
				int interval = normalInterval;
				if (hour >= rushHourMorningStart && hour < rushHourMorningEnd) {
					interval = rushHourMorningInterval;
				} else if (hour >= rushHourEveningStart && hour < rushHourEveningEnd) {
					interval = rushHourEveningInterval;
				}

				for (int minute = 0; minute < 60; minute += interval) {
					timesList.add(String.format("%02d:%02d:00", hour, minute));
				}
			}

			return timesList.toArray(new String[0]);
		}



		public static void main(String[] args) {
			// Initialise scenario
			String scenarioPath = "examples/scenarios/UrbanLine/20kmx1km";
			String configFilePath = "examples/scenarios/UrbanLine/20kmx1km/config.xml";
			Config config = ConfigUtils.loadConfig(configFilePath);
			Scenario scenario = ScenarioUtils.loadScenario(config);

			double[] distances = {
				// First half: links from 700 to 1500
				700.0, 742.0, 868.0, 910.0, 952.0,
				1036.0, 1246.0,
				1414.0, 1456.0,
				// Next quarter: links from 1500 to 3000
				1666.0, 1998.0, 2330.0, 2994.0,
				//  last bit: 10 links from 2500 to 3500
				2765.0, 2895.0   };

			String[] times = generateTimes(15, 7, 10);

			String[] vehicleRefIds = new String[times.length];
			for (int i = 0; i < times.length; i++) {
				vehicleRefIds[i] = "tr_" + ((i % 25) + 1);  // rotates between tr_1, tr_2, .... tr_25
			}

			RailScheduleCreator creator = new RailScheduleCreator();
			creator.generateSchedule(scenarioPath, times, vehicleRefIds, distances);

		}
	}
}
