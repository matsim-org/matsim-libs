/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * EventsConverter.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.tobiqui.master;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;

public class EventsConverter{
	static LinkedHashMap<Id<Vehicle>, VehicleData> vehicles = new LinkedHashMap<>();
	static LinkedHashMap<Id<Person>, PersonData> persons = new LinkedHashMap<>();
	static List<Event> events = new ArrayList<>();
	static String option;
	static HashSet<Id<Person>> handledPersons = new HashSet<>(); //persons with departureEvent
	static HashSet<Id<Person>> personInfo = new HashSet<>(); //persons in tripInfo-file

	static String configFileName = "../../matsim/examples/siouxfalls-2014/config_renamed.xml";
	static String populationInput = "../../matsim/output/siouxfalls-2014_renamed/output_plans.xml.gz";

	static Config config = ConfigUtils.loadConfig(configFileName);
	static Scenario scenario = ScenarioUtils.createScenario(config);
	static {new MatsimPopulationReader(scenario).readFile(populationInput);}

	public static void main(String[] args) {
		Boolean runTest = false; //false -> run with normal files; true -> run with test-files
		String inputTripInfo = "../../matsim/output/siouxfalls-2014/tripInfo.xml";
		String inputVehroute = "../../matsim/output/siouxfalls-2014/vehroute.xml";
		String inputTripInfoTest = "../../matsim/output/siouxfalls-2014/TestTripInfo.xml";
		String inputVehrouteTest = "../../matsim/output/siouxfalls-2014/TestVehroute.xml";
		String outputEvents = "../../matsim/output/siouxfalls-2014/events.xml";
		String outputEventsTest = "../../matsim/output/siouxfalls-2014/TestEvents.xml";//
		EventsParser eventsParser = new EventsParser();
		option = "parsePersonInfoOnly";
		if (runTest.equals(false)){
			eventsParser.parse(inputTripInfo);
			option = "parseVehiclesOnly";
			eventsParser.parse(inputVehroute);
			option = "parseBusStopsOnly";
			eventsParser.parse(inputVehroute);
			option = "parsePersonsOnly";
			eventsParser.parse(inputVehroute);
		}else{
			eventsParser.parse(inputTripInfoTest);
			option = "parseVehiclesOnly";
			eventsParser.parse(inputVehrouteTest);
			option = "parseBusStopsOnly";
			eventsParser.parse(inputVehrouteTest);
			option = "parsePersonsOnly";
			eventsParser.parse(inputVehrouteTest);
		}
		for(VehicleData vd : vehicles.values()){
			for(int it = 0; it < vd.getLinkTimes().size(); it++){
				Id<Link> linkId = vd.getLinkTimes().get(it).getId();
				Double linkEntered = vd.getLinkTimes().get(it).getEntered();
				Double linkLeft = vd.getLinkTimes().get(it).getLeft();
				Id<Person> personId;
				String vehicleId = vd.getId().toString();
				if (vd.getType().getId().toString().equals("car"))
					personId = Id.createPersonId(vehicleId.substring(vehicleId.indexOf("_"), vehicleId.length()));
				else
					personId = Id.createPersonId("pt_" + vehicleId + "_" + vd.getType().getId().toString());
				if (linkEntered != null)
					events.add(new LinkEnterEvent(linkEntered, personId, linkId, vd.getId()));
				if (it < vd.getLinkTimes().size() -1)
					events.add(new LinkLeaveEvent(linkLeft, personId, linkId, vd.getId()));
			}
		}
		Collections.sort(events, new Comparator<Event>() {
			@Override
			public int compare(Event o1, Event o2) {
				return Double.compare(o1.getTime(), o2.getTime());
			}
		});

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventWriterXML eventWriterXML;
		if (runTest.equals(false))
			eventWriterXML = new EventWriterXML(outputEvents);
		else
			eventWriterXML = new EventWriterXML(outputEventsTest);
		eventsManager.addHandler(eventWriterXML);
		for (Event event : events) {
			eventsManager.processEvent(event);
		}
		eventWriterXML.closeFile();

		System.out.println("number of persons: " + personInfo.size() + "    number of persons with departureEvent (excl. bus driver): " + handledPersons.size());
	}

