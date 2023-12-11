/* *********************************************************************** *
 * project: org.matsim.*
 * TripsCSVWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.analysis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.analysis.TripsAndLegsWriter.NoLegsWriterExtension;
import org.matsim.analysis.TripsAndLegsWriter.NoTripWriterExtension;
import org.matsim.api.core.v01.*;
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
import org.matsim.vehicles.Vehicle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author Aravind
 *
 */
public class TripsAndLegsWriterTest {

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

	// initialize column array index with negative value -> note if they have been set
	private int dep_time = -1;
	private int trav_time = -1;
	private int traveled_distance = -1;
	private int euclidean_distance = -1;
	private int main_mode = -1;
	private int longest_distance_mode = -1;
	private int modes = -1;
	private int start_activity_type = -1;
	private int end_activity_type = -1;
	private int start_facility_id = -1;
	private int start_link = -1;
	private int start_x = -1;
	private int start_y = -1;
	private int end_facility_id = -1;
	private int end_link = -1;
	private int end_x = -1;
	private int end_y = -1;
	private int trip_id = -1;
	private int distance = -1;
	private int mode = -1;
	private int wait_time = -1;
	private int access_stop_id = -1;
	private int egress_stop_id = -1;
	private int transit_line = -1;
	private int transit_route = -1;
	private int vehicle_id = -1;
	private int first_pt_boarding_stop = -1;
	private int last_pt_egress_stop = -1;
	private int trip_number = -1;
	private int person = -1;
	private int transitStopsVisited = -1;
	private int isIntermodalWalkPt = -1;

