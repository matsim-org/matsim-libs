/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBStops2PlansConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.dziemke.analysis.srv;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import playground.dziemke.analysis.Trip;

/**
 * @author dziemke
 */
public class SrVConverter {
	private final static Logger log = Logger.getLogger(SrVConverter.class);

	public static void convert(TreeMap<Id<Person>, TreeMap<Double, Trip>> personTripsMap, Network network, CoordinateTransformation ct, String outputDirectory) {
		
		
		// create objects
		// for writing plans files (newer ones...)
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		
		
		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();
		
		
		List<Event> events = new ArrayList<Event>();
	
	    // add activities from map to plans
	    int tripMapEntryCounter = 0;
	    
	    for (Id<Person> personId : personTripsMap.keySet()) {
	    	
	    	// add person to population
	    	if (!population.getPersons().containsKey(personId)) {
	    		Person person = populationFactory.createPerson(personId);
	    		Plan plan = populationFactory.createPlan();
    			person.addPlan(plan);
    			population.addPerson(person);
    		}
	    	
	    	TreeMap<Double, Trip> tripsMap = personTripsMap.get(personId);
	    	Person person = population.getPersons().get(personId);
	    	
	    	// TODO exclude trip if first activity is not "home"
	    	
	    	for (double departureTime : tripsMap.keySet()) {
	    		tripMapEntryCounter++;
	    		
	    		// plans
	    		Plan plan = person.getPlans().get(0);
	    		
	    		Trip trip = tripsMap.get(departureTime);

	    		// TODO substitute zone by something better; or use alternative (new... as discussed earlier...) data structure that can handle zones
	    		double x = Double.parseDouble(trip.getDepartureZoneId().toString());
	    		double y = x;
	    		// TODO add appropriate coordinate transformation
				Coord departureCoordinates = new Coord(x, y);
	    		

				Id<Person> idToBeChecked = Id.create("1363_1", Person.class);
				
				String activityTypeEndingActivity = trip.getActivityEndActType();	
				if (personId == idToBeChecked) {
					System.err.println("personId = " + personId + " -- trip.getActivityEndActType() = "	+ activityTypeEndingActivity);
				}
				
				Activity endingActivity = populationFactory.createActivityFromCoord(activityTypeEndingActivity, ct.transform(departureCoordinates));
	    		double departureTimeInSeconds = trip.getDepartureTime_s();
				endingActivity.setEndTime(departureTimeInSeconds);
				
				plan.addActivity(endingActivity);
	    		
	    		// TODO make mode adjustable; right now its okay since non-car trips are excluded anyways
	    		Leg leg = populationFactory.createLeg("car");
	    		plan.addLeg(leg);
	    		
	    		// last activity
	    		String activityTypeStartingActivity = trip.getActivityStartActType();
	    		
	    		if (departureTime == tripsMap.lastKey()) {
		    		double x2 = Double.parseDouble(trip.getArrivalZoneId().toString());
		    		double y2 = x2;
		    		Coord arrivalCoordinates = new Coord(x2, y2);
		    		Activity startingActivity = populationFactory.createActivityFromCoord(activityTypeStartingActivity, ct.transform(arrivalCoordinates));
		    		plan.addActivity(startingActivity);
	    		}
				
	    		
				// events
				ActivityEndEvent activityEndEvent = new ActivityEndEvent(departureTimeInSeconds, personId, null, null, activityTypeEndingActivity);
				events.add(activityEndEvent);
//				eventsMap.put(departureTimeInSeconds, activityEndEvent);
				// TODO make mode adjustable
				PersonDepartureEvent personDepartureEvent = new PersonDepartureEvent(departureTimeInSeconds, personId, null, "car");
				events.add(personDepartureEvent);
//				eventsMap.put(departureTimeInSeconds, personDepartureEvent);
				
				double arrivalTimeInSeconds = trip.getArrivalTime_s();
				// TODO make mode adjustable
				PersonArrivalEvent personArrivalEvent = new PersonArrivalEvent(arrivalTimeInSeconds, personId, null, "car");
				events.add(personArrivalEvent);
//				eventsMap.put(arrivalTimeInSeconds, personArrivalEvent);
				ActivityStartEvent activityStartEvent = new ActivityStartEvent(arrivalTimeInSeconds, personId, null, null, activityTypeStartingActivity);
				events.add(activityStartEvent);	
//				eventsMap.put(arrivalTimeInSeconds, activityStartEvent);
	    	}  	
	    }	    
	    
	    // write population
	    MatsimWriter popWriter = new PopulationWriter(population, scenario.getNetwork());
	    popWriter.write(outputDirectory + "plans.xml");
	    
	    //  write events
	    // TODO have events sorted by time
	    int eventsCounter = 0;
	    EventWriterXML eventWriter = new EventWriterXML(outputDirectory + "events.xml");
	    for (Event event : events) {
	    	eventWriter.handleEvent(event);
	    	eventsCounter++;
	    }
	    eventWriter.closeFile();
	    
	    // print counters
	    System.out.println("tripMapEntryCounter = " + tripMapEntryCounter);
	    System.out.println("events added: " + eventsCounter);
	}
}