	static class EventsParser extends MatsimXmlParser{
		public EventsParser(){
			super();
		}
		VehicleData vehicleData;
		PersonData personData;
		Id<Person> personId;
		Id<Vehicle> vehicleId;
		ArrayList<LinkData> linkTimes = new ArrayList<>();
		LinkedHashMap<Id<Link>, Double> busStopDepartures;
		String[] lines;
		ArrayList<TripData> tripInfo;
		int tripIndex = 0;
		Double actTime = 0.0;
		Id<Link> firstLinkId;
		Id<Link> lastLinkId;
		Double arrival;
		Double duration;
		Double departure;
		String mode;
		Boolean isFirstRide = true;
		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			if (option.equals("parsePersonInfoOnly")){
				if ("personinfo".equals(name)){
					personId = Id.create(atts.getValue("id"), Person.class);
					if (personInfo.contains(personId))
						System.out.println(personId);
					personInfo.add(personId);
					personData = new PersonData(personId);
					tripInfo = new ArrayList<>();
				}
				if ("stop".equals(name)){
					if (atts.getValue("arrival") != null){
						TripData tripData = new TripData("stop");
						tripData.setArrival(Double.valueOf(atts.getValue("arrival")));
						tripInfo.add(tripData);
					}
				}
				if ("ride".equals(name)){
					TripData tripData = new TripData("ride");
					if (atts.getValue("arrival") != null){
						tripData.setArrival(Double.valueOf(atts.getValue("arrival")));
					}
					if (atts.getValue("depart") != null){
						tripData.setDeparture(Double.valueOf(atts.getValue("depart")));
					}					
					tripInfo.add(tripData);
				}
				if ("walk".equals(name)){
					if (atts.getValue("arrival") != null){
						TripData tripData = new TripData("walk");
						tripData.setArrival(Double.valueOf(atts.getValue("arrival")));
						tripInfo.add(tripData);
					}
				}
			}