	final IdMap<Person, Plan> map = new IdMap<>(Person.class);
	ArrayList<Object> legsfromplan = new ArrayList<>();
	Map<String, Object> persontrips = new HashMap<>();

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testTripsAndLegsCSVWriter() {

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
		TripsAndLegsWriter.NoTripWriterExtension tripsWriterExtension = new NoTripWriterExtension();
		TripsAndLegsWriter.NoLegsWriterExtension legWriterExtension = new NoLegsWriterExtension();
		TripsAndLegsWriter.CustomTimeWriter timeWriter = new TripsAndLegsWriter.DefaultTimeWriter();
		TripsAndLegsWriter.CustomTripsWriterExtension customTripsWriterExtension = new CustomTripsWriterExtesion();
		TripsAndLegsWriter.CustomLegsWriterExtension customLegsWriterExtension = new CustomLegsWriterExtesion();
		TripsAndLegsWriter tripsAndLegsWriter = new TripsAndLegsWriter(scenario, tripsWriterExtension,
				legWriterExtension, mainModeIdentifier, timeWriter);
		tripsAndLegsWriter.write(map, tripsFilename, legsFilename);
		readTripsFromPlansFile(map, mainModeIdentifier);
		readAndValidateTrips(persontrips, tripsFilename);
		readLegsFromPlansFile(map);
		readAndValidateLegs(legsfromplan, legsFilename);
		TripsAndLegsWriter tripsAndLegsWriterTest = new TripsAndLegsWriter(scenario, customTripsWriterExtension,
				customLegsWriterExtension, mainModeIdentifier, timeWriter);
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
				Map<String, Object> tripvalues = new HashMap<>();
				StringBuilder modes = new StringBuilder();
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
	            Map<String, Double> modeDistance = new HashMap<>();
	            modeDistance.put(TransportMode.walk, 0.0);
	            modeDistance.put(TransportMode.pt, 0.0);
	            modeDistance.put(TransportMode.car, 0.0);
	            String first_pt_boarding_stop = null;
				String last_pt_egress_stop = null;
				for (Leg leg : trip.getLegsOnly()) {
					double dis = leg.getRoute().getDistance();
					traveled_distance += dis;
					String mode_val = leg.getMode();
					modes.append(mode_val).append("-");
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

				String mainMode = mainModeIdentifier == null ? "" : mainModeIdentifier.identifyMainMode(trip.getTripElements());
				// null is replaced with "" in the writer
				if (mainMode == null)
					mainMode = "";

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

				StringBuffer modestrim = new StringBuffer(modes.toString());
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
					Map<String, Object> legvalues = new HashMap<>();
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
					String transitLine = "";
					String transitRoute = "";
					String ptAccessStop = "";
					String ptEgressStop = "";
					if (leg.getRoute() instanceof TransitPassengerRoute) {
						 TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
				         transitLine = route.getLineId().toString();
				         transitRoute = route.getRouteId().toString();
				         ptAccessStop = route.getAccessStopId().toString();
				         ptEgressStop = route.getEgressStopId().toString();
					}
					legvalues.put("access_stop_id", ptAccessStop);
					legvalues.put("egress_stop_id", ptEgressStop);
					legvalues.put("transit_line", transitLine);
					legvalues.put("transit_route", transitRoute);
					Id<Vehicle> vehicleId = (Id<Vehicle>) leg.getAttributes().getAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME);
					String vehicleIdString = vehicleId != null ? vehicleId.toString() : "";
					legvalues.put("vehicle_id", vehicleIdString);
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
				String[] column = line.split(";", -1);
				Map<String, Object> nextleg = (Map<String, Object>) legItr.next();
				Assertions.assertEquals(String.valueOf(nextleg.get("dep_time")) , column[dep_time], "dep_time is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("trav_time")) , column[trav_time], "trav_time is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("wait_time")) , column[wait_time], "wait_time is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("distance")) , column[distance], "Distance is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("mode")) , column[mode], "mode is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("start_link")) , column[start_link], "start_link is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("start_x")) , column[start_x], "start_x is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("start_y")) , column[start_y], "start_y is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("end_link")) , column[end_link], "End link is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("end_x")) , column[end_x], "end_x is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("end_y")) , column[end_y], "end_y is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("person")) , column[person], "person is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("trip_id")) , column[trip_id], "trip_id is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("access_stop_id")) , column[access_stop_id] != null ? column[access_stop_id] : "", "access_stop_id is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("egress_stop_id")) , column[egress_stop_id] != null ? column[egress_stop_id] : "", "egress_stop_id is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("transit_line")) , column[transit_line] != null ? column[transit_line] : "", "transit_line is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("transit_route")) , column[transit_route] != null ? column[transit_route] : "", "transit_route is not as expected");
				Assertions.assertEquals(String.valueOf(nextleg.get("vehicle_id")) , column[vehicle_id] != null ? column[vehicle_id] : "", "vehicleId is not as expected");
				if (isIntermodalWalkPt >= 0) {
					// column from CustomLegsWriterExtension is present
					Assertions.assertEquals(String.valueOf(nextleg.get("isIntermodalWalkPt")) , column[isIntermodalWalkPt], "isIntermodalWalkPt is not as expected");
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
				Assertions.assertEquals(String.valueOf(tripvalues.get("departureTime")), column[dep_time], "Departure time is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("travelTime")), column[trav_time], "Travel time is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("traveled_distance")),
						column[traveled_distance],
						"Travel distance is not as expected");
				Assertions.assertEquals(tripvalues.get("euclideanDistance"),
						Integer.parseInt(column[euclidean_distance]),
						"Euclidean distance is not as expected");
				Assertions.assertEquals(tripvalues.get("main_mode"), column[main_mode], "Main mode is not as expected");
				Assertions.assertEquals(tripvalues.get("longest_distance_mode"), column[longest_distance_mode], "Longest distance mode is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("modes")), column[modes], "Modes is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("start_activity_type")),
						column[start_activity_type],
						"Start activity type is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("end_activity_type")),
						column[end_activity_type],
						"End activity type is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("start_facility_id")),
						column[start_facility_id],
						"Start facility id is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("start_link")), column[start_link], "Start link is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("start_x")), column[start_x], "Start x is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("start_y")), column[start_y], "Start y is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("end_facility_id")),
						column[end_facility_id],
						"End facility id is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("end_link")), column[end_link], "End link is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("end_x")), column[end_x], "End x is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("end_y")), column[end_y], "End y is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("waiting_time")), column[wait_time], "waiting_time is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("person")), column[person], "person is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("trip_number")), column[trip_number], "trip_number is not as expected");
				Assertions.assertEquals(String.valueOf(tripvalues.get("trip_id")), column[trip_id], "trip_id is not as expected");
				if(column.length > 21) {
					Assertions.assertEquals(String.valueOf(tripvalues.get("first_pt_boarding_stop")), column[first_pt_boarding_stop], "first_pt_boarding_stop is not as expected");
					Assertions.assertEquals(String.valueOf(tripvalues.get("last_pt_egress_stop")), column[last_pt_egress_stop], "last_pt_egress_stop is not as expected");
				}
				if(column.length > 23) {
					Assertions.assertEquals(String.valueOf(tripvalues.get("transitStopsVisited")), column[transitStopsVisited], "transitStopsVisited is not as expected");
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	/*******************************Deciding the columns of the output files************************************/
	private void decideColumns(String[] columnNames) {

		int i = 0;
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

			case "vehicle_id":
				vehicle_id = i;
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
				default:
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
	private static class CustomTripsWriterExtesion implements TripsAndLegsWriter.CustomTripsWriterExtension{

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

	static class CustomLegsWriterExtesion implements TripsAndLegsWriter.CustomLegsWriterExtension {
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


