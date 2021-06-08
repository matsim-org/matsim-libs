/**
 * 
 */
package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.RoutingModeMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Aravind
 *
 */
public class TripsAndLegsCSVWriterTest {
	
	Config config = ConfigUtils.createConfig();
	Scenario scenario = ScenarioUtils.createScenario(config);
	Id<Person> person1 = Id.create("person1", Person.class);
	Id<Person> person2 = Id.create("person2", Person.class);
	Id<Person> person3 = Id.create("person3", Person.class);
	Id<Person> person4 = Id.create("person4", Person.class);
	Id<Person> person5 = Id.create("person5", Person.class);
	
	final Id<Link> link1 = Id.create(10723, Link.class);
	final Id<Link> link2 = Id.create(123160, Link.class);
	final Id<Link> link3 = Id.create(130181, Link.class);
	
	private int dep_time;
	private int trav_time;
	private int traveled_distance;
	private int euclidean_distance;
	private int main_mode;
	private int longest_distance_mode;
	private int modes;
	private int start_activity_type;
	private int end_activity_type;
	private int start_facility_id;
	private int start_link;
	private int start_x;
	private int start_y;
	private int end_facility_id;
	private int end_link;
	private int end_x;
	private int end_y;
	private int trip_id;
	private int distance;
	private int mode;
	private int wait_time;
	private int access_stop_id;
	private int egress_stop_id;
	private int transit_line;
	private int transit_route;
	private int first_pt_boarding_stop;
	private int last_pt_egress_stop;
	private int trip_number;
	private int person;
	private int transitStopsVisited;
	private int isIntermodalWalkPt;
	
	final IdMap<Person, Plan> map = new IdMap<>(Person.class);
	ArrayList<Object> legsfromplan = new ArrayList<Object>();
	Map<String, Object> persontrips = new HashMap<String, Object>();
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testTripsAndLegsCSVWriter() {
		
		Plans plans = new Plans();

		/****************************
		 * Plan 1 - creating plan 1
		 ************************************/

		Plan plan1 = plans.createPlanOne();

		/********************************
		 * Plan 2 - creating plan 2
		 ********************************/
		Plan plan2 = plans.createPlanTwo();

		/*****************************
		 * Plan 3 - creating plan 3
		 ************************************/
		Plan plan3 = plans.createPlanThree();

		/************************
		 * Plan 4-----creating plan 4
		 **************************************/
		Plan plan4 = plans.createPlanFour();		

		/************************
		 * Plan 5-----creating plan 5
		 **************************************/
		Plan plan5 = plans.createPlanFive();
		
		map.put(person1, plan1);
		map.put(person2, plan2);
		map.put(person3, plan3);
		map.put(person4, plan4);
		map.put(person5, plan5);

		createNetwork();

		//Test with useful AnalysisMainModeIdentifier
		performTest(utils.getOutputDirectory() + "/trip.csv",
				utils.getOutputDirectory() + "/leg.csv", map, new TransportPlanningMainModeIdentifier());

		//Test it does not crash with ill-suited AnalysisMainModeIdentifier
		performTest(utils.getOutputDirectory() + "/trip.csv",
				utils.getOutputDirectory() + "/leg.csv", map, new RoutingModeMainModeIdentifier());
	}
	
	private void performTest(String tripsFilename, String legsFilename, IdMap<Person, Plan> map, AnalysisMainModeIdentifier mainModeIdentifier) {
		TripsAndLegsCSVWriter.NoTripWriterExtension tripsWriterExtension = new NoTripWriterExtension();
		TripsAndLegsCSVWriter.NoLegsWriterExtension legWriterExtension = new NoLegsWriterExtension();
		TripsAndLegsCSVWriter.CustomTripsWriterExtension customTripsWriterExtension = new CustomTripsWriterExtesion();
		TripsAndLegsCSVWriter.CustomLegsWriterExtension customLegsWriterExtension = new CustomLegsWriterExtesion();
		TripsAndLegsCSVWriter tripsAndLegsWriter = new TripsAndLegsCSVWriter(scenario, tripsWriterExtension,
				legWriterExtension, mainModeIdentifier);
		tripsAndLegsWriter.write(map, tripsFilename, legsFilename);
		readTripsFromPlansFile(map, mainModeIdentifier);
		readAndValidateTrips(persontrips, tripsFilename);
		readLegsFromPlansFile(map);
		readAndValidateLegs(legsfromplan, legsFilename);
		TripsAndLegsCSVWriter tripsAndLegsWriterTest = new TripsAndLegsCSVWriter(scenario, customTripsWriterExtension,
				customLegsWriterExtension, mainModeIdentifier);
		tripsAndLegsWriterTest.write(map, tripsFilename, legsFilename);
	}
	
