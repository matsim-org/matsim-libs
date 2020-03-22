/**
 * 
 */
package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.TripsAndLegsCSVWriter.NoLegsWriterExtension;
import org.matsim.analysis.TripsAndLegsCSVWriter.NoTripWriterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Aravind
 *
 */
public class TripsAndLegsCSVWriterTest {

	Config config = ConfigUtils.createConfig();
	Scenario scenario = ScenarioUtils.createScenario(config);

	private static int dep_time;
	private static int trav_time;
	private static int traveled_distance;
	private static int euclidean_distance;
	private static int longest_distance_mode;
	private static int modes;
	private static int start_activity_type;
	private static int end_activity_type;
	private static int start_facility_id;
	private static int start_link;
	private static int start_x;
	private static int start_y;
	private static int end_facility_id;
	private static int end_link;
	private static int end_x;
	private static int end_y;
	private static int trip_id;

	final Id<Link> link1 = Id.create(10723, Link.class);
	final Id<Link> link2 = Id.create(123160, Link.class);
	final Id<Link> link3 = Id.create(130181, Link.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testTripsAndLegsCSVWriter() {
		final IdMap<Person, Plan> map = new IdMap<>(Person.class);
		/************************************
		 * Person - creating person 1
		 ************************************/
		Person person1 = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		final Plan person1_plan1 = PopulationUtils.createPlan(person1);

		Map<String, Object> persontrips = new HashMap<String, Object>();
		Map<String, Object> tour1 = new HashMap<String, Object>();

		Activity act1_1_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		act1_1_1.setStartTime(21600.0);
		act1_1_1.setEndTime(21900.0);
		act1_1_1.setLinkId(link1);
		act1_1_1.setCoord(CoordUtils.createCoord(11.0, 22.0));
		act1_1_1.setFacilityId(Id.create("id1", ActivityFacility.class));
		person1_plan1.addActivity(act1_1_1);
		Leg leg1_1_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route1_1_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route1_1_1.setDistance(100);
		leg1_1_1.setRoute(route1_1_1);
		leg1_1_1.setTravelTime(300.0);
		person1_plan1.addLeg(leg1_1_1);
		// ****************************************************************************************
		Activity act1_1_2 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		act1_1_2.setStartTime(22200.0);
		act1_1_2.setEndTime(22200.0);
		act1_1_2.setLinkId(link1);
		act1_1_2.setCoord(CoordUtils.createCoord(10.0, 20.0));
		act1_1_2.setFacilityId(Id.create("id2", ActivityFacility.class));
		person1_plan1.addActivity(act1_1_2);
		Leg leg1_1_2 = PopulationUtils.createLeg(TransportMode.car);
		Route route1_1_2 = RouteUtils.createGenericRouteImpl(link1, link2);
		route1_1_2.setDistance(5000);
		leg1_1_2.setRoute(route1_1_2);
		leg1_1_2.setTravelTime(1800.0);
		person1_plan1.addLeg(leg1_1_2);
		// ***************************************************************************************
		Activity act1_1_3 = PopulationUtils.createActivityFromLinkId("car interaction", link3);
		act1_1_3.setStartTime(24000.0);
		act1_1_3.setEndTime(24000.0);
		act1_1_3.setLinkId(link3);
		act1_1_3.setCoord(CoordUtils.createCoord(111.0, 213.0));
		act1_1_3.setFacilityId(Id.create("id3", ActivityFacility.class));
		person1_plan1.addActivity(act1_1_3);
		Leg leg1_1_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route1_1_3 = RouteUtils.createGenericRouteImpl(link3, link3);
		route1_1_3.setDistance(300);
		leg1_1_3.setRoute(route1_1_3);
		leg1_1_3.setTravelTime(900.0);
		person1_plan1.addLeg(leg1_1_3);
		// **********************Trip
		// ends***********************************************************
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

		tour1.put("dep_time", LocalTime.of(21900 / 3600, (21900 % 3600) / 60, (21900 % 3600) % 60).format(dtf));
		tour1.put("trav_time", LocalTime.of(3000 / 3600, (3000 % 3600) / 60, (3000 % 3600) % 60).format(dtf));
		tour1.put("traveled_distance", "5400");
		tour1.put("euclidean_distance",
				(int) CoordUtils.calcEuclideanDistance(act1_1_1.getCoord(), act1_1_3.getCoord()));
		tour1.put("longest_distance_mode", new String(TransportMode.car));
		tour1.put("modes", new String(TransportMode.walk + "-" + TransportMode.car + "-" + TransportMode.walk));
		tour1.put("start_activity_type", "home");
		tour1.put("end_activity_type", "work");
		tour1.put("start_facility_id", String.valueOf(act1_1_1.getFacilityId()));
		tour1.put("start_link", String.valueOf(act1_1_1.getLinkId()));
		tour1.put("start_x", String.valueOf(act1_1_1.getCoord().getX()));
		tour1.put("start_y", String.valueOf(act1_1_1.getCoord().getY()));

		// ***************************************************************************************
		Map<String, Object> tour2 = new HashMap<String, Object>();

		Activity act1_1_4 = PopulationUtils.createActivityFromLinkId("work", link3);
		act1_1_4.setStartTime(24900.0);
		act1_1_4.setEndTime(46500.0);
		act1_1_4.setLinkId(link3);
		act1_1_4.setCoord(CoordUtils.createCoord(111.0, 213.0));
		act1_1_4.setFacilityId(Id.create("id4", ActivityFacility.class));
		person1_plan1.addActivity(act1_1_4);
		Leg leg1_1_4 = PopulationUtils.createLeg(TransportMode.walk);
		Route route1_1_4 = RouteUtils.createGenericRouteImpl(link3, link3);
		route1_1_4.setDistance(300);
		leg1_1_4.setTravelTime(900.0);
		leg1_1_4.setRoute(route1_1_4);
		person1_plan1.addLeg(leg1_1_4);
		// **************************************************************************************
		tour1.put("end_facility_id", String.valueOf(act1_1_4.getFacilityId()));
		tour1.put("end_link", String.valueOf(act1_1_4.getLinkId()));
		tour1.put("end_x", String.valueOf(act1_1_4.getCoord().getX()));
		tour1.put("end_y", String.valueOf(act1_1_4.getCoord().getY()));
		persontrips.put("person1_1", tour1);
		// ***************************************************************************************
		Activity act1_1_5 = PopulationUtils.createActivityFromLinkId("car interaction", link3);
		act1_1_5.setStartTime(47400.0);
		act1_1_5.setEndTime(47400.0);
		act1_1_5.setLinkId(link2);
		act1_1_5.setCoord(CoordUtils.createCoord(111.0, 213.0));
		act1_1_5.setFacilityId(Id.create("id5", ActivityFacility.class));
		person1_plan1.addActivity(act1_1_5);
		Leg leg1_1_5 = PopulationUtils.createLeg(TransportMode.car);
		Route route1_1_5 = RouteUtils.createGenericRouteImpl(link2, link1);
		route1_1_5.setDistance(5000);
		leg1_1_5.setTravelTime(1800.0);
		leg1_1_5.setRoute(route1_1_5);
		person1_plan1.addLeg(leg1_1_5);
		// ******************************************************************************************
		Activity act1_1_6 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		act1_1_6.setStartTime(49200.0);
		act1_1_6.setEndTime(49200.0);
		act1_1_6.setLinkId(link1);
		act1_1_6.setCoord(CoordUtils.createCoord(10.0, 20.0));
		act1_1_6.setFacilityId(Id.create("id6", ActivityFacility.class));
		person1_plan1.addActivity(act1_1_6);
		Leg leg1_1_6 = PopulationUtils.createLeg(TransportMode.walk);
		leg1_1_6.setRoute(route1_1_1);
		leg1_1_6.setTravelTime(300.0);
		person1_plan1.addLeg(leg1_1_6);
		// *******************************************************************************************+
		Activity act1_1_7 = PopulationUtils.createActivityFromLinkId("home", link1);
		act1_1_7.setStartTime(49500.0);
		act1_1_7.setLinkId(link1);
		act1_1_7.setCoord(CoordUtils.createCoord(11.0, 22.0));
		act1_1_7.setFacilityId(Id.create("id1", ActivityFacility.class));
		person1_plan1.addActivity(act1_1_7);
		// **********************Trip
		// ends***********************************************************

		tour2.put("dep_time", LocalTime.of(46500 / 3600, (46500 % 3600) / 60, (46500 % 3600) % 60).format(dtf));
		tour2.put("trav_time", LocalTime.of(3000 / 3600, (3000 % 3600) / 60, (3000 % 3600) % 60).format(dtf));
		tour2.put("traveled_distance", "5400");
		tour2.put("euclidean_distance",
				(int) CoordUtils.calcEuclideanDistance(act1_1_4.getCoord(), act1_1_7.getCoord()));
		tour2.put("longest_distance_mode", new String(TransportMode.car));
		tour2.put("modes", new String(TransportMode.walk + "-" + TransportMode.car + "-" + TransportMode.walk));
		tour2.put("start_activity_type", "work");
		tour2.put("end_activity_type", "home");
		tour2.put("start_facility_id", String.valueOf(act1_1_4.getFacilityId()));
		tour2.put("start_link", String.valueOf(act1_1_4.getLinkId()));
		tour2.put("start_x", String.valueOf(act1_1_4.getCoord().getX()));
		tour2.put("start_y", String.valueOf(act1_1_4.getCoord().getY()));
		tour2.put("end_facility_id", String.valueOf(act1_1_7.getFacilityId()));
		tour2.put("end_link", String.valueOf(act1_1_7.getLinkId()));
		tour2.put("end_x", String.valueOf(act1_1_7.getCoord().getX()));
		tour2.put("end_y", String.valueOf(act1_1_7.getCoord().getY()));
		persontrips.put("person1_2", tour2);
		person1_plan1.setScore(123.0);

		map.put(Id.create("person1", Person.class), person1_plan1);
		/************************************
		 * Person - creating person 2
		 ************************************/
		Person person2 = PopulationUtils.getFactory().createPerson(Id.create("2", Person.class));
		final Plan person2_plan1 = PopulationUtils.createPlan(person2);

		Activity act2_1_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		act2_1_1.setStartTime(21600.0);
		act2_1_1.setEndTime(21900.0);
		act2_1_1.setLinkId(link1);
		// act2_1_1.setCoord(CoordUtils.createCoord(11.0, 22.0));
		act2_1_1.setFacilityId(Id.create("id5", ActivityFacility.class));
		person2_plan1.addActivity(act2_1_1);
		Leg leg2_1_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route2_1_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route2_1_1.setDistance(100);
		leg2_1_1.setRoute(route1_1_1);
		leg2_1_1.setTravelTime(300.0);
		person2_plan1.addLeg(leg2_1_1);
		// ****************************************************************************************
		Activity act2_1_2 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		act2_1_2.setStartTime(22200.0);
		act2_1_2.setEndTime(22200.0);
		act2_1_2.setLinkId(link1);
		act2_1_2.setCoord(CoordUtils.createCoord(10.0, 20.0));
		act2_1_2.setFacilityId(Id.create("id6", ActivityFacility.class));
		person2_plan1.addActivity(act2_1_2);
		Leg leg2_1_2 = PopulationUtils.createLeg(TransportMode.pt);
		Route route2_1_2 = RouteUtils.createGenericRouteImpl(link1, link2);
		route2_1_2.setDistance(5000);
		leg2_1_2.setRoute(route2_1_2);
		leg2_1_2.setTravelTime(1800.0);
		person2_plan1.addLeg(leg2_1_2);
		// ***************************************************************************************
		Activity act2_1_3 = PopulationUtils.createActivityFromLinkId("car interaction", link3);
		act2_1_3.setStartTime(24000.0);
		act2_1_3.setEndTime(24000.0);
		act2_1_3.setLinkId(link3);
		act2_1_3.setCoord(CoordUtils.createCoord(111.0, 213.0));
		act2_1_3.setFacilityId(Id.create("id7", ActivityFacility.class));
		person1_plan1.addActivity(act2_1_3);
		Leg leg2_1_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route2_1_3 = RouteUtils.createGenericRouteImpl(link3, link3);
		route2_1_3.setDistance(300);
		leg2_1_3.setRoute(route2_1_3);
		leg2_1_3.setTravelTime(900.0);
		person2_plan1.addLeg(leg2_1_3);

		Activity act2_1_4 = PopulationUtils.createActivityFromLinkId("shopping", link3);
		act2_1_4.setStartTime(24900.0);
		act2_1_4.setEndTime(46500.0);
		// act2_1_4.setLinkId(link3);
		act2_1_4.setCoord(CoordUtils.createCoord(111.0, 213.0));
		// act2_1_4.setFacilityId(Id.create("id8", ActivityFacility.class));
		person2_plan1.addActivity(act2_1_4);
		// **********************Trip
		// ends***********************************************************
		Map<String, Object> tour2_1 = new HashMap<String, Object>();
		tour2_1.put("dep_time", LocalTime.of(21900 / 3600, (21900 % 3600) / 60, (21900 % 3600) % 60).format(dtf));
		tour2_1.put("trav_time", LocalTime.of(3000 / 3600, (3000 % 3600) / 60, (3000 % 3600) % 60).format(dtf));
		tour2_1.put("traveled_distance", "5400");
		tour2_1.put("euclidean_distance",
				(int) CoordUtils.calcEuclideanDistance(CoordUtils.createCoord(10.0, 20.0), act2_1_4.getCoord()));
		tour2_1.put("longest_distance_mode", new String(TransportMode.pt));
		tour2_1.put("modes", new String(TransportMode.walk + "-" + TransportMode.pt + "-" + TransportMode.walk));
		tour2_1.put("start_activity_type", "home");
		tour2_1.put("end_activity_type", "shopping");
		tour2_1.put("start_facility_id", String.valueOf(act2_1_1.getFacilityId()));
		tour2_1.put("start_link", String.valueOf(act2_1_1.getLinkId()));
		tour2_1.put("start_x", String.valueOf(act2_1_2.getCoord().getX()));
		tour2_1.put("start_y", String.valueOf(act2_1_2.getCoord().getY()));
		tour2_1.put("end_facility_id", String.valueOf(act2_1_4.getFacilityId()));
		tour2_1.put("end_link", String.valueOf(act2_1_4.getLinkId()));
		tour2_1.put("end_x", String.valueOf(act2_1_4.getCoord().getX()));
		tour2_1.put("end_y", String.valueOf(act2_1_4.getCoord().getY()));
		persontrips.put("person2_1", tour2_1);
		map.put(Id.create("person2", Person.class), person2_plan1);

		performTest(utils.getOutputDirectory() + "/trip.csv", utils.getOutputDirectory() + "/leg.csv", map,
				persontrips);
	}

	private void performTest(String tripsFilename, String legsFilename, IdMap<Person, Plan> map,
			Map<String, Object> persontrips) {

		TripsAndLegsCSVWriter.NoTripWriterExtension tripsWriterExtension = new NoTripWriterExtension();
		TripsAndLegsCSVWriter.NoLegsWriterExtension legWriterExtension = new NoLegsWriterExtension();
		createNetwork();

		TripsAndLegsCSVWriter tripsAndLegsWriter = new TripsAndLegsCSVWriter(scenario, tripsWriterExtension,
				legWriterExtension);
		tripsAndLegsWriter.write(map, tripsFilename, legsFilename);
		readAndValidateValues(persontrips, tripsFilename, legsFilename);
	}

	private void readAndValidateValues(Map<String, Object> persontrips, String tripFile, String legFile) {

		BufferedReader br;
		String line;
		try {
			br = new BufferedReader(new FileReader(tripFile));
			String firstRow = br.readLine();
			String[] columnNames = firstRow.split(";");
			decideColumns(columnNames);
			while ((line = br.readLine()) != null) {

				String[] column = line.split(";");
				String dep_time_value = column[dep_time];
				String trav_time_value = column[trav_time];
				String traveled_distance_value = column[traveled_distance];
				Integer euclidean_distance_value = Integer.parseInt(column[euclidean_distance]);
				String longest_distance_mode_value = column[longest_distance_mode];
				String modes_value = column[modes];
				String start_activity_type_value = column[start_activity_type];
				String end_activity_type_value = column[end_activity_type];
				String start_facility_id_value = column[start_facility_id];
				String start_link_value = column[start_link];
				String start_x_value = column[start_x];
				String start_y_value = column[start_y];
				String end_facility_id_value = column[end_facility_id];
				String end_link_value = column[end_link];
				String end_x_value = column[end_x];
				String end_y_value = column[end_y];
				String trip_id_value = column[trip_id];
				Map<String, Object> tripvalues = (Map<String, Object>) persontrips.get(trip_id_value);
				Assert.assertEquals("Departure time is not as expected", tripvalues.get("dep_time"), dep_time_value);
				Assert.assertEquals("Travel time is not as expected", tripvalues.get("trav_time"), trav_time_value);
				Assert.assertEquals("Travel distance is not as expected", tripvalues.get("traveled_distance"),
						traveled_distance_value);
				Assert.assertEquals("Euclidean distance is not as expected", tripvalues.get("euclidean_distance"),
						euclidean_distance_value);
				Assert.assertEquals("DLongest distance mode is not as expected",
						tripvalues.get("longest_distance_mode"), longest_distance_mode_value);
				Assert.assertEquals("Modes is not as expected", tripvalues.get("modes"), modes_value);
				Assert.assertEquals("Start activity type is not as expected", tripvalues.get("start_activity_type"),
						start_activity_type_value);
				Assert.assertEquals("End activity type is not as expected", tripvalues.get("end_activity_type"),
						end_activity_type_value);
				Assert.assertEquals("Start facility id is not as expected", tripvalues.get("start_facility_id"),
						start_facility_id_value);
				Assert.assertEquals("Start link is not as expected", tripvalues.get("start_link"), start_link_value);
				Assert.assertEquals("Start x is not as expected", tripvalues.get("start_x"), start_x_value);
				Assert.assertEquals("Start y is not as expected", tripvalues.get("start_y"), start_y_value);
				Assert.assertEquals("End facility id is not as expected", tripvalues.get("end_facility_id"),
						end_facility_id_value);
				Assert.assertEquals("End link is not as expected", tripvalues.get("end_link"), end_link_value);
				Assert.assertEquals("End x is not as expected", tripvalues.get("end_x"), end_x_value);
				Assert.assertEquals("End y is not as expected", tripvalues.get("end_y"), end_y_value);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void decideColumns(String[] columnNames) {

		Integer i = 0;
		while (i < columnNames.length) {
			String name = columnNames[i];
			switch (name) {

			case "dep_time":
				dep_time = i;
				break;

			case "trav_time":
				trav_time = i;
				break;

			case "traveled_distance":
				traveled_distance = i;
				break;

			case "euclidean_distance":
				euclidean_distance = i;
				break;

			case "longest_distance_mode":
				longest_distance_mode = i;
				break;

			case "modes":
				modes = i;
				break;

			case "start_activity_type":
				start_activity_type = i;
				break;

			case "end_activity_type":
				end_activity_type = i;
				break;

			case "start_facility_id":
				start_facility_id = i;
				break;

			case "start_link":
				start_link = i;
				break;

			case "start_x":
				start_x = i;
				break;

			case "start_y":
				start_y = i;
				break;

			case "end_facility_id":
				end_facility_id = i;
				break;

			case "end_link":
				end_link = i;
				break;

			case "end_x":
				end_x = i;
				break;

			case "end_y":
				end_y = i;
				break;

			case "trip_id":
				trip_id = i;
				break;
			}
			i++;
		}
	}

	private void createNetwork() {

		Network network = NetworkUtils.createNetwork();
		NetworkFactory factory = network.getFactory();

		Node n0 = factory.createNode(Id.createNodeId(0), new Coord(11, 22));
		network.addNode(n0);
		Node n1 = factory.createNode(Id.createNodeId(1), new Coord(10, 20));
		network.addNode(n1);
		Node n2 = factory.createNode(Id.createNodeId(2), new Coord(111, 213));
		network.addNode(n2);

		Link link_1 = factory.createLink(link1, n0, n1);
		network.addLink(link_1);
		Link link_2 = factory.createLink(link2, n1, n2);
		network.addLink(link_2);
		Link link_3 = factory.createLink(link3, n2, n0);
		network.addLink(link_3);
		NetworkUtils.writeNetwork(network, utils.getOutputDirectory() + "/network.xml");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(utils.getOutputDirectory() + "/network.xml");
	}

}