			if (option.equals("parseVehiclesOnly")){
				if ("vehicle".equals(name)){
					vehicleId = Id.create(atts.getValue("id"), Vehicle.class);
					VehicleType type = new VehicleTypeImpl(Id.create(atts.getValue("type"), VehicleType.class));
					Double departure = Double.valueOf(atts.getValue("depart"));
					Double arrival;
					if (atts.getValue("arrival") != null)
						arrival = Double.valueOf(atts.getValue("arrival"));
					else
						arrival = null;
					vehicleData = new VehicleData(vehicleId, type, departure, arrival);
				}
				if ("route".equals(name)){
					String[] edges = atts.getValue("edges").split(" ");
					String[] exitTimes = atts.getValue("exitTimes").split(" ");
					ArrayList<LinkData> linkTimes = new ArrayList<>();
					int length = 0;
					if (edges.length == exitTimes.length)
						length = edges.length;
					else
						if (edges.length > exitTimes.length){
							length = exitTimes.length;
							System.out.println("more edges than exitTimes");
						}else{
							length = edges.length;
							System.out.println("more exitTimes than edges");
						}
					for (int it = 0; it < length; it++){
						if (it == 0)
							linkTimes.add(new LinkData(Id.createLinkId(edges[it]), null, Double.valueOf(exitTimes[it])));
						else 
							linkTimes.add(new LinkData(Id.createLinkId(edges[it]), Double.valueOf(exitTimes[it-1]), Double.valueOf(exitTimes[it])));
					}
					vehicleData.setLinkTimes(linkTimes);
					vehicles.put(vehicleId, vehicleData);
					if (vehicleData.getType().getId().toString().startsWith("Bus")){
						Id<Person> driverId = Id.createPersonId("pt_" + vehicleId + "_" + vehicleData.getType().getId().toString());
						Id<TransitLine> transitLineId = Id.create(vehicleId.toString().substring(vehicleId.toString().indexOf("_") + 1, vehicleId.toString().lastIndexOf("_")), TransitLine.class);
						Id<TransitRoute> transitRouteId = Id.create(edges[0] + "to" + edges[edges.length-1], TransitRoute.class);
						Id<Departure> departureId = Id.create(vehicleId.toString().substring(vehicleId.toString().lastIndexOf("_") + 1, vehicleId.toString().length()), Departure.class);
						//						System.out.println(departure + "\t" + vehicleId + "\t" + driverId + "\t" + transitLineId + "\t" + transitRouteId + "\t" + departureId);
						events.add(new TransitDriverStartsEvent(vehicleData.getDeparture(), driverId, vehicleId, transitLineId, transitRouteId, departureId));
						Id<Link> startLink = Id.createLinkId(edges[0]);
						Id<Link> endLink = Id.createLinkId(edges[edges.length-1]);
						Id<TransitStopFacility> to = Id.create(endLink, TransitStopFacility.class);
						events.add(new PersonDepartureEvent(vehicleData.getDeparture(), driverId, startLink, "car"));
						events.add(new PersonEntersVehicleEvent(vehicleData.getDeparture(), driverId, vehicleId));
						if (vehicleData.getArrival() != null){
							events.add(new VehicleDepartsAtFacilityEvent(vehicleData.getArrival(), vehicleId, to, 0));
							events.add(new PersonLeavesVehicleEvent(vehicleData.getArrival(), driverId, vehicleId));
							events.add(new PersonArrivalEvent(vehicleData.getArrival(), driverId, endLink, "car"));
						}else{
							System.out.println("vehicle " + vehicleId + " did not arrive");
						}
					}
				}
			}
			if (option.equals("parseBusStopsOnly")){
				//get busStop departures
				if ("person".equals(name)){
					personId = Id.create(atts.getValue("id"), Person.class);
					busStopDepartures = new LinkedHashMap<>();
					isFirstRide = true;
					tripIndex = 1;
				}
				if (personId.toString().contains("pt")){
					if ("ride".equals(name)){
						Id<Link> stopId = Id.createLinkId(atts.getValue("from"));
						Double departure = persons.get(personId).getTripInfo().get(tripIndex).getDeparture();
						busStopDepartures.put(stopId, departure);
						if (isFirstRide){
							vehicleId = Id.create(atts.getValue("lines"), Vehicle.class);
							isFirstRide = false;
						}
						tripIndex++;
					}
					if ("stop".equals(name)){
						tripIndex++;
					}
				}
			}
			if (option.equals("parsePersonsOnly")){
				if ("person".equals(name)){
					tripIndex = 1;
					personId = Id.create(atts.getValue("id"), Person.class);
				}

				if ("walk".equals(name)){
					if (personId.toString().contains("pt") == false){
						mode = "walk";
						String edges = atts.getValue("edges");
						if (edges.contains(" ")){ //walk has more than one edge?
							firstLinkId = Id.createLinkId(edges.substring(0, edges.indexOf(" ")));
							lastLinkId = Id.createLinkId(edges.substring(edges.lastIndexOf(" ") + 1, edges.length()));
						}else{
							firstLinkId = Id.createLinkId(edges); //walk has only one edge
							lastLinkId = firstLinkId;
						}
						Double departure = persons.get(personId).getTripInfo().get(tripIndex-1).getArrival();
						Double arrival = persons.get(personId).getTripInfo().get(tripIndex).getArrival();
						Double duration = Double.valueOf(atts.getValue("duration"));
						Id<ActivityFacility> actFacId = ((Activity) scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(tripIndex-1)).getFacilityId();
						String actType = ((Activity) scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(tripIndex-1)).getType();
						mode = ((Leg) scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(tripIndex)).getMode();
						events.add(new ActivityEndEvent(departure, personId, firstLinkId, actFacId, actType));
						events.add(new PersonDepartureEvent(departure, personId, firstLinkId, mode));
						handledPersons.add(personId);
						events.add(new PersonArrivalEvent(arrival, personId, lastLinkId, mode));
						actFacId = ((Activity) scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(tripIndex+1)).getFacilityId();
						actType = ((Activity) scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(tripIndex+1)).getType();
						events.add(new ActivityStartEvent(arrival, personId, lastLinkId, actFacId, actType));
					}
					tripIndex++;
				}
				if ("ride".equals(name)){
					if (personId.toString().contains("pt") == false){
						if (atts.getValue("lines").contains("car"))
							mode = "car";
						if (atts.getValue("lines").contains("bus")){
							mode = "pt";
							lines = atts.getValue("lines").split(" ");
						}

						firstLinkId = Id.createLinkId(atts.getValue("from"));
						lastLinkId = Id.createLinkId(atts.getValue("to"));

						Double departure = persons.get(personId).getTripInfo().get(tripIndex).getDeparture();
						Double arrival;
						Id<ActivityFacility> actFacId = ((Activity) scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(tripIndex-1)).getFacilityId();
						String actType = ((Activity) scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(tripIndex-1)).getType();
						if (persons.get(personId).getTripInfo().get(tripIndex).getArrival() != null)
							arrival = persons.get(personId).getTripInfo().get(tripIndex).getArrival();
						else
							arrival = null;
						//						events.add(new ActivityEndEvent(departure, personId, firstLinkId, actFacId, actType));
						//						events.add(new PersonDepartureEvent(departure, personId, firstLinkId, mode));
						if ("pt".equals(mode)){
							Double ptDeparture = persons.get(personId).getTripInfo().get(tripIndex-1).getArrival();
							Id<TransitStopFacility> from = Id.create(firstLinkId, TransitStopFacility.class);
							Id<TransitStopFacility> to = Id.create(lastLinkId, TransitStopFacility.class);
							events.add(new ActivityEndEvent(ptDeparture, personId, firstLinkId, actFacId, actType));
							events.add(new PersonDepartureEvent(ptDeparture, personId, firstLinkId, mode));
							handledPersons.add(personId);
							events.add(new AgentWaitingForPtEvent(ptDeparture, personId, from, to));
						}
						if ("car".equals(mode)){
							events.add(new ActivityEndEvent(departure, personId, firstLinkId, actFacId, actType));
							events.add(new PersonDepartureEvent(departure, personId, firstLinkId, mode));
							handledPersons.add(personId);
							events.add(new PersonEntersVehicleEvent(departure, personId, Id.create("car_" + personId.toString(), Vehicle.class)));
						}else
							if ("pt".equals(mode))
								for(int i = 0; i < lines.length; i++){
									Id<Vehicle> busId = Id.create(lines[i], Vehicle.class);
									Double busDeparture, busArrival;
									if (vehicles.containsKey(busId)){
										busDeparture = vehicles.get(busId).getDeparture();
										busArrival = vehicles.get(busId).getArrival();
									}
									else{
										System.out.println(busId.toString() + "not in vehicles");
										continue;
									}
									if (vehicles.get(busId).getBusStopDepartures() != null){
										if (vehicles.get(busId).getBusStopDepartures().get(firstLinkId) - departure == 0){
											events.add(new PersonEntersVehicleEvent(departure, personId, busId));
											events.add(new PersonLeavesVehicleEvent(arrival, personId, busId));
											break;
										}
									}else
										System.out.println("vehicle " + busId + " has no busStopDepartures [person: " + personId + "; linkId: " + firstLinkId + ": "
												+ vehicles.get(busId).getBusStopDepartures().get(firstLinkId) + " <-> " + departure);
									if (i - lines.length + 1 == 0)
										System.out.println("no PersonEnters- and PersonLeavesVehicleEvents created (personId: " + personId.toString());
								}
						if (arrival != null){
							if ("car".equals(mode))
								events.add(new PersonLeavesVehicleEvent(arrival, personId, Id.create("car_" + personId.toString(), Vehicle.class)));
							events.add(new PersonArrivalEvent(arrival, personId, lastLinkId, mode));

							actFacId = ((Activity) scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(tripIndex+1)).getFacilityId();
							actType = ((Activity) scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(tripIndex+1)).getType();
							events.add(new ActivityStartEvent(arrival, personId, lastLinkId, actFacId, actType));
						}
					}else{
						vehicleId = Id.create(atts.getValue("lines"), Vehicle.class);
						Double arrival = persons.get(personId).getTripInfo().get(tripIndex-1).getArrival();
						Double departure = persons.get(personId).getTripInfo().get(tripIndex).getDeparture();
						Id<TransitStopFacility> from = Id.create(atts.getValue("from").replace("-", "_"), TransitStopFacility.class);
						events.add(new VehicleArrivesAtFacilityEvent(arrival, vehicleId, from, 0));
						events.add(new VehicleDepartsAtFacilityEvent(departure, vehicleId, from, 0));
						if (persons.get(personId).getTripInfo().size() - tripIndex == 1){
							Id<TransitStopFacility> to = Id.create(atts.getValue("to").replace("-", "_"), TransitStopFacility.class);
							arrival = persons.get(personId).getTripInfo().get(tripIndex).getArrival();
							events.add(new VehicleArrivesAtFacilityEvent(arrival, vehicleId, to, 0));
						}
					}
					tripIndex++;
				}
				if ("stop".equals(name)){
					tripIndex++;
				}
			}
		}