	/***********************************************************
	 * Reading all the trips from the plans file.
	 ***********************************************************/
	private void readTripsFromPlansFile(IdMap<Person, Plan> map, AnalysisMainModeIdentifier mainModeIdentifier) {
		//double trav_time = 0;
		
		for (Map.Entry<Id<Person>, Plan> entry : map.entrySet()) {
			int tripno = 1;
			Plan plan = entry.getValue();
			Id<Person> personId = entry.getKey();
			List<Trip> trips = TripStructureUtils.getTrips(plan);
			Iterator<Trip> tripItr = trips.iterator();
			int tripNo = 1;
			while(tripItr.hasNext()) {
				Map<String, Object> tripvalues = new HashMap<String, Object>();
				String modes = "";
				int traveled_distance = 0;
				double waiting_time = 0;
				Trip trip = tripItr.next();
				Id<Link> start_link = trip.getOriginActivity().getLinkId();
				Coord start_coord = trip.getOriginActivity().getCoord();
				double start_x = 0.0;
				double start_y = 0.0;
				if(start_coord == null) {
					start_coord = scenario.getNetwork().getLinks().get(start_link).getToNode().getCoord();
					start_x = start_coord.getX();
					start_y = start_coord.getY();
				}else {
					start_x = start_coord.getX();
					 start_y = start_coord.getY();
				}
				Id<Link> end_link = trip.getDestinationActivity().getLinkId();
				Coord end_coord = trip.getDestinationActivity().getCoord();
				double end_x = 0.0;
				double end_y = 0.0;
				if(end_coord == null) {
					end_coord = scenario.getNetwork().getLinks().get(end_link).getToNode().getCoord();
					end_x = end_coord.getX();
					end_y = end_coord.getY();
				}else {
					 end_x = end_coord.getX();
					 end_y = end_coord.getY();
				}	
				Id<ActivityFacility> start_facility_id = trip.getOriginActivity().getFacilityId();
				Id<ActivityFacility> end_facility_id = trip.getDestinationActivity().getFacilityId();
				String start_activity_type = trip.getOriginActivity().getType();
				String end_activity_type = trip.getDestinationActivity().getType();
				int euclideanDistance = (int) CoordUtils.calcEuclideanDistance(start_coord, end_coord);
				double departureTime = trip.getOriginActivity().getEndTime().orElse(0);
	            double travelTime = trip.getDestinationActivity().getStartTime().orElse(0) - departureTime;
	            Map<String, Double> modeDistance = new HashMap<String, Double>();
	            modeDistance.put(TransportMode.walk, 0.0);
	            modeDistance.put(TransportMode.pt, 0.0);
	            modeDistance.put(TransportMode.car, 0.0);
	            String first_pt_boarding_stop = null;
				String last_pt_egress_stop = null;
				for (Leg leg : trip.getLegsOnly()) {
					double dis = leg.getRoute().getDistance();
					traveled_distance += dis;
					String mode_val = leg.getMode();
					modes += mode_val + "-";
					dis += modeDistance.get(mode_val);
					modeDistance.put(mode_val, dis);
					//trav_time += leg.getTravelTime();
					Double boardingTime = (Double) leg.getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME);
					if(boardingTime != null) {
						waiting_time += boardingTime - leg.getDepartureTime().seconds();
					}
					if (leg.getRoute() instanceof TransitPassengerRoute) {
						TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
						first_pt_boarding_stop =  first_pt_boarding_stop != null ? first_pt_boarding_stop : route.getAccessStopId().toString();		
				        last_pt_egress_stop = route.getEgressStopId().toString();
					}
				}

				String mainMode = mainModeIdentifier.getClass().equals(RoutingModeMainModeIdentifier.class) ?
						"" : mainModeIdentifier.identifyMainMode(trip.getTripElements());

				Set<String> keyset = modeDistance.keySet();
				Iterator<String> keysetitr = keyset.iterator();
				String longest_distance_mode="";
				double highest = 0.0;
				/***********************************************************
				 * Identifying the mode which has highest distance traveled.
				 ***********************************************************/
				while(keysetitr.hasNext()) {
					String key = keysetitr.next();
					Double value = modeDistance.get(key);
					if(highest < value) {
						highest = value;
						longest_distance_mode = key;
					}
				}
				String transitStopsVisited = null;
				for (Leg leg: trip.getLegsOnly()) {
					if (leg.getRoute() instanceof TransitPassengerRoute) {
						TransitPassengerRoute expTransitRoute = (TransitPassengerRoute) leg.getRoute();
						transitStopsVisited = expTransitRoute.getAccessStopId().toString() + "-" + expTransitRoute.getEgressStopId().toString() + "-";
					}
				}
				if(transitStopsVisited != null) {
					transitStopsVisited = transitStopsVisited.substring(0, transitStopsVisited.length() - 1);
				}
				
				StringBuffer modestrim = new StringBuffer(modes);
				modestrim.deleteCharAt(modestrim.length()-1);
				tripvalues.put("start_link", start_link);
	            tripvalues.put("start_x", start_x);
	            tripvalues.put("start_y", start_y);
	            tripvalues.put("end_link", end_link);
	            tripvalues.put("end_x", end_x);
	            tripvalues.put("end_y", end_y);
	            tripvalues.put("start_facility_id", start_facility_id);
	            tripvalues.put("end_facility_id", end_facility_id);
	            tripvalues.put("start_activity_type", start_activity_type);
	            tripvalues.put("end_activity_type", end_activity_type);
	            tripvalues.put("euclideanDistance", euclideanDistance);
	            tripvalues.put("departureTime", Time.writeTime(departureTime));
	            tripvalues.put("travelTime", Time.writeTime(travelTime));
				tripvalues.put("traveled_distance", traveled_distance);
				tripvalues.put("modes", modestrim);
				tripvalues.put("waiting_time", Time.writeTime(waiting_time));
				tripvalues.put("main_mode", mainMode);
				tripvalues.put("longest_distance_mode", longest_distance_mode);
				tripvalues.put("first_pt_boarding_stop", first_pt_boarding_stop);
				tripvalues.put("last_pt_egress_stop", last_pt_egress_stop);
				tripvalues.put("person", personId);
				tripvalues.put("trip_number", tripNo);
				tripvalues.put("trip_id", personId+"_"+tripNo);
				tripvalues.put("transitStopsVisited", transitStopsVisited);
				tripNo++;
				persontrips.put(entry.getKey()+"_"+tripno, tripvalues);
				tripno++;
			}
		}
	}
	
	/***********************************************************
	 * Reading all the legs from the plans file.
	 ***********************************************************/
	private void readLegsFromPlansFile(IdMap<Person, Plan> map) {

		for (Map.Entry<Id<Person>, Plan> entry : map.entrySet()) {
			Plan plan = entry.getValue();
			Id<Person> personId = entry.getKey();
			List<Trip> trips = TripStructureUtils.getTrips(plan);
			Iterator<Trip> tripItr = trips.iterator();
			int tripNo = 1;
			while(tripItr.hasNext()) {
				Trip trip = tripItr.next();
				List<Leg> legs = trip.getLegsOnly();
				for(Leg leg : legs) {
					Map<String, Object> legvalues = new HashMap<String, Object>();
					OptionalTime travel_time = leg.getTravelTime();
					OptionalTime departure_time = leg.getDepartureTime();
					int leg_distance = (int) leg.getRoute().getDistance();
					String leg_mode = leg.getMode();
					String leg_start_link = leg.getRoute().getStartLinkId().toString();
					String leg_end_link = leg.getRoute().getEndLinkId().toString();
					Coord start_coord = (Coord) leg.getAttributes().getAttribute("startcoord");
					Coord end_coord = (Coord) leg.getAttributes().getAttribute("endcoord");
					double start_x_value = 0.0;
					double start_y_value = 0.0;
					if(start_coord == null) {
						start_coord = scenario.getNetwork().getLinks().get(leg.getAttributes().getAttribute("link")).getToNode().getCoord();
						start_x_value = start_coord.getX();
						start_y_value = start_coord.getY();
					}else {
						start_x_value = start_coord.getX();
						start_y_value = start_coord.getY();
					}
					double end_x_value = end_coord.getX();
					double end_y_value = end_coord.getY();
					double waitingTime = 0;
					Double boardingTime = (Double) leg.getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME);
					if (boardingTime != null) {
			            waitingTime = boardingTime - leg.getDepartureTime().seconds();
			        }
					if (leg.getRoute() instanceof TransitPassengerRoute) {
						 TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
				         String transitLine = route.getLineId().toString();
				         String transitRoute = route.getRouteId().toString();
				         String ptAccessStop = route.getAccessStopId().toString();
				         String ptEgressStop = route.getEgressStopId().toString();
				         
				         legvalues.put("access_stop_id", ptAccessStop);
				         legvalues.put("egress_stop_id", ptEgressStop);
				         legvalues.put("transit_line", transitLine);
				         legvalues.put("transit_route", transitRoute);
					 }
					boolean containsWalk = false;
					boolean containsPt = false;
					if (leg.getMode().equals(TransportMode.walk) || leg.getMode().equals("walk_teleportation")) {
						containsWalk = true;
					} else if (leg.getMode().equals(TransportMode.pt)) {
						containsPt = true;
					}
					String isIntermodalWalkPt = (containsWalk && containsPt) ? "true" : "false";
					legvalues.put("dep_time", Time.writeTime(departure_time));
					legvalues.put("trav_time", Time.writeTime(travel_time));
					legvalues.put("distance", leg_distance);
					legvalues.put("mode", leg_mode);
					legvalues.put("start_link", leg_start_link);
					legvalues.put("start_x", start_x_value);
					legvalues.put("start_y", start_y_value);
					legvalues.put("end_link", leg_end_link);
					legvalues.put("end_x", end_x_value);
					legvalues.put("end_y", end_y_value);
					legvalues.put("wait_time", Time.writeTime(waitingTime));
					legvalues.put("person", personId);
					legvalues.put("trip_id", personId+"_"+tripNo);
					legvalues.put("isIntermodalWalkPt", isIntermodalWalkPt);
					legsfromplan.add(legvalues);
				}
				tripNo++;
			}
		}
	}
	
	/*******************Reading and validating the legs output file************************/
	private void readAndValidateLegs(ArrayList<Object> legsfromplan, String legFile) {
		
		BufferedReader br;
		String line;
		try {
			br = new BufferedReader(new FileReader(legFile));
			String firstRow = br.readLine();
			String[] columnNames = firstRow.split(";");
			Iterator<Object> legItr = legsfromplan.iterator();
			decideColumns(columnNames);
			while ((line = br.readLine()) != null && legItr.hasNext()) {
				String[] column = line.split(";");
				Map<String, Object> nextleg = (Map<String, Object>) legItr.next();
				Assert.assertEquals("dep_time is not as expected", String.valueOf(nextleg.get("dep_time")) , column[dep_time]);
				Assert.assertEquals("trav_time is not as expected", String.valueOf(nextleg.get("trav_time")) , column[trav_time]);
				Assert.assertEquals("wait_time is not as expected", String.valueOf(nextleg.get("wait_time")) , column[wait_time]);
				Assert.assertEquals("Distance is not as expected", String.valueOf(nextleg.get("distance")) , column[distance]);
				Assert.assertEquals("mode is not as expected", String.valueOf(nextleg.get("mode")) , column[mode]);
				Assert.assertEquals("start_link is not as expected", String.valueOf(nextleg.get("start_link")) , column[start_link]);
				Assert.assertEquals("start_x is not as expected", String.valueOf(nextleg.get("start_x")) , column[start_x]);
				Assert.assertEquals("start_y is not as expected", String.valueOf(nextleg.get("start_y")) , column[start_y]);
				Assert.assertEquals("End link is not as expected", String.valueOf(nextleg.get("end_link")) , column[end_link]);
				Assert.assertEquals("end_x is not as expected", String.valueOf(nextleg.get("end_x")) , column[end_x]);
				Assert.assertEquals("end_y is not as expected", String.valueOf(nextleg.get("end_y")) , column[end_y]);
				Assert.assertEquals("person is not as expected", String.valueOf(nextleg.get("person")) , column[person]);
				Assert.assertEquals("trip_id is not as expected", String.valueOf(nextleg.get("trip_id")) , column[trip_id]);
				if(column.length > 13) {
					Assert.assertEquals("access_stop_id is not as expected", String.valueOf(nextleg.get("access_stop_id")) , column[access_stop_id]);
					Assert.assertEquals("egress_stop_id is not as expected", String.valueOf(nextleg.get("egress_stop_id")) , column[egress_stop_id]);
					Assert.assertEquals("transit_line is not as expected", String.valueOf(nextleg.get("transit_line")) , column[transit_line]);
					Assert.assertEquals("transit_route is not as expected", String.valueOf(nextleg.get("transit_route")) , column[transit_route]);
				}
				if(column.length > 17) {
					Assert.assertEquals("isIntermodalWalkPt is not as expected", String.valueOf(nextleg.get("isIntermodalWalkPt")) , column[isIntermodalWalkPt]);
				}
			}
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}
	
	/*******************Reading and validating the trips output file************************/
	private void readAndValidateTrips(Map<String, Object> persontrips, String tripFile) {

		BufferedReader br;
		String line;
		try {
			br = new BufferedReader(new FileReader(tripFile));
			String firstRow = br.readLine();
			String[] columnNames = firstRow.split(";");
			decideColumns(columnNames);
			while ((line = br.readLine()) != null) {

				String[] column = line.split(";");
				String trip_id_value = column[trip_id];
				Map<String, Object> tripvalues = (Map<String, Object>) persontrips.get(trip_id_value);
				Assert.assertEquals("Departure time is not as expected", String.valueOf(tripvalues.get("departureTime")), column[dep_time]);
				Assert.assertEquals("Travel time is not as expected", String.valueOf(tripvalues.get("travelTime")), column[trav_time]);
				Assert.assertEquals("Travel distance is not as expected", String.valueOf(tripvalues.get("traveled_distance")),
						column[traveled_distance]);
				Assert.assertEquals("Euclidean distance is not as expected", tripvalues.get("euclideanDistance"),
						Integer.parseInt(column[euclidean_distance]));
				Assert.assertEquals("Main mode is not as expected",
						tripvalues.get("main_mode"), column[main_mode]);
				Assert.assertEquals("Longest distance mode is not as expected",
						tripvalues.get("longest_distance_mode"), column[longest_distance_mode]);
				Assert.assertEquals("Modes is not as expected", String.valueOf(tripvalues.get("modes")), column[modes]);
				Assert.assertEquals("Start activity type is not as expected", String.valueOf(tripvalues.get("start_activity_type")),
						column[start_activity_type]);
				Assert.assertEquals("End activity type is not as expected", String.valueOf(tripvalues.get("end_activity_type")),
						column[end_activity_type]);
				Assert.assertEquals("Start facility id is not as expected", String.valueOf(tripvalues.get("start_facility_id")),
						column[start_facility_id]);
				Assert.assertEquals("Start link is not as expected", String.valueOf(tripvalues.get("start_link")), column[start_link]);
				Assert.assertEquals("Start x is not as expected", String.valueOf(tripvalues.get("start_x")), column[start_x]);
				Assert.assertEquals("Start y is not as expected", String.valueOf(tripvalues.get("start_y")), column[start_y]);
				Assert.assertEquals("End facility id is not as expected", String.valueOf(tripvalues.get("end_facility_id")),
						column[end_facility_id]);
				Assert.assertEquals("End link is not as expected", String.valueOf(tripvalues.get("end_link")), column[end_link]);
				Assert.assertEquals("End x is not as expected", String.valueOf(tripvalues.get("end_x")), column[end_x]);
				Assert.assertEquals("End y is not as expected", String.valueOf(tripvalues.get("end_y")), column[end_y]);
				Assert.assertEquals("waiting_time is not as expected", String.valueOf(tripvalues.get("waiting_time")), column[wait_time]);
				Assert.assertEquals("person is not as expected", String.valueOf(tripvalues.get("person")), column[person]);
				Assert.assertEquals("trip_number is not as expected", String.valueOf(tripvalues.get("trip_number")), column[trip_number]);
				Assert.assertEquals("trip_id is not as expected", String.valueOf(tripvalues.get("trip_id")), column[trip_id]);
				if(column.length > 21) {
					Assert.assertEquals("first_pt_boarding_stop is not as expected", String.valueOf(tripvalues.get("first_pt_boarding_stop")), column[first_pt_boarding_stop]);
					Assert.assertEquals("last_pt_egress_stop is not as expected", String.valueOf(tripvalues.get("last_pt_egress_stop")), column[last_pt_egress_stop]);
				}
				if(column.length > 23) {
					Assert.assertEquals("transitStopsVisited is not as expected", String.valueOf(tripvalues.get("transitStopsVisited")), column[transitStopsVisited]);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	/*******************************Deciding the columns of the output files************************************/
	private void decideColumns(String[] columnNames) {

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

			case "main_mode":
				main_mode = i;
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
			
			case "distance":
				distance = i;
				break;
				
			case "mode":
				mode = i;
				break;
				
			case "wait_time":
				wait_time = i;
				break;
				
			case "access_stop_id":
				access_stop_id = i;
				break;
				
			case "egress_stop_id":
				egress_stop_id = i;
				break;
				
			case "transit_line":
				transit_line = i;
				break;
				
			case "transit_route":
				transit_route = i;
				break;
				
			case "first_pt_boarding_stop":
				first_pt_boarding_stop = i;
				break;
				
			case "last_pt_egress_stop":
				last_pt_egress_stop = i;
				break;
				
			case "trip_number":
				trip_number = i;
				break;
				
			case "person":
				person = i;
				break;
				
			case "transitStopsVisited":
				transitStopsVisited = i;
				break;
				
			case "isIntermodalWalkPt":
				isIntermodalWalkPt = i;
				break;			
				
			}
			i++;
		}
	}
	/**************************Creating a network*********************************/
	private void createNetwork() {

		Network network = NetworkUtils.createNetwork();
		NetworkFactory factory = network.getFactory();
		
		Node n0, n1, n2, n3;
		network.addNode(n0 = factory.createNode(Id.createNodeId(0), new Coord(30.0, 50.0)));
		network.addNode(n1 = factory.createNode(Id.createNodeId(1), new Coord(100.0, 120.0)));
		network.addNode(n2 = factory.createNode(Id.createNodeId(2), new Coord(111, 213)));
		network.addNode(n3 = factory.createNode(Id.createNodeId(3), new Coord(120, 250)));

		network.addLink(factory.createLink(link1, n0, n1));
		network.addLink(factory.createLink(link2, n1, n2));
		network.addLink(factory.createLink(link3, n2, n3));

		NetworkUtils.writeNetwork(network, utils.getOutputDirectory() + "/network.xml");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(utils.getOutputDirectory() + "/network.xml");
	}
	private class CustomTripsWriterExtesion implements TripsAndLegsCSVWriter.CustomTripsWriterExtension{

		@Override
		public String[] getAdditionalTripHeader() {
			List<String> header = new ArrayList<>();
			header.add("transitStopsVisited");
			return header.toArray(new String[0]);
		}

		@Override
		public List<String> getAdditionalTripColumns(Trip trip) {

			List<String> values = new ArrayList<>();

			String transitStopsVisited = null;
			for (Leg leg: trip.getLegsOnly()) {
				if (leg.getRoute() instanceof TransitPassengerRoute) {
					TransitPassengerRoute expTransitRoute = (TransitPassengerRoute) leg.getRoute();
					transitStopsVisited = expTransitRoute.getAccessStopId().toString() + "-" + expTransitRoute.getEgressStopId().toString() + "-";
				}
			}
			if(transitStopsVisited != null) {
				transitStopsVisited = transitStopsVisited.substring(0, transitStopsVisited.length() - 1);
			}
			values.add(transitStopsVisited);
			return values;
		}
		
	}
	
	static class CustomLegsWriterExtesion implements TripsAndLegsCSVWriter.CustomLegsWriterExtension {
		@Override
		public String[] getAdditionalLegHeader() {
			String[] legHeader = new String[]{"isIntermodalWalkPt"};
			return legHeader;
		}

		@Override
		public List<String> getAdditionalLegColumns(TripStructureUtils.Trip experiencedTrip, Leg experiencedLeg) {
			List<String> legColumn = new ArrayList<>();

			boolean containsWalk = false;
			boolean containsPt = false;

			for (Leg leg: experiencedTrip.getLegsOnly()) {
				if (leg.getMode().equals(TransportMode.walk) || leg.getMode().equals("walk_teleportation")) {
					containsWalk = true;
				} else if (leg.getMode().equals(TransportMode.pt)) {
					containsPt = true;
				}
			}
			String isIntermodalWalkPt = (containsWalk && containsPt) ? "true" : "false";
			legColumn.add(isIntermodalWalkPt);
			return legColumn;
		}
	}
}


