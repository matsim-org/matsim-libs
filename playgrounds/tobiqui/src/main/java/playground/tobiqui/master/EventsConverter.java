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
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;

public class EventsConverter{
	static LinkedHashMap<Id<Vehicle>, VehicleData> vehicles = new LinkedHashMap<>();
	static LinkedHashMap<Id<Person>, PersonData> persons = new LinkedHashMap<>();
	static List<Event> events = new ArrayList<>();
	static Boolean parsePersonInfoOnly = false;
	static Boolean parsePersonsOnly = false;
	static Boolean parseVehiclesOnly = false;
	public static void main(String[] args) {
		EventsParser eventsParser = new EventsParser();
		parsePersonInfoOnly = true;
		eventsParser.parse("E:/MA/sumo-0.22.0/bin/tripInfo.xml");
		parsePersonInfoOnly = false;
		parseVehiclesOnly = true;
		eventsParser.parse("E:/MA/sumo-0.22.0/bin/vehroute.xml");
		parsePersonsOnly = true;
		parseVehiclesOnly = false;
		eventsParser.parse("E:/MA/sumo-0.22.0/bin/vehroute.xml");

		Collections.sort(events, new Comparator<Event>() {
			@Override
			public int compare(Event o1, Event o2) {
				return Double.compare(o1.getTime(), o2.getTime());
			}
		});

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventWriterXML eventWriterXML = new EventWriterXML("../../matsim/output/siouxfalls-2014/events.xml");
		eventsManager.addHandler(eventWriterXML);
		for (Event event : events) {
			eventsManager.processEvent(event);
		}
		eventWriterXML.closeFile();

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
		ArrayList<TripData> tripInfo;
		int tripIndex = 0;
		Double actTime = 0.0;
		Id<Link> firstLinkId;
		Id<Link> lastLinkId;
		Double arrival;
		Double duration;
		Double departure;
		String mode;
		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			if (parsePersonInfoOnly){
				if ("personinfo".equals(name)){
					personId = Id.create(atts.getValue("id"), Person.class);
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
			
			if (parseVehiclesOnly){
				if ("vehicle".equals(name)){
					Id<Vehicle> vehicleId = Id.create(atts.getValue("id"), Vehicle.class);
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
						if ((it == 0) || (it == length-1))
							linkTimes.add(new LinkData(Id.createLinkId(edges[it]), Double.valueOf(exitTimes[it]), Double.valueOf(exitTimes[it])));
						else 
							linkTimes.add(new LinkData(Id.createLinkId(edges[it]), Double.valueOf(exitTimes[it-1]), Double.valueOf(exitTimes[it])));
					}
					vehicleData.setLinkTimes(linkTimes);
					vehicles.put(vehicleId, vehicleData);
				}
			}
			if (parsePersonsOnly){
				if ("person".equals(name)){
					tripIndex = 1;
					personId = Id.create(atts.getValue("id"), Person.class);
				}

				if ("walk".equals(name)){
					mode = "walk";
					String edges = atts.getValue("edges");
					if (edges.contains(" ")){ //walk has more than one edge?
						firstLinkId = Id.createLinkId(edges.substring(0, edges.indexOf(" ")));
						lastLinkId = Id.createLinkId(edges.substring(edges.lastIndexOf(" ") + 1, edges.length()));
					}else{
						firstLinkId = Id.createLinkId(edges); //walk has only one edge
						lastLinkId = firstLinkId;
					}
					Double arrival = persons.get(personId).getTripInfo().get(tripIndex).getArrival();
					Double duration = Double.valueOf(atts.getValue("duration"));
					events.add(new PersonDepartureEvent(arrival - duration, personId, firstLinkId, mode));
					events.add(new PersonArrivalEvent(arrival, personId, lastLinkId, mode));
					tripIndex++;
				}
				if ("ride".equals(name)){
					if (atts.getValue("lines").contains("car"))
						mode = "car";
					if (atts.getValue("lines").contains("bus"))
						mode = "pt";

					firstLinkId = Id.createLinkId(atts.getValue("from"));
					lastLinkId = Id.createLinkId(atts.getValue("to"));

					Double departure = persons.get(personId).getTripInfo().get(tripIndex).getDeparture();
					Double arrival;
					if (persons.get(personId).getTripInfo().get(tripIndex).getArrival() != null)
						arrival = persons.get(personId).getTripInfo().get(tripIndex).getArrival();
					else
						arrival = null;
					events.add(new PersonDepartureEvent(departure, personId, lastLinkId, mode));
					if (arrival != null)
						events.add(new PersonArrivalEvent(arrival, personId, lastLinkId, mode));
					tripIndex++;
				}
			}

			if ("stop".equals(name)){
				tripIndex++;
			}
		}

		@Override
		public void endTag(String name, String content, Stack<String> context) {
			if ("personinfo".equals(name)){
				personData.setTripInfo(tripInfo);
				persons.put(personId, personData);
			}
			// TODO Auto-generated method stub		
		}
	}

	static class VehicleData implements Vehicle{
		private Id<Vehicle> id;
		private VehicleType type;
		private Double departure;
		private Double arrival;
		private ArrayList<LinkData> linkTimes = new ArrayList<>();

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