		@Override
		public void endTag(String name, String content, Stack<String> context) {
			if ("personinfo".equals(name)){
				personData.setTripInfo(tripInfo);
				persons.put(personId, personData);
			}
			if (option.equals("parseBusStopsOnly"))
				if ("person".equals(name))
					if (personId.toString().contains("pt")){
						vehicles.get(vehicleId).setBusStopDepartures(busStopDepartures);
						//						System.out.println(vehicleId + ": " + vehicles.get(vehicleId).getBusStopDepartures().values().toString());
					}
		}
	}

	static class VehicleData implements Vehicle{
		private Id<Vehicle> id;
		private VehicleType type;
		private Double departure;
		private Double arrival;
		private ArrayList<LinkData> linkTimes = new ArrayList<>();
		private LinkedHashMap<Id<Link>, Double> busStopDepartures = new LinkedHashMap<>(); //for busses only

		public VehicleData(Id<Vehicle> id, VehicleType type, Double departure,
				Double arrival) {
			this.id = id;
			this.type = type;
			this.departure = departure;
			this.arrival = arrival;
		}

		public void setLinkTimes(ArrayList<LinkData> linkTimes) {
			this.linkTimes = linkTimes;
		}

		public LinkedHashMap<Id<Link>, Double> getBusStopDepartures() {
			return busStopDepartures;
		}

		public void setBusStopDepartures(LinkedHashMap<Id<Link>, Double> busStopDepartures) {
			this.busStopDepartures = busStopDepartures;
		}

		@Override
		public Id<Vehicle> getId() {
			return id;
		}
		@Override
		public VehicleType getType() {
			return type;
		}
		public Double getDeparture() {
			return departure;
		}
		public Double getArrival() {
			return arrival;
		}
		public ArrayList<LinkData> getLinkTimes() {
			return linkTimes;
		}
	}

	static class PersonData{
		private Id<Person> id;
		private Double departure;
		private Double arrival;
		private ArrayList<TripData> tripInfo = new ArrayList<>();

		public PersonData(Id<Person> id) {
			this.id = id;
		}

		public void setDeparture(Double departure) {
			this.departure = departure;
		}

		public void setArrival(Double arrival) {
			this.arrival = arrival;
		}

		public ArrayList<TripData> getTripInfo() {
			return tripInfo;
		}
		public Id<Person> getId() {
			return id;
		}
		public Double getDeparture() {
			return departure;
		}
		public Double getArrival() {
			return arrival;
		}

		public void setTripInfo(ArrayList<TripData> tripInfo) {
			this.tripInfo = tripInfo;
		}
	}

	static class LinkData{
		private Id<Link> id;
		private Double entered;
		private Double left;

		public LinkData(Id<Link> id, Double entered, Double left) {
			this.id = id;
			this.entered = entered;
			this.left = left;
		}

		public Id<Link> getId() {
			return id;
		}
		public Double getEntered() {
			return entered;
		}
		public Double getLeft() {
			return left;
		}
	}

	static class TripData{
		private String type;	//stop, walk or ride
		private Double arrival;
		private Double departure;

		public TripData(String type){
			this.type = type;
		}

		public Double getArrival() {
			return arrival;
		}
		public void setArrival(Double arrival) {
			this.arrival = arrival;
		}
		public Double getDeparture() {
			return departure;
		}
		public void setDeparture(Double departure) {
			this.departure = departure;
		}
		public String getType() {
			return type;
		}
	}
}